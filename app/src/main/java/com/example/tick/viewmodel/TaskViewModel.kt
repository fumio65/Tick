package com.example.tick.viewmodel

import androidx.lifecycle.ViewModel
import com.example.tick.data.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel : ViewModel() {

    // In-memory task list (initially empty)
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private var nextId = 0

    fun addTask(title: String, description: String = "") {
        val newTask = Task(id = nextId++, title = title, description = description)
        _tasks.value = _tasks.value + newTask
    }

    fun toggleComplete(taskId: Int) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(isCompleted = !task.isCompleted)
            else task
        }
    }

    fun deleteTask(taskId: Int) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
    }

    fun editTask(taskId: Int, newTitle: String, newDescription: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(title = newTitle, description = newDescription)
            else task
        }
    }
}
