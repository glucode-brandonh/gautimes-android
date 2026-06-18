package com.glucode.gautimes.components.sharing

data class ShareInfo(
    val from: String,
    val to: String,
    val departureTime: String,
    val arrivalTime: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timeUntilDeparture: String? = null
)
