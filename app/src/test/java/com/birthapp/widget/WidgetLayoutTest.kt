package com.birthapp.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 小组件布局纯函数测试：档位分流 + 行数换算。
 * 锁验收尺寸——真实 options 竖屏语义：
 * 110×110 静态回退 → 紧凑单焦；4×2(266×135) → 宽档 2 行；
 * 4×3(266×208) → 宽档 3 行；4×4(266×281) → 大档 4 行（Hero+4 = 5 条）；行数上限 5。
 */
class WidgetLayoutTest {

    // ---- 档位分流 ----

    @Test
    fun `manifest静态回退110x110_走紧凑单焦`() {
        assertEquals(WidgetLayout.Tier.COMPACT, WidgetLayout.tierOf(110, 110))
    }

    @Test
    fun `2格内宽度_高度再高也紧凑`() {
        assertEquals(WidgetLayout.Tier.COMPACT, WidgetLayout.tierOf(199, 400))
    }

    @Test
    fun `4x2竖屏_宽档`() {
        assertEquals(WidgetLayout.Tier.WIDE, WidgetLayout.tierOf(266, 135))
    }

    @Test
    fun `4x3竖屏_宽档`() {
        assertEquals(WidgetLayout.Tier.WIDE, WidgetLayout.tierOf(266, 208))
    }

    @Test
    fun `4x4竖屏_大档`() {
        assertEquals(WidgetLayout.Tier.LARGE, WidgetLayout.tierOf(266, 281))
    }

    @Test
    fun `高度210边界_进大档`() {
        assertEquals(WidgetLayout.Tier.LARGE, WidgetLayout.tierOf(266, 210))
    }

    @Test
    fun `横屏465x224_按高度进大档`() {
        assertEquals(WidgetLayout.Tier.LARGE, WidgetLayout.tierOf(465, 224))
    }

    // ---- 行数公式 ----

    @Test
    fun `可用高不足1行_退1行`() {
        assertEquals(1, WidgetLayout.rowsFor(0))
        assertEquals(1, WidgetLayout.rowsFor(-20))
        assertEquals(1, WidgetLayout.rowsFor(91))
    }

    @Test
    fun `2行边界_92dp恰好两行`() {
        assertEquals(2, WidgetLayout.rowsFor(92))
        assertEquals(1, WidgetLayout.rowsFor(91))
    }

    @Test
    fun `3行边界_140dp恰好三行`() {
        assertEquals(3, WidgetLayout.rowsFor(140))
        assertEquals(2, WidgetLayout.rowsFor(139))
    }

    @Test
    fun `行数上限5_空间再多也只5行`() {
        assertEquals(5, WidgetLayout.rowsFor(1000))
        assertEquals(5, WidgetLayout.rowsFor(236))
        assertEquals(4, WidgetLayout.rowsFor(235))
    }

    // ---- 宽档：4×2 → 2 行、4×3 → 3 行 ----

    @Test
    fun `4x2高135_宽档2行`() {
        assertEquals(2, WidgetLayout.wideRows(135))
    }

    @Test
    fun `4x3高208_宽档3行`() {
        assertEquals(3, WidgetLayout.wideRows(208))
    }

    @Test
    fun `宽档拉高逐步加行_上限5`() {
        assertEquals(2, WidgetLayout.wideRows(176 - 1))
        assertEquals(3, WidgetLayout.wideRows(176))
        assertEquals(5, WidgetLayout.wideRows(400))
    }

    // ---- 大档：≥4×4 → Hero + 4 行（完整 5 条）----

    @Test
    fun `4x4高281_大档4行`() {
        assertEquals(4, WidgetLayout.largeRows(281))
    }

    @Test
    fun `大档拉高逐步加行_上限5`() {
        assertEquals(5, WidgetLayout.largeRows(400))
        assertEquals(5, WidgetLayout.largeRows(355))
        assertEquals(4, WidgetLayout.largeRows(281))
    }

    // ---- 行框挤压实态 ----

    @Test
    fun `4x2两行行框约50dp_需紧凑变体`() {
        val listHeight = WidgetLayout.wideListHeight(135)
        assertTrue(WidgetLayout.rowNeedsDense(listHeight, 2))
    }

    @Test
    fun `行框富余_普通行即可`() {
        assertFalse(WidgetLayout.rowNeedsDense(172, 3))
        assertFalse(WidgetLayout.rowNeedsDense(159, 3))
    }

    @Test
    fun `只有1行_永不切紧凑`() {
        assertFalse(WidgetLayout.rowNeedsDense(20, 1))
    }
}