package com.birthapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 排版层级约定（material-3 audit 收敛）：
 * - 倒计时数字统一走 display 系（等宽 tabular-nums，宽度不随数字变化抖动）
 * - 卡片姓名 titleMedium(600)，比数字低一档字重，让数字成为唯一焦点
 * - 辅助信息 bodyMedium + onSurfaceVariant（浅色 4.6:1 达标）
 * - 时钟/标签 chip 用 labelSmall（胶囊内小字）
 */
val AppTypography = Typography(
    // 倒计时最大数字：Hero 聚焦卡
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 56.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
        fontFeatureSettings = "tnum"
    ),
    // 倒计时大数字：Hero 卡、详情页（44sp 一档）
    displayMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    // 倒计时数字：首页列表卡（34sp 一档）
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    // 卡片姓名：从 800 降一档到 600，与 display 数字拉开层级
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // 时钟 chip、角标等最小文字
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

/**
 * 列表紧凑卡的倒计时数字（26sp 一档，displaySmall 装不下）。
 * 独立于 Typography 的扩展 token：同样启用 tabular-nums 等宽。
 */
val CountdownCompact = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 26.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.25).sp,
    fontFeatureSettings = "tnum"
)
