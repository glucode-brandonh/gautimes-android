package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
