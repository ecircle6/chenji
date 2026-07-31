package com.birthapp.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DateUtils {

    /**
     * 计算从今天到目标日期的天数
     */
    fun daysUntil(targetMonth: Int, targetDay: Int): Int {
        val today = LocalDate.now()
        var target = LocalDate.of(today.year, targetMonth, targetDay)
        if (target.isBefore(today) || target.isEqual(today)) {
            target = LocalDate.of(today.year + 1, targetMonth, targetDay)
        }
        return ChronoUnit.DAYS.between(today, target).toInt()
    }

    /**
     * 计算从今天到目标阳历日期的天数（包含今年）
     */
    fun daysUntilDate(target: LocalDate): Int {
        val today = LocalDate.now()
        return ChronoUnit.DAYS.between(today, target).toInt()
    }

    /**
     * 获取今年的阳历生日日期
     */
    fun getThisYearBirthday(month: Int, day: Int): LocalDate {
        val today = LocalDate.now()
        val thisYear = LocalDate.of(today.year, month, day)
        return if (thisYear.isBefore(today)) {
            LocalDate.of(today.year + 1, month, day)
        } else {
            thisYear
        }
    }

    /**
     * 格式化阳历日期为中文
     */
    fun formatSolarDate(year: Int, month: Int, day: Int): String {
        return "${year}年${month}月${day}日"
    }

    /**
     * 格式化月日为简短中文
     */
    fun formatSolarMonthDay(month: Int, day: Int): String {
        return "${month}月${day}日"
    }

    /**
     * 格式化提醒时间
     */
    fun formatReminderTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }
}
