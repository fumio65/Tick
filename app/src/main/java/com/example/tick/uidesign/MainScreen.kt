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
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tick.viewmodel.TaskViewModel
import com.example.tick.util.formatDueDate

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
    val context = LocalContext.current

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
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
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
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {

                items(filteredTasks, key = { it.id }) { task ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * 0.5f }
                    )

                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        LaunchedEffect(task.id) {
                            viewModel.deleteTask(task.id, context)
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val direction = dismissState.dismissDirection
                            val progress = dismissState.progress

                            val swipeProgress =
                                if (direction == SwipeToDismissBoxValue.EndToStart)
                                    progress
                                else 0f

                            if (swipeProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE53935)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = swipeProgress),
                                        modifier = Modifier
                                            .padding(end = 24.dp)
                                            .size((24 + 10 * swipeProgress).dp)
                                    )
                                }
                            }
                        }
                    ) {
                        TaskCard(
                            task = task,
                            onEdit = { onEditTaskClick(task.id) },
                            onToggle = { viewModel.toggleComplete(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: com.example.tick.data.Task,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    val dueColor: Color =
        when {
            task.dueDate == null -> MaterialTheme.colorScheme.outline
            task.dueDate < System.currentTimeMillis() -> Color(0xFFE57373)
            task.dueDate - System.currentTimeMillis() < 86400000 -> Color(0xFFFFB74D)
            else -> MaterialTheme.colorScheme.primary
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {

            Text(task.title, style = MaterialTheme.typography.titleLarge)

            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    task.category,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            task.dueDate?.let { date ->
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .background(dueColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        formatDueDate(date),
                        color = dueColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}