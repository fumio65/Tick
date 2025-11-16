package com.example.tick.uidesign

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import com.example.tick.util.*
import java.util.Calendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: Int,
    viewModel: TaskViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val allTasks = viewModel.tasks.collectAsState().value
    val task = allTasks.find { it.id == taskId }

    if (task == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Edit Task") }) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { Text("Task not found.") }
        }
        return
    }

    // task fields
    var title by rememberSaveable(taskId) { mutableStateOf(task.title) }
    var description by rememberSaveable(taskId) { mutableStateOf(task.description) }
    var category by rememberSaveable(taskId) { mutableStateOf(task.category) }
    var dueDate by rememberSaveable(taskId) { mutableStateOf(task.dueDate) }

    // dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    // Load the existing due date
    LaunchedEffect(taskId) {
        task.dueDate?.let {
            calendarStoreDateOnly(it)
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
            timePickerState.minute = cal.get(Calendar.MINUTE)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Task") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            CategoryDropdown(
                selectedCategory = category,
                categories = viewModel.categories,
                onCategorySelected = { category = it }
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(dueDate?.let { "Due: ${formatDueDate(it)}" } ?: "Set Due Date")
            }

            if (dueDate != null) {
                OutlinedButton(
                    onClick = { dueDate = null },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove Due Date") }
            }

            // DATE DIALOG
            if (showDatePicker) {
                AlertDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val ms = datePickerState.selectedDateMillis
                            if (ms != null) {
                                calendarStoreDateOnly(ms)
                                showDatePicker = false
                                showTimePicker = true
                            } else showDatePicker = false
                        }) { Text("Next") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    },
                    text = { DatePicker(state = datePickerState) }
                )
            }

            // TIME DIALOG
            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val finalDate = calendarApplyTime(
                                timePickerState.hour,
                                timePickerState.minute
                            )
                            dueDate = finalDate
                            showTimePicker = false
                        }) { Text("Set Time") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                    },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            // BUTTONS
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.editTask(
                                task.id,
                                title,
                                description,
                                category,
                                dueDate
                            )
                            onSave()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
            }
        }
    }
}
