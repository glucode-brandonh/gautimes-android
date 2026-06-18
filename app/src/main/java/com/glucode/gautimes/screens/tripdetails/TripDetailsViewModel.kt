package com.glucode.gautimes.screens.tripdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.repository.JourneysRepository
import com.glucode.gautimes.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.glucode.gautimes.components.reminders.ReminderInfo
import com.glucode.gautimes.data.repository.JourneyResult
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

data class StationUiData(
    val name: String,
    val arrivalTime: String,
    val isBold: Boolean = false
)

data class TripDetailsUiState(
    val isLoading: Boolean = true,
    val stations: List<StationUiData> = emptyList(),
    val reminderInfo: ReminderInfo? = null,
    val schedule: List<Pair<String, String>> = emptyList()
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

    init {
        loadTripDetails()
    }

    private fun loadTripDetails() {
        viewModelScope.launch {
            val journey = journeysRepository.getJourneyById(tripId)
            if (journey != null) {
                stationDao.getStations().collect { stations ->
                    val departureStation = stations.find { it.id == journey.journey.fromStationId }
                    val arrivalStation = stations.find { it.id == journey.journey.toStationId }

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
                                arrivalTime = it.arrivalTime?.let { time -> DateUtils.formatIsoTime(time) } ?: "--:--"
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

                    val reminderInfo = ReminderInfo(
                        from = departureStation?.name ?: "Unknown",
                        to = arrivalStation?.name ?: "Unknown",
                        departureTime = journey.journey.departureTime,
                        arrivalTime = journey.journey.arrivalTime
                    )

                    // Fetch full schedule for reminders
                    val journeyResult = journeysRepository.getJourneys(
                        journey.journey.fromStationId,
                        journey.journey.toStationId
                    ).firstOrNull { it is JourneyResult.Success } as? JourneyResult.Success

                    val schedule = journeyResult?.journeys?.map {
                        it.journey.departureTime to it.journey.arrivalTime
                    } ?: emptyList()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        stations = stationList,
                        reminderInfo = reminderInfo,
                        schedule = schedule
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
