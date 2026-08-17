package com.birthapp.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.ui.preview.PreviewData
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
        // 在一起三周年是最近事件，被 Hero 卡聚焦且从列表去重：全屏只出现一次
        compose.onAllNodesWithText("在一起三周年").assertCountEquals(1)
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
        // Hero 取最近日期组里的庆祝事件 = 在一起三周年；列表去重后全屏只出现一次
        var clickedId: Long = -1
        render(onItemClick = { clickedId = it })
        compose.onNodeWithText("即将到来").assertIsDisplayed()
        compose.onAllNodesWithText("在一起三周年").assertCountEquals(1)
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
    fun `Hero卡_最近全是缅怀时不显示`() {
        // 最近日期里只有缅怀 → 悼念不被庆祝式大卡放大，列表正常展示缅怀卡
        val memorials = listOf(
            PreviewData.display(
                PreviewData.birthday(id = 1, name = "爷爷", eventType = EventType.MEMORIAL),
                countdown = 5
            ),
            PreviewData.display(
                PreviewData.birthday(id = 2, name = "奶奶", eventType = EventType.MEMORIAL),
                countdown = 12
            )
        )
        render(birthdays = memorials)
        compose.onNodeWithText("即将到来").assertDoesNotExist()
        compose.onNodeWithText("爷爷").assertIsDisplayed()
    }

    @Test
    fun `Hero卡_7天内显示提醒进度条`() {
        val list = listOf(
            PreviewData.display(
                PreviewData.birthday(id = 1, name = "妈妈生日", month = 8, day = 22),
                countdown = 5
            )
        )
        render(birthdays = list)
        compose.onNodeWithText("即将到来").assertIsDisplayed()
        // 5 天后 → 已进入 7 天窗口第 3 天：已过去 2 天
        compose.onNodeWithText("提醒进度").assertIsDisplayed()
        compose.onNodeWithText("已过去 2 天").assertIsDisplayed()
    }

    @Test
    fun `Hero卡_超过7天不显示进度条`() {
        val list = listOf(
            PreviewData.display(
                PreviewData.birthday(id = 1, name = "妈妈生日", month = 9, day = 4),
                countdown = 18
            )
        )
        render(birthdays = list)
        compose.onNodeWithText("即将到来").assertIsDisplayed()
        compose.onNodeWithText("提醒进度").assertDoesNotExist()
    }

    @Test
    fun `Hero卡_同日缅怀与庆典并存_聚焦庆典且缅怀留在列表`() {
        // 爷爷缅怀与二叔生日同日（都剩 5 天）→ Hero 聚焦庆祝事件；
        // 列表去重只去掉 Hero 那条，缅怀仍以紧急卡呈现
        val list = listOf(
            PreviewData.display(
                PreviewData.birthday(id = 1, name = "爷爷", eventType = EventType.MEMORIAL),
                countdown = 5
            ),
            PreviewData.display(
                PreviewData.birthday(id = 2, name = "二叔生日"),
                countdown = 5
            )
        )
        render(birthdays = list)
        compose.onNodeWithText("即将到来").assertIsDisplayed()
        compose.onAllNodesWithText("二叔生日").assertCountEquals(1)
        compose.onNodeWithText("爷爷").assertIsDisplayed()
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

    @Test
    fun `远景_跨年两个月份分组_不崩溃且标题可渲染`() {
        // 复现跨年远景行的分组 key 碰撞场景：明年第 1 月与第 12 月各一条远景记录，
        // 会生成两个 label 相同的「YYYY 年」分组标题。LazyColumn key 必须唯一，否则组合期崩溃。
        // 用缅怀记录构造：缅怀不参与 Hero 聚焦、不会被去重，两条都留在远景分组
        val nextYear = java.time.LocalDate.now().year + 1
        val list = listOf(
            PreviewData.display(
                PreviewData.birthday(id = 11, name = "明年一月缅怀", eventType = EventType.MEMORIAL),
                countdown = 120
            ).copy(nextEventYear = nextYear, nextEventMonth = 1),
            PreviewData.display(
                PreviewData.birthday(id = 12, name = "明年十二月缅怀", eventType = EventType.MEMORIAL),
                countdown = 300
            ).copy(nextEventYear = nextYear, nextEventMonth = 12)
        )
        render(birthdays = list)
        // 两个分组标题应同时渲染（一个在下个月之前，一个更远）
        compose.onAllNodesWithText("$nextYear 年").assertCountEquals(2)
        // 两条缅怀都留在列表（远景行文本是「名字 · 日期」拼接，用 substring 计数）
        compose.onAllNodesWithText("明年一月缅怀", substring = true).assertCountEquals(1)
        compose.onAllNodesWithText("明年十二月缅怀", substring = true).assertCountEquals(1)
    }
}