package com.glucode.gautimes.widget

sealed class DepartureWidgetState {
    data object Loading : DepartureWidgetState()

    data class Active(
        val routeLabel: String,
        val minutesUntilDeparture: Long,
        val departureTime: String,
        val arrivalTime: String,
        val secondaryDepartures: List<String>
    ) : DepartureWidgetState()

    data class Empty(
        val routeLabel: String,
        val message: String
    ) : DepartureWidgetState()

    data class RefreshFailed(
        val routeLabel: String,
        val message: String
    ) : DepartureWidgetState()
}
