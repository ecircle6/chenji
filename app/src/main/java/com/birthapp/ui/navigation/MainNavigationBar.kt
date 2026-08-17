package com.birthapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航的两个 tab 定义：路由 + 文案 + 图标 */
private val TABS = listOf(
    Triple("home", "首页", Icons.Filled.Home),
    Triple("calendar", "日历", Icons.Filled.CalendarMonth)
)

/**
 * 首页/日历双 tab 底部导航。
 * 作为 HomeScreen 与 CalendarScreenPage 各自 Scaffold 的 bottomBar 参数传入
 * （不额外包一层 Scaffold，避免 contentWindowInsets 双重 padding），
 * detail/add/edit/settings 不传则全屏压栈不变。
 */
@Composable
fun MainNavigationBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar {
        TABS.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onTabSelected(route) },
                icon = { Icon(imageVector = icon, contentDescription = null) },
                label = { Text(label) }
            )
        }
    }
}