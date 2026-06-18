package com.glucode.gautimes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme
import com.glucode.gautimes.utils.DateUtils

data class DepartureTimeCardData(
    val id: String = "",
    val timeValue: String = "",
    val progressDescription: String = "",
    val arrivalTime: String = "",
    val departureTime: String = "",
    val price: String? = null,
    val stops: List<String> = emptyList()
)

@Composable
fun DepartureTimeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onReminderClick: () -> Unit = {},
    data: DepartureTimeCardData = DepartureTimeCardData()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                data.price?.let { price ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = price,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    "NEXT TRAIN LEAVING IN",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )

                IconButton(
                    onClick = onReminderClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Set Reminder",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            FallingPixelNumberDisplay(
                number = data.timeValue
            )
            Text(
                data.progressDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Arrive at: ${if (data.arrivalTime.isNotEmpty()) DateUtils.formatIsoTime(data.arrivalTime) else "- -"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
fun DepartureTimeCardPreview(modifier: Modifier = Modifier) {
    GautimesTheme {
        DepartureTimeCard(
            data = DepartureTimeCardData(
                timeValue = "17",
                progressDescription = "minutes until departure"
            ), onClick = {})
    }
}
