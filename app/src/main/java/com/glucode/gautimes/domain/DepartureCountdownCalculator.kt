 package com.glucode.gautimes.domain

import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import java.time.Duration
import java.time.OffsetDateTime
import javax.inject.Inject

data class DepartureCountdownWindow(
    val upcomingJourneys: List<JourneyWithLegs>,
    val primary: DepartureCountdown?
)

data class DepartureCountdown(
    val journey: JourneyWithLegs,
    val minutesUntilDeparture: Long,
    val secondaryDepartures: List<JourneyWithLegs>
)

class DepartureCountdownCalculator @Inject constructor(
    private val clock: Clock
) {
    fun evaluate(
        journeys: List<JourneyWithLegs>,
        secondaryDepartureLimit: Int = DEFAULT_SECONDARY_DEPARTURE_LIMIT
    ): DepartureCountdownWindow {
        val now = clock.now()
        val upcoming = journeys
            .filter { OffsetDateTime.parse(it.journey.departureTime).isAfter(now) }
            .sortedBy { it.journey.departureTime }

        val next = upcoming.firstOrNull()
            ?: return DepartureCountdownWindow(upcomingJourneys = emptyList(), primary = null)

        return DepartureCountdownWindow(
            upcomingJourneys = upcoming,
            primary = DepartureCountdown(
                journey = next,
                minutesUntilDeparture = ceilingMinutesUntil(now, next.journey.departureTime),
                secondaryDepartures = upcoming.drop(1).take(secondaryDepartureLimit)
            )
        )
    }

    private fun ceilingMinutesUntil(now: OffsetDateTime, departureTime: String): Long {
        val millisUntilDeparture = Duration.between(now, OffsetDateTime.parse(departureTime)).toMillis()
        return (millisUntilDeparture + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE
    }

    private companion object {
        const val DEFAULT_SECONDARY_DEPARTURE_LIMIT = 2
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
