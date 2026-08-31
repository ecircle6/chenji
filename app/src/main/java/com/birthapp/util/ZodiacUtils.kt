package com.birthapp.util

import android.content.res.Resources
import com.birthapp.R

object ZodiacUtils {

    // 生肖中文名：既是数据语义（筛选 key、数据库无关）也是默认文案。
    // 顺序与 ZODIAC_EMOJIS / 资源数组一一对应（鼠..猪）
    private val ZODIAC_NAMES = arrayOf(
        "鼠", "牛", "虎", "兔", "龙", "蛇",
        "马", "羊", "猴", "鸡", "狗", "猪"
    )

    private val ZODIAC_EMOJIS = arrayOf(
        "🐭", "🐮", "🐯", "🐰", "🐲", "🐍",
        "🐴", "🐑", "🐵", "🐔", "🐶", "🐷"
    )

    /**
     * 根据出生年份计算生肖（基于农历年份，简化用阳历年份近似）。
     * 返回中文名——它是筛选条件/摘要的稳定 key，与语言无关；
     * 界面展示要跟随语言时用 [zodiacDisplayName]
     */
    fun getZodiacName(year: Int): String {
        val index = zodiacIndex(year)
        return ZODIAC_NAMES[index]
    }

    fun getZodiacEmoji(year: Int): String {
        val index = zodiacIndex(year)
        return ZODIAC_EMOJIS[index]
    }

    /** 生肖在生肖环上的索引（0=鼠），供资源数组取显示名 */
    private fun zodiacIndex(year: Int): Int = ((year - 4) % 12 + 12) % 12

    /**
     * 生肖显示名：按当前语言取资源数组（中文默认「鼠」，英文库取 Rat..Pig）。
     * 中文 key（getZodiacName 输出）也能通过 [zodiacDisplayNameByKey] 转显示名
     */
    fun zodiacDisplayName(resources: Resources, year: Int): String =
        zodiacDisplayNameByIndex(resources, zodiacIndex(year))

    /** 由中文 key（筛选状态里存的就是它）取显示名 */
    fun zodiacDisplayNameByKey(resources: Resources, key: String): String {
        val index = ZODIAC_NAMES.indexOf(key)
        return if (index >= 0) zodiacDisplayNameByIndex(resources, index) else key
    }

    private fun zodiacDisplayNameByIndex(resources: Resources, index: Int): String {
        val array = if (LocaleUtils.isEnglish(resources)) {
            resources.getStringArray(R.array.zodiac_names_en)
        } else {
            resources.getStringArray(R.array.zodiac_names)
        }
        return array[index % array.size]
    }

    /**
     * 计算年龄（虚岁）
     */
    fun getAge(birthYear: Int, currentYear: Int): Int {
        return currentYear - birthYear
    }

    /** 关系标签的资源 id（显示文案集中管理） */
    fun getRelationLabelRes(relation: String): Int = when (relation) {
        "family" -> R.string.relation_family
        "friend" -> R.string.relation_friend
        "colleague" -> R.string.relation_colleague
        else -> R.string.relation_other
    }

    fun getRelationLabel(resources: Resources, relation: String): String =
        resources.getString(getRelationLabelRes(relation))

    fun getRelationEmoji(relation: String): String {
        return when (relation) {
            "family" -> "👨‍👩‍👧"
            "friend" -> "🤝"
            "colleague" -> "💼"
            "other" -> "👤"
            else -> "👤"
        }
    }
}