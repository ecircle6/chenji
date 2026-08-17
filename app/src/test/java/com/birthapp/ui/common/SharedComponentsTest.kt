package com.birthapp.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.birthapp.ui.preview.previewBirthdays
import com.birthapp.ui.theme.BirthAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 共享组件 Compose UI 测试：BirthdayCard（紧凑布局）与两种空态。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharedComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `生日卡片_显示名字类型标签日期关系与倒计时`() {
        // previewBirthdays 第一条是置顶的小明（8月14日·家人，364 天后）
        val display = previewBirthdays()[0]
        compose.setContent {
            BirthAppTheme { BirthdayCard(display = display, index = 0, onClick = {}) }
        }
        compose.onNodeWithText("小明").assertIsDisplayed()
        // emoji 字形在 Robolectric 下节点尺寸为 0，改用存在性断言
        compose.onNodeWithText("📌").assertExists()
        compose.onNodeWithText("生日").assertExists()          // 类型标签
        compose.onNodeWithText("8月14日 · 家人", substring = true).assertExists()
        compose.onNodeWithText("364").assertIsDisplayed()
        // 紧凑版移除了 ⏰ 提醒时刻徽标
        compose.onNodeWithText("⏰", substring = true).assertDoesNotExist()
    }

    @Test
    fun `空列表_显示引导文案与按钮`() {
        var clicked = false
        compose.setContent {
            BirthAppTheme { EmptyBirthdayList(onAddClick = { clicked = true }) }
        }
        compose.onNodeWithText("还没有任何记录哦").assertIsDisplayed()
        compose.onNodeWithText("添加第一个记录").performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun `空搜索_显示关键词`() {
        compose.setContent {
            BirthAppTheme { EmptySearchResult(keyword = "王五") }
        }
        compose.onNodeWithText("没有找到「王五」").assertIsDisplayed()
    }
}