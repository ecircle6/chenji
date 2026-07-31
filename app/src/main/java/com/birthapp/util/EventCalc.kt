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

    /** 下一次事件发生的阳历日期：从“上一个年份”开始找第一个不早于今天的 */
    fun nextSolarDate(birthday: Birthday, today: LocalDate = LocalDate.now()): SolarDate {
        // 必须从 today.year - 1 起步：农历腊月/冬月对应的阳历日期落在下一个公历年，
        // 公历 1-2 月时眼前这次事件属于“上一个农历年”，从今年找会漏掉它、多算出一整年
        for (year in today.year - 1..today.year + 1) {
            val candidate = solarDateInYear(birthday, year)
            if (!candidate.toLocalDate().isBefore(today)) return candidate
        }
        return solarDateInYear(birthday, today.year + 1)
    }

    /** 距下一次事件还有几天，0 表示就是今天 */
    fun countdown(birthday: Birthday, today: LocalDate = LocalDate.now()): Int =
        java.time.temporal.ChronoUnit.DAYS
            .between(today, nextSolarDate(birthday, today).toLocalDate()).toInt()
}
