package com.example.tick.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao
) {

    // ==================== TASK OPERATIONS ====================

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

    // Get task with subtasks
    suspend fun getTaskWithSubtasks(taskId: Int): TaskWithSubtasks? {
        return taskDao.getTaskWithSubtasks(taskId)
    }

    // Get all tasks with subtasks
    fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasks>> {
        return taskDao.getAllTasksWithSubtasks()
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

    // ==================== SUBTASK OPERATIONS ====================

    // Get all subtasks for a task
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>> {
        return subtaskDao.getSubtasksForTask(taskId)
    }

    // Get a single subtask by ID
    suspend fun getSubtaskById(subtaskId: Int): Subtask? {
        return subtaskDao.getSubtaskById(subtaskId)
    }

    // Insert a new subtask
    suspend fun insertSubtask(subtask: Subtask): Long {
        return subtaskDao.insertSubtask(subtask)
    }

    // Insert multiple subtasks
    suspend fun insertSubtasks(subtasks: List<Subtask>) {
        subtaskDao.insertSubtasks(subtasks)
    }

    // Update an existing subtask
    suspend fun updateSubtask(subtask: Subtask) {
        subtaskDao.updateSubtask(subtask)
    }

    // Delete a subtask
    suspend fun deleteSubtask(subtask: Subtask) {
        subtaskDao.deleteSubtask(subtask)
    }

    // Delete all subtasks for a task
    suspend fun deleteSubtasksForTask(taskId: Int) {
        subtaskDao.deleteSubtasksForTask(taskId)
    }

    // Toggle subtask completion
    suspend fun toggleSubtaskCompletion(subtaskId: Int, isCompleted: Boolean) {
        subtaskDao.updateSubtaskCompletion(subtaskId, isCompleted)
    }

    // Get subtask progress for a task
    suspend fun getSubtaskProgress(taskId: Int): Pair<Int, Int> {
        val completed = subtaskDao.getCompletedSubtaskCount(taskId)
        val total = subtaskDao.getTotalSubtaskCount(taskId)
        return Pair(completed, total)
    }

    // Update subtask order
    suspend fun updateSubtaskOrder(subtaskId: Int, orderIndex: Int) {
        subtaskDao.updateSubtaskOrder(subtaskId, orderIndex)
    }
}