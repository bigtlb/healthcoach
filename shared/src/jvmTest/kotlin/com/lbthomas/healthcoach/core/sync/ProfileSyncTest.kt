package com.lbthomas.healthcoach.core.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.handlers.ProfileTableSyncHandler
import kotlin.test.*

class ProfileSyncTest {

    private lateinit var driverA: JdbcSqliteDriver
    private lateinit var dbA: Database
    private lateinit var driverB: JdbcSqliteDriver
    private lateinit var dbB: Database
    private lateinit var driverBase: JdbcSqliteDriver
    private lateinit var dbBase: Database

    @BeforeTest
    fun setUp() {
        driverA = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbA = createDatabaseForDriver(driverA)

        driverB = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbB = createDatabaseForDriver(driverB)

        driverBase = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        dbBase = createDatabaseForDriver(driverBase)
    }

    @Test
    fun testProfileSettingBootstrapAndUnionMerge() {
        // Insert profile settings on dbA
        dbA.profileSettingQueries.insertOrUpdate("name", "Alice", 1000L)
        dbA.profileSettingQueries.insertOrUpdate("gender", "FEMALE", 1000L)

        // Bootstrap from dbA to dbB
        val bootstrapStats = ProfileTableSyncHandler.bootstrap(sourceDb = dbA, targetDb = dbB)
        assertEquals(0, bootstrapStats.uploaded)
        assertEquals(2, bootstrapStats.downloaded)

        val settingsOnB = dbB.profileSettingQueries.selectAll().executeAsList()
        assertEquals(2, settingsOnB.size)
        assertEquals("Alice", dbB.profileSettingQueries.selectByKey("name").executeAsOneOrNull()?.value_)
        assertEquals("FEMALE", dbB.profileSettingQueries.selectByKey("gender").executeAsOneOrNull()?.value_)

        // Insert new key on dbB, update key on dbA
        dbB.profileSettingQueries.insertOrUpdate("age", "32", 1500L)
        dbA.profileSettingQueries.insertOrUpdate("name", "Alice B", 2000L)

        val unionStats = ProfileTableSyncHandler.unionMerge(localDb = dbA, remoteDb = dbB)
        assertEquals(1, unionStats.uploaded) // 'name' updated on B
        assertEquals(1, unionStats.downloaded) // 'age' downloaded to A

        assertEquals("Alice B", dbA.profileSettingQueries.selectByKey("name").executeAsOneOrNull()?.value_)
        assertEquals("Alice B", dbB.profileSettingQueries.selectByKey("name").executeAsOneOrNull()?.value_)
        assertEquals("32", dbA.profileSettingQueries.selectByKey("age").executeAsOneOrNull()?.value_)
        assertEquals("32", dbB.profileSettingQueries.selectByKey("age").executeAsOneOrNull()?.value_)
    }

    @Test
    fun testProfileSettingThreeWayMerge() {
        // Base state
        dbBase.profileSettingQueries.insertOrUpdate("name", "Alice", 1000L)
        dbBase.profileSettingQueries.insertOrUpdate("activityLevel", "SEDENTARY", 1000L)

        // Local (dbA) updates activityLevel
        dbA.profileSettingQueries.insertOrUpdate("name", "Alice", 1000L)
        dbA.profileSettingQueries.insertOrUpdate("activityLevel", "MODERATE", 2000L)

        // Remote (dbB) adds targetWeightKg
        dbB.profileSettingQueries.insertOrUpdate("name", "Alice", 1000L)
        dbB.profileSettingQueries.insertOrUpdate("activityLevel", "SEDENTARY", 1000L)
        dbB.profileSettingQueries.insertOrUpdate("targetWeightKg", "65.0", 1500L)

        val stats = ProfileTableSyncHandler.threeWayMerge(baseDb = dbBase, localDb = dbA, remoteDb = dbB)
        assertEquals(1, stats.uploaded) // dbB gets MODERATE
        assertEquals(1, stats.downloaded) // dbA gets targetWeightKg

        assertEquals("MODERATE", dbA.profileSettingQueries.selectByKey("activityLevel").executeAsOneOrNull()?.value_)
        assertEquals("MODERATE", dbB.profileSettingQueries.selectByKey("activityLevel").executeAsOneOrNull()?.value_)
        assertEquals("65.0", dbA.profileSettingQueries.selectByKey("targetWeightKg").executeAsOneOrNull()?.value_)
        assertEquals("65.0", dbB.profileSettingQueries.selectByKey("targetWeightKg").executeAsOneOrNull()?.value_)
    }
}
