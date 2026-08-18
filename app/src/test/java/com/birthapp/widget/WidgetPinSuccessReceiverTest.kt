package com.birthapp.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * 放置成功回调收件人：只在小组件真正创建成功后系统才发这条广播，
 * 这里锁「收到广播 → 弹已添加提示」的文案。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPinSuccessReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetToast() {
        // ShadowToast 最近一条是全局静态，先清掉避免与别的用例串
        ShadowToast.reset()
    }

    @Test
    fun `收到成功广播时弹已添加提示`() {
        WidgetPinSuccessReceiver().onReceive(context, Intent())
        assertEquals("小组件已添加到桌面", ShadowToast.getTextOfLatestToast())
    }
}