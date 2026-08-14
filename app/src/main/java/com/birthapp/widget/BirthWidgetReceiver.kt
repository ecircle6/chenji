package com.birthapp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 系统与小组件之间的入口。
 *
 * 真正的界面在 BirthWidget 里，这里只负责把系统的广播接过来。
 */
class BirthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthWidget()

    /** 小组件被移除时清掉它的配置，避免残留无用的选择项 */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetConfigStore.clear(context, it) }
    }
}
