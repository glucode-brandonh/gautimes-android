package com.glucode.gautimes.screens.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.components.AnimatedLocationChip
import com.glucode.gautimes.screens.home.LocationTarget

@Composable
fun LocationSelection(
    modifier: Modifier = Modifier,
    fromLocation: String,
    toLocation: String,
    isFromNear: Boolean = true,
    onLocationChange: (target: LocationTarget) -> Unit,
    onFlipLocations: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(LocationTarget.FROM.label, style = MaterialTheme.typography.titleMedium)
            AnimatedVisibility(
                visible = !isFromNear,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.size(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("Not the nearest to you") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = null
                    )
                }
            }
        }
        AnimatedLocationChip(
            location = fromLocation,
            onClick = { onLocationChange(LocationTarget.FROM) },
            animationLabel = "FromLocationAnimation"
        )

        Spacer(modifier = Modifier.size(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(LocationTarget.TO.label, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.size(8.dp))
            AssistChip(
                onClick = onFlipLocations,
                label = { Text("Swap") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(),
                border = null
            )
        }
        AnimatedLocationChip(
            location = toLocation,
            onClick = { onLocationChange(LocationTarget.TO) },
            animationLabel = "ToLocationAnimation"
        )
    }
}
