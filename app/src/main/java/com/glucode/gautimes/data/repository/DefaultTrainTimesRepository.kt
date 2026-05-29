package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.ApiEnvelopeDto
import com.glucode.gautimes.data.remote.dto.HealthDataDto
import com.glucode.gautimes.data.remote.dto.HealthMetaDto
import com.glucode.gautimes.data.remote.dto.JourneysDataDto
import com.glucode.gautimes.data.remote.dto.JourneysMetaDto
import com.glucode.gautimes.data.remote.dto.ProblemDetailDto
import com.glucode.gautimes.data.remote.dto.StationDto
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class DefaultTrainTimesRepository @Inject constructor(
    private val api: TrainTimesApi,
    private val stationDao: StationDao,
    private val json: Json
) : TrainTimesRepository {
    override suspend fun getHealth(
        forceNetwork: Boolean
    ): ApiResult<ApiEnvelopeDto<HealthDataDto, HealthMetaDto>> =
        execute { api.getHealth(cacheControl = cacheControl(forceNetwork)) }

    override fun getStationsStream(): Flow<List<StationEntity>> =
        stationDao.getStations()

    override suspend fun refreshStations(
        forceNetwork: Boolean
    ): ApiResult<Unit> {
        val result = execute { api.getStations(cacheControl = cacheControl(forceNetwork)) }
        return when (result) {
            is ApiResult.Success -> {
                val entities = result.value.data.stations.map { it.asEntity() }
                stationDao.insertStations(entities)
                ApiResult.Success(Unit, result.rateLimit)
            }
            is ApiResult.Failure -> ApiResult.Failure(result.error)
        }
    }

    private fun StationDto.asEntity() = StationEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude
    )

    override suspend fun getJourneys(
        from: String,
        to: String,
        after: String?,
        includePolylines: Boolean
    ): ApiResult<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>> =
        execute {
            api.getJourneys(
                from = from,
                to = to,
                after = after,
                include = if (includePolylines) "polylines" else null
            )
        }

    private suspend fun <T> execute(call: suspend () -> Response<T>): ApiResult<T> =
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

    private fun cacheControl(forceNetwork: Boolean): String? =
        if (forceNetwork) CACHE_CONTROL_NO_CACHE else null

    private companion object {
        const val CACHE_CONTROL_NO_CACHE = "no-cache"
    }
}
