package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelopeDto<Data, Meta>(
    val data: Data,
    val meta: Meta
)

@Serializable
data class HealthDataDto(
    val status: String
)

@Serializable
data class HealthMetaDto(
    @SerialName("as_of")
    val asOf: String
)

@Serializable
data class StationsDataDto(
    val stations: List<StationDto>
)

@Serializable
data class StationDto(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val modes: List<String>
)

@Serializable
data class StationsMetaDto(
    val count: Int,
    @SerialName("as_of")
    val asOf: String,
    val cache: CacheDto
)

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
    val legs: List<JourneyLegDto>
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

@Serializable
data class CacheDto(
    val status: String,
    @SerialName("cached_at")
    val cachedAt: String,
    @SerialName("age_seconds")
    val ageSeconds: Int,
    @SerialName("ttl_seconds")
    val ttlSeconds: Int
)

@Serializable
data class ProblemDetailDto(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String? = null
)
