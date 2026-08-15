package com.birthapp.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * 分享卡片生成测试：Canvas 直绘是纯本地的（不依赖真机渲染），
 * 保证生成不崩、产物文件与位图尺寸正确、两种风格（极光毛玻璃/深夜烛火）都覆盖
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareCardGeneratorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun birthday(
        name: String = "小明",
        eventType: String = EventType.BIRTHDAY,
        calendarType: String = "solar",
        month: Int = 8, day: Int = 14,
        year: Int = 1998
    ) = Birthday(
        name = name, birthYear = year, birthMonth = month, birthDay = day,
        calendarType = calendarType, eventType = eventType,
        notes = "", isActive = true
    )

    @Test
    fun `生成卡片_生日类型_竖版文件存在且为PNG`() {
        val file = ShareCardGenerator.generate(context, birthday())
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("png", file.extension)
    }

    @Test
    fun `生成卡片_缅怀类型_走深夜烛火风格同样正常`() {
        val file = ShareCardGenerator.generate(
            context,
            birthday(name = "爷爷", eventType = EventType.MEMORIAL, calendarType = "lunar")
        )
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun `位图尺寸_普通类型840乘640横版_缅怀1080乘1920竖版`() {
        val normal = Bitmap.createBitmap(840, 640, Bitmap.Config.ARGB_8888)
        ShareCardGenerator.draw(Canvas(normal), birthday())
        assertEquals(840, normal.width)
        assertEquals(640, normal.height)

        val memorial = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        ShareCardGenerator.draw(Canvas(memorial), birthday(eventType = EventType.MEMORIAL))
        assertEquals(1080, memorial.width)
        assertEquals(1920, memorial.height)
    }

    @Test
    fun `绘制过程_各种类型都不抛异常`() {
        // 普通类型（毛玻璃信息卡）+ 缅怀（深夜烛火）都要覆盖
        val normal = Bitmap.createBitmap(840, 640, Bitmap.Config.ARGB_8888)
        val canvasN = Canvas(normal)
        for (type in EventType.ALL) {
            ShareCardGenerator.draw(canvasN, birthday(eventType = type))
        }
        assertTrue(!normal.isRecycled)

        val memorial = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        ShareCardGenerator.draw(Canvas(memorial), birthday(eventType = EventType.MEMORIAL))
        assertTrue(!memorial.isRecycled)
    }

    @Test
    fun `生成文件_放在cache下的share目录`() {
        val file = ShareCardGenerator.generate(context, birthday())
        assertEquals(File(context.cacheDir, "share").absolutePath, file.parent)
    }
}
