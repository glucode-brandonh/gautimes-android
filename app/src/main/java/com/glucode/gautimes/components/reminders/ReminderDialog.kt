package com.glucode.gautimes.components.reminders

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ReminderDialog(
    onDismiss: () -> Unit,
    onSetReminder: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSetReminder()
                onDismiss()
            }) {
                Text("Get Updates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        title = {
            Text(
                text = "Live Trip Updates",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Stay informed with real-time notifications for this journey. We'll send you live updates on departure times and any status changes directly to your device.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}
