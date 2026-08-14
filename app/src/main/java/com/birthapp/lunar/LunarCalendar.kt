package com.birthapp.lunar

import java.time.DateTimeException
import java.time.LocalDate

data class SolarDate(val year: Int, val month: Int, val day: Int) {
    /**
     * 转 LocalDate。阳历 2/29 在平年不存在，退到 2/28（闰日生日平年提前一天过），
     * 不让 AlarmScheduler / EventCalc 等调用方崩
     */
    fun toLocalDate(): LocalDate = try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        LocalDate.of(year, month, day - 1)
    }
}

data class LunarDate(val year: Int, val month: Int, val day: Int, val isLeapMonth: Boolean = false)

object LunarCalendar {

    // 基准：农历1900年正月初一 = 阳历1900年1月31日
    private const val BASE_YEAR = 1900
    private const val BASE_SOLAR_MONTH = 1
    private const val BASE_SOLAR_DAY = 31

    private val LUNAR_MONTH_NAMES = arrayOf(
        "", "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )
    private val CHINESE_DAYS = arrayOf(
        "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    // ==================== 公开 API ====================

    /** 获取某年闰月月份（1-12），0 表示无闰月 */
    fun leapMonth(year: Int): Int = LUNAR_INFO[year - BASE_YEAR] and 0xf

    /** 获取某年闰月天数，0 表示无闰月 */
    fun leapMonthDays(year: Int): Int {
        return if (leapMonth(year) == 0) 0
        else if ((LUNAR_INFO[year - BASE_YEAR] and 0x10000) != 0) 30 else 29
    }

    /** 获取某年某农历月天数（29 或 30） */
    fun monthDays(year: Int, month: Int): Int {
        return if ((LUNAR_INFO[year - BASE_YEAR] and (0x10000 shr month)) != 0) 30 else 29
    }

    /** 获取某农历年的总天数 */
    fun yearDays(year: Int): Int {
        var sum = 0
        for (m in 1..12) sum += monthDays(year, m)
        return sum + leapMonthDays(year)
    }

    /** 农历转阳历 */
    fun lunarToSolar(year: Int, month: Int, day: Int, isLeap: Boolean = false): SolarDate {
        var totalDays = 0
        for (y in BASE_YEAR until year) totalDays += yearDays(y)
        for (m in 1 until month) totalDays += monthDays(year, m)
        val lm = leapMonth(year)
        if (!isLeap && lm in 1 until month) totalDays += leapMonthDays(year)
        if (isLeap && lm == month) totalDays += monthDays(year, month)
        totalDays += day - 1
        return addDaysToBase(totalDays)
    }

    /** 阳历转农历 */
    fun solarToLunar(year: Int, month: Int, day: Int): LunarDate {
        val totalDays = daysSinceBase(year, month, day)
        var lunarYear = BASE_YEAR
        var daysLeft = totalDays
        while (lunarYear < BASE_YEAR + LUNAR_INFO.size) {
            val yd = yearDays(lunarYear)
            if (daysLeft < yd) break
            daysLeft -= yd
            lunarYear++
        }
        val lm = leapMonth(lunarYear)
        var lunarMonth = 1
        var isLeapResult = false
        var leapHandled = false
        while (lunarMonth <= 12) {
            val md = monthDays(lunarYear, lunarMonth)
            if (daysLeft < md) break
            daysLeft -= md
            if (lunarMonth == lm && !leapHandled) {
                val lmd = leapMonthDays(lunarYear)
                if (daysLeft < lmd) { isLeapResult = true; break }
                daysLeft -= lmd
                leapHandled = true
            }
            lunarMonth++
        }
        return LunarDate(lunarYear, lunarMonth, daysLeft + 1, isLeapResult)
    }

    /**
     * 获取下一次农历生日对应的阳历日期。
     * 处理边界：该日不存在时降级为当月最后一天。
     */
    fun getNextLunarBirthdayInSolar(lunarMonth: Int, lunarDay: Int, isLeap: Boolean, fromYear: Int): SolarDate {
        for (y in fromYear..fromYear + 2) {
            try {
                if (isLeap && leapMonth(y) != lunarMonth) {
                    val adjustedDay = lunarDay.coerceAtMost(monthDays(y, lunarMonth))
                    return lunarToSolar(y, lunarMonth, adjustedDay, false)
                }
                if (!isLeap || leapMonth(y) == lunarMonth) {
                    val maxDay = if (isLeap) leapMonthDays(y) else monthDays(y, lunarMonth)
                    val adjustedDay = lunarDay.coerceAtMost(maxDay)
                    return lunarToSolar(y, lunarMonth, adjustedDay, isLeap && leapMonth(y) == lunarMonth)
                }
            } catch (_: Exception) { /* 该年该日不存在，继续尝试下一年 */ }
        }
        val safeDay = lunarDay.coerceAtMost(29)
        return lunarToSolar(fromYear, lunarMonth, safeDay, false)
    }

    /** 获取某年某月农历天数（含闰月） */
    fun getLunarMonthDays(year: Int, month: Int, isLeap: Boolean): Int {
        return if (isLeap && leapMonth(year) == month) leapMonthDays(year)
        else monthDays(year, month)
    }

    /** 格式化农历日期为中文 */
    fun formatLunarDate(month: Int, day: Int): String {
        val m = if (month in 1..12) LUNAR_MONTH_NAMES[month] else "${month}月"
        val d = if (day in 1..30) CHINESE_DAYS[day] else "${day}日"
        return "$m$d"
    }

    /** 农历日子名（初一、十五、廿三…），月历单元格小字标注用，不带月份 */
    fun lunarDayName(day: Int): String = if (day in 1..30) CHINESE_DAYS[day] else "${day}日"

    // ==================== 内部工具 ====================

    private fun daysSinceBase(year: Int, month: Int, day: Int): Int {
        val date = LocalDate.of(year, month, day)
        val base = LocalDate.of(BASE_YEAR, BASE_SOLAR_MONTH, BASE_SOLAR_DAY)
        return java.time.temporal.ChronoUnit.DAYS.between(base, date).toInt()
    }

    private fun addDaysToBase(days: Int): SolarDate {
        val base = LocalDate.of(BASE_YEAR, BASE_SOLAR_MONTH, BASE_SOLAR_DAY)
        val result = base.plusDays(days.toLong())
        return SolarDate(result.year, result.monthValue, result.dayOfMonth)
    }

    // ==================== 农历查表数据 (1900-2100) ====================
    // 编码: bit0-3=闰月月份(0=无), bit4-15=12-1月大小(1=30天), bit16=闰月大小(1=30天)

    private val LUNAR_INFO = intArrayOf(
        // 1900-1909
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        // 1910-1919
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        // 1920-1929
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        // 1930-1939
        0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        // 1940-1949
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        // 1950-1959
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        // 1960-1969
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        // 1970-1979
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        // 1980-1989
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        // 1990-1999
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        // 2000-2009
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        // 2010-2019
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        // 2020-2029
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        // 2030-2039
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        // 2040-2049
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        // 2050-2059
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06aa0, 0x1a6c4, 0x0aae0,
        // 2060-2069
        0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        // 2070-2079
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        // 2080-2089
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        // 2090-2099
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
        // 2100
        0x0d520
    )
}
