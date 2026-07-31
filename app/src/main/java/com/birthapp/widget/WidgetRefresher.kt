package com.birthapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * 通知桌面小组件重画。
 *
 * 小组件读的是数据库快照，不是 Flow，所以增删改、暂停恢复、提醒触发之后
 * 都得主动喊它一声，否则桌面上会一直停在旧数据上。
 * 桌面上没有添加过小组件时调用它也是安全的（updateAll 找不到实例就什么都不做）。
 */
object WidgetRefresher {

    suspend fun refresh(context: Context) {
        runCatching { BirthWidget().updateAll(context) }
    }
}
