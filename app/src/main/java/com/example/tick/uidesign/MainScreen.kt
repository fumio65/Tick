package com.example.tick.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode



enum class TaskFilter { ALL, COMPLETED, PENDING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TaskViewModel,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Int) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val tasks = viewModel.tasks.collectAsState()
    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var showMenu by remember { mutableStateOf(false) }

    val filteredTasks = when (filter) {
        TaskFilter.ALL -> tasks.value
        TaskFilter.COMPLETED -> tasks.value.filter { it.isCompleted }
        TaskFilter.PENDING -> tasks.value.filter { !it.isCompleted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tick") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Filter Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Tasks") },
                                onClick = {
                                    filter = TaskFilter.ALL
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Completed") },
                                onClick = {
                                    filter = TaskFilter.COMPLETED
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pending") },
                                onClick = {
                                    filter = TaskFilter.PENDING
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) {
                Text("+")
            }
        }
    ) { padding ->
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No tasks found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(filteredTasks) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onEditTaskClick(task.id) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(task.title, style = MaterialTheme.typography.titleMedium)
                                if (task.description.isNotEmpty()) {
                                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Row {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.toggleComplete(task.id) }
                                )
                                IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Task"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
