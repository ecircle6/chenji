package com.birthapp.util

import com.birthapp.data.Birthday
import com.birthapp.lunar.LunarCalendar
import com.birthapp.lunar.SolarDate
import java.time.LocalDate

/**
 * 「下一次事件在哪天、还剩几天」的算法。
 *
 * 首页列表、详情页、桌面小组件都要算这个数。放在一处，
 * 避免各写一份之后农历闰月或跨年的处理慢慢走样，最后三个地方显示不同的天数。
 */
object EventCalc {

    /** 指定年份里这个事件对应的阳历日期。农历换算失败时退回同月同日，不让页面崩掉 */
    fun solarDateInYear(birthday: Birthday, year: Int): SolarDate = with(birthday) {
        if (calendarType == "lunar") {
            runCatching {
                LunarCalendar.getNextLunarBirthdayInSolar(birthMonth, birthDay, isLeapMonth, year)
            }.getOrDefault(SolarDate(year, birthMonth, birthDay))
        } else {
            SolarDate(year, birthMonth, birthDay)
        }
    }

    /** 下一次事件发生的阳历日期：今年的已经过了就取明年 */
    fun nextSolarDate(birthday: Birthday, today: LocalDate = LocalDate.now()): SolarDate {
        val thisYear = solarDateInYear(birthday, today.year)
        return if (DateUtils.daysUntilDate(thisYear.toLocalDate()) < 0) {
            solarDateInYear(birthday, today.year + 1)
        } else {
            thisYear
        }
    }

    /** 距下一次事件还有几天，0 表示就是今天 */
    fun countdown(birthday: Birthday, today: LocalDate = LocalDate.now()): Int =
        DateUtils.daysUntilDate(nextSolarDate(birthday, today).toLocalDate())
}
