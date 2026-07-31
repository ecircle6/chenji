package com.birthapp.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.lunar.LunarCalendar
import com.birthapp.ui.theme.*
import java.time.YearMonth
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    birthdayId: Long = 0,
    onBack: () -> Unit,
    viewModel: AddEditViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(birthdayId) {
        if (birthdayId > 0) viewModel.loadBirthday(birthdayId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) "编辑生日" else "添加生日",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = Coral500)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("姓名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral500,
                    focusedLabelColor = Coral500
                )
            )

            // Calendar type toggle
            SectionLabel("生日偏好")
            Text(
                "选择按哪种日历过生日，切换时日期自动换算",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.calendarType == "solar",
                    onClick = { viewModel.updateCalendarType("solar") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Coral500,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("☀ 阳历", fontWeight = FontWeight.Medium)
                }
                SegmentedButton(
                    selected = state.calendarType == "lunar",
                    onClick = { viewModel.updateCalendarType("lunar") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Teal500,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("\uD83C\uDF19 农历", fontWeight = FontWeight.Medium)
                }
            }

            // Date picker
            if (state.calendarType == "solar") {
                SolarDatePickerSection(
                    year = state.birthYear,
                    month = state.birthMonth,
                    day = state.birthDay,
                    onYearChange = { viewModel.updateBirthYear(it) },
                    onMonthChange = { viewModel.updateBirthMonth(it) },
                    onDayChange = { viewModel.updateBirthDay(it) },
                    onOpenCalendar = { showDatePicker = true }
                )
            } else {
                LunarDatePickerSection(
                    year = state.birthYear,
                    month = state.birthMonth,
                    day = state.birthDay,
                    isLeap = state.isLeapMonth,
                    onYearChange = { viewModel.updateBirthYear(it) },
                    onMonthChange = { viewModel.updateBirthMonth(it) },
                    onDayChange = { viewModel.updateBirthDay(it) },
                    onLeapChange = { viewModel.updateLeapMonth(it) }
                )
            }

            // Reminder time
            SectionLabel("提醒时间")
            Surface(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardPeach
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format("%02d : %02d", state.reminderHour, state.reminderMinute),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Coral500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "  \uD83D\uDD50  ",
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Advance days
            SectionLabel("提前提醒")
            val advanceOptions = listOf(0 to "当天", 1 to "1天", 3 to "3天", 5 to "5天", 7 to "7天")
            val presetDays = advanceOptions.map { it.first }
            val isCustomSelected = state.advanceDays !in presetDays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                advanceOptions.forEach { (days, label) ->
                    val isSelected = state.advanceDays == days
                    Surface(
                        onClick = { viewModel.updateAdvanceDays(days) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Teal500 else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null
                        else androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
                // 自定义天数
                Surface(
                    onClick = { showCustomDaysDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCustomSelected) Teal500 else MaterialTheme.colorScheme.surface,
                    border = if (isCustomSelected) null
                    else androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isCustomSelected) "${state.advanceDays}天 ✏" else "自定义",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = if (isCustomSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }

            // Relation
            SectionLabel("关系分类")
            val relations = listOf(
                "family" to "家人" to Coral500,
                "friend" to "朋友" to Teal500,
                "colleague" to "同事" to SunnyYellow700,
                "other" to "其他" to Coral400
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relations.forEach { (keyLabel, color) ->
                    val (key, label) = keyLabel
                    val isSelected = state.relation == key
                    Surface(
                        onClick = { viewModel.updateRelation(key) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) color.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(
                            1.5.dp, color
                        ) else androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal500,
                    focusedLabelColor = Teal500
                )
            )

            // Save button
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral500)
            ) {
                Text(
                    "保存生日",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择提醒时间", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("确定", color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Solar calendar picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance().apply {
                set(state.birthYear, state.birthMonth - 1, state.birthDay, 12, 0, 0)
            }.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = millis
                        viewModel.updateBirthYear(cal.get(Calendar.YEAR))
                        viewModel.updateBirthMonth(cal.get(Calendar.MONTH) + 1)
                        viewModel.updateBirthDay(cal.get(Calendar.DAY_OF_MONTH))
                    }
                    showDatePicker = false
                }) { Text("确定", color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Custom advance days dialog
    if (showCustomDaysDialog) {
        var customDaysInput by remember {
            mutableStateOf(if (state.advanceDays > 0) state.advanceDays.toString() else "")
        }
        val inputDays = customDaysInput.toIntOrNull()
        val isValid = inputDays != null && inputDays in 0..365
        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = { Text("自定义提前天数", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customDaysInput,
                        onValueChange = { input ->
                            if (input.length <= 3 && input.all { it.isDigit() }) {
                                customDaysInput = input
                            }
                        },
                        label = { Text("提前天数") },
                        suffix = { Text("天") },
                        singleLine = true,
                        isError = customDaysInput.isNotEmpty() && !isValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Teal500,
                            focusedLabelColor = Teal500
                        )
                    )
                    Text(
                        if (customDaysInput.isNotEmpty() && !isValid) "请输入 0~365 之间的天数"
                        else "可输入 0~365，例如 15 表示提前 15 天提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (customDaysInput.isNotEmpty() && !isValid) Coral500
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = isValid,
                    onClick = {
                        viewModel.updateAdvanceDays(inputDays!!)
                        showCustomDaysDialog = false
                    }
                ) { Text("确定", color = if (isValid) Coral500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除", fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除「${state.name}」的生日记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text("删除", color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ==================== Section Label ====================

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ==================== Solar Date Picker Section ====================

@Composable
private fun SolarDatePickerSection(
    year: Int, month: Int, day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onOpenCalendar: () -> Unit
) {
    val maxDays = runCatching { YearMonth.of(year, month).lengthOfMonth() }.getOrDefault(31)

    // 月份/年份变化后，日期超出当月天数时自动回退
    LaunchedEffect(year, month) {
        if (day > maxDays) onDayChange(maxDays)
    }

    // 标题 + 日历选择入口
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel("出生日期（阳历）")
        Surface(
            onClick = onOpenCalendar,
            shape = RoundedCornerShape(10.dp),
            color = Coral500.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = "日历选择",
                    tint = Coral500,
                    modifier = Modifier.height(16.dp)
                )
                Text(
                    "日历选择",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Coral500
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateDropdown(
            label = "年份",
            options = (1920..2100).map { "${it}年" },
            selectedIndex = (year - 1920).coerceIn(0, 180),
            onSelected = { onYearChange(it + 1920) },
            accentColor = Coral500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = "月份",
            options = (1..12).map { "${it}月" },
            selectedIndex = (month - 1).coerceIn(0, 11),
            onSelected = { onMonthChange(it + 1) },
            accentColor = Coral500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = "日期",
            options = (1..maxDays).map { "${it}日" },
            selectedIndex = (day - 1).coerceIn(0, maxDays - 1),
            onSelected = { onDayChange(it + 1) },
            accentColor = Coral500,
            modifier = Modifier.weight(1f)
        )
    }
}

// ==================== Lunar Date Picker Section ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LunarDatePickerSection(
    year: Int, month: Int, day: Int, isLeap: Boolean,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onLeapChange: (Boolean) -> Unit
) {
    val lunarMonthNames = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )
    val lunarDayNames = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    val maxDays = try {
        if (isLeap) LunarCalendar.getLunarMonthDays(year, month, true)
        else LunarCalendar.getLunarMonthDays(year, month, false)
    } catch (_: Exception) { 30 }

    val hasLeapMonth = try { LunarCalendar.leapMonth(year) != 0 } catch (_: Exception) { false }

    SectionLabel("出生日期（农历）")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateDropdown(
            label = "年份",
            options = (1920..2100).map { "${it}年" },
            selectedIndex = (year - 1920).coerceIn(0, 180),
            onSelected = { onYearChange(it + 1920) },
            accentColor = Teal500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = "月份",
            options = lunarMonthNames,
            selectedIndex = (month - 1).coerceIn(0, 11),
            onSelected = { onMonthChange(it + 1) },
            accentColor = Teal500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = "日期",
            options = lunarDayNames.subList(0, maxDays.coerceIn(1, 30)),
            selectedIndex = (day - 1).coerceIn(0, maxDays - 1),
            onSelected = { onDayChange(it + 1) },
            accentColor = Teal500,
            modifier = Modifier.weight(1f)
        )
    }

    // Leap month toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "闰月",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (hasLeapMonth) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(
            checked = isLeap,
            onCheckedChange = { onLeapChange(it) },
            enabled = hasLeapMonth,
            colors = SwitchDefaults.colors(checkedTrackColor = Teal500)
        )
    }

    // Solar date preview
    val solarPreview = remember(year, month, day, isLeap) {
        runCatching { LunarCalendar.lunarToSolar(year, month, day, isLeap) }.getOrNull()
    }
    if (solarPreview != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = CardLavender
        ) {
            Text(
                "\uD83D\uDCC5 对应阳历：${solarPreview.year}年${solarPreview.month}月${solarPreview.day}日",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Coral500.copy(alpha = 0.1f)
        ) {
            Text(
                "⚠️ 该农历日期在当前年份可能不存在",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Coral500
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { options.first() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                unfocusedBorderColor = accentColor.copy(alpha = 0.5f),
                unfocusedLabelColor = accentColor,
                unfocusedTrailingIconColor = accentColor,
                focusedTrailingIconColor = accentColor
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (index == selectedIndex) accentColor
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold
                            else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
