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
import androidx.compose.ui.res.stringResource
import com.birthapp.R

/** 底部导航的两个 tab 定义：路由 + 文案资源 id + 图标 */
private val TABS = listOf(
    Triple("home", R.string.nav_home, Icons.Filled.Home),
    Triple("calendar", R.string.nav_calendar, Icons.Filled.CalendarMonth)
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
        TABS.forEach { (route, labelRes, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onTabSelected(route) },
                icon = { Icon(imageVector = icon, contentDescription = null) },
                label = { Text(stringResource(labelRes)) }
            )
        }
    }
}