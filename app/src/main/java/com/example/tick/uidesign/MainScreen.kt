package com.example.tick.uidesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import com.example.tick.util.formatDueDate
import java.util.*

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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("No tasks found.") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(filteredTasks) { task ->

                    // -----------------------------
                    // Due Date Badge Color Logic
                    // -----------------------------
                    val dueColor: Color
                    val now = System.currentTimeMillis()

                    if (task.dueDate == null) {
                        dueColor = MaterialTheme.colorScheme.outline
                    } else if (task.dueDate < now) {
                        dueColor = Color(0xFFE57373) // red - overdue
                    } else if (task.dueDate - now < 86_400_000) {
                        dueColor = Color(0xFFFFB74D) // orange - soon
                    } else {
                        dueColor = MaterialTheme.colorScheme.primary // normal
                    }

                    // -----------------------------
                    // Card UI
                    // -----------------------------
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clickable { onEditTaskClick(task.id) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {

                            // TITLE
                            Text(
                                task.title,
                                style = MaterialTheme.typography.titleLarge
                            )

                            // DESCRIPTION
                            if (task.description.isNotBlank()) {
                                Text(
                                    task.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // CATEGORY TAG
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            // -----------------------------
                            // DUE DATE BADGE (NEW)
                            // -----------------------------
                            if (task.dueDate != null) {
                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                dueColor.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = formatDueDate(task.dueDate),
                                            color = dueColor,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }

                            // FOOTER ACTIONS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
