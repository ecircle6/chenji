package com.birthapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
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
    outline = TextSecondary
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
    outline = TextOnDarkSecondary
)

/**
 * 当前是否处于深色。各屏幕（首页卡片、详情页配色）不再直接读系统深色，
 * 改读这个值，才能跟随「设置」里选择的「跟随系统/始终浅色/始终深色」。
 */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun BirthAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
