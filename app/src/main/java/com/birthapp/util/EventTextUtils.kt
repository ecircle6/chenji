package com.birthapp.util

import com.birthapp.data.EventType

/**
 * 按事件类型生成展示文案。
 *
 * 卡片、通知、后续的详情页与小组件都从这里取文案，
 * 目的是让忌日这类庄重措辞只定义一处，不会出现某个入口漏改而说出"祝福快乐"的情况。
 */
object EventTextUtils {

    /**
     * 卡片第二行：类型 · 日期 · 年龄或周年
     *
     * 生日不加"🎂 生日"前缀：它是默认类型且占绝大多数，
     * 农历日期加属相加年龄本身已经很长，再加前缀会挤到换行。
     */
    fun infoLine(
        eventType: String,
        calendarType: String,
        dateLabel: String,
        zodiacEmoji: String,
        zodiac: String,
        age: Int
    ): String {
        val calendarEmoji = if (calendarType == "lunar") "\uD83C\uDF19" else "☀️"
        val prefix = if (eventType == EventType.BIRTHDAY) {
            ""
        } else {
            "${EventType.emoji(eventType)} ${EventType.label(eventType)}  ·  "
        }
        val tail = if (EventType.usesAge(eventType)) {
            // 花括号不能省：紧跟中文时 Kotlin 会把"$zodiacEmoji属"整体当成变量名
            "${zodiacEmoji}属${zodiac} · ${age}岁"
        } else {
            "第 $age 周年"
        }
        return "$prefix$calendarEmoji $dateLabel  ·  $tail"
    }

    /** 卡片上"就是今天"的横幅。卡片已显示姓名，所以生日类不再重复姓名 */
    fun cardBanner(eventType: String, name: String, years: Int): String = when (eventType) {
        EventType.BABY -> "\uD83D\uDC76 宝宝今天 $years 岁啦！"
        EventType.MARRIAGE -> "\uD83D\uDC8D 今天是结婚 $years 周年纪念日！"
        EventType.LOVE -> "❤\uFE0F 恋爱 $years 周年快乐！"
        EventType.MEMORIAL -> "\uD83D\uDD6F\uFE0F 今天是${name}逝世 $years 周年"
        EventType.OTHER -> "\uD83D\uDCCC 今天是「$name」第 $years 周年"
        else -> "\uD83C\uDF82 今天 $years 岁生日！"
    }

    /** 当天提醒的通知标题 */
    fun notificationTitleToday(eventType: String, name: String, years: Int): String = when (eventType) {
        EventType.BABY -> "$name 今天 $years 岁啦！"
        EventType.MARRIAGE -> "今天是结婚 $years 周年纪念日！"
        EventType.LOVE -> "恋爱 $years 周年快乐！"
        EventType.MEMORIAL -> "今天是 $name 逝世 $years 周年"
        EventType.OTHER -> "今天是「$name」第 $years 周年"
        else -> if (years > 0) "今天是 $name 的 ${years}岁生日！" else "今天是 $name 的生日！"
    }

    /** 提前提醒的通知标题 */
    fun notificationTitleAdvance(eventType: String, name: String, years: Int): String = when (eventType) {
        EventType.MARRIAGE -> "结婚 $years 周年纪念日快到了"
        EventType.LOVE -> "恋爱 $years 周年快到了"
        EventType.MEMORIAL -> "$name 逝世 $years 周年将至"
        EventType.OTHER -> "「$name」第 $years 周年快到了"
        else -> "$name 的生日快到了！"
    }

    /** 通知正文。庄重类型不出现"祝福"字样 */
    fun notificationText(eventType: String, dateInfo: String, advanceDays: Int): String = when {
        advanceDays > 0 -> "还有 $advanceDays 天 · $dateInfo"
        EventType.isSolemn(eventType) -> dateInfo
        else -> "$dateInfo - 别忘了送上祝福！"
    }
}
