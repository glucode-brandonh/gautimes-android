package com.glucode.gautimes.screens.home

import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.components.ScheduleTimeLineItemData
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.StationEntity
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
        userInteraction: UserInteractionState,
        data: DataState
    ): HomeState {
        if (isLoading) return HomeState.Loading

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val selection = userInteraction.selection
        val locationSheet = userInteraction.locationSheet
        val serviceProbe = data.serviceProbe
        val stations = data.stations
        val journeysResult = data.journeysResult
        val location = data.currentLocation

        val fromStation = stations.find { it.name == selection.from }
        val isFromNear = isLocationNearStation(location, fromStation, stations)

        val currentLat = location?.latitude
        val currentLong = location?.longitude

        val stationNames = stations.map { it.name }
        val scheduleTimes = if (journeysResult is JourneyResult.Success) {
            journeysResult.journeys
                .filter { OffsetDateTime.parse(it.journey.departureTime).isAfter(now) }
                .map { journey ->
                    val firstLeg = journey.legs.firstOrNull()
                    ScheduleTimeLineItemData(
                        timeText = DateUtils.formatIsoTime(journey.journey.departureTime),
                        cartColor = firstLeg?.lineColour?.toColor() ?: cartYellow,
                        cartNumber = firstLeg?.carriages ?: 4
                    )
                }
        } else emptyList()

        val nextJourney = if (journeysResult is JourneyResult.Success) {
            journeysResult.journeys
                .filter { OffsetDateTime.parse(it.journey.departureTime).isAfter(now) }
                .minByOrNull { it.journey.departureTime }
        } else null

        return HomeState.HasData(
            data = HomeData(
                fromLocation = selection.from,
                toLocation = selection.to,
                dateLabel = DateUtils.formatDateLabel(selection.dateMillis),
                scheduleTimes = scheduleTimes,
                journeyResult = journeysResult,
                isRefreshing = isRefreshing,
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

    private fun isLocationNearStation(
        location: android.location.Location?,
        station: StationEntity?,
        allStations: List<StationEntity>
    ): Boolean {
        if (location == null || station == null || allStations.isEmpty()) return true
        val closestStation = allStations.minByOrNull { s ->
            val sLocation = android.location.Location("").apply {
                latitude = s.latitude
                longitude = s.longitude
            }
            location.distanceTo(sLocation)
        }
        return station.id == closestStation?.id
    }

    private fun String.toColor(): androidx.compose.ui.graphics.Color {
        return try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(this))
        } catch (e: Exception) {
            androidx.compose.ui.graphics.Color.Gray
        }
    }
}
