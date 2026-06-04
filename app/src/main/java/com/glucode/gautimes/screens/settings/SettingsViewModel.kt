package com.glucode.gautimes.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.domain.GetUserSettingsUseCase
import com.glucode.gautimes.domain.SaveUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userSettings: UserSettingsEntity = UserSettingsEntity(),
    val stations: List<StationEntity> = emptyList(),
    val showLocationSheet: Boolean = false,
    val locationTarget: SettingsLocationTarget = SettingsLocationTarget.DEFAULT_FROM
)

enum class SettingsLocationTarget {
    DEFAULT_FROM,
    DEFAULT_TO,
    MORNING_FROM,
    MORNING_TO,
    AFTERNOON_FROM,
    AFTERNOON_TO
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val saveUserSettingsUseCase: SaveUserSettingsUseCase,
    private val stationsRepository: StationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        getUserSettingsUseCase(),
        stationsRepository.getStationsStream()
    ) { state, settings, stations ->
        state.copy(
            userSettings = if (state.userSettings == UserSettingsEntity()) settings else state.userSettings,
            stations = stations
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.UpdateUseSchedule -> {
                updateSettings { it.copy(useSchedule = action.use) }
            }

            is SettingsAction.OpenLocationPicker -> {
                _uiState.update {
                    it.copy(
                        showLocationSheet = true,
                        locationTarget = action.target
                    )
                }
            }

            is SettingsAction.CloseLocationPicker -> {
                _uiState.update { it.copy(showLocationSheet = false) }
            }

            is SettingsAction.SelectLocation -> {
                updateLocation(action.target, action.stationId)
                _uiState.update { it.copy(showLocationSheet = false) }
            }
        }
    }

    private fun updateLocation(target: SettingsLocationTarget, stationId: String) {
        updateSettings { current ->
            when (target) {
                SettingsLocationTarget.DEFAULT_FROM -> current.copy(defaultFromId = stationId)
                SettingsLocationTarget.DEFAULT_TO -> current.copy(defaultToId = stationId)
                SettingsLocationTarget.MORNING_FROM -> current.copy(morningFromId = stationId)
                SettingsLocationTarget.MORNING_TO -> current.copy(morningToId = stationId)
                SettingsLocationTarget.AFTERNOON_FROM -> current.copy(afternoonFromId = stationId)
                SettingsLocationTarget.AFTERNOON_TO -> current.copy(afternoonToId = stationId)
            }
        }
    }

    private fun updateSettings(update: (UserSettingsEntity) -> UserSettingsEntity) {
        viewModelScope.launch {
            val current = uiState.value.userSettings
            val updated = update(current)
            _uiState.update { it.copy(userSettings = updated) }
            saveUserSettingsUseCase(updated)
        }
    }
}

sealed class SettingsAction {
    data class UpdateUseSchedule(val use: Boolean) : SettingsAction()
    data class OpenLocationPicker(val target: SettingsLocationTarget) : SettingsAction()
    data object CloseLocationPicker : SettingsAction()
    data class SelectLocation(val target: SettingsLocationTarget, val stationId: String) :
        SettingsAction()
}
