package com.birthapp.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 深色模式的三种取向。label 直接用于设置页展示。
 */
enum class ThemeMode(val label: String, val desc: String) {
    SYSTEM("跟随系统", "随手机的深色模式自动切换"),
    LIGHT("始终浅色", "无论系统如何都用浅色"),
    DARK("始终深色", "无论系统如何都用深色")
}

/**
 * 主题偏好存储。用 SharedPreferences 落地，包一层 StateFlow 让界面能即时响应切换。
 *
 * 单例挂在 BirthApp 上（跟 database 一样），全 App 共用一份状态，
 * 在设置页改了之后首页、详情页会立刻跟着变。
 */
class ThemeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(load())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    /** Material You 动态取色：跟随系统壁纸配色（仅 Android 12+ 生效） */
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private fun load(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        // valueOf 对脏数据会抛异常，兜底回跟随系统
        return runCatching { ThemeMode.valueOf(saved!!) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _mode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "birthapp_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
