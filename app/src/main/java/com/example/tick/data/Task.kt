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
    val scheduledDate: Long? = null,

    // NEW FIELDS FOR TIMEBLOCKING
    val startTime: Long? = null,        // Start time of the time block
    val endTime: Long? = null,          // End time of the time block
    val duration: Int? = null,          // Duration in minutes (calculated from start/end)
    val isTimeBlocked: Boolean = false, // Flag to distinguish timeblocked tasks

    val isCompleted: Boolean = false,
    val color: Int? = null,
    val priority: String = "Medium",
    val createdAt: Long = System.currentTimeMillis()
)