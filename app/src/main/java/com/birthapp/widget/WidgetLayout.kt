package com.birthapp.widget

/**
 * 小组件布局纯函数：档位分流与行数换算。
 *
 * 真实尺寸由来（见 [BirthWidget.realWidgetSize]）：SizeMode.Single 下 LocalSize
 * 恒为 manifest 静态 fallback（110×110dp），真实宽高从系统 options 读
 * （OPTION_APPWIDGET_MIN_WIDTH × MIN_HEIGHT，放置/resize 时写入）。
 * 这里的函数只做「拿到真实尺寸之后」的数学换算，不碰系统读值，
 * 以便纯 JUnit 锁验收尺寸：4×2(高≈135dp)→2 行、4×3(≈208dp)→3 行、
 * ≥4×4(≥281dp)→Hero 大档 3 行；行数上限 5。
 *
 * 行数公式：n 行需 n×ROW_HEIGHT + (n-1)×ROW_GAP ≤ 可用高，
 * 反解 n ≤ (可用高+ROW_GAP)/(ROW_HEIGHT+ROW_GAP) 取下整，
 * 桌面空间不够 1 行时退 1 行（行框 defaultWeight 均分后内容居中）。
 */
internal object WidgetLayout {

    // ---- 档位阈值（dp）----
    /** 宽度低于该值走 2×2 紧凑单焦（2 格 ≈ 110~140dp） */
    const val COMPACT_MAX_WIDTH = 200
    /** 高度达到该值走 Hero 大档（4 行及以上，与竖屏 4×4 ≈281dp 对齐） */
    const val LARGE_MIN_HEIGHT = 210

    // ---- 行高/行距（dp）----
    const val ROW_HEIGHT = 44
    const val ROW_GAP = 4
    /** 行数上限：桌面拉多高都不超过 5 行 */
    const val MAX_ROWS = 5

    // ---- 宽档 chrome（dp）：外边距×2 + 问候行 + 头行距 ----
    const val WIDE_OUTER_PADDING = 6
    const val GREETING_ROW_HEIGHT = 20
    const val GREETING_GAP = 4

    // ---- 大档 chrome（dp）：外边距×2 + Hero + 头行「其他近期」+ 两处间距 ----
    const val LARGE_OUTER_PADDING = 14
    const val HERO_HEADER_HEIGHT = 70
    const val LARGE_HEADER_GAP = 6
    const val LARGE_TITLE_ROW = 14
    const val LARGE_LIST_GAP = 4

    enum class Tier { COMPACT, WIDE, LARGE }

    /** 档位：先按宽（2 格内必紧凑），宽后再按高分宽档/大档 */
    fun tierOf(widthDp: Int, heightDp: Int): Tier = when {
        widthDp < COMPACT_MAX_WIDTH -> Tier.COMPACT
        heightDp >= LARGE_MIN_HEIGHT -> Tier.LARGE
        else -> Tier.WIDE
    }

    /** 可用高度能放几行：n 行需 n×ROW_HEIGHT+(n-1)×ROW_GAP，钳到 1..MAX_ROWS */
    fun rowsFor(availableHeightDp: Int): Int =
        ((availableHeightDp + ROW_GAP) / (ROW_HEIGHT + ROW_GAP)).coerceIn(1, MAX_ROWS)

    /** 宽档留给纸笺行的可用高度（组件高 - 外边距 - 问候行 - 间距） */
    fun wideListHeight(heightDp: Int): Int =
        heightDp - WIDE_OUTER_PADDING * 2 - GREETING_ROW_HEIGHT - GREETING_GAP

    /** 宽档行数（按组件真实高度换算，含 chrome 扣减） */
    fun wideRows(heightDp: Int): Int = rowsFor(wideListHeight(heightDp))

    /** 大档留给列表行的可用高度（组件高 - 外边距 - Hero - 头行 - 两处间距） */
    fun largeListHeight(heightDp: Int): Int =
        heightDp - LARGE_OUTER_PADDING * 2 - HERO_HEADER_HEIGHT - LARGE_HEADER_GAP -
            LARGE_TITLE_ROW - LARGE_LIST_GAP

    /** 大档行数（按组件真实高度换算，含 chrome 扣减） */
    fun largeRows(heightDp: Int): Int = rowsFor(largeListHeight(heightDp))

    /**
     * 行框被压到 52dp 以下时行内容放不下 50dp 常规行（36dp 头像 + 内边距），
     * 需要切紧凑变体（32dp 头像 + 更小内边距/字号）防溢出裁切。
     */
    fun rowNeedsDense(listHeightDp: Int, rows: Int): Boolean =
        rows > 1 && listHeightDp / rows < 52
}