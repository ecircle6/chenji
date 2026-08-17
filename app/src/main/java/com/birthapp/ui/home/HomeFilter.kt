package com.birthapp.ui.home

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.util.ZodiacUtils

/**
 * 首页筛选状态：关系 / 类型 / 生肖三个维度，单一不可变对象。
 * 三维组合语义：组内互斥、组间叠加（家人 ∩ 生日 ∩ 属虎）。
 * 日后要加新维度（如地区）只需加一个字段，HomeViewModel/UI/测试同步小改。
 */
data class FilterState(
    val relation: String = "all",   // "all" | family / friend / colleague / other
    val type: String = "all",       // "all" | EventType 常量
    val zodiac: String? = null      // null = 不限 | 生肖中文名（鼠/牛/…/猪）
) {
    /** 是否有任一维度处于非默认筛选（用于空态判断：筛选为空 ≠ 数据为空） */
    val isActive: Boolean
        get() = relation != "all" || type != "all" || zodiac != null
}

/**
 * 首页列表的筛选规则，抽成纯函数方便单元测试。
 *
 * 维度优先级：
 * - 搜索关键词非空时是全局查找，三个筛选维度都不叠加——
 *   用户搜名字时不该因为停在某个标签上而搜不到人
 * - 平时关系 / 类型 / 生肖是"与"的关系，可以叠加（家人 + 生日 + 属虎）
 */
object HomeFilter {

    /** 类型胶囊的展示顺序。老数据里可能还有结婚/宝宝类型，插在对应位置 */
    private val TYPE_ORDER = listOf(
        EventType.BIRTHDAY, EventType.LOVE, EventType.MARRIAGE,
        EventType.BABY, EventType.MEMORIAL, EventType.OTHER
    )

    /**
     * 当前记录里实际出现过的类型，按固定顺序排。
     * 不认识的类型不进胶囊（只能在"全部"里看到），避免出现文案错乱的按钮。
     */
    fun availableTypes(list: List<Birthday>): List<String> {
        val present = list.mapTo(HashSet()) { it.eventType }
        return TYPE_ORDER.filter { it in present }
    }

    fun apply(
        list: List<Birthday>,
        filter: FilterState,
        keyword: String
    ): List<Birthday> {
        val kw = keyword.trim()
        if (kw.isNotEmpty()) {
            return list.filter {
                it.name.contains(kw, ignoreCase = true) ||
                        it.notes.contains(kw, ignoreCase = true)
            }
        }
        return list
            .filter { filter.relation == "all" || it.relation == filter.relation }
            .filter { filter.type == "all" || it.eventType == filter.type }
            .filter { filter.zodiac == null || ZodiacUtils.getZodiacName(it.birthYear) == filter.zodiac }
    }
}