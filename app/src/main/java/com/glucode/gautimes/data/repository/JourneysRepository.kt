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

    suspend fun loadMore(from: String, to: String, cursor: String): ApiResult<Unit>
}

class DefaultJourneysRepository @Inject constructor(
    private val api: TrainTimesApi,
    private val journeyDao: JourneyDao,
    json: Json
) : BaseRepository(json), JourneysRepository {

    override fun getJourneys(from: String, to: String, forceRefresh: Boolean): Flow<JourneyResult> =
        channelFlow {
            send(JourneyResult.Loading)

            val initialMetadata = journeyDao.getMetadataForRoute(from, to)
            val isFresh = initialMetadata != null && isCacheFresh(initialMetadata.lastUpdatedMillis)

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
                        printFirstAndLastSchedule(
                            source = "get journeys",
                            result.value.data.journeys
                        )
                        saveToCache(from, to, result.value.data, result.value.meta.nextCursor, shouldWipe = !isFresh)
                    }
                }
            }

            journeyDao.getJourneysStreamForRoute(from, to).collect { journeys ->
                val metadata = journeyDao.getMetadataForRoute(from, to)
                if (journeys.isNotEmpty()) {
                    send(JourneyResult.Success(journeys, nextCursor = metadata?.nextCursor))
                } else if (isFresh) {
                    send(JourneyResult.Success(emptyList(), nextCursor = metadata?.nextCursor))
                }
                // If empty and not fresh, we wait for the network fetch launched above to update the DB
            }
        }

    override suspend fun loadMore(from: String, to: String, cursor: String): ApiResult<Unit> {
        val result = execute {
            api.getJourneys(
                from = from,
                to = to,
                after = cursor,
                include = null
            )
        }

        return when (result) {
            is ApiResult.Success -> {
                printFirstAndLastSchedule(source = "load more", result.value.data.journeys)
                appendToCache(from, to, result.value.data, result.value.meta.nextCursor)
                ApiResult.Success(Unit, result.rateLimit)
            }

            is ApiResult.Failure -> ApiResult.Failure(result.error)
        }
    }

    private fun isCacheFresh(lastUpdatedMillis: Long): Boolean {
        val age = System.currentTimeMillis() - lastUpdatedMillis
        return age < CACHE_TIMEOUT.inWholeMilliseconds
    }

    private suspend fun saveToCache(
        from: String,
        to: String,
        data: JourneysDataDto,
        nextCursor: String?,
        shouldWipe: Boolean
    ) {
        val journeyEntities = data.journeys.map { it.asEntity(from, to) }
        val legEntities = data.journeys.flatMap { journey ->
            journey.legs.map { it.asEntity(journey.id) }
        }
        val metadata = JourneyQueryMetadataEntity(from, to, System.currentTimeMillis(), nextCursor)

        if (shouldWipe) {
            journeyDao.updateJourneysForRoute(from, to, journeyEntities, legEntities, metadata)
        } else {
            journeyDao.appendJourneysForRoute(journeyEntities, legEntities, metadata)
        }
    }

    private suspend fun appendToCache(
        from: String,
        to: String,
        data: JourneysDataDto,
        nextCursor: String?
    ) {
        val journeyEntities = data.journeys.map { it.asEntity(from, to) }
        val legEntities = data.journeys.flatMap { journey ->
            journey.legs.map { it.asEntity(journey.id) }
        }
        val metadata = JourneyQueryMetadataEntity(from, to, System.currentTimeMillis(), nextCursor)

        journeyDao.appendJourneysForRoute(journeyEntities, legEntities, metadata)
    }

    fun printFirstAndLastSchedule(source: String, journeys: List<JourneyDto>) {
        println("GauDebug Schedule Batch $source First Departure " + journeys.first().departureTime)
        println("GauDebug Schedule Batch $source Last Departure " + journeys.last().departureTime)
        println("GauDebug Schedule Batch $source Last Departure ---------")
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
