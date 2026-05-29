package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.StationDto
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface StationsRepository {
    fun getStationsStream(): Flow<List<StationEntity>>

    suspend fun refreshStations(
        forceNetwork: Boolean = false
    ): ApiResult<Unit>
}

class DefaultStationsRepository @Inject constructor(
    private val api: TrainTimesApi,
    private val stationDao: StationDao,
    json: Json
) : BaseRepository(json), StationsRepository {
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
}
