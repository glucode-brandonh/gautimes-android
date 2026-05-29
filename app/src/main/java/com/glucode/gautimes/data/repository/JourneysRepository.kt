package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.dao.JourneyDao
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

interface JourneysRepository {
    suspend fun getJourneys(
        from: String,
        to: String,
        after: String? = null,
        includePolylines: Boolean = false,
        forceRefresh: Boolean = false
    ): ApiResult<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>>
}

class DefaultJourneysRepository @Inject constructor(
    private val api: TrainTimesApi,
    private val journeyDao: JourneyDao,
    json: Json
) : BaseRepository(json), JourneysRepository {

    override suspend fun getJourneys(
        from: String,
        to: String,
        after: String?,
        includePolylines: Boolean,
        forceRefresh: Boolean
    ): ApiResult<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>> {
        if (!forceRefresh) {
            val metadata = journeyDao.getMetadataForRoute(from, to)
            if (metadata != null && isCacheFresh(metadata.lastUpdatedMillis)) {
                val cachedJourneys = journeyDao.getJourneysForRoute(from, to)
                if (cachedJourneys.isNotEmpty()) {
                    return ApiResult.Success(
                        value = cachedJourneys.toEnvelopeDto(from, to),
                        rateLimit = RateLimitInfo(null, null, null)
                    )
                }
            }
        }

        val result = execute {
            api.getJourneys(
                from = from,
                to = to,
                after = after,
                include = if (includePolylines) "polylines" else null
            )
        }

        if (result is ApiResult.Success) {
            saveToCache(from, to, result.value.data)
        }

        return result
    }

    private fun isCacheFresh(lastUpdatedMillis: Long): Boolean {
        val age = System.currentTimeMillis() - lastUpdatedMillis
        return age < CACHE_TIMEOUT.inWholeMilliseconds
    }

    private suspend fun saveToCache(from: String, to: String, data: JourneysDataDto) {
        val journeyEntities = data.journeys.map { it.asEntity(from, to) }
        val legEntities = data.journeys.flatMap { journey ->
            journey.legs.map { it.asEntity(journey.id) }
        }
        val metadata = JourneyQueryMetadataEntity(from, to, System.currentTimeMillis())

        journeyDao.updateJourneysForRoute(from, to, journeyEntities, legEntities, metadata)
    }

    private fun List<JourneyWithLegs>.toEnvelopeDto(from: String, to: String): ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto> {
        val journeys = map { it.asDto() }
        return ApiEnvelopeDto(
            data = JourneysDataDto(journeys),
            meta = JourneysMetaDto(
                count = journeys.size,
                from = from,
                to = to,
                asOf = "Cached", // Or format the timestamp
                cache = CacheDto("HIT", "", 0, 0)
            )
        )
    }

    private fun JourneyWithLegs.asDto() = JourneyDto(
        id = journey.id,
        departureTime = journey.departureTime,
        arrivalTime = journey.arrivalTime,
        durationSeconds = journey.durationSeconds,
        distanceMetres = journey.distanceMetres,
        totalFareZar = journey.totalFareZar,
        parkingCostZar = journey.parkingCostZar,
        legs = legs.map { it.asDto() }
    )

    private fun JourneyLegEntity.asDto() = JourneyLegDto(
        id = id,
        mode = mode,
        lineName = lineName,
        lineColour = lineColour,
        departureStop = departureStop,
        arrivalStop = arrivalStop,
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        headsign = headsign,
        carriages = carriages,
        fareAmountZar = fareAmountZar,
        fareIsPeak = fareIsPeak,
        fareProduct = fareProduct,
        tripId = tripId,
        polyline = emptyList() // Ignored as per request
    )

    private fun JourneyDto.asEntity(from: String, to: String) = JourneyEntity(
        id = id,
        fromStationId = from,
        toStationId = to,
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        totalFareZar = totalFareZar,
        parkingCostZar = parkingCostZar
    )

    private fun JourneyLegDto.asEntity(journeyId: String) = JourneyLegEntity(
        id = id,
        journeyId = journeyId,
        mode = mode,
        lineName = lineName,
        lineColour = lineColour,
        departureStop = departureStop,
        arrivalStop = arrivalStop,
        departureTime = departureTime,
        arrivalTime = arrivalTime,
        durationSeconds = durationSeconds,
        distanceMetres = distanceMetres,
        headsign = headsign,
        carriages = carriages,
        fareAmountZar = fareAmountZar,
        fareIsPeak = fareIsPeak,
        fareProduct = fareProduct,
        tripId = tripId
    )

    companion object {
        private val CACHE_TIMEOUT = 15.minutes
    }
}
