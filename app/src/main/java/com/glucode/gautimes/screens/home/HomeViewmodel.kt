package com.glucode.gautimes.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.components.LocationSelectorBottomSheetNew
import com.glucode.gautimes.components.ProgressCardData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.ui.theme.cartGray
import com.glucode.gautimes.ui.theme.cartYellow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewmodel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<HomeState>(HomeState.Loading)
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _fromLocation = MutableStateFlow("Sandton")
    val fromLocation: StateFlow<String> = _fromLocation.asStateFlow()

    private val _toLocation = MutableStateFlow("Hatfield")
    val toLocation: StateFlow<String> = _toLocation.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeState.Loading
            delay(2000) // Simulate network delay
            _uiState.value = HomeState.HasData(
                data = HomeData(
                    fromLocation = _fromLocation.value,
                    toLocation = _toLocation.value,
                    scheduleTimes = times,
                    infoText = HomeInfoText(
                        title = "Coming up next",
                        description = "Peak fares will be in-affect until 18:45 tonight"
                    ),
                    progress = ProgressCardData(
                        progressTitleTime = "20 min",
                        progressDescription = "until arrive"
                    ),
                )
            )
        }
    }

    fun updateFromLocation(location: String) {
        _fromLocation.value = location
        updateHomeData { it.copy(fromLocation = location) }
    }

    fun updateToLocation(location: String) {
        _toLocation.value = location
        updateHomeData { it.copy(toLocation = location) }
    }

    private fun updateHomeData(update: (HomeData) -> HomeData) {
        val currentState = _uiState.value
        if (currentState is HomeState.HasData) {
            _uiState.value = HomeState.HasData(update(currentState.data))
        }
    }

    fun buildLocationSelector(
        target: LocationTarget,
        fromLocation: String,
        toLocation: String
    ): LocationSelectorBottomSheetNew {
        val selected = if (target == LocationTarget.FROM) fromLocation else toLocation
        val disabled = if (target == LocationTarget.FROM) toLocation else fromLocation
        return LocationSelectorBottomSheetNew(
            locations = locations,
            selectedLocation = selected,
            disabledLocation = disabled,
            locationTarget = target
        )
    }

    fun toggleLocationSheet(show: Boolean, target: LocationTarget) {
        val currentState = _uiState.value
        if (currentState is HomeState.HasData) {
            _uiState.value = currentState.copy(
                data = currentState.data.copy(
                    showLocationSheet = show,
                    locationSection = buildLocationSelector(
                        target = target,
                        fromLocation = _fromLocation.value,
                        toLocation = _toLocation.value
                    )
                )
            )
        }
    }
}

data class HomeData(
    val fromLocation: String = "",
    val toLocation: String = "",
    val scheduleTimes: List<ScheduleTimeLineItemData> = emptyList(),
    val infoText: HomeInfoText = HomeInfoText(),
    val progress: ProgressCardData = ProgressCardData(),
    val locationSection: LocationSelectorBottomSheetNew = LocationSelectorBottomSheetNew(locations = locations),
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
