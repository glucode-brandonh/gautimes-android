package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.remote.ApiKeyInterceptor
import com.glucode.gautimes.data.remote.TrainTimesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import kotlin.io.path.createTempDirectory

@OptIn(ExperimentalSerializationApi::class)
class DefaultTrainTimesRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: TrainTimesRepository
    private lateinit var cacheDirectory: File

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        cacheDirectory = createTempDirectory(prefix = "train-times-http-cache").toFile()
        repository = createRepository(apiKey = "test-key")
    }

    @After
    fun tearDown() {
        server.shutdown()
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun healthRequestDoesNotSendApiKey() = runBlocking {
        server.enqueue(jsonResponse(healthBody()))

        val result = repository.getHealth()
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("/health", request.path)
        assertNull(request.getHeader("X-Api-Key"))
    }

    @Test
    fun stationsRequestSendsApiKey() = runBlocking {
        server.enqueue(jsonResponse(stationsBody()))

        val result = repository.getStations()
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("/v1/stations", request.path)
        assertEquals("test-key", request.getHeader("X-Api-Key"))
        assertNull(request.getHeader("Cache-Control"))
    }

    @Test
    fun forcedStationsRequestSendsNoCacheHeaderAndApiKey() = runBlocking {
        server.enqueue(jsonResponse(stationsBody()))

        val result = repository.getStations(forceNetwork = true)
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals("/v1/stations", request.path)
        assertEquals("test-key", request.getHeader("X-Api-Key"))
        assertEquals("no-cache", request.getHeader("Cache-Control"))
    }

    @Test
    fun journeysRequestPreservesOpaqueCursor() = runBlocking {
        server.enqueue(jsonResponse(journeysBody()))

        val result = repository.getJourneys(
            from = "rosebank",
            to = "marlboro",
            after = "MjAyNi0wNS0yOFQwNDoxMzozNlo",
            includePolylines = true
        )
        val request = server.takeRequest()

        assertTrue(result is ApiResult.Success)
        assertEquals(
            "/v1/journeys?from=rosebank&to=marlboro&after=MjAyNi0wNS0yOFQwNDoxMzozNlo&include=polylines",
            request.path
        )
        assertEquals("test-key", request.getHeader("X-Api-Key"))
    }

    @Test
    fun mapsUnauthorizedProblemResponse() = runBlocking {
        server.enqueue(problemResponse(code = 401, type = "unauthorized"))

        val result = repository.getStations()

        assertTrue(result is ApiResult.Failure)
        val failure = result as ApiResult.Failure
        assertTrue(failure.error is ApiError.Problem)
        val error = failure.error as ApiError.Problem
        assertEquals(401, error.problem.status)
        assertEquals("Unauthorized", error.problem.title)
    }

    @Test
    fun mapsRateLimitProblemResponseWithRetryHeaders() = runBlocking {
        server.enqueue(
            problemResponse(code = 429, type = "too_many_requests")
                .addHeader("Retry-After", "24")
                .addHeader("X-RateLimit-Limit", "60")
                .addHeader("X-RateLimit-Remaining", "0")
                .addHeader("X-RateLimit-Reset", "24")
        )

        val result = repository.getStations()

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error as ApiError.Problem
        assertEquals(429, error.problem.status)
        assertEquals(24, error.retryAfterSeconds)
        assertEquals(60, error.rateLimit.limit)
        assertEquals(0, error.rateLimit.remaining)
        assertEquals(24, error.rateLimit.resetSeconds)
    }

    @Test
    fun mapsUpstreamUnavailableProblemResponse() = runBlocking {
        server.enqueue(problemResponse(code = 502, type = "upstream_unavailable"))

        val result = repository.getStations()

        assertTrue(result is ApiResult.Failure)
        val error = (result as ApiResult.Failure).error as ApiError.Problem
        assertEquals(502, error.problem.status)
        assertEquals("Upstream unavailable", error.problem.title)
    }

    private fun createRepository(apiKey: String): TrainTimesRepository {
        val client = OkHttpClient.Builder()
            .cache(Cache(cacheDirectory, 5L * 1024L * 1024L))
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TrainTimesApi::class.java)

        return DefaultTrainTimesRepository(api, json)
    }

    private fun jsonResponse(body: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-RateLimit-Limit", "60")
            .addHeader("X-RateLimit-Remaining", "59")
            .addHeader("X-RateLimit-Reset", "1")
            .setBody(body)

    private fun problemResponse(code: Int, type: String): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/problem+json")
            .setBody(
                """
                    {
                      "type": "https://traintimes.api/errors/$type",
                      "title": "${type.replace("_", " ").replaceFirstChar { it.uppercase() }}",
                      "status": $code,
                      "detail": "Request failed",
                      "instance": "/v1/errors/request-id"
                    }
                """.trimIndent()
            )

    private fun healthBody(): String =
        """
            {
              "data": { "status": "ok" },
              "meta": { "as_of": "2026-05-28T12:00:00Z" }
            }
        """.trimIndent()

    private fun stationsBody(): String =
        """
            {
              "data": {
                "stations": [
                  {
                    "id": "rosebank",
                    "name": "Rosebank",
                    "latitude": -26.14622,
                    "longitude": 28.04451,
                    "modes": ["bus", "rail"]
                  }
                ]
              },
              "meta": {
                "count": 1,
                "as_of": "2026-05-27T15:47:09Z",
                "cache": {
                  "status": "fresh",
                  "cached_at": "2026-05-27T15:47:09Z",
                  "age_seconds": 12,
                  "ttl_seconds": 86400
                }
              }
            }
        """.trimIndent()

    private fun journeysBody(): String =
        """
            {
              "data": {
                "journeys": [
                  {
                    "id": "journey-1",
                    "departure_time": "2026-05-28T03:33:36Z",
                    "arrival_time": "2026-05-28T03:41:21Z",
                    "duration_seconds": 503,
                    "distance_metres": 10891,
                    "total_fare_zar": 34.0,
                    "parking_cost_zar": 25.0,
                    "legs": [
                      {
                        "id": "leg-1",
                        "mode": "rail",
                        "line_name": "North - South Line",
                        "line_colour": "#b12f23",
                        "departure_stop": "rosebank",
                        "arrival_stop": "marlboro",
                        "departure_time": "2026-05-28T03:33:36Z",
                        "arrival_time": "2026-05-28T03:41:21Z",
                        "duration_seconds": 503,
                        "distance_metres": 10891,
                        "headsign": "Marlboro",
                        "carriages": 4,
                        "fare_amount_zar": 34.0,
                        "fare_is_peak": false,
                        "fare_product": "Pay-As-You-Go",
                        "trip_id": "trip-1",
                        "polyline": []
                      }
                    ]
                  }
                ]
              },
              "meta": {
                "count": 1,
                "from": "rosebank",
                "to": "marlboro",
                "next_cursor": "next-page",
                "as_of": "2026-05-27T15:47:16Z",
                "includes": ["polylines"],
                "cache": {
                  "status": "fresh",
                  "cached_at": "2026-05-27T15:47:09Z",
                  "age_seconds": 7,
                  "ttl_seconds": 15
                }
              }
            }
        """.trimIndent()
}
