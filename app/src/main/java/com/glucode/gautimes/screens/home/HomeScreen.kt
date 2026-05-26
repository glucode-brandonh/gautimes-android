package com.glucode.gautimes.screens.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import java.util.Calendar
import java.util.TimeZone
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
import com.glucode.gautimes.components.LocationSelectorBottomSheet
import com.glucode.gautimes.components.LocationTargetSection
import com.glucode.gautimes.components.ProgressCard
import com.glucode.gautimes.components.ScheduleTimeLineItem
import com.glucode.gautimes.components.ScheduleTimeLineItemData

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        AssistChip(onClick = { showDatePicker = true }, label = {
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

        LocationSection(
            fromLocation = data.fromLocation,
            toLocation = data.toLocation,
            onLocationChange = { target ->
                viewmodel.toggleLocationSheet(true, target)
            },
            onFlipLocations = {
                viewmodel.flipLocations()
            }
        )

        Spacer(modifier = Modifier.size(8.dp))
        ProgressCard(data = data.progress, onClick = {})
        Spacer(modifier = Modifier.size(8.dp))

        InfoSection(info = data.infoText)

        Spacer(modifier = Modifier.size(8.dp))
        HomeScreenScheduleList(times = data.scheduleTimes)

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
}

@Composable
fun LocationSection(
    modifier: Modifier = Modifier,
    fromLocation: String,
    toLocation: String,
    onLocationChange: (target: LocationTarget) -> Unit,
    onFlipLocations: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LocationTargetSection(
                targetLabel = LocationTarget.FROM.label,
                locationName = fromLocation,
                onClick = {
                    onLocationChange(LocationTarget.FROM)
                })
            LocationTargetSection(
                targetLabel = LocationTarget.TO.label,
                locationName = toLocation,
                onClick = {
                    onLocationChange(LocationTarget.TO)
                })
        }
        IconButton(onClick = onFlipLocations) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "Flip Locations",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InfoSection(info: HomeInfoText) {
    Column {
        Text(info.title, style = MaterialTheme.typography.headlineSmall)
        Text(info.description)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return utcTimeMillis >= calendar.timeInMillis
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= Calendar.getInstance().get(Calendar.YEAR)
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}

@Composable
fun HomeScreenScheduleList(modifier: Modifier = Modifier, times: List<ScheduleTimeLineItemData>) {
    LazyColumn(modifier = modifier) {
        items(times.size) { index ->
            ScheduleTimeLineItem(
                data = times[index]
            )
            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}
