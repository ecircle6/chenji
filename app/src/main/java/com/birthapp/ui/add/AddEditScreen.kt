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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.ui.common.eventAccent
import com.birthapp.ui.theme.*
import java.time.YearMonth
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        if (state.isEditMode) "编辑记录" else "添加记录",
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
            // Event type selector
            SectionLabel("类型")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventType.ALL.chunked(3).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { type ->
                            val isSelected = state.eventType == type
                            val accent = eventAccent(type)
                            Surface(
                                onClick = { viewModel.updateEventType(type) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) accent
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "${EventType.emoji(type)} ${EventType.label(type)}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text(EventType.nameFieldLabel(state.eventType)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral500,
                    focusedLabelColor = Coral500
                )
            )

            // Calendar type toggle
            val isBirthdayLike = EventType.usesAge(state.eventType)
            SectionLabel(if (isBirthdayLike) "生日偏好" else "日期偏好")
            Text(
                if (isBirthdayLike) "选择按哪种日历过生日，切换时日期自动换算"
                else "选择按哪种日历纪念，切换时日期自动换算",
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
                    dateLabel = EventType.dateFieldLabel(state.eventType),
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
                    dateLabel = EventType.dateFieldLabel(state.eventType),
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
            val advanceOptions = PRESET_ADVANCE_DAYS.map { it to if (it == 0) "当天" else "${it}天" }
            val presetDays = PRESET_ADVANCE_DAYS
            val isCustomSelected = state.advanceDays.any { it !in presetDays }
            // 用 FlowRow 而不是 Row：6 个选项在窄屏一行放不下时会整体折到第二行，
            // 每个 chip 按自身文字宽度排布，不会再把「自定义」挤成两行；
            // 「自定义」选中后文案会变长（如「30天 ✏」），FlowRow 同样能容纳
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 预设项可多选（多级提前提醒）：再点一次取消，0（当天）可与其它级别共存
                advanceOptions.forEach { (days, label) ->
                    val isSelected = days in state.advanceDays
                    Surface(
                        onClick = { viewModel.toggleAdvanceDay(days) },
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
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
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
                        text = if (isCustomSelected) "自定义 ×${state.advanceDays.count { it !in presetDays }}" else "自定义",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = if (isCustomSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCustomSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false
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
                    "保存",
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

    // Custom advance days dialog：可连续添加多个级别，已添加的能逐个移除
    if (showCustomDaysDialog) {
        var customDaysInput by remember { mutableStateOf("") }
        val inputDays = customDaysInput.toIntOrNull()
        val inputValid = inputDays != null && inputDays in 0..365
        val alreadyAdded = inputDays != null && inputDays in state.advanceDays
        val atLimit = state.advanceDays.size >= AlarmScheduler.MAX_ADVANCE_LEVELS
        val customLevels = state.advanceDays.filter { it !in PRESET_ADVANCE_DAYS }
        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = { Text("自定义提前天数", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            isError = customDaysInput.isNotEmpty() && (!inputValid || alreadyAdded),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal500,
                                focusedLabelColor = Teal500
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = inputValid && !alreadyAdded && !atLimit,
                            onClick = {
                                viewModel.addCustomAdvanceDay(inputDays!!)
                                customDaysInput = ""
                            }
                        ) { Text("添加", color = Teal500, fontWeight = FontWeight.SemiBold) }
                    }
                    Text(
                        when {
                            customDaysInput.isNotEmpty() && !inputValid -> "请输入 0~365 之间的天数"
                            alreadyAdded -> "该天数已在列表中"
                            atLimit -> "最多 ${AlarmScheduler.MAX_ADVANCE_LEVELS} 个提前级别"
                            else -> "可输入 0~365，例如 15 表示提前 15 天提醒"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (customDaysInput.isNotEmpty() && !inputValid || alreadyAdded || atLimit)
                            Coral500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    if (customLevels.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            "已添加（点击移除）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customLevels.forEach { day ->
                                Surface(
                                    onClick = { viewModel.removeAdvanceDay(day) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Teal500,
                                    border = null
                                ) {
                                    Text(
                                        text = "${day}天 ✕",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) {
                    Text("完成", color = Coral500, fontWeight = FontWeight.SemiBold)
                }
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
            text = { Text("确定要删除「${state.name}」的记录吗？") },
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

/** 预设提前提醒级别（chips 与自定义 dialog 共用，0 = 当天） */
private val PRESET_ADVANCE_DAYS = listOf(0, 1, 3, 5, 7)

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
    dateLabel: String,
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
        SectionLabel("$dateLabel（阳历）")
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
    dateLabel: String,
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

    SectionLabel("$dateLabel（农历）")

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
