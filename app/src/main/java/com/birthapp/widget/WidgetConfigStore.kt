package com.birthapp.widget

import android.content.Context

/**
 * 小组件配置存储：按 appWidgetId 记录展示方式。
 * "auto" = 自动展示最近记录（默认）；数字字符串 = 只展示指定 id 的记录。
 * 与主题设置共用 birthapp_settings 文件
 */
object WidgetConfigStore {

    private const val PREFS_NAME = "birthapp_settings"
    private const val KEY_PREFIX = "widget_selection_"

    /** 默认值：自动展示最近记录 */
    const val AUTO = "auto"

    fun get(context: Context, appWidgetId: Int): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("$KEY_PREFIX$appWidgetId", AUTO) ?: AUTO

    fun set(context: Context, appWidgetId: Int, selection: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_PREFIX$appWidgetId", selection)
            .apply()
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_PREFIX$appWidgetId")
            .apply()
    }
}
