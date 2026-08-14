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
 * 保证生成不崩、产物文件与位图尺寸正确
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareCardGeneratorTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun birthday(
        name: String = "小明",
        eventType: String = EventType.BIRTHDAY,
        calendarType: String = "solar",
        month: Int = 8, day: Int = 14
    ) = Birthday(
        name = name, birthYear = 1998, birthMonth = month, birthDay = day,
        calendarType = calendarType, eventType = eventType,
        notes = "", isActive = true
    )

    @Test
    fun `生成卡片_生日类型_文件存在且为PNG`() {
        val file = ShareCardGenerator.generate(context, birthday())
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("png", file.extension)
    }

    @Test
    fun `生成卡片_缅怀类型_同样正常`() {
        val file = ShareCardGenerator.generate(
            context,
            birthday(name = "王奶奶", eventType = EventType.MEMORIAL, calendarType = "lunar")
        )
        assertTrue(file.exists())
    }

    @Test
    fun `绘制过程_各种类型都不抛异常`() {
        val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        for (type in EventType.ALL) {
            ShareCardGenerator.draw(canvas, birthday(eventType = type))
        }
        // 画过之后位图可用且尺寸正确
        assertTrue(!bitmap.isRecycled)
        assertEquals(1080, bitmap.width)
        assertEquals(1080, bitmap.height)
    }

    @Test
    fun `生成文件_放在cache下的share目录`() {
        val file = ShareCardGenerator.generate(context, birthday())
        assertEquals(File(context.cacheDir, "share").absolutePath, file.parent)
    }
}
