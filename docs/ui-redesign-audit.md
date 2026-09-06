# UI 改版审计报告（改版第 1 步）

> 审计日期：2026-09-06。对象：辰记 v2.1.20（versionCode 23，Debug 包）。
> 方法：模拟器（Pixel 7 AVD · API 35）逐页截图基线 + lint 基线（75 项，见 `docs/ui-redesign-tooling.md` §9.1）+ 关键点代码核对。
> 本报告只记录问题与建议，不含代码改动；P0/P1/P2 是后续改版的执行顺序依据。
> 遗留状态：模拟器 App 分应用语言 = zh-CN，主题 = 跟随系统（审计后已恢复）。

## 0. 截图索引（app/build/verify/）

| 文件 | 内容 |
|---|---|
| audit_home_light.png | 首页浅色（首启，日期走英文模板的现场） |
| audit_home_light_cold.png / audit_home_light_v2.png | 进程冷启动后首页（日期恢复中文，佐证 P0-2） |
| audit_calendar_light.png | 日历页浅色 |
| audit_detail_light.png | 详情页浅色（冷启动后，正常态） |
| audit_add_light.png / audit_add_light_2.png | 新增页浅色上/下半部 |
| audit_settings_light.png | 设置页浅色 |
| audit_home_dark.png / audit_detail_dark.png | 首页/详情暗色 |

---

## 1. P0 —— 真缺陷 / 崩溃风险（改版动工前先修）

### P0-1【崩溃】英文环境下打开任意详情页必崩
- **现象**：点击首页 Hero 卡进详情，App 直接崩溃退到桌面（已复现，崩溃日志在 `adb logcat -b crash`）。
- **根因**：`values*/strings.xml:31` 的 `date_solar_full_en` 模板占位符序号写错——`%2$s %3$d, %1$d`，而 `DateUtils.formatSolarDate`（DateUtils.kt:52）按「月份(String)、日(Int)、年(Int)」顺序传参，`%1$d` 把 String 当 `%d` 格式化 → `IllegalFormatConversionException: d != java.lang.String`。**英文系统用户（isEnglish=true）打开详情 100% 崩溃**；纯中文系统不受影响（走 `date_solar_full_zh`）。
- **修法**：模板改为 `%1$s %2$d, %3$d`；同批核对全部 `_en` 日期模板（`date_solar_month_day_en`）与 `share_date_line_en`（见 P0-4）。此条即 lint StringFormatMatches 的实锤。
- **关联截图**：audit_home_light.png（点击的 Hero 卡）。

### P0-2【i18n 口径】分应用语言与实际格式化 locale 分裂
- **现象**：系统 en + 分应用语言 zh-CN 时，界面文案是中文，但首页 Hero 日期显示「Jan 1」、远景行「Feb 2」（英文模板）；且此状态下点详情触发 P0-1 崩溃。**force-stop 冷启动后全部恢复中文**（对比 audit_home_light.png 与 audit_home_light_v2.png）。
- **根因**：UI 字符串走 Activity 级 Resources（per-app locale 生效），而 `DetailViewModel`/`HomeTier` 等经 `getApplication().resources` 取 Resources——该实例在语言变更后、进程重启前仍是旧 locale，`LocaleUtils.isEnglish`（LocaleUtils.kt:12）随之判错。
- **修法方向**：统一「资源获取 + 语言判定」口径为单一来源（建议 ViewModel 层统一改用 Activity/带 override 的 context，或封装 LocaleStore 监听变更）；至少保证模板选择与字符串资源解析用同一份 configuration。

### P0-3【行为】小组件尺寸变化回调未调 super
- BirthWidgetReceiver.kt:97 覆写 `onAppWidgetOptionsChanged` 未调 `super.`（lint MissingSuperCall error）。v2.1.19/20 的尺寸自适应链路依赖 options 回调，缺 super 调用可能丢失系统侧处理。修法：补 `super.onAppWidgetOptionsChanged(...)`。

### P0-4【格式】英文分享卡日期行参数不匹配
- ShareCardGenerator.kt:383 处 `share_date_line_en` 要求 4 个参数但调用传入不匹配（lint StringFormatMatches）。分享卡是 Canvas 直绘，若在英文分支走到该模板存在同类格式化异常风险。修法：核对模板占位与传参并修正。

---

## 2. P1 —— 视觉层次与一致性（改版主线）

### P1-1 双强调色无规则混用（全 App 性）
同一类「选中态/主操作」组件在不同页面用 teal 与 coral 两套强调色，无语义规则：
- 首页筛选「全部」选中 = coral（audit_home_light.png）；底栏选中胶囊 = teal；
- 新增页「提前提醒」选中 chip = teal，「关系分类」选中 chip = coral（audit_add_light_2.png 同屏并存）；
- 新增页「选择 Emoji」主按钮 = teal 填充（audit_add_light.png）；
- 详情页倒计时数字 = teal、底部「编辑」主按钮 = coral（accent，DetailScreen.kt:356-363）。
**建议**：改版时定规则——主操作/选中态 = primary（建议 coral 系），teal 退为辅助/成功语义或仅在类型色（宝宝=Teal）使用；类型色（EventTyleStyle）只承担类型语义不承担交互语义。

### P1-2 详情页倒计时卡冷暖冲突
浅色：淡珊瑚底 + teal 大数字；暗色：深棕底 + teal 大数字（audit_detail_dark.png）——底色是类型色 wash、数字是固定 teal，冷暖两色同框打架。与 P1-1 同根，随色彩规则一并处理。

### P1-3 日历页顶栏空旷、无事件日缺锚点
顶栏只有右上齿轮、无标题语境（与首页「辰记」大标题不呼应，audit_calendar_light.png）；下半屏大面积空白，无事件的月份没有任何引导/占位/图例。改版方向：月份语境入顶栏、空态引导（「添加第一个日子」）、事件日在日期格上的标记样式也需补充设计（现数据无事件日，基线未能覆盖，需造数复拍）。

### P1-4 首页筛选行溢出观感
横向 LazyRow（HomeScreen.kt:452 起 FilterChip）滚动区末尾露出半截白色胶囊贴着筛选 icon（audit_home_light.png x≈780 处），边界感差。建议：滚动渐隐遮罩（fade edge）或拉开与 icon 的间距。

### P1-5 日期选择双路径并存
新增页同时有「日历选择」入口与 年/月/日 三个下拉框（audit_add_light.png）。改版时二选一（建议 M3 自带 DatePicker，material3 已内嵌、零新依赖），保留一种心智模型。

### P1-6 空态/少数据态的视觉重量
仅 2 条数据时首屏大面积留白，远景行（灰字 + 小圆点）视觉重量过弱几乎不可见（audit_home_light_v2.png）。改版结合方向重排（能否把「年份分组/远景折叠」在少数据时降级为更紧凑的形态）。

---

## 3. P2 —— 细节打磨（随改版顺带）

1. **PluralsCandidate×21**：英文复数未用 plurals 资源（如「1 days」类风险，当前小组件即写死 "days later"）；中文无感，i18n 打磨项。
2. **UnusedResources×12 / UnusedAttribute×4**：资源清理。
3. **DefaultLocale×3**：`String.format` 未显式带 Locale（DateUtils.formatReminderTime 等）。
4. **UnstableCollections×9**：可变集合参数稳定性（配合改版重构顺手改 immutable）。
5. **compose-lints API 规范 12 项**：ComposeModifierMissing×8、ContentEmitter×2、ComposeParamOrder×1、ComposeModifierWithoutDefault×1——逐页改版时按 slack 规则统一，不单独返工。
6. **RestrictedApi×2**（BirthWidget.kt:81 Glance AppWidgetId 跨组访问）：评估 `@OptIn` 或替代 API。
7. **底栏选中态视觉过重**：teal 胶囊 + 黑色图标 + 加粗文字三层叠加（主观项，随 P1-1 色彩规则与改版方向一起定）。

---

## 4. 各页现状小结

- **首页**：三层卡片体系（Hero 渐变 / 标准卡 / 远景折叠行）+ 筛选 chips + 双 tab 底栏，骨架完整；问题集中在 P0-1/2（日期）、P1-1（色彩规则）、P1-4（筛选行溢出）。
- **日历**：公历+农历双行日期格、今日 coral 高亮，基础干净；问题在 P1-3（顶栏空旷/空态）与事件标记样式缺失（基线未覆盖，需造数补拍）。
- **详情**：信息卡分组（日期/提醒/备注）清晰、农历换算与生肖年龄正确；问题在 P1-2（倒计时卡配色）与底部「编辑」按钮未入视口需滚动。
- **新增/编辑**：类型选择/日历偏好/提醒配置功能齐全；问题在 P1-1（选中色混乱）、P1-5（日期选择双路径）、主按钮色（teal）与品牌色（coral）倒挂。
- **设置**：分组卡 + 分割线，层级清楚；「默认提醒时间」值用 teal 强调（同 P1-1 范畴）。
- **暗色**：整体成立（暖黑底、卡片层级、状态栏适配均正常），问题同上（P1-2 在暗色下更明显）。

## 5. 下一步（第 2 步：风格定稿，需拍板）

1. **色彩规则**：primary/选中态是否统一为 coral、teal 退居辅助？（对应 P1-1/2）
2. **改版幅度**：在现有「暖纸底 + 类型色卡片」骨架上精修，还是更大胆的视觉刷新（可先用 Google Stitch 免费出 2-3 套灵感稿对比）？
3. **优先页面**：建议顺序 首页 → 新增/编辑 → 详情 → 日历 → 设置，是否认可？
4. P0 四项是否随改版第一批先修（建议是——P0-1/P0-2 是一组的 i18n 修复，P0-3/P0-4 各一行改动）。
