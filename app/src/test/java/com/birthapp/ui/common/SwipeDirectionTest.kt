package com.birthapp.ui.common

import org.junit.Assert.assertEquals
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
}
