package com.lbthomas.healthcoach.features.foodjournal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.features.foodjournal.data.FoodJournalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FoodJournalRepositoryTest {

    private lateinit var database: Database
    private lateinit var repository: FoodJournalRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver)
        repository = FoodJournalRepository(database)
    }

    @Test
    fun testFoodUnitCrud() = runBlocking {
        val unitId = repository.addFoodUnit(
            name = "Cup",
            abbreviation = "c",
            isDefault = true
        )

        val units = repository.observeAllFoodUnits().first()
        assertEquals(1, units.size)
        assertEquals("Cup", units[0].name)
        assertEquals("c", units[0].abbreviation)
        assertTrue(units[0].isDefault)

        repository.updateFoodUnit(units[0].copy(name = "Large Cup"))
        val updatedUnits = repository.observeAllFoodUnits().first()
        assertEquals("Large Cup", updatedUnits[0].name)

        repository.deleteFoodUnit(unitId)
        val emptyUnits = repository.observeAllFoodUnits().first()
        assertEquals(0, emptyUnits.size)
    }

    @Test
    fun testFoodItemCrud() = runBlocking {
        val foodId = repository.addFoodItem(
            name = "Rolled Oats",
            brand = "Quaker",
            upc = "0123456789",
            description = "100% Whole Grain",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0
        )

        val items = repository.observeAllFoodItems().first()
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Rolled Oats", item.name)
        assertEquals("Quaker", item.brand)
        assertEquals(150.0, item.caloriesPerUnit)

        repository.updateFoodItem(item.copy(caloriesPerUnit = 160.0))
        val updated = repository.getFoodItemById(foodId)
        assertNotNull(updated)
        assertEquals(160.0, updated.caloriesPerUnit)

        repository.deleteFoodItem(foodId)
        val afterDelete = repository.observeAllFoodItems().first()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testMealEntryLoggingAndHistoricalSnapshot() = runBlocking {
        val foodId = repository.addFoodItem(
            name = "Greek Yogurt",
            brand = "Chobani",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 120.0,
            description = "Plain non-fat"
        )

        val mealDate = LocalDate(2026, 9, 30)
        val mealId = repository.addMealEntry(
            date = mealDate,
            mealTime = MealTime.BREAKFAST,
            foodId = foodId,
            foodName = "Greek Yogurt",
            foodDescription = "Plain non-fat",
            brand = "Chobani",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 120.0,
            portionMultiplier = 1.5,
            totalCalories = 180.0
        )

        val summary = repository.observeDailyMealSummary(mealDate).first()
        assertEquals(180.0, summary.totalCalories)
        val breakfastGroup = summary.mealGroups.first { it.mealTime == MealTime.BREAKFAST }
        assertEquals(1, breakfastGroup.entries.size)
        assertEquals(180.0, breakfastGroup.subtotalCalories)
        assertEquals("Greek Yogurt", breakfastGroup.entries[0].foodName)

        // Mutate master food in library to 200 calories
        val masterFood = repository.getFoodItemById(foodId)!!
        repository.updateFoodItem(masterFood.copy(caloriesPerUnit = 200.0, name = "Greek Yogurt 2.0"))

        // Historical snapshot in meal entry must remain unchanged
        val entries = repository.observeMealEntriesByDate(mealDate).first()
        assertEquals(1, entries.size)
        assertEquals("Greek Yogurt", entries[0].foodName)
        assertEquals(120.0, entries[0].caloriesPerUnit)
        assertEquals(180.0, entries[0].totalCalories)
    }

    @Test
    fun testTokenizedMultiFieldSearch() = runBlocking {
        repository.addFoodItem(
            name = "Rolled Oats",
            brand = "Quaker",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0,
            description = "Organic hot cereal"
        )
        repository.addFoodItem(
            name = "Greek Yogurt Plain",
            brand = "Kirkland",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 100.0,
            description = "Low fat organic"
        )
        repository.addFoodItem(
            name = "Whole Almonds",
            brand = "Kirkland",
            unitName = "oz",
            unitQuantity = 1.0,
            caloriesPerUnit = 160.0,
            description = "Dry roasted"
        )

        // Multi-word search matching across brand and name: "Quaker Oats"
        val search1 = repository.searchFoodItems("Quaker Oats").first()
        assertEquals(1, search1.size)
        assertEquals("Rolled Oats", search1[0].name)

        // Multi-word search matching brand and description: "Kirkland roasted"
        val search2 = repository.searchFoodItems("Kirkland roasted").first()
        assertEquals(1, search2.size)
        assertEquals("Whole Almonds", search2[0].name)

        // Brand matching multiple items: "Kirkland"
        val search3 = repository.searchFoodItems("Kirkland").first()
        assertEquals(2, search3.size)
    }

    @Test
    fun testSearchRelevanceBoostedByFrequentAndRecentUsage() = runBlocking {
        val now = 1775000000000L

        // Food 1: "Oatmeal standard" - 0 usages
        val food1Id = repository.addFoodItem(
            name = "Oatmeal Standard",
            brand = "Store Brand",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0
        )

        // Food 2: "Oatmeal Instant" - logged 5 times recently
        val food2Id = repository.addFoodItem(
            name = "Oatmeal Instant",
            brand = "Quaker",
            unitName = "Packet",
            unitQuantity = 1.0,
            caloriesPerUnit = 160.0
        )

        for (i in 1..5) {
            repository.addMealEntry(
                date = LocalDate(2026, 9, 30),
                mealTime = MealTime.BREAKFAST,
                foodId = food2Id,
                foodName = "Oatmeal Instant",
                unitName = "Packet",
                unitQuantity = 1.0,
                caloriesPerUnit = 160.0,
                portionMultiplier = 1.0,
                totalCalories = 160.0,
                updatedAt = now - (i * 3600_000L) // Logged today / recently
            )
        }

        val searchResults = repository.searchFoodItemsWithRelevance("Oatmeal", nowMillis = now).first()
        assertEquals(2, searchResults.size)

        // The frequently and recently used food item should rank FIRST
        assertEquals(food2Id, searchResults[0].foodItem.id)
        assertEquals("Oatmeal Instant", searchResults[0].foodItem.name)
        assertTrue(searchResults[0].relevanceScore > searchResults[1].relevanceScore)

        assertEquals(food1Id, searchResults[1].foodItem.id)
        assertEquals("Oatmeal Standard", searchResults[1].foodItem.name)
    }

    @Test
    fun testFoodItemMatchesQuery() {
        val item = com.lbthomas.healthcoach.features.foodjournal.data.FoodItemData(
            id = "1",
            name = "Rolled Oats",
            brand = "Quaker",
            description = "Organic hot cereal",
            unitName = "Cup",
            unitQuantity = 1.0,
            caloriesPerUnit = 150.0,
            updatedAt = 0L
        )

        // Blank query matches everything
        assertTrue(item.matchesQuery(""))
        assertTrue(item.matchesQuery("   "))

        // Single word matches in name, brand, or description
        assertTrue(item.matchesQuery("oat"))
        assertTrue(item.matchesQuery("QUAKER"))
        assertTrue(item.matchesQuery("cereal"))

        // Multi-token match across fields
        assertTrue(item.matchesQuery("quaker oats"))
        assertTrue(item.matchesQuery("rolled organic"))

        // Non-matching query
        kotlin.test.assertFalse(item.matchesQuery("banana"))
        kotlin.test.assertFalse(item.matchesQuery("quaker yogurt"))
    }

    @Test
    fun testEnsureDefaultFoodDataSeedingAndSelfHealing() = runBlocking {
        // Initially empty test DB
        assertEquals(0, repository.observeAllFoodItems().first().size)
        assertEquals(0, repository.observeAllFoodUnits().first().size)

        // Seed default food items and units
        repository.ensureDefaultFoods()

        val seededItems = repository.observeAllFoodItems().first()
        val seededUnits = repository.observeAllFoodUnits().first()

        assertTrue(seededItems.size >= 1000, "Should seed at least 1,000 food items (found ${seededItems.size})")
        assertTrue(seededUnits.size >= 15, "Should seed default units (found ${seededUnits.size})")
        assertTrue(seededUnits.any { it.name == "Each" })
        assertTrue(seededUnits.any { it.name == "Piece" })
        assertTrue(seededUnits.any { it.name == "Slice" })
        assertTrue(seededUnits.any { it.name == "Bar" })
        assertTrue(seededUnits.any { it.name == "Can" })
        assertTrue(seededUnits.any { it.name == "Bottle" })
        assertTrue(seededUnits.any { it.name == "Scoop" })

        // Verify specific user-requested food items are present
        val jimmyDean = repository.getFoodItemById("food-jimmy-dean-croissant")
        assertNotNull(jimmyDean)
        assertEquals("Sausage, Egg & Cheese Croissant Sandwich", jimmyDean.name)
        assertEquals("Each", jimmyDean.unitName)
        assertEquals(490.0, jimmyDean.caloriesPerUnit)

        val thickBacon = repository.getFoodItemById("food-thick-bacon")
        assertNotNull(thickBacon)
        assertEquals("Slice", thickBacon.unitName)

        val regularBacon = repository.getFoodItemById("food-regular-bacon")
        assertNotNull(regularBacon)
        assertEquals("Slice", regularBacon.unitName)

        val pureProtein = repository.getFoodItemById("food-pure-protein-vanilla")
        assertNotNull(pureProtein)
        assertEquals("Bottle", pureProtein.unitName)
        assertEquals(140.0, pureProtein.caloriesPerUnit)

        val greekYogurt = repository.getFoodItemById("food-greek-yogurt-kirkland")
        assertNotNull(greekYogurt)

        val cottageCheese = repository.getFoodItemById("food-cottage-cheese-daisy")
        assertNotNull(cottageCheese)

        val americanCheese = repository.getFoodItemById("food-kraft-american-cheese")
        assertNotNull(americanCheese)

        val ramen = repository.getFoodItemById("food-maruchan-chicken-ramen")
        assertNotNull(ramen)

        val potstickers = repository.getFoodItemById("food-bibigo-vegetable-potstickers")
        assertNotNull(potstickers)

        val meatballs = repository.getFoodItemById("food-kirkland-meatballs")
        assertNotNull(meatballs)

        val cannedChicken = repository.getFoodItemById("food-kirkland-canned-chicken")
        assertNotNull(cannedChicken)

        val tuna = repository.getFoodItemById("food-chicken-of-sea-tuna")
        assertNotNull(tuna)

        val flatbread = repository.getFoodItemById("food-carbwise-flatbread")
        assertNotNull(flatbread)

        // Test Self-Healing: Delete a default item and verify it is restored upon ensureDefaultFoods
        repository.deleteFoodItem("food-jimmy-dean-croissant")
        kotlin.test.assertNull(repository.getFoodItemById("food-jimmy-dean-croissant"))

        repository.ensureDefaultFoods()
        val restoredItem = repository.getFoodItemById("food-jimmy-dean-croissant")
        assertNotNull(restoredItem, "Deleted default food item should be restored by ensureDefaultFoods")
        assertEquals("Sausage, Egg & Cheese Croissant Sandwich", restoredItem.name)

        // Test Customization Preservation: Modify an existing default item, ensureDefaultFoods should NOT overwrite it
        repository.updateFoodItem(restoredItem.copy(caloriesPerUnit = 550.0))
        repository.ensureDefaultFoods()
        val preservedItem = repository.getFoodItemById("food-jimmy-dean-croissant")
        assertNotNull(preservedItem)
        assertEquals(550.0, preservedItem.caloriesPerUnit, "User customized calories must be preserved")
    }
}
