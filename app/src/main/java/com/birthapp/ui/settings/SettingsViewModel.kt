package com.birthapp.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.backup.BackupCodec
import com.birthapp.backup.BackupMerge
import com.birthapp.backup.BackupSettings
import com.birthapp.backup.ImportItem
import com.birthapp.settings.ThemeMode
import com.birthapp.widget.WidgetRefresher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 设置页的一次性事件：提示文字直接弹 Toast，分享事件由界面拉起系统分享面板 */
sealed interface SettingsEvent {
    data class Toast(val text: String) : SettingsEvent
    data class ShareFile(val uri: Uri) : SettingsEvent

    /** 导入预览：解析完备份文件后交给界面弹三选对话框（含备份里的主题设置） */
    data class ImportPreview(val items: List<ImportItem>, val settings: BackupSettings?) : SettingsEvent
}

/** 导入预览里每条记录的三选动作 */
enum class ImportAction { SKIP, INSERT, OVERWRITE }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)
    private val themeStore = (application as BirthApp).themeStore

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    /** 最近一次解析出的待导入记录（预览对话框确认时用） */
    private var pendingImport: List<ImportItem> = emptyList()

    /** 导出到用户在系统文件选择器里挑好的位置 */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = BackupCodec.encode(recordsForExport(), currentThemeMode(), currentDynamicColor())
                withContext(Dispatchers.IO) {
                    // "wt"：覆盖写。用户选了已存在的文件时要整个换掉，
                    // 默认模式只从头覆盖不截断，旧文件比新内容长会留一段脏尾巴
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri, "wt")
                        ?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        ?: throw IllegalArgumentException("无法写入所选位置")
                }
            }
            result
                .onSuccess { toast("备份已保存") }
                .onFailure { toast(friendlyMessage(it, "导出失败，请换个位置再试")) }
        }
    }

    /** 生成备份文件后交给界面拉起分享面板（发微信/QQ 传到另一台手机） */
    fun shareBackup() {
        viewModelScope.launch {
            val result = runCatching {
                val text = BackupCodec.encode(recordsForExport(), currentThemeMode(), currentDynamicColor())
                val file = withContext(Dispatchers.IO) {
                    // 放在缓存目录的独立子目录里，跟 FileProvider 配置的路径对应；
                    // 文件名固定，反复分享不会在缓存里越积越多
                    val dir = File(getApplication<Application>().cacheDir, "backup")
                    dir.mkdirs()
                    File(dir, backupFileName()).apply { writeText(text) }
                }
                FileProvider.getUriForFile(
                    getApplication(), "com.birthapp.fileprovider", file
                )
            }
            result
                .onSuccess { _events.emit(SettingsEvent.ShareFile(it)) }
                .onFailure { toast(friendlyMessage(it, "生成备份失败")) }
        }
    }

    /**
     * 解析用户选的备份文件，弹导入预览（逐条三选），不直接写库
     */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)
                        ?.use { input ->
                            val bytes = input.readBytes()
                            // 正常备份最多几百 KB，超大文件肯定是选错了
                            require(bytes.size <= MAX_IMPORT_BYTES) { "文件太大，不像是辰记的备份" }
                            String(bytes, Charsets.UTF_8)
                        }
                        ?: throw IllegalArgumentException("无法读取所选文件")
                }
                val incoming = BackupCodec.decode(text)
                val items = BackupMerge.classify(database.birthdayDao().getAllOnce(), incoming)
                val settings = BackupCodec.decodeSettings(text)
                items to settings
            }
            result
                .onSuccess { (items, settings) ->
                    pendingImport = items
                    pendingImportSettings = settings
                    _events.emit(SettingsEvent.ImportPreview(items, settings))
                }
                .onFailure { toast(friendlyMessage(it, "导入失败，请确认选的是辰记的备份文件")) }
        }
    }

    /**
     * 应用导入预览的逐条三选结果。
     * @param actions 与预览条目一一对应的动作
     * @param restoreSettings 是否同时恢复备份里的主题设置
     */
    fun applyImport(actions: List<ImportAction>, restoreSettings: Boolean) {
        viewModelScope.launch {
            val result = runCatching {
                val dao = database.birthdayDao()
                val existing = dao.getAllOnce()
                var inserted = 0
                var overwritten = 0
                pendingImport.zip(actions) { item, action ->
                    when (action) {
                        ImportAction.SKIP -> Unit
                        ImportAction.INSERT -> {
                            val id = dao.insert(item.record)
                            val saved = item.record.copy(id = id)
                            if (saved.isActive) scheduler.scheduleBirthdayReminder(saved)
                            inserted++
                        }
                        ImportAction.OVERWRITE -> {
                            // 覆盖 = 按判重 key 找到本机同一条，用导入数据更新（保留原 id）
                            val match = existing.firstOrNull {
                                BackupMerge.dedupKey(it) == BackupMerge.dedupKey(item.record)
                            }
                            if (match != null) {
                                val merged = item.record.copy(id = match.id)
                                dao.update(merged)
                                if (merged.isActive) scheduler.scheduleBirthdayReminder(merged)
                                else scheduler.cancelBirthdayReminder(merged)
                                overwritten++
                            } else {
                                // 理论上不会出现（预览时判过重），兜底按新增处理
                                val id = dao.insert(item.record)
                                val saved = item.record.copy(id = id)
                                if (saved.isActive) scheduler.scheduleBirthdayReminder(saved)
                                inserted++
                            }
                        }
                    }
                }
                if (inserted > 0 || overwritten > 0) {
                    WidgetRefresher.refresh(getApplication())
                }
                if (restoreSettings) {
                    pendingImportSettings?.let { s ->
                        s.themeMode?.let { mode ->
                            runCatching { themeStore.setMode(ThemeMode.valueOf(mode)) }
                        }
                        s.dynamicColor?.let { themeStore.setDynamicColor(it) }
                    }
                }
                inserted to overwritten
            }
            result
                .onSuccess { (inserted, overwritten) ->
                    if (inserted == 0 && overwritten == 0) {
                        toast("没有导入任何记录")
                    } else {
                        val parts = mutableListOf<String>()
                        if (inserted > 0) parts.add("新增 $inserted 条")
                        if (overwritten > 0) parts.add("覆盖 $overwritten 条")
                        toast("导入完成：${parts.joinToString("，")}")
                    }
                }
                .onFailure { toast(friendlyMessage(it, "导入失败，请重试")) }
        }
    }

    /** 预览对话框里带的主题设置（applyImport 恢复时用） */
    private var pendingImportSettings: BackupSettings? = null

    private suspend fun recordsForExport() =
        database.birthdayDao().getAllOnce().also {
            require(it.isNotEmpty()) { "还没有记录，添加之后再来备份" }
        }

    private fun currentThemeMode(): String = themeStore.mode.value.name
    private fun currentDynamicColor(): Boolean = themeStore.dynamicColor.value

    private suspend fun toast(text: String) = _events.emit(SettingsEvent.Toast(text))

    /** 自己抛的 IllegalArgumentException 里都是给用户看的话，直接用；其余给兜底文案 */
    private fun friendlyMessage(e: Throwable, fallback: String): String =
        if (e is IllegalArgumentException && !e.message.isNullOrBlank()) e.message!! else fallback

    companion object {
        private const val MAX_IMPORT_BYTES = 5 * 1024 * 1024

        /** 备份文件名带日期，用户存了多份也分得清 */
        fun backupFileName(): String =
            "辰记备份_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.json"
    }
}
