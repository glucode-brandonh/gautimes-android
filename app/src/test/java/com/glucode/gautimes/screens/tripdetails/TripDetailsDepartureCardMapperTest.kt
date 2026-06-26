package com.glucode.gautimes.screens.tripdetails

import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.domain.Clock
import com.glucode.gautimes.domain.DepartureCountdownCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

class TripDetailsDepartureCardMapperTest {
    @Test
    fun `departure card uses shared countdown semantics`() {
        val mapper = mapper(now = "2026-06-26T08:00:59Z")

        val card = mapper.map(journey("next", "2026-06-26T08:12:00Z"))

        assertEquals("12", card.timeValue)
        assertEquals("MINUTES UNTIL DEPARTURE", card.progressDescription)
    }

    @Test
    fun `departure card does not show negative countdown for departed trip`() {
        val mapper = mapper(now = "2026-06-26T08:12:00Z")

        val card = mapper.map(journey("departed", "2026-06-26T08:12:00Z"))

        assertEquals("--", card.timeValue)
        assertEquals("DEPARTED", card.progressDescription)
    }

    private fun mapper(now: String): TripDetailsDepartureCardMapper =
        TripDetailsDepartureCardMapper(
            departureCountdownCalculator = DepartureCountdownCalculator(FixedClock(now))
        )

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
