package com.example.tick.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Get all tasks ordered by due date
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>

    // Get active (incomplete) tasks
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getActiveTasks(): Flow<List<Task>>

    // Get completed tasks
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY dueDate DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    // Get tasks by category
    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY dueDate ASC")
    fun getTasksByCategory(category: String): Flow<List<Task>>

    // Get tasks by priority
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY dueDate ASC")
    fun getTasksByPriority(priority: String): Flow<List<Task>>

    // Get a single task by ID
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    // Get tasks due today or overdue
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate <= :timestamp ORDER BY dueDate ASC")
    fun getUpcomingTasks(timestamp: Long): Flow<List<Task>>

    // Insert a new task
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    // Insert multiple tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    // Update an existing task
    @Update
    suspend fun updateTask(task: Task)

    // Delete a task
    @Delete
    suspend fun deleteTask(task: Task)

    // Delete all completed tasks
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    // Delete all tasks
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Toggle task completion status
    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean)

    // Update task priority
    @Query("UPDATE tasks SET priority = :priority WHERE id = :taskId")
    suspend fun updateTaskPriority(taskId: Int, priority: String)

    // Search tasks by title or description
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' ORDER BY dueDate ASC")
    fun searchTasks(searchQuery: String): Flow<List<Task>>

    // Get task count
    @Query("SELECT COUNT(*) FROM tasks")
    fun getTaskCount(): Flow<Int>

    // Get active task count
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getActiveTaskCount(): Flow<Int>
}