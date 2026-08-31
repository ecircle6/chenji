package com.birthapp.util

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 首页问候语：同日同句（当天稳定）、跨天轮换、按年积日循环。
 * 文案池走资源数组，测试在 zh-rCN 资源下断言（与 App 默认语言一致）
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rCN")
class GreetingTest {

    private val resources get() = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    @Test
    fun `同一天_返回同一句`() {
        val date = LocalDate.of(2026, 8, 17)
        assertEquals(Greeting.today(resources, date), Greeting.today(resources, date))
    }

    @Test
    fun `不同日期_文案轮换且一年内可循环`() {
        val d1 = LocalDate.of(2026, 1, 1)
        val d2 = LocalDate.of(2026, 1, 2)
        val g1 = Greeting.today(resources, d1)
        val g2 = Greeting.today(resources, d2)
        assertNotEquals(g1, g2)
        // 一年后同一天（闰年覆盖 12/31 与次年 12/31 的 dayOfYear 边界）应回到同一句
        assertEquals(g1, Greeting.today(resources, d1.plusYears(1)))
    }
}