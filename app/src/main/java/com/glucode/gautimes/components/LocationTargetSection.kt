package com.glucode.gautimes.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme

@Composable
fun LocationTargetSection(
    targetLabel: String,
    locationName: String,
    isNear: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(targetLabel, style = MaterialTheme.typography.headlineSmall)
            if (!isNear) {
                Spacer(modifier = Modifier.size(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Not near you") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFFF9800),
                        labelColor = Color.White,
                        leadingIconContentColor = Color.White
                    ),
                    border = null
                )
            }
        }
        AssistChip(onClick = onClick, label = {
            Text(
                locationName,
                style = MaterialTheme.typography.headlineLarge,
            )
        })
    }
}

@Preview(showBackground = true)
@Composable
fun LocationTargetSectionPreview() {
    GautimesTheme() {
        Column {
            LocationTargetSection("From", locationName = "Sandton", onClick = {})
            Spacer(modifier = Modifier.size(16.dp))
            LocationTargetSection("To", locationName = "Hatfield", isNear = false, onClick = {})
        }
    }
}
