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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme

data class DepartureTimeCardData(
    val timeValue: String = "",
    val progressDescription: String = "",
    val arrivalTime: String = "",
    val stops: List<String> = emptyList()
)

@Composable
fun DepartureTimeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
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
            Text(
                "NEXT TRAIN LEAVING IN",
                style = MaterialTheme.typography.labelLargeEmphasized,
            )
            FallingPixelNumberDisplay(
                number = data.timeValue
            )
            Text(
                data.progressDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (data.arrivalTime.isNotEmpty()) {
                Text(
                    "Arrive at: ${data.arrivalTime}",
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
