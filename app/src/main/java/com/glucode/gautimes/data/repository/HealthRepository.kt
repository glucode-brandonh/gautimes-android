package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.ApiEnvelopeDto
import com.glucode.gautimes.data.remote.dto.HealthDataDto
import com.glucode.gautimes.data.remote.dto.HealthMetaDto
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface HealthRepository {
    suspend fun getHealth(
        forceNetwork: Boolean = false
    ): ApiResult<ApiEnvelopeDto<HealthDataDto, HealthMetaDto>>
}

class DefaultHealthRepository @Inject constructor(
    private val api: TrainTimesApi,
    json: Json
) : BaseRepository(json), HealthRepository {
    override suspend fun getHealth(
        forceNetwork: Boolean
    ): ApiResult<ApiEnvelopeDto<HealthDataDto, HealthMetaDto>> =
        execute { api.getHealth(cacheControl = cacheControl(forceNetwork)) }
}
