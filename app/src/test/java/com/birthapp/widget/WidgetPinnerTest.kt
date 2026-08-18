package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * WidgetPinner（长按图标 / 设置页共用的小组件放置请求）单测。
 *
 * 注：Robolectric 的 ShadowAppWidgetManager 不记录传入的 provider，只有
 * `requestPinAppWidgetSupported` 这一个开关决定返回值，所以这里只锁「返回
 * 布尔契约 + 不崩溃」；provider 指向 BirthWidgetReceiver 由实现保证（即
 * manifest 注册的那个 AppWidgetProvider），真机验证兜底。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPinnerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `request_桌面支持pin时返回true且不崩溃`() {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(true)
        assertTrue(WidgetPinner.request(context))
    }

    @Test
    fun `request_桌面不支持pin时返回false`() {
        shadowOf(AppWidgetManager.getInstance(context)).setRequestPinAppWidgetSupported(false)
        assertFalse(WidgetPinner.request(context))
    }
}