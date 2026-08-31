package com.birthapp.util

import android.content.res.Resources
import com.birthapp.R
import java.time.LocalDate

/**
 * 首页问候语：按年积日从文案池取一句。
 * 当天稳定（同一天永远同句）、跨天轮换、文案池走资源（中英两套）。
 * 用 dayOfYear 而非随机数：随机数会在重组时跳句，显得"每次打开都变"。
 */
object Greeting {

    fun today(resources: Resources, date: LocalDate = LocalDate.now()): String {
        val pool = resources.getStringArray(R.array.greeting_pool)
        return pool[(date.dayOfYear - 1) % pool.size]
    }
}