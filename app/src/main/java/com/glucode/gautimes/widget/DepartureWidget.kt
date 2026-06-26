package com.glucode.gautimes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.glucode.gautimes.MainActivity
import com.glucode.gautimes.ui.theme.amberLED
import com.glucode.gautimes.utils.DateUtils
import dagger.hilt.android.EntryPointAccessors

class DepartureWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DepartureWidgetEntryPoint::class.java
        )
        val state = entryPoint.getDepartureWidgetStateUseCase().invoke()

        provideContent {
            DepartureWidgetContent(
                state = state,
                openHomeIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
    }
}

@Composable
private fun DepartureWidgetContent(
    state: DepartureWidgetState,
    openHomeIntent: Intent
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF111827)))
            .clickable(actionStartActivity(openHomeIntent))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            DepartureWidgetState.Loading -> StatusContent(
                title = "NEXT TRAIN",
                value = "--",
                message = "Loading"
            )

            is DepartureWidgetState.Active -> ActiveContent(state)
            is DepartureWidgetState.Empty -> StatusContent(
                title = state.routeLabel,
                value = "--",
                message = state.message
            )

            is DepartureWidgetState.RefreshFailed -> StatusContent(
                title = state.routeLabel,
                value = "--",
                message = state.message
            )
        }
    }
}

@Composable
private fun ActiveContent(state: DepartureWidgetState.Active) {
    Text(
        text = state.routeLabel,
        style = TextStyle(
            color = ColorProvider(Color(0xFFE5E7EB)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = state.minutesUntilDeparture.toString(),
        style = TextStyle(
            color = ColorProvider(amberLED),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    Text(
        text = "minutes until departure",
        style = TextStyle(
            color = ColorProvider(Color(0xFF9CA3AF)),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = "Arrive ${DateUtils.formatIsoTime(state.arrivalTime)}",
        style = TextStyle(
            color = ColorProvider(Color(0xFFD1D5DB)),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    if (state.secondaryDepartures.isNotEmpty()) {
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Next",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9CA3AF)),
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = state.secondaryDepartures.joinToString(", ") { DateUtils.formatIsoTime(it) },
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE5E7EB)),
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusContent(
    title: String,
    value: String,
    message: String
) {
    Text(
        text = title,
        style = TextStyle(
            color = ColorProvider(Color(0xFFE5E7EB)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(6.dp))
    Text(
        text = value,
        style = TextStyle(
            color = ColorProvider(Color(0xFFFFFFFF)),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
    Text(
        text = message,
        style = TextStyle(
            color = ColorProvider(Color(0xFF9CA3AF)),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        ),
        maxLines = 2
    )
}
