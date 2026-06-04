package com.glucode.gautimes.screens.home

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val stationsRepository: StationsRepository,
    private val journeysRepository: JourneysRepository,
    private val locationRepository: LocationRepository,
    private val homeMapper: HomeMapper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _fromLocation = MutableStateFlow("Sandton")
    private val _toLocation = MutableStateFlow("Hatfield")
    private val _selectedDate = MutableStateFlow(Calendar.getInstance().timeInMillis)
    private val _showLocationSheet = MutableStateFlow(false)
    private val _locationTarget = MutableStateFlow(LocationTarget.FROM)
    private val _healthCheck = MutableStateFlow<HealthCheckState>(HealthCheckState.Checking)
    private val _stationsCheck = MutableStateFlow<StationsCheckState>(StationsCheckState.Checking)
    private val _journeysCheck = MutableStateFlow<JourneysCheckState>(JourneysCheckState.Idle)
    private val _isProbeCachingEnabled = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val refreshLocationTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refreshJourneysTrigger = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    private val stations = stationsRepository.getStationsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    private val selectionState = combine(
        _fromLocation,
        _toLocation,
        _selectedDate
    ) { from, to, date ->
        SelectionState(from, to, date)
    }

    @OptIn(
        kotlinx.coroutines.ExperimentalCoroutinesApi::class,
        kotlinx.coroutines.FlowPreview::class
    )
    private val journeys = combine(
        selectionState,
        stations,
        refreshJourneysTrigger.onStart { emit(false) }
    ) { selection, stations, force ->
        val fromId = stations.find { it.name == selection.from }?.id ?: selection.from.lowercase()
        val toId = stations.find { it.name == selection.to }?.id ?: selection.to.lowercase()
        Triple(fromId, toId, force)
    }.debounce(100.milliseconds)
        .flatMapLatest { (fromId, toId, force) ->
            journeysRepository.getJourneys(fromId, toId, forceRefresh = force)
                .transform { result ->
                    if (result is JourneyResult.Loading) {
                        delay(400.milliseconds)
                    }
                    emit(result)
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = JourneyResult.Loading
        )

    private val serviceProbeState = combine(
        _healthCheck,
        _stationsCheck,
        journeys,
        _isProbeCachingEnabled
    ) { healthCheck, stationsCheck, journeysResult, isCachingEnabled ->
        val journeysCheck = when (journeysResult) {
            is JourneyResult.Loading -> JourneysCheckState.Checking
            is JourneyResult.Success -> JourneysCheckState.Loaded(
                journeysResult.journeys.size,
                "Just now"
            )

            is JourneyResult.Error -> JourneysCheckState.Failed(journeysResult.message)
        }
        ServiceProbeState(healthCheck, stationsCheck, journeysCheck, isCachingEnabled)
    }

    private val locationSheetState = combine(
        _showLocationSheet,
        _locationTarget
    ) { show, target ->
        LocationSheetState(show, target)
    }

    private val userInteractionState = combine(
        selectionState,
        locationSheetState
    ) { selection, locationSheet ->
        UserInteractionState(selection, locationSheet)
    }

    private val dataState = combine(
        serviceProbeState,
        stations,
        journeys,
        currentLocation
    ) { serviceProbe, stations, journeysResult, location ->
        DataState(serviceProbe, stations, journeysResult, location)
    }

    val uiState: StateFlow<HomeState> = combine(
        _isLoading,
        _isRefreshing,
        userInteractionState,
        dataState,
        ticker.onStart { emit(Unit) }
    ) { isLoading, isRefreshing, userInteraction, data, _ ->
        homeMapper.mapToHomeState(isLoading, isRefreshing, userInteraction, data)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState.Loading
    )

    init {
        loadData()
        if (BuildConfig.DEBUG) {
            refreshHealth()
        }
        refreshStations()
        refreshJourneys()
        refreshLocation()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds) // Simulate network delay
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshStations(force = true)
            refreshJourneys(force = true)
            refreshLocation()
            if (BuildConfig.DEBUG) {
                refreshHealth(force = true)
            }
            delay(500.milliseconds)
            _isRefreshing.value = false
        }
    }

    fun refreshHealth(force: Boolean = shouldForceNetwork()) {
        viewModelScope.launch {
            _healthCheck.value = HealthCheckState.Checking
            _healthCheck.value = when (val result =
                healthRepository.getHealth(forceNetwork = force)) {
                is ApiResult.Success -> HealthCheckState.Online(result.value.meta.asOf)
                is ApiResult.Failure -> HealthCheckState.Offline(result.error.toDisplayMessage())
            }
        }
    }

    fun refreshStations(force: Boolean = shouldForceNetwork()) {
        viewModelScope.launch {
            _stationsCheck.value = StationsCheckState.Checking
            _stationsCheck.value = when (val result =
                stationsRepository.refreshStations(forceNetwork = force)) {
                is ApiResult.Success -> StationsCheckState.Loaded(
                    count = stations.value.size,
                    asOf = "Just now" // Simplified for now
                )

                is ApiResult.Failure -> StationsCheckState.Failed(result.error.toDisplayMessage())
            }
        }
    }

    fun refreshJourneys(force: Boolean = false) {
        refreshJourneysTrigger.tryEmit(force)
    }

    fun refreshLocation() {
        refreshLocationTrigger.tryEmit(Unit)
    }

    fun toggleProbeCaching() {
        _isProbeCachingEnabled.value = !_isProbeCachingEnabled.value
        refreshHealth()
        refreshStations()
        if (_journeysCheck.value !is JourneysCheckState.Idle) {
            refreshJourneys()
        }
    }

    private fun shouldForceNetwork(): Boolean =
        BuildConfig.DEBUG && !_isProbeCachingEnabled.value

    fun updateFromLocation(location: String) {
        _fromLocation.value = location
    }

    fun updateToLocation(location: String) {
        _toLocation.value = location
    }

    fun flipLocations() {
        val temp = _fromLocation.value
        _fromLocation.value = _toLocation.value
        _toLocation.value = temp
    }

    fun updateDate(millis: Long?) {
        millis?.let {
            _selectedDate.value = it
        }
    }

    fun toggleLocationSheet(show: Boolean, target: LocationTarget) {
        _locationTarget.value = target
        _showLocationSheet.value = show
    }

    companion object {
    }
}
