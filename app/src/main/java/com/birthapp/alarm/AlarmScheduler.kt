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
     * 为指定生日调度精确闹钟（链式：每次触发后调度下一年）。
     * 多级提前提醒：每个级别一个闹钟（提前 N 天与当天落在不同日期，互不干扰），
     * requestCode 用 id*32+级别索引 区分，不同记录之间不会互相覆盖
     */
    suspend fun scheduleBirthdayReminder(birthday: Birthday) {
        val levels = normalizeAdvanceLevels(birthday.advanceDays)
        val triggerTimes = levels.mapIndexedNotNull { index, level ->
            val triggerTime = calculateNextTriggerTime(birthday, level) ?: return@mapIndexedNotNull null

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_BIRTHDAY_ID, birthday.id)
                putExtra(EXTRA_NAME, birthday.name)
                putExtra(EXTRA_ADVANCE_DAYS, level)
                putExtra(EXTRA_CALENDAR_TYPE, birthday.calendarType)
                putExtra(EXTRA_BIRTH_MONTH, birthday.birthMonth)
                putExtra(EXTRA_BIRTH_DAY, birthday.birthDay)
                putExtra(EXTRA_BIRTH_YEAR, birthday.birthYear)
                putExtra(EXTRA_EVENT_TYPE, birthday.eventType)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(birthday.id, index),
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
            triggerTime
        }

        // 缓存最近一次触发（所有级别里最早的），详情页"下次提醒"与真实闹钟一致
        val nextTrigger = triggerTimes.minOrNull()
        if (nextTrigger != null) {
            val triggerDate = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(nextTrigger),
                ZoneId.systemDefault()
            ).toLocalDate()
            database.birthdayDao().updateNextReminderDate(
                birthday.id,
                triggerDate.toString()
            )
        }
    }

    /**
     * 取消指定生日的全部闹钟：所有级别 + 兼容 v2 时代排下的单闹钟码（id.toInt()）
     */
    fun cancelBirthdayReminder(birthday: Birthday) {
        val levels = normalizeAdvanceLevels(birthday.advanceDays)
        levels.indices.forEach { index ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(birthday.id, index),
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        val legacyPendingIntent = PendingIntent.getBroadcast(
            context,
            birthday.id.toInt(),
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(legacyPendingIntent)
    }

    companion object {
        /** 每条记录最多支持的提前提醒级别数 */
        const val MAX_ADVANCE_LEVELS = 10

        /** 多级闹钟的 requestCode：id*32+级别索引，不同记录的码段不重叠 */
        fun requestCode(birthdayId: Long, levelIndex: Int): Int = birthdayId.toInt() * 32 + levelIndex

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

/**
 * 提前提醒级别规范化：钳制 0..365、去重、升序、上限 10 个；空列表退化为 [0]（当天）
 */
fun normalizeAdvanceLevels(days: List<Int>): List<Int> {
    val normalized = days
        .filter { it in 0..365 }
        .distinct()
        .sorted()
        .take(AlarmScheduler.MAX_ADVANCE_LEVELS)
    return if (normalized.isEmpty()) listOf(0) else normalized
}

/**
 * 计算某级别提前提醒的下次触发时间戳（毫秒）。
 * 顶层纯函数（`now` 可注入便于单测），AlarmScheduler 与 DetailViewModel 共用，
 * 保证页面显示的"下次提醒"和真正会响的闹钟是同一个时间。
 * 必须严格晚于当前时刻，否则闹钟会立即触发，导致提醒无限重复弹出
 */
fun calculateNextTriggerTime(
    birthday: Birthday,
    advanceDays: Int,
    now: LocalDateTime = LocalDateTime.now()
): Long? {
    // 从去年起步：农历腊月/冬月对应的阳历日期落在下一个公历年，
    // 公历 1-2 月时眼前这次属于"上一个农历年"，从今年找会直接错过这次提醒
    var year = now.year - 1

    // 逐年尝试，直到找到一个严格在未来的触发时刻
    repeat(4) {
        val birthdaySolar = getNextBirthdaySolarDate(birthday, year)
        val reminderDate = birthdaySolar.toLocalDate().minusDays(advanceDays.toLong())
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
