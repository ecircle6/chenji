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

    /**
     * 类型选择器的展示顺序。
     *
     * MARRIAGE、BABY 不再放进选择器（宝宝生日与生日完全重复；结婚纪念并入情侣纪念），
     * 但常量与下面的文案映射仍保留，以便老数据里的这两类记录继续正常显示。
     */
    val ALL = listOf(BIRTHDAY, LOVE, MEMORIAL, OTHER)

    fun label(type: String): String = when (type) {
        LOVE -> "情侣纪念"
        MARRIAGE -> "结婚纪念"
        BABY -> "宝宝生日"
        MEMORIAL -> "缅怀"
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

    /** 日期输入项在该类型下的叫法，例如缅怀要叫“离开的日子”而不是“出生日期”。
     * 用户反馈“忌日”“逝世”这类字眼犯忌讳，展示文案全部改用“缅怀”“离开” */
    fun dateFieldLabel(type: String): String = when (type) {
        LOVE -> "纪念日期"
        MARRIAGE -> "结婚日期"
        MEMORIAL -> "离开的日子"
        OTHER -> "纪念日期"
        else -> "出生日期"
    }

    /** 名称输入项的叫法。结婚/恋爱/其他记的不一定是人，叫“姓名”不合适 */
    fun nameFieldLabel(type: String): String = when (type) {
        MARRIAGE, LOVE, OTHER -> "称呼 / 标题"
        else -> "姓名"
    }

    /** true 表示按“年龄 + 属相”展示，false 表示按“第 N 周年”展示 */
    fun usesAge(type: String): Boolean = type == BIRTHDAY || type == BABY

    /** 庄重类型：用灰蓝配色，文案不出现蛋糕、快乐、祝福等庆祝字样 */
    fun isSolemn(type: String): Boolean = type == MEMORIAL
}
