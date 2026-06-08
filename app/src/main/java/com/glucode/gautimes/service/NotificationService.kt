package com.glucode.gautimes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.glucode.gautimes.MainActivity
import com.glucode.gautimes.R
import com.glucode.gautimes.utils.DateUtils
import java.time.Duration
import java.time.OffsetDateTime

class NotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var from: String = ""
    private var to: String = ""
    private var schedule: List<Pair<String, String>> = emptyList()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        from = intent?.getStringExtra(EXTRA_FROM) ?: ""
        to = intent?.getStringExtra(EXTRA_TO) ?: ""
        
        val rawSchedule = intent?.getStringArrayListExtra(EXTRA_SCHEDULE) ?: emptyList()
        schedule = rawSchedule.mapNotNull {
            val parts = it.split("|")
            if (parts.size == 2) parts[0] to parts[1] else null
        }

        createNotificationChannel()
        
        val notification = createNotification()
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        scheduleUpdate()

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trip Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows live updates for your upcoming train journey"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification? {
        val now = OffsetDateTime.now()
        
        // Find the first journey that hasn't departed yet
        val currentJourney = schedule.firstOrNull { (dep, _) ->
            try {
                !OffsetDateTime.parse(dep).isBefore(now)
            } catch (_: Exception) {
                false
            }
        }

        if (currentJourney == null) {
            return null
        }

        val departureTime = currentJourney.first
        val arrivalTime = currentJourney.second

        val departure = try {
            OffsetDateTime.parse(departureTime)
        } catch (_: Exception) {
            now
        }
        val duration = Duration.between(now, departure)
        val minutesLeft = duration.toMinutes()
        val chronometerTime = departure.toInstant().toEpochMilli()
        val shortText = if (minutesLeft > 0) "${minutesLeft}m" else "Now"

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use platform builder for API 35+ to ensure Live Update features are triggered
        if (Build.VERSION.SDK_INT >= 35) {
            val builder = Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("$from to $to")
                .setContentText("Arrives at ${DateUtils.formatIsoTime(arrivalTime)}")
                .setSmallIcon(R.drawable.train)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(Notification.BigTextStyle().bigText("Arrives at ${DateUtils.formatIsoTime(arrivalTime)}"))
                .setWhen(chronometerTime)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setDeleteIntent(stopPendingIntent)
                .addAction(
                    Notification.Action.Builder(
                        null, "Train caught!", stopPendingIntent
                    ).build()
                )

            // Use reflection to call the newest methods to avoid any compile-time API level issues
            try {
                builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
                builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                    .invoke(builder, shortText)
            } catch (_: Exception) {
            }
            return builder.build()
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$from to $to")
            .setContentText("Arrives at ${DateUtils.formatIsoTime(arrivalTime)}")
            .setSmallIcon(R.drawable.train)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle())
            .setWhen(chronometerTime)
            .setUsesChronometer(true)
            .setDeleteIntent(stopPendingIntent)
            .addAction(0, "Train caught!", stopPendingIntent)

        // Enable countdown if supported by the OS
        builder.getExtras().putBoolean("android.chronometerCountDown", true)

        return builder.build()
    }

    private fun scheduleUpdate() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = object : Runnable {
            override fun run() {
                val notification = createNotification()
                if (notification != null) {
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    handler.postDelayed(this, 10000)
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        handler.postDelayed(updateRunnable!!, 10000)
    }

    override fun onDestroy() {
        updateRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "trip_reminders_v3"
        
        const val ACTION_STOP = "STOP_NOTIFICATION"
        const val EXTRA_FROM = "EXTRA_FROM"
        const val EXTRA_TO = "EXTRA_TO"
        const val EXTRA_SCHEDULE = "EXTRA_SCHEDULE"

        fun start(context: Context, from: String, to: String, schedule: List<Pair<String, String>>) {
            val intent = Intent(context, NotificationService::class.java).apply {
                putExtra(EXTRA_FROM, from)
                putExtra(EXTRA_TO, to)
                val rawSchedule = ArrayList(schedule.map { "${it.first}|${it.second}" })
                putStringArrayListExtra(EXTRA_SCHEDULE, rawSchedule)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, NotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
