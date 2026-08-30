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
            version = "2.1.18",
            title = "响应速度与图标、小组件留白打磨",
            items = listOf(
                "首页响应提速：保存/删除后返回不再重播整屏入场动画，列表即时呈现；错峰动效保留在打开 App 与切换筛选时，节奏也更快",
                "保存/删除操作提前返回：提醒闹钟转入后台排定，点完保存马上回首页",
                "应用图标重排版：月亮收小至合适比例并居中，星星全部收进安全区，任何桌面遮罩都不再贴边或被裁切",
                "小组件按桌面实际尺寸自适应渲染：拉高就多显示记录、行距均匀铺满整面，不再有沉底或夹心的空白段；4×4 记录多时也不再溢出裁切"
            )
        ),
        ChangelogEntry(
            version = "2.1.17",
            title = "小组件「纸笺辰刻」打磨：层级、夜间适配与母题",
            items = listOf(
                "4×4 大组件 Hero 头升级为与 App 内同源的渐变底（生日珊瑚/情侣紫/其他青/缅怀灰蓝，深色自动切暗金等夜间渐变）",
                "字重层级重排：姓名/标签降为次级字重，倒计时数字成为行内唯一焦点",
                "夜间对比度修正：类型强调色夜间整体提亮一档、头像底色透明度夜间加倍，深色壁纸上不再看不清",
                "颜色全部收敛到统一调色板，类型色白天换成更深一档，小字号也够清晰",
                "4×2 与 2×2 头部加入 🌙 母题，与「怀月藏星」图标呼应"
            )
        ),
        ChangelogEntry(
            version = "2.1.16",
            title = "动效系统上线：界面第一次「动」起来",
            items = listOf(
                "首页错峰入场：打开 App / 切换筛选时，Hero 与列表卡片依次轻盈浮起（spring 回弹，逐项 55ms 错峰）",
                "倒计时数字滚动：跨天或编辑日期后，数字上滑滚入新值而不是生硬跳变，等宽字阶保证滚动不横移",
                "「今天」呼吸光晕：当天的卡片/横幅外圈轻轻呼吸（2.6 秒一周期），一眼锁定今天的主角",
                "全部动效基于 spring 物理，与系统手感一致；测试全量回归通过"
            )
        ),
        ChangelogEntry(
            version = "2.1.15",
            title = "主题系统升级：排版、圆角与对比度全面收敛",
            items = listOf(
                "倒计时数字全面等宽化（tabular-nums）：数字变化时宽度不再抖动，Hero/详情页/列表卡统一接入 display 字阶",
                "卡片姓名降为次级字重、数字成为唯一视觉焦点，层级一眼可辨",
                "全局圆角收敛为 4 档语义规格（弹层 28 / 卡片 16 / 内嵌 12 / 输入 8 + 胶囊），57 处零散数值清理完毕",
                "次要文字颜色加深（#8E8E8E → #6E6E6E），浅色模式下对比度从 3.06:1 提升到 4.6:1，读起来更省力",
                "深浅色主题补全 surface 容器五档与分隔线角色，为后续动效与卡片分层打底"
            )
        ),
        ChangelogEntry(
            version = "2.1.14",
            title = "图标与小组件焕新：怀月藏星 × 纸笺辰刻",
            items = listOf(
                "图标支持 Themed 单色（Android 13+）：浅壁纸黑 on 白、深壁纸白 on 黑自动取色，单色下靠形体（主星 4 尖/副星圆点）区分主次，56dp 小尺寸仍可辨",
                "小组件纸笺辰刻重构：2×2 日历撕页单焦（34sp 倒计时）· 4×2 问候语 + 左 3.5dp 色条 + 36dp 头像纸笺行 · 4×4 Hero 渐变头 + 列表，0.8 秒一眼捕捉「还有几天」",
                "小组件三档与 App 同源：复用左色条/头像/类型标签/倒计时体系，缅怀灰蓝、≤7 天呼吸边框、空态明确「＋添加」按钮，暗色自动适配",
                "点小组件任意一行直达该记录详情页（此前仅加号可点），点 Hero/空白进首页/新增页",
                "修复图标在 Themed 模式下多色黄星被系统单色化消失的问题"
            )
        ),
        ChangelogEntry(
            version = "2.1.13",
            title = "小组件添加入口体验修正",
            items = listOf(
                "长按应用图标添加小组件时不再打开 App：点击后只弹出桌面放置框，放完直接回原地",
                "添加成功后新增提示「小组件已添加到桌面」，不会以为没成功而重复添加",
                "提示：弹出放置框里可勾选尺寸（默认最大尺寸）；通过此入口添加时小组件按默认设置展示最近记录"
            )
        ),
        ChangelogEntry(
            version = "2.1.12",
            title = "小组件快捷添加",
            items = listOf(
                "长按应用图标 →「添加小组件」直接弹出桌面放置框，选好尺寸确认即可",
                "设置页「关于」新增「添加桌面小组件」入口，随时可以再添加",
                "不再需要去桌面小组件列表里翻找（当前桌面不支持时会给提示引导）"
            )
        ),
        ChangelogEntry(
            version = "2.1.11",
            title = "界面细节优化",
            items = listOf(
                "日历页去掉顶部重复标题：正文已有「YYYY年M月 · 农历」月份标题，界面更清爽"
            )
        ),
        ChangelogEntry(
            version = "2.1.10",
            title = "首页聚焦卡规则优化",
            items = listOf(
                "聚焦卡与列表去重：最近的记录只在聚焦卡展示一次，列表不再重复出现",
                "缅怀记录不再被聚焦卡放大：最近全是缅怀时不显示聚焦卡，悼念保持肃穆",
                "同日缅怀与庆祝并存时，聚焦卡让位给同天的庆祝事件",
                "聚焦卡进入 7 天倒计时后自带提醒进度条（与紧急卡进度语义一致）"
            )
        ),
        ChangelogEntry(
            version = "2.1.9",
            title = "首页体验升级：三层卡片 + 专属 Emoji 头像 + 类型配色区分",
            items = listOf(
                "首页卡片按倒计时分层：紧急(0-7天)大卡带进度条 · 标准(8-30天)紧凑卡 · 远景(>30天)按月折叠迷你行",
                "新增专属 Emoji 头像：新建/编辑记录时可选个人 Emoji，替代统一图标",
                "类型配色区分：生日=珊瑚 · 情侣纪念=紫 · 缅怀=灰蓝 · 其他=黄，一眼分辨",
                "紧急卡底部进度条：「提醒进度 · 已过去 X 天」直观传达时间紧迫感",
                "远景行按月份分组：远期日期自动折叠为极简行，首屏不被远期日期占满",
                "头像 Emoji 改为底部面板选择：表单只保留览与「自动/选择」两个入口，不再被图标撑满",
                "修复首页远景迷你行透出删除图标的问题（删除图标仅左滑时随进度显示）",
                "修复「更多筛选」面板类型行无卡片时点击无效果的问题",
                "修复新建页退出慢（转场 700ms→200ms）及退出动画期间可误点控件的问题",
                "修复导入备份预览对话框条目多时无法滚动的问题"
            )
        ),
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
