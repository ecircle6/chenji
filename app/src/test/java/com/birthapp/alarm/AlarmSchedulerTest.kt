package com.birthapp.alarm

import com.birthapp.data.Birthday
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * calculateNextTriggerTime 的纯逻辑单测（JVM，不依赖 Android 框架）。
 * 覆盖：提前天数、自定义提醒时刻、农历腊月/冬月跨公历年边界、闰月缺失年降级、
 * 阳历 2/29 平年降级、严格大于 now 的边界、确定性。
 * 农历换算的期望值取自已通过 1901-2098 全量往返验证的 LunarCalendar 换算结果。
 */
class AlarmSchedulerTest {

    private fun solar(
        month: Int, day: Int,
        year: Int = 2000,
        advanceDays: Int = 0,
        hour: Int = 8, minute: Int = 0
    ) = Birthday(
        name = "测试", birthYear = year, birthMonth = month, birthDay = day,
        calendarType = "solar", advanceDays = advanceDays,
        reminderHour = hour, reminderMinute = minute
    )

    private fun lunar(
        month: Int, day: Int,
        isLeap: Boolean = false,
        advanceDays: Int = 0,
        hour: Int = 8, minute: Int = 0
    ) = Birthday(
        name = "测试", birthYear = 2000, birthMonth = month, birthDay = day,
        calendarType = "lunar", isLeapMonth = isLeap, advanceDays = advanceDays,
        reminderHour = hour, reminderMinute = minute
    )

    /** 期望时刻转成与函数相同口径的时间戳（本地时区） */
    private fun epoch(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ==================== 当天提醒 / 提前天数 ====================

    @Test
    fun `当天提醒_时刻未到_今天触发`() {
        val b = solar(8, 10, hour = 8)
        val now = LocalDateTime.of(2026, 8, 10, 7, 0)
        assertEquals(epoch(2026, 8, 10, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `当天提醒_时刻已过_顺延到明年同一天`() {
        val b = solar(8, 10, hour = 8)
        val now = LocalDateTime.of(2026, 8, 10, 9, 0)
        assertEquals(epoch(2027, 8, 10, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `触发时刻恰好等于now_严格大于_顺延明年`() {
        val b = solar(8, 10, hour = 8)
        val now = LocalDateTime.of(2026, 8, 10, 8, 0)
        assertEquals(epoch(2027, 8, 10, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `提前一天_在生日前一天触发`() {
        val b = solar(8, 10, advanceDays = 1)
        val now = LocalDateTime.of(2026, 8, 1, 0, 0)
        assertEquals(epoch(2026, 8, 9, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `提前七天_跨月_三月一日生日在二月二十二日提醒`() {
        val b = solar(3, 1, advanceDays = 7)
        val now = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(epoch(2026, 2, 22, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `提前天数大使提醒日已过_顺延到下一年`() {
        val b = solar(8, 10, advanceDays = 5)
        // 8/5 已经过了
        val now = LocalDateTime.of(2026, 8, 8, 8, 0)
        assertEquals(epoch(2027, 8, 5, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `提前365天_提醒日落在四年的搜索窗口尾部`() {
        val b = solar(1, 1, advanceDays = 365)
        val now = LocalDateTime.of(2026, 8, 14, 0, 0)
        // 2028-01-01 生日提前 365 天 = 2027-01-01，是窗口内第一个未来时刻
        assertEquals(epoch(2027, 1, 1, 8, 0), calculateNextTriggerTime(b, now))
    }

    // ==================== 自定义提醒时刻 ====================

    @Test
    fun `自定义提醒时刻_23点59分边界`() {
        val b = solar(8, 10, hour = 23, minute = 59)
        val now = LocalDateTime.of(2026, 8, 10, 23, 58)
        assertEquals(epoch(2026, 8, 10, 23, 59), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `自定义提醒时刻_零点_前一天23点59分后触发`() {
        val b = solar(8, 10, hour = 0, minute = 0)
        val now = LocalDateTime.of(2026, 8, 9, 23, 59)
        assertEquals(epoch(2026, 8, 10, 0, 0), calculateNextTriggerTime(b, now))
    }

    // ==================== 阳历 2/29（平年降级）====================

    @Test
    fun `阳历2月29日_平年不存在_按2月28日提醒`() {
        val b = solar(2, 29)
        val now = LocalDateTime.of(2026, 1, 1, 0, 0) // 2026 非闰年
        assertEquals(epoch(2026, 2, 28, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `阳历2月29日_闰年正常当天提醒`() {
        val b = solar(2, 29)
        val now = LocalDateTime.of(2028, 2, 28, 0, 0) // 2028 是闰年
        assertEquals(epoch(2028, 2, 29, 8, 0), calculateNextTriggerTime(b, now))
    }

    // ==================== 农历跨公历年边界 ====================

    @Test
    fun `农历腊月生日_跨公历年_正月前命中上一个农历年`() {
        // 农历 2025 腊月十五 = 阳历 2026-02-02；now 在 2026 年初，应命中这一次
        val b = lunar(12, 15)
        val now = LocalDateTime.of(2026, 1, 10, 0, 0)
        assertEquals(epoch(2026, 2, 2, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `农历冬月生日_跨公历年_冬月属于上一个农历年`() {
        // 农历 2025 冬月十五 = 阳历 2026-01-03（2025 冬月 29 天，冬月初一 = 2025-12-20）；
        // now 在 2025 年底，应命中它
        val b = lunar(11, 15)
        val now = LocalDateTime.of(2025, 12, 25, 0, 0)
        assertEquals(epoch(2026, 1, 3, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `农历腊月生日_已过_顺延到下一个腊月`() {
        // 农历 2026 腊月十五 = 阳历 2027-01-22
        val b = lunar(12, 15)
        val now = LocalDateTime.of(2026, 2, 2, 8, 1)
        assertEquals(epoch(2027, 1, 22, 8, 0), calculateNextTriggerTime(b, now))
    }

    // ==================== 闰月生日 ====================

    @Test
    fun `闰月生日_当年无闰月_降级为正常月份`() {
        // 2026 年没有闰六月：闰六月十五的生日在 2026 年按六月十五过
        // 农历 2026 六月十五 = 阳历 2026-07-28
        val b = lunar(6, 15, isLeap = true)
        val now = LocalDateTime.of(2026, 6, 1, 0, 0)
        assertEquals(epoch(2026, 7, 28, 8, 0), calculateNextTriggerTime(b, now))
    }

    @Test
    fun `闰月生日_当年有闰月_在闰月过`() {
        // 2025 年有闰六月：农历 2025 闰六月十五 = 阳历 2025-08-08
        val b = lunar(6, 15, isLeap = true)
        val now = LocalDateTime.of(2025, 5, 1, 0, 0)
        assertEquals(epoch(2025, 8, 8, 8, 0), calculateNextTriggerTime(b, now))
    }

    // ==================== 确定性 ====================

    @Test
    fun `相同输入_多次调用结果一致`() {
        val b = lunar(12, 15)
        val now = LocalDateTime.of(2026, 1, 10, 0, 0)
        val first = calculateNextTriggerTime(b, now)
        val second = calculateNextTriggerTime(b, now)
        assertEquals(first, second)
    }
}
