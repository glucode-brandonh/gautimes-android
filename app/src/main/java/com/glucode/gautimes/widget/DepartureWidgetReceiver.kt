package com.glucode.gautimes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DepartureWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DepartureWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DepartureWidgetUpdateScheduler.scheduleNextTick(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        DepartureWidgetUpdateScheduler.scheduleNextTick(context)
    }

    override fun onDisabled(context: Context) {
        DepartureWidgetUpdateScheduler.cancel(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != DepartureWidgetUpdateScheduler.ACTION_REFRESH) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                DepartureWidget().updateAll(context)
                DepartureWidgetUpdateScheduler.scheduleNextTick(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
