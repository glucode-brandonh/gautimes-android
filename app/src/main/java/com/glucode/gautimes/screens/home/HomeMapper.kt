package com.glucode.gautimes.screens.home

import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.DateUtils
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.util.Locale
import java.time.ZoneOffset
import javax.inject.Inject

class HomeMapper @Inject constructor() {

    fun mapToHomeState(
        homeUiState: HomeUiState,
        userInteraction: UserInteractionState,
        data: DataState,
        isFromNear: Boolean,
        showLocationPermissionCard: Boolean,
        nearestStationName: String? = null
    ): HomeState {
        if (homeUiState.isLoading) return HomeState.Loading

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val selection = userInteraction.selection
        val locationSheet = userInteraction.locationSheet
        val journeysResult = data.journeysResult
        val location = data.currentLocation

        val currentLat = location?.latitude
        val currentLong = location?.longitude

        val stationNames = data.stations.map { it.name }
        
        val upcomingJourneys = if (journeysResult is JourneyResult.Success) {
            journeysResult.journeys
                .filter { OffsetDateTime.parse(it.journey.departureTime).isAfter(now) }
                .sortedBy { it.journey.departureTime }
        } else emptyList()

        val nextCursor = (journeysResult as? JourneyResult.Success)?.nextCursor

        val scheduleTimes = upcomingJourneys.map { journey ->
            val firstLeg = journey.legs.firstOrNull()
            ScheduleTimeLineItemData(
                id = journey.journey.id,
                cartColor = firstLeg?.lineColour?.toColor() ?: cartYellow,
                cartNumber = firstLeg?.carriages ?: 4,
                departureTime = journey.journey.departureTime,
                arrivalTime = journey.journey.arrivalTime
            )
        }

        val nextJourney = upcomingJourneys.firstOrNull()

        return HomeState.HasData(
            data = HomeData(
                fromLocation = selection.from,
                toLocation = selection.to,
                dateLabel = DateUtils.formatDateLabel(selection.dateMillis),
                scheduleTimes = scheduleTimes,
                journeyResult = journeysResult,
                isRefreshing = homeUiState.isRefreshing,
                isFetchingMore = homeUiState.isFetchingMore,
                nextCursor = nextCursor,
                infoText = HomeInfoText(
                    title = "Coming up next",
                    description = "Peak hours on weekdays are 06:00 to 08:30 in the morning and 15:00 to 18:30 in the afternoon."
                ),
                isGrantingPermission = homeUiState.isGrantingPermission,
                isFromNear = isFromNear,
                currentLat = currentLat,
                currentLong = currentLong,
                progress = buildProgressCard(nextJourney),
                showLocationSheet = locationSheet.show,
                showLocationPermissionCard = showLocationPermissionCard,
                locationSection = buildLocationSelector(
                    locationSheet.target,
                    selection.from,
                    selection.to,
                    stationNames,
                    nearestStationName
                )
            )
        )
    }

    private fun buildProgressCard(
        nextJourney: JourneyWithLegs?
    ): DepartureTimeCardData {
        return if (nextJourney != null) {
            val minutesUntil = DateUtils.getMinutesUntil(nextJourney.journey.departureTime)
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
            val price = currencyFormat.format(nextJourney.journey.totalFareZar)

            DepartureTimeCardData(
                id = nextJourney.journey.id,
                timeValue = minutesUntil.toString(),
                title = "NEXT TRAIN LEAVING IN",
                progressDescription = "MINUTES UNTIL DEPARTURE",
                arrivalTime = nextJourney.journey.arrivalTime,
                departureTime = nextJourney.journey.departureTime,
                price = price
            )
        } else {
            DepartureTimeCardData(
                title = "NEXT TRAIN LEAVING IN",
                timeValue = "--",
                progressDescription = "NO MORE JOURNEYS"
            )
        }
    }

    private fun buildLocationSelector(
        target: LocationTarget,
        fromLocation: String,
        toLocation: String,
        availableLocations: List<String> = emptyList(),
        nearestStationName: String? = null
    ): LocationSelectorBottomSheetData {
        val selected = if (target == LocationTarget.FROM) fromLocation else toLocation
        val disabled = if (target == LocationTarget.FROM) toLocation else fromLocation
        return LocationSelectorBottomSheetData(
            locations = availableLocations,
            selectedLocation = selected,
            disabledLocation = disabled,
            locationTarget = target,
            nearestLocation = if (target == LocationTarget.FROM) nearestStationName else null
        )
    }

    private fun String.toColor(): androidx.compose.ui.graphics.Color {
        return try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color.Gray
        }
    }
}
