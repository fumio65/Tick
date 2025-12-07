package com.example.tick.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    // Observe all tasks
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    // Observe active tasks
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()

    // Observe completed tasks
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()

    // Observe task counts
    val taskCount: Flow<Int> = taskDao.getTaskCount()
    val activeTaskCount: Flow<Int> = taskDao.getActiveTaskCount()

    // Get tasks by category
    fun getTasksByCategory(category: String): Flow<List<Task>> {
        return taskDao.getTasksByCategory(category)
    }

    // Get tasks by priority
    fun getTasksByPriority(priority: String): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority)
    }

    // Get upcoming tasks (due today or overdue)
    fun getUpcomingTasks(timestamp: Long = System.currentTimeMillis()): Flow<List<Task>> {
        return taskDao.getUpcomingTasks(timestamp)
    }

    // Search tasks
    fun searchTasks(searchQuery: String): Flow<List<Task>> {
        return taskDao.searchTasks(searchQuery)
    }

    // Get task by ID
    suspend fun getTaskById(taskId: Int): Task? {
        return taskDao.getTaskById(taskId)
    }

    // Insert a new task
    suspend fun insertTask(task: Task): Long {
        return taskDao.insertTask(task)
    }

    // Insert multiple tasks
    suspend fun insertTasks(tasks: List<Task>) {
        taskDao.insertTasks(tasks)
    }

    // Update an existing task
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    // Delete a task
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    // Delete all completed tasks
    suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }

    // Delete all tasks
    suspend fun deleteAllTasks() {
        taskDao.deleteAllTasks()
    }

    // Toggle task completion
    suspend fun toggleTaskCompletion(taskId: Int, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }

    // Update task priority
    suspend fun updateTaskPriority(taskId: Int, priority: String) {
        taskDao.updateTaskPriority(taskId, priority)
    }
}