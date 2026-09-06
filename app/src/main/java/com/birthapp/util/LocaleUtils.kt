package com.birthapp.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build

/**
 * 语言判断小工具：i18n 后部分格式化需要按语言走不同模板
 * （中文「1月5日」vs 英文「Jan 5」），统一判定口径。
 */
object LocaleUtils {

    /** 当前资源是否解析到英文（values-en）。非英文一律按默认（中文）处理 */
    fun isEnglish(resources: Resources): Boolean =
        resources.configuration.locales[0].language == "en"

    /**
     * 取「与分应用语言一致」的 Resources，ViewModel 层取文案/格式化统一走这里。
     *
     * Android 13+ 分应用语言切换后，Activity 会立即重建拿到新 configuration，
     * 但进程不重启时 Application 级 Resources（getApplication() 取到的）语言滞后——
     * 表现为 UI 已是中文而格式化仍走英文模板，甚至触发英文模板的格式化异常。
     * 显式读 LocaleManager.applicationLocales 包一层 configuration context 即可
     * 与 UI 口径对齐；未设置分应用语言（跟随系统）时原样返回。
     */
    fun localizedResources(context: Context): Resources {
        if (Build.VERSION.SDK_INT >= 33) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
            val appLocales = localeManager?.applicationLocales
            if (appLocales != null && !appLocales.isEmpty) {
                val config = Configuration(context.resources.configuration)
                config.setLocales(appLocales)
                return context.createConfigurationContext(config).resources
            }
        }
        return context.resources
    }
}