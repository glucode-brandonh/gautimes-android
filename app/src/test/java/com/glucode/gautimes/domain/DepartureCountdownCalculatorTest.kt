package com.glucode.gautimes.domain

import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class DepartureCountdownCalculatorTest {
    @Test
    fun `selects the soonest upcoming journey and secondary departures`() {
        val calculator = DepartureCountdownCalculator(FixedClock("2026-06-26T08:00:00Z"))

        val window = calculator.evaluate(
            listOf(
                journey("expired", "2026-06-26T07:59:59Z"),
                journey("third", "2026-06-26T08:30:00Z"),
                journey("next", "2026-06-26T08:10:00Z"),
                journey("second", "2026-06-26T08:20:00Z")
            )
        )

        assertEquals("next", window.primary?.journey?.journey?.id)
        assertEquals(10L, window.primary?.minutesUntilDeparture)
        assertEquals(listOf("second", "third"), window.primary?.secondaryDepartures?.map { it.journey.id })
        assertEquals(listOf("next", "second", "third"), window.upcomingJourneys.map { it.journey.id })
    }

    @Test
    fun `rounds future departures up to the next whole minute`() {
        val calculator = DepartureCountdownCalculator(FixedClock("2026-06-26T08:00:59Z"))

        val window = calculator.evaluate(
            listOf(journey("next", "2026-06-26T08:12:00Z"))
        )

        assertEquals(12L, window.primary?.minutesUntilDeparture)
    }

    @Test
    fun `expires a journey at its departure time`() {
        val calculator = DepartureCountdownCalculator(FixedClock("2026-06-26T08:12:00Z"))

        val window = calculator.evaluate(
            listOf(journey("departing-now", "2026-06-26T08:12:00Z"))
        )

        assertNull(window.primary)
        assertEquals(emptyList<JourneyWithLegs>(), window.upcomingJourneys)
    }

    private fun journey(id: String, departureTime: String): JourneyWithLegs =
        JourneyWithLegs(
            journey = JourneyEntity(
                id = id,
                fromStationId = "sandton",
                toStationId = "hatfield",
                departureTime = departureTime,
                arrivalTime = OffsetDateTime.parse(departureTime).plusMinutes(20).toString(),
                durationSeconds = 1200,
                distanceMetres = 10000,
                totalFareZar = 34.0,
                parkingCostZar = null
            ),
            legs = emptyList(),
            intermediateStations = emptyList()
        )

    private class FixedClock(private val isoNow: String) : Clock {
        override fun now(): OffsetDateTime = OffsetDateTime.parse(isoNow)
    }
}
