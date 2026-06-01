package com.glucode.gautimes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.ui.theme.GautimesTheme

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
    )
)

@Composable
fun BlockNumber(
    number: String,
    modifier: Modifier = Modifier,
    color: Color = AmberLED,
    blockSize: Dp = 10.dp,
    spacing: Dp = 1.dp,
    digitSpacing: Dp = 12.dp
) {
    Box(
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(digitSpacing),
            modifier = Modifier.blur(8.dp).padding(10.dp)
        ) {
            number.forEach { char ->
                val bitmap = digitBitmaps[char]
                if (bitmap != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing), modifier = Modifier) {
                        bitmap.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                row.forEach { bit ->
                                    Box(
                                        modifier = Modifier
                                            .size(blockSize)
                                            .background(if (bit == '#') color.copy(alpha = 0.7f) else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(digitSpacing),
        ) {
            number.forEach { char ->
                val bitmap = digitBitmaps[char]
                if (bitmap != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        bitmap.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                row.forEach { bit ->
                                    Box(
                                        modifier = Modifier
                                            .size(blockSize)
                                            .clip(CircleShape)
                                            .background(if (bit == '#') color else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BlockNumberPreview() {
    GautimesTheme {
        BlockNumber(number = "12:34")
    }
}
