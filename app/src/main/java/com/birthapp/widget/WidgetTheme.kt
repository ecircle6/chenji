package com.birthapp.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
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

/**
 * 小组件调色板（白底列表）：小组件不跟随 App 内 Material 主题，
 * 日/夜两套颜色全部集中在这里，页面代码不再散落硬编码。
 *
 * 对比度约定（v2.1.17 保留）：强调色文字白天用 700 档
 * （珊瑚 #E05555 / 青 #009688，加粗 ≥14sp 大文本 3:1 达标），夜间提亮一档。
 * 品牌头「辰记」与倒计时高亮共用的 coral/teal/solemn 三色即 A 版三色
 * （今天珊瑚 / 正常青绿 / 缅怀灰蓝）的对比度修正版。
 */
internal object WidgetTheme {

    // ---- 基础面/文字 ----
    /** 组件底色：白（日）/ 深灰（夜） */
    val bg = ColorProvider(day = Color.White, night = SurfaceDark)
    /** 记录名字 */
    val name = ColorProvider(day = TextPrimary, night = TextOnDark)
    /** 次要文字（emoji / 空态提示） */
    val sub = ColorProvider(day = TextSecondary, night = TextOnDarkSecondary)

    // ---- 倒计时三色（A 版规则）：今天珊瑚 / 正常青绿 / 缅怀灰蓝 ----
    val coral = ColorProvider(day = Coral700, night = Coral400)
    val teal = ColorProvider(day = Teal700, night = Teal400)
    val solemn = ColorProvider(day = SlateInk, night = SlateInkLight)
}