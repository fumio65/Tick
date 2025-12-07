package com.example.tick.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tick.data.Task
import com.example.tick.data.TaskDatabase
import com.example.tick.data.TaskRepository
import com.example.tick.receiver.ReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    // ------------------------------------------------------
    // TASK LIST STATE (from Room Database)
    // ------------------------------------------------------
    val tasks: StateFlow<List<Task>>
    val activeTasks: StateFlow<List<Task>>
    val completedTasks: StateFlow<List<Task>>
    val taskCount: StateFlow<Int>
    val activeTaskCount: StateFlow<Int>

    init {
        val taskDao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeTasks = repository.activeTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        completedTasks = repository.completedTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        taskCount = repository.taskCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        activeTaskCount = repository.activeTaskCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    }

    // ------------------------------------------------------
    // THEME STATE
    // ------------------------------------------------------
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // ------------------------------------------------------
    // CATEGORY STATE
    // ------------------------------------------------------
    val categories = listOf("Work", "School", "Personal", "Home", "Others")

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    // ------------------------------------------------------
    // DUE DATE STATE
    // ------------------------------------------------------
    private val _selectedDueDate = MutableStateFlow<Long?>(null)
    val selectedDueDate: StateFlow<Long?> = _selectedDueDate

    fun setDueDate(timestamp: Long?) {
        _selectedDueDate.value = timestamp
    }

    // ------------------------------------------------------
    // FILTER STATE (for displaying tasks by category)
    // ------------------------------------------------------
    fun getTasksByCategory(category: String): StateFlow<List<Task>> {
        return repository.getTasksByCategory(category).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getTasksByPriority(priority: String): StateFlow<List<Task>> {
        return repository.getTasksByPriority(priority).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getUpcomingTasks(): StateFlow<List<Task>> {
        return repository.getUpcomingTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun searchTasks(query: String): StateFlow<List<Task>> {
        return repository.searchTasks(query).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // ------------------------------------------------------
    // ADD TASK + schedule alarm
    // ------------------------------------------------------
    fun addTask(
        title: String,
        description: String,
        context: Context,
        color: Color? = null,
        priority: String = "Medium"
    ) {
        // Validate input
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                val newTask = Task(
                    id = 0, // Room will auto-generate
                    title = title.trim(),
                    description = description.trim(),
                    category = _selectedCategory.value ?: "Others",
                    dueDate = _selectedDueDate.value,
                    color = color?.toArgb(),
                    priority = priority,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis()
                )

                // Insert task and get the generated ID
                val taskId = repository.insertTask(newTask).toInt()

                // Schedule reminder only if dueDate exists
                _selectedDueDate.value?.let { dueDate ->
                    scheduleReminder(context, taskId, newTask.title, dueDate)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        newColor: Color? = null,
        newPriority: String = "Medium"
    ) {
        viewModelScope.launch {
            try {
                val existingTask = repository.getTaskById(taskId)
                existingTask?.let { task ->
                    val updatedTask = task.copy(
                        title = newTitle.trim(),
                        description = newDescription.trim(),
                        category = newCategory,
                        dueDate = newDueDate,
                        color = newColor?.toArgb(),
                        priority = newPriority
                    )
                    repository.updateTask(updatedTask)

                    // Cancel old reminder and schedule new one
                    cancelReminder(context, taskId)
                    newDueDate?.let { dueDate ->
                        scheduleReminder(context, taskId, newTitle, dueDate)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // DELETE TASK + cancel alarm
    // ------------------------------------------------------
    fun deleteTask(taskId: Int, context: Context) {
        viewModelScope.launch {
            try {
                val task = repository.getTaskById(taskId)
                task?.let {
                    repository.deleteTask(it)
                    cancelReminder(context, taskId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete task object directly
    fun deleteTask(task: Task, context: Context) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
                cancelReminder(context, task.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // DELETE ALL COMPLETED TASKS
    // ------------------------------------------------------
    fun deleteCompletedTasks() {
        viewModelScope.launch {
            try {
                repository.deleteCompletedTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // RESTORE TASK (UNDO delete)
    // ------------------------------------------------------
    fun restoreTask(task: Task, context: Context) {
        viewModelScope.launch {
            try {
                repository.insertTask(task)
                task.dueDate?.let { dueDate ->
                    scheduleReminder(context, task.id, task.title, dueDate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // TOGGLE COMPLETE
    // ------------------------------------------------------
    fun toggleComplete(taskId: Int) {
        viewModelScope.launch {
            try {
                val task = repository.getTaskById(taskId)
                task?.let {
                    repository.toggleTaskCompletion(taskId, !it.isCompleted)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // UPDATE TASK PRIORITY
    // ------------------------------------------------------
    fun updateTaskPriority(taskId: Int, priority: String) {
        viewModelScope.launch {
            try {
                repository.updateTaskPriority(taskId, priority)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------
    // GET TASK BY ID
    // ------------------------------------------------------
    suspend fun getTaskById(taskId: Int): Task? {
        return try {
            repository.getTaskById(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ------------------------------------------------------
    // SCHEDULE REMINDER
    // ------------------------------------------------------
    private fun scheduleReminder(
        context: Context,
        taskId: Int,
        title: String,
        dueDate: Long
    ) {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ------------------------------------------------------
    // CANCEL REMINDER
    // ------------------------------------------------------
    private fun cancelReminder(context: Context, taskId: Int) {
        try {
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
            pending.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}