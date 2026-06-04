package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.dao.UserSettingsDao
import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserSettingsRepository {
    fun getUserSettingsStream(): Flow<UserSettingsEntity?>
    suspend fun saveUserSettings(settings: UserSettingsEntity)
}

class DefaultUserSettingsRepository @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) : UserSettingsRepository {
    override fun getUserSettingsStream(): Flow<UserSettingsEntity?> =
        userSettingsDao.getUserSettings()

    override suspend fun saveUserSettings(settings: UserSettingsEntity) {
        userSettingsDao.saveUserSettings(settings)
    }
}
