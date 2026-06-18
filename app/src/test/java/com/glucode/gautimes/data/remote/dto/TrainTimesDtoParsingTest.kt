package com.glucode.gautimes.data.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class TrainTimesDtoParsingTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesHealthEnvelope() {
        val body = """
            {
              "data": { "status": "ok" },
              "meta": { "as_of": "2026-05-28T12:00:00Z" }
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelopeDto<HealthDataDto, HealthMetaDto>>(body)

        assertEquals("ok", envelope.data.status)
        assertEquals("2026-05-28T12:00:00Z", envelope.meta.asOf)
    }

    @Test
    fun parsesStationsEnvelope() {
        val body = """
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

        val envelope = json.decodeFromString<ApiEnvelopeDto<StationsDataDto, StationsMetaDto>>(body)

        assertEquals(1, envelope.meta.count)
        assertEquals("rosebank", envelope.data.stations.first().id)
        assertEquals(listOf("bus", "rail"), envelope.data.stations.first().modes)
        assertEquals("fresh", envelope.meta.cache.status)
    }

    @Test
    fun parsesJourneysEnvelope() {
        val body = """
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
                        "polyline": [[-26.14622, 28.04451]]
                      }
                    ]
                  }
                ]
              },
              "meta": {
                "count": 1,
                "from": "rosebank",
                "to": "marlboro",
                "next_cursor": "MjAyNi0wNS0yOFQwNDoxMzozNlo",
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

        val envelope = json.decodeFromString<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>>(body)
        val journey = envelope.data.journeys.first()
        val leg = journey.legs.first()

        assertEquals("MjAyNi0wNS0yOFQwNDoxMzozNlo", envelope.meta.nextCursor)
        assertEquals(34.0, journey.totalFareZar, 0.0)
        assertFalse(leg.fareIsPeak)
        assertEquals(listOf(-26.14622, 28.04451), leg.polyline.first())
    }

    @Test
    fun parsesJourneysWithIntermediateStations() {
        val body = """
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
                    "intermediate_stations": [
                      {
                        "id": "sandton",
                        "name": "Sandton",
                        "arrival_time": "2026-05-28T03:37:00Z",
                        "duration_seconds": 204
                      }
                    ],
                    "legs": []
                  }
                ]
              },
              "meta": {
                "count": 1,
                "from": "rosebank",
                "to": "marlboro",
                "as_of": "2026-05-27T15:47:16Z",
                "cache": {
                  "status": "fresh",
                  "cached_at": "2026-05-27T15:47:09Z",
                  "age_seconds": 7,
                  "ttl_seconds": 15
                }
              }
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelopeDto<JourneysDataDto, JourneysMetaDto>>(body)
        val journey = envelope.data.journeys.first()
        val intermediateStation = journey.intermediateStations.first()

        assertEquals(1, journey.intermediateStations.size)
        assertEquals("sandton", intermediateStation.id)
        assertEquals("Sandton", intermediateStation.name)
        assertEquals("2026-05-28T03:37:00Z", intermediateStation.arrivalTime)
        assertEquals(204, intermediateStation.durationSeconds)
    }

    @Test
    fun parsesProblemDetail() {
        val body = """
            {
              "type": "https://traintimes.api/errors/same_station",
              "title": "Same station",
              "status": 400,
              "detail": "Origin and destination must be different stations",
              "instance": "/v1/errors/request-id",
              "from": "rosebank",
              "to": "rosebank"
            }
        """.trimIndent()

        val problem = json.decodeFromString<ProblemDetailDto>(body)

        assertEquals(400, problem.status)
        assertEquals("Same station", problem.title)
        assertEquals("Origin and destination must be different stations", problem.detail)
    }
}
