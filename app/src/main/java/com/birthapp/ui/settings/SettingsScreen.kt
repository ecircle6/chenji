package com.birthapp.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.BirthApp
import com.birthapp.R
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.settings.Changelog
import com.birthapp.settings.ReminderSettings
import com.birthapp.settings.ThemeMode
import com.birthapp.ui.theme.BirthAppTheme
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.Teal500
import com.birthapp.util.DateUtils
import com.birthapp.widget.WidgetPinner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** 设置页的纯渲染状态：主题/通知偏好 + 版本号，全部由外部传入 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val remindersEnabled: Boolean = true,
    val defaultHour: Int = 8,
    val defaultMinute: Int = 0,
    val versionName: String = ""
)

/** 设置页的全部用户动作回调（壳层接到真实实现：写偏好、起 Intent、调 ViewModel） */
class SettingsCallbacks(
    val onBack: () -> Unit,
    val onThemeModeSelect: (ThemeMode) -> Unit,
    val onToggleDynamicColor: (Boolean) -> Unit,
    val onSetDefaultTime: (hour: Int, minute: Int) -> Unit,
    val onToggleReminders: (Boolean) -> Unit,
    val onExportClick: () -> Unit,
    val onShareBackup: () -> Unit,
    val onImportClick: () -> Unit,
    val onApplyImport: (List<ImportAction>, Boolean) -> Unit,
    val onToast: (String) -> Unit,
    val onShareFile: (Uri) -> Unit,
    val onOpenSystemNotificationSettings: () -> Unit,
    val onAddWidget: () -> Unit
)

/**
 * 设置页入口（薄壳）：读主题/通知偏好、组装 [SettingsCallbacks]（含系统文件选择器、
 * Intent、Toast 等真实环境行为），渲染逻辑全部在无状态的 [SettingsContent] 里。
 */
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

    val versionName = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: ""

    // 导出：系统文件选择器让用户自己挑保存位置，不需要存储权限
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportTo(it) } }

    // 导入：类型放宽到 */*，微信/QQ 传过来的文件经常丢掉标准后缀，
    // 按 json 类型过滤会把它们灰掉选不中；真选错文件解析时会拦住
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFrom(it) } }

    SettingsContent(
        uiState = SettingsUiState(
            themeMode = currentMode,
            dynamicColor = dynamicColor,
            remindersEnabled = remindersEnabled,
            defaultHour = reminderSettings.defaultHour,
            defaultMinute = reminderSettings.defaultMinute,
            versionName = versionName
        ),
        events = viewModel.events,
        callbacks = SettingsCallbacks(
            onBack = onBack,
            onThemeModeSelect = { themeStore.setMode(it) },
            onToggleDynamicColor = { themeStore.setDynamicColor(it) },
            onSetDefaultTime = { h, m -> reminderSettings.setDefaultTime(h, m) },
            onToggleReminders = { enabled ->
                reminderSettings.setRemindersEnabled(enabled)
                // 重排一次：关闭时清掉已排闹钟，重开时恢复
                (context.applicationContext as BirthApp).rescheduleAllAlarms()
            },
            onExportClick = { exportLauncher.launch(SettingsViewModel.backupFileName()) },
            onShareBackup = { viewModel.shareBackup() },
            onImportClick = { importLauncher.launch(arrayOf("*/*")) },
            onApplyImport = { choices, restore -> viewModel.applyImport(choices, restore) },
            onToast = { text -> Toast.makeText(context, text, Toast.LENGTH_LONG).show() },
            onShareFile = { uri ->
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(send, context.getString(R.string.settings_share_chooser))
                )
            },
            onOpenSystemNotificationSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            // 添加桌面小组件：与长按应用图标共用同一个放置请求。
            // 带上成功回调：真放置成功时系统发广播 → 弹「已添加」Toast，
            // 避免用户以为没反应重复点击加了两个
            onAddWidget = {
                if (!WidgetPinner.request(context, WidgetPinner.successPendingIntent(context))) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_widget_not_supported),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    )
}

/**
 * 设置页纯渲染：状态 + 一次性事件流 + 回调，不感知 ViewModel / Context，
 * 可 @Preview / UI 测试。Toast、分享、系统跳转等副作用全部走 [SettingsCallbacks]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    events: Flow<SettingsEvent>,
    callbacks: SettingsCallbacks
) {
    var defaultHour by remember { mutableIntStateOf(uiState.defaultHour) }
    var defaultMinute by remember { mutableIntStateOf(uiState.defaultMinute) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 导入预览：解析完备份文件后弹逐条三选对话框
    var importPreview by remember { mutableStateOf<SettingsEvent.ImportPreview?>(null) }

    // 版本更新说明：升级弹窗外，设置页也留一个入口看全部历史
    var showChangelog by remember { mutableStateOf(false) }

    // 一次性事件：Toast / 分享备份 / 导入预览
    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is SettingsEvent.Toast -> callbacks.onToast(event.text)
                is SettingsEvent.ShareFile -> callbacks.onShareFile(event.uri)
                is SettingsEvent.ImportPreview -> importPreview = event
            }
        }
    }

    // 默认提醒时间的 TimePicker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.defaultHour,
            initialMinute = uiState.defaultMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_default_time), fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    callbacks.onSetDefaultTime(timePickerState.hour, timePickerState.minute)
                    defaultHour = timePickerState.hour
                    defaultMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.common_confirm), color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = callbacks.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            SettingsSectionLabel(stringResource(R.string.settings_section_theme))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        ThemeModeRow(
                            mode = mode,
                            selected = uiState.themeMode == mode,
                            onSelect = { callbacks.onThemeModeSelect(mode) }
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
                                .clickable { callbacks.onToggleDynamicColor(!uiState.dynamicColor) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_dynamic_color), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(
                                    stringResource(R.string.settings_dynamic_color_desc),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Switch(
                                checked = uiState.dynamicColor,
                                onCheckedChange = callbacks.onToggleDynamicColor
                            )
                        }
                    }
                }
            }

            SettingsSectionLabel(stringResource(R.string.settings_section_notifications))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
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
                            Text(stringResource(R.string.settings_default_time), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                stringResource(R.string.settings_default_time_desc),
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
                            Text(stringResource(R.string.settings_reminders), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                stringResource(R.string.settings_reminders_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        Switch(
                            checked = uiState.remindersEnabled,
                            onCheckedChange = callbacks.onToggleReminders,
                            modifier = Modifier.testTag("switch_reminders")
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
                            .clickable(onClick = callbacks.onOpenSystemNotificationSettings)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_system_notif), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            Text(
                                stringResource(R.string.settings_system_notif_desc),
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

            SettingsSectionLabel(stringResource(R.string.settings_section_backup))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    BackupActionRow(
                        icon = Icons.Outlined.Download,
                        title = stringResource(R.string.settings_export),
                        desc = stringResource(R.string.settings_export_desc),
                        onClick = callbacks.onExportClick
                    )
                    BackupDivider()
                    BackupActionRow(
                        icon = Icons.Outlined.Share,
                        title = stringResource(R.string.settings_share_backup),
                        desc = stringResource(R.string.settings_share_backup_desc),
                        onClick = callbacks.onShareBackup
                    )
                    BackupDivider()
                    BackupActionRow(
                        icon = Icons.Outlined.Upload,
                        title = stringResource(R.string.settings_import),
                        desc = stringResource(R.string.settings_import_desc),
                        onClick = callbacks.onImportClick
                    )
                }
            }

            SettingsSectionLabel(stringResource(R.string.settings_section_about))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column {
                    BackupActionRow(
                        icon = Icons.Outlined.Widgets,
                        title = stringResource(R.string.settings_add_widget),
                        desc = stringResource(R.string.settings_add_widget_desc),
                        onClick = callbacks.onAddWidget
                    )
                    BackupDivider()
                    BackupActionRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.settings_changelog),
                        desc = stringResource(R.string.settings_changelog_desc, uiState.versionName),
                        onClick = { showChangelog = true }
                    )
                }
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
            title = { Text(stringResource(R.string.settings_import_preview_title, preview.items.size), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    // 条目多时会撑出屏幕：M3 AlertDialog 内容默认不可滚动，
                    // 限制高度 + verticalScroll 才能在有限空间内滑到每一条（与版本更新说明对话框同一写法）
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (duplicateCount > 0) {
                        Text(
                            stringResource(R.string.settings_import_duplicate, duplicateCount),
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
                            ImportChoicePill(stringResource(R.string.settings_import_skip), choices[index] == ImportAction.SKIP) {
                                choices = choices.toMutableList().also { it[index] = ImportAction.SKIP }
                            }
                            ImportChoicePill(stringResource(R.string.settings_import_overwrite), choices[index] == ImportAction.OVERWRITE) {
                                choices = choices.toMutableList().also { it[index] = ImportAction.OVERWRITE }
                            }
                            ImportChoicePill(stringResource(R.string.settings_import_insert), choices[index] == ImportAction.INSERT) {
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
                            Text(stringResource(R.string.settings_import_restore_settings), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    callbacks.onApplyImport(choices, restoreSettings)
                    importPreview = null
                }) { Text(stringResource(R.string.settings_import_insert), color = Coral500, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { importPreview = null }) { Text(stringResource(R.string.common_cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    // 版本更新说明对话框：全部历史，最新在前，当前版本加标记
    if (showChangelog) {
        AlertDialog(
            onDismissRequest = { showChangelog = false },
            title = { Text(stringResource(R.string.settings_changelog), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Changelog.all.forEach { entry ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "v${entry.version} · ${entry.title}" +
                                        if (entry.version == uiState.versionName) {
                                            stringResource(R.string.settings_changelog_current)
                                        } else "",
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
                TextButton(onClick = { showChangelog = false }) { Text(stringResource(R.string.common_got_it)) }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

/** 导入条目副标题：类型 + 日期（与首页信息行同一口径） */
@Composable
private fun importItemSubtitle(b: Birthday): String {
    val resources = LocalContext.current.resources
    val dateLabel = if (b.calendarType == "lunar") {
        resources.getString(R.string.date_lunar_prefix, LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay))
    } else {
        DateUtils.formatSolarMonthDay(resources, b.birthMonth, b.birthDay)
    }
    return "${EventType.label(resources, b.eventType)} · $dateLabel"
}

@Composable
private fun ImportChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
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
                text = stringResource(mode.labelRes),
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(mode.descRes),
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

private val previewSettingsCallbacks = SettingsCallbacks(
    onBack = {},
    onThemeModeSelect = {},
    onToggleDynamicColor = {},
    onSetDefaultTime = { _, _ -> },
    onToggleReminders = {},
    onExportClick = {},
    onShareBackup = {},
    onImportClick = {},
    onApplyImport = { _, _ -> },
    onToast = {},
    onShareFile = {},
    onOpenSystemNotificationSettings = {},
    onAddWidget = {}
)

@Preview(showBackground = true, locale = "zh-rCN", name = "设置页 · 浅色")
@Composable
private fun SettingsContentPreview() {
    BirthAppTheme {
        SettingsContent(
            uiState = SettingsUiState(versionName = "2.1.5"),
            events = emptyFlow(),
            callbacks = previewSettingsCallbacks
        )
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "设置页 · 深色")
@Composable
private fun SettingsContentPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(themeMode = ThemeMode.DARK, dynamicColor = true, versionName = "2.1.5"),
            events = emptyFlow(),
            callbacks = previewSettingsCallbacks
        )
    }
}
