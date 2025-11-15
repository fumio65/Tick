package com.example.tick.uidesign

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: Int,
    viewModel: TaskViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val tasksState = viewModel.tasks.collectAsState()
    val task = tasksState.value.find { it.id == taskId }

    // If task not found, show a friendly message
    if (task == null) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Edit Task") })
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Task not found.")
            }
        }
        return
    }

    var title by rememberSaveable(taskId) { mutableStateOf(task.title) }
    var description by rememberSaveable(taskId) { mutableStateOf(task.description) }

    // initial category: prefer task.category, fallback to ViewModel's selectedCategory
    val vmSelectedCategory by viewModel.selectedCategory.collectAsState()
    val initialCategory = task.category.ifBlank { vmSelectedCategory }
    var selectedCategory by rememberSaveable(taskId) { mutableStateOf(initialCategory) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Task") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // CATEGORY DROPDOWN (editable)
            CategoryDropdown(
                selectedCategory = selectedCategory,
                categories = viewModel.categories,
                onCategorySelected = { selectedCategory = it }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.editTask(task.id, title, description, selectedCategory)
                            onSave()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = { onCancel() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
