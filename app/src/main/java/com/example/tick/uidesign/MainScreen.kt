        package com.example.tick.uidesign

        import androidx.compose.animation.AnimatedVisibility
        import androidx.compose.animation.animateContentSize
        import androidx.compose.animation.core.Spring
        import androidx.compose.animation.core.animateFloatAsState
        import androidx.compose.animation.core.spring
        import androidx.compose.animation.fadeIn
        import androidx.compose.animation.fadeOut
        import androidx.compose.animation.scaleIn
        import androidx.compose.animation.scaleOut
        import androidx.compose.foundation.background
        import androidx.compose.foundation.clickable
        import androidx.compose.foundation.layout.*
        import androidx.compose.foundation.lazy.LazyColumn
        import androidx.compose.foundation.lazy.items
        import androidx.compose.foundation.shape.CircleShape
        import androidx.compose.foundation.shape.RoundedCornerShape
        import androidx.compose.material.icons.Icons
        import androidx.compose.material.icons.filled.*
        import androidx.compose.material.icons.outlined.*
        import androidx.compose.material3.*
        import androidx.compose.material3.SwipeToDismissBox
        import androidx.compose.material3.SwipeToDismissBoxValue
        import androidx.compose.material3.rememberSwipeToDismissBoxState
        import androidx.compose.runtime.*
        import androidx.compose.ui.Alignment
        import androidx.compose.ui.Modifier
        import androidx.compose.ui.draw.clip
        import androidx.compose.ui.draw.scale
        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.platform.LocalContext
        import androidx.compose.ui.text.font.FontWeight
        import androidx.compose.ui.text.style.TextAlign
        import androidx.compose.ui.text.style.TextDecoration
        import androidx.compose.ui.text.style.TextOverflow
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.sp
        import com.example.tick.util.formatDueDate
        import com.example.tick.viewmodel.TaskViewModel
        import kotlinx.coroutines.launch
        import java.text.SimpleDateFormat
        import androidx.compose.foundation.BorderStroke
        import androidx.compose.runtime.saveable.rememberSaveable
        import java.util.*
        import androidx.lifecycle.viewmodel.compose.viewModel
        import androidx.compose.foundation.ExperimentalFoundationApi
        import androidx.compose.foundation.combinedClickable
        import androidx.compose.ui.window.Dialog

        enum class TaskFilter { ALL, COMPLETED, PENDING }

        data class CalendarDay(
            val date: Long,
            val dayOfMonth: Int,
            val dayOfWeek: String,
            val monthYear: String,
            val isToday: Boolean,
            val isCurrentMonth: Boolean,
            val taskCount: Int,
            val hasOverdue: Boolean
        )

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun MainScreen(
            viewModel: TaskViewModel = viewModel(),
            onAddTaskClick: () -> Unit,
            onEditTaskClick: (Int) -> Unit
        ) {
            val tasks by viewModel.tasks.collectAsState()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            var filter by remember { mutableStateOf(TaskFilter.ALL) }
            var showMenu by remember { mutableStateOf(false) }
            var showCompleted by remember { mutableStateOf(true) }
            var selectedDate by remember { mutableStateOf<Long?>(null) }
            var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
            var taskToDelete by remember { mutableStateOf<com.example.tick.data.Task?>(null) }
            var showDeleteDialog by remember { mutableStateOf(false) }
            var showCalendar by rememberSaveable  { mutableStateOf(true) }

            var taskToPreview by remember { mutableStateOf<com.example.tick.data.Task?>(null) }
            var showPreviewDialog by remember { mutableStateOf(false) }

            val pendingDeletion = remember { mutableStateMapOf<Int, com.example.tick.data.Task>() }

            /* ---------------------- CALENDAR GENERATION ---------------------- */

            val calendarDays = remember(currentMonth, tasks, pendingDeletion) {
                val calendar = Calendar.getInstance()
                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val today = todayCalendar.timeInMillis

                calendar.time = currentMonth.time
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)

                val days = mutableListOf<CalendarDay>()
                val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                calendar.add(Calendar.DAY_OF_MONTH, -(firstDayOfMonth - 1))
                val startDate = calendar.timeInMillis
                val totalDays = 42

                for (i in 0 until totalDays) {
                    calendar.timeInMillis = startDate
                    calendar.add(Calendar.DAY_OF_MONTH, i)

                    val dayStart = Calendar.getInstance().apply {
                        set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                        set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val dayEnd = dayStart + (24 * 60 * 60 * 1000 - 1)

                    val tasksOnDay = tasks.filter { task ->
                        task.scheduledDate  != null &&
                                task.scheduledDate  in dayStart..dayEnd &&
                                !pendingDeletion.containsKey(task.id)
                    }

                    val hasOverdue = tasksOnDay.any {
                        it.scheduledDate !! < System.currentTimeMillis() && !it.isCompleted
                    }

                    val isCurrentMonthDay =
                        calendar.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)

                    days.add(
                        CalendarDay(
                            date = dayStart,
                            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH),
                            dayOfWeek = dayFormat.format(calendar.time),
                            monthYear = monthYearFormat.format(currentMonth.time),
                            isToday = dayStart == today,
                            isCurrentMonth = isCurrentMonthDay,
                            taskCount = tasksOnDay.size,
                            hasOverdue = hasOverdue
                        )
                    )
                }
                days
            }

            /* ---------------------- FILTERED TASK LIST ---------------------- */

            val displayed = remember(tasks, pendingDeletion, filter, selectedDate) {
                tasks.filter { t ->
                    val matchesFilter = when (filter) {
                        TaskFilter.ALL -> true
                        TaskFilter.COMPLETED -> t.isCompleted
                        TaskFilter.PENDING -> !t.isCompleted
                    }

                    val matchesDate =
                        selectedDate == null || (t.scheduledDate  != null && isSameDay(t.scheduledDate , selectedDate!!))

                    matchesFilter && matchesDate && !pendingDeletion.containsKey(t.id)
                }
            }

            val pending = displayed.filter { !it.isCompleted }
            val completed = displayed.filter { it.isCompleted }

            /* ---------------------- DELETE CONFIRMATION ---------------------- */

            if (showDeleteDialog && taskToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        taskToDelete = null
                    },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            "Delete Task?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            "Are you sure you want to delete \"${taskToDelete?.title}\"? This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                taskToDelete?.let {
                                    viewModel.deleteTask(it, context)
                                }
                                showDeleteDialog = false
                                taskToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                taskToDelete = null
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }

            /* ---------------------- TASK PREVIEW DIALOG ---------------------- */

            if (showPreviewDialog && taskToPreview != null) {
                TaskPreviewDialog(
                    task = taskToPreview!!,
                    viewModel = viewModel,  // ⬅️ ADD THIS LINE
                    onDismiss = {
                        showPreviewDialog = false
                        taskToPreview = null
                    },
                    onEdit = {
                        showPreviewDialog = false
                        onEditTaskClick(taskToPreview!!.id)
                        taskToPreview = null
                    },
                    onDelete = {
                        showPreviewDialog = false
                        taskToDelete = taskToPreview
                        taskToPreview = null
                        showDeleteDialog = true
                    },
                    onToggleComplete = {
                        viewModel.toggleComplete(taskToPreview!!.id)
                    }
                )
            }

            /* ---------------------- UI LAYOUT ---------------------- */

            Scaffold(
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(12.dp),
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            actionColor = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Tick",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${pending.size} pending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showCalendar = !showCalendar }) {
                                Icon(
                                    if (showCalendar) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                    contentDescription = "Toggle Calendar"
                                )
                            }

                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Filter")
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Tasks", fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            filter = TaskFilter.ALL
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.List, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Completed", fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            filter = TaskFilter.COMPLETED
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pending", fontWeight = FontWeight.Medium) },
                                        onClick = {
                                            filter = TaskFilter.PENDING
                                            showMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Schedule, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = onAddTaskClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add task")
                        Spacer(Modifier.width(8.dp))
                        Text("New Task", fontWeight = FontWeight.SemiBold)
                    }
                }
            ) { paddingValues ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    /* ============ CALENDAR CARD ============ */

                    AnimatedVisibility(
                        visible = showCalendar,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        CalendarCard(
                            calendarDays = calendarDays,
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            onMonthChange = { newMonth -> currentMonth = newMonth },
                            onClearFilter = { selectedDate = null },
                            onSelectDate = { selectedDate = it }
                        )
                    }

                    /* ============ STAT BAR ============ */

                    if (displayed.isNotEmpty()) {
                        StatBar(
                            displayed = displayed,
                            pending = pending,
                            completed = completed
                        )
                    }

                    /* ============ TASK LIST ============ */

                    TaskListSection(
                        displayed = displayed,
                        pending = pending,
                        completed = completed,
                        showCompleted = showCompleted,
                        onToggleCompletedSection = { showCompleted = !showCompleted },
                        snackbarHostState = snackbarHostState,
                        pendingDeletion = pendingDeletion,
                        viewModel = viewModel,
                        context = context,
                        onEditTaskClick = onEditTaskClick,
                        onConfirmDelete = {
                            taskToDelete = it
                            showDeleteDialog = true
                        },
                        onPreview = {
                            taskToPreview = it
                            showPreviewDialog = true
                        }
                    )
                }
            }
        }

        /* ===================== HELPER: CHECK SAME DAY ===================== */

        private fun isSameDay(taskDate: Long, selected: Long): Boolean {
            val t = Calendar.getInstance().apply { timeInMillis = taskDate }
            val s = Calendar.getInstance().apply { timeInMillis = selected }

            return t.get(Calendar.YEAR) == s.get(Calendar.YEAR) &&
                    t.get(Calendar.MONTH) == s.get(Calendar.MONTH) &&
                    t.get(Calendar.DAY_OF_MONTH) == s.get(Calendar.DAY_OF_MONTH)
        }


        /* ---------------------- CALENDAR CARD  ---------------------- */

        @Composable
        private fun CalendarCard(
            calendarDays: List<CalendarDay>,
            currentMonth: Calendar,
            selectedDate: Long?,
            onMonthChange: (Calendar) -> Unit,
            onClearFilter: () -> Unit,
            onSelectDate: (Long) -> Unit
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = null
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    /* MONTH HEADER */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                val newMonth = currentMonth.clone() as Calendar
                                newMonth.add(Calendar.MONTH, -1)
                                onMonthChange(newMonth)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }

                        Text(
                            text = calendarDays.firstOrNull { it.isCurrentMonth }?.monthYear
                                ?: SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        FilledIconButton(
                            onClick = {
                                val newMonth = currentMonth.clone() as Calendar
                                newMonth.add(Calendar.MONTH, 1)
                                onMonthChange(newMonth)
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    /* DAY HEADERS */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    /* CALENDAR GRID */
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        calendarDays.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                week.forEach { day ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CalendarDayCell(
                                            day = day,
                                            isSelected = selectedDate == day.date,
                                            onClick = {
                                                if (selectedDate == day.date) {
                                                    onClearFilter()  // Double-click behavior: deselect
                                                } else {
                                                    onSelectDate(day.date)  // First click: select
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        /* ---------------------- STAT BAR ---------------------- */

        @Composable
        private fun StatBar(
            displayed: List<com.example.tick.data.Task>,
            pending: List<com.example.tick.data.Task>,
            completed: List<com.example.tick.data.Task>
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Total",
                    value = displayed.size.toString(),
                    icon = Icons.Outlined.List,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Pending",
                    value = pending.size.toString(),
                    icon = Icons.Outlined.Schedule,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.tertiary
                )
                StatCard(
                    label = "Done",
                    value = completed.size.toString(),
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        /* ---------------------- TASK LIST ---------------------- */

        @Composable
        private fun TaskListSection(
            displayed: List<com.example.tick.data.Task>,
            pending: List<com.example.tick.data.Task>,
            completed: List<com.example.tick.data.Task>,
            showCompleted: Boolean,
            onToggleCompletedSection: () -> Unit,
            snackbarHostState: SnackbarHostState,
            pendingDeletion: MutableMap<Int, com.example.tick.data.Task>,
            viewModel: TaskViewModel,
            context: android.content.Context,
            onEditTaskClick: (Int) -> Unit,
            onConfirmDelete: (com.example.tick.data.Task) -> Unit,
            onPreview: (com.example.tick.data.Task) -> Unit
        ) {
            if (displayed.isEmpty()) {
                EmptyState(selectedDateNotNull = false)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pending Section
                    if (pending.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Pending Tasks",
                                count = pending.size,
                                icon = Icons.Outlined.Schedule
                            )
                        }
                        items(pending, key = { it.id }) { task ->
                            SwipeToDismissTaskItem(
                                task = task,
                                snackbarHostState = snackbarHostState,
                                pendingDeletion = pendingDeletion,
                                viewModel = viewModel,
                                context = context,
                                onEdit = { onEditTaskClick(task.id) },
                                onToggle = { viewModel.toggleComplete(task.id) },
                                isCompleted = false,
                                onDeleteClick = { onConfirmDelete(task) },
                                onPreview = { onPreview(task) }
                            )
                        }
                    }

                    // Completed Section
                    if (completed.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    title = "Completed Tasks",
                                    count = completed.size,
                                    icon = Icons.Outlined.CheckCircle,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledIconButton(
                                    onClick = onToggleCompletedSection,
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(
                                        if (showCompleted) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle completed",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (showCompleted) {
                            items(completed, key = { it.id }) { task ->
                                SwipeToDismissTaskItem(
                                    task = task,
                                    snackbarHostState = snackbarHostState,
                                    pendingDeletion = pendingDeletion,
                                    viewModel = viewModel,
                                    context = context,
                                    onEdit = { onEditTaskClick(task.id) },
                                    onToggle = { viewModel.toggleComplete(task.id) },
                                    isCompleted = true,
                                    onDeleteClick = { onConfirmDelete(task) },
                                    onPreview = { onPreview(task) }
                                )
                            }
                        }
                    }
                }
            }
        }

        /* ---------------------- EMPTY STATE ---------------------- */

        @Composable
        private fun EmptyState(selectedDateNotNull: Boolean) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = if (selectedDateNotNull) "No tasks on this day" else "No tasks found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Tap the + button to create your first task",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        /* ---------------------- STAT CARD ---------------------- */

        @Composable
        private fun StatCard(
            label: String,
            value: String,
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            modifier: Modifier = Modifier,
            color: Color = MaterialTheme.colorScheme.secondary
        ) {
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        /* ---------------------- SECTION HEADER ---------------------- */

        @Composable
        private fun SectionHeader(
            title: String,
            count: Int,
            icon: androidx.compose.ui.graphics.vector.ImageVector,
            modifier: Modifier = Modifier
        ) {
            Row(
                modifier = modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }



        /* ---------------------- CALENDAR CELL ---------------------- */

        @Composable
        private fun CalendarDayCell(
            day: CalendarDay,
            isSelected: Boolean,
            onClick: () -> Unit
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "scale"
            )

            val backgroundColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                day.isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }

            val contentColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                day.isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .clickable(enabled = day.isCurrentMonth, onClick = onClick)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        fontSize = 14.sp
                    )

                    if (day.taskCount > 0 && day.isCurrentMonth) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(minOf(day.taskCount, 3)) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (day.hasOverdue) MaterialTheme.colorScheme.error
                                            else if (isSelected) contentColor
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }


        /* ---------------------- SWIPE TO DISMISS ---------------------- */

        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        private fun SwipeToDismissTaskItem(
            task: com.example.tick.data.Task,
            snackbarHostState: SnackbarHostState,
            pendingDeletion: MutableMap<Int, com.example.tick.data.Task>,
            viewModel: TaskViewModel,
            context: android.content.Context,
            onEdit: () -> Unit,
            onToggle: () -> Unit,
            onDeleteClick: () -> Unit,
            isCompleted: Boolean = false,
            onPreview: () -> Unit,
        ) {
            val coroutineScope = rememberCoroutineScope()
            var show by remember { mutableStateOf(true) }

            if (!show) {
                // When swiped away, don't render anything
                LaunchedEffect(Unit) {
                    val result = snackbarHostState.showSnackbar(
                        message = "Task deleted",
                        actionLabel = "UNDO",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        // User clicked UNDO - restore the card
                        show = true
                        pendingDeletion.remove(task.id)
                    } else {
                        // Proceed with deletion
                        if (pendingDeletion.containsKey(task.id)) {
                            viewModel.deleteTask(task, context)
                            pendingDeletion.remove(task.id)
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = show,
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically()
            ) {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            show = false
                            pendingDeletion[task.id] = task
                            true
                        } else {
                            false
                        }
                    },
                    positionalThreshold = { it * 0.5f }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val dismissDirection = dismissState.dismissDirection
                        if (dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Delete",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                ) {
                    ModernTaskCard(
                        task = task,
                        onEdit = onEdit,
                        onToggle = onToggle,
                        onDeleteClick = onDeleteClick,
                        isCompleted = isCompleted,
                        onPreview = onPreview
                    )
                }
            }
        }



        /* ---------------------- MODERN TASK CARD ---------------------- */
        @Composable
        private fun ModernTaskCard(
            task: com.example.tick.data.Task,
            onEdit: () -> Unit,
            onToggle: () -> Unit,
            onDeleteClick: () -> Unit,
            isCompleted: Boolean = false,
            onPreview: () -> Unit,
        ) {
            // Convert the saved Int color back to a Color object
            val taskColor = task.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

            val dueColor = when {
                task.scheduledDate  == null -> MaterialTheme.colorScheme.outline
                task.scheduledDate  < System.currentTimeMillis() && !isCompleted -> MaterialTheme.colorScheme.error
                task.scheduledDate  - System.currentTimeMillis() < 86_400_000 -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),

                // 🔥 Remove the thick gray border/shadow
                elevation = CardDefaults.cardElevation(0.dp),

                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else
                        taskColor.copy(alpha = 0.08f)
                ),
                border = null
            ) {
                @OptIn(ExperimentalFoundationApi::class)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onEdit() },
                            onLongClick = { onPreview() }
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (task.isCompleted)
                                    taskColor
                                else
                                    Color.Transparent
                            )
                            .then(
                                if (!task.isCompleted) Modifier.clip(CircleShape)
                                else Modifier
                            )
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Transparent)
                                    .background(
                                        taskColor.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Content
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Title
                        Text(
                            text = task.title,
                            color = if (isCompleted)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp
                        )

                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                color = if (isCompleted)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                fontSize = 13.sp
                            )
                        }

                        // Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCompleted)
                                    taskColor.copy(alpha = 0.15f)
                                else
                                    taskColor.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted)
                                                    taskColor.copy(alpha = 0.5f)
                                                else
                                                    taskColor
                                            )
                                    )
                                    Text(
                                        text = task.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isCompleted)
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Show timeblock info if it's a timeblocked task
                            if (task.isTimeBlocked && task.startTime != null && task.endTime != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCompleted)
                                        dueColor.copy(alpha = 0.1f)
                                    else
                                        dueColor.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isCompleted)
                                                dueColor.copy(alpha = 0.5f)
                                            else
                                                dueColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = formatTimeBlock(task.startTime, task.endTime),
                                            color = if (isCompleted)
                                                dueColor.copy(alpha = 0.5f)
                                            else
                                                dueColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                // Show regular due date
                                task.scheduledDate ?.let {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCompleted)
                                            dueColor.copy(alpha = 0.1f)
                                        else
                                            dueColor.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (isCompleted)
                                                    dueColor.copy(alpha = 0.5f)
                                                else
                                                    dueColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = formatDueDate(it),
                                                color = if (isCompleted)
                                                    dueColor.copy(alpha = 0.5f)
                                                else
                                                    dueColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Format timeblock times (e.g., "9:00 AM - 1:00 PM")
        private fun formatTimeBlock(startTime: Long, endTime: Long): String {
            val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
            val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            return "${timeFormat.format(startCal.time)} - ${timeFormat.format(endCal.time)}"
        }



        /* ---------------------- TASK PREVIEW DIALOG ---------------------- */

        @Composable
        private fun TaskPreviewDialog(
            task: com.example.tick.data.Task,
            viewModel: TaskViewModel,
            onDismiss: () -> Unit,
            onEdit: () -> Unit,
            onDelete: () -> Unit,
            onToggleComplete: () -> Unit
        ) {
            val taskColor = task.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

            // Capture task ID once to prevent recomposition issues
            val taskId = remember { task.id }
            val subtasks by viewModel.getSubtasksForTask(taskId).collectAsState()

            Dialog(
                onDismissRequest = onDismiss
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header with close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Task Details",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Title with status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }

                        // Description
                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Info Cards
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Category Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = taskColor.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(taskColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Label,
                                            contentDescription = null,
                                            tint = taskColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Category",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            task.category,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Time Block or Due Date Card
                            if (task.isTimeBlocked && task.startTime != null && task.endTime != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                "Time Block",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                formatTimeBlock(task.startTime, task.endTime),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            } else if (task.scheduledDate != null) {
                                val isOverdue = task.scheduledDate < System.currentTimeMillis() && !task.isCompleted
                                val cardColor = if (isOverdue)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = cardColor,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isOverdue) MaterialTheme.colorScheme.errorContainer
                                                    else MaterialTheme.colorScheme.primaryContainer
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = if (isOverdue) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                if (isOverdue) "Overdue" else "Due Date",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isOverdue) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                formatDueDate(task.scheduledDate),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Status Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (task.isCompleted)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (task.isCompleted)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Status",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            if (task.isCompleted) "Completed" else "Pending",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // ============ SUBTASKS SECTION ============
                        if (subtasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            // Subtasks Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Subtasks",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val completedCount = remember(subtasks) { subtasks.count { it.isCompleted } }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "$completedCount/${subtasks.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subtasks List
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    subtasks.forEach { subtask ->
                                        key(subtask.id) {
                                            SubtaskItem(
                                                subtask = subtask,
                                                onToggle = {
                                                    viewModel.toggleSubtaskComplete(subtask.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Toggle Complete Button
                            OutlinedButton(
                                onClick = {
                                    onToggleComplete()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    if (task.isCompleted) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (task.isCompleted) "Incomplete" else "Complete",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }

                            // Edit Button
                            Button(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Edit",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // ============ SUBTASK COMPONENTS ============


        @Composable
        private fun SubtaskItem(
            subtask: com.example.tick.data.Subtask,
            onToggle: () -> Unit
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggle() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (subtask.isCompleted)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (subtask.isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Subtask text
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (subtask.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
            }
        }

//        I noticed the order of task card is alphabetical order from task title