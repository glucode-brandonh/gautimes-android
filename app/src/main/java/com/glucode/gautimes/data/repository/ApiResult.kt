package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.remote.dto.ProblemDetailDto

sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
        val rateLimit: RateLimitInfo
    ) : ApiResult<T>

    data class Failure(
        val error: ApiError
    ) : ApiResult<Nothing>
}

sealed interface ApiError {
    data class Problem(
        val problem: ProblemDetailDto,
        val rateLimit: RateLimitInfo,
        val retryAfterSeconds: Int?
    ) : ApiError

    data class Http(
        val code: Int,
        val message: String,
        val rateLimit: RateLimitInfo,
        val retryAfterSeconds: Int?
    ) : ApiError

    data class Network(
        val message: String
    ) : ApiError

    data class Serialization(
        val message: String
    ) : ApiError

    data object EmptyBody : ApiError

    fun toDisplayMessage(): String =
        when (this) {
            is Problem -> problem.detail
            is Http -> message.ifBlank { "HTTP $code" }
            is Network -> message
            is Serialization -> message
            EmptyBody -> "The response was empty."
        }
}

data class RateLimitInfo(
    val limit: Int?,
    val remaining: Int?,
    val resetSeconds: Int?
)
