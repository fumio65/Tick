package com.example.tick.util

import java.util.Calendar

var tempCalendar: Calendar? = null

fun calendarStoreDateOnly(dateMillis: Long) {
    val cal = Calendar.getInstance()
    cal.timeInMillis = dateMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    tempCalendar = cal
}

fun calendarApplyTime(hour: Int, minute: Int): Long {
    val cal = tempCalendar ?: Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val result = cal.timeInMillis
    tempCalendar = null
    return result
}

fun formatDueDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$month/$day/$year  $hour:$minute"
}
