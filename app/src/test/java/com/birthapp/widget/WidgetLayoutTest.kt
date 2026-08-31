package com.birthapp.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 小组件布局纯函数测试：档位分流 + 行数换算（还原「版本 A」）。
 * 锁验收尺寸——真实 options 竖屏语义：
 * 110×110 静态回退 → 紧凑大字；4×2(266×135) → 宽档 3 行；
 * 4×3(266×208) → 宽档 3 行；4×4(266×281) → 大档 6 行；行数上限 6。
 */
class WidgetLayoutTest {

    // ---- 档位分流 ----

    @Test
    fun `manifest静态回退110x110_走紧凑大字`() {
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

    // ---- 目标行数（A 版固定）----

    @Test
    fun `目标行数_宽档3行_大档6行_紧凑1行`() {
        assertEquals(1, WidgetLayout.targetRows(WidgetLayout.Tier.COMPACT))
        assertEquals(3, WidgetLayout.targetRows(WidgetLayout.Tier.WIDE))
        assertEquals(6, WidgetLayout.targetRows(WidgetLayout.Tier.LARGE))
    }

    // ---- 实际行数（目标 × 高度护栏）----

    @Test
    fun `4x2高135_宽档3行`() {
        assertEquals(3, WidgetLayout.maxRowsFor(135, WidgetLayout.Tier.WIDE))
    }

    @Test
    fun `4x3高208_宽档3行`() {
        assertEquals(3, WidgetLayout.maxRowsFor(208, WidgetLayout.Tier.WIDE))
    }

    @Test
    fun `4x4高281_大档6行`() {
        assertEquals(6, WidgetLayout.maxRowsFor(281, WidgetLayout.Tier.LARGE))
    }

    @Test
    fun `横屏465x224_大档6行`() {
        assertEquals(6, WidgetLayout.maxRowsFor(224, WidgetLayout.Tier.LARGE))
    }

    @Test
    fun `高度富余_不超目标与上限`() {
        assertEquals(6, WidgetLayout.maxRowsFor(400, WidgetLayout.Tier.LARGE))
        assertEquals(3, WidgetLayout.maxRowsFor(400, WidgetLayout.Tier.WIDE))
    }

    @Test
    fun `过矮高度_护栏降行数`() {
        // 4×1 高 110：可用高 62dp，只够 2 行（防文字溢出裁切）
        assertEquals(2, WidgetLayout.maxRowsFor(110, WidgetLayout.Tier.WIDE))
        // 单格高 ~95：可用高 47dp，退 1 行
        assertEquals(1, WidgetLayout.maxRowsFor(95, WidgetLayout.Tier.WIDE))
        assertEquals(1, WidgetLayout.maxRowsFor(60, WidgetLayout.Tier.LARGE))
    }

    @Test
    fun `紧凑档_恒为1行`() {
        assertEquals(1, WidgetLayout.maxRowsFor(400, WidgetLayout.Tier.COMPACT))
        assertEquals(1, WidgetLayout.maxRowsFor(60, WidgetLayout.Tier.COMPACT))
    }

    // ---- 列表可用高度 ----

    @Test
    fun `列表可用高_扣掉外边距与品牌头`() {
        // 135 − 12×2 − 20 − 4 = 87
        assertEquals(87, WidgetLayout.listHeight(135))
        // 281 − 12×2 − 20 − 4 = 233
        assertEquals(233, WidgetLayout.listHeight(281))
    }
}