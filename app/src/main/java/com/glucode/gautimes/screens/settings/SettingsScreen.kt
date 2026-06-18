package com.glucode.gautimes.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.gautimes.components.LocationSelectorBottomSheet
import com.glucode.gautimes.components.LocationSelectorBottomSheetData
import com.glucode.gautimes.screens.home.LocationTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showWipeConfirm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            viewModel.onAction(SettingsAction.PermissionChanged)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "General Defaults",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            SettingsLocationItem(
                label = "Default From",
                stationId = uiState.userSettings.defaultFromId,
                stations = uiState.stations,
                onClick = {
                    viewModel.onAction(
                        SettingsAction.OpenLocationPicker(
                            SettingsLocationTarget.DEFAULT_FROM
                        )
                    )
                }
            )
            SettingsLocationItem(
                label = "Default To",
                stationId = uiState.userSettings.defaultToId,
                stations = uiState.stations,
                onClick = {
                    viewModel.onAction(
                        SettingsAction.OpenLocationPicker(
                            SettingsLocationTarget.DEFAULT_TO
                        )
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            ListItem(
                headlineContent = { Text("Use Schedule") },
                supportingContent = { Text("Automatically change defaults based on time of day") },
                trailingContent = {
                    Switch(
                        checked = uiState.userSettings.useSchedule,
                        onCheckedChange = { viewModel.onAction(SettingsAction.UpdateUseSchedule(it)) }
                    )
                }
            )

            if (uiState.userSettings.useSchedule) {
                Text(
                    "Morning Routine (05:00 - 12:00)",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsLocationItem(
                    label = "Morning From",
                    stationId = uiState.userSettings.morningFromId,
                    stations = uiState.stations,
                    onClick = {
                        viewModel.onAction(
                            SettingsAction.OpenLocationPicker(
                                SettingsLocationTarget.MORNING_FROM
                            )
                        )
                    }
                )
                SettingsLocationItem(
                    label = "Morning To",
                    stationId = uiState.userSettings.morningToId,
                    stations = uiState.stations,
                    onClick = {
                        viewModel.onAction(
                            SettingsAction.OpenLocationPicker(
                                SettingsLocationTarget.MORNING_TO
                            )
                        )
                    }
                )

                Text(
                    "Afternoon Routine (12:00 - 21:00)",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                SettingsLocationItem(
                    label = "Afternoon From",
                    stationId = uiState.userSettings.afternoonFromId,
                    stations = uiState.stations,
                    onClick = {
                        viewModel.onAction(
                            SettingsAction.OpenLocationPicker(
                                SettingsLocationTarget.AFTERNOON_FROM
                            )
                        )
                    }
                )
                SettingsLocationItem(
                    label = "Afternoon To",
                    stationId = uiState.userSettings.afternoonToId,
                    stations = uiState.stations,
                    onClick = {
                        viewModel.onAction(
                            SettingsAction.OpenLocationPicker(
                                SettingsLocationTarget.AFTERNOON_TO
                            )
                        )
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Permissions",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            ListItem(
                headlineContent = { Text("Location Access") },
                supportingContent = {
                    Text(
                        if (uiState.isLocationPermissionGranted) "Access Granted"
                        else "Access not granted. Tap to enable."
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (uiState.isLocationPermissionGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                },
                modifier = Modifier.clickable(enabled = !uiState.isLocationPermissionGranted) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Data",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            ListItem(
                headlineContent = { Text("Wipe Journeys Cache") },
                supportingContent = { Text("Clear all cached journey data from the database") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    showWipeConfirm = true
                }
            )
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Journeys Cache") },
            text = { Text("Are you sure you want to clear all cached journey data? This will require a network connection to view journeys again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(SettingsAction.WipeJourneys)
                        showWipeConfirm = false
                    }
                ) {
                    Text("Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showLocationSheet) {
        val selectedStationId = when (uiState.locationTarget) {
            SettingsLocationTarget.DEFAULT_FROM -> uiState.userSettings.defaultFromId
            SettingsLocationTarget.DEFAULT_TO -> uiState.userSettings.defaultToId
            SettingsLocationTarget.MORNING_FROM -> uiState.userSettings.morningFromId
            SettingsLocationTarget.MORNING_TO -> uiState.userSettings.morningToId
            SettingsLocationTarget.AFTERNOON_FROM -> uiState.userSettings.afternoonFromId
            SettingsLocationTarget.AFTERNOON_TO -> uiState.userSettings.afternoonToId
        }
        val selectedStationName = uiState.stations.find { it.id == selectedStationId }?.name ?: ""

        LocationSelectorBottomSheet(
            data = LocationSelectorBottomSheetData(
                locations = uiState.stations.map { it.name },
                selectedLocation = selectedStationName,
                locationTarget = LocationTarget.FROM // Mock target for reuse
            ),
            onDismissRequest = { name, _ ->
                val stationId = uiState.stations.find { it.name == name }?.id
                if (stationId != null) {
                    viewModel.onAction(
                        SettingsAction.SelectLocation(
                            uiState.locationTarget,
                            stationId
                        )
                    )
                } else {
                    viewModel.onAction(SettingsAction.CloseLocationPicker)
                }
            }
        )
    }
}

@Composable
fun SettingsLocationItem(
    label: String,
    stationId: String?,
    stations: List<com.glucode.gautimes.data.local.entities.StationEntity>,
    onClick: () -> Unit
) {
    val stationName = stations.find { it.id == stationId }?.name ?: "Select Station"
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(stationName) },
        modifier = Modifier.clickable { onClick() }
    )
}
