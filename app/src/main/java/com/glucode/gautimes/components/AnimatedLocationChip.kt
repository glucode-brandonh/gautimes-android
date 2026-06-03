package com.glucode.gautimes.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AnimatedLocationChip(
    location: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationLabel: String = "LocationAnimation",
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier.animateContentSize(),
        colors = AssistChipDefaults.assistChipColors(),
        label = {
            AnimatedContent(
                targetState = location,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                },
                label = animationLabel
            ) { targetLocation ->
                Text(
                    text = targetLocation,
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
        }
    )
}
