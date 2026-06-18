package com.glucode.gautimes.screens.home.ui.debug

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

data class DebugUiState(
    val healthCheck: HealthCheckState = HealthCheckState.Checking,
    val stationsCheck: StationsCheckState = StationsCheckState.Checking,
    val journeysCheck: JourneysCheckState = JourneysCheckState.Idle,
    val isProbeCachingEnabled: Boolean = true,
    val currentLat: Double? = null,
    val currentLong: Double? = null
)

sealed class DebugAction {
    data object RefreshHealth : DebugAction()
    data object RefreshStations : DebugAction()
    data class RefreshJourneys(val force: Boolean = false) : DebugAction()
    data object RefreshLocation : DebugAction()
    data object ToggleProbeCaching : DebugAction()
    data object TestNotification : DebugAction()
}

sealed class DebugEffect {
    data class ShowError(val message: String) : DebugEffect()
    data object RunNotificationTest : DebugEffect()
}
