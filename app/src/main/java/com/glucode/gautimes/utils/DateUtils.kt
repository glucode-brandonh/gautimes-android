package com.glucode.gautimes.utils

import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object DateUtils {
    fun formatDateLabel(millis: Long): String {
        val selectedCalendar = Calendar.getInstance().apply { timeInMillis = millis }
        val todayCalendar = Calendar.getInstance()
        val nextWeekCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        return when {
            isSameDay(selectedCalendar, todayCalendar) -> "Today"
            selectedCalendar.after(todayCalendar) && selectedCalendar.before(nextWeekCalendar) -> {
                SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedCalendar.time)
            }
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(selectedCalendar.time)
        }
    }

    fun formatIsoTime(isoTime: String): String {
        return try {
            val odt = OffsetDateTime.parse(isoTime)
            val adjusted = odt.plusHours(2)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            adjusted.format(formatter)
        } catch (e: Exception) {
            "00:00"
        }
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
