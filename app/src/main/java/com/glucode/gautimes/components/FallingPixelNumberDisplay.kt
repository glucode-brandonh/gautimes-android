package com.glucode.gautimes.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AmberLED = Color(0xFFFFB000)

private val digitBitmaps = mapOf(
    '0' to listOf(
        ".####.",
        "######",
        "##..##",
        "##..##",
        "##.###",
        "###.##",
        "##..##",
        "##..##",
        "######",
        ".####."
    ),
    '1' to listOf(
        "####..",
        "####..",
        "..##..",
        "..##..",
        "..##..",
        "..##..",
        "..##..",
        "..##..",
        "######",
        "######"
    ),
    '2' to listOf(
        "#####.",
        "######",
        "....##",
        "....##",
        ".#####",
        "######",
        "##....",
        "##....",
        "######",
        "######"
    ),
    '3' to listOf(
        "#####.",
        "######",
        "....##",
        "....##",
        "#####.",
        "#####.",
        "....##",
        "....##",
        "######",
        "#####."
    ),
    '4' to listOf(
        "##..##",
        "##..##",
        "##..##",
        "##..##",
        "######",
        "######",
        "....##",
        "....##",
        "....##",
        "....##"
    ),
    '5' to listOf(
        "######",
        "######",
        "##....",
        "##....",
        "#####.",
        "######",
        "....##",
        "....##",
        "######",
        "#####."
    ),
    '6' to listOf(
        ".#####",
        "######",
        "##....",
        "##....",
        "#####.",
        "######",
        "##..##",
        "##..##",
        "######",
        ".####."
    ),
    '7' to listOf(
        "######",
        "######",
        "##..##",
        "##..##",
        "....##",
        "....##",
        "....##",
        "....##",
        "....##",
        "....##"
    ),
    '8' to listOf(
        ".####.",
        "######",
        "##..##",
        "##..##",
        ".####.",
        ".####.",
        "##..##",
        "##..##",
        "######",
        ".####."
    ),
    '9' to listOf(
        ".####.",
        "######",
        "##..##",
        "##..##",
        "######",
        ".#####",
        "....##",
        "....##",
        "######",
        "#####."
    ),
    ' ' to listOf(
        "......",
        "......",
        "......",
        "......",
        "......",
        "......",
        "......",
        "......",
        "......",
        "......"
    ),
    '-' to listOf(
        "......",
        "......",
        "......",
        "......",
        "######",
        "......",
        "......",
        "......",
        "......",
        "......"
    ),
    ':' to listOf(
        "......",
        "..##..",
        "..##..",
        "......",
        "......",
        "......",
        "......",
        "..##..",
        "..##..",
        "......"
    ),
    '+' to listOf(
        "......",
        "..##..",
        "..##..",
        "..##..",
        "######",
        "######",
        "..##..",
        "..##..",
        "..##..",
        "......"
    )
)

@Composable
fun FallingPixelNumberDisplay(
    number: String,
    modifier: Modifier = Modifier,
    color: Color = AmberLED,
    blockSize: Dp = 10.dp,
    spacing: Dp = 1.dp,
    digitSpacing: Dp = 0.dp,
    debounceMillis: Long = 300L
) {
    var debouncedNumber by remember { mutableStateOf(number) }

    LaunchedEffect(number) {
        if (debounceMillis > 0) {
            delay(debounceMillis.milliseconds)
        }
        debouncedNumber = number
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = debouncedNumber,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (slideInVertically { height -> -height } + fadeIn(tween(300)))
                    .togetherWith(slideOutVertically { height -> height } + fadeOut(tween(400)))
            },
            label = "NumberAnimation"
        ) { targetNumber ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(digitSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                targetNumber.forEach { char ->
                    Box {
                        DigitLayer(
                            targetChar = char,
                            color = color.copy(alpha = 0.7f),
                            blockSize = blockSize,
                            spacing = spacing,
                            isBlur = true
                        )
                        DigitLayer(
                            targetChar = char,
                            color = color,
                            blockSize = blockSize,
                            spacing = spacing,
                            isBlur = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitLayer(
    targetChar: Char,
    color: Color,
    blockSize: Dp,
    spacing: Dp,
    isBlur: Boolean
) {
    val bitmap = digitBitmaps[targetChar] ?: return
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = if (isBlur) Modifier
            .blur(8.dp)
            .padding(10.dp) else Modifier.padding(10.dp)
    ) {
        bitmap.forEachIndexed { rowIndex, rowText ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                rowText.forEach { bit ->
                    if (bit == '#') {
                        FallingBit(color, blockSize, rowIndex, isBlur)
                    } else {
                        Box(Modifier.size(blockSize))
                    }
                }
            }
        }
    }
}

@Composable
private fun FallingBit(
    color: Color,
    size: Dp,
    rowIndex: Int,
    isBlur: Boolean
) {
    val density = LocalDensity.current
    val yOffsetPx = remember { Animatable(with(density) { -150.dp.toPx() }) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay((rowIndex * 40L + (0..150).random()).milliseconds)
        launch {
            yOffsetPx.animateTo(0f, tween(durationMillis = 1000, easing = LinearOutSlowInEasing))
        }
        launch {
            alpha.animateTo(1f, tween(durationMillis = 500))
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                translationY = yOffsetPx.value
                this.alpha = alpha.value
            }
            .clip(if (isBlur) RectangleShape else CircleShape)
            .background(color)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun FallingPixelNumberDisplayPreview() {
    GautimesTheme {
        var number by remember { mutableStateOf("0") }
        LaunchedEffect(Unit) {
            var count = 0
            while (true) {
                delay(2000.milliseconds)
                // Simulate rapid changes that should be debounced
                number = "1"
                delay(100.milliseconds)
                number = "12"
                delay(100.milliseconds)
                number = (count % 100).toString()
                count++
            }
        }
        Box(modifier = Modifier.padding(40.dp)) {
            FallingPixelNumberDisplay(number = number)
        }
    }
}
