package com.glucode.gautimes.data.remote

import com.glucode.gautimes.data.remote.dto.ApiEnvelopeDto
import com.glucode.gautimes.data.remote.dto.HealthDataDto
import com.glucode.gautimes.data.remote.dto.HealthMetaDto
import com.glucode.gautimes.data.remote.dto.JourneysDataDto
import com.glucode.gautimes.data.remote.dto.JourneysMetaDto
import com.glucode.gautimes.data.remote.dto.StationsDataDto
import com.glucode.gautimes.data.remote.dto.StationsMetaDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TrainTimesApi {
    @GET("health")
    suspend fun getHealth(
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ApiEnvelopeDto<HealthDataDto, HealthMetaDto>>

    @GET("v1/stations")
    suspend fun getStations(
        @Header("Cache-Control") cacheControl: String? = null
    ): Response<ApiEnvelopeDto<StationsDataDto, StationsMetaDto>>

    @GET("v1/journeys")
    suspend fun getJourneys(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("after") after: String? = null,
        @Query("include") include: String? = null
    ): Response<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>>
}
