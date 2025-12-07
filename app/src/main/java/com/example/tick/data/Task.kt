package com.example.tick.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val color: Int? = null,
    val priority: String = "Medium", // Low, Medium, High
    val createdAt: Long = System.currentTimeMillis()
)