package com.birthapp.util

import android.content.res.Resources

/**
 * 语言判断小工具：i18n 后部分格式化需要按语言走不同模板
 * （中文「1月5日」vs 英文「Jan 5」），统一判定口径。
 */
object LocaleUtils {

    /** 当前资源是否解析到英文（values-en）。非英文一律按默认（中文）处理 */
    fun isEnglish(resources: Resources): Boolean =
        resources.configuration.locales[0].language == "en"
}