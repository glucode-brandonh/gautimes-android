package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JourneysDataDto(
    val journeys: List<JourneyDto>
)

@Serializable
data class JourneyDto(
    val id: String,
    @SerialName("departure_time")
    val departureTime: String,
    @SerialName("arrival_time")
    val arrivalTime: String,
    @SerialName("duration_seconds")
    val durationSeconds: Int,
    @SerialName("distance_metres")
    val distanceMetres: Int,
    @SerialName("total_fare_zar")
    val totalFareZar: Double,
    @SerialName("parking_cost_zar")
    val parkingCostZar: Double? = null,
    @SerialName("intermediate_stations")
    val intermediateStations: List<JourneyIntermediateStationDto> = emptyList(),
    val legs: List<JourneyLegDto>
)

@Serializable
data class JourneyIntermediateStationDto(
    val id: String,
    val name: String,
    @SerialName("arrival_time")
    val arrivalTime: String?,
    @SerialName("duration_seconds")
    val durationSeconds: Int
)

@Serializable
data class JourneyLegDto(
    val id: String,
    val mode: String,
    @SerialName("line_name")
    val lineName: String? = null,
    @SerialName("line_colour")
    val lineColour: String? = null,
    @SerialName("departure_stop")
    val departureStop: String,
    @SerialName("arrival_stop")
    val arrivalStop: String,
    @SerialName("departure_time")
    val departureTime: String,
    @SerialName("arrival_time")
    val arrivalTime: String,
    @SerialName("duration_seconds")
    val durationSeconds: Int,
    @SerialName("distance_metres")
    val distanceMetres: Int,
    val headsign: String? = null,
    val carriages: Int,
    @SerialName("fare_amount_zar")
    val fareAmountZar: Double,
    @SerialName("fare_is_peak")
    val fareIsPeak: Boolean,
    @SerialName("fare_product")
    val fareProduct: String? = null,
    @SerialName("trip_id")
    val tripId: String? = null,
    val polyline: List<List<Double>> = emptyList()
)

@Serializable
data class JourneysMetaDto(
    val count: Int,
    val from: String,
    val to: String,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("as_of")
    val asOf: String,
    val includes: List<String> = emptyList(),
    val cache: CacheDto
)
