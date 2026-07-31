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
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    /** 导出到用户在系统文件选择器里挑好的位置 */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = BackupCodec.encode(recordsForExport())
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
                val text = BackupCodec.encode(recordsForExport())
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

    /** 从用户选的备份文件导入，合并去重：已有的跳过，新的追加 */
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
                val dao = database.birthdayDao()
                val fresh = BackupMerge.filterNew(dao.getAllOnce(), incoming)
                for (record in fresh) {
                    val id = dao.insert(record)
                    // 提醒闹钟按导入后的真实 id 排；已暂停的记录不排
                    val saved = record.copy(id = id)
                    if (saved.isActive) scheduler.scheduleBirthdayReminder(saved)
                }
                if (fresh.isNotEmpty()) WidgetRefresher.refresh(getApplication())
                fresh.size to (incoming.size - fresh.size)
            }
            result
                .onSuccess { (added, skipped) ->
                    toast(
                        if (added == 0) "没有新增：备份里的 $skipped 条记录这台手机上都有了"
                        else "导入完成：新增 $added 条" +
                            if (skipped > 0) "，跳过已有的 $skipped 条" else ""
                    )
                }
                .onFailure { toast(friendlyMessage(it, "导入失败，请确认选的是辰记的备份文件")) }
        }
    }

    private suspend fun recordsForExport() =
        database.birthdayDao().getAllOnce().also {
            require(it.isNotEmpty()) { "还没有记录，添加之后再来备份" }
        }

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
