package com.birthapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.birthapp.settings.ThemeMode
import com.birthapp.ui.add.AddEditScreen
import com.birthapp.ui.detail.DetailScreen
import com.birthapp.ui.home.HomeScreen
import com.birthapp.ui.settings.SettingsScreen
import com.birthapp.ui.theme.BirthAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // 强制应用使用中文环境，确保日历选择器等系统控件显示中文
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.SIMPLIFIED_CHINESE
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户授权或拒绝后的回调，这里静默处理 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 只在首次创建时请求权限。切换系统深色模式、旋转屏幕都会重建 Activity，
        // 此时 savedInstanceState 非空；若每次都请求，就会反复弹出电池优化设置页打扰用户
        if (savedInstanceState == null) {
            requestNotificationPermission()
            requestIgnoreBatteryOptimization()
        }

        setContent {
            // 读主题偏好：跟随系统时看系统深色，否则按用户选的强制浅/深
            val themeMode by (application as BirthApp).themeStore.mode
                .collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BirthAppTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 从小组件的加号进来时直接落到新增页
                    BirthAppNav(openAdd = intent?.action == ACTION_OPEN_ADD)
                }
            }
        }
    }

    companion object {
        /** 小组件加号的跳转标记。用 action 而不是 extra，否则两个 PendingIntent 会被系统当成同一个 */
        const val ACTION_OPEN_ADD = "com.birthapp.action.OPEN_ADD"

        /** 与 ThemeStore 共用同一份应用设置文件 */
        private const val PREFS_NAME = "birthapp_settings"
        /** 是否已经引导过电池优化豁免：只引导一次，之后不再自动弹出 */
        private const val KEY_ASKED_BATTERY_OPT = "asked_ignore_battery_opt"
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // 请求忽略电池优化，避免系统省电机制杀掉后台闹钟导致提醒不触发。
    // 这个引导一辈子只做一次：无论用户当时是否同意，之后都不再自动弹，
    // 否则每次打开 App 都会跳到系统的电池优化设置页，非常打扰
    private fun requestIgnoreBatteryOptimization() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_BATTERY_OPT, false)) return

        val powerManager = getSystemService(PowerManager::class.java)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                // 部分机型不支持该请求页面，静默忽略
            }
        }
        // 不论是否已豁免、是否成功弹出，都标记为「已问过」，下次不再自动弹
        prefs.edit().putBoolean(KEY_ASKED_BATTERY_OPT, true).apply()
    }
}

@Composable
fun BirthAppNav(openAdd: Boolean = false) {
    val navController = rememberNavController()

    // 入口仍然是首页，只是多跳一步到新增页；
    // 这样从小组件进来后按返回是回到列表，而不是直接退出 App
    LaunchedEffect(Unit) {
        if (openAdd) navController.navigate("add")
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onAddClick = { navController.navigate("add") },
                // 点卡片先进只读详情页，避免一不小心在表单里改到数据
                onItemClick = { id -> navController.navigate("detail/$id") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "detail/{birthdayId}",
            arguments = listOf(
                navArgument("birthdayId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val birthdayId = backStackEntry.arguments?.getLong("birthdayId") ?: 0L
            DetailScreen(
                birthdayId = birthdayId,
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate("edit/$id") }
            )
        }
        composable("add") {
            AddEditScreen(
                birthdayId = 0,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit/{birthdayId}",
            arguments = listOf(
                navArgument("birthdayId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val birthdayId = backStackEntry.arguments?.getLong("birthdayId") ?: 0L
            AddEditScreen(
                birthdayId = birthdayId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
