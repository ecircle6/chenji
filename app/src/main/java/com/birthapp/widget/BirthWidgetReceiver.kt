package com.birthapp.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.unit.IntSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 每个小组件一份的尺寸状态。
 *
 * Glance 1.1.1 对长活组合会话（provideContent 挂着 Flow 订阅，会话一直存活）
 * 的 `update()` 是空操作——`updateGlance` 只刷新 state datastore，不会以新
 * 尺寸重组，缩放后档位因此锁死在会话开始时的值。让尺寸以可观察状态进入组合：
 * Receiver 防抖停稳后写入 [State.size]，运行中的组合 collect 它自动重组一次；
 * 无会话场景（冷进程首帧）由 updateAll 启动会话，组合读 [State.size] 现值。
 * [State.lastOptions] 保留最后一次回调原文，供停稳写入时取值。
 */
internal object WidgetSizeCache {
    class State {
        val size = MutableStateFlow<IntSize?>(null)
        var lastOptions: Bundle? = null
    }

    private val states = mutableMapOf<Int, State>()

    fun stateFor(appWidgetId: Int): State =
        synchronized(states) { states.getOrPut(appWidgetId) { State() } }

    fun clear(appWidgetId: Int) = synchronized(states) { states.remove(appWidgetId) }
}

/** 小组件尺寸链路诊断日志：回调/防抖/应用/取值/档位各环节的排障入口 */
internal fun dbg(msg: String) {
    Log.d("BirthWidget", msg)
}

class BirthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BirthWidget()

    /** 小组件被移除时清掉它的配置与尺寸状态，避免残留 */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach {
            WidgetConfigStore.clear(context, it)
            WidgetSizeCache.clear(it)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // 调试辅助（仅 debug 包生效）：免手势模拟一次 options 变更，供缩放回归——
        // adb shell am broadcast -n com.birthapp/.widget.BirthWidgetReceiver \
        //   -a com.birthapp.DEBUG_SIMULATE_RESIZE --ei id 2 --ei w 172 --ei h 135
        if (intent.action == ACTION_DEBUG_SIMULATE_RESIZE &&
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        ) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            val w = intent.getIntExtra(EXTRA_W, -1)
            val h = intent.getIntExtra(EXTRA_H, -1)
            if (id > 0 && w > 0 && h > 0) {
                dbg("debug simulate resize id=$id ${w}x$h")
                val opts = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, w)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, h)
                }
                onOptionsChanged(context, id, opts)
            }
        }
    }

    /**
     * 用户拖动调整小组件大小（系统入口）。
     *
     * 拖拽期间系统每变一格就回调一次 options；若每次都推新 RemoteViews
     * （super 会逐次调 resize 重画），桌面端切换内容时的过渡动画会把新旧
     * 两套档位画面短暂叠在一起。这里改为防抖：回调只暂存 options，尺寸
     * 停止变化 [RESIZE_APPLY_DELAY_MS] 后才应用一次——拖动中旧画面由
     * launcher 拉伸展示（各小组件标准行为），松手停稳后一次性切到新档位。
     *
     * 不调 super 是刻意的：super 逐次回调立即 resize 正是叠影来源；停稳后
     * 的这次应用（尺寸状态流 + updateAll）已覆盖最终档位。
     * 必须自己 [goAsync]：冷启动进程里 onReceive 返回后进程立即可被回收，
     * 300ms 后的防抖应用会随进程一起丢掉；新回调取代旧任务时提前 finish。
     */
    // MissingSuperCall 在此是误报：不调 super 正是上方注释所述的防叠影设计
    @SuppressLint("MissingSuperCall")
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        onOptionsChanged(context, appWidgetId, newOptions)
    }

    private fun onOptionsChanged(context: Context, appWidgetId: Int, newOptions: Bundle) {
        val result = goAsync()
        val w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)
        val h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        dbg("optionsChanged id=$appWidgetId ${w}x$h")
        WidgetSizeCache.stateFor(appWidgetId).lastOptions = Bundle(newOptions)
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
            // 1) 尺寸写入可观察状态：运行中的会话据此重组（update() 对活会话
            //    只刷 state 不重组，纯读值链路的档位会锁死在会话开始时）
            val state = WidgetSizeCache.stateFor(appWidgetId)
            state.lastOptions?.let { opts ->
                val cW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                val cH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                if (cW > 0 && cH > 0) state.size.value = IntSize(cW, cH)
            }
            // 2) updateAll：无会话场景（冷进程首帧）由此启动会话渲染；
            //    活会话时无害空操作
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
        dbg("scheduled apply id=$appWidgetId in ${RESIZE_APPLY_DELAY_MS}ms")
    }

    /** 一次待应用的防抖任务 + 撑住进程存活的广播 PendingResult */
    private class PendingApply(val runnable: Runnable, val result: BroadcastReceiver.PendingResult)

    private companion object {
        const val RESIZE_APPLY_DELAY_MS = 300L
        const val ACTION_DEBUG_SIMULATE_RESIZE = "com.birthapp.DEBUG_SIMULATE_RESIZE"
        const val EXTRA_ID = "id"
        const val EXTRA_W = "w"
        const val EXTRA_H = "h"
        val resizeHandler = Handler(Looper.getMainLooper())
        val pendingApplies = mutableMapOf<Int, PendingApply>()
    }
}
