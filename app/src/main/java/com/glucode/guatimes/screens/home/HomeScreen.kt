package com.glucode.guatimes.screens.home

import android.R
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.guatimes.components.ProgressCard
import com.glucode.guatimes.components.ScheduleTimeLineItem
import com.glucode.guatimes.components.ScheduleTimeLineItemData

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewmodel: HomeViewmodel = hiltViewModel()) {
    val uiState by viewmodel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is HomeState.Loading -> {
                CircularProgressIndicator()
            }

            is HomeState.Error -> {
                Text(text = "Error: ${state.message}")
            }

            is HomeState.HasData -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    AssistChip(onClick = {}, label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = "Calendar"
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Today",  style = MaterialTheme.typography.titleMedium)
                        }
                    })
                    Text("From", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Sandton",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("To", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Hatfield",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    ProgressCard(data = state.data.progress, onClick = {})
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(state.data.infoText.title, style = MaterialTheme.typography.headlineSmall)
                    Text(state.data.infoText.description)
                    Spacer(modifier = Modifier.size(8.dp))
                    HomeScreenScheduleList(times = state.data.scheduleTimes)
                }
            }
        }
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

@Preview
@Composable
fun HomeScreenPreview(modifier: Modifier = Modifier) {
    HomeScreen()
}