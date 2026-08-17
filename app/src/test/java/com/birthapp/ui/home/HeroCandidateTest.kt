package com.birthapp.ui.home

import com.birthapp.data.EventType
import com.birthapp.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Hero 候选规则纯函数测试（HeroCard.kt 的 heroCandidate）。
 * 规则：取「最近日期组」里的庆祝事件（非缅怀）；最近全是缅怀 → null；
 * 暂停不参与；同日多条庆祝取排序第一条（输入即已排序：置顶优先）。
 */
class HeroCandidateTest {

    private fun display(
        id: Long,
        countdown: Int,
        eventType: String = EventType.BIRTHDAY,
        pinned: Boolean = false,
        paused: Boolean = false
    ): BirthdayDisplay = PreviewData.display(
        PreviewData.birthday(id = id, name = "测试$id", eventType = eventType),
        countdown = countdown,
        isPaused = paused
    ).copy(isPinned = pinned)

    @Test
    fun `空列表_无Hero`() {
        assertEquals(null, heroCandidate(emptyList()))
    }

    @Test
    fun `全部暂停_无Hero`() {
        val list = listOf(
            display(1, 3, paused = true),
            display(2, 10, paused = true)
        )
        assertEquals(null, heroCandidate(list))
    }

    @Test
    fun `取倒计时最小_非置顶也可当选`() {
        // 置顶记录列表排最前，但可能不是最近；Hero 应聚焦最近的庆祝事件
        val list = listOf(
            display(1, 364, pinned = true),
            display(2, 7)
        )
        assertEquals(2L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `庆祝超过7天_仍当选Hero`() {
        val list = listOf(display(1, 18), display(2, 40))
        assertEquals(1L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `庆祝7天内_当选Hero`() {
        val list = listOf(display(1, 5), display(2, 40))
        assertEquals(1L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `最近全是缅怀_无Hero`() {
        val list = listOf(
            display(1, 5, eventType = EventType.MEMORIAL),
            display(2, 5, eventType = EventType.MEMORIAL)
        )
        assertEquals(null, heroCandidate(list))
    }

    @Test
    fun `最近的缅怀_即使有更远的庆祝_也无Hero`() {
        // 最近日期里只有缅怀 → 悼念不被庆祝式大卡放大
        val list = listOf(
            display(1, 3, eventType = EventType.MEMORIAL),
            display(2, 12, eventType = EventType.BIRTHDAY)
        )
        assertEquals(null, heroCandidate(list))
    }

    @Test
    fun `同日缅怀与庆典_取庆典`() {
        val list = listOf(
            display(1, 5, eventType = EventType.MEMORIAL),
            display(2, 5, eventType = EventType.BIRTHDAY)
        )
        assertEquals(2L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `同日多条庆祝_取排序第一条_置顶优先`() {
        val list = listOf(
            display(1, 18, pinned = true),
            display(2, 18),
            display(3, 18)
        )
        assertEquals(1L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `暂停记录不参与候选_哪怕倒计时最小`() {
        val list = listOf(
            display(1, 1, paused = true),
            display(2, 9)
        )
        assertEquals(2L, heroCandidate(list)?.birthday?.id)
    }

    @Test
    fun `情侣纪念也是庆祝_可取Hero`() {
        val list = listOf(
            display(1, 6, eventType = EventType.LOVE),
            display(2, 20, eventType = EventType.MEMORIAL)
        )
        assertEquals(1L, heroCandidate(list)?.birthday?.id)
    }
}
