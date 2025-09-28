package com.example.tick.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.tick.data.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TaskViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Restore saved tasks or start empty
    private val _tasks = MutableStateFlow<List<Task>>(
        savedStateHandle["tasks"] ?: emptyList()
    )
    val tasks: StateFlow<List<Task>> = _tasks

    private var nextId = savedStateHandle["nextId"] ?: 0

    // --- Dark Theme ---
    private val _isDarkTheme = MutableStateFlow(savedStateHandle["isDarkTheme"] ?: false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private fun saveState() {
        savedStateHandle["tasks"] = _tasks.value
        savedStateHandle["nextId"] = nextId
        savedStateHandle["isDarkTheme"] = _isDarkTheme.value
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
        saveState()
    }

    // --- Task functions ---
    fun addTask(title: String, description: String = "") {
        val newTask = Task(id = nextId++, title = title, description = description)
        _tasks.value = _tasks.value + newTask
        saveState()
    }

    fun toggleComplete(taskId: Int) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(isCompleted = !task.isCompleted) else task
        }
        saveState()
    }

    fun deleteTask(taskId: Int) {
        _tasks.value = _tasks.value.filterNot { it.id == taskId }
        saveState()
    }

    fun editTask(taskId: Int, newTitle: String, newDescription: String) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) task.copy(title = newTitle, description = newDescription) else task
        }
        saveState()
    }

    fun getTaskById(taskId: Int): Task? {
        return _tasks.value.find { it.id == taskId }
    }
}
