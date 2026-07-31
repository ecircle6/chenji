package com.birthapp.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 农历换算正确性测试。
 *
 * 标准答案全部取自公开万年历的公认日期（春节=正月初一、中秋=八月十五、端午=五月初五）。
 * 换算表 LUNAR_INFO 只要错一年，之后所有年份都会连锁偏移，
 * 所以春节逐年对照是最灵敏的探针。
 */
class LunarCalendarTest {

    /** 1930-2035 每年春节的阳历日期（正月初一） */
    private val springFestivals = mapOf(
        1930 to "01-30", 1931 to "02-17", 1932 to "02-06", 1933 to "01-26", 1934 to "02-14",
        1935 to "02-04", 1936 to "01-24", 1937 to "02-11", 1938 to "01-31", 1939 to "02-19",
        1940 to "02-08", 1941 to "01-27", 1942 to "02-15", 1943 to "02-05", 1944 to "01-25",
        1945 to "02-13", 1946 to "02-02", 1947 to "01-22", 1948 to "02-10", 1949 to "01-29",
        1950 to "02-17", 1951 to "02-06", 1952 to "01-27", 1953 to "02-14", 1954 to "02-03",
        1955 to "01-24", 1956 to "02-12", 1957 to "01-31", 1958 to "02-18", 1959 to "02-08",
        1960 to "01-28", 1961 to "02-15", 1962 to "02-05", 1963 to "01-25", 1964 to "02-13",
        1965 to "02-02", 1966 to "01-21", 1967 to "02-09", 1968 to "01-30", 1969 to "02-17",
        1970 to "02-06", 1971 to "01-27", 1972 to "02-15", 1973 to "02-03", 1974 to "01-23",
        1975 to "02-11", 1976 to "01-31", 1977 to "02-18", 1978 to "02-07", 1979 to "01-28",
        1980 to "02-16", 1981 to "02-05", 1982 to "01-25", 1983 to "02-13", 1984 to "02-02",
        1985 to "02-20", 1986 to "02-09", 1987 to "01-29", 1988 to "02-17", 1989 to "02-06",
        1990 to "01-27", 1991 to "02-15", 1992 to "02-04", 1993 to "01-23", 1994 to "02-10",
        1995 to "01-31", 1996 to "02-19", 1997 to "02-07", 1998 to "01-28", 1999 to "02-16",
        2000 to "02-05", 2001 to "01-24", 2002 to "02-12", 2003 to "02-01", 2004 to "01-22",
        2005 to "02-09", 2006 to "01-29", 2007 to "02-18", 2008 to "02-07", 2009 to "01-26",
        2010 to "02-14", 2011 to "02-03", 2012 to "01-23", 2013 to "02-10", 2014 to "01-31",
        2015 to "02-19", 2016 to "02-08", 2017 to "01-28", 2018 to "02-16", 2019 to "02-05",
        2020 to "01-25", 2021 to "02-12", 2022 to "02-01", 2023 to "01-22", 2024 to "02-10",
        2025 to "01-29", 2026 to "02-17", 2027 to "02-06", 2028 to "01-26", 2029 to "02-13",
        2030 to "02-03", 2031 to "01-23", 2032 to "02-11", 2033 to "01-31", 2034 to "02-19",
        2035 to "02-08"
    )

    @Test
    fun `春节逐年对照 农历转阳历`() {
        val errors = mutableListOf<String>()
        for ((year, expected) in springFestivals) {
            val s = LunarCalendar.lunarToSolar(year, 1, 1)
            val actual = "%02d-%02d".format(s.month, s.day)
            if (s.year != year || actual != expected) {
                errors.add("农历${year}年正月初一: 期望 $year-$expected, 实际 ${s.year}-$actual")
            }
        }
        assertTrue("换算表错误:\n" + errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `春节逐年对照 阳历转农历`() {
        val errors = mutableListOf<String>()
        for ((year, expected) in springFestivals) {
            val (m, d) = expected.split("-").map { it.toInt() }
            val l = LunarCalendar.solarToLunar(year, m, d)
            if (l.year != year || l.month != 1 || l.day != 1 || l.isLeapMonth) {
                errors.add("$year-$expected: 期望 农历${year}年1月1日, 实际 农历${l.year}年${l.month}月${l.day}日 闰=${l.isLeapMonth}")
            }
        }
        assertTrue("换算表错误:\n" + errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `中秋对照`() {
        // 八月十五
        val midAutumn = mapOf(
            2010 to "09-22", 2011 to "09-12", 2012 to "09-30", 2013 to "09-19",
            2014 to "09-08", 2015 to "09-27", 2016 to "09-15", 2017 to "10-04",
            2018 to "09-24", 2019 to "09-13",
            2020 to "10-01", 2021 to "09-21", 2022 to "09-10", 2023 to "09-29",
            2024 to "09-17", 2025 to "10-06", 2026 to "09-25"
        )
        val errors = mutableListOf<String>()
        for ((year, expected) in midAutumn) {
            val s = LunarCalendar.lunarToSolar(year, 8, 15)
            val actual = "%02d-%02d".format(s.month, s.day)
            if (s.year != year || actual != expected) {
                errors.add("农历${year}年八月十五: 期望 $year-$expected, 实际 ${s.year}-$actual")
            }
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `端午对照`() {
        // 五月初五
        val dragonBoat = mapOf(
            2010 to "06-16", 2011 to "06-06", 2012 to "06-23", 2013 to "06-12",
            2014 to "06-02", 2015 to "06-20", 2016 to "06-09", 2017 to "05-30",
            2018 to "06-18", 2019 to "06-07",
            2020 to "06-25", 2021 to "06-14", 2022 to "06-03", 2023 to "06-22",
            2024 to "06-10", 2025 to "05-31", 2026 to "06-19"
        )
        val errors = mutableListOf<String>()
        for ((year, expected) in dragonBoat) {
            val s = LunarCalendar.lunarToSolar(year, 5, 5)
            val actual = "%02d-%02d".format(s.month, s.day)
            if (s.year != year || actual != expected) {
                errors.add("农历${year}年五月初五: 期望 $year-$expected, 实际 ${s.year}-$actual")
            }
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `闰月年份对照`() {
        val leapMonths = mapOf(
            1930 to 6, 1933 to 5, 1936 to 3, 1938 to 7, 1941 to 6, 1944 to 4, 1947 to 2,
            1949 to 7, 1952 to 5, 1955 to 3, 1957 to 8, 1960 to 6, 1963 to 4, 1966 to 3,
            1968 to 7, 1971 to 5, 1974 to 4, 1976 to 8, 1979 to 6, 1982 to 4, 1984 to 10,
            1987 to 6, 1990 to 5, 1993 to 3, 1995 to 8, 1998 to 5, 2001 to 4, 2004 to 2,
            2006 to 7, 2009 to 5, 2012 to 4, 2014 to 9, 2017 to 6, 2020 to 4, 2023 to 2,
            2025 to 6, 2028 to 5,
            // 无闰月的年份抽查
            2022 to 0, 2024 to 0, 2026 to 0, 2000 to 0, 1999 to 0
        )
        val errors = mutableListOf<String>()
        for ((year, expected) in leapMonths) {
            val actual = LunarCalendar.leapMonth(year)
            if (actual != expected) errors.add("${year}年闰月: 期望 $expected, 实际 $actual")
        }
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `全量往返一致性 1901-2098`() {
        // 阳历→农历→阳历 应回到同一天。查表数据内部不自洽时这里会暴露
        var date = java.time.LocalDate.of(1901, 1, 1)
        val end = java.time.LocalDate.of(2098, 12, 31)
        var checked = 0
        while (!date.isAfter(end)) {
            val l = LunarCalendar.solarToLunar(date.year, date.monthValue, date.dayOfMonth)
            val s = LunarCalendar.lunarToSolar(l.year, l.month, l.day, l.isLeapMonth)
            assertEquals(
                "往返不一致 @ $date -> 农历${l.year}-${l.month}-${l.day}(闰=${l.isLeapMonth})",
                date, s.toLocalDate()
            )
            checked++
            date = date.plusDays(1)
        }
        assertTrue(checked > 70000)
    }
}
