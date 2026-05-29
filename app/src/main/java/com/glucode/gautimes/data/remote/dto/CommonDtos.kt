package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelopeDto<Data, Meta>(
    val data: Data,
    val meta: Meta
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
