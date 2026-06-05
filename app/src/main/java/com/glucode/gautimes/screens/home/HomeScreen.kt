package com.glucode.gautimes.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucode.gautimes.BuildConfig
import com.glucode.gautimes.components.DepartureTimeCard
import com.glucode.gautimes.components.LocationPermissionInfoCard
import com.glucode.gautimes.components.LocationSelectorBottomSheet
import com.glucode.gautimes.components.ScheduleTimeLineItem
import com.glucode.gautimes.components.ScheduleTimeLineItemSkeleton
import com.glucode.gautimes.components.StatusMessage
import com.glucode.gautimes.data.repository.JourneyResult
import com.glucode.gautimes.screens.home.ui.DatePickerModal
import com.glucode.gautimes.screens.home.ui.LocationSelection
import com.glucode.gautimes.screens.home.ui.ReminderDialog
import com.glucode.gautimes.service.NotificationService
import com.glucode.gautimes.screens.home.ui.debug.CacheModeChip
import com.glucode.gautimes.screens.home.ui.debug.HealthStatusChip
import com.glucode.gautimes.screens.home.ui.debug.JourneysStatusChip
import com.glucode.gautimes.screens.home.ui.debug.LocationDebugChip
import com.glucode.gautimes.screens.home.ui.debug.StationsStatusChip
import java.time.OffsetDateTime

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewmodel: HomeViewmodel = hiltViewModel(),
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewmodel.uiState.collectAsState()
    val localContext = LocalContext.current

    LaunchedEffect(Unit) {
        viewmodel.uiEffect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowError -> {
                    Toast.makeText(localContext, effect.message, Toast.LENGTH_SHORT).show()
                }

                HomeEffect.RunNotificationTest -> {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guatimes") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is HomeState.Loading -> CircularProgressIndicator()
                is HomeState.Error -> Text(text = "Error: ${state.message}")
                is HomeState.HasData -> HomeContent(data = state.data, viewmodel = viewmodel)
            }
        }
    }
}

data class ReminderInfo(
    val from: String,
    val to: String,
    val departureTime: String,
    val arrivalTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(data: HomeData, viewmodel: HomeViewmodel) {
    val localContext = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<ReminderInfo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewmodel.onAction(HomeAction.SetGrantingPermission(false))
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewmodel.onAction(HomeAction.RefreshLocation)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            selectedReminder?.let { info ->
                val scheduleList = data.scheduleTimes
                    .filter { it.departureTime >= info.departureTime }
                    .map { it.departureTime to it.arrivalTime }
                NotificationService.start(
                    localContext,
                    info.from,
                    info.to,
                    scheduleList
                )
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = data.isRefreshing,
        onRefresh = { viewmodel.onAction(HomeAction.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                HomeAssistChips(
                    data = data,
                    onDateClick = { showDatePicker = true },
                    onAction = viewmodel::onAction
                )

                if (data.showLocationPermissionCard) {
                    Spacer(modifier = Modifier.size(8.dp))
                    LocationPermissionInfoCard(
                        isLoading = data.isGrantingPermission,
                        onGrantAccessClick = {
                            viewmodel.onAction(HomeAction.SetGrantingPermission(true))
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onDismissClick = {
                            viewmodel.onAction(HomeAction.DismissLocationPermissionCard)
                        }
                    )
                }

                LocationSelection(
                    fromLocation = data.fromLocation,
                    toLocation = data.toLocation,
                    isFromNear = data.isFromNear,
                    onLocationChange = { target ->
                        viewmodel.onAction(HomeAction.ToggleLocationSheet(true, target))
                    },
                    onFlipLocations = {
                        viewmodel.onAction(HomeAction.FlipLocations)
                    }
                )

                Spacer(modifier = Modifier.size(8.dp))
                DepartureTimeCard(
                    data = data.progress,
                    onClick = { viewmodel.onAction(HomeAction.RefreshJourneys(force = true)) },
                    onReminderClick = {
                        selectedReminder = ReminderInfo(
                            from = data.fromLocation,
                            to = data.toLocation,
                            departureTime = data.progress.departureTime,
                            arrivalTime = data.progress.arrivalTime
                        )
                        showReminderDialog = true
                    }
                )
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
                        StatusMessage(message = journeyResult.message)
                    }
                }

                is JourneyResult.Success -> {
                    if (data.scheduleTimes.isEmpty()) {
                        item {
                            StatusMessage(message = "No schedule found")
                        }
                    } else {
                        items(data.scheduleTimes, key = { it.id }) { itemData ->
                            ScheduleTimeLineItem(
                                modifier = Modifier.animateItem(),
                                data = itemData,
                                onClick = {
                                    selectedReminder = ReminderInfo(
                                        from = data.fromLocation,
                                        to = data.toLocation,
                                        departureTime = itemData.departureTime,
                                        arrivalTime = itemData.arrivalTime
                                    )
                                    showReminderDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }

                        if (data.nextCursor != null) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (data.isFetchingMore) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        TextButton(
                                            onClick = {
                                                viewmodel.onAction(
                                                    HomeAction.LoadMore(data.nextCursor)
                                                )
                                            }
                                        ) {
                                            Text("Load More")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { viewmodel.onAction(HomeAction.UpdateDate(it)) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            onDismiss = { showReminderDialog = false },
            onSetReminder = {
                val info = selectedReminder
                if (info != null) {
                    val scheduleList = data.scheduleTimes
                        .filter { it.departureTime >= info.departureTime }
                        .map { it.departureTime to it.arrivalTime }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                localContext,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationService.start(
                                localContext,
                                info.from,
                                info.to,
                                scheduleList
                            )
                        }
                    } else {
                        NotificationService.start(
                            localContext,
                            info.from,
                            info.to,
                            scheduleList
                        )
                    }
                }
            }
        )
    }

    if (data.showLocationSheet) {
        LocationSelectorBottomSheet(
            data = data.locationSection,
            onDismissRequest = { location, target ->
                if (target == LocationTarget.FROM) {
                    viewmodel.onAction(HomeAction.UpdateFromLocation(location))
                } else {
                    viewmodel.onAction(HomeAction.UpdateToLocation(location))
                }
                viewmodel.onAction(HomeAction.ToggleLocationSheet(false, target))
            },
        )
    }
}

@Composable
private fun HomeAssistChips(
    data: HomeData,
    onDateClick: () -> Unit,
    onAction: (HomeAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(onClick = onDateClick, label = {
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
                onClick = { onAction(HomeAction.RefreshHealth) }
            )
            StationsStatusChip(
                stationsCheck = data.stationsCheck,
                onClick = { onAction(HomeAction.RefreshStations) }
            )
            JourneysStatusChip(
                journeysCheck = data.journeysCheck,
                onClick = { onAction(HomeAction.RefreshJourneys(force = true)) }
            )
            AssistChip(
                onClick = { onAction(HomeAction.TestNotification) },
                label = { Text("🔔 Test Notification") }
            )
            CacheModeChip(
                isCachingEnabled = data.isProbeCachingEnabled,
                onClick = { onAction(HomeAction.ToggleProbeCaching) }
            )
            if (data.currentLat != null && data.currentLong != null) {
                LocationDebugChip(
                    lat = data.currentLat,
                    lon = data.currentLong,
                    onClick = { onAction(HomeAction.RefreshLocation) }
                )
            }
        }
    }
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
