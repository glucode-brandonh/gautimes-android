package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.remote.dto.ProblemDetailDto
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

abstract class BaseRepository(private val json: Json) {

    protected suspend fun <T> execute(call: suspend () -> Response<T>): ApiResult<T> =
        try {
            val response = call()
            val rateLimit = response.headers().rateLimitInfo()
            val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                ?: response.headers()["X-RateLimit-After"]?.toIntOrNull()

            if (response.isSuccessful) {
                val body = response.body()
                if (body == null) {
                    ApiResult.Failure(ApiError.EmptyBody)
                } else {
                    ApiResult.Success(body, rateLimit)
                }
            } else {
                ApiResult.Failure(response.toApiError(rateLimit, retryAfter))
            }
        } catch (error: SerializationException) {
            ApiResult.Failure(ApiError.Serialization(error.message ?: "Unable to decode API response."))
        } catch (error: IOException) {
            ApiResult.Failure(ApiError.Network(error.message ?: "Network request failed."))
        }

    private fun <T> Response<T>.toApiError(
        rateLimit: RateLimitInfo,
        retryAfter: Int?
    ): ApiError {
        val problem = errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { rawBody ->
            runCatching { json.decodeFromString<ProblemDetailDto>(rawBody) }.getOrNull()
        }

        return if (problem != null) {
            ApiError.Problem(problem, rateLimit, retryAfter)
        } else {
            ApiError.Http(code(), message(), rateLimit, retryAfter)
        }
    }

    private fun okhttp3.Headers.rateLimitInfo(): RateLimitInfo =
        RateLimitInfo(
            limit = this["X-RateLimit-Limit"]?.toIntOrNull(),
            remaining = this["X-RateLimit-Remaining"]?.toIntOrNull(),
            resetSeconds = this["X-RateLimit-Reset"]?.toIntOrNull()
        )

    protected fun cacheControl(forceNetwork: Boolean): String? =
        if (forceNetwork) CACHE_CONTROL_NO_CACHE else null

    companion object {
        const val CACHE_CONTROL_NO_CACHE = "no-cache"
    }
}
