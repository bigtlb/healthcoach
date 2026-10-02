package com.lbthomas.healthcoach.core.sync.handlers

import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.sync.GenericTableSyncHandler
import com.lbthomas.healthcoach.profile.data.ProfileSetting

/**
 * Synchronization handler for the `profileSetting` table.
 */
object ProfileTableSyncHandler : GenericTableSyncHandler<ProfileSetting>("profileSetting") {
    override fun selectAll(database: Database): List<ProfileSetting> =
        database.profileSettingQueries.selectAll().executeAsList()

    override fun getId(entity: ProfileSetting): String = entity.key
    override fun getUpdatedAt(entity: ProfileSetting): Long = entity.updated_at

    override fun insert(database: Database, entity: ProfileSetting) {
        database.profileSettingQueries.insertOrUpdate(entity.key, entity.value_, entity.updated_at)
    }

    override fun update(database: Database, entity: ProfileSetting) {
        database.profileSettingQueries.insertOrUpdate(entity.key, entity.value_, entity.updated_at)
    }

    override fun delete(database: Database, id: String) {
        database.profileSettingQueries.delete(id)
    }
}
