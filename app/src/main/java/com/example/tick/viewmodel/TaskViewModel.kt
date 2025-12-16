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
import com.example.tick.data.Subtask
import com.example.tick.data.TaskWithSubtasks
import kotlinx.coroutines.flow.first

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
        val database = TaskDatabase.getDatabase(application)
        val taskDao = database.taskDao()
        val subtaskDao = database.subtaskDao()
        repository = TaskRepository(taskDao, subtaskDao)

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
    private val _selectedScheduledDate = MutableStateFlow<Long?>(null)
    val selectedScheduledDate: StateFlow<Long?> = _selectedScheduledDate

    fun setScheduledDate(timestamp: Long?) {
        _selectedScheduledDate.value = timestamp
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
    // ADD TASK + schedule alarm (FIXED WITH TIME BLOCKING)
    // ------------------------------------------------------
    fun addTask(
        title: String,
        description: String,
        context: Context,
        color: Color? = null,
        priority: String = "Medium",
        // NEW PARAMETERS FOR TIME BLOCKING
        isTimeBlocked: Boolean = false,
        startTime: Long? = null,
        endTime: Long? = null,
        subtasks: List<String> = emptyList()
    ) {
        // Validate input
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                // Calculate duration if time blocking is enabled
                val duration = if (isTimeBlocked && startTime != null && endTime != null) {
                    ((endTime - startTime) / (60 * 1000)).toInt() // Convert to minutes
                } else null

                val newTask = Task(
                    id = 0, // Room will auto-generate
                    title = title.trim(),
                    description = description.trim(),
                    category = _selectedCategory.value ?: "Others",
                    scheduledDate = _selectedScheduledDate.value,
                    color = color?.toArgb(),
                    priority = priority,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    // TIME BLOCKING FIELDS
                    isTimeBlocked = isTimeBlocked,
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration
                )

                // Insert task and get the generated ID
                val taskId = repository.insertTask(newTask).toInt()

                // Insert subtasks if any
                // Insert subtasks if any
                if (subtasks.isNotEmpty()) {
                    val subtaskEntities = subtasks.map { subtaskTitle ->
                        Subtask(
                            parentTaskId = taskId,  // ⬅️ Change from taskId to parentTaskId
                            title = subtaskTitle,
                            isCompleted = false
                        )
                    }
                    repository.insertSubtasks(subtaskEntities)
                }

                // Schedule reminder based on mode
                if (isTimeBlocked && startTime != null) {
                    // For time-blocked tasks, set reminder for start time
                    scheduleReminder(context, taskId, newTask.title, startTime)
                } else {
                    // For regular tasks, use due date
                    _selectedScheduledDate.value?.let { dueDate ->
                        scheduleReminder(context, taskId, newTask.title, dueDate)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearTaskFormState() {
        _selectedCategory.value = null
        _selectedScheduledDate.value = null
    }

    // ------------------------------------------------------
    // EDIT TASK + reschedule alarm (FIXED WITH TIME BLOCKING)
    // ------------------------------------------------------
    fun editTask(
        taskId: Int,
        newTitle: String,
        newDescription: String,
        newCategory: String,
        newDueDate: Long?,
        context: Context,
        newColor: Color? = null,
        newPriority: String = "Medium",
        // NEW PARAMETERS FOR TIME BLOCKING
        isTimeBlocked: Boolean = false,
        startTime: Long? = null,
        endTime: Long? = null
    ) {
        viewModelScope.launch {
            try {
                val existingTask = repository.getTaskById(taskId)
                existingTask?.let { task ->
                    // Calculate duration if time blocking is enabled
                    val duration = if (isTimeBlocked && startTime != null && endTime != null) {
                        ((endTime - startTime) / (60 * 1000)).toInt()
                    } else null

                    val updatedTask = task.copy(
                        title = newTitle.trim(),
                        description = newDescription.trim(),
                        category = newCategory,
                        scheduledDate  = newDueDate,
                        color = newColor?.toArgb(),
                        priority = newPriority,
                        isTimeBlocked = isTimeBlocked,
                        startTime = startTime,
                        endTime = endTime,
                        duration = duration
                    )
                    repository.updateTask(updatedTask)

                    // Cancel old reminder and schedule new one
                    cancelReminder(context, taskId)
                    if (isTimeBlocked && startTime != null) {
                        scheduleReminder(context, taskId, newTitle, startTime)
                    } else {
                        newDueDate?.let { dueDate ->
                            scheduleReminder(context, taskId, newTitle, dueDate)
                        }
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
                if (task.isTimeBlocked && task.startTime != null) {
                    scheduleReminder(context, task.id, task.title, task.startTime)
                } else {
                    task.scheduledDate ?.let { dueDate ->
                        scheduleReminder(context, task.id, task.title, dueDate)
                    }
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

    // ==================== SUBTASK OPERATIONS ====================

    // Get subtasks for a specific task
    fun getSubtasksForTask(taskId: Int): StateFlow<List<Subtask>> {
        return repository.getSubtasksForTask(taskId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    suspend fun getSubtasksForTaskOnce(taskId: Int): List<Subtask> {
        return repository.getSubtasksForTask(taskId).first()
    }

    // Add a new subtask
    fun addSubtask(taskId: Int, title: String) {
        if (title.isBlank()) return

        viewModelScope.launch {
            try {
                val subtask = Subtask(
                    parentTaskId = taskId,
                    title = title.trim(),
                    isCompleted = false,
                    orderIndex = 0
                )
                repository.insertSubtask(subtask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Add multiple subtasks at once
    fun addSubtasks(taskId: Int, subtaskTitles: List<String>) {
        viewModelScope.launch {
            try {
                val subtasks = subtaskTitles
                    .filter { it.isNotBlank() }
                    .mapIndexed { index, title ->
                        Subtask(
                            parentTaskId = taskId,
                            title = title.trim(),
                            isCompleted = false,
                            orderIndex = index
                        )
                    }
                if (subtasks.isNotEmpty()) {
                    repository.insertSubtasks(subtasks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Toggle subtask completion
    fun toggleSubtaskComplete(subtaskId: Int) {
        viewModelScope.launch {
            try {
                val subtask = repository.getSubtaskById(subtaskId)
                subtask?.let {
                    repository.toggleSubtaskCompletion(subtaskId, !it.isCompleted)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Update subtask title
    fun updateSubtaskTitle(subtaskId: Int, newTitle: String) {
        if (newTitle.isBlank()) return

        viewModelScope.launch {
            try {
                val subtask = repository.getSubtaskById(subtaskId)
                subtask?.let {
                    val updated = it.copy(title = newTitle.trim())
                    repository.updateSubtask(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete a subtask
    fun deleteSubtask(subtaskId: Int) {
        viewModelScope.launch {
            try {
                val subtask = repository.getSubtaskById(subtaskId)
                subtask?.let {
                    repository.deleteSubtask(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete subtask object directly
    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch {
            try {
                repository.deleteSubtask(subtask)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete all subtasks for a task
    fun deleteAllSubtasksForTask(taskId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteSubtasksForTask(taskId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Get subtask progress (completed/total)
    suspend fun getSubtaskProgress(taskId: Int): Pair<Int, Int> {
        return try {
            repository.getSubtaskProgress(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }

    // Update subtask order (for drag-to-reorder later)
    fun updateSubtaskOrder(subtaskId: Int, orderIndex: Int) {
        viewModelScope.launch {
            try {
                repository.updateSubtaskOrder(subtaskId, orderIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Get task with all its subtasks
    suspend fun getTaskWithSubtasks(taskId: Int): TaskWithSubtasks? {
        return try {
            repository.getTaskWithSubtasks(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}