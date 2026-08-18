package com.birthapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 小组件放置成功的回调收件人。
 *
 * `requestPinAppWidget` 的 successCallback 只在小组件**真正创建成功后**才发，
 * 用户取消放置则收不到——所以这个提示不会谎报「已添加」。
 * 内部的 exported=false：只有我们自己构造的 PendingIntent 能触达。
 */
class WidgetPinSuccessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "小组件已添加到桌面", Toast.LENGTH_LONG).show()
    }
}