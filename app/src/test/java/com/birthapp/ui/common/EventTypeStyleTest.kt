package com.birthapp.ui.common

import com.birthapp.data.EventType
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.SlateInk
import com.birthapp.ui.theme.SunnyYellow700
import com.birthapp.ui.theme.Teal500
import com.birthapp.ui.theme.Violet500
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 类型固定配色测试：修「生日/情侣纪念同色」的回归护栏。
 * 生日=Coral（品牌主色）· 情侣/结婚=Violet · 缅怀=SlateInk · 其他=SunnyYellow。
 */
class EventTypeStyleTest {

    @Test
    fun `生日与情侣纪念不再同色`() {
        assertNotEquals(
            eventAccent(EventType.BIRTHDAY),
            eventAccent(EventType.LOVE)
        )
    }

    @Test
    fun `生日为品牌主色Coral`() {
        assertEquals(Coral500, eventAccent(EventType.BIRTHDAY))
    }

    @Test
    fun `情侣与结婚同为紫色`() {
        assertEquals(Violet500, eventAccent(EventType.LOVE))
        assertEquals(eventAccent(EventType.LOVE), eventAccent(EventType.MARRIAGE))
    }

    @Test
    fun `缅怀为灰蓝_其他为黄`() {
        assertEquals(SlateInk, eventAccent(EventType.MEMORIAL))
        assertEquals(SunnyYellow700, eventAccent(EventType.OTHER))
    }

    @Test
    fun `宝宝与未知类型有兜底色不冲突`() {
        assertEquals(Teal500, eventAccent(EventType.BABY))
        // 未知类型走 Coral 兜底（与生日同色是预期的容错）
        assertEquals(Coral500, eventAccent("unknown"))
    }
}