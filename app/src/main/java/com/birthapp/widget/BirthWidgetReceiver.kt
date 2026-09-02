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
     * 用户拖动调整小组件大小后，主动拉起重画。
     *
     * Glance 1.1.1 的 provider 对 options 变更没有任何处理（实测 resize 后
     * 桌面内容停在旧尺寸布局上），必须在这里 update 触发 provideGlance 重跑——
     * BirthWidget 的 composition 会从系统 options 读到新尺寸（见
     * BirthWidget.realWidgetSize）。updateAll 在没有任何实例时是安全的 no-op。
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