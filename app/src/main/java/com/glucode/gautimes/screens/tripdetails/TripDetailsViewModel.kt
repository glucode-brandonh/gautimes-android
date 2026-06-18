package com.glucode.gautimes.screens.tripdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.components.DepartureTimeCardData
import com.glucode.gautimes.components.reminders.ReminderInfo
import com.glucode.gautimes.components.sharing.ShareInfo
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

data class StationUiData(
    val name: String,
    val arrivalTime: String,
    val isBold: Boolean = false
)

data class TripDetailsUiState(
    val isLoading: Boolean = true,
    val stations: List<StationUiData> = emptyList(),
    val reminderInfo: ReminderInfo? = null,
    val shareInfo: ShareInfo? = null,
    val schedule: List<Pair<String, String>> = emptyList(),
    val progress: DepartureTimeCardData = DepartureTimeCardData(),
)

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journeysRepository: JourneysRepository,
    private val stationDao: StationDao
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState: StateFlow<TripDetailsUiState> = _uiState.asStateFlow()

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    init {
        loadTripDetails()
    }

    private fun loadTripDetails() {
        viewModelScope.launch {
            combine(
                flow { emit(journeysRepository.getJourneyById(tripId)) },
                stationDao.getStations(),
                ticker
            ) { journey, stations, _ ->
                journey to stations
            }.collect { (journey, stations) ->
                if (journey == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@collect
                }

                val departureStation = stations.find { it.id == journey.journey.fromStationId }
                val arrivalStation = stations.find { it.id == journey.journey.toStationId }

                val reminderInfo = ReminderInfo(
                    from = departureStation?.name ?: "Unknown",
                    to = arrivalStation?.name ?: "Unknown",
                    departureTime = journey.journey.departureTime,
                    arrivalTime = journey.journey.arrivalTime
                )

                val shareInfo = ShareInfo(
                    from = departureStation?.name ?: "Unknown",
                    to = arrivalStation?.name ?: "Unknown",
                    departureTime = journey.journey.departureTime,
                    arrivalTime = journey.journey.arrivalTime,
                    latitude = departureStation?.latitude,
                    longitude = departureStation?.longitude
                )

                val journeyResult = journeysRepository.getJourneys(
                    journey.journey.fromStationId,
                    journey.journey.toStationId
                ).firstOrNull { it is JourneyResult.Success } as? JourneyResult.Success

                val schedule = journeyResult?.journeys?.map {
                    it.journey.departureTime to it.journey.arrivalTime
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    stations = buildStationList(
                        journey = journey,
                        departureStation = departureStation,
                        arrivalStation = arrivalStation
                    ),
                    reminderInfo = reminderInfo,
                    shareInfo = shareInfo,
                    schedule = schedule,
                    progress = buildDepartureCard(journey.journey)
                )
            }
        }
    }

    fun buildDepartureCard(journey: JourneyEntity): DepartureTimeCardData {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"))
        val price = currencyFormat.format(journey.totalFareZar)
        val minutesUntil = DateUtils.getMinutesUntil(journey.departureTime)

        return DepartureTimeCardData(
            id = journey.id,
            timeValue = minutesUntil.toString(),
            title = "TRAIN LEAVING IN",
            progressDescription = "MINUTES UNTIL DEPARTURE",
            arrivalTime = journey.arrivalTime,
            departureTime = journey.departureTime,
            price = price
        )
    }

    fun buildStationList(
        journey: JourneyWithLegs,
        departureStation: StationEntity?,
        arrivalStation: StationEntity?
    ): List<StationUiData> {
        val stationList = mutableListOf<StationUiData>()

        // Start Station
        stationList.add(
            StationUiData(
                name = departureStation?.name ?: "Unknown",
                arrivalTime = DateUtils.formatIsoTime(journey.journey.departureTime),
                isBold = true
            )
        )

        // Intermediate Stations
        stationList.addAll(
            journey.intermediateStations.map {
                StationUiData(
                    name = it.name,
                    arrivalTime = it.arrivalTime?.let { time ->
                        DateUtils.formatIsoTime(
                            time
                        )
                    } ?: "--:--"
                )
            }
        )

        // End Station
        stationList.add(
            StationUiData(
                name = arrivalStation?.name ?: "Unknown",
                arrivalTime = DateUtils.formatIsoTime(journey.journey.arrivalTime),
                isBold = true
            )
        )

        return stationList
    }
}
