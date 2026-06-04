package com.glucode.gautimes.screens.home

import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.DateUtils
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class HomeMapper @Inject constructor() {

    fun mapToHomeState(
        isLoading: Boolean,
        isRefreshing: Boolean,
        isFetchingMore: Boolean,
        userInteraction: UserInteractionState,
        data: DataState,
        isFromNear: Boolean
    ): HomeState {
        if (isLoading) return HomeState.Loading

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val selection = userInteraction.selection
        val locationSheet = userInteraction.locationSheet
        val serviceProbe = data.serviceProbe
        val stations = data.stations
        val journeysResult = data.journeysResult
        val location = data.currentLocation

        val currentLat = location?.latitude
        val currentLong = location?.longitude

        val stationNames = stations.map { it.name }
        
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
                timeText = DateUtils.formatIsoTime(journey.journey.departureTime),
                cartColor = firstLeg?.lineColour?.toColor() ?: cartYellow,
                cartNumber = firstLeg?.carriages ?: 4
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
                isRefreshing = isRefreshing,
                isFetchingMore = isFetchingMore,
                nextCursor = nextCursor,
                infoText = HomeInfoText(
                    title = "Coming up next",
                    description = "Peak fares will be in-affect until 18:45 tonight"
                ),
                healthCheck = serviceProbe.healthCheck,
                stationsCheck = serviceProbe.stationsCheck,
                journeysCheck = serviceProbe.journeysCheck,
                isProbeCachingEnabled = serviceProbe.isCachingEnabled,
                isFromNear = isFromNear,
                currentLat = currentLat,
                currentLong = currentLong,
                progress = buildProgressCard(nextJourney),
                showLocationSheet = locationSheet.show,
                locationSection = buildLocationSelector(
                    locationSheet.target,
                    selection.from,
                    selection.to,
                    stationNames
                )
            )
        )
    }

    private fun buildProgressCard(nextJourney: JourneyWithLegs?): DepartureTimeCardData {
        return if (nextJourney != null) {
            val minutesUntil = DateUtils.getMinutesUntil(nextJourney.journey.departureTime)
            DepartureTimeCardData(
                timeValue = minutesUntil.toString(),
                progressDescription = "MINUTES UNTIL DEPARTURE",
                arrivalTime = DateUtils.formatIsoTime(nextJourney.journey.arrivalTime)
            )
        } else {
            DepartureTimeCardData(
                timeValue = "--",
                progressDescription = "NO MORE JOURNEYS"
            )
        }
    }

    private fun buildLocationSelector(
        target: LocationTarget,
        fromLocation: String,
        toLocation: String,
        availableLocations: List<String> = emptyList()
    ): LocationSelectorBottomSheetData {
        val selected = if (target == LocationTarget.FROM) fromLocation else toLocation
        val disabled = if (target == LocationTarget.FROM) toLocation else fromLocation
        return LocationSelectorBottomSheetData(
            locations = availableLocations,
            selectedLocation = selected,
            disabledLocation = disabled,
            locationTarget = target
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
