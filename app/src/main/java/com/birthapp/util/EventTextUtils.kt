package com.birthapp.util

import android.content.res.Resources
import com.birthapp.R
import com.birthapp.data.EventType

/**
 * 按事件类型生成展示文案。
 *
 * 卡片、通知、后续的详情页与小组件都从这里取文案，
 * 目的是让缅怀这类庄重措辞只定义一处，不会出现某个入口漏改而说出"祝福快乐"的情况。
 * 文案走资源（中英两套），函数接收 [Resources] 以便非 Compose 环境（ViewModel/通知）也能用。
 */
object EventTextUtils {

    /**
     * 卡片第二行：类型 · 日期 · 年龄或周年
     *
     * 生日不加"🎂 生日"前缀：它是默认类型且占绝大多数，
     * 农历日期加属相加年龄本身已经很长，再加前缀会挤到换行。
     */
    fun infoLine(
        resources: Resources,
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
            resources.getString(
                R.string.info_line_prefix,
                EventType.emoji(eventType),
                EventType.label(resources, eventType)
            )
        }
        val tail = if (EventType.usesAge(eventType)) {
            // 花括号不能省：紧跟中文时 Kotlin 会把"$zodiacEmoji属"整体当成变量名
            if (LocaleUtils.isEnglish(resources)) {
                resources.getString(R.string.info_line_age_en, zodiacEmoji, zodiac, age)
            } else {
                resources.getString(R.string.info_line_age_zh, zodiacEmoji, zodiac, age)
            }
        } else {
            if (LocaleUtils.isEnglish(resources)) {
                resources.getString(R.string.info_line_anniversary_en, age)
            } else {
                resources.getString(R.string.info_line_anniversary_zh, age)
            }
        }
        return "$prefix$calendarEmoji $dateLabel  ·  $tail"
    }

    /** 卡片上"就是今天"的横幅。卡片已显示姓名，所以生日类不再重复姓名 */
    fun cardBanner(resources: Resources, eventType: String, name: String, years: Int): String =
        when (eventType) {
            EventType.BABY -> resources.getString(R.string.banner_baby, years)
            EventType.MARRIAGE -> resources.getString(R.string.banner_marriage, years)
            EventType.LOVE -> resources.getString(R.string.banner_love, years)
            EventType.MEMORIAL -> resources.getString(R.string.banner_memorial, name, years)
            EventType.OTHER -> resources.getString(R.string.banner_other, name, years)
            else -> resources.getString(R.string.banner_birthday, years)
        }

    /** 当天提醒的通知标题 */
    fun notificationTitleToday(resources: Resources, eventType: String, name: String, years: Int): String =
        when (eventType) {
            EventType.BABY -> resources.getString(R.string.notif_today_baby, name, years)
            EventType.MARRIAGE -> resources.getString(R.string.notif_today_marriage, years)
            EventType.LOVE -> resources.getString(R.string.notif_today_love, years)
            EventType.MEMORIAL -> resources.getString(R.string.notif_today_memorial, name, years)
            EventType.OTHER -> resources.getString(R.string.notif_today_other, name, years)
            else -> if (years > 0) {
                resources.getString(R.string.notif_today_birthday_age, name, years)
            } else {
                resources.getString(R.string.notif_today_birthday, name)
            }
        }

    /** 提前提醒的通知标题 */
    fun notificationTitleAdvance(resources: Resources, eventType: String, name: String, years: Int): String =
        when (eventType) {
            EventType.MARRIAGE -> resources.getString(R.string.notif_advance_marriage, years)
            EventType.LOVE -> resources.getString(R.string.notif_advance_love, years)
            EventType.MEMORIAL -> resources.getString(R.string.notif_advance_memorial, name, years)
            EventType.OTHER -> resources.getString(R.string.notif_advance_other, name, years)
            else -> resources.getString(R.string.notif_advance_birthday, name)
        }

    /** 通知正文。庄重类型不出现"祝福"字样 */
    fun notificationText(resources: Resources, eventType: String, dateInfo: String, advanceDays: Int): String =
        when {
            advanceDays > 0 -> resources.getString(R.string.notif_text_advance, advanceDays, dateInfo)
            EventType.isSolemn(eventType) -> dateInfo
            else -> resources.getString(R.string.notif_text_bless, dateInfo)
        }
}