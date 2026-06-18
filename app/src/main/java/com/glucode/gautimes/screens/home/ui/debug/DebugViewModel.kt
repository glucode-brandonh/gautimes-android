package com.glucode.gautimes.screens.home.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.HealthRepository
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.LocationRepository
import com.glucode.gautimes.data.repository.StationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DebugViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val stationsRepository: StationsRepository,
    private val journeysRepository: JourneysRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DebugUiState())
    private val _effect = MutableSharedFlow<DebugEffect>()
    val effect = _effect.asSharedFlow()

    private val refreshLocationTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refreshJourneysTrigger = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val stations = stationsRepository.getStationsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val currentLocation = refreshLocationTrigger.onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                emit(locationRepository.getCurrentLocation())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val journeys = refreshJourneysTrigger.onStart { emit(false) }
        .flatMapLatest { force ->
            // In a real scenario, we might want to use the same selections as HomeViewModel
            // but for debug we can just use defaults or last known
            journeysRepository.getJourneys("sandton", "hatfield", forceRefresh = force)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JourneyResult.Loading
        )

    val uiState = combine(
        _state,
        currentLocation,
        journeys
    ) { state, location, journeysResult ->
        val journeysCheck = when (journeysResult) {
            is JourneyResult.Loading -> JourneysCheckState.Checking
            is JourneyResult.Success -> JourneysCheckState.Loaded(
                journeysResult.journeys.size,
                "Just now"
            )
            is JourneyResult.Error -> JourneysCheckState.Failed(journeysResult.message)
        }
        state.copy(
            currentLat = location?.latitude,
            currentLong = location?.longitude,
            journeysCheck = journeysCheck
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebugUiState()
    )

    init {
        onAction(DebugAction.RefreshHealth)
        onAction(DebugAction.RefreshStations)
    }

    fun onAction(action: DebugAction) {
        when (action) {
            DebugAction.RefreshHealth -> refreshHealth()
            DebugAction.RefreshStations -> refreshStations()
            is DebugAction.RefreshJourneys -> refreshJourneys(action.force)
            DebugAction.RefreshLocation -> refreshLocation()
            DebugAction.ToggleProbeCaching -> toggleProbeCaching()
            DebugAction.TestNotification -> testNotification()
        }
    }

    private fun refreshHealth(force: Boolean = shouldForceNetwork()) {
        viewModelScope.launch {
            _state.update { it.copy(healthCheck = HealthCheckState.Checking) }
            val newState = when (val result = healthRepository.getHealth(forceNetwork = force)) {
                is ApiResult.Success -> HealthCheckState.Online(result.value.meta.asOf)
                is ApiResult.Failure -> {
                    _effect.emit(DebugEffect.ShowError(result.error.toDisplayMessage()))
                    HealthCheckState.Offline(result.error.toDisplayMessage())
                }
            }
            _state.update { it.copy(healthCheck = newState) }
        }
    }

    private fun refreshStations(force: Boolean = shouldForceNetwork()) {
        viewModelScope.launch {
            _state.update { it.copy(stationsCheck = StationsCheckState.Checking) }
            val newState =
                when (val result = stationsRepository.refreshStations(forceNetwork = force)) {
                    is ApiResult.Success -> StationsCheckState.Loaded(
                        count = stations.value.size,
                        asOf = "Just now"
                    )
                    is ApiResult.Failure -> {
                        _effect.emit(DebugEffect.ShowError(result.error.toDisplayMessage()))
                        StationsCheckState.Failed(result.error.toDisplayMessage())
                    }
                }
            _state.update { it.copy(stationsCheck = newState) }
        }
    }

    private fun refreshJourneys(force: Boolean = false) {
        refreshJourneysTrigger.tryEmit(force)
    }

    private fun refreshLocation() {
        refreshLocationTrigger.tryEmit(Unit)
    }

    private fun toggleProbeCaching() {
        _state.update { it.copy(isProbeCachingEnabled = !it.isProbeCachingEnabled) }
        refreshHealth()
        refreshStations()
        refreshJourneys(force = true)
    }

    private fun testNotification() {
        viewModelScope.launch {
            _effect.emit(DebugEffect.RunNotificationTest)
        }
    }

    private fun shouldForceNetwork(): Boolean =
        BuildConfig.DEBUG && !_state.value.isProbeCachingEnabled
}
