package com.glucode.gautimes.screens.tripdetails

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.gautimes.components.DepartureTimeCard
import com.glucode.gautimes.components.reminders.ReminderHandler
import com.glucode.gautimes.components.reminders.rememberReminderState
import com.glucode.gautimes.components.sharing.ShareHandler
import com.glucode.gautimes.components.sharing.rememberShareState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    onBackClick: () -> Unit = {},
    viewModel: TripDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val localContext = LocalContext.current
    val reminderState = rememberReminderState()
    val shareState = rememberShareState()

    ReminderHandler(state = reminderState)
    ShareHandler(state = shareState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    uiState.shareInfo?.let { info ->
                        if (info.latitude != null && info.longitude != null) {
                            IconButton(onClick = {
                                val uri = "geo:${info.latitude},${info.longitude}?q=${info.latitude},${info.longitude}"
                                val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
                                localContext.startActivity(intent)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = "Navigate to station"
                                )
                            }
                        }
                        IconButton(onClick = {
                            shareState.share(info)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }
                    }
                    uiState.reminderInfo?.let { info ->
                        IconButton(onClick = {
                            reminderState.triggerReminder(info, uiState.schedule)
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = "Set Reminder"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item{
                        DepartureTimeCard(
                            showReminderButton = false,
                            data = uiState.progress
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Stations",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            uiState.stations.forEachIndexed { index, station ->
                                StationItem(
                                    station = station,
                                    isLast = index == uiState.stations.size - 1
                                )
                            }
                            if (uiState.stations.isEmpty()) {
                                Text(
                                    text = "No stations found for this journey.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationItem(station: StationUiData, isLast: Boolean) {
    val fontWeight = if (station.isBold) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            if (!isLast) {
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.primary,
                    thickness = 2.dp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 0.dp else 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = fontWeight
            )
            Text(
                text = station.arrivalTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = fontWeight
            )
        }
    }
}
