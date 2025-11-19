package com.example.tick.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
    // CATEGORY STATE
    // ------------------------------------------------------
    val categories = listOf("Work", "School", "Personal", "Home", "Others")

    private val _selectedCategory =
        MutableStateFlow(savedStateHandle["selectedCategory"] ?: "Others")
    val selectedCategory: StateFlow<String> = _selectedCategory

    fun updateSelectedCategory(category: String) {
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
    // ADD TASK + schedule alarm
    // ------------------------------------------------------
    fun addTask(title: String, description: String, context: Context) {

        val newTask = Task(
            id = nextId++,
            title = title,
            description = description,
            category = _selectedCategory.value,
            dueDate = _selectedDueDate.value
        )

        _tasks.value = _tasks.value + newTask
        saveState()

        scheduleReminder(context, newTask.id, newTask.title, newTask.dueDate)
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
        context: Context
    ) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) {
                task.copy(
                    title = newTitle,
                    description = newDescription,
                    category = newCategory,
                    dueDate = newDueDate
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
    // TOGGLE COMPLETE (needed by MainScreen checkbox)
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
    // GET TASK (optional, safe)
    // ------------------------------------------------------
    fun getTaskById(taskId: Int): Task? =
        _tasks.value.find { it.id == taskId }


    // ------------------------------------------------------
    // SCHEDULE REMINDER (AlarmManager)
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

        // Android 12+ must have explicit exact alarm permission
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
