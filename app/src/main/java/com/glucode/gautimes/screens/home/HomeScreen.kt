package com.glucode.gautimes.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
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
            is HomeState.HasData -> HomeContent(data = state.data)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(data: HomeData) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

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
                Text("Today", style = MaterialTheme.typography.titleMedium)
            }
        })

        LocationSection(
            data = data.locationSection.copy(
                onLocationChange = {
                    showLocationSheet = true
                },
            )
        )

        Spacer(modifier = Modifier.size(8.dp))
        ProgressCard(data = data.progress, onClick = {})
        Spacer(modifier = Modifier.size(8.dp))

        InfoSection(info = data.infoText)

        Spacer(modifier = Modifier.size(8.dp))
        HomeScreenScheduleList(times = data.scheduleTimes)

        if (showDatePicker) {
            DatePickerModal(
                onDateSelected = { /* Handle date selection */ },
                onDismiss = { showDatePicker = false }
            )
        }

        if (showLocationSheet) {
            LocationSelectorBottomSheet(
                data = LocationSelectorBottomSheetData(locations = data.locationSection.locations),
                onDismissRequest = { showLocationSheet = false },
                onLocationSelected = { location ->
                    showLocationSheet = false
                }
            )
        }
    }
}

@Composable
fun LocationSection(data: HomeLocationSelectionData) {
    LocationTargetSection(
        targetLabel = LocationTarget.FROM.label,
        locationName = data.fromLocation,
        onClick = {
            data.onLocationChange(LocationTarget.FROM)
        })

    LocationTargetSection(
        targetLabel = LocationTarget.TO.label,
        locationName = data.toLocation,
        onClick = {
            data.onLocationChange(LocationTarget.FROM)
        })

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
    val datePickerState = rememberDatePickerState()

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
        DatePicker(state = datePickerState)
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
