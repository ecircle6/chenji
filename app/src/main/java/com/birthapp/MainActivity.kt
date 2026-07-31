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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.birthapp.ui.add.AddEditScreen
import com.birthapp.ui.home.HomeScreen
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
        requestNotificationPermission()
        requestIgnoreBatteryOptimization()

        setContent {
            BirthAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BirthAppNav()
                }
            }
        }
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

    // 请求忽略电池优化，避免系统省电机制杀掉后台闹钟导致提醒不触发
    private fun requestIgnoreBatteryOptimization() {
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
    }
}

@Composable
fun BirthAppNav() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onAddClick = { navController.navigate("add") },
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
