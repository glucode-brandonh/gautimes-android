package com.glucode.gautimes.domain

import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    operator fun invoke(): Flow<UserSettingsEntity> {
        return userSettingsRepository.getUserSettingsStream().map { it ?: UserSettingsEntity() }
    }
}
