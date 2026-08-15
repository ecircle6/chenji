package com.birthapp.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * 首页 Compose UI 测试（Robolectric 本地跑，不依赖模拟器）：
 * 渲染无状态的 HomeContent，用语义树断言列表/空态/切换，点击验证回调触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(
        birthdays: List<BirthdayDisplay> = previewBirthdays(),
        selectedTab: String = "all",
        selectedType: String = "all",
        availableTypes: List<String> = emptyList(),
        searchQuery: String = "",
        isSearching: Boolean = false,
        onAddClick: () -> Unit = {},
        onItemClick: (Long) -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onTabSelect: (String) -> Unit = {},
        onTypeSelect: (String) -> Unit = {},
        onSearchChange: (String) -> Unit = {},
        onEnterSearch: () -> Unit = {},
        onExitSearch: () -> Unit = {},
        onDeleteBirthday: (com.birthapp.data.Birthday) -> Unit = {}
    ) {
        compose.setContent {
            BirthAppTheme {
                HomeContent(
                    birthdays = birthdays,
                    selectedTab = selectedTab,
                    selectedType = selectedType,
                    availableTypes = availableTypes,
                    searchQuery = searchQuery,
                    isSearching = isSearching,
                    onAddClick = onAddClick,
                    onItemClick = onItemClick,
                    onSettingsClick = onSettingsClick,
                    onTabSelect = onTabSelect,
                    onTypeSelect = onTypeSelect,
                    onSearchChange = onSearchChange,
                    onEnterSearch = onEnterSearch,
                    onExitSearch = onExitSearch,
                    onDeleteBirthday = onDeleteBirthday
                )
            }
        }
    }

    @Test
    fun `列表_显示记录名字与倒计时`() {
        render()
        compose.onNodeWithText("小明").assertIsDisplayed()
        compose.onNodeWithText("364").assertIsDisplayed()
        compose.onNodeWithText("在一起三周年").assertIsDisplayed()
    }

    @Test
    fun `列表_空数据_显示引导空态`() {
        render(birthdays = emptyList())
        compose.onNodeWithText("还没有任何记录哦").assertIsDisplayed()
        compose.onNodeWithText("添加第一个记录").assertIsDisplayed()
    }

    @Test
    fun `搜索_无结果_显示搜索空态`() {
        render(birthdays = emptyList(), searchQuery = "张三", isSearching = true)
        compose.onNodeWithText("没有找到「张三」").assertIsDisplayed()
    }

    @Test
    fun `标签切换_点击朋友触发回调`() {
        var selected: String? = null
        render(onTabSelect = { selected = it })
        // 「家人」会同时命中卡片上的关系徽标（小明/爷爷都是家人），
        // 「朋友」只出现在标签行，选择唯一节点
        compose.onNodeWithText("朋友").performClick()
        assertEquals("friend", selected)
    }

    @Test
    fun `类型胶囊_点击类型触发回调`() {
        var selected: String? = null
        render(availableTypes = listOf("birthday", "love"), onTypeSelect = { selected = it })
        // 类型胶囊文案是「emoji + 类型名」，缅怀的 🕯️ 与生日区分明确
        compose.onNodeWithText("🎂 生日").performClick()
        assertEquals("birthday", selected)
    }

    @Test
    fun `搜索态_返回按钮触发退出回调`() {
        var exited = false
        render(isSearching = true, onExitSearch = { exited = true })
        compose.onNodeWithText("搜姓名或备注").assertIsDisplayed()
        compose.onNodeWithContentDescription("退出搜索").performClick()
        assertEquals(true, exited)
    }
}
