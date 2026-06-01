package com.glucode.gautimes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme

data class ProgressCardData(
    val timeValue: String = "",
    val progressDescription: String = "",
    val arrivalTime: String = "",
    val stops: List<String> = emptyList()
)

@Composable
fun ProgressCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    data: ProgressCardData = ProgressCardData()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "NEXT TRAIN LEAVING IN",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            BlockNumber(
                number = data.timeValue
            )
            Text(
                data.progressDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (data.arrivalTime.isNotEmpty()) {
                Text(
                    "Arrive at: ${data.arrivalTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview
@Composable
fun ProgressCardPreview(modifier: Modifier = Modifier) {
    GautimesTheme {
        ProgressCard(
            data = ProgressCardData(
                timeValue = "17",
                progressDescription = "minutes until departure"
            ), onClick = {})
    }
}
