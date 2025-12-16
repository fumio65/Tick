package com.example.tick.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    // Get all subtasks for a specific task
    @Query("SELECT * FROM subtasks WHERE parentTaskId = :taskId ORDER BY orderIndex ASC, createdAt ASC")
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>>

    // Get a single subtask by ID
    @Query("SELECT * FROM subtasks WHERE id = :subtaskId")
    suspend fun getSubtaskById(subtaskId: Int): Subtask?

    // Insert a new subtask
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask): Long

    // Insert multiple subtasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<Subtask>)

    // Update an existing subtask
    @Update
    suspend fun updateSubtask(subtask: Subtask)

    // Delete a subtask
    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    // Delete all subtasks for a task
    @Query("DELETE FROM subtasks WHERE parentTaskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Int)

    // Toggle subtask completion
    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId")
    suspend fun updateSubtaskCompletion(subtaskId: Int, isCompleted: Boolean)

    // Get completed subtask count for a task
    @Query("SELECT COUNT(*) FROM subtasks WHERE parentTaskId = :taskId AND isCompleted = 1")
    suspend fun getCompletedSubtaskCount(taskId: Int): Int

    // Get total subtask count for a task
    @Query("SELECT COUNT(*) FROM subtasks WHERE parentTaskId = :taskId")
    suspend fun getTotalSubtaskCount(taskId: Int): Int

    // Update subtask order
    @Query("UPDATE subtasks SET orderIndex = :orderIndex WHERE id = :subtaskId")
    suspend fun updateSubtaskOrder(subtaskId: Int, orderIndex: Int)
}