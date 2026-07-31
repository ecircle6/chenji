package com.birthapp.util

import com.birthapp.data.Birthday
import com.birthapp.lunar.LunarCalendar
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 倒计时算法测试。重点盯农历腊月/冬月这类"对应阳历日期落在下一个公历年"的跨年场景：
 * 公历 1-2 月时，眼前这次事件属于"上一个农历年"，算法漏掉它就会多算出一整年。
 */
class EventCalcTest {

    private fun lunarBirthday(month: Int, day: Int) = Birthday(
        name = "测试",
        birthYear = 1990,
        birthMonth = month,
        birthDay = day,
        calendarType = "lunar"
    )

    @Test
    fun `公历年初时 腊月生日应算到眼前这次而不是一年后`() {
        // 农历2025年腊月十五 落在公历2026年年初（2026年春节=2月17日之前）
        val expected = LunarCalendar.lunarToSolar(2025, 12, 15).toLocalDate()
        // 站在 2026-01-10 看，这次生日就在眼前
        val today = LocalDate.of(2026, 1, 10)
        val next = EventCalc.nextSolarDate(lunarBirthday(12, 15), today).toLocalDate()
        assertEquals(expected, next)
    }

    @Test
    fun `公历年初时 冬月生日已过 应算下一个农历年`() {
        // 农历2025年冬月初一已经过了（在2025年12月内），下一次是农历2026年冬月初一
        val expected = LunarCalendar.lunarToSolar(2026, 11, 1).toLocalDate()
        val today = LocalDate.of(2026, 1, 10)
        val next = EventCalc.nextSolarDate(lunarBirthday(11, 1), today).toLocalDate()
        assertEquals(expected, next)
    }

    @Test
    fun `年中时 农历生日正常取当年或下一年`() {
        // 站在 2026-07-30：农历2026年五月初五(=2026-06-19)已过，下一次是2027年的
        val expected = LunarCalendar.lunarToSolar(2027, 5, 5).toLocalDate()
        val today = LocalDate.of(2026, 7, 30)
        val next = EventCalc.nextSolarDate(lunarBirthday(5, 5), today).toLocalDate()
        assertEquals(expected, next)

        // 农历2026年八月十五(=2026-09-25)还没到，就取它
        val expected2 = LocalDate.of(2026, 9, 25)
        val next2 = EventCalc.nextSolarDate(lunarBirthday(8, 15), today).toLocalDate()
        assertEquals(expected2, next2)
    }

    @Test
    fun `阳历生日跨年正常`() {
        val b = Birthday(
            name = "测试", birthYear = 1990, birthMonth = 3, birthDay = 5,
            calendarType = "solar"
        )
        assertEquals(
            LocalDate.of(2026, 3, 5),
            EventCalc.nextSolarDate(b, LocalDate.of(2026, 1, 10)).toLocalDate()
        )
        assertEquals(
            LocalDate.of(2027, 3, 5),
            EventCalc.nextSolarDate(b, LocalDate.of(2026, 7, 30)).toLocalDate()
        )
        // 当天算今天，不跳明年
        assertEquals(
            LocalDate.of(2026, 3, 5),
            EventCalc.nextSolarDate(b, LocalDate.of(2026, 3, 5)).toLocalDate()
        )
    }

    @Test
    fun `countdown 与指定的今天一致`() {
        val b = Birthday(
            name = "测试", birthYear = 1990, birthMonth = 8, birthDay = 10,
            calendarType = "solar"
        )
        assertEquals(11, EventCalc.countdown(b, LocalDate.of(2026, 7, 30)))
        assertEquals(0, EventCalc.countdown(b, LocalDate.of(2026, 8, 10)))
    }
}
