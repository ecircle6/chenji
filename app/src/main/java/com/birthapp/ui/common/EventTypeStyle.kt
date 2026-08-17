package com.birthapp.ui.common

import androidx.compose.ui.graphics.Color
import com.birthapp.data.EventType
import com.birthapp.ui.theme.*

/**
 * 事件类型到配色的映射。
 * 放在 ui 层而不是 EventType 里，是为了让 data 层不依赖 Compose。
 */

/** 类型强调色：头像圈、选择器选中态、庄重类型的倒计时数字都用它 */
fun eventAccent(type: String): Color = when (type) {
    EventType.BIRTHDAY -> Coral500        // 品牌主色
    EventType.LOVE, EventType.MARRIAGE -> Violet500  // 情侣/结婚同紫，与 HeroLove 渐变一致
    EventType.BABY -> Teal500
    EventType.MEMORIAL -> SlateInk
    EventType.OTHER -> SunnyYellow700
    else -> Coral500                      // 兜底：未知类型用品牌主色
}

/** 当天横幅的（底色, 文字色）。缅怀用灰蓝，其余按类型给暖色 */
fun eventBannerColors(type: String, darkTheme: Boolean): Pair<Color, Color> = when (type) {
    EventType.MEMORIAL -> SlateInk.copy(alpha = if (darkTheme) 0.24f else 0.13f) to
            if (darkTheme) SlateInkLight else SlateInk

    EventType.MARRIAGE -> Violet500.copy(alpha = if (darkTheme) 0.22f else 0.16f) to
            if (darkTheme) Violet300 else Violet700

    EventType.BABY -> Teal500.copy(alpha = if (darkTheme) 0.22f else 0.16f) to
            if (darkTheme) Teal300 else Teal700

    EventType.LOVE -> Violet500.copy(alpha = if (darkTheme) 0.22f else 0.16f) to
            if (darkTheme) Violet300 else Violet700

    // 生日和其他纪念沿用原来的暖黄高亮
    else -> SunnyYellow.copy(alpha = if (darkTheme) 0.18f else 0.3f) to
            if (darkTheme) Coral300 else Coral700
}
