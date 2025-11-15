package com.example.tick.uidesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.tick.viewmodel.TaskViewModel

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
                contentAlignment = Alignment.Center
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

                    // ⭐ ENHANCED CARD UI
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp), // Card handles rounding
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant // ❗ no alpha
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            // TOP ROW: Title + Checkbox + Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    // CATEGORY CHIP
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = task.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleComplete(task.id) }
                                    )
                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Task",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            // DESCRIPTION BLOCK
                            if (task.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                    thickness = 0.5.dp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
