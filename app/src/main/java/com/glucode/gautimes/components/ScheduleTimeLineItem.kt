package com.glucode.gautimes.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.glucode.gautimes.R
import com.glucode.gautimes.ui.theme.GautimesTheme
import com.glucode.gautimes.ui.theme.cartGray
import com.glucode.gautimes.ui.theme.cartYellow
import com.glucode.gautimes.utils.pulse

data class ScheduleTimeLineItemData(
    val timeText: String = "00:00",
    val cartColor: Color = Color.White,
    val cartNumber: Int = 0
)

@Composable
fun ScheduleTimeLineItemSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pulse(),
        colors = CardDefaults.cardColors()
    ) {
    }
}

@Preview
@Composable
fun ScheduleTimeLineItemSkeletonPreview() {
    GautimesTheme {
        ScheduleTimeLineItemSkeleton()
    }
}

@Composable
fun ScheduleTimeLineItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    data: ScheduleTimeLineItemData = ScheduleTimeLineItemData()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = data.timeText,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                },
                label = "TimeAnimation"
            ) { targetTime ->
                Text(text = targetTime, style = MaterialTheme.typography.titleLarge)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = data.cartColor,
                    ),
                    border = null,
                    shape = CircleShape,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.train),
                                tint = MaterialTheme.colorScheme.background,
                                contentDescription = ""
                            )

                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                data.cartNumber.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    },
                    onClick = {}
                )

                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Preview
@Composable
fun ScheduleTimeLineItemPreview(modifier: Modifier = Modifier) {
    GautimesTheme {
        ScheduleTimeLineItem(data = times.first())
    }
}

@Preview
@Composable
fun ScheduleTimeLineItemListPreview(modifier: Modifier = Modifier) {
    GautimesTheme {
        LazyColumn {
            items(times.size) { index ->
                ScheduleTimeLineItem(
                    data = times[index]
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

private val times = listOf(
    ScheduleTimeLineItemData(timeText = "06:15", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "07:00", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "08:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "09:45", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "11:00", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "12:15", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "13:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "14:45", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "16:00", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "17:15", cartColor = cartGray, cartNumber = 8),
    ScheduleTimeLineItemData(timeText = "18:30", cartColor = cartYellow, cartNumber = 4),
    ScheduleTimeLineItemData(timeText = "19:45", cartColor = cartGray, cartNumber = 8)
)
