package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.time.Duration

/**
 * 长按图标「添加小组件」的透明中转页：只发放置请求、不渲染任何界面。
 * 验证：支持 pin 时无失败提示 + 超时自动结束；不支持时弹引导；错误 action 直接结束。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPinRequestActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** ShadowToast 的最近一条是全局静态，跨用例/跨类会串，先清掉保证独立 */
    @Before
    fun resetToast() {
        ShadowToast.reset()
    }

    private fun buildActivity(action: String) = Robolectric.buildActivity(
        WidgetPinRequestActivity::class.java,
        Intent(context, WidgetPinRequestActivity::class.java).setAction(action)
    ).setup().get()

    /** 把主 looper 拨到 600ms 兜底超时之后，让中转页真正执行 finish */
    private fun idlePastTimeout() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(700))
    }

    @Test
    fun `支持pin时发出请求且成功回调被送达_超时后自动结束`() {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(true)
        val activity = buildActivity(WidgetPinRequestActivity.ACTION_REQUEST_WIDGET)
        // Robolectric 阴影「支持 pin」时会同步发出成功回调 → 收件人弹「已添加」，
        // 这里顺带证明了 请求→回调→提示 链路是通的（真实设备上系统在放置成功后发出）
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("小组件已添加到桌面", ShadowToast.getTextOfLatestToast())
        idlePastTimeout()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `不支持pin时弹引导提示且自动结束`() {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(false)
        val activity = buildActivity(WidgetPinRequestActivity.ACTION_REQUEST_WIDGET)
        assertEquals(
            "当前桌面不支持直接添加，请长按桌面空白处 → 小组件 → 辰记",
            ShadowToast.getTextOfLatestToast()
        )
        idlePastTimeout()
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `错误action时直接结束且不发请求`() {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(true)
        val activity = buildActivity("com.example.wrong.action")
        assertTrue(activity.isFinishing)
        assertNull(ShadowToast.getTextOfLatestToast())
    }
}