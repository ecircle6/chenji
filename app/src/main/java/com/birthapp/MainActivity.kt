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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.settings.ThemeMode
import com.birthapp.ui.add.AddEditScreen
import com.birthapp.ui.detail.DetailScreen
import com.birthapp.ui.home.HomeScreen
import com.birthapp.ui.settings.SettingsScreen
import com.birthapp.ui.theme.BirthAppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // 小组件加号的跳转请求。用计数而不是布尔值：singleTask 下 App 活着时
    // 再点加号走的是 onNewIntent，每次 +1 都能触发一次新的跳转
    private val openAddRequests = mutableIntStateOf(0)

    // 通知点击的详情跳转请求。存记录 id 而不是布尔值：App 活着时点通知走
    // onNewIntent，同一条通知被点两次需要能再次跳转，所以导航完成后要清空
    private val pendingDetailId = mutableStateOf<Long?>(null)

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
        // targetSdk 35 下 Android 15 强制 edge-to-edge：内容延伸到状态栏/导航栏后面，
        // 系统按浅/深色自动绘状态栏背景，App 内不再手动涂色
        enableEdgeToEdge()
        // 只在首次创建时请求权限。切换系统深色模式、旋转屏幕都会重建 Activity，
        // 此时 savedInstanceState 非空；若每次都请求，就会反复弹出电池优化设置页打扰用户
        if (savedInstanceState == null) {
            requestNotificationPermission()
            requestIgnoreBatteryOptimization()
            // 冷启动就带着加号 action 进来（App 没活着时点小组件加号）。
            // 旋转屏幕、切深色模式重建时 savedInstanceState 非空，不会重复跳
            if (intent?.action == ACTION_OPEN_ADD) openAddRequests.intValue++
            // 冷启动点通知进来：同样只在首次创建时处理，避免重建后重复跳详情
            handleDetailIntent(intent)
        }

        setContent {
            // 读主题偏好：跟随系统时看系统深色，否则按用户选的强制浅/深
            val themeMode by (application as BirthApp).themeStore.mode
                .collectAsStateWithLifecycle()
            val dynamicColor by (application as BirthApp).themeStore.dynamicColor
                .collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            BirthAppTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 从小组件的加号进来时直接落到新增页；点通知进来时直达详情页
                    BirthAppNav(
                        openAddRequest = openAddRequests.intValue,
                        pendingDetailId = pendingDetailId.value,
                        onDetailHandled = { pendingDetailId.value = null }
                    )
                }
            }
        }
    }

    // singleTask 下 App 已在运行时，点小组件不会重建界面而是走这里；
    // 不处理的话，App 活着时点加号就只是把界面唤回来，不会跳到新增页
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_ADD) openAddRequests.intValue++
        handleDetailIntent(intent)
    }

    /**
     * 读取通知点击的 extra，记录要直达的详情页 id。
     * 通知点击与小组件加号共用 onNewIntent 通道，互不冲突：
     * 通知带 EXTRA_BIRTHDAY_ID（可正可负），加号只带 action
     */
    private fun handleDetailIntent(intent: Intent?) {
        val id = intent?.getLongExtra(AlarmScheduler.EXTRA_BIRTHDAY_ID, -1L) ?: -1L
        if (id > 0) pendingDetailId.value = id
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
fun BirthAppNav(
    openAddRequest: Int = 0,
    pendingDetailId: Long? = null,
    onDetailHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    // 入口仍然是首页，只是多跳一步到新增页；
    // 这样从小组件进来后按返回是回到列表，而不是直接退出 App。
    // launchSingleTop：已经停在新增页时再点加号不会叠第二层
    LaunchedEffect(openAddRequest) {
        if (openAddRequest > 0) {
            navController.navigate("add") { launchSingleTop = true }
        }
    }

    // 通知点击直达详情页。导航后立刻清空状态，保证同一条通知再点一次还能跳；
    // 记录已被删除时 DetailScreen 会自动返回首页，这里不用额外兜底
    LaunchedEffect(pendingDetailId) {
        val id = pendingDetailId ?: return@LaunchedEffect
        navController.navigate("detail/$id") { launchSingleTop = true }
        onDetailHandled()
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
