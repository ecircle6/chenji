package com.birthapp.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 通知相关偏好：默认提醒时间（新记录用）与提醒总开关。
 * 与主题设置共用 birthapp_settings 文件。
 * 提醒总开关用 StateFlow 包装，设置页切换后界面即时响应
 */
class ReminderSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 新记录默认提醒时刻（时） */
    val defaultHour: Int
        get() = prefs.getInt(KEY_DEFAULT_HOUR, 8).coerceIn(0, 23)

    /** 新记录默认提醒时刻（分） */
    val defaultMinute: Int
        get() = prefs.getInt(KEY_DEFAULT_MINUTE, 0).coerceIn(0, 59)

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDERS_ENABLED, true))
    /** 提醒总开关：关闭后不再调度任何闹钟 */
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    fun setDefaultTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DEFAULT_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_DEFAULT_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
        _remindersEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "birthapp_settings"
        private const val KEY_DEFAULT_HOUR = "default_reminder_hour"
        private const val KEY_DEFAULT_MINUTE = "default_reminder_minute"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }
}
