package com.glucode.gautimes.widget

import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.local.entities.UserSettingsEntity
import com.glucode.gautimes.data.repository.ApiResult
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.data.repository.RateLimitInfo
import com.glucode.gautimes.data.repository.StationsRepository
import com.glucode.gautimes.data.repository.UserSettingsRepository
import com.glucode.gautimes.domain.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime

class GetDepartureWidgetStateUseCaseTest {
    @Test
    fun `uses the default effective route to show the next departure countdown`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                result = JourneyResult.Success(
                    journeys = listOf(
                        journey("past", "2026-06-26T07:55:00Z"),
                        journey("next", "2026-06-26T08:12:00Z"),
                        journey("second", "2026-06-26T08:24:00Z"),
                        journey("third", "2026-06-26T08:36:00Z")
                    )
                )
            ),
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals("Sandton to Hatfield", active.routeLabel)
        assertEquals(12, active.minutesUntilDeparture)
        assertEquals("2026-06-26T08:12:00Z", active.departureTime)
        assertEquals(
            listOf("2026-06-26T08:24:00Z", "2026-06-26T08:36:00Z"),
            active.secondaryDepartures
        )
    }

    @Test
    fun `waits for journey data after the repository emits loading`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                results = listOf(
                    JourneyResult.Loading,
                    JourneyResult.Success(
                        journeys = listOf(journey("next", "2026-06-26T08:12:00Z"))
                    )
                )
            ),
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals(12, active.minutesUntilDeparture)
    }

    @Test
    fun `uses the morning time-of-day route when scheduled defaults are enabled`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(
                    defaultFromId = "sandton",
                    defaultToId = "hatfield",
                    useSchedule = true,
                    morningFromId = "park",
                    morningToId = "rosebank"
                )
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                result = JourneyResult.Success(
                    journeys = listOf(journey("next", "2026-06-26T08:12:00Z"))
                )
            ),
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals("Park to Rosebank", active.routeLabel)
    }

    @Test
    fun `uses the afternoon time-of-day route when scheduled defaults are enabled`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(
                    defaultFromId = "sandton",
                    defaultToId = "hatfield",
                    useSchedule = true,
                    afternoonFromId = "hatfield",
                    afternoonToId = "sandton"
                )
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                result = JourneyResult.Success(
                    journeys = listOf(journey("next", "2026-06-26T16:12:00Z"))
                )
            ),
            clock = FixedClock("2026-06-26T16:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals("Hatfield to Sandton", active.routeLabel)
        assertEquals(12, active.minutesUntilDeparture)
    }

    @Test
    fun `extends the journey window when cached departures have expired and a next cursor exists`() = runBlocking {
        val journeysRepository = FakeJourneysRepository(
            getJourneyResults = listOf(
                JourneyResult.Success(
                    journeys = listOf(journey("expired", "2026-06-26T07:30:00Z")),
                    nextCursor = "next-window"
                ),
                JourneyResult.Success(
                    journeys = listOf(journey("next", "2026-06-26T08:12:00Z"))
                )
            )
        )
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = journeysRepository,
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals(12, active.minutesUntilDeparture)
        assertEquals(listOf("next-window"), journeysRepository.loadedCursors)
    }

    @Test
    fun `forces a fresh journey query when an expired journey window has no next cursor`() = runBlocking {
        val journeysRepository = FakeJourneysRepository(
            getJourneyResults = listOf(
                JourneyResult.Success(
                    journeys = listOf(journey("expired", "2026-06-26T07:30:00Z")),
                    nextCursor = null
                ),
                JourneyResult.Success(
                    journeys = listOf(journey("next", "2026-06-26T08:12:00Z"))
                )
            )
        )
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = journeysRepository,
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val active = state as DepartureWidgetState.Active
        assertEquals(12, active.minutesUntilDeparture)
        assertEquals(listOf(false, true), journeysRepository.forceRefreshRequests)
    }

    @Test
    fun `shows an empty state when refresh has no upcoming departures`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                getJourneyResults = listOf(
                    JourneyResult.Success(
                        journeys = listOf(journey("expired", "2026-06-26T07:30:00Z")),
                        nextCursor = null
                    ),
                    JourneyResult.Success(journeys = emptyList())
                )
            ),
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val empty = state as DepartureWidgetState.Empty
        assertEquals("Sandton to Hatfield", empty.routeLabel)
        assertEquals("No upcoming trains", empty.message)
    }

    @Test
    fun `shows a refresh failed state when forced refresh fails`() = runBlocking {
        val useCase = GetDepartureWidgetStateUseCase(
            userSettingsRepository = FakeUserSettingsRepository(
                UserSettingsEntity(defaultFromId = "sandton", defaultToId = "hatfield")
            ),
            stationsRepository = FakeStationsRepository(),
            journeysRepository = FakeJourneysRepository(
                getJourneyResults = listOf(
                    JourneyResult.Success(
                        journeys = listOf(journey("expired", "2026-06-26T07:30:00Z")),
                        nextCursor = null
                    ),
                    JourneyResult.Error("Unable to refresh")
                )
            ),
            clock = FixedClock("2026-06-26T08:00:00Z")
        )

        val state = useCase()

        val failed = state as DepartureWidgetState.RefreshFailed
        assertEquals("Sandton to Hatfield", failed.routeLabel)
        assertEquals("Unable to refresh", failed.message)
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

    private class FakeUserSettingsRepository(
        private val settings: UserSettingsEntity?
    ) : UserSettingsRepository {
        override fun getUserSettingsStream(): Flow<UserSettingsEntity?> = flowOf(settings)
        override suspend fun saveUserSettings(settings: UserSettingsEntity) = Unit
    }

    private class FakeStationsRepository : StationsRepository {
        override fun getStationsStream(): Flow<List<StationEntity>> = flowOf(
            listOf(
                StationEntity("sandton", "Sandton", -26.107, 28.056),
                StationEntity("hatfield", "Hatfield", -25.747, 28.238),
                StationEntity("park", "Park", -26.197, 28.041),
                StationEntity("rosebank", "Rosebank", -26.146, 28.044)
            )
        )

        override suspend fun refreshStations(forceNetwork: Boolean): ApiResult<Unit> =
            ApiResult.Success(Unit, RateLimitInfo(null, null, null))
    }

    private class FakeJourneysRepository(
        private val results: List<JourneyResult>,
        private val getJourneyResults: List<JourneyResult> = emptyList(),
        private val loadMoreResult: ApiResult<Unit> = ApiResult.Success(
            Unit,
            RateLimitInfo(null, null, null)
        )
    ) : JourneysRepository {
        constructor(result: JourneyResult) : this(listOf(result))
        constructor(getJourneyResults: List<JourneyResult>) : this(emptyList(), getJourneyResults)

        val loadedCursors = mutableListOf<String>()
        val forceRefreshRequests = mutableListOf<Boolean>()
        private var getJourneysCallCount = 0

        override fun getJourneys(from: String, to: String, forceRefresh: Boolean): Flow<JourneyResult> =
            flow {
                forceRefreshRequests += forceRefresh
                val callResults = getJourneyResults.getOrNull(getJourneysCallCount)?.let { listOf(it) } ?: results
                getJourneysCallCount += 1
                callResults.forEach { emit(it) }
            }

        override suspend fun loadMore(from: String, to: String, cursor: String): ApiResult<Unit> =
            loadMoreResult.also { loadedCursors += cursor }

        override suspend fun getJourneyById(id: String): JourneyWithLegs? = null
        override suspend fun wipeJourneys() = Unit
    }
}
