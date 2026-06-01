package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.dao.JourneyDao
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
 import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

interface JourneysRepository {
    fun getJourneys(
        from: String,
        to: String,
        forceRefresh: Boolean = false
    ): Flow<JourneyResult>
}

class DefaultJourneysRepository @Inject constructor(
    private val api: TrainTimesApi,
    private val journeyDao: JourneyDao,
    json: Json
) : BaseRepository(json), JourneysRepository {

    override fun getJourneys(from: String, to: String, forceRefresh: Boolean): Flow<JourneyResult> = channelFlow {
        send(JourneyResult.Loading)

        val metadata = journeyDao.getMetadataForRoute(from, to)
        val isFresh = metadata != null && isCacheFresh(metadata.lastUpdatedMillis)

        if (forceRefresh || !isFresh) {
            launch {
                val result = execute {
                    api.getJourneys(
                        from = from,
                        to = to,
                        after = null,
                        include = null
                    )
                }
                if (result is ApiResult.Failure) {
                    send(JourneyResult.Error(result.error.toDisplayMessage()))
                } else if (result is ApiResult.Success) {
                    saveToCache(from, to, result.value.data)
                }
            }
        }

        journeyDao.getJourneysStreamForRoute(from, to).collect { journeys ->
            if (journeys.isNotEmpty()) {
                send(JourneyResult.Success(journeys))
            } else if (isFresh) {
                send(JourneyResult.Success(emptyList()))
            }
            // If empty and not fresh, we wait for the network fetch launched above to update the DB
        }
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
