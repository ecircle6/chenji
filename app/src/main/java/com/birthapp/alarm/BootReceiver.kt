package com.birthapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.birthapp.BirthApp
import kotlinx.coroutines.launch

/**
 * 在以下场景重新调度所有生日闹钟，保证后台提醒不丢失：
 * - 设备重启（BOOT_COMPLETED）
 * - 应用更新/覆盖安装（MY_PACKAGE_REPLACED，系统会清掉旧闹钟）
 * - 系统时间或时区被修改（TIME_SET / TIMEZONE_CHANGED）
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }

        // goAsync 保证 onReceive 返回后进程不会立即被杀，异步调度能可靠完成
        val pendingResult = goAsync()
        val app = context.applicationContext as BirthApp
        app.applicationScope.launch {
            try {
                val birthdays = app.database.birthdayDao().getAllActiveOnce()
                val scheduler = AlarmScheduler(context, app.database)
                for (birthday in birthdays) {
                    scheduler.scheduleBirthdayReminder(birthday)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
