package com.birthapp.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.birthapp.data.Birthday
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
 * 渲染无状态的 HomeContent，用语义树断言列表/Hero/筛选/空态，点击验证回调触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(
        birthdays: List<BirthdayDisplay> = previewBirthdays(),
        filter: FilterState = FilterState(),
        availableTypes: List<String> = emptyList(),
        searchQuery: String = "",
        isSearching: Boolean = false,
        onAddClick: () -> Unit = {},
        onItemClick: (Long) -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onQuickFilter: (String, String) -> Unit = { _, _ -> },
        onUpdateFilter: (String, String) -> Unit = { _, _ -> },
        onClearFilters: () -> Unit = {},
        onSearchChange: (String) -> Unit = {},
        onEnterSearch: () -> Unit = {},
        onExitSearch: () -> Unit = {},
        onDeleteBirthday: (Birthday) -> Unit = {}
    ) {
        compose.setContent {
            BirthAppTheme {
                HomeContent(
                    birthdays = birthdays,
                    filter = filter,
                    availableTypes = availableTypes,
                    searchQuery = searchQuery,
                    isSearching = isSearching,
                    onAddClick = onAddClick,
                    onItemClick = onItemClick,
                    onSettingsClick = onSettingsClick,
                    onQuickFilter = onQuickFilter,
                    onUpdateFilter = onUpdateFilter,
                    onClearFilters = onClearFilters,
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
        // 在一起三周年同时出现在 Hero 卡与列表卡片（各一处）
        compose.onAllNodesWithText("在一起三周年").assertCountEquals(2)
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
    fun `快捷胶囊_点击朋友触发单维筛选回调`() {
        var dim: String? = null
        var value: String? = null
        render(onQuickFilter = { d, v -> dim = d; value = v })
        // 「家人」会同时命中卡片关系小字（小明/爷爷都是家人）；
        // 「朋友」只出现在快捷筛选行，选择唯一节点
        compose.onNodeWithText("朋友").performClick()
        assertEquals(FilterDim.RELATION, dim)
        assertEquals("friend", value)
    }

    @Test
    fun `快捷胶囊_点击类型触发回调`() {
        var dim: String? = null
        var value: String? = null
        render(availableTypes = listOf("birthday", "love"), onQuickFilter = { d, v -> dim = d; value = v })
        // 快捷行类型胶囊文案是「emoji + 类型名」
        compose.onNodeWithText("🎂 生日").performClick()
        assertEquals(FilterDim.TYPE, dim)
        assertEquals("birthday", value)
    }

    @Test
    fun `Hero卡_显示最近事件并可点击进详情`() {
        // previewBirthdays：小明 364 天（置顶）、在一起三周年 7 天、爷爷 12 天
        // Hero 取倒计时最小 = 在一起三周年
        var clickedId: Long = -1
        render(onItemClick = { clickedId = it })
        compose.onNodeWithText("即将到来").assertIsDisplayed()
        // Hero 与列表卡片各一处
        compose.onAllNodesWithText("在一起三周年").assertCountEquals(2)
        compose.onNodeWithText("即将到来").performClick()
        assertEquals(2L, clickedId)
    }

    @Test
    fun `Hero卡_全暂停时不显示`() {
        val allPaused = previewBirthdays().map {
            it.copy(isPaused = true, countdown = Int.MAX_VALUE)
        }
        render(birthdays = allPaused)
        compose.onNodeWithText("即将到来").assertDoesNotExist()
    }

    @Test
    fun `更多筛选_点击更多按钮弹筛选面板`() {
        render()
        compose.onNodeWithContentDescription("更多筛选").performClick()
        compose.onNodeWithText("生肖").assertIsDisplayed()
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