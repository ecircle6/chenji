package com.birthapp.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.R
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.ui.common.eventAccent
import com.birthapp.ui.theme.*
import com.birthapp.util.LocaleUtils
import java.time.YearMonth
import java.util.Calendar

/**
 * 添加/编辑页入口（薄壳）：收集 ViewModel 状态、处理「保存后返回」，
 * 渲染逻辑全部在无状态的 [AddEditContent] 里，便于 @Preview 与 UI 测试直接驱动。
 */
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

    AddEditContent(
        state = state,
        onBack = onBack,
        onUpdateEventType = { viewModel.updateEventType(it) },
        onUpdateName = { viewModel.updateName(it) },
        onUpdateEmoji = { viewModel.updateEmoji(it) },
        onUpdateCalendarType = { viewModel.updateCalendarType(it) },
        onUpdateBirthYear = { viewModel.updateBirthYear(it) },
        onUpdateBirthMonth = { viewModel.updateBirthMonth(it) },
        onUpdateBirthDay = { viewModel.updateBirthDay(it) },
        onUpdateLeapMonth = { viewModel.updateLeapMonth(it) },
        onToggleAdvanceDay = { viewModel.toggleAdvanceDay(it) },
        onAddCustomAdvanceDay = { viewModel.addCustomAdvanceDay(it) },
        onRemoveAdvanceDay = { viewModel.removeAdvanceDay(it) },
        onUpdateReminderTime = { h, m -> viewModel.updateReminderTime(h, m) },
        onUpdateRelation = { viewModel.updateRelation(it) },
        onUpdateNotes = { viewModel.updateNotes(it) },
        onSave = { viewModel.save() },
        onDelete = { viewModel.delete() }
    )
}

/** 添加/编辑页纯渲染：状态 + 回调，不感知 ViewModel，可 @Preview / UI 测试 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditContent(
    state: AddEditUiState,
    onBack: () -> Unit,
    onUpdateEventType: (String) -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateEmoji: (String) -> Unit,
    onUpdateCalendarType: (String) -> Unit,
    onUpdateBirthYear: (Int) -> Unit,
    onUpdateBirthMonth: (Int) -> Unit,
    onUpdateBirthDay: (Int) -> Unit,
    onUpdateLeapMonth: (Boolean) -> Unit,
    onToggleAdvanceDay: (Int) -> Unit,
    onAddCustomAdvanceDay: (Int) -> Unit,
    onRemoveAdvanceDay: (Int) -> Unit,
    onUpdateReminderTime: (hour: Int, minute: Int) -> Unit,
    onUpdateRelation: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) {
                            stringResource(R.string.addedit_title_edit)
                        } else {
                            stringResource(R.string.addedit_title_add)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = Coral500)
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
            SectionLabel(stringResource(R.string.addedit_section_type))
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
                                onClick = { onUpdateEventType(type) },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) accent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) accent
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "${EventType.emoji(type)} ${EventType.label(LocalContext.current.resources, type)}",
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
                onValueChange = onUpdateName,
                label = { Text(EventType.nameFieldLabel(LocalContext.current.resources, state.eventType)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral500,
                    focusedLabelColor = Coral500
                )
            )

            // Emoji 头像选择器：表单只留预览 + 两个入口，全量选项放进底部面板，
            // 避免 24 个方块平铺在表单里显得杂乱；空 = 自动（生日→姓名首字，其他→类型 emoji）
            SectionLabel(stringResource(R.string.addedit_section_emoji))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 当前头像预览：空时按自动规则实时预览（与首页卡片一致）
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(eventAccent(state.eventType).copy(alpha = 0.10f))
                        .border(1.dp, eventAccent(state.eventType).copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.emoji.ifBlank {
                            if (state.eventType == EventType.BIRTHDAY) {
                                state.name.firstOrNull()?.toString() ?: "?"
                            } else EventType.emoji(state.eventType)
                        },
                        fontSize = 22.sp
                    )
                }
                // 恢复自动
                TextButton(
                    onClick = { onUpdateEmoji("") },
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        if (state.emoji.isEmpty()) {
                            stringResource(R.string.addedit_emoji_auto)
                        } else {
                            stringResource(R.string.addedit_emoji_restore)
                        },
                        color = if (state.emoji.isEmpty()) Teal500 else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (state.emoji.isEmpty()) FontWeight.Bold else FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // 打开底部面板选 Emoji
                Button(
                    onClick = { showEmojiSheet = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal500)
                ) {
                    Text(stringResource(R.string.addedit_emoji_choose), fontWeight = FontWeight.Bold)
                }
            }

            // Calendar type toggle
            val isBirthdayLike = EventType.usesAge(state.eventType)
            SectionLabel(
                if (isBirthdayLike) stringResource(R.string.addedit_section_birthday_pref)
                else stringResource(R.string.addedit_section_date_pref)
            )
            Text(
                if (isBirthdayLike) stringResource(R.string.addedit_birthday_pref_hint)
                else stringResource(R.string.addedit_date_pref_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.calendarType == "solar",
                    onClick = { onUpdateCalendarType("solar") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Coral500,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.addedit_solar), fontWeight = FontWeight.Medium)
                }
                SegmentedButton(
                    selected = state.calendarType == "lunar",
                    onClick = { onUpdateCalendarType("lunar") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Teal500,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.addedit_lunar), fontWeight = FontWeight.Medium)
                }
            }

            // Date picker
            if (state.calendarType == "solar") {
                SolarDatePickerSection(
                    dateLabel = EventType.dateFieldLabel(LocalContext.current.resources, state.eventType),
                    year = state.birthYear,
                    month = state.birthMonth,
                    day = state.birthDay,
                    onYearChange = onUpdateBirthYear,
                    onMonthChange = onUpdateBirthMonth,
                    onDayChange = onUpdateBirthDay,
                    onOpenCalendar = { showDatePicker = true }
                )
            } else {
                LunarDatePickerSection(
                    dateLabel = EventType.dateFieldLabel(LocalContext.current.resources, state.eventType),
                    year = state.birthYear,
                    month = state.birthMonth,
                    day = state.birthDay,
                    isLeap = state.isLeapMonth,
                    onYearChange = onUpdateBirthYear,
                    onMonthChange = onUpdateBirthMonth,
                    onDayChange = onUpdateBirthDay,
                    onLeapChange = onUpdateLeapMonth
                )
            }

            // Reminder time
            SectionLabel(stringResource(R.string.addedit_section_reminder_time))
            Surface(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.small,
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
            SectionLabel(stringResource(R.string.addedit_section_advance))
            val resources = LocalContext.current.resources
            val advanceOptions = PRESET_ADVANCE_DAYS.map {
                it to if (it == 0) resources.getString(R.string.addedit_advance_today)
                else resources.getString(R.string.addedit_advance_day, it)
            }
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
                        onClick = { onToggleAdvanceDay(days) },
                        shape = MaterialTheme.shapes.medium,
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
                    shape = MaterialTheme.shapes.medium,
                    color = if (isCustomSelected) Teal500 else MaterialTheme.colorScheme.surface,
                    border = if (isCustomSelected) null
                    else androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isCustomSelected) {
                            resources.getString(R.string.addedit_advance_custom_selected, state.advanceDays.count { it !in presetDays })
                        } else {
                            resources.getString(R.string.addedit_advance_custom)
                        },
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
            SectionLabel(stringResource(R.string.addedit_section_relation))
            val relations = listOf(
                "family" to stringResource(R.string.relation_family) to Coral500,
                "friend" to stringResource(R.string.relation_friend) to Teal500,
                "colleague" to stringResource(R.string.relation_colleague) to SunnyYellow700,
                "other" to stringResource(R.string.relation_other) to Coral400
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relations.forEach { (keyLabel, color) ->
                    val (key, label) = keyLabel
                    val isSelected = state.relation == key
                    Surface(
                        onClick = { onUpdateRelation(key) },
                        shape = MaterialTheme.shapes.medium,
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
                onValueChange = onUpdateNotes,
                label = { Text(stringResource(R.string.addedit_notes_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal500,
                    focusedLabelColor = Teal500
                )
            )

            // Save button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = state.name.isNotBlank(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Coral500)
            ) {
                Text(
                    stringResource(R.string.addedit_save),
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
            title = { Text(stringResource(R.string.addedit_choose_time), fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_confirm), color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
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
                        onUpdateBirthYear(cal.get(Calendar.YEAR))
                        onUpdateBirthMonth(cal.get(Calendar.MONTH) + 1)
                        onUpdateBirthDay(cal.get(Calendar.DAY_OF_MONTH))
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_confirm), color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
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
            title = { Text(stringResource(R.string.addedit_custom_days_title), fontWeight = FontWeight.Bold) },
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
                            label = { Text(stringResource(R.string.addedit_custom_days_label)) },
                            suffix = { Text(stringResource(R.string.addedit_custom_days_unit)) },
                            singleLine = true,
                            isError = customDaysInput.isNotEmpty() && (!inputValid || alreadyAdded),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Teal500,
                                focusedLabelColor = Teal500
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            enabled = inputValid && !alreadyAdded && !atLimit,
                            onClick = {
                                onAddCustomAdvanceDay(inputDays!!)
                                customDaysInput = ""
                            }
                        ) { Text(stringResource(R.string.addedit_custom_days_add), color = Teal500, fontWeight = FontWeight.SemiBold) }
                    }
                    Text(
                        when {
                            customDaysInput.isNotEmpty() && !inputValid -> stringResource(R.string.addedit_custom_days_invalid)
                            alreadyAdded -> stringResource(R.string.addedit_custom_days_duplicate)
                            atLimit -> stringResource(R.string.addedit_custom_days_limit, AlarmScheduler.MAX_ADVANCE_LEVELS)
                            else -> stringResource(R.string.addedit_custom_days_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (customDaysInput.isNotEmpty() && !inputValid || alreadyAdded || atLimit)
                            Coral500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    if (customLevels.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            stringResource(R.string.addedit_custom_days_added),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customLevels.forEach { day ->
                                Surface(
                                    onClick = { onRemoveAdvanceDay(day) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = Teal500,
                                    border = null
                                ) {
                                    Text(
                                        text = stringResource(R.string.addedit_custom_days_chip, day),
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
                    Text(stringResource(R.string.addedit_custom_days_done), color = Coral500, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    // Emoji 头像底部面板：全量选项收纳于此，点选即生效并收起（与首页筛选面板同款交互）
    if (showEmojiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiSheet = false },
            shape = SheetShape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.addedit_emoji_sheet_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // 「自动」chip（清除手动选择，恢复自动头像）
                EmojiChip(
                    emoji = stringResource(R.string.addedit_emoji_auto_chip),
                    isSelected = state.emoji.isEmpty(),
                    onClick = {
                        onUpdateEmoji("")
                        showEmojiSheet = false
                    }
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EMOJI_OPTIONS.forEach { emoji ->
                        EmojiChip(
                            emoji = emoji,
                            isSelected = state.emoji == emoji,
                            onClick = {
                                onUpdateEmoji(emoji)
                                showEmojiSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.addedit_confirm_delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.addedit_confirm_delete_text, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text(stringResource(R.string.common_delete), color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
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

// ==================== Emoji Chip ====================

/** 预设 Emoji 头像选项：人/家人/朋友/宠物/物品/符号（每项唯一，供点选不重复） */
private val EMOJI_OPTIONS = listOf(
    "\uD83D\uDC67", "\uD83D\uDC68", "\uD83D\uDC69",     // 👧👨👩
    "\uD83D\uDC74", "\uD83D\uDC75", "\uD83D\uDC66",     // 👴👵👦
    "\uD83D\uDC76", "\uD83E\uDDD2", "\uD83D\uDC78",     // 👶🧒👸
    "\uD83E\uDDD1\u200D\uD83E\uDD1D\u200D\uD83E\uDDD1", "\uD83E\uDD1D", "\uD83D\uDC64", // 🧑🤝🧑🤝👤
    "\uD83D\uDC31", "\uD83D\uDC36", "\uD83D\uDC39",     // 🐱🐶🐹
    "\uD83D\uDC30", "\uD83E\uDD81", "\uD83D\uDC3C",     // 🐰🦁🐼
    "\uD83D\uDC8D", "\uD83C\uDF81", "\uD83C\uDF39",     // 💍🎁🌹
    "\u2764\uFE0F", "\uD83D\uDD6F\uFE0F", "\uD83D\uDCCC" // ❤️🕯️📌
)

@Composable
private fun EmojiChip(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) Teal500 else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null
        else androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 16.sp,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
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
        SectionLabel(stringResource(R.string.addedit_solar_section, dateLabel))
        Surface(
            onClick = onOpenCalendar,
            shape = MaterialTheme.shapes.small,
            color = Coral500.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = stringResource(R.string.addedit_calendar_choose),
                    tint = Coral500,
                    modifier = Modifier.height(16.dp)
                )
                Text(
                    stringResource(R.string.addedit_calendar_choose),
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
            label = stringResource(R.string.addedit_year),
            options = (1920..2100).map { stringResource(R.string.addedit_year_option, it) },
            selectedIndex = (year - 1920).coerceIn(0, 180),
            onSelected = { onYearChange(it + 1920) },
            accentColor = Coral500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = stringResource(R.string.addedit_month),
            options = (1..12).map { stringResource(R.string.addedit_month_option, it) },
            selectedIndex = (month - 1).coerceIn(0, 11),
            onSelected = { onMonthChange(it + 1) },
            accentColor = Coral500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = stringResource(R.string.addedit_day),
            options = (1..maxDays).map { stringResource(R.string.addedit_day_option, it) },
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

    SectionLabel(stringResource(R.string.addedit_lunar_section, dateLabel))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateDropdown(
            label = stringResource(R.string.addedit_year),
            options = (1920..2100).map { stringResource(R.string.addedit_year_option, it) },
            selectedIndex = (year - 1920).coerceIn(0, 180),
            onSelected = { onYearChange(it + 1920) },
            accentColor = Teal500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = stringResource(R.string.addedit_month),
            options = lunarMonthNames,
            selectedIndex = (month - 1).coerceIn(0, 11),
            onSelected = { onMonthChange(it + 1) },
            accentColor = Teal500,
            modifier = Modifier.weight(1f)
        )
        DateDropdown(
            label = stringResource(R.string.addedit_day),
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
            stringResource(R.string.addedit_lunar_leap),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (hasLeapMonth) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(
            checked = isLeap,
            onCheckedChange = onLeapChange,
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
            shape = MaterialTheme.shapes.medium,
            color = CardLavender
        ) {
            val resources = LocalContext.current.resources
            Text(
                if (LocaleUtils.isEnglish(resources)) {
                    stringResource(
                        R.string.addedit_lunar_preview_en,
                        solarPreview.year,
                        resources.getStringArray(R.array.months_short)[(solarPreview.month - 1).coerceIn(0, 11)],
                        solarPreview.day
                    )
                } else {
                    stringResource(
                        R.string.addedit_lunar_preview_zh,
                        solarPreview.year, solarPreview.month, solarPreview.day
                    )
                },
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = Coral500.copy(alpha = 0.1f)
        ) {
            Text(
                stringResource(R.string.addedit_lunar_missing),
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
            shape = MaterialTheme.shapes.medium,
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
            shape = MaterialTheme.shapes.large
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

// ==================== Previews ====================

@Preview(showBackground = true, locale = "zh-rCN", name = "添加记录 · 浅色")
@Composable
private fun AddEditContentPreview() {
    BirthAppTheme {
        AddEditContent(
            state = AddEditUiState(name = "小明", reminderHour = 8, reminderMinute = 30),
            onBack = {}, onUpdateEventType = {}, onUpdateName = {}, onUpdateEmoji = {}, onUpdateCalendarType = {},
            onUpdateBirthYear = {}, onUpdateBirthMonth = {}, onUpdateBirthDay = {},
            onUpdateLeapMonth = {}, onToggleAdvanceDay = {}, onAddCustomAdvanceDay = {},
            onRemoveAdvanceDay = {}, onUpdateReminderTime = { _, _ -> },
            onUpdateRelation = {}, onUpdateNotes = {}, onSave = {}, onDelete = {}
        )
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "编辑记录 · 深色")
@Composable
private fun AddEditContentPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        AddEditContent(
            state = AddEditUiState(
                id = 1, name = "爷爷", eventType = EventType.MEMORIAL,
                calendarType = "lunar", birthYear = 1945, birthMonth = 7, birthDay = 15,
                advanceDays = listOf(0, 3), isEditMode = true
            ),
            onBack = {}, onUpdateEventType = {}, onUpdateName = {}, onUpdateEmoji = {}, onUpdateCalendarType = {},
            onUpdateBirthYear = {}, onUpdateBirthMonth = {}, onUpdateBirthDay = {},
            onUpdateLeapMonth = {}, onToggleAdvanceDay = {}, onAddCustomAdvanceDay = {},
            onRemoveAdvanceDay = {}, onUpdateReminderTime = { _, _ -> },
            onUpdateRelation = {}, onUpdateNotes = {}, onSave = {}, onDelete = {}
        )
    }
}
