package com.example.tick.data

data class Task(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val category: String = "Others",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null   // ⭐ NEW
)
