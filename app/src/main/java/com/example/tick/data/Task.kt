package com.example.tick.data

data class Task(
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val category: String = "Others"   // ⭐ NEW FIELD
)
