package com.glucode.gautimes.components.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.glucode.gautimes.service.NotificationService

@Composable
fun rememberReminderState(): ReminderState {
    return remember { ReminderState() }
}

class ReminderState {
    var showReminderDialog by mutableStateOf(false)
    var selectedReminder by mutableStateOf<ReminderInfo?>(null)
    var scheduleForReminder by mutableStateOf<List<Pair<String, String>>>(emptyList())

    fun triggerReminder(info: ReminderInfo, schedule: List<Pair<String, String>>) {
        selectedReminder = info
        scheduleForReminder = schedule
        showReminderDialog = true
    }
}

@Composable
fun ReminderHandler(
    state: ReminderState
) {
    val localContext = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            state.selectedReminder?.let { info ->
                NotificationService.start(
                    localContext,
                    info.from,
                    info.to,
                    state.scheduleForReminder
                )
            }
        }
    }

    if (state.showReminderDialog) {
        ReminderDialog(
            onDismiss = { state.showReminderDialog = false },
            onSetReminder = {
                val info = state.selectedReminder
                if (info != null) {
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
                                state.scheduleForReminder
                            )
                        }
                    } else {
                        NotificationService.start(
                            localContext,
                            info.from,
                            info.to,
                            state.scheduleForReminder
                        )
                    }
                }
            }
        )
    }
}
