package com.example.tick.uidesign

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.*

@Composable
fun DueDatePicker(
    currentTimestamp: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    currentTimestamp?.let { calendar.timeInMillis = it }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    OutlinedButton(onClick = {
        showDatePicker(context, year, month, day) { newDate ->
            showTimePicker(context, hour, minute) { newTime ->
                val final = Calendar.getInstance()
                final.timeInMillis = newDate
                val timeCal = Calendar.getInstance().apply { timeInMillis = newTime }

                final.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                final.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))

                onDateSelected(final.timeInMillis)
            }
        }
    }) {
        Text(
            currentTimestamp?.let {
                java.text.SimpleDateFormat("MMM dd, yyyy • hh:mm a").format(Date(it))
            } ?: "Set Due Date"
        )
    }
}

private fun showDatePicker(
    context: Context,
    year: Int,
    month: Int,
    day: Int,
    onPicked: (Long) -> Unit
) {
    DatePickerDialog(context, { _, y, m, d ->
        val cal = Calendar.getInstance()
        cal.set(y, m, d)
        onPicked(cal.timeInMillis)
    }, year, month, day).show()
}

private fun showTimePicker(
    context: Context,
    hour: Int,
    minute: Int,
    onPicked: (Long) -> Unit
) {
    TimePickerDialog(context, { _, h, m ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        onPicked(cal.timeInMillis)
    }, hour, minute, false).show()
}
