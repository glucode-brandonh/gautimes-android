package com.glucode.gautimes.screens.home

import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.repository.JourneyResult
import java.util.Calendar

sealed class HomeState {
    data object Loading : HomeState()
    data class Error(val message: String) : HomeState()
    data class HasData(val data: HomeData) : HomeState()
}

data class HomeData(
    val fromLocation: String = "",
    val toLocation: String = "",
    val dateLabel: String = "Today",
    val scheduleTimes: List<ScheduleTimeLineItemData> = emptyList(),
    val journeyResult: JourneyResult = JourneyResult.Loading,
    val isRefreshing: Boolean = false,
    val isFetchingMore: Boolean = false,
    val nextCursor: String? = null,
    val infoText: HomeInfoText = HomeInfoText(),
    val isGrantingPermission: Boolean = false,
    val healthCheck: HealthCheckState = HealthCheckState.Checking,
    val stationsCheck: StationsCheckState = StationsCheckState.Checking,
    val journeysCheck: JourneysCheckState = JourneysCheckState.Idle,
    val isProbeCachingEnabled: Boolean = true,
    val isFromNear: Boolean = true,
    val currentLat: Double? = null,
    val currentLong: Double? = null,
    val progress: DepartureTimeCardData = DepartureTimeCardData(),
    val locationSection: LocationSelectorBottomSheetData = LocationSelectorBottomSheetData(),
    val showLocationSheet: Boolean = false,
    val showLocationPermissionCard: Boolean = false,
)

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

enum class LocationTarget(val label: String) {
    FROM("From"),
    TO("To")
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFetchingMore: Boolean = false,
    val fromLocation: String = "Sandton",
    val toLocation: String = "Hatfield",
    val selectedDate: Long = Calendar.getInstance().timeInMillis,
    val showLocationSheet: Boolean = false,
    val isGrantingPermission: Boolean = false,
    val locationTarget: LocationTarget = LocationTarget.FROM,
    val healthCheck: HealthCheckState = HealthCheckState.Checking,
    val stationsCheck: StationsCheckState = StationsCheckState.Checking,
    val isProbeCachingEnabled: Boolean = true
)

sealed class HomeAction {
    data object Refresh : HomeAction()
    data class UpdateFromLocation(val location: String) : HomeAction()
    data class UpdateToLocation(val location: String) : HomeAction()
    data object FlipLocations : HomeAction()
    data class UpdateDate(val millis: Long?) : HomeAction()
    data class ToggleLocationSheet(val show: Boolean, val target: LocationTarget) : HomeAction()
    data object ToggleProbeCaching : HomeAction()
    data object RefreshHealth : HomeAction()
    data object RefreshStations : HomeAction()
    data class RefreshJourneys(val force: Boolean = false) : HomeAction()
    data object RefreshLocation : HomeAction()
    data class LoadMore(val cursor: String) : HomeAction()
    data object DismissLocationPermissionCard : HomeAction()
    data class SetGrantingPermission(val isGranting: Boolean) : HomeAction()
}

sealed class HomeEffect {
    data class ShowError(val message: String) : HomeEffect()
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
    val journeysResult: JourneyResult,
    val currentLocation: android.location.Location?
)

data class ServiceProbeState(
    val healthCheck: HealthCheckState,
    val stationsCheck: StationsCheckState,
    val journeysCheck: JourneysCheckState,
    val isCachingEnabled: Boolean
)
