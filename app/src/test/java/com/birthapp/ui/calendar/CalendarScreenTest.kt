package com.birthapp.ui.calendar

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.ui.theme.BirthAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.YearMonth

/**
 * 日历页 UI 测试：标题为「YYYY年M月」格式、翻月可切换、
 * 有事件的日期可弹窗进详情。月历是首页改版后新独立页，必须补 UI 保护。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarScreenTest {

    @get:Rule
    val compose = createComposeRule()

    /** 月份标题匹配器：YYYY年M月（不依赖系统当前日期） */
    private val monthTitle = SemanticsMatcher("月份标题") { node ->
        node.config.getOrNull(SemanticsProperties.Text)
            ?.any { Regex("\\d{4}年\\d{1,2}月").matches(it.text) } == true
    }

    private fun birthday(name: String, month: Int, day: Int, id: Long = 1L) = Birthday(
        id = id,
        name = name,
        birthYear = 1990,
        birthMonth = month,
        birthDay = day,
        calendarType = "solar",
        relation = "family",
        eventType = EventType.BIRTHDAY
    )

    /** 当前标题文本（YYYY年M月） */
    private fun titleText(): String? =
        compose.onAllNodes(monthTitle)
            .fetchSemanticsNodes()
            .firstOrNull()
            ?.config?.getOrNull(SemanticsProperties.Text)
            ?.joinToString()

    @Test
    fun `月历_标题为年月格式_可翻月`() {
        // 当月 1 号放一个事件
        val first = YearMonth.now().atDay(1)
        val birthdays = listOf(birthday("小明", first.monthValue, first.dayOfMonth))

        compose.setContent {
            BirthAppTheme {
                CalendarScreen(birthdays = birthdays, onItemClick = {})
            }
        }
        compose.onNode(monthTitle).assertExists()
        compose.onNodeWithContentDescription("上个月").assertExists()
        compose.onNodeWithContentDescription("下个月").assertExists()

        // 翻到上个月不崩溃，标题仍是年月格式
        compose.onNodeWithContentDescription("上个月").performClick()
        compose.onNode(monthTitle).assertExists()
    }

    @Test
    fun `月历_有事件的日期_弹窗可进详情`() {
        val first = YearMonth.now().atDay(1)
        val birthdays = listOf(birthday("小明", first.monthValue, first.dayOfMonth))

        var clickedId: Long = -1
        compose.setContent {
            BirthAppTheme {
                CalendarScreen(birthdays = birthdays, onItemClick = { clickedId = it })
            }
        }
        // 点击月初日 → 弹窗列出当日事件 → 点名字进详情
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("小明").assertExists()
        compose.onNodeWithText("小明").performClick()
        assertEquals(1L, clickedId)
    }

    @Test
    fun `月历_翻月后标题变化`() {
        compose.setContent {
            BirthAppTheme {
                CalendarScreen(birthdays = emptyList(), onItemClick = {})
            }
        }
        val before = titleText()
        compose.onNodeWithContentDescription("下个月").performClick()
        val after = titleText()
        assertNotEquals("翻月后标题应变", before, after)
    }
}