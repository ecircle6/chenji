package com.birthapp.ui.home

import com.birthapp.data.EventType
import com.birthapp.util.EventCalc
import java.time.LocalDate

/**
 * 首页卡片三层化与月份分组的纯函数。
 *
 * 倒计时分层：紧急 0-7 天（大卡+进度条）/ 标准 8-30 天（紧凑卡） / 远景 >30 天（迷你行+月份分隔）。
 * 这些函数不持有状态、不依赖 ViewModel，全部可单测。
 */
enum class CardTier { URGENT, NORMAL, DISTANT }

/** 首页列表的异构行：月份标题或卡片 */
sealed class HomeListItem {
    /**
     * 月份分组标题。yearMonth 是分组的稳定唯一标识（年×100+月），
     * label 只是展示文案：跨年时所有月份都显示「YYYY 年」，
     * 但 LazyColumn 的 key 必须用 yearMonth，否则同一年不同月份的
     * 两个分组会因 label 相同而 key 碰撞、组合期直接崩溃
     */
    data class MonthHeader(val label: String, val yearMonth: Int) : HomeListItem()
    data class Card(val display: BirthdayDisplay, val tier: CardTier) : HomeListItem()
}

object HomeTier {

    // ===================== 分层 =====================

    /** 按倒计时分层：0-7 紧急 / 8-30 标准 / >30 远景 */
    fun tierOf(countdown: Int): CardTier = when {
        countdown <= 7 -> CardTier.URGENT
        countdown <= 30 -> CardTier.NORMAL
        else -> CardTier.DISTANT
    }

    // ===================== 紧急进度条 =====================

    /** 进度条比例：已过天数 / 7，today = 100%，7 天前 = 0% */
    fun progressOf(countdown: Int): Float = (7 - countdown).coerceIn(0, 7) / 7f

    /** 进入紧急窗口后已过天数 */
    fun elapsedDays(countdown: Int): Int = (7 - countdown).coerceIn(0, 7)

    // ===================== 月份标签 =====================

    /** 远景行月份分隔标题：同年显示「X 月」，跨年显示「YYYY 年」 */
    fun monthLabel(eventYear: Int, eventMonth: Int, today: LocalDate): String =
        if (eventYear == today.year) "$eventMonth 月" else "$eventYear 年"

    // ===================== 构建异构列表 =====================

    /**
     * 从排序后的 [BirthdayDisplay] 构建首页 LazyColumn 异构行列表。
     *
     * 排序前提：输入已按「置顶→暂停→倒计时」排好（由 HomeViewModel 保证）。
     * 行顺序：置顶卡 → 紧急卡(0-7) → 标准卡(8-30) → 月份分隔+远景行(>30) → 暂停卡(灰显)
     */
    fun buildRows(list: List<BirthdayDisplay>): List<HomeListItem> {
        val today = LocalDate.now()
        val pinned = list.filter { it.isPinned }
        val active = list.filter { !it.isPinned && !it.isPaused }
        val paused = list.filter { it.isPaused && !it.isPinned }

        val result = mutableListOf<HomeListItem>()

        // 1. 置顶卡（统一按标准卡渲染，不看 countdown 层级）
        pinned.forEach { result.add(HomeListItem.Card(it, CardTier.NORMAL)) }

        // 2. 活动卡分层（active 已按 countdown 升序）
        val urgent = active.filter { tierOf(it.countdown) == CardTier.URGENT }
        val normal = active.filter { tierOf(it.countdown) == CardTier.NORMAL }
        val distant = active.filter { tierOf(it.countdown) == CardTier.DISTANT }

        urgent.forEach { result.add(HomeListItem.Card(it, CardTier.URGENT)) }
        normal.forEach { result.add(HomeListItem.Card(it, CardTier.NORMAL)) }

        // 远景：按（年,月）连续分组，同组只插一个月份标题
        var lastYearMonth = -1
        distant.forEach { display ->
            val key = display.nextEventYear * 100 + display.nextEventMonth
            if (key != lastYearMonth) {
                result.add(
                    HomeListItem.MonthHeader(
                        label = monthLabel(display.nextEventYear, display.nextEventMonth, today),
                        yearMonth = key
                    )
                )
                lastYearMonth = key
            }
            result.add(HomeListItem.Card(display, CardTier.DISTANT))
        }

        // 3. 暂停卡（灰显标准卡，沉底）
        paused.forEach { result.add(HomeListItem.Card(it, CardTier.NORMAL)) }

        return result
    }
}
