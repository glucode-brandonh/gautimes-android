package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthDataDto(
    val status: String
)

@Serializable
data class HealthMetaDto(
    @SerialName("as_of")
    val asOf: String
)
