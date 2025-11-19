package com.example.tick.uidesign

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import java.util.Calendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TaskViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current  // ⭐ REQUIRED for scheduling reminders

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDueDate by viewModel.selectedDueDate.collectAsState()

    // Dialog controllers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Date & time picker states
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    // If user has an existing due date, initialize tempCalendar so time picker will use it
    LaunchedEffect(selectedDueDate) {
        selectedDueDate?.let { ms ->
            calendarStoreDateOnly(ms)
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
            timePickerState.minute = cal.get(Calendar.MINUTE)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Task") }) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // TITLE
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // DESCRIPTION
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // CATEGORY
            CategoryDropdown(
                selectedCategory = selectedCategory,
                categories = viewModel.categories,
                onCategorySelected = { category ->
                    viewModel.updateSelectedCategory(category)
                }
            )

            // DUE DATE BUTTON
            OutlinedButton(
                onClick = {
                    selectedDueDate?.let { dateMillis ->
                        calendarStoreDateOnly(dateMillis)
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                        timePickerState.minute = cal.get(Calendar.MINUTE)
                    } ?: run {
                        tempCalendar = null
                    }
                    showDatePicker = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selectedDueDate?.let { "Due: ${formatDueDate(it)}" }
                        ?: "Set Due Date"
                )
            }

            // DATE PICKER (AlertDialog wrapper)
            if (showDatePicker) {
                AlertDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val selectedDateMillis = datePickerState.selectedDateMillis
                            if (selectedDateMillis != null) {
                                calendarStoreDateOnly(selectedDateMillis)
                                showDatePicker = false
                                showTimePicker = true
                            } else {
                                showDatePicker = false
                            }
                        }) { Text("Next") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    },
                    text = { DatePicker(state = datePickerState) }
                )
            }

            // TIME PICKER
            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val finalTimestamp = calendarApplyTime(
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                            viewModel.setDueDate(finalTimestamp)
                            showTimePicker = false
                        }) { Text("Set Time") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            // ACTION BUTTONS
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            // ⭐ Now correctly passes context
                            viewModel.addTask(title, description, context)
                            onSave()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}



// -------------------------
// Helper functions
// -------------------------

private var tempCalendar: Calendar? = null

private fun calendarStoreDateOnly(dateMillis: Long) {
    val cal = Calendar.getInstance()
    cal.timeInMillis = dateMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    tempCalendar = cal
}

private fun calendarApplyTime(hour: Int, minute: Int): Long {
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
