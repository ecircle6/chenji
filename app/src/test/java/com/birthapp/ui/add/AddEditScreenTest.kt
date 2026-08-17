package com.birthapp.ui.add

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.birthapp.data.EventType
import com.birthapp.ui.theme.BirthAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 添加/编辑页 Compose UI 测试：渲染无状态的 AddEditContent，
 * 断言默认表单、保存按钮启停、切换与各回调触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AddEditScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(
        state: AddEditUiState = AddEditUiState(),
        onUpdateEventType: (String) -> Unit = {},
        onUpdateName: (String) -> Unit = {},
        onUpdateEmoji: (String) -> Unit = {},
        onUpdateCalendarType: (String) -> Unit = {},
        onToggleAdvanceDay: (Int) -> Unit = {},
        onSave: () -> Unit = {},
        onDelete: () -> Unit = {}
    ) {
        compose.setContent {
            BirthAppTheme {
                AddEditContent(
                    state = state,
                    onBack = {},
                    onUpdateEventType = onUpdateEventType,
                    onUpdateName = onUpdateName,
                    onUpdateEmoji = onUpdateEmoji,
                    onUpdateCalendarType = onUpdateCalendarType,
                    onUpdateBirthYear = {},
                    onUpdateBirthMonth = {},
                    onUpdateBirthDay = {},
                    onUpdateLeapMonth = {},
                    onToggleAdvanceDay = onToggleAdvanceDay,
                    onAddCustomAdvanceDay = {},
                    onRemoveAdvanceDay = {},
                    onUpdateReminderTime = { _, _ -> },
                    onUpdateRelation = {},
                    onUpdateNotes = {},
                    onSave = onSave,
                    onDelete = onDelete
                )
            }
        }
    }

    @Test
    fun `添加模式_默认表单完整_保存按钮禁用`() {
        render()
        compose.onNodeWithText("添加记录").assertIsDisplayed()
        compose.onNodeWithText("姓名").assertIsDisplayed()
        compose.onNodeWithText("保存").assertIsNotEnabled()
    }

    @Test
    fun `添加模式_名字非空_保存按钮启用`() {
        render(state = AddEditUiState(name = "小明"))
        compose.onNodeWithText("保存").assertIsEnabled()
    }

    @Test
    fun `阳历农历切换_点击农历触发回调`() {
        var type: String? = null
        render(onUpdateCalendarType = { type = it })
        compose.onNodeWithText("🌙 农历").performClick()
        assertEquals("lunar", type)
    }

    @Test
    fun `提前提醒_点击预设chip触发回调`() {
        var day: Int? = null
        render(onToggleAdvanceDay = { day = it })
        // Emoji 区块导致表单变长，"3天" 可能需要滚动才能可见
        compose.onNodeWithText("3天").performScrollTo().performClick()
        assertEquals(3, day)
    }

    @Test
    fun `编辑模式_显示删除按钮且标题为编辑`() {
        render(state = AddEditUiState(id = 1, name = "爷爷", isEditMode = true))
        compose.onNodeWithText("编辑记录").assertIsDisplayed()
        compose.onNodeWithContentDescription("删除").assertIsDisplayed()
    }

    @Test
    fun `类型选择_默认生日选中_点击缅怀触发回调`() {
        var type: String? = null
        render(onUpdateEventType = { type = it })
        compose.onNodeWithText("🕯️ 缅怀").performClick()
        assertEquals(EventType.MEMORIAL, type)
    }

    // ===================== Emoji 面板 =====================

    @Test
    fun `Emoji面板_点击选择按钮弹出全量选项`() {
        render()
        compose.onNodeWithText("选择 Emoji").performClick()
        compose.onNodeWithText("选择头像 Emoji").assertIsDisplayed()
        compose.onNodeWithText("🏷️ 自动").assertIsDisplayed()
        // 面板里能看到部分选项（如 🐶）
        compose.onNodeWithText("🐶").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Emoji面板_点选一个Emoji触发回调`() {
        var picked: String? = null
        render(onUpdateEmoji = { picked = it })
        compose.onNodeWithText("选择 Emoji").performClick()
        compose.onNodeWithText("🐶").performScrollTo().performClick()
        assertEquals("🐶", picked)
    }

    @Test
    fun `Emoji面板_点自动清空为自动头像`() {
        var picked: String? = "🐶"
        render(state = AddEditUiState(name = "小明", emoji = "🐶"), onUpdateEmoji = { picked = it })
        compose.onNodeWithText("选择 Emoji").performClick()
        compose.onNodeWithText("🏷️ 自动").performScrollTo().performClick()
        assertEquals("", picked)
    }

    @Test
    fun `Emoji_恢复自动入口与空状态文字`() {
        // 空 emoji 时显示「自动头像」，点击后回调空字符串
        var cleared: String? = "x"
        render(onUpdateEmoji = { cleared = it })
        compose.onNodeWithText("自动头像").performClick()
        assertEquals("", cleared)
    }
}
