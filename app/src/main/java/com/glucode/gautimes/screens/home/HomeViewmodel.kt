package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ProgressCardData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.repository.*
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.ui.theme.cartGray
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val journeys = selectionState.flatMapLatest { selection ->
        journeysRepository.getJourneysStream(selection.from, selection.to)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val serviceProbeState = combine(
        _healthCheck,
        _stationsCheck,
        _journeysCheck,
        _isProbeCachingEnabled
    ) { healthCheck, stationsCheck, journeysCheck, isCachingEnabled ->
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
    ) { serviceProbe, stations, journeys ->
        DataState(serviceProbe, stations, journeys)
    }

    val uiState: StateFlow<HomeState> = combine(
        _isLoading,
        userInteractionState,
        dataState
    ) { isLoading, userInteraction, data ->
        if (isLoading) {
            HomeState.Loading
        } else {
            val selection = userInteraction.selection
            val locationSheet = userInteraction.locationSheet
            val serviceProbe = data.serviceProbe
            val stations = data.stations
            val journeys = data.journeys

            val stationNames = stations.map { it.name }.ifEmpty { locations }
            val scheduleTimes = journeys.map { journey ->
                val firstLeg = journey.legs.firstOrNull()
                ScheduleTimeLineItemData(
                    timeText = formatTime(journey.journey.departureTime),
                    cartColor = firstLeg?.lineColour?.toColor() ?: cartYellow,
                    cartNumber = firstLeg?.carriages ?: 4
                )
            }.ifEmpty { times }

            HomeState.HasData(
                data = HomeData(
                    fromLocation = selection.from,
                    toLocation = selection.to,
                    dateLabel = DateUtils.formatDateLabel(selection.dateMillis),
                    scheduleTimes = scheduleTimes,
                    infoText = HomeInfoText(
                        title = "Coming up next",
                        description = "Peak fares will be in-affect until 18:45 tonight"
                    ),
                    healthCheck = serviceProbe.healthCheck,
                    stationsCheck = serviceProbe.stationsCheck,
                    journeysCheck = serviceProbe.journeysCheck,
                    isProbeCachingEnabled = serviceProbe.isCachingEnabled,
                    progress = ProgressCardData(
                        progressTitleTime = "20 min",
                        progressDescription = "until arrive"
                    ),
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
        viewModelScope.launch {
            _journeysCheck.value = JourneysCheckState.Checking
            _journeysCheck.value = when (val result =
                journeysRepository.getJourneys(
                    from = _fromLocation.value,
                    to = _toLocation.value,
                    forceRefresh = shouldForceNetwork()
                )) {
                is ApiResult.Success -> JourneysCheckState.Loaded(
                    count = result.value.data.journeys.size,
                    asOf = result.value.meta.asOf
                )

                is ApiResult.Failure -> JourneysCheckState.Failed(result.error.toDisplayMessage())
            }
        }
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
        refreshJourneys()
    }

    fun updateToLocation(location: String) {
        _toLocation.value = location
        refreshJourneys()
    }

    fun flipLocations() {
        val temp = _fromLocation.value
        _fromLocation.value = _toLocation.value
        _toLocation.value = temp
        refreshJourneys()
    }

    fun updateDate(millis: Long?) {
        millis?.let {
            _selectedDate.value = it
        }
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
    val journeys: List<JourneyWithLegs>
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

private fun ApiError.toDisplayMessage(): String =
    when (this) {
        is ApiError.Problem -> problem.detail
        is ApiError.Http -> message.ifBlank { "HTTP $code" }
        is ApiError.Network -> message
        is ApiError.Serialization -> message
        ApiError.EmptyBody -> "The health response was empty."
    }

private fun formatTime(isoTime: String): String {
    return try {
        // Simple extraction of HH:mm from "YYYY-MM-DDTHH:mm:ss" or similar
        val timePart = isoTime.substringAfter('T').substringBefore(':')
        val minutePart = isoTime.substringAfter('T').substringAfter(':').substringBefore(':')
        "$timePart:$minutePart"
    } catch (e: Exception) {
        "00:00"
    }
}

private fun String.toColor(): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }
}

private val times = listOf(
    ScheduleTimeLineItemData(timeText = "06:15", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "07:00", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "08:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "09:45", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "11:00", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "12:15", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "13:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "14:45", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "16:00", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "17:15", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "18:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "19:45", cartColor = cartGray, cartNumber = 8)
)

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
