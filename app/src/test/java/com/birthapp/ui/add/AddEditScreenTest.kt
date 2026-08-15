package com.birthapp.ui.add

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("3天").performClick()
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
}
