package com.birthapp.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 滑动删除方向锁定测试：只有横向明显占优（|Δx| > |Δy|）才判给左滑，
 * 垂直/斜向手势交给列表滚动，避免下滑滚动时误触发删除。
 */
class SwipeDirectionTest {

    private fun decide(totalX: Float, totalY: Float, slop: Float = 18f) =
        swipeDirection(totalX, totalY, slop)

    @Test
    fun `纯垂直滑动_判为纵向`() {
        assertEquals(SwipeDirection.Vertical, decide(0f, 40f))
    }

    @Test
    fun `纯水平滑动_判为横向`() {
        assertEquals(SwipeDirection.Horizontal, decide(-40f, 0f))
    }

    @Test
    fun `斜向滑动_纵向主导_判为纵向`() {
        assertEquals(SwipeDirection.Vertical, decide(10f, 40f))
    }

    @Test
    fun `斜向滑动_横向主导_判为横向`() {
        assertEquals(SwipeDirection.Horizontal, decide(-40f, 10f))
    }

    @Test
    fun `两轴位移相等_保守判为纵向`() {
        assertEquals(SwipeDirection.Vertical, decide(-20f, 20f))
    }

    @Test
    fun `两轴都未达触摸阈值_仍为待定`() {
        assertEquals(SwipeDirection.Undecided, decide(10f, 10f))
    }

    @Test
    fun `单轴已超阈值_另一轴未到_按已超阈值的方向判定`() {
        assertEquals(SwipeDirection.Horizontal, decide(-40f, 10f))
        assertEquals(SwipeDirection.Vertical, decide(10f, 40f))
    }

    @Test
    fun `自定义触摸阈值参与判定`() {
        assertEquals(SwipeDirection.Undecided, decide(5f, 5f, slop = 8f))
        assertEquals(SwipeDirection.Horizontal, decide(-10f, 6f, slop = 8f))
    }

    // ===================== 删除图标显隐（deleteIconAlpha）=====================

    @Test
    fun `静止时图标完全隐藏_透明远景行不透出`() {
        // 回归：远景迷你行透明背景，图标必须 opacity 0，否则从左滑容器里露出来
        assertEquals(0f, deleteIconAlpha(offset = 0f, width = 1000f), 0.001f)
    }

    @Test
    fun `宽未量到_一律隐藏_首帧不闪图标`() {
        assertEquals(0f, deleteIconAlpha(offset = -100f, width = 0f), 0.001f)
    }

    @Test
    fun `左滑超过三分之一宽度_图标全显`() {
        assertEquals(1f, deleteIconAlpha(offset = -340f, width = 1000f), 0.001f)
        assertEquals(1f, deleteIconAlpha(offset = -1000f, width = 1000f), 0.001f)
    }

    @Test
    fun `左滑中途_图标按进度线性淡入`() {
        // 滑 1/6 宽度 → 淡入到半透明
        assertEquals(0.5f, deleteIconAlpha(offset = -166.67f, width = 1000f), 0.01f)
        // 滑 1/10 → 刚起步
        val a = deleteIconAlpha(offset = -100f, width = 1000f)
        assertTrue(a in 0.2f..0.4f)
    }

    @Test
    fun `右滑或负位移被钳制为不显示`() {
        assertEquals(0f, deleteIconAlpha(offset = 100f, width = 1000f), 0.001f)
    }
}
