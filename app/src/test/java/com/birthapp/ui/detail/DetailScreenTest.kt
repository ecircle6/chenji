package com.birthapp.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.birthapp.ui.preview.previewDetailState
import com.birthapp.ui.theme.BirthAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 详情页 Compose UI 测试：渲染无状态的 DetailContent，断言关键信息与回调触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(
        state: DetailUiState = previewDetailState(),
        onBack: () -> Unit = {},
        onEditClick: (Long) -> Unit = {},
        onTogglePinned: () -> Unit = {},
        onShare: () -> Unit = {},
        onDelete: () -> Unit = {},
        onToggleActive: (Boolean) -> Unit = {}
    ) {
        compose.setContent {
            BirthAppTheme {
                DetailContent(
                    state = state,
                    onBack = onBack,
                    onEditClick = onEditClick,
                    onTogglePinned = onTogglePinned,
                    onShare = onShare,
                    onDelete = onDelete,
                    onToggleActive = onToggleActive
                )
            }
        }
    }

    @Test
    fun `详情_显示名字日期与倒计时`() {
        render()
        compose.onNodeWithText("小明").assertIsDisplayed()
        // 日期卡片在滚动区域下方，先滚到可见再断言
        compose.onNodeWithText("1998年8月14日 · 阳历").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("364").assertIsDisplayed()
        // 「 天后」带前导空格，需 substring 匹配
        compose.onNodeWithText("天后", substring = true).assertExists()
    }

    @Test
    fun `详情_置顶开关触发回调`() {
        var pinnedCalls = 0
        render(onTogglePinned = { pinnedCalls++ })
        // 预览数据 isPinned = true → 置顶态，contentDescription 是「取消置顶」
        compose.onNodeWithContentDescription("取消置顶").performClick()
        assertEquals(1, pinnedCalls)
    }

    @Test
    fun `详情_分享按钮触发回调`() {
        var shareCalls = 0
        render(onShare = { shareCalls++ })
        compose.onNodeWithContentDescription("分享卡片").performClick()
        assertEquals(1, shareCalls)
    }

    @Test
    fun `详情_编辑按钮触发回调`() {
        var editId = -1L
        render(onEditClick = { editId = it })
        compose.onNodeWithContentDescription("编辑").performClick()
        assertEquals(1L, editId)
    }

    @Test
    fun `详情_删除需二次确认后触发回调`() {
        var deleteCalls = 0
        render(onDelete = { deleteCalls++ })
        compose.onNodeWithContentDescription("删除").performClick()
        // 弹窗确认
        compose.onNodeWithText("删除记录").assertIsDisplayed()
        compose.onNodeWithText("删除").performClick()
        assertEquals(1, deleteCalls)
    }

    @Test
    fun `详情_记录不存在_显示提示`() {
        render(state = DetailUiState(notFound = true))
        compose.onNodeWithText("这条记录已经不存在了").assertIsDisplayed()
    }

    @Test
    fun `详情_提醒开关触发回调`() {
        var activeValue: Boolean? = null
        render(onToggleActive = { activeValue = it })
        compose.onNodeWithText("提醒已开启").assertIsDisplayed()
        // Switch 无文字，用 isToggleable 语义定位（详情页只有一个开关）
        compose.onNode(isToggleable()).performClick()
        assertEquals(false, activeValue)
    }
}
