package com.glucode.gautimes.domain

import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

data class DefaultLocations(
    val fromId: String,
    val toId: String
)

class GetDefaultLocationsUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository
) {
    operator fun invoke(): Flow<DefaultLocations> {
        return userSettingsRepository.getUserSettingsStream().map { settings ->
            val effectiveSettings = settings ?: UserSettingsEntity()
            if (effectiveSettings.useSchedule) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 6..9 && effectiveSettings.morningFromId != null && effectiveSettings.morningToId != null -> {
                        DefaultLocations(
                            effectiveSettings.morningFromId,
                            effectiveSettings.morningToId
                        )
                    }

                    hour in 15..18 && effectiveSettings.afternoonFromId != null && effectiveSettings.afternoonToId != null -> {
                        DefaultLocations(
                            effectiveSettings.afternoonFromId,
                            effectiveSettings.afternoonToId
                        )
                    }

                    else -> {
                        DefaultLocations(
                            effectiveSettings.defaultFromId,
                            effectiveSettings.defaultToId
                        )
                    }
                }
            } else {
                DefaultLocations(effectiveSettings.defaultFromId, effectiveSettings.defaultToId)
            }
        }
    }
}
