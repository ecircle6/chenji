package com.birthapp.ui.home

import com.birthapp.data.Birthday
import com.birthapp.data.EventType

/**
 * 首页列表的筛选规则，抽成纯函数方便单元测试。
 *
 * 三个维度的优先级：
 * - 搜索关键词非空时是全局查找，关系/类型筛选都不叠加——
 *   用户搜名字时不该因为停在某个标签上而搜不到人
 * - 平时关系标签和类型标签是"与"的关系，可以叠加（家人 + 缅怀）
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
        tab: String,
        type: String,
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
            .filter { tab == "all" || it.relation == tab }
            .filter { type == "all" || it.eventType == type }
    }
}
