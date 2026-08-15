package com.birthapp.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.BirthApp
import com.birthapp.settings.Changelog
import com.birthapp.settings.ReminderSettings
import com.birthapp.settings.ThemeMode
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.Teal500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    // 主题偏好直接读 Application 上的全局单例，改动会即时反映到全 App
    val themeStore = (LocalContext.current.applicationContext as BirthApp).themeStore
    val currentMode by themeStore.mode.collectAsStateWithLifecycle()
    val dynamicColor by themeStore.dynamicColor.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 通知偏好：默认提醒时间 + 提醒总开关
    val reminderSettings = ReminderSettings(context)
    val remindersEnabled by reminderSettings.remindersEnabled.collectAsStateWithLifecycle()
    var defaultHour by remember { mutableIntStateOf(reminderSettings.defaultHour) }
    var defaultMinute by remember { mutableIntStateOf(reminderSettings.defaultMinute) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 导入预览：解析完备份文件后弹逐条三选对话框
    var importPreview by remember { mutableStateOf<SettingsEvent.ImportPreview?>(null) }

    // 版本更新说明：升级弹窗外，设置页也留一个入口看全部历史
    var showChangelog by remember { mutableStateOf(false) }
    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: ""

    // 默认提醒时间的 TimePicker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderSettings.defaultHour,
            initialMinute = reminderSettings.defaultMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("默认提醒时间", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    reminderSettings.setDefaultTime(timePickerState.hour, timePickerState.minute)
                    defaultHour = timePickerState.hour
                    defaultMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("确定", color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 导出：系统文件选择器让用户自己挑保存位置，不需要存储权限
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportTo(it) } }

    // 导入：类型放宽到 */*，微信/QQ 传过来的文件经常丢掉标准后缀，
    // 按 json 类型过滤会把它们灰掉选不中；真选错文件解析时会拦住
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFrom(it) } }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Toast ->
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                is SettingsEvent.ShareFile -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "把备份发给另一台手机"))
                }
                is SettingsEvent.ImportPreview -> importPreview = event
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSectionLabel("深夜模式")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        ThemeModeRow(
                            mode = mode,
                            selected = currentMode == mode,
                            onSelect = { themeStore.setMode(mode) }
                        )
                        // 选项之间加细分割线，最后一项不加
                        if (index < ThemeMode.entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 20.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        }
                    }
                    // 动态取色只有 Android 12+ 有壁纸取色能力，低版本直接不显示
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { themeStore.setDynamicColor(!dynamicColor) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("动态取色", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    "跟随系统壁纸配色（Material You）",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = { themeStore.setDynamicColor(it) }
                            )
                        }
                    }
                }
            }

            SettingsSectionLabel("通知")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    // 默认提醒时间：新建记录默认在几点提醒
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("默认提醒时间", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "新建记录默认在几点提醒",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Text(
                            String.format("%02d:%02d", defaultHour, defaultMinute),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                    // 提醒总开关：关闭后不再调度任何闹钟
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("提醒", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "关闭后不再提醒任何记录",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { enabled ->
                                reminderSettings.setRemindersEnabled(enabled)
                                // 重排一次：关闭时清掉已排闹钟，重开时恢复
                                (context.applicationContext as BirthApp).rescheduleAllAlarms()
                            }
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                    // 系统通知设置：声音/震动/锁屏显示只能由用户在系统里改
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("系统通知设置", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                "声音、震动、锁屏显示等",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Text(
                            "›",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            SettingsSectionLabel("数据备份")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    BackupActionRow(
                        icon = Icons.Outlined.Download,
                        title = "导出备份到手机",
                        desc = "存成一个文件，换手机、误删时可恢复",
                        onClick = { exportLauncher.launch(SettingsViewModel.backupFileName()) }
                    )
                    BackupDivider()
                    BackupActionRow(
                        icon = Icons.Outlined.Share,
                        title = "把备份发到其他设备",
                        desc = "通过微信 / QQ 等发给另一台手机",
                        onClick = { viewModel.shareBackup() }
                    )
                    BackupDivider()
                    BackupActionRow(
                        icon = Icons.Outlined.Upload,
                        title = "导入备份",
                        desc = "已有记录会保留，重复的自动跳过",
                        onClick = { importLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }

            SettingsSectionLabel("关于")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                BackupActionRow(
                    icon = Icons.Outlined.Info,
                    title = "版本更新说明",
                    desc = "当前版本 v$versionName · 查看每次更新了什么",
                    onClick = { showChangelog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 导入预览对话框：逐条三选（跳过/覆盖/导入），重复的默认跳过
    importPreview?.let { preview ->
        var choices by remember(preview.items) {
            mutableStateOf(
                preview.items.map { if (it.isDuplicate) ImportAction.SKIP else ImportAction.INSERT }
            )
        }
        var restoreSettings by remember { mutableStateOf(false) }
        val duplicateCount = preview.items.count { it.isDuplicate }

        AlertDialog(
            onDismissRequest = { importPreview = null },
            title = { Text("导入预览（${preview.items.size} 条）", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (duplicateCount > 0) {
                        Text(
                            "发现 $duplicateCount 条与现有记录重复，默认跳过",
                            fontSize = 13.sp,
                            color = Coral500
                        )
                    }
                    preview.items.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.record.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    importItemSubtitle(item.record),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            ImportChoicePill("跳过", choices[index] == ImportAction.SKIP) {
                                choices = choices.toMutableList().also { it[index] = ImportAction.SKIP }
                            }
                            ImportChoicePill("覆盖", choices[index] == ImportAction.OVERWRITE) {
                                choices = choices.toMutableList().also { it[index] = ImportAction.OVERWRITE }
                            }
                            ImportChoicePill("导入", choices[index] == ImportAction.INSERT) {
                                choices = choices.toMutableList().also { it[index] = ImportAction.INSERT }
                            }
                        }
                    }
                    if (preview.settings != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { restoreSettings = !restoreSettings },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = restoreSettings,
                                onCheckedChange = { restoreSettings = it }
                            )
                            Text("同时恢复备份中的主题设置", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyImport(choices, restoreSettings)
                    importPreview = null
                }) { Text("导入", color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { importPreview = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 版本更新说明对话框：全部历史，最新在前，当前版本加标记
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text("版本更新说明", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Changelog.all.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "v${entry.version} · ${entry.title}${if (entry.version == versionName) "（当前版本）" else ""}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            entry.items.forEach { item ->
                                Text(
                                    "· $item",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelog = false }) { Text("知道了") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/** 导入条目副标题：类型 + 日期（与首页信息行同一口径） */
private fun importItemSubtitle(b: com.birthapp.data.Birthday): String {
    val dateLabel = if (b.calendarType == "lunar") {
        "农历${com.birthapp.lunar.LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)}"
    } else {
        "${b.birthMonth}月${b.birthDay}日"
    }
    return "${com.birthapp.data.EventType.label(b.eventType)} · $dateLabel"
}

@Composable
private fun ImportChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Teal500 else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Teal500 else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BackupActionRow(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BackupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    )
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = androidx.compose.ui.semantics.Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // 点击由整行的 selectable 统一处理，避免出现两个可点区域
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = mode.desc,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp)
    )
}
