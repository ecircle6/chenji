package com.birthapp.data

import androidx.room.TypeConverter

/**
 * Room 类型转换：多级提前提醒存成逗号分隔字符串（如 "0,3"）。
 * 排序和去重交给业务层（normalizeAdvanceLevels），这里只做无损的存取
 */
class Converters {

    @TypeConverter
    fun advanceDaysToString(list: List<Int>): String = list.joinToString(",")

    @TypeConverter
    fun stringToAdvanceDays(value: String): List<Int> =
        value.split(",").mapNotNull { it.trim().toIntOrNull() }
}
