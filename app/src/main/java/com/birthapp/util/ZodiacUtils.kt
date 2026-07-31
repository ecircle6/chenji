package com.birthapp.util

object ZodiacUtils {

    private val ZODIAC_NAMES = arrayOf(
        "鼠", "牛", "虎", "兔", "龙", "蛇",
        "马", "羊", "猴", "鸡", "狗", "猪"
    )

    private val ZODIAC_EMOJIS = arrayOf(
        "🐭", "🐮", "🐯", "🐰", "🐲", "🐍",
        "🐴", "🐑", "🐵", "🐔", "🐶", "🐷"
    )

    /**
     * 根据出生年份计算生肖（基于农历年份，简化用阳历年份近似）
     */
    fun getZodiacName(year: Int): String {
        val index = ((year - 4) % 12 + 12) % 12
        return ZODIAC_NAMES[index]
    }

    fun getZodiacEmoji(year: Int): String {
        val index = ((year - 4) % 12 + 12) % 12
        return ZODIAC_EMOJIS[index]
    }

    /**
     * 计算年龄（虚岁）
     */
    fun getAge(birthYear: Int, currentYear: Int): Int {
        return currentYear - birthYear
    }

    /**
     * 获取关系标签文字
     */
    fun getRelationLabel(relation: String): String {
        return when (relation) {
            "family" -> "家人"
            "friend" -> "朋友"
            "colleague" -> "同事"
            "other" -> "其他"
            else -> relation
        }
    }

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
