package com.birthapp.util

import java.time.LocalDate

/**
 * 首页问候语：按年积日从文案池取一句。
 * 当天稳定（同一天永远同句）、跨天轮换、纯函数可单测。
 * 用 dayOfYear 而非随机数：随机数会在重组时跳句，显得"每次打开都变"。
 */
object Greeting {

    private val POOL = listOf(
        "愿你被时光温柔以待",
        "每一个日子都值得铭记",
        "时光不老，我们不散",
        "珍惜当下，铭记过往",
        "岁月漫长，值得等待"
    )

    fun today(date: LocalDate = LocalDate.now()): String =
        POOL[(date.dayOfYear - 1) % POOL.size]
}