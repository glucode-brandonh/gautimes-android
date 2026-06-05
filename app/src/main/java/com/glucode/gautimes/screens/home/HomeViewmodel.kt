package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.HealthRepository
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.LocationRepository
import com.glucode.gautimes.data.repository.PermissionRepository
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.domain.GetDefaultLocationsUseCase
import com.glucode.gautimes.domain.GetNearestStationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val stationsRepository: StationsRepository,
    private val journeysRepository: JourneysRepository,
    private val locationRepository: LocationRepository,
    private val permissionRepository: PermissionRepository,
    private val homeMapper: HomeMapper,
    private val getNearestStationUseCase: GetNearestStationUseCase,
    private val getDefaultLocationsUseCase: GetDefaultLocationsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    private val _effect = MutableSharedFlow<HomeEffect>()
    val uiEffect = _effect.asSharedFlow()

    private val refreshLocationTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val refreshJourneysTrigger = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    private val permissionCardDismissed = MutableStateFlow(permissionRepository.isLocationCardDismissed())

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
        _state.map { it.fromLocation },
        _state.map { it.toLocation },
        _state.map { it.selectedDate }
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
        _state.map { it.healthCheck },
        _state.map { it.stationsCheck },
        journeys,
        _state.map { it.isProbeCachingEnabled }
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

    private val userInteractionState = _state.map {
        UserInteractionState(
            selection = SelectionState(it.fromLocation, it.toLocation, it.selectedDate),
            locationSheet = LocationSheetState(it.showLocationSheet, it.locationTarget)
        )
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
        _state,
        userInteractionState,
        dataState,
        permissionCardDismissed,
        ticker.onStart { emit(Unit) }
    ) { state, userInteraction, data, dismissed, _ ->
        val fromStation = data.stations.find { it.name == state.fromLocation }
        val nearestStation = getNearestStationUseCase(data.currentLocation, data.stations)
        val isFromNear = fromStation?.id == nearestStation?.id || nearestStation == null

        val showPermissionCard = !dismissed && !permissionRepository.isLocationPermissionGranted()

        homeMapper.mapToHomeState(
            state.isLoading,
            state.isRefreshing,
            state.isFetchingMore,
            state.isGrantingPermission,
            userInteraction,
            data,
            isFromNear,
            showPermissionCard,
            nearestStation?.name
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState.Loading
    )

    init {
        onAction(HomeAction.RefreshLocation)
        onAction(HomeAction.RefreshStations)
        onAction(HomeAction.RefreshJourneys())
        onAction(HomeAction.RefreshHealth)

        viewModelScope.launch {
            getDefaultLocationsUseCase().collect { defaults ->
                val stations = stations.value
                val fromName = stations.find { it.id == defaults.fromId }?.name
                val toName = stations.find { it.id == defaults.toId }?.name

                if (fromName != null) {
                    onAction(HomeAction.UpdateFromLocation(fromName))
                }
                if (toName != null) {
                    onAction(HomeAction.UpdateToLocation(toName))
                }
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Refresh -> refresh()
            is HomeAction.UpdateFromLocation -> _state.update { it.copy(fromLocation = action.location) }
            is HomeAction.UpdateToLocation -> _state.update { it.copy(toLocation = action.location) }
            HomeAction.FlipLocations -> flipLocations()
            is HomeAction.UpdateDate -> updateDate(action.millis)
            is HomeAction.ToggleLocationSheet -> toggleLocationSheet(action.show, action.target)
            HomeAction.ToggleProbeCaching -> toggleProbeCaching()
            HomeAction.RefreshHealth -> refreshHealth()
            HomeAction.RefreshStations -> refreshStations()
            is HomeAction.RefreshJourneys -> refreshJourneys(action.force)
            HomeAction.RefreshLocation -> refreshLocation()
            HomeAction.TestNotification -> testNotification()
            is HomeAction.LoadMore -> loadMore(action.cursor)
            HomeAction.DismissLocationPermissionCard -> dismissLocationPermissionCard()
            is HomeAction.SetGrantingPermission -> _state.update { it.copy(isGrantingPermission = action.isGranting) }
        }
    }

    private fun dismissLocationPermissionCard() {
        permissionRepository.dismissLocationCard()
        permissionCardDismissed.value = true
    }

    private fun loadMore(cursor: String) {
        viewModelScope.launch {
            _state.update { it.copy(isFetchingMore = true) }
            val stations = stations.value
            val from = _state.value.fromLocation
            val to = _state.value.toLocation
            val fromId = stations.find { it.name == from }?.id ?: from.lowercase()
            val toId = stations.find { it.name == to }?.id ?: to.lowercase()

            when (val result = journeysRepository.loadMore(fromId, toId, cursor)) {
                is ApiResult.Success -> {
                    // DB update will trigger flow update
                }

                is ApiResult.Failure -> {
                    _effect.emit(HomeEffect.ShowError(result.error.toDisplayMessage()))
                }
            }
            _state.update { it.copy(isFetchingMore = false) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            refreshStations(force = true)
            refreshJourneys(force = true)
            refreshLocation()
            refreshHealth(force = true)
            delay(500.milliseconds)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun refreshHealth(force: Boolean = shouldForceNetwork()) {
        viewModelScope.launch {
            _state.update { it.copy(healthCheck = HealthCheckState.Checking) }
            val newState = when (val result = healthRepository.getHealth(forceNetwork = force)) {
                is ApiResult.Success -> HealthCheckState.Online(result.value.meta.asOf)
                is ApiResult.Failure -> {
                    _effect.emit(HomeEffect.ShowError(result.error.toDisplayMessage()))
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
                        _effect.emit(HomeEffect.ShowError(result.error.toDisplayMessage()))
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

    private fun testNotification() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.RunNotificationTest)
        }
    }

    private fun toggleProbeCaching() {
        _state.update { it.copy(isProbeCachingEnabled = !it.isProbeCachingEnabled) }
        refreshHealth()
        refreshStations()
        refreshJourneys()
    }

    private fun shouldForceNetwork(): Boolean =
        BuildConfig.DEBUG && !_state.value.isProbeCachingEnabled

    private fun flipLocations() {
        _state.update {
            it.copy(
                fromLocation = it.toLocation,
                toLocation = it.fromLocation
            )
        }
    }

    private fun updateDate(millis: Long?) {
        millis?.let { m ->
            _state.update { it.copy(selectedDate = m) }
        }
    }

    private fun toggleLocationSheet(show: Boolean, target: LocationTarget) {
        _state.update {
            it.copy(
                showLocationSheet = show,
                locationTarget = target
            )
        }
    }
}
