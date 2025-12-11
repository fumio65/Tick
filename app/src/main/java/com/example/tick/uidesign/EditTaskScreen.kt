package com.example.tick.uidesign

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tick.viewmodel.TaskViewModel
import java.util.Calendar
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: Int,
    viewModel: TaskViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    val allTasks = viewModel.tasks.collectAsState().value
    val task = allTasks.find { it.id == taskId }

    if (task == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Edit Task",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Task not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    // Task fields
    var title by rememberSaveable(taskId) { mutableStateOf(task.title) }
    var description by rememberSaveable(taskId) { mutableStateOf(task.description) }
    var category by rememberSaveable(taskId) { mutableStateOf(task.category) }
    var scheduledDate by rememberSaveable(taskId) { mutableStateOf(task.scheduledDate) }
    var selectedColor by rememberSaveable(taskId) {
        mutableStateOf(task.color?.let { colorInt ->
            val color = Color(colorInt)
            taskColors.find { it.first.value == color.value }?.second
        })
    }

    // Time blocking fields
    var isTimeBlockEnabled by rememberSaveable(taskId) { mutableStateOf(task.isTimeBlocked) }
    var selectedDuration by rememberSaveable(taskId) {
        mutableStateOf(task.duration ?: 30)
    }
    var selectedStartTime by rememberSaveable(taskId) { mutableStateOf(task.startTime) }
    var selectedEndTime by rememberSaveable(taskId) { mutableStateOf(task.endTime) }

    // Dialogs
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState()
    val startTimePickerState = rememberTimePickerState()
    val endTimePickerState = rememberTimePickerState()

    // Animation states
    val scrollState = rememberScrollState()
    var isVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Load the existing scheduled date and time
    LaunchedEffect(taskId) {
        task.scheduledDate?.let { ms ->
            val cal = Calendar.getInstance().apply { timeInMillis = ms }
            timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
            timePickerState.minute = cal.get(Calendar.MINUTE)
        }
    }

    // Get the selected color object
    val selectedColorObj = taskColors.find { it.second == selectedColor }?.first

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Task",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // TITLE INPUT
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Task Title",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                "What needs to be done?",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // DESCRIPTION
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) +
                        slideInVertically(initialOffsetY = { -20 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = {
                            Text(
                                "Add more details...",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // CATEGORY
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) +
                        slideInVertically(initialOffsetY = { -20 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    CategoryDropdown(
                        selectedCategory = category,
                        categories = viewModel.categories,
                        onCategorySelected = { category = it }
                    )
                }
            }

            // COLOR SELECTION
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 300)) +
                        slideInVertically(initialOffsetY = { -20 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showColorPicker = !showColorPicker },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            selectedColorObj ?: MaterialTheme.colorScheme.primary
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Task Color",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selectedColor != null) {
                                        Text(
                                            text = selectedColor!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            if (selectedColor != null) {
                                IconButton(
                                    onClick = { selectedColor = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showColorPicker,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(taskColors) { (color, colorName) ->
                                    ColorOption(
                                        color = color,
                                        isSelected = selectedColor == colorName,
                                        onClick = {
                                            selectedColor = if (selectedColor == colorName) null else colorName
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SCHEDULE CARD WITH TIME BLOCKING
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) +
                        slideInVertically(initialOffsetY = { -20 })
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            AnimatedVisibility(
                                visible = scheduledDate != null || isTimeBlockEnabled,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Surface(
                                    onClick = {
                                        scheduledDate = null
                                        isTimeBlockEnabled = false
                                        selectedStartTime = null
                                        selectedEndTime = null
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Clear",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Time Block Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Time Block",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isTimeBlockEnabled) "Reserve specific time" else "Set due date only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isTimeBlockEnabled,
                                onCheckedChange = {
                                    isTimeBlockEnabled = it
                                    if (it && scheduledDate == null) {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        scheduledDate = cal.timeInMillis
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        // Show different UI based on toggle
                        if (!isTimeBlockEnabled) {
                            // REGULAR MODE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ElevatedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 18.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface,
                                        contentColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.elevatedButtonElevation(
                                        defaultElevation = if (scheduledDate != null) 2.dp else 0.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        scheduledDate?.let {
                                            val cal = Calendar.getInstance().apply { timeInMillis = it }
                                            "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
                                        } ?: "Date",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }

                                ElevatedButton(
                                    onClick = {
                                        if (scheduledDate == null) {
                                            val cal = Calendar.getInstance()
                                            cal.set(Calendar.HOUR_OF_DAY, 0)
                                            cal.set(Calendar.MINUTE, 0)
                                            cal.set(Calendar.SECOND, 0)
                                            cal.set(Calendar.MILLISECOND, 0)
                                            scheduledDate = cal.timeInMillis
                                        }
                                        showTimePicker = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 18.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface,
                                        contentColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    ),
                                    elevation = ButtonDefaults.elevatedButtonElevation(
                                        defaultElevation = if (scheduledDate != null) 2.dp else 0.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        scheduledDate?.let {
                                            val cal = Calendar.getInstance().apply { timeInMillis = it }
                                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                                            val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
                                            "$hour:$minute"
                                        } ?: "Time",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        } else {
                            // TIMEBLOCK MODE
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date button
                                ElevatedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(vertical = 18.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface,
                                        contentColor = if (scheduledDate != null)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        scheduledDate?.let {
                                            val cal = Calendar.getInstance().apply { timeInMillis = it }
                                            "${getMonthName(cal.get(Calendar.MONTH))} ${cal.get(Calendar.DAY_OF_MONTH)}, ${cal.get(Calendar.YEAR)}"
                                        } ?: "Select Date",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }

                                // Duration chips
                                Text(
                                    "Duration",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(listOf(15, 30, 45, 60, 90, 120)) { duration ->
                                        FilterChip(
                                            selected = selectedDuration == duration,
                                            onClick = {
                                                selectedDuration = duration
                                                selectedStartTime?.let { start ->
                                                    selectedEndTime = start + (duration * 60 * 1000)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    when {
                                                        duration < 60 -> "${duration}m"
                                                        duration == 60 -> "1h"
                                                        else -> "${duration / 60}h"
                                                    }
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                // Start and End time buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ElevatedButton(
                                        onClick = { showStartTimePicker = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 18.dp),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (selectedStartTime != null)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                selectedStartTime?.let {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                                                    "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"
                                                } ?: "--:--",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    ElevatedButton(
                                        onClick = { showEndTimePicker = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(vertical = 18.dp),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (selectedEndTime != null)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("End", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                selectedEndTime?.let {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = it }
                                                    "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"
                                                } ?: "--:--",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }

                                // Duration preview
                                if (selectedStartTime != null && selectedEndTime != null) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "Time Block Duration",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    formatDuration(selectedStartTime!!, selectedEndTime!!),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                Icons.Default.AccessTime,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ACTION BUTTONS
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 500)) +
                        slideInVertically(initialOffsetY = { 20 })
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isSaving) {
                                onCancel()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(18.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "Cancel",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank() && !isSaving) {
                                isSaving = true

                                coroutineScope.launch {
                                    try {
                                        val colorToSave = taskColors.find { it.second == selectedColor }?.first

                                        viewModel.editTask(
                                            taskId = task.id,
                                            newTitle = title,
                                            newDescription = description,
                                            newCategory = category,
                                            newDueDate = scheduledDate,
                                            context = context,
                                            newColor = colorToSave,
                                            isTimeBlocked = isTimeBlockEnabled,
                                            startTime = selectedStartTime,
                                            endTime = selectedEndTime
                                        )

                                        delay(50)
                                        onSave()
                                    } catch (e: Exception) {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank() && !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(18.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Save Changes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Custom Date Picker Dialog
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { dateMillis ->
                scheduledDate = dateMillis
                showDatePicker = false
                // Only open time picker if NOT in time block mode
                if (!isTimeBlockEnabled) {
                    showTimePicker = true
                }
            },
            initialDate = scheduledDate
        )
    }

    // Regular Time Picker Dialog (for non-timeblock mode)
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTimePicker = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scheduledDate?.let { dateMillis ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = dateMillis
                                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(Calendar.MINUTE, timePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    scheduledDate = cal.timeInMillis
                                }
                                showTimePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Text("Set Time", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        Dialog(onDismissRequest = { showStartTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Select Start Time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TimePicker(
                        state = startTimePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showStartTimePicker = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scheduledDate?.let { dateMillis ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = dateMillis
                                        set(Calendar.HOUR_OF_DAY, startTimePickerState.hour)
                                        set(Calendar.MINUTE, startTimePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedStartTime = cal.timeInMillis
                                    selectedEndTime = selectedStartTime!! + (selectedDuration * 60 * 1000)
                                }
                                showStartTimePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Set Start Time", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        Dialog(onDismissRequest = { showEndTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Select End Time",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TimePicker(
                        state = endTimePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectorColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showEndTimePicker = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                scheduledDate?.let { dateMillis ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = dateMillis
                                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                                        set(Calendar.MINUTE, endTimePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedEndTime = cal.timeInMillis
                                    selectedStartTime?.let { start ->
                                        selectedDuration = ((selectedEndTime!! - start) / (60 * 1000)).toInt()
                                    }
                                }
                                showEndTimePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text("Set End Time", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}