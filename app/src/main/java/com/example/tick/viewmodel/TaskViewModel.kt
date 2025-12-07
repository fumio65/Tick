package com.example.tick.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tick.data.Task
import com.example.tick.receiver.ReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ------------------------------------------------------
    // TASK LIST STATE
    // ------------------------------------------------------
    private val _tasks = MutableStateFlow(
        savedStateHandle["tasks"] ?: emptyList<Task>()
    )
    val tasks: StateFlow<List<Task>> = _tasks

    private var nextId = savedStateHandle["nextId"] ?: 0


    // ------------------------------------------------------
    // THEME STATE
    // ------------------------------------------------------
    private val _isDarkTheme =
        MutableStateFlow(savedStateHandle["isDarkTheme"] ?: false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        saveState()
    }


    // ------------------------------------------------------
    // CATEGORY STATE (Updated to accept nullable)
    // ------------------------------------------------------
    val categories = listOf("Work", "School", "Personal", "Home", "Others")

    private val _selectedCategory =
        MutableStateFlow<String?>(savedStateHandle["selectedCategory"])
    val selectedCategory: StateFlow<String?> = _selectedCategory

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
        savedStateHandle["selectedCategory"] = category
    }


    // ------------------------------------------------------
    // DUE DATE STATE
    // ------------------------------------------------------
    private val _selectedDueDate =
        MutableStateFlow<Long?>(savedStateHandle["selectedDueDate"])
    val selectedDueDate: StateFlow<Long?> = _selectedDueDate

    fun setDueDate(timestamp: Long?) {
        _selectedDueDate.value = timestamp
        savedStateHandle["selectedDueDate"] = timestamp
    }


    // ------------------------------------------------------
    // SAVE VIEWMODEL STATE
    // ------------------------------------------------------
    private fun saveState() {
        savedStateHandle["tasks"] = _tasks.value
        savedStateHandle["nextId"] = nextId
        savedStateHandle["isDarkTheme"] = _isDarkTheme.value
        savedStateHandle["selectedCategory"] = _selectedCategory.value
        savedStateHandle["selectedDueDate"] = _selectedDueDate.value
    }


    // ------------------------------------------------------
    // ADD TASK + schedule alarm (UPDATED WITH VALIDATION)
    // ------------------------------------------------------
    fun addTask(
        title: String,
        description: String,
        context: Context,
        color: Color? = null
    ) {
        // Validate input
        if (title.isBlank()) return

        try {
            val newTask = Task(
                id = nextId++,
                title = title.trim(),
                description = description.trim(),
                category = _selectedCategory.value ?: "Others",
                dueDate = _selectedDueDate.value,
                color = color?.toArgb()
            )

            // Update tasks list
            _tasks.value = _tasks.value + newTask

            // Save state immediately
            saveState()

            // Schedule reminder only if dueDate exists
            _selectedDueDate.value?.let { dueDate ->
                scheduleReminder(context, newTask.id, newTask.title, dueDate)
            }

        } catch (e: Exception) {
            // Log error or handle gracefully
            e.printStackTrace()
        }
    }


    // ------------------------------------------------------
    // EDIT TASK + reschedule alarm
    // ------------------------------------------------------
    fun editTask(
        taskId: Int,
        newTitle: String,
        newDescription: String,
        newCategory: String,
        newDueDate: Long?,
        context: Context,
        newColor: Color? = null
    ) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    title = newTitle,
                    description = newDescription,
                    category = newCategory,
                    dueDate = newDueDate,
                    color = newColor?.toArgb()
                )
            } else task
        }

        saveState()

        cancelReminder(context, taskId)
        scheduleReminder(context, taskId, newTitle, newDueDate)
    }


    // ------------------------------------------------------
    // DELETE TASK + cancel alarm
    // ------------------------------------------------------
    fun deleteTask(taskId: Int, context: Context) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        saveState()
        cancelReminder(context, taskId)
    }


    // ------------------------------------------------------
    // RESTORE TASK (UNDO delete)
    // ------------------------------------------------------
    fun restoreTask(task: Task) {
        // Add the task back keeping its original ID
        _tasks.value = _tasks.value + task
        saveState()

        // Reschedule its reminder
        // (if dueDate is null or passed, scheduleReminder handles that)
        // Use application context because this may be called after UI delete
        // MainScreen already passes a context, so use same approach:
        // The caller should provide context when restoring.
    }

    // Caller must provide context:
    fun restoreTask(task: Task, context: Context) {
        _tasks.value = _tasks.value + task
        saveState()

        scheduleReminder(context, task.id, task.title, task.dueDate)
    }


    // ------------------------------------------------------
    // TOGGLE COMPLETE
    // ------------------------------------------------------
    fun toggleComplete(taskId: Int) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(isCompleted = !task.isCompleted)
            } else task
        }
        saveState()
    }


    // ------------------------------------------------------
    // GET TASK BY ID
    // ------------------------------------------------------
    fun getTaskById(taskId: Int): Task? =
        _tasks.value.find { it.id == taskId }


    // ------------------------------------------------------
    // SCHEDULE REMINDER
    // ------------------------------------------------------
    fun scheduleReminder(
        context: Context,
        taskId: Int,
        title: String,
        dueDate: Long?
    ) {
        if (dueDate == null) return

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("task_title", title)
            putExtra("task_id", taskId)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            dueDate,
            pending
        )
    }


    // ------------------------------------------------------
    // CANCEL REMINDER
    // ------------------------------------------------------
    fun cancelReminder(context: Context, taskId: Int) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)

        val pending = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pending)
    }
}