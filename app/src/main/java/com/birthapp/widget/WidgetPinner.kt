package com.birthapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * 「添加桌面小组件」的统一入口。
 *
 * `requestPinAppWidget`（API 26+，minSdk 26 全覆盖）是系统提供的唯一
 * 「App 自己唤起小组件放置流程」的公开 API：会弹出系统「添加到主屏幕」
 * 放置框，用户确认后才真正创建小组件。
 *
 * 约束（AOSP 明文）：仅在调用方持有**前台 Activity 或前台 Service** 时可用，
 * 否则抛 IllegalStateException——所以入口方都在前台时调（中转页在 onResume
 * 发起、主界面在点击瞬间调）。
 */
object WidgetPinner {

    /** 成功回调 PendingIntent 的固定请求码：同一条 Intent 反复覆写，不累积 */
    private const val SUCCESS_REQUEST_CODE = 0x5B11

    /**
     * 请求把 [BirthWidgetReceiver] 的小组件放到桌面。
     * @param onAdded 非空时，系统在小组件**真正创建成功后**发这条广播；
     *                用户取消放置则不会收到任何回调（API 不通知失败）。
     * @return 是否成功发起（桌面不支持 pin 机制的 launcher 会返回 false，
     *         调用方应给出引导提示而不是静默失败）
     */
    fun request(context: Context, onAdded: PendingIntent? = null): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        // provider 必须是 manifest 里注册的那个 AppWidgetProvider（即 Glance 的 receiver 本体）
        val provider = ComponentName(context, BirthWidgetReceiver::class.java)
        // SDK 26 起公开三参版本：后两个分别是 extras 与成功回调 PendingIntent，均可空
        return manager.requestPinAppWidget(provider, null, onAdded)
    }

    /** 构造「放置成功」回调广播：到达 [WidgetPinSuccessReceiver]（弹已添加提示） */
    fun successPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetPinSuccessReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            SUCCESS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}