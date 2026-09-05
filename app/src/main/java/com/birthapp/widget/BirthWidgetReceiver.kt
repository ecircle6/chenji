package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统与小组件之间的入口。
 *
 * 真正的界面在 BirthWidget 里，这里只负责把系统的广播接过来。
 */
/** 最新一次 options 变更的缓存：解决缩小后 MIN 值在 getAppWidgetOptions 中短暂滞后的问题 */
internal object WidgetSizeCache {
    @Volatile var lastOptions: Bundle? = null
    @Volatile var lastId: Int = -1
    @Volatile var lastAt: Long = 0L
}

class BirthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthWidget()

    /** 小组件被移除时清掉它的配置，避免残留无用的选择项 */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetConfigStore.clear(context, it) }
    }

    /**
     * 用户拖动调整小组件大小后的兜底重画。
     *
     * SizeMode.Exact 下 Glance 已订阅尺寸变化，resize 会自动重新组合并读新
     * 尺寸（见 BirthWidget.realWidgetSize），这里主动 updateAll 只作额外保险：
     * 覆盖个别 launcher 回调时序异常时组合未被拉起的情况。updateAll 在没有
     * 任何实例时是安全的 no-op。
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetSizeCache.lastOptions = Bundle(newOptions)
        WidgetSizeCache.lastId = appWidgetId
        WidgetSizeCache.lastAt = System.currentTimeMillis()
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        CoroutineScope(Dispatchers.Default).launch { WidgetRefresher.refresh(context) }
    }
}