package com.birthapp.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import com.birthapp.R
import com.birthapp.data.EventType
import com.birthapp.ui.theme.Coral400
import com.birthapp.ui.theme.Coral700
import com.birthapp.ui.theme.SlateInk
import com.birthapp.ui.theme.SlateInkLight
import com.birthapp.ui.theme.SurfaceDark
import com.birthapp.ui.theme.Teal400
import com.birthapp.ui.theme.Teal700
import com.birthapp.ui.theme.TextOnDark
import com.birthapp.ui.theme.TextOnDarkSecondary
import com.birthapp.ui.theme.TextPrimary
import com.birthapp.ui.theme.TextSecondary
import com.birthapp.ui.theme.Violet300
import com.birthapp.ui.theme.Violet700

/**
 * 小组件调色板（纸笺辰刻）：小组件不跟随 App 内 Material 主题，
 * 日/夜两套颜色全部集中在这里，页面代码不再散落硬编码。
 *
 * 对比度约定（v2.1.17）：
 * - 强调色文字白天用 700 档（珊瑚 #E05555 3.7:1 / 青 #009688 3.7:1 / 紫 #5B4BC4 6.5:1），
 *   夜间提亮一档（400/300 档 6~7:1）；均满足加粗 ≥14sp 大文本 3:1 的底线
 * - 类型 wash 头像底：白天 10%（0x1A）、夜间 18%（0x2E）——夜间低透明度 wash 在深底上几乎不可见
 * - Glance 的 DayNight ColorProvider 不支持 copy(alpha)，wash 用预计算 ARGB 固定色
 */
internal object WidgetTheme {

    // ---- 基础面/文字 ----
    val bg = ColorProvider(day = Color.White, night = SurfaceDark)
    val rowBg = ColorProvider(day = Color.White, night = Color(0xFF242422))
    val name = ColorProvider(day = TextPrimary, night = TextOnDark)
    val sub = ColorProvider(day = TextSecondary, night = TextOnDarkSecondary)
    val white = ColorProvider(day = Color.White, night = Color.White)
    val white90 = ColorProvider(day = Color.White.copy(alpha = 0.9f), night = Color.White.copy(alpha = 0.9f))
    val white85 = ColorProvider(day = Color.White.copy(alpha = 0.85f), night = Color.White.copy(alpha = 0.85f))

    // ---- 类型强调（文字/数字/色条用）----
    val coral = ColorProvider(day = Coral700, night = Coral400)
    val violet = ColorProvider(day = Violet700, night = Violet300)
    val teal = ColorProvider(day = Teal700, night = Teal400)
    val solemn = ColorProvider(day = SlateInk, night = SlateInkLight)

    // ---- 类型强调（实色，头像字用；同上日夜分流）----
    private val coralSolid = ColorProvider(day = Coral700, night = Coral400)
    private val violetSolid = ColorProvider(day = Violet700, night = Violet300)
    private val tealSolid = ColorProvider(day = Teal700, night = Teal400)
    private val solemnSolid = ColorProvider(day = SlateInk, night = SlateInkLight)

    // ---- 类型 wash（头像/空态圆底用）：日 10%，夜 18% ----
    private val coralWash = ColorProvider(
        day = Color(0x1AE05555),
        night = Color(0x2EFF8A8A)
    )
    private val violetWash = ColorProvider(
        day = Color(0x1A5B4BC4),
        night = Color(0x2EB5ACFF)
    )
    private val tealWash = ColorProvider(
        day = Color(0x1A009688),
        night = Color(0x2E26D9C0)
    )
    private val solemnWash = ColorProvider(
        day = Color(0x1A5B6B7A),
        night = Color(0x2E9FB0BF)
    )

    /** 行强调色（色条/数字/急标签） */
    fun accent(eventType: String, isSolemn: Boolean): androidx.glance.unit.ColorProvider = when {
        isSolemn -> solemn
        eventType == EventType.LOVE || eventType == EventType.MARRIAGE -> violet
        eventType == EventType.OTHER -> teal
        else -> coral
    }

    /** 头像字实色（比 accent 夜间再稳一点的同一套，预留独立调档位） */
    fun accentSolid(eventType: String, isSolemn: Boolean): androidx.glance.unit.ColorProvider = when {
        isSolemn -> solemnSolid
        eventType == EventType.LOVE || eventType == EventType.MARRIAGE -> violetSolid
        eventType == EventType.OTHER -> tealSolid
        else -> coralSolid
    }

    /** 头像/圆底 wash（日夜透明度分流） */
    fun wash(eventType: String): androidx.glance.unit.ColorProvider = when (eventType) {
        EventType.LOVE, EventType.MARRIAGE -> violetWash
        EventType.OTHER -> tealWash
        EventType.MEMORIAL -> solemnWash
        else -> coralWash
    }

    /**
     * 4×4 Hero 渐变 drawable：单资源 id，日夜由 drawable-night 限定符自动切换
     * （Glance 1.1 的 ImageProvider 没有 day/night 双 res 构造，走标准资源限定符）。
     * 色值与 App 内 HeroCard 同源（ui/theme/Color.kt 的 Hero 系列）。
     */
    fun heroImage(eventType: String): androidx.glance.ImageProvider = when {
        eventType == EventType.LOVE || eventType == EventType.MARRIAGE -> androidx.glance.ImageProvider(
            R.drawable.widget_hero_love
        )
        eventType == EventType.MEMORIAL -> androidx.glance.ImageProvider(
            R.drawable.widget_hero_memorial
        )
        eventType == EventType.OTHER -> androidx.glance.ImageProvider(
            R.drawable.widget_hero_other
        )
        else -> androidx.glance.ImageProvider(
            R.drawable.widget_hero_birthday
        )
    }
}
