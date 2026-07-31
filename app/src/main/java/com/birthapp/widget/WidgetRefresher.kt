package com.birthapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * 通知桌面小组件重画。
 *
 * 小组件平时坐在数据库 Flow 上自动跟新，但那个订阅只在小组件的
 * 后台会话存活期间有效，而系统很快就会回收会话。增删改之后
 * 主动喊一声，能把会话重新拉起来、立刻按最新数据重画。
 * 桌面上没有添加过小组件时调用它也是安全的（updateAll 找不到实例就什么都不做）。
 */
object WidgetRefresher {

    suspend fun refresh(context: Context) {
        runCatching { BirthWidget().updateAll(context) }
    }
}
