package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ProgressCardData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.ui.theme.cartGray
import com.glucode.gautimes.ui.theme.cartYellow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class HomeViewmodel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _fromLocation = MutableStateFlow("Sandton")
    private val _toLocation = MutableStateFlow("Hatfield")
    private val _showLocationSheet = MutableStateFlow(false)
    private val _locationTarget = MutableStateFlow(LocationTarget.FROM)

    val uiState: StateFlow<HomeState> = combine(
        _isLoading,
        _fromLocation,
        _toLocation,
        _showLocationSheet,
        _locationTarget
    ) { isLoading, from, to, showSheet, target ->
        if (isLoading) {
            HomeState.Loading
        } else {
            HomeState.HasData(
                data = HomeData(
                    fromLocation = from,
                    toLocation = to,
                    scheduleTimes = times,
                    infoText = HomeInfoText(
                        title = "Coming up next",
                        description = "Peak fares will be in-affect until 18:45 tonight"
                    ),
                    progress = ProgressCardData(
                        progressTitleTime = "20 min",
                        progressDescription = "until arrive"
                    ),
                    showLocationSheet = showSheet,
                    locationSection = buildLocationSelector(target, from, to)
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
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(2.seconds) // Simulate network delay
            _isLoading.value = false
        }
    }

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

    fun buildLocationSelector(
        target: LocationTarget,
        fromLocation: String,
        toLocation: String
    ): LocationSelectorBottomSheetData {
        val selected = if (target == LocationTarget.FROM) fromLocation else toLocation
        val disabled = if (target == LocationTarget.FROM) toLocation else fromLocation
        return LocationSelectorBottomSheetData(
            locations = locations,
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

data class HomeData(
    val fromLocation: String = "",
    val toLocation: String = "",
    val scheduleTimes: List<ScheduleTimeLineItemData> = emptyList(),
    val infoText: HomeInfoText = HomeInfoText(),
    val progress: ProgressCardData = ProgressCardData(),
    val locationSection: LocationSelectorBottomSheetData = LocationSelectorBottomSheetData(locations = locations),
    val showLocationSheet: Boolean = false,
)

enum class LocationTarget(val label: String) {
    FROM("From"),
    TO("To")
}

data class HomeInfoText(val title: String = "", val description: String = "")

sealed class HomeState {
    object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class HasData(val data: HomeData) : HomeState()
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
