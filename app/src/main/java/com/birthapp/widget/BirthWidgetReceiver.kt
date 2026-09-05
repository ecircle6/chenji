package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 最新一次 options 变更的缓存：解决 resize 后 MIN 值在 getAppWidgetOptions 中短暂滞后的问题 */
internal object WidgetSizeCache {
    @Volatile var lastOptions: Bundle? = null
    @Volatile var lastId: Int = -1
    @Volatile var lastAt: Long = 0L
}

/** 小组件尺寸链路诊断日志：回调/防抖/应用/取值/档位各环节的排障入口 */
internal fun dbg(msg: String) {
    Log.d("BirthWidget", msg)
}

class BirthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthWidget()

    /** 小组件被移除时清掉它的配置，避免残留无用的选择项 */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetConfigStore.clear(context, it) }
    }

    /**
     * 用户拖动调整小组件大小。
     *
     * 拖拽期间系统每变一格就回调一次 options；若每次都重组合并推新
     * RemoteViews（super 的默认行为是立即 resize 重画），桌面端切换内容时
     * 的过渡动画会把新旧两套档位画面短暂叠在一起。这里改为防抖：回调只更新
     * [WidgetSizeCache]，尺寸停止变化 [RESIZE_APPLY_DELAY_MS] 后才重画一次——
     * 拖动中旧画面由 launcher 拉伸展示，松手停稳后一次性切到新档位。
     *
     * 不调 super 是刻意的：super 逐次回调立即重组合正是叠影来源，停稳后的
     * 这次 refresh 已覆盖最终档位（updateAll 无实例时是安全 no-op）。
     * 但必须自己 [goAsync]：本回调若发生在冷启动的进程里（系统为这条广播
     * 专门拉起进程），返回后进程立即可被回收，300ms 后的防抖应用会随进程
     * 一起丢掉——表现为「缩小后仍显示旧的多个通知」。goAsync 把进程存活期
     * 延长到防抖应用真正完成；新回调取代旧任务时提前 finish 旧的。
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val result = goAsync()
        val w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)
        val h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        dbg("optionsChanged id=$appWidgetId ${w}x$h")
        WidgetSizeCache.lastOptions = Bundle(newOptions)
        WidgetSizeCache.lastId = appWidgetId
        WidgetSizeCache.lastAt = System.currentTimeMillis()
        // 回调都在主线程，pending 表无需加锁；系统可能为每次回调重建 receiver
        // 实例，待应用任务必须挂在 companion 上，后一次才能取消前一次
        pendingApplies.remove(appWidgetId)?.let {
            resizeHandler.removeCallbacks(it.runnable)
            it.result.finish()
            dbg("superseded apply id=$appWidgetId（新回调取代，旧任务取消）")
        }
        val apply = Runnable {
            pendingApplies.remove(appWidgetId)
            dbg("apply firing id=$appWidgetId (debounce 到期)")
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    WidgetRefresher.refresh(context.applicationContext)
                } finally {
                    result.finish()
                }
            }
        }
        pendingApplies[appWidgetId] = PendingApply(apply, result)
        resizeHandler.postDelayed(apply, RESIZE_APPLY_DELAY_MS)
    }

    /** 一次待应用的防抖任务 + 撑住进程存活的广播 PendingResult */
    private class PendingApply(val runnable: Runnable, val result: BroadcastReceiver.PendingResult)

    private companion object {
        const val RESIZE_APPLY_DELAY_MS = 300L
        val resizeHandler = Handler(Looper.getMainLooper())
        val pendingApplies = mutableMapOf<Int, PendingApply>()
    }
}
