package com.glucode.gautimes.screens.home.ui.debug

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.gautimes.service.NotificationService
import java.time.OffsetDateTime

@Composable
fun DebugMenu(
    viewModel: DebugViewModel = hiltViewModel()
) {
    var expanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val localContext = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DebugEffect.ShowError -> {
                    Toast.makeText(localContext, effect.message, Toast.LENGTH_SHORT).show()
                }

                DebugEffect.RunNotificationTest -> {
                    val now = OffsetDateTime.now()
                    val dummySchedule = listOf(
                        now.plusSeconds(5).toString() to now.plusMinutes(5).toString(),
                        now.plusSeconds(35).toString() to now.plusMinutes(6).toString(),
                        now.plusSeconds(65).toString() to now.plusMinutes(7).toString()
                    )
                    NotificationService.start(
                        localContext,
                        "Test Station A",
                        "Test Station B",
                        dummySchedule
                    )
                }
            }
        }
    }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Outlined.BugReport, contentDescription = "Debug")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.padding(8.dp)
    ) {
        Column {
            Text("Debug Helpers", style = MaterialTheme.typography.titleSmall)

            HealthStatusChip(
                healthCheck = uiState.healthCheck,
                onClick = { viewModel.onAction(DebugAction.RefreshHealth) }
            )
            StationsStatusChip(
                stationsCheck = uiState.stationsCheck,
                onClick = { viewModel.onAction(DebugAction.RefreshStations) }
            )
            JourneysStatusChip(
                journeysCheck = uiState.journeysCheck,
                onClick = { viewModel.onAction(DebugAction.RefreshJourneys(force = true)) }
            )
            CacheModeChip(
                isCachingEnabled = uiState.isProbeCachingEnabled,
                onClick = { viewModel.onAction(DebugAction.ToggleProbeCaching) }
            )
            TestNotificationChip(
                onClick = { viewModel.onAction(DebugAction.TestNotification) },
            )

            if (uiState.currentLat != null && uiState.currentLong != null) {
                LocationDebugChip(
                    lat = uiState.currentLat!!,
                    lon = uiState.currentLong!!,
                    onClick = { viewModel.onAction(DebugAction.RefreshLocation) }
                )
            }
        }
    }
}
