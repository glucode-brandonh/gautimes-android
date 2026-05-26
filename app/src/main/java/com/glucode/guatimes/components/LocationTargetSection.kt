package com.glucode.guatimes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.glucode.guatimes.ui.theme.GuatimesTheme

@Composable
fun LocationTargetSection(targetLabel: String, locationName: String, onClick: () -> Unit) {
    Column {
        Text(targetLabel, style = MaterialTheme.typography.headlineSmall)
        AssistChip(onClick = onClick, label = {
            Text(
                locationName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        })
    }
}

@Preview(showBackground = true)
@Composable
fun LocationTargetSectionPreview() {
    GuatimesTheme() {
        LocationTargetSection("From", locationName = "Sandton", onClick = {})
    }
}