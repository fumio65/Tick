package com.example.tick.data

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false
)
