package com.lbthomas.healthcoach.generator

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.utils.generateUuid
import kotlinx.datetime.*
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Generates a realistic sample SQLite database (`docs/healthcoach_sample.db`)
 * containing 2 months of health tracking data for a hypothetical 50-year-old male
 * starting at ~260 lbs who is pursuing healthy weight loss with blood pressure tracking
 * and food journaling.
 */
class SampleDatabaseGeneratorTest {

    @Test
    fun generateSampleDatabase() {
        val userDir = File(System.getProperty("user.dir"))
        val projectRoot = if (userDir.name == "shared") userDir.parentFile else userDir
        val docsDir = File(projectRoot, "docs")
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        val dbFile = File(docsDir, "healthcoach_sample.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        val database = createDatabaseForDriver(driver)

        // Random generator with fixed seed for deterministic, reproducible generation
        val random = Random(42)

        val startDate = LocalDate(2026, 8, 1)
        val endDate = LocalDate(2026, 9, 30)
        val totalDays = (endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1

        println("Generating sample dataset from $startDate to $endDate ($totalDays days)...")

        // =========================================================================
        // 1. Master Food Library Items
        // =========================================================================
        data class MasterFoodDef(
            val id: String,
            val name: String,
            val brand: String?,
            val description: String?,
            val unitName: String,
            val unitQuantity: Double,
            val caloriesPerUnit: Double,
            val upc: String? = null
        )

        val masterFoods = listOf(
            MasterFoodDef("food-oatmeal", "Steel Cut Oatmeal", "Quaker", "Whole grain steel cut rolled oats", "Cup", 0.5, 150.0),
            MasterFoodDef("food-blueberries", "Fresh Blueberries", null, "Organic fresh blueberries", "Cup", 1.0, 84.0),
            MasterFoodDef("food-banana", "Banana", null, "Medium fresh whole banana", "Each", 1.0, 105.0),
            MasterFoodDef("food-eggs", "Large Eggs", "Vital Farms", "Pasture-raised grade A large eggs", "Each", 2.0, 140.0),
            MasterFoodDef("food-toast", "Whole Wheat Toast", "Ezekiel", "Sprouted whole grain bread slice", "Slice", 1.0, 80.0),
            MasterFoodDef("food-butter", "Grass-Fed Butter", "Kerrygold", "Pure Irish salted butter", "Tablespoon", 1.0, 100.0),
            MasterFoodDef("food-greek-yogurt", "Plain Greek Yogurt (0% Fat)", "Kirkland", "Nonfat strained Greek yogurt", "Cup", 0.75, 100.0),
            MasterFoodDef("food-honey", "Raw Organic Honey", "Nature Nate's", "100% pure raw unfiltered honey", "Tablespoon", 1.0, 60.0),
            MasterFoodDef("food-almonds", "Raw Almonds", "Kirkland", "Whole natural California almonds", "oz", 1.0, 160.0),
            MasterFoodDef("food-protein-shake", "Whey Protein Shake", "Optimum Nutrition", "Gold Standard 100% Whey Double Rich Chocolate", "Cup", 1.0, 120.0),
            MasterFoodDef("food-chicken-salad", "Grilled Chicken Salad", null, "Mixed greens with cherry tomatoes, cucumber, carrots", "Cup", 2.0, 90.0),
            MasterFoodDef("food-chicken-breast", "Grilled Chicken Breast", "Kirkland", "Boneless skinless grilled chicken breast", "oz", 6.0, 190.0),
            MasterFoodDef("food-olive-oil", "Extra Virgin Olive Oil", "California Olive Ranch", "Cold-pressed extra virgin olive oil", "Tablespoon", 1.0, 120.0),
            MasterFoodDef("food-balsamic", "Balsamic Vinaigrette", "Newman's Own", "Light balsamic vinaigrette salad dressing", "Tablespoon", 2.0, 70.0),
            MasterFoodDef("food-turkey-wrap", "Turkey Avocado Wrap", null, "Oven roasted sliced turkey, avocado, whole wheat flatbread", "Piece", 1.0, 380.0),
            MasterFoodDef("food-salmon", "Atlantic Salmon Fillet", null, "Pan-seared fresh Atlantic salmon", "oz", 6.0, 280.0),
            MasterFoodDef("food-brown-rice", "Brown Jasmine Rice", "Lundberg", "Steamed organic whole grain brown rice", "Cup", 1.0, 215.0),
            MasterFoodDef("food-broccoli", "Steamed Broccoli", null, "Fresh steamed broccoli florets with sea salt", "Cup", 1.5, 55.0),
            MasterFoodDef("food-sirloin", "Sirloin Steak", null, "Grilled top sirloin beef steak", "oz", 8.0, 360.0),
            MasterFoodDef("food-baked-potato", "Baked Russet Potato", null, "Medium baked potato with skin", "Each", 1.0, 160.0),
            MasterFoodDef("food-sour-cream", "Light Sour Cream", "Daisy", "Pure & natural light sour cream", "Tablespoon", 2.0, 40.0),
            MasterFoodDef("food-ground-turkey", "Lean Ground Turkey (93/7)", "Jennie-O", "All natural lean ground turkey meat", "oz", 6.0, 220.0),
            MasterFoodDef("food-pasta", "Whole Grain Penne", "Barilla", "Whole wheat penne rigate pasta", "Cup", 1.0, 200.0),
            MasterFoodDef("food-marinara", "Marinara Sauce", "Rao's", "Homemade all natural marinara sauce", "Cup", 0.5, 80.0),
            MasterFoodDef("food-apple", "Honeycrisp Apple", null, "Large crisp sweet Honeycrisp apple", "Each", 1.0, 110.0),
            MasterFoodDef("food-peanut-butter", "Creamy Peanut Butter", "Jif", "Natural creamy peanut butter", "Tablespoon", 2.0, 190.0),
            MasterFoodDef("food-black-coffee", "Black Coffee", "Starbucks", "Freshly brewed dark roast coffee", "Cup", 1.5, 5.0),
            // Indulgent / Weekend excursions
            MasterFoodDef("food-pizza", "Pepperoni Pizza Slice", "Costco", "Hand tossed classic pepperoni pizza slice", "Slice", 1.0, 650.0),
            MasterFoodDef("food-craft-beer", "IPA Craft Beer", "Sierra Nevada", "Torpedoe Extra IPA 12 fl oz", "Can", 1.0, 210.0),
            MasterFoodDef("food-cheeseburger", "Bacon Cheeseburger", null, "Quarter-pound beef burger with cheddar, bacon, brioche bun", "Piece", 1.0, 720.0),
            MasterFoodDef("food-fries", "French Fries", null, "Crispy golden salted potato fries", "Package", 1.0, 365.0)
        )

        for (food in masterFoods) {
            database.foodItemQueries.insert(
                id = food.id,
                name = food.name,
                brand = food.brand,
                upc = food.upc,
                description = food.description,
                unitName = food.unitName,
                unitQuantity = food.unitQuantity,
                caloriesPerUnit = food.caloriesPerUnit,
                updated_at = startDate.atTime(0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
            )
        }

        // =========================================================================
        // 2. Daily Simulation: Weight, Blood Pressure, Meals (60 Days)
        // =========================================================================
        // Target profile: 50yo Male, starts at 261.5 lbs, aims for ~1.25 lb loss/week down to ~251 lbs.
        // Convert to kg (1 lb = 0.45359237 kg)
        val lbsToKg = 0.45359237
        var currentWeightLbs = 261.5

        for (dayIndex in 0 until totalDays) {
            val date = startDate.plus(DatePeriod(days = dayIndex))
            val epochMillis = date.atTime(7, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
            val dayOfWeek = date.dayOfWeek
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

            // -------------------------------------------------------------
            // Weight Entry (logged ~85% of days, mostly mornings)
            // -------------------------------------------------------------
            // Daily weight fluctuates (+- 0.4-1.2 lbs for hydration/salt) while trending down ~0.17 lbs/day
            val dailyLoss = 0.175 + (random.nextDouble(-0.35, 0.25))
            currentWeightLbs = (currentWeightLbs - dailyLoss).coerceIn(249.0, 263.0)

            if (random.nextDouble() < 0.88 || dayIndex == 0 || dayIndex == totalDays - 1) {
                val weightKg = (currentWeightLbs * lbsToKg * 10.0).toInt() / 10.0 // round to 0.1 kg
                database.weightEntryQueries.insert(
                    id = generateUuid(),
                    date = date.toString(),
                    weight = weightKg,
                    updated_at = epochMillis
                )
            }

            // -------------------------------------------------------------
            // Blood Pressure Entries (1-2 per day with realistic excursions)
            // -------------------------------------------------------------
            // Base profile for this male: Stage 1 / Elevated (126-135 / 82-88, pulse 70-76)
            // We introduce realistic excursions across the clinical levels:
            // - Normal: relaxed days / post-workout (e.g. 116-120 / 76-79)
            // - Elevated: 121-129 / 78-82
            // - Stage 1: 130-139 / 83-89
            // - Stage 2: 142-152 / 92-98 (e.g. Day 18 stressful meeting, Day 42 high sodium weekend)
            // - Crisis excursion: 164/102 (Day 31 acute stress episode)
            val isHypertensiveCrisisDay = dayIndex == 30 // Mid-point stress excursion
            val isStage2Day = dayIndex in listOf(10, 18, 41, 52)
            val isNormalDay = dayIndex in listOf(7, 14, 21, 28, 35, 48, 56) // restful recovery days

            // Morning BP reading (around 7:30 - 8:30 AM)
            val morningHour = 7 + random.nextInt(2)
            val morningMin = random.nextInt(60)
            val morningTimeStr = "${date}T${morningHour.toString().padStart(2, '0')}:${morningMin.toString().padStart(2, '0')}:00Z"

            val (mSystolic, mDiastolic, mPulse) = when {
                isHypertensiveCrisisDay -> Triple(164 + random.nextInt(4), 102 + random.nextInt(3), 92 + random.nextInt(6))
                isStage2Day -> Triple(145 + random.nextInt(7), 93 + random.nextInt(5), 82 + random.nextInt(6))
                isNormalDay -> Triple(117 + random.nextInt(4), 76 + random.nextInt(4), 66 + random.nextInt(5))
                isWeekend -> Triple(124 + random.nextInt(6), 79 + random.nextInt(5), 70 + random.nextInt(5))
                else -> Triple(128 + random.nextInt(8), 83 + random.nextInt(6), 72 + random.nextInt(6))
            }

            database.bloodPressureEntryQueries.insert(
                id = generateUuid(),
                dateTime = morningTimeStr,
                systolic = mSystolic.toLong(),
                diastolic = mDiastolic.toLong(),
                pulse = mPulse.toLong(),
                updated_at = epochMillis + 1800000L
            )

            // Evening BP reading (~60% of days, around 7:00 - 9:30 PM)
            if (random.nextDouble() < 0.65 || isHypertensiveCrisisDay || isStage2Day) {
                val eveningHour = 19 + random.nextInt(3)
                val eveningMin = random.nextInt(60)
                val eveningTimeStr = "${date}T${eveningHour.toString().padStart(2, '0')}:${eveningMin.toString().padStart(2, '0')}:00Z"

                val (eSystolic, eDiastolic, ePulse) = when {
                    isHypertensiveCrisisDay -> Triple(158 + random.nextInt(5), 98 + random.nextInt(4), 88 + random.nextInt(5))
                    isStage2Day -> Triple(142 + random.nextInt(6), 91 + random.nextInt(5), 80 + random.nextInt(5))
                    isNormalDay -> Triple(119 + random.nextInt(3), 77 + random.nextInt(3), 68 + random.nextInt(4))
                    else -> Triple(mSystolic + random.nextInt(-4, 5), mDiastolic + random.nextInt(-3, 4), mPulse + random.nextInt(-3, 4))
                }

                database.bloodPressureEntryQueries.insert(
                    id = generateUuid(),
                    dateTime = eveningTimeStr,
                    systolic = eSystolic.toLong(),
                    diastolic = eDiastolic.toLong(),
                    pulse = ePulse.toLong(),
                    updated_at = date.atTime(eveningHour, eveningMin).toInstant(TimeZone.UTC).toEpochMilliseconds()
                )
            }

            // -------------------------------------------------------------
            // Food Journal & Meal Entries (Breakfast, Lunch, Dinner, Snacks)
            // -------------------------------------------------------------
            // Helper to log meal snapshot
            fun logMeal(mealTime: MealTime, food: MasterFoodDef, portionMultiplier: Double = 1.0) {
                val totalCal = portionMultiplier * food.caloriesPerUnit
                database.mealEntryQueries.insert(
                    id = generateUuid(),
                    date = date.toString(),
                    mealTime = mealTime.name,
                    foodId = food.id,
                    foodName = food.name,
                    foodDescription = food.description,
                    brand = food.brand,
                    unitName = food.unitName,
                    unitQuantity = food.unitQuantity,
                    caloriesPerUnit = food.caloriesPerUnit,
                    portionMultiplier = portionMultiplier,
                    totalCalories = totalCal,
                    updated_at = date.atTime(
                        when (mealTime) {
                            MealTime.BREAKFAST -> 8
                            MealTime.MORNING_SNACK -> 10
                            MealTime.LUNCH -> 12
                            MealTime.MIDDAY_SNACK -> 15
                            MealTime.DINNER -> 18
                            MealTime.EVENING_SNACK -> 21
                        },
                        random.nextInt(60)
                    ).toInstant(TimeZone.UTC).toEpochMilliseconds()
                )
            }

            // Breakfast
            logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-black-coffee" }, 1.0)
            if (random.nextBoolean()) {
                logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-oatmeal" }, 1.5) // 225 cal
                logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-blueberries" }, 0.5) // 42 cal
            } else {
                logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-eggs" }, 1.0) // 140 cal (2 eggs)
                logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-toast" }, 2.0) // 160 cal (2 slices)
                logMeal(MealTime.BREAKFAST, masterFoods.first { it.id == "food-butter" }, 0.5) // 50 cal
            }

            // Lunch
            if (isWeekend && random.nextDouble() < 0.4) {
                // Weekend burger excursion
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-cheeseburger" }, 1.0)
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-fries" }, 1.0)
            } else if (random.nextBoolean()) {
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-turkey-wrap" }, 1.0)
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-apple" }, 1.0)
            } else {
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-chicken-salad" }, 1.0)
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-chicken-breast" }, 1.0)
                logMeal(MealTime.LUNCH, masterFoods.first { it.id == "food-balsamic" }, 1.0)
            }

            // Mid-day Snack / Pre-workout
            if (random.nextDouble() < 0.75) {
                if (random.nextBoolean()) {
                    logMeal(MealTime.MIDDAY_SNACK, masterFoods.first { it.id == "food-protein-shake" }, 1.0)
                    logMeal(MealTime.MIDDAY_SNACK, masterFoods.first { it.id == "food-banana" }, 1.0)
                } else {
                    logMeal(MealTime.MIDDAY_SNACK, masterFoods.first { it.id == "food-greek-yogurt" }, 1.0)
                    logMeal(MealTime.MIDDAY_SNACK, masterFoods.first { it.id == "food-honey" }, 0.5)
                    logMeal(MealTime.MIDDAY_SNACK, masterFoods.first { it.id == "food-almonds" }, 0.5)
                }
            }

            // Dinner
            if (isWeekend && random.nextDouble() < 0.5) {
                // Weekend Pizza & Beer night
                logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-pizza" }, 2.0) // 2 slices = 1300 cal
                logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-craft-beer" }, 2.0) // 2 beers = 420 cal
            } else {
                val dinnerChoice = random.nextInt(3)
                when (dinnerChoice) {
                    0 -> {
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-salmon" }, 1.0)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-brown-rice" }, 1.0)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-broccoli" }, 1.0)
                    }
                    1 -> {
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-sirloin" }, 1.0)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-baked-potato" }, 1.0)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-sour-cream" }, 0.5)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-broccoli" }, 1.0)
                    }
                    else -> {
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-ground-turkey" }, 1.0)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-pasta" }, 1.5)
                        logMeal(MealTime.DINNER, masterFoods.first { it.id == "food-marinara" }, 1.0)
                    }
                }
            }

            // Evening Snack (occasional)
            if (random.nextDouble() < 0.35) {
                logMeal(MealTime.EVENING_SNACK, masterFoods.first { it.id == "food-apple" }, 1.0)
                logMeal(MealTime.EVENING_SNACK, masterFoods.first { it.id == "food-peanut-butter" }, 0.5)
            }
        }

        // =========================================================================
        // 3. Validation & Reporting
        // =========================================================================
        val totalWeights = database.weightEntryQueries.selectAll().executeAsList().size
        val totalBp = database.bloodPressureEntryQueries.selectAll().executeAsList().size
        val totalMeals = database.mealEntryQueries.selectAll().executeAsList().size
        val totalFoodItems = database.foodItemQueries.selectAll().executeAsList().size

        println("Sample database generated successfully at: ${dbFile.absolutePath}")
        println("File size: ${dbFile.length()} bytes")
        println("Records created:")
        println(" - Weight entries: $totalWeights")
        println(" - Blood Pressure entries: $totalBp")
        println(" - Food Library items: $totalFoodItems")
        println(" - Meal entries: $totalMeals")

        assertTrue(dbFile.exists(), "Database file must exist")
        assertTrue(dbFile.length() > 0, "Database file must not be empty")
        assertTrue(totalWeights >= 45, "Should have at least 45 weight readings")
        assertTrue(totalBp >= 80, "Should have at least 80 blood pressure readings")
        assertTrue(totalMeals >= 150, "Should have at least 150 meal entries")
        assertTrue(totalFoodItems >= 20, "Should have at least 20 food items")
    }
}
