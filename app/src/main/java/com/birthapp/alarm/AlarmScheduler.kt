package com.birthapp.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.lunar.LunarCalendar
import com.birthapp.lunar.SolarDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context, private val database: AppDatabase) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 为指定生日调度精确闹钟（链式：每次触发后调度下一年）
     * suspend 函数：避免在主线程阻塞写数据库
     */
    suspend fun scheduleBirthdayReminder(birthday: Birthday) {
        val triggerTime = calculateNextTriggerTime(birthday) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_BIRTHDAY_ID, birthday.id)
            putExtra(EXTRA_NAME, birthday.name)
            putExtra(EXTRA_ADVANCE_DAYS, birthday.advanceDays)
            putExtra(EXTRA_CALENDAR_TYPE, birthday.calendarType)
            putExtra(EXTRA_BIRTH_MONTH, birthday.birthMonth)
            putExtra(EXTRA_BIRTH_DAY, birthday.birthDay)
            putExtra(EXTRA_BIRTH_YEAR, birthday.birthYear)
            putExtra(EXTRA_EVENT_TYPE, birthday.eventType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            birthday.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // 精确闹钟权限未授予，降级为非精确闹钟
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        // 缓存下次提醒日期
        val triggerDate = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(triggerTime),
            ZoneId.systemDefault()
        ).toLocalDate()
        database.birthdayDao().updateNextReminderDate(
            birthday.id,
            triggerDate.toString()
        )
    }

    /**
     * 取消指定生日的闹钟
     */
    fun cancelBirthdayReminder(birthdayId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            birthdayId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 计算下次触发闹钟的时间戳（毫秒）
     * 必须严格晚于当前时刻，否则闹钟会立即触发，导致提醒无限重复弹出
     */
    fun calculateNextTriggerTime(birthday: Birthday): Long? {
        val now = LocalDateTime.now()
        var year = now.year

        // 从今年开始逐年尝试，直到找到一个严格在未来的触发时刻
        repeat(3) {
            val birthdaySolar = getNextBirthdaySolarDate(birthday, year)
            val reminderDate = birthdaySolar.toLocalDate().minusDays(birthday.advanceDays.toLong())
            val triggerDateTime = reminderDate.atTime(birthday.reminderHour, birthday.reminderMinute)
            if (triggerDateTime.isAfter(now)) {
                return triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            year++
        }
        return null
    }

    /**
     * 计算生日在指定年份对应的阳历日期
     */
    private fun getNextBirthdaySolarDate(birthday: Birthday, year: Int): SolarDate {
        return if (birthday.calendarType == "lunar") {
            LunarCalendar.getNextLunarBirthdayInSolar(
                birthday.birthMonth,
                birthday.birthDay,
                birthday.isLeapMonth,
                year
            )
        } else {
            SolarDate(year, birthday.birthMonth, birthday.birthDay)
        }
    }

    companion object {
        const val EXTRA_BIRTHDAY_ID = "birthday_id"
        const val EXTRA_NAME = "name"
        const val EXTRA_ADVANCE_DAYS = "advance_days"
        const val EXTRA_CALENDAR_TYPE = "calendar_type"
        const val EXTRA_BIRTH_MONTH = "birth_month"
        const val EXTRA_BIRTH_DAY = "birth_day"
        const val EXTRA_BIRTH_YEAR = "birth_year"
        const val EXTRA_EVENT_TYPE = "event_type"
    }
}
