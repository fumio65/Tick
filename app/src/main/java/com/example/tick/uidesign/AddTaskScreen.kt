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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tick.viewmodel.TaskViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.rotate
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Predefined task colors with better palette
val taskColors = listOf(
    Color(0xFFFF6B6B) to "Sunset Red",
    Color(0xFFFF8ED4) to "Pink Blossom",
    Color(0xFFBB86FC) to "Lavender",
    Color(0xFF7C4DFF) to "Deep Purple",
    Color(0xFF448AFF) to "Ocean Blue",
    Color(0xFF40C4FF) to "Sky Blue",
    Color(0xFF1DE9B6) to "Turquoise",
    Color(0xFF69F0AE) to "Mint Green",
    Color(0xFFB9F6CA) to "Spring Green",
    Color(0xFFFFD740) to "Golden",
    Color(0xFFFFAB40) to "Amber Glow",
    Color(0xFFFF6E40) to "Coral",
    Color(0xFFBCAAA4) to "Warm Taupe",
    Color(0xFF90A4AE) to "Steel Blue"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: TaskViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDueDate by viewModel.selectedDueDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState()

    // Animation states
    val scrollState = rememberScrollState()
    var isVisible by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(selectedDueDate) {
        selectedDueDate?.let { ms ->
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
                        "Create New Task",
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

            // TITLE INPUT with gradient focus
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

            // DESCRIPTION with modern styling
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

            // CATEGORY with enhanced design
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
                        selectedCategory = selectedCategory,
                        categories = viewModel.categories,
                        onCategorySelected = { category ->
                            viewModel.updateSelectedCategory(category)
                        }
                    )
                }
            }

            // COLOR SELECTION - Collapsible
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

            // SCHEDULE CARD - Enhanced
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
                                visible = selectedDueDate != null,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Surface(
                                    onClick = { viewModel.setDueDate(null) },
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Date Button - Enhanced
                            ElevatedButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 18.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = if (selectedDueDate != null)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selectedDueDate != null)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.elevatedButtonElevation(
                                    defaultElevation = if (selectedDueDate != null) 2.dp else 0.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    selectedDueDate?.let {
                                        val cal = Calendar.getInstance().apply { timeInMillis = it }
                                        "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}"
                                    } ?: "Date",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }

                            // Time Button - Enhanced
                            ElevatedButton(
                                onClick = {
                                    if (selectedDueDate == null) {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        viewModel.setDueDate(cal.timeInMillis)
                                    }
                                    showTimePicker = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 18.dp),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = if (selectedDueDate != null)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface,
                                    contentColor = if (selectedDueDate != null)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = ButtonDefaults.elevatedButtonElevation(
                                    defaultElevation = if (selectedDueDate != null) 2.dp else 0.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    selectedDueDate?.let {
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ACTION BUTTONS - Enhanced
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
                                viewModel.updateSelectedCategory(null)
                                viewModel.setDueDate(null)
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
                                        // Get the actual Color object from the selected color name
                                        val colorToSave = taskColors.find { it.second == selectedColor }?.first

                                        // Pass the color to addTask
                                        viewModel.addTask(title, description, context, colorToSave)

                                        // Small delay to ensure state is saved
                                        delay(50)

                                        // Clear the form state
                                        viewModel.updateSelectedCategory(null)
                                        viewModel.setDueDate(null)

                                        // Navigate back
                                        onSave()
                                    } catch (e: Exception) {
                                        // Handle any errors
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
                                "Save Task",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
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
                viewModel.setDueDate(dateMillis)
                showDatePicker = false
                showTimePicker = true
            },
            initialDate = selectedDueDate
        )
    }

    // Time Picker Dialog
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
                                selectedDueDate?.let { dateMillis ->
                                    val cal = Calendar.getInstance().apply {
                                        timeInMillis = dateMillis
                                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        set(Calendar.MINUTE, timePickerState.minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    viewModel.setDueDate(cal.timeInMillis)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String?,  // Now accepts nullable
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory ?: "",  // Show empty if null
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    "Select a category",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Dropdown",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
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

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .exposedDropdownSize()
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedCategory == category)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                            )

                            if (selectedCategory == category) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedCategory == category)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

// Custom Date Picker Dialog
@Composable
fun CustomDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    initialDate: Long?
) {
    val calendar = remember {
        Calendar.getInstance().apply {
            initialDate?.let { timeInMillis = it }
        }
    }

    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedDay by remember {
        mutableStateOf(if (initialDate != null) calendar.get(Calendar.DAY_OF_MONTH) else -1)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Month/Year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${getMonthName(currentMonth)} $currentYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Days of week
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }

                // Calendar grid
                val daysInMonth = getDaysInMonth(currentMonth, currentYear)
                val firstDayOfWeek = getFirstDayOfWeek(currentMonth, currentYear)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Empty cells before first day
                    items(firstDayOfWeek) {
                        Box(modifier = Modifier.size(40.dp))
                    }

                    // Day cells
                    items(daysInMonth) { day ->
                        val dayNumber = day + 1
                        val isSelected = selectedDay == dayNumber
                        val isToday = isToday(dayNumber, currentMonth, currentYear)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    selectedDay = dayNumber
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNumber.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            if (selectedDay > 0) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, currentYear)
                                    set(Calendar.MONTH, currentMonth)
                                    set(Calendar.DAY_OF_MONTH, selectedDay)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onDateSelected(cal.timeInMillis)
                            }
                        },
                        enabled = selectedDay > 0,
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper functions for calendar
private fun getDaysInMonth(month: Int, year: Int): Int {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun getFirstDayOfWeek(month: Int, year: Int): Int {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.get(Calendar.DAY_OF_WEEK) - 1
}

private fun getMonthName(month: Int): String {
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(
        Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
        }.time
    )
}

private fun isToday(day: Int, month: Int, year: Int): Boolean {
    val today = Calendar.getInstance()
    return day == today.get(Calendar.DAY_OF_MONTH) &&
            month == today.get(Calendar.MONTH) &&
            year == today.get(Calendar.YEAR)
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

fun formatDueDate(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$month/$day/$year  $hour:$minute"
}