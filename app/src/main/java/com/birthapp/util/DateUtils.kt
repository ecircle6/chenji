package com.birthapp.util

import android.content.res.Resources
import com.birthapp.R
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

    /** 英文月份缩写（Jan..Dec），与 values/values-en 的 months_short 数组同名 */
    private fun shortMonth(resources: Resources, month: Int): String =
        resources.getStringArray(R.array.months_short)[(month - 1).coerceIn(0, 11)]

    /**
     * 格式化阳历日期：中文「1997年1月5日」，英文「Jan 5, 1997」（模板见资源）
     */
    fun formatSolarDate(resources: Resources, year: Int, month: Int, day: Int): String =
        if (LocaleUtils.isEnglish(resources)) {
            resources.getString(R.string.date_solar_full_en, shortMonth(resources, month), day, year)
        } else {
            resources.getString(R.string.date_solar_full_zh, year, month, day)
        }

    /**
     * 格式化月日：中文「1月5日」，英文「Jan 5」
     */
    fun formatSolarMonthDay(resources: Resources, month: Int, day: Int): String =
        if (LocaleUtils.isEnglish(resources)) {
            resources.getString(R.string.date_solar_month_day_en, shortMonth(resources, month), day)
        } else {
            resources.getString(R.string.date_solar_month_day_zh, month, day)
        }

    /** 星期简称：中文「周一」，英文「Mon」 */
    fun weekdayShort(resources: Resources, dayOfWeek: java.time.DayOfWeek): String =
        resources.getStringArray(R.array.weekday_short)[dayOfWeek.value - 1]

    /**
     * 格式化提醒时间
     */
    fun formatReminderTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }
}