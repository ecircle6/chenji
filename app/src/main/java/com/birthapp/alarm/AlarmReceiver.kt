package com.birthapp.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.birthapp.BirthApp
import com.birthapp.notification.NotificationHelper
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

        // 发送通知
        val app = context.applicationContext as BirthApp
        NotificationHelper(context).showBirthdayNotification(
            birthdayId = birthdayId,
            name = name,
            advanceDays = advanceDays,
            calendarType = calendarType,
            birthMonth = birthMonth,
            birthDay = birthDay,
            birthYear = birthYear
        )

        // 链式调度下一年的提醒（goAsync 保证后台进程不会在调度完成前被杀）
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                val birthday = app.database.birthdayDao().getById(birthdayId) ?: return@launch
                val scheduler = AlarmScheduler(context, app.database)
                scheduler.scheduleBirthdayReminder(birthday)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
