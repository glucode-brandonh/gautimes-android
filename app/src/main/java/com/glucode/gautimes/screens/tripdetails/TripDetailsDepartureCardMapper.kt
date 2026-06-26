package com.glucode.gautimes.screens.tripdetails

import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.domain.DepartureCountdownCalculator
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

class TripDetailsDepartureCardMapper @Inject constructor(
    private val departureCountdownCalculator: DepartureCountdownCalculator
) {
    fun map(journey: JourneyWithLegs): DepartureTimeCardData {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
        val price = currencyFormat.format(journey.journey.totalFareZar)
        val countdown = departureCountdownCalculator.evaluate(listOf(journey)).primary
            ?: return DepartureTimeCardData(
                id = journey.journey.id,
                timeValue = "--",
                title = "TRAIN LEAVING IN",
                progressDescription = "DEPARTED",
                arrivalTime = journey.journey.arrivalTime,
                departureTime = journey.journey.departureTime,
                price = price
            )

        return DepartureTimeCardData(
            id = journey.journey.id,
            timeValue = countdown.minutesUntilDeparture.toString(),
            title = "TRAIN LEAVING IN",
            progressDescription = "MINUTES UNTIL DEPARTURE",
            arrivalTime = journey.journey.arrivalTime,
            departureTime = journey.journey.departureTime,
            price = price
        )
    }
}
