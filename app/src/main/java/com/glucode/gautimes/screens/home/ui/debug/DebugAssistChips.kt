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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

private val SuccessGreen = Color(0xFF81C784)

@Composable
fun LocationDebugChip(
    lat: Double,
    lon: Double,
    onClick: () -> Unit
) {
    val label =
        "Loc: ${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)}"

    AssistChip(
        onClick = onClick,
        label = {
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
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun HealthStatusChip(
    healthCheck: HealthCheckState,
    onClick: () -> Unit
) {
    val (icon, label, isSuccess) = when (healthCheck) {
        HealthCheckState.Checking -> Triple(Icons.Default.Sync, "API: checking", false)
        is HealthCheckState.Online -> Triple(Icons.Default.CloudDone, "API: online", true)
        is HealthCheckState.Offline -> Triple(Icons.Default.CloudOff, "API: offline", false)
    }

    AssistChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun StationsStatusChip(
    stationsCheck: StationsCheckState,
    onClick: () -> Unit
) {
    val (icon, label, isSuccess) = when (stationsCheck) {
        StationsCheckState.Checking -> Triple(Icons.Default.Sync, "Stations: checking", false)
        is StationsCheckState.Loaded -> Triple(Icons.Default.CloudDone, "Stations: ${stationsCheck.count}", true)
        is StationsCheckState.Failed -> Triple(Icons.Default.CloudOff, "Stations: offline", false)
    }

    AssistChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun JourneysStatusChip(
    journeysCheck: JourneysCheckState,
    onClick: () -> Unit
) {
    val (icon, label, isSuccess) = when (journeysCheck) {
        JourneysCheckState.Idle -> Triple(Icons.Default.Sync, "Journeys: idle", false)
        JourneysCheckState.Checking -> Triple(Icons.Default.Sync, "Journeys: checking", false)
        is JourneysCheckState.Loaded -> Triple(Icons.Default.CloudDone, "Journeys: ${journeysCheck.count}", true)
        is JourneysCheckState.Failed -> Triple(Icons.Default.CloudOff, "Journeys: offline", false)
    }

    AssistChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSuccess) SuccessGreen else MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun CacheModeChip(
    isCachingEnabled: Boolean,
    onClick: () -> Unit
) {
    val label = if (isCachingEnabled) "Cache: on" else "Cache: off"

    AssistChip(
        onClick = onClick,
        label = {
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
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}