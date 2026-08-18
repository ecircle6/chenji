package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * 「添加快捷方式到主屏幕」的统一入口。
 *
 * `requestPinAppWidget`（API 26+，minSdk 26 全覆盖）是系统提供的唯一
 * 「App 自己唤起小组件放置流程」的公开 API：会弹出系统「添加到主屏幕」
 * 放置框，用户确认后自动走 birth_widget_info.xml 的 configure 配置页。
 * 长按应用图标的快捷方式和设置页入口共用这里，避免两处各写一遍。
 */
object WidgetPinner {

    /**
     * 请求把 [BirthWidgetReceiver] 的小组件放到桌面。
     * @return 是否成功发起（桌面不支持 pin 机制的 launcher 会返回 false，
     *         调用方应给出引导提示而不是静默失败）
     */
    fun request(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        // provider 必须是 manifest 里注册的那个 AppWidgetProvider（即 Glance 的 receiver 本体）
        val provider = ComponentName(context, BirthWidgetReceiver::class.java)
        // SDK 26 起公开三参版本：后两个分别是成功回调 Bundle 与 PendingIntent，均可空
        return manager.requestPinAppWidget(provider, null, null)
    }
}