package com.birthapp.widget

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 小组件配置存储测试：按 appWidgetId 隔离、默认自动、删除清理
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetConfigStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `未配置时_默认自动`() {
        assertEquals(WidgetConfigStore.AUTO, WidgetConfigStore.get(context, 1))
    }

    @Test
    fun `设置后_按appWidgetId隔离读取`() {
        WidgetConfigStore.set(context, 1, "5")
        WidgetConfigStore.set(context, 2, WidgetConfigStore.AUTO)
        assertEquals("5", WidgetConfigStore.get(context, 1))
        // 不同实例互不影响
        assertEquals(WidgetConfigStore.AUTO, WidgetConfigStore.get(context, 2))
    }

    @Test
    fun `清除后_回到默认自动`() {
        WidgetConfigStore.set(context, 3, "7")
        WidgetConfigStore.clear(context, 3)
        assertEquals(WidgetConfigStore.AUTO, WidgetConfigStore.get(context, 3))
    }
}
