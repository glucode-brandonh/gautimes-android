package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.remote.TrainTimesApi
import com.glucode.gautimes.data.remote.dto.ApiEnvelopeDto
import com.glucode.gautimes.data.remote.dto.JourneysDataDto
import com.glucode.gautimes.data.remote.dto.JourneysMetaDto
import kotlinx.serialization.json.Json
import javax.inject.Inject

interface JourneysRepository {
    suspend fun getJourneys(
        from: String,
        to: String,
        after: String? = null,
        includePolylines: Boolean = false
    ): ApiResult<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>>
}

class DefaultJourneysRepository @Inject constructor(
    private val api: TrainTimesApi,
    json: Json
) : BaseRepository(json), JourneysRepository {
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
}
