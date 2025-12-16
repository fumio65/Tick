package com.example.tick.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE // Delete subtasks when parent task is deleted
        )
    ],
    indices = [Index(value = ["parentTaskId"])] // Index for faster queries
)
data class Subtask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val parentTaskId: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0, // For custom ordering
    val createdAt: Long = System.currentTimeMillis()
)