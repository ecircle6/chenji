package com.birthapp.data

/**
 * 事件类型。数据库以字符串存储（v2 新增字段），升级前的老数据一律视为生日。
 *
 * 注意：常量值一旦发布就不能改，改了会导致老数据的类型失效。
 */
object EventType {
    const val BIRTHDAY = "birthday"
    const val MARRIAGE = "marriage"
    const val BABY = "baby"
    const val LOVE = "love"
    const val MEMORIAL = "memorial"
    const val OTHER = "other"

    /** 类型选择器的展示顺序 */
    val ALL = listOf(BIRTHDAY, MARRIAGE, BABY, LOVE, MEMORIAL, OTHER)

    fun label(type: String): String = when (type) {
        MARRIAGE -> "结婚纪念"
        BABY -> "宝宝生日"
        LOVE -> "恋爱纪念"
        MEMORIAL -> "忌日"
        OTHER -> "其他纪念"
        else -> "生日"
    }

    fun emoji(type: String): String = when (type) {
        MARRIAGE -> "\uD83D\uDC8D"
        BABY -> "\uD83D\uDC76"
        LOVE -> "❤\uFE0F"
        MEMORIAL -> "\uD83D\uDD6F\uFE0F"
        OTHER -> "\uD83D\uDCCC"
        else -> "\uD83C\uDF82"
    }

    /** 日期输入项在该类型下的叫法，例如忌日要叫“逝世日期”而不是“出生日期” */
    fun dateFieldLabel(type: String): String = when (type) {
        MARRIAGE -> "结婚日期"
        LOVE -> "恋爱日期"
        MEMORIAL -> "逝世日期"
        OTHER -> "纪念日期"
        else -> "出生日期"
    }

    /** true 表示按“年龄 + 属相”展示，false 表示按“第 N 周年”展示 */
    fun usesAge(type: String): Boolean = type == BIRTHDAY || type == BABY

    /** 庄重类型：用灰蓝配色，文案不出现蛋糕、快乐、祝福等庆祝字样 */
    fun isSolemn(type: String): Boolean = type == MEMORIAL
}
