package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ProgressCardData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.HealthRepository
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.DateUtils
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val stationsRepository: StationsRepository,
    private val journeysRepository: JourneysRepository
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
    private val refreshJourneysTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

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
        refreshJourneysTrigger.onStart { emit(Unit) }
    ) { selection, stations, _ ->
        val fromId = stations.find { it.name == selection.from }?.id ?: selection.from.lowercase()
        val toId = stations.find { it.name == selection.to }?.id ?: selection.to.lowercase()
        Triple(fromId, toId, selection.dateMillis)
    }.debounce(100.milliseconds)
        .flatMapLatest { (fromId, toId, _) ->
            journeysRepository.getJourneys(fromId, toId)
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
        journeys
    ) { serviceProbe, stations, journeysResult ->
        DataState(serviceProbe, stations, journeysResult)
    }

    val uiState: StateFlow<HomeState> = combine(
        _isLoading,
        userInteractionState,
        dataState,
        ticker.onStart { emit(Unit) }
    ) { isLoading, userInteraction, data, _ ->
        if (isLoading) {
            HomeState.Loading
        } else {
            val selection = userInteraction.selection
            val locationSheet = userInteraction.locationSheet
            val serviceProbe = data.serviceProbe
            val stations = data.stations
            val journeysResult = data.journeysResult

            val stationNames = stations.map { it.name }.ifEmpty { locations }
            val scheduleTimes = if (journeysResult is JourneyResult.Success) {
                journeysResult.journeys.map { journey ->
                    val firstLeg = journey.legs.firstOrNull()
                    ScheduleTimeLineItemData(
                        timeText = DateUtils.formatIsoTime(journey.journey.departureTime),
                        cartColor = firstLeg?.lineColour?.toColor() ?: cartYellow,
                        cartNumber = firstLeg?.carriages ?: 4
                    )
                }
            } else emptyList()
            
            val nextJourney = if (journeysResult is JourneyResult.Success) {
                val now = OffsetDateTime.now(ZoneOffset.UTC)
                journeysResult.journeys
                    .filter { OffsetDateTime.parse(it.journey.departureTime).isAfter(now) }
                    .minByOrNull { it.journey.departureTime }
            } else null

            HomeState.HasData(
                data = HomeData(
                    fromLocation = selection.from,
                    toLocation = selection.to,
                    dateLabel = DateUtils.formatDateLabel(selection.dateMillis),
                    scheduleTimes = scheduleTimes,
                    journeyResult = journeysResult,
                    infoText = HomeInfoText(
                        title = "Coming up next",
                        description = "Peak fares will be in-affect until 18:45 tonight"
                    ),
                    healthCheck = serviceProbe.healthCheck,
                    stationsCheck = serviceProbe.stationsCheck,
                    journeysCheck = serviceProbe.journeysCheck,
                    isProbeCachingEnabled = serviceProbe.isCachingEnabled,
                    progress = buildProgressCard(nextJourney),
                    showLocationSheet = locationSheet.show,
                    locationSection = buildLocationSelector(
                        locationSheet.target,
                        selection.from,
                        selection.to,
                        stationNames
                    )
                )
            )
        }
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
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds) // Simulate network delay
            _isLoading.value = false
        }
    }

    fun refreshHealth() {
        viewModelScope.launch {
            _healthCheck.value = HealthCheckState.Checking
            _healthCheck.value = when (val result =
                healthRepository.getHealth(forceNetwork = shouldForceNetwork())) {
                is ApiResult.Success -> HealthCheckState.Online(result.value.meta.asOf)
                is ApiResult.Failure -> HealthCheckState.Offline(result.error.toDisplayMessage())
            }
        }
    }

    fun refreshStations() {
        viewModelScope.launch {
            _stationsCheck.value = StationsCheckState.Checking
            _stationsCheck.value = when (val result =
                stationsRepository.refreshStations(forceNetwork = shouldForceNetwork())) {
                is ApiResult.Success -> StationsCheckState.Loaded(
                    count = stations.value.size,
                    asOf = "Just now" // Simplified for now
                )

                is ApiResult.Failure -> StationsCheckState.Failed(result.error.toDisplayMessage())
            }
        }
    }

    fun refreshJourneys() {
        refreshJourneysTrigger.tryEmit(Unit)
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

    fun buildProgressCard(nextJourney: JourneyWithLegs?): ProgressCardData {
        val progress = if (nextJourney != null) {
            val minutesUntil = DateUtils.getMinutesUntil(nextJourney.journey.departureTime)
            ProgressCardData(
                timeValue = minutesUntil.toString(),
                progressDescription = "MINUTES UNTIL DEPARTURE"
            )
        } else {
            ProgressCardData(
                timeValue = "--",
                progressDescription = "NO MORE JOURNEYS"
            )
        }
        return progress
    }

    fun buildLocationSelector(
        target: LocationTarget,
        fromLocation: String,
        toLocation: String,
        availableLocations: List<String> = locations
    ): LocationSelectorBottomSheetData {
        val selected = if (target == LocationTarget.FROM) fromLocation else toLocation
        val disabled = if (target == LocationTarget.FROM) toLocation else fromLocation
        return LocationSelectorBottomSheetData(
            locations = availableLocations,
            selectedLocation = selected,
            disabledLocation = disabled,
            locationTarget = target
        )
    }

    fun toggleLocationSheet(show: Boolean, target: LocationTarget) {
        _locationTarget.value = target
        _showLocationSheet.value = show
    }
}

data class SelectionState(
    val from: String,
    val to: String,
    val dateMillis: Long
)

data class LocationSheetState(
    val show: Boolean,
    val target: LocationTarget
)

data class UserInteractionState(
    val selection: SelectionState,
    val locationSheet: LocationSheetState
)

data class DataState(
    val serviceProbe: ServiceProbeState,
    val stations: List<StationEntity>,
    val journeysResult: JourneyResult
)

data class ServiceProbeState(
    val healthCheck: HealthCheckState,
    val stationsCheck: StationsCheckState,
    val journeysCheck: JourneysCheckState,
    val isCachingEnabled: Boolean
)

data class HomeData(
    val fromLocation: String = "",
    val toLocation: String = "",
    val dateLabel: String = "Today",
    val scheduleTimes: List<ScheduleTimeLineItemData> = emptyList(),
    val journeyResult: JourneyResult = JourneyResult.Loading,
    val infoText: HomeInfoText = HomeInfoText(),
    val healthCheck: HealthCheckState = HealthCheckState.Checking,
    val stationsCheck: StationsCheckState = StationsCheckState.Checking,
    val journeysCheck: JourneysCheckState = JourneysCheckState.Idle,
    val isProbeCachingEnabled: Boolean = true,
    val progress: ProgressCardData = ProgressCardData(),
    val locationSection: LocationSelectorBottomSheetData = LocationSelectorBottomSheetData(locations = locations),
    val showLocationSheet: Boolean = false,
)

enum class LocationTarget(val label: String) {
    FROM("From"),
    TO("To")
}

data class HomeInfoText(val title: String = "", val description: String = "")

sealed class HealthCheckState {
    data object Checking : HealthCheckState()
    data class Online(val asOf: String) : HealthCheckState()
    data class Offline(val reason: String) : HealthCheckState()
}

sealed class StationsCheckState {
    data object Checking : StationsCheckState()
    data class Loaded(val count: Int, val asOf: String) : StationsCheckState()
    data class Failed(val reason: String) : StationsCheckState()
}

sealed class JourneysCheckState {
    data object Idle : JourneysCheckState()
    data object Checking : JourneysCheckState()
    data class Loaded(val count: Int, val asOf: String) : JourneysCheckState()
    data class Failed(val reason: String) : JourneysCheckState()
}

sealed class HomeState {
    object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class HasData(val data: HomeData) : HomeState()
}

private fun String.toColor(): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }
}

private val locations = listOf(
    "Sandton",
    "Park",
    "Rosebank",
    "Marlboro",
    "Rhodesfield",
    "O.R. Tambo",
    "Midrand",
    "Centurion",
    "Pretoria",
    "Hatfield"
)
