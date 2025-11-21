package com.example.tick.uidesign

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tick.util.formatDueDate
import com.example.tick.viewmodel.TaskViewModel

enum class TaskFilter { ALL, COMPLETED, PENDING }

/**
 * Modern MainScreen:
 * - Collapsible "Completed" section kept (user requested it)
 * - Compact cards with spacing
 * - Side-only swipe-to-delete panel that grows with swipe progress
 * - Uses Material3 SwipeToDismissBox (keeps your earlier approach) but reads progress only
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TaskViewModel,
    onAddTaskClick: () -> Unit,
    onEditTaskClick: (Int) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var showMenu by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(true) }

    val filtered = when (filter) {
        TaskFilter.ALL -> tasks
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
    }

    val pending = filtered.filter { !it.isCompleted }
    val completed = filtered.filter { it.isCompleted }

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
                            Icon(Icons.Default.MoreVert, contentDescription = "Filter")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("All Tasks") }, onClick = {
                                filter = TaskFilter.ALL
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Completed") }, onClick = {
                                filter = TaskFilter.COMPLETED
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Pending") }, onClick = {
                                filter = TaskFilter.PENDING
                                showMenu = false
                            })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTaskClick) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { paddingValues ->

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks found.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Pending tasks first
            items(pending, key = { it.id }) { task ->
                // state for this swipe
                val dismissState = rememberSwipeToDismissBoxState(
                    positionalThreshold = { it * 0.5f } // halfway
                )

                // When swipe finishes to EndToStart, delete and reset state
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                    LaunchedEffect(task.id) {
                        viewModel.deleteTask(task.id, context)
                        dismissState.reset()
                    }
                }

                // Background content: draw a RIGHT-side panel that grows as user swipes.
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        // progress is 0..1 describing swipe fraction (safe to read)
                        val progress = dismissState.progress.coerceIn(0f, 1f)

                        // We don't want the red to bleed above/below card; match the card vertical padding.
                        // Wrap with the same vertical padding used for cards.
                        if (progress > 0f) {
                            // animate width smoothly when progress changes
                            val panelWidth by animateDpAsState(
                                targetValue = sidePanelWidth(progress),
                                animationSpec = tween(durationMillis = 120)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 12.dp), // same padding as TaskCard
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 64.dp) // keep a sensible min height so icon vertically centers
                                        .fillMaxHeight()
                                        .width(panelWidth)
                                        .background(Color(0xFFE53935), shape = RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = (0.4f + 0.6f * progress))
                                    )
                                }
                            }
                        } else {
                            // nothing shown when not swiping
                            Spacer(modifier = Modifier.height(0.dp))
                        }
                    }
                ) {
                    // The actual card content
                    TaskCardCompact(
                        task = task,
                        onEdit = { onEditTaskClick(task.id) },
                        onToggle = { viewModel.toggleComplete(task.id) }
                    )
                }
            }

            // Completed section header (collapsible)
            if (completed.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        IconButton(onClick = { showCompleted = !showCompleted }) {
                            Icon(
                                imageVector = if (showCompleted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle completed"
                            )
                        }
                    }
                }

                if (showCompleted) {
                    items(completed, key = { it.id }) { task ->
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
                                val progress = dismissState.progress.coerceIn(0f, 1f)
                                if (progress > 0f) {
                                    val panelWidth by animateDpAsState(
                                        targetValue = sidePanelWidth(progress),
                                        animationSpec = tween(durationMillis = 120)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 64.dp)
                                                .fillMaxHeight()
                                                .width(panelWidth)
                                                .background(Color(0xFFE53935), shape = RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.White.copy(alpha = (0.4f + 0.6f * progress))
                                            )
                                        }
                                    }
                                } else Spacer(modifier = Modifier.height(0.dp))
                            }
                        ) {
                            TaskCardCompact(
                                task = task,
                                onEdit = { onEditTaskClick(task.id) },
                                onToggle = { viewModel.toggleComplete(task.id) },
                                isCompleted = true
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper: compute side panel width from swipe progress.
 * - progress in [0,1] -> width in dp.
 * - tweak numbers as you like to make the swipe feel right.
 */
private fun sidePanelWidth(progress: Float): Dp {
    val min = 48.dp.value    // show a small icon early
    val max = 140.dp.value   // maximum width when fully swiped
    val actual = min + (max - min) * progress
    return actual.dp
}

/**
 * Compact, modern task card used for both pending and completed tasks.
 * Smaller, better spacing, subtle elevation.
 */
@Composable
private fun TaskCardCompact(
    task: com.example.tick.data.Task,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    isCompleted: Boolean = false
) {
    val dueColor: Color = when {
        task.dueDate == null -> MaterialTheme.colorScheme.outline
        task.dueDate < System.currentTimeMillis() -> Color(0xFFE57373)
        task.dueDate - System.currentTimeMillis() < 86_400_000 -> Color(0xFFFFB74D)
        else -> MaterialTheme.colorScheme.primary
    }

    // Visual differences for completed items
    val containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val titleColor = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // important: same vertical padding used for swipe background to avoid bleed
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 2
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    task.dueDate?.let { ts ->
                        Box(
                            modifier = Modifier
                                .background(dueColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(formatDueDate(ts), style = MaterialTheme.typography.labelSmall, color = dueColor)
                        }
                    }
                }

                // checkbox on the right
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}
