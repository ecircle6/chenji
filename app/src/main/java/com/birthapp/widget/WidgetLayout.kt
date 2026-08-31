package com.birthapp.widget

/**
 * 小组件布局纯函数：档位分流与行数换算。
 *
 * 真实尺寸由来（见 [BirthWidget.realWidgetSize]）：SizeMode.Single 下 LocalSize
 * 恒为 manifest 静态 fallback（110×110dp），真实宽高从系统 options 读
 * （OPTION_APPWIDGET_MIN_WIDTH × MIN_HEIGHT，放置/resize 时写入）。
 * 这里的函数只做「拿到真实尺寸之后」的数学换算，不碰系统读值，
 * 以便纯 JUnit 锁验收尺寸：4×2(高≈135dp)→3 行、4×4(≈281dp)→6 行。
 *
 * 行数语义（还原「版本 A」显示形态）：
 * - 目标行数固定——宽档 3 行、大档 6 行（A 版行为，不是随高度自适应）；
 * - 高度护栏——行框由 defaultWeight 均分，均分后行高低于 [MIN_ROW_HEIGHT]
 *   就降行数，防止文字在真实尺寸过矮时被裁切（4×1 等高仅可放的场景）。
 */
internal object WidgetLayout {

    // ---- 档位阈值（dp）----
    /** 宽度低于该值走 2×2 紧凑大字（2 格 ≈ 110~140dp） */
    const val COMPACT_MAX_WIDTH = 200
    /** 高度达到该值走大档 6 行（4 行及以上，与竖屏 4×4 ≈281dp 对齐） */
    const val LARGE_MIN_HEIGHT = 210

    // ---- 列表 chrome（dp）：外边距×2 + 品牌头行 + 头行间距 ----
    const val OUTER_PADDING = 12
    const val HEADER_ROW_HEIGHT = 20
    const val HEADER_GAP = 4

    // ---- 行数 ----
    /** 宽档目标行数（4×2 / 4×3） */
    const val WIDE_TARGET_ROWS = 3
    /** 大档目标行数（≥4×4，与取数上限 MAX_ITEMS 一致） */
    const val LARGE_TARGET_ROWS = 6
    /** 行数上限：桌面拉多高都不超过 6 行 */
    const val MAX_ROWS = 6
    /** 单行最低高度：14sp 文字一行约 19dp，24dp 留出垂直余量 */
    const val MIN_ROW_HEIGHT = 24

    enum class Tier { COMPACT, WIDE, LARGE }

    /** 档位：先按宽（2 格内必紧凑），宽后再按高分宽档/大档 */
    fun tierOf(widthDp: Int, heightDp: Int): Tier = when {
        widthDp < COMPACT_MAX_WIDTH -> Tier.COMPACT
        heightDp >= LARGE_MIN_HEIGHT -> Tier.LARGE
        else -> Tier.WIDE
    }

    /** 目标行数：宽档 3 行、大档 6 行、紧凑档 1 行 */
    fun targetRows(tier: Tier): Int = when (tier) {
        Tier.COMPACT -> 1
        Tier.WIDE -> WIDE_TARGET_ROWS
        Tier.LARGE -> LARGE_TARGET_ROWS
    }

    /** 列表可用高度（组件高 - 外边距×2 - 品牌头行 - 头行间距） */
    fun listHeight(heightDp: Int): Int =
        heightDp - OUTER_PADDING * 2 - HEADER_ROW_HEIGHT - HEADER_GAP

    /**
     * 实际渲染行数：目标行数与高度护栏的交集。
     * 可用高度均分给目标行数后行高不足 [MIN_ROW_HEIGHT] 时降行数
     * （4×1 等高只有 ~62dp 可放两行），钳到 1..MAX_ROWS。
     */
    fun maxRowsFor(heightDp: Int, tier: Tier): Int = when (tier) {
        Tier.COMPACT -> 1
        else -> (listHeight(heightDp) / MIN_ROW_HEIGHT)
            .coerceIn(1, minOf(targetRows(tier), MAX_ROWS))
    }
}