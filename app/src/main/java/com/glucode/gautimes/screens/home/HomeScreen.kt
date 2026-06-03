package com.glucode.gautimes.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.components.LocationSelectorBottomSheet
import com.glucode.gautimes.components.DepartureTimeCard
import com.glucode.gautimes.components.ScheduleTimeLineItem
import com.glucode.gautimes.components.ScheduleTimeLineItemSkeleton
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.screens.home.ui.DatePickerModal
import com.glucode.gautimes.screens.home.ui.LocationSelection
import com.glucode.gautimes.screens.home.ui.debug.CacheModeChip
import com.glucode.gautimes.screens.home.ui.debug.HealthStatusChip
import com.glucode.gautimes.screens.home.ui.debug.JourneysStatusChip
import com.glucode.gautimes.screens.home.ui.debug.LocationDebugChip
import com.glucode.gautimes.screens.home.ui.debug.StationsStatusChip

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewmodel: HomeViewmodel = hiltViewModel()) {
    val uiState by viewmodel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is HomeState.Loading -> CircularProgressIndicator()
            is HomeState.Error -> Text(text = "Error: ${state.message}")
            is HomeState.HasData -> HomeContent(data = state.data, viewmodel = viewmodel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(data: HomeData, viewmodel: HomeViewmodel) {
    var showDatePicker by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = data.isRefreshing,
        onRefresh = { viewmodel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                showDatePicker = HomeAssistChips(showDatePicker, data, viewmodel)

                LocationSelection(
                    fromLocation = data.fromLocation,
                    toLocation = data.toLocation,
                    isFromNear = data.isFromNear,
                    onLocationChange = { target ->
                        viewmodel.toggleLocationSheet(true, target)
                    },
                    onFlipLocations = {
                        viewmodel.flipLocations()
                    }
                )

                Spacer(modifier = Modifier.size(8.dp))
                DepartureTimeCard(data = data.progress, onClick = {})
                Spacer(modifier = Modifier.size(8.dp))
                InfoSection(info = data.infoText)
                Spacer(modifier = Modifier.size(8.dp))
            }

            when (val journeyResult = data.journeyResult) {
                is JourneyResult.Loading -> {
                    items(5) {
                        ScheduleTimeLineItemSkeleton()
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }

                is JourneyResult.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                journeyResult.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is JourneyResult.Success -> {
                    if (data.scheduleTimes.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    "No schedule found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(data.scheduleTimes.size) { index ->
                            ScheduleTimeLineItem(
                                data = data.scheduleTimes[index]
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { viewmodel.updateDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (data.showLocationSheet) {
        LocationSelectorBottomSheet(
            data = data.locationSection,
            onDismissRequest = { location, target ->
                if (target == LocationTarget.FROM) {
                    viewmodel.updateFromLocation(location)
                } else {
                    viewmodel.updateToLocation(location)
                }
                viewmodel.toggleLocationSheet(false, target)
            },
        )
    }
}

@Composable
private fun HomeAssistChips(
    showDatePicker: Boolean,
    data: HomeData,
    viewmodel: HomeViewmodel
): Boolean {
    var showDatePicker1 = showDatePicker
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(onClick = { showDatePicker1 = true }, label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Calendar"
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(data.dateLabel, style = MaterialTheme.typography.titleMedium)
            }
        })
        if (BuildConfig.DEBUG) {
            HealthStatusChip(
                healthCheck = data.healthCheck,
                onClick = viewmodel::refreshHealth
            )
            StationsStatusChip(
                stationsCheck = data.stationsCheck,
                onClick = viewmodel::refreshStations
            )
            JourneysStatusChip(
                journeysCheck = data.journeysCheck,
                onClick = { viewmodel.refreshJourneys(force = true) }
            )
            CacheModeChip(
                isCachingEnabled = data.isProbeCachingEnabled,
                onClick = viewmodel::toggleProbeCaching
            )
            if (data.currentLat != null && data.currentLong != null) {
                LocationDebugChip(
                    lat = data.currentLat,
                    lon = data.currentLong,
                    onClick = viewmodel::refreshLocation
                )
            }
        }
    }
    return showDatePicker1
}

@Composable
fun InfoSection(info: HomeInfoText) {
    Column {
        Text(
            info.title,
            style = MaterialTheme.typography.headlineSmallEmphasized
        )
        Text(info.description)
    }
}


