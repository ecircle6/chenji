package com.birthapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.birthapp.BirthApp
import com.birthapp.data.EventType
import com.birthapp.notification.NotificationHelper
import com.birthapp.widget.WidgetRefresher
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val birthdayId = intent.getLongExtra(AlarmScheduler.EXTRA_BIRTHDAY_ID, -1)
        if (birthdayId == -1L) return

        val name = intent.getStringExtra(AlarmScheduler.EXTRA_NAME) ?: ""
        val advanceDays = intent.getIntExtra(AlarmScheduler.EXTRA_ADVANCE_DAYS, 0)
        val calendarType = intent.getStringExtra(AlarmScheduler.EXTRA_CALENDAR_TYPE) ?: "solar"
        val birthMonth = intent.getIntExtra(AlarmScheduler.EXTRA_BIRTH_MONTH, 1)
        val birthDay = intent.getIntExtra(AlarmScheduler.EXTRA_BIRTH_DAY, 1)
        val birthYear = intent.getIntExtra(AlarmScheduler.EXTRA_BIRTH_YEAR, 0)
        // 老闹钟（升级前排下的）没带类型，按生日处理
        val eventType = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_TYPE) ?: EventType.BIRTHDAY

        // 发送通知
        val app = context.applicationContext as BirthApp
        NotificationHelper(context).showBirthdayNotification(
            birthdayId = birthdayId,
            name = name,
            advanceDays = advanceDays,
            calendarType = calendarType,
            birthMonth = birthMonth,
            birthDay = birthDay,
            birthYear = birthYear,
            eventType = eventType
        )

        // 链式调度下一年的提醒（goAsync 保证后台进程不会在调度完成前被杀）
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                val birthday = app.database.birthdayDao().getById(birthdayId) ?: return@launch
                val scheduler = AlarmScheduler(context, app.database)
                scheduler.scheduleBirthdayReminder(birthday)
                // 提醒响过之后倒计时已经翻到下一年，桌面小组件得跟着更新
                WidgetRefresher.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
