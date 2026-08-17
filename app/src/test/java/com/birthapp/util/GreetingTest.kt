package com.birthapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

/** 首页问候语：同日同句（当天稳定）、跨天轮换、按年积日循环 */
class GreetingTest {

    @Test
    fun `同一天_返回同一句`() {
        val date = LocalDate.of(2026, 8, 17)
        assertEquals(Greeting.today(date), Greeting.today(date))
    }

    @Test
    fun `不同日期_文案轮换且一年内可循环`() {
        val d1 = LocalDate.of(2026, 1, 1)
        val d2 = LocalDate.of(2026, 1, 2)
        val g1 = Greeting.today(d1)
        val g2 = Greeting.today(d2)
        assertNotEquals(g1, g2)
        // 一年后同一天（闰年覆盖 12/31 与次年 12/31 的 dayOfYear 边界）应回到同一句
        assertEquals(g1, Greeting.today(d1.plusYears(1)))
    }
}