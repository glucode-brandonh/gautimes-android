package com.glucode.gautimes.screens.home.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.screens.home.HealthCheckState
import com.glucode.gautimes.screens.home.JourneysCheckState
import com.glucode.gautimes.screens.home.StationsCheckState
import java.util.Locale

@Composable
fun LocationDebugChip(
    lat: Double,
    lon: Double,
    onClick: () -> Unit
) {
    val label =
        "Loc: ${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)}"

    AssistChip(onClick = onClick, label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = label
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    })
}

@Composable
fun HealthStatusChip(
    healthCheck: HealthCheckState,
    onClick: () -> Unit
) {
    val (icon, label) = when (healthCheck) {
        HealthCheckState.Checking -> Icons.Default.Sync to "API: checking"
        is HealthCheckState.Online -> Icons.Default.CloudDone to "API: online"
        is HealthCheckState.Offline -> Icons.Default.CloudOff to "API: offline"
    }

    AssistChip(onClick = onClick, label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    })
}

@Composable
fun StationsStatusChip(
    stationsCheck: StationsCheckState,
    onClick: () -> Unit
) {
    val (icon, label) = when (stationsCheck) {
        StationsCheckState.Checking -> Icons.Default.Sync to "Stations: checking"
        is StationsCheckState.Loaded -> Icons.Default.CloudDone to "Stations: ${stationsCheck.count}"
        is StationsCheckState.Failed -> Icons.Default.CloudOff to "Stations: offline"
    }

    AssistChip(onClick = onClick, label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    })
}

@Composable
fun JourneysStatusChip(
    journeysCheck: JourneysCheckState,
    onClick: () -> Unit
) {
    val (icon, label) = when (journeysCheck) {
        JourneysCheckState.Idle -> Icons.Default.Sync to "Journeys: idle"
        JourneysCheckState.Checking -> Icons.Default.Sync to "Journeys: checking"
        is JourneysCheckState.Loaded -> Icons.Default.CloudDone to "Journeys: ${journeysCheck.count}"
        is JourneysCheckState.Failed -> Icons.Default.CloudOff to "Journeys: offline"
    }

    AssistChip(onClick = onClick, label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    })
}

@Composable
fun CacheModeChip(
    isCachingEnabled: Boolean,
    onClick: () -> Unit
) {
    val label = if (isCachingEnabled) "Cache: on" else "Cache: off"

    AssistChip(onClick = onClick, label = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = label
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    })
}