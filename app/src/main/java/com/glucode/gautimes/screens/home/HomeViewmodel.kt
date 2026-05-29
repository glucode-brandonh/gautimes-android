package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ProgressCardData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.repository.ApiError
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.TrainTimesRepository
import com.glucode.gautimes.ui.theme.cartGray
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewmodel @Inject constructor(
    private val trainTimesRepository: TrainTimesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _fromLocation = MutableStateFlow("Sandton")
    private val _toLocation = MutableStateFlow("Hatfield")
    private val _selectedDate = MutableStateFlow(Calendar.getInstance().timeInMillis)
    private val _showLocationSheet = MutableStateFlow(false)
    private val _locationTarget = MutableStateFlow(LocationTarget.FROM)
    private val _healthCheck = MutableStateFlow<HealthCheckState>(HealthCheckState.Checking)
    private val _stationsCheck = MutableStateFlow<StationsCheckState>(StationsCheckState.Checking)
    private val _isProbeCachingEnabled = MutableStateFlow(true)

    private val stations = trainTimesRepository.getStationsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val serviceProbeState = combine(
        _healthCheck,
        _stationsCheck,
        _isProbeCachingEnabled
    ) { healthCheck, stationsCheck, isCachingEnabled ->
        ServiceProbeState(healthCheck, stationsCheck, isCachingEnabled)
    }

    private val selectionState = combine(
        _fromLocation,
        _toLocation,
        _selectedDate
    ) { from, to, date ->
        SelectionState(from, to, date)
    }

    private val locationSheetState = combine(
        _showLocationSheet,
        _locationTarget
    ) { show, target ->
        LocationSheetState(show, target)
    }

    val uiState: StateFlow<HomeState> = combine(
        _isLoading,
        selectionState,
        locationSheetState,
        serviceProbeState,
        stations
    ) { isLoading, selection, locationSheet, serviceProbe, stations ->
        if (isLoading) {
            HomeState.Loading
        } else {
            val stationNames = stations.map { it.name }.ifEmpty { locations }
            HomeState.HasData(
                data = HomeData(
                    fromLocation = selection.from,
                    toLocation = selection.to,
                    dateLabel = DateUtils.formatDateLabel(selection.dateMillis),
                    scheduleTimes = times,
                    infoText = HomeInfoText(
                        title = "Coming up next",
                        description = "Peak fares will be in-affect until 18:45 tonight"
                    ),
                    healthCheck = serviceProbe.healthCheck,
                    stationsCheck = serviceProbe.stationsCheck,
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
            refreshStations()
        }
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
                trainTimesRepository.getHealth(forceNetwork = shouldForceNetwork())) {
                is ApiResult.Success -> HealthCheckState.Online(result.value.meta.asOf)
                is ApiResult.Failure -> HealthCheckState.Offline(result.error.toDisplayMessage())
            }
        }
    }

    fun refreshStations() {
        viewModelScope.launch {
            _stationsCheck.value = StationsCheckState.Checking
            _stationsCheck.value = when (val result =
                trainTimesRepository.refreshStations(forceNetwork = shouldForceNetwork())) {
                is ApiResult.Success -> StationsCheckState.Loaded(
                    count = stations.value.size,
                    asOf = "Just now" // Simplified for now
                )

                is ApiResult.Failure -> StationsCheckState.Failed(result.error.toDisplayMessage())
            }
        }
    }

    fun toggleProbeCaching() {
        _isProbeCachingEnabled.value = !_isProbeCachingEnabled.value
        refreshHealth()
        refreshStations()
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

data class ServiceProbeState(
    val healthCheck: HealthCheckState,
    val stationsCheck: StationsCheckState,
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
