package com.birthapp.ui.home

import com.birthapp.data.EventType
import com.birthapp.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 首页三层化 + 月份分组的纯函数测试（HomeTier.kt）。
 * 与 HomeScreen 渲染解耦：这里只验证分层/进度/分组规则本身。
 */
class HomeTierTest {

    private fun display(
        id: Long,
        countdown: Int,
        eventYear: Int,
        eventMonth: Int,
        pinned: Boolean = false,
        paused: Boolean = false,
        eventType: String = EventType.BIRTHDAY
    ): BirthdayDisplay = PreviewData.display(
        PreviewData.birthday(id = id, name = "测试$id", eventType = eventType),
        countdown = countdown,
        isPaused = paused
    ).copy(isPinned = pinned, nextEventYear = eventYear, nextEventMonth = eventMonth)

    // ===================== 分层 =====================

    @Test
    fun `tierOf_按倒计时分层_0-7紧急_8-30标准_31以上远景`() {
        assertEquals(CardTier.URGENT, HomeTier.tierOf(0))
        assertEquals(CardTier.URGENT, HomeTier.tierOf(7))
        assertEquals(CardTier.NORMAL, HomeTier.tierOf(8))
        assertEquals(CardTier.NORMAL, HomeTier.tierOf(30))
        assertEquals(CardTier.DISTANT, HomeTier.tierOf(31))
        assertEquals(CardTier.DISTANT, HomeTier.tierOf(365))
    }

    // ===================== 紧急进度条 =====================

    @Test
    fun `progressOf_今天满_第7天归零_中间按天推进`() {
        assertEquals(1f, HomeTier.progressOf(0), 0.001f)          // 今天 = 100%
        assertEquals(4f / 7f, HomeTier.progressOf(3), 0.001f)      // 进入窗口第 4 天 = 57%
        assertEquals(0f, HomeTier.progressOf(7), 0.001f)           // 刚进窗口 = 0%
    }

    @Test
    fun `progressOf_窗口外钳制到_0`() {
        assertEquals(0f, HomeTier.progressOf(8), 0.001f)
        assertEquals(0f, HomeTier.progressOf(100), 0.001f)
    }

    @Test
    fun `elapsedDays_已过天数与倒计时互逆`() {
        assertEquals(7, HomeTier.elapsedDays(0))
        assertEquals(4, HomeTier.elapsedDays(3))
        assertEquals(0, HomeTier.elapsedDays(7))
    }

    // ===================== 月份标签 =====================

    @Test
    fun `monthLabel_同年显示月_跨年显示年份`() {
        val today = LocalDate.of(2026, 8, 14)
        assertEquals("9 月", HomeTier.monthLabel(2026, 9, today))
        assertEquals("12 月", HomeTier.monthLabel(2026, 12, today))
        assertEquals("2027 年", HomeTier.monthLabel(2027, 1, today))
    }

    // ===================== 构建异构列表 =====================

    @Test
    fun `buildRows_置顶卡置顶_暂停卡沉底`() {
        val rows = HomeTier.buildRows(
            listOf(
                display(id = 1, countdown = 300, eventYear = 2027, eventMonth = 5, paused = true),
                display(id = 2, countdown = 100, eventYear = 2026, eventMonth = 12, pinned = true),
                display(id = 3, countdown = 60, eventYear = 2026, eventMonth = 11)
            )
        )
        val cards = rows.filterIsInstance<HomeListItem.Card>()
        assertEquals(listOf(2L, 3L, 1L), cards.map { it.display.birthday.id })
    }

    @Test
    fun `buildRows_紧急标准远景按序排`() {
        val rows = HomeTier.buildRows(
            listOf(
                display(id = 1, countdown = 40, eventYear = 2026, eventMonth = 10),
                display(id = 2, countdown = 3, eventYear = 2026, eventMonth = 8),
                display(id = 3, countdown = 15, eventYear = 2026, eventMonth = 9)
            )
        )
        val cards = rows.filterIsInstance<HomeListItem.Card>()
        assertEquals(
            listOf(CardTier.URGENT, CardTier.NORMAL, CardTier.DISTANT),
            cards.map { it.tier }
        )
        // 远景行前有月份分隔标题
        val headers = rows.filterIsInstance<HomeListItem.MonthHeader>()
        assertEquals(listOf("10 月"), headers.map { it.label })
    }

    @Test
    fun `buildRows_同月远景只插一个分组标题`() {
        val rows = HomeTier.buildRows(
            listOf(
                display(id = 1, countdown = 100, eventYear = 2026, eventMonth = 12),
                display(id = 2, countdown = 120, eventYear = 2026, eventMonth = 12)
            )
        )
        assertEquals(
            1,
            rows.filterIsInstance<HomeListItem.MonthHeader>().size
        )
    }

    @Test
    fun `buildRows_跨年不同月份的分组标题key不重复`() {
        // 同一年里两个不同的远景月份：各成一个分组，label 都是「YYYY 年」。
        // LazyColumn 的 key 必须用 yearMonth（唯一），用 label 会碰撞崩溃——这里验证 key 唯一
        val today = LocalDate.now()
        val nextYear = today.year + 1
        val rows = HomeTier.buildRows(
            listOf(
                display(id = 1, countdown = 100, eventYear = nextYear, eventMonth = 1),
                display(id = 2, countdown = 300, eventYear = nextYear, eventMonth = 12)
            )
        )
        val headers = rows.filterIsInstance<HomeListItem.MonthHeader>()
        assertEquals(2, headers.size)
        // label 相同（跨年都显示「YYYY 年」），但 yearMonth 标识唯一
        assertEquals(1, headers.map { it.label }.distinct().size)
        assertEquals(2, headers.map { it.yearMonth }.distinct().size)
    }
}
