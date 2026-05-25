package com.glucode.guatimes.components

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
import com.glucode.guatimes.ui.theme.GuatimesTheme

data class ProgressCardData(
    val progressTitleTime: String = "",
    val progressDescription: String = "",
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
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(data.progressTitleTime, style = MaterialTheme.typography.displayLarge)
            Text(data.progressDescription)
        }
    }
}

@Preview
@Composable
fun ProgressCardPreview(modifier: Modifier = Modifier) {
    GuatimesTheme {
        ProgressCard(
            data = ProgressCardData(
                progressTitleTime = "20 Min",
                progressDescription = "until arrive"
            ), onClick = {})
    }
}
