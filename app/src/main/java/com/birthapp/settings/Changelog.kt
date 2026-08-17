package com.birthapp.settings

import android.content.Context

/**
 * 版本更新说明（"这版更新了什么"）。
 *
 * Android 侧载安装时系统安装器不会展示升级说明（那是应用商店的机制），
 * 所以做成应用内展示：升级后首次打开自动弹窗 + 设置页「版本更新说明」可看全部历史。
 *
 * 发版约定：每次 release 必须在 all 列表头部加一条新条目，
 * version 与 build.gradle.kts 的 versionName 保持一致。
 */
data class ChangelogEntry(
    /** 对应 build.gradle.kts 的 versionName */
    val version: String,
    /** 一句话主题 */
    val title: String,
    /** 逐条说明 */
    val items: List<String>
)

object Changelog {

    /** 全部版本说明，最新在前。发版时在头部插入新条目。 */
    val all = listOf(
        ChangelogEntry(
            version = "2.1.8",
            title = "首页全新改版",
            items = listOf(
                "底部新增「首页/日历」双页签，日历独立成页展示全部记录",
                "首页新增分类筛选：快捷胶囊 +「更多筛选」面板，支持关系/类型/生肖三维叠加",
                "新增即将到来的大倒计时聚焦卡片（最近的生日/纪念日一眼可见）",
                "卡片布局紧凑化：类型色条 + 类型标签 + 日期·关系双行展示",
                "新增页首问候语，每天一句暖心寄语"
            )
        ),
        ChangelogEntry(
            version = "2.1.7",
            title = "界面细节修复",
            items = listOf(
                "修复首页卡片右上角删除图标透出的问题"
            )
        ),
        ChangelogEntry(
            version = "2.1.6",
            title = "界面预览与测试覆盖",
            items = listOf(
                "各页面新增 @Preview 预览，开发调试更直观",
                "新增 Compose UI 测试，核心页面交互回归有保障"
            )
        ),
        ChangelogEntry(
            version = "2.1.5",
            title = "更新说明上线",
            items = listOf(
                "新增版本更新说明：升级后首次打开自动展示本版更新内容，设置页可随时查看全部历史",
                "分享卡片圆角外铺深色底，在浅色背景的相册里不再露白角"
            )
        ),
        ChangelogEntry(
            version = "2.1.4",
            title = "分享卡片定稿",
            items = listOf(
                "分享卡片按设计稿逐项对齐：1080×1920 竖版双风格（极光毛玻璃 / 深夜烛火）"
            )
        ),
        ChangelogEntry(
            version = "2.1.3",
            title = "分享卡片重写",
            items = listOf(
                "分享卡片按修正版设计严格重写：1:1 像素、全居中布局、光斑与金色配色修正"
            )
        ),
        ChangelogEntry(
            version = "2.1.2",
            title = "新增分享卡片",
            items = listOf(
                "可以把生日记录生成一张精美卡片，分享到微信、朋友圈等"
            )
        )
    )

    /** 最新版本说明（即当前 release 的说明） */
    val latest: ChangelogEntry get() = all.first()

    /** 与 ThemeStore / MainActivity 共用同一份应用设置文件 */
    private const val PREFS_NAME = "birthapp_settings"
    private const val KEY_LAST_SEEN_CODE = "last_seen_changelog_code"

    /** 上次已展示过说明的 versionCode（0 表示从没看过） */
    fun lastSeenCode(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SEEN_CODE, 0L)

    /** 记录本次已展示的版本，避免每次打开都弹 */
    fun markSeen(context: Context, code: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_SEEN_CODE, code).apply()
    }
}
