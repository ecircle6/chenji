package com.birthapp.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.birthapp.settings.ThemeMode
import com.birthapp.ui.theme.BirthAppTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 设置页 Compose UI 测试：渲染无状态的 SettingsContent，
 * 断言分组、主题单选回调、提醒开关状态与默认时间显示。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun render(
        uiState: SettingsUiState = SettingsUiState(versionName = "2.1.5"),
        callbacks: SettingsCallbacks = SettingsCallbacks(
            onBack = {},
            onThemeModeSelect = {},
            onToggleDynamicColor = {},
            onSetDefaultTime = { _, _ -> },
            onToggleReminders = {},
            onExportClick = {},
            onShareBackup = {},
            onImportClick = {},
            onApplyImport = { _, _ -> },
            onToast = {},
            onShareFile = {},
            onOpenSystemNotificationSettings = {}
        )
    ) {
        compose.setContent {
            BirthAppTheme {
                SettingsContent(uiState = uiState, events = emptyFlow(), callbacks = callbacks)
            }
        }
    }

    @Test
    fun `设置_四个分组与版本号显示`() {
        render()
        compose.onNodeWithText("深夜模式").assertIsDisplayed()
        compose.onNodeWithText("通知").assertIsDisplayed()
        // 数据备份 / 关于在滚动区域下方，先滚到可见再断言
        compose.onNodeWithText("数据备份").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("关于").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("当前版本 v2.1.5 · 查看每次更新了什么").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `深色模式_点击始终深色触发回调`() {
        var selected: ThemeMode? = null
        render(callbacks = previewCallbacks(onThemeModeSelect = { selected = it }))
        compose.onNodeWithText("始终深色").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    @Test
    fun `提醒开关_默认开启_点击触发回调`() {
        var toggled: Boolean? = null
        render(callbacks = previewCallbacks(onToggleReminders = { toggled = it }))
        compose.onNodeWithTag("switch_reminders").assertIsOn()
        compose.onNodeWithTag("switch_reminders").performClick()
        assertEquals(false, toggled)
    }

    @Test
    fun `默认提醒时间_按状态显示`() {
        render(uiState = SettingsUiState(defaultHour = 7, defaultMinute = 30, versionName = "2.1.5"))
        compose.onNodeWithText("07:30").assertIsDisplayed()
    }

    private fun previewCallbacks(
        onThemeModeSelect: (ThemeMode) -> Unit = {},
        onToggleReminders: (Boolean) -> Unit = {}
    ) = SettingsCallbacks(
        onBack = {},
        onThemeModeSelect = onThemeModeSelect,
        onToggleDynamicColor = {},
        onSetDefaultTime = { _, _ -> },
        onToggleReminders = onToggleReminders,
        onExportClick = {},
        onShareBackup = {},
        onImportClick = {},
        onApplyImport = { _, _ -> },
        onToast = {},
        onShareFile = {},
        onOpenSystemNotificationSettings = {}
    )
}
