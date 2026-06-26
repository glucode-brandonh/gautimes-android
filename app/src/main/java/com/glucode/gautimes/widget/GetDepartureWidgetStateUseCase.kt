package com.glucode.gautimes.widget

import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.data.repository.UserSettingsRepository
import com.glucode.gautimes.domain.Clock
import com.glucode.gautimes.domain.DepartureCountdownCalculator
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetDepartureWidgetStateUseCase @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val stationsRepository: StationsRepository,
    private val journeysRepository: JourneysRepository,
    private val clock: Clock,
    private val departureCountdownCalculator: DepartureCountdownCalculator
) {
    suspend operator fun invoke(): DepartureWidgetState {
        val settings = userSettingsRepository.getUserSettingsStream().first() ?: UserSettingsEntity()
        val stations = stationsRepository.getStationsStream().first()
        val route = settings.effectiveRoute(clock.now().hour)
        val fromName = stations.find { it.id == route.fromId }?.name ?: route.fromId
        val toName = stations.find { it.id == route.toId }?.name ?: route.toId
        val routeLabel = "$fromName to $toName"

        val firstResult = loadJourneyResult(route)
        val firstState = firstResult.toWidgetState(routeLabel)

        if (firstState is DepartureWidgetState.Empty && firstResult is JourneyResult.Success) {
            if (firstResult.nextCursor != null) {
                val loadMoreResult = journeysRepository.loadMore(route.fromId, route.toId, firstResult.nextCursor)
                if (loadMoreResult is ApiResult.Success) {
                    val loadedState = loadJourneyResult(route).toWidgetState(routeLabel)
                    if (loadedState !is DepartureWidgetState.Empty) return loadedState
                }
            }

            return loadJourneyResult(route, forceRefresh = true).toWidgetState(routeLabel)
        }

        return firstState
    }

    private suspend fun loadJourneyResult(
        route: WidgetRoute,
        forceRefresh: Boolean = false
    ): JourneyResult =
        journeysRepository.getJourneys(route.fromId, route.toId, forceRefresh = forceRefresh)
            .filterNot { it is JourneyResult.Loading }
            .first()

    private fun JourneyResult.toWidgetState(routeLabel: String): DepartureWidgetState =
        when (this) {
            JourneyResult.Loading -> DepartureWidgetState.Loading
            is JourneyResult.Error -> DepartureWidgetState.RefreshFailed(routeLabel, message)
            is JourneyResult.Success -> journeys.toActiveState(routeLabel)
        }

    private fun List<JourneyWithLegs>.toActiveState(routeLabel: String): DepartureWidgetState {
        val countdown = departureCountdownCalculator.evaluate(this).primary
            ?: return DepartureWidgetState.Empty(routeLabel, "No upcoming trains")

        val next = countdown.journey
        return DepartureWidgetState.Active(
            routeLabel = routeLabel,
            minutesUntilDeparture = countdown.minutesUntilDeparture,
            departureTime = next.journey.departureTime,
            arrivalTime = next.journey.arrivalTime,
            secondaryDepartures = countdown.secondaryDepartures.map { it.journey.departureTime }
        )
    }

    private fun UserSettingsEntity.effectiveRoute(hour: Int): WidgetRoute {
        if (useSchedule) {
            if (hour in 5..11 && morningFromId != null && morningToId != null) {
                return WidgetRoute(morningFromId, morningToId)
            }
            if (hour in 12..20 && afternoonFromId != null && afternoonToId != null) {
                return WidgetRoute(afternoonFromId, afternoonToId)
            }
        }
        return WidgetRoute(defaultFromId, defaultToId)
    }

    private data class WidgetRoute(
        val fromId: String,
        val toId: String
    )
}
