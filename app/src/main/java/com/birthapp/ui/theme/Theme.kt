package com.birthapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Coral500,
    onPrimary = SurfaceLight,
    primaryContainer = Coral300,
    onPrimaryContainer = TextPrimary,
    secondary = Teal500,
    onSecondary = SurfaceLight,
    secondaryContainer = Teal300,
    onSecondaryContainer = TextPrimary,
    tertiary = SunnyYellow,
    onTertiary = TextPrimary,
    tertiaryContainer = SunnyYellow300,
    onTertiaryContainer = TextPrimary,
    background = WarmLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = CardPeach,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = ContainerLowestLight,
    surfaceContainerLow = ContainerLowLight,
    surfaceContainer = ContainerLight,
    surfaceContainerHigh = ContainerHighLight,
    surfaceContainerHighest = ContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Coral400,
    onPrimary = WarmDark,
    primaryContainer = Coral700,
    onPrimaryContainer = TextOnDark,
    secondary = Teal400,
    onSecondary = WarmDark,
    secondaryContainer = Teal700,
    onSecondaryContainer = TextOnDark,
    tertiary = SunnyYellow300,
    onTertiary = WarmDark,
    tertiaryContainer = SunnyYellow700,
    onTertiaryContainer = TextOnDark,
    background = WarmDark,
    onBackground = TextOnDark,
    surface = SurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = CardPeachDark,
    onSurfaceVariant = TextOnDarkSecondary,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = ContainerLowestDark,
    surfaceContainerLow = ContainerLowDark,
    surfaceContainer = ContainerDark,
    surfaceContainerHigh = ContainerHighDark,
    surfaceContainerHighest = ContainerHighestDark
)

/**
 * 形状语义（4 档 + 胶囊），全局圆角只允许走这里，不再写魔法数字：
 * - extraLarge 28：Hero 卡、底部弹层
 * - large 16：列表卡、信息分组
 * - medium 12：卡内嵌块、菜单
 * - small 8：输入框、提示条
 * - 胶囊（full）直接用 RoundedCornerShape(50)/CircleShape
 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** 底部弹层/抽屉只圆上角的形状（M3 用 extraLarge 档） */
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * 当前是否处于深色。各屏幕（首页卡片、详情页配色）不再直接读系统深色，
 * 改读这个值，才能跟随「设置」里选择的「跟随系统/始终浅色/始终深色」。
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun BirthAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // 动态取色（Material You）：Android 12+ 且设置里开启时，用系统从壁纸
    // 提取的配色；否则用品牌固定色板（Coral/Teal）
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // edge-to-edge 下状态栏背景由系统绘制（透明），这里只控制图标明暗
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
