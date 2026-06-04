package com.glucode.gautimes.domain

import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.UserSettingsRepository
import javax.inject.Inject

class SaveUserSettingsUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(settings: UserSettingsEntity) {
        userSettingsRepository.saveUserSettings(settings)
    }
}
