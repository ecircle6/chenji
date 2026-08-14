# 辰记 · 开发待办清单（TODO）

> 本文档由「GitHub 同类项目调研 + 源码自审」产出（2026-08-14）。
> 竞品对照：MemoD（Kotlin/Compose 纪念日 App）、JeffGu98/birthday-reminder-android（双历法生日提醒）、CountdownDay（倒计时 App）、商业标杆「倒数日 Days Matter」。
> 优先级说明：P0 缺陷修复 > P1 功能对齐竞品 > P2 工程与体验现代化 > P3 远期可选。

## 项目定位（决策依据）

- **差异化护城河**：生日 + 缅怀一体、极致隐私（数据仅存本机）、自研农历引擎
- **不追平的功能**（与定位冲突，除非有真实使用反馈）：云同步、账号体系、应用锁
- 完善方向 = 修缺陷 → 补竞品已验证的需求 → 工程现代化

---

## P0 — 缺陷修复（优先做，改动小、收益直接）

- [x] **通知点击跳转详情页**（2026-08-14 完成）
  - 实现：`MainActivity` 新增 `pendingDetailId` 状态，`onCreate`（冷启动）与 `onNewIntent`（热启动）解析 `birthday_id` extra → 导航到现成的 `detail/{id}` 路由；导航完成即清空状态，同一条通知可重复跳转；记录已删时详情页自动返回首页
  - 配套：`NotificationHelper` 的 extra 键改用 `AlarmScheduler.EXTRA_BIRTHDAY_ID` 常量，消除字面量重复
  - 验收：✅ 冷启动/热启动点击通知均直达详情页；记录已删时安全回首页

- [x] **AlarmScheduler 单元测试**（2026-08-14 完成）
  - 实现：`calculateNextTriggerTime` 提取为顶层纯函数（`now` 参数可注入时钟），DetailViewModel 改调同一函数
  - 新增 `AlarmSchedulerTest.kt` 共 17 用例：当天/提前天数/自定义时刻（含 23:59、0:00 边界）/农历腊月冬月跨年/闰月缺失年降级/闰月年正常/提前 365 天窗口尾部/now 恰好等于触发点/确定性
  - 测试驱动发现并修复缺陷：阳历 2/29 在平年 `LocalDate.of` 会抛异常，`SolarDate.toLocalDate()` 降级为 2/28（闰日生日平年提前一天过），AlarmScheduler 与 EventCalc 同源一并修复
  - 验收：✅ 17 个用例全部通过，回归 5 个既有测试类无破坏

- [x] **开启 Room exportSchema 并补迁移测试**（2026-08-14 完成）
  - 实现：`AppDatabase` 开 `exportSchema = true`，schema 产物（1.json/2.json）入库到 `app/src/test/assets/`；根构建加 `androidx.room` 插件，`room.schemaDirectory` 指向同目录，schema 随 KSP 编译自动生成
  - 新增 `MigrationTest.kt`（Robolectric + 真实 SQLite）：从 1.json 建表 SQL 建 v1 库并插入老数据 → `Room.databaseBuilder` 打开触发 `MIGRATION_1_2` → Room 运行时校验表结构与实体一致、老数据完整、eventType 默认 'birthday'。改实体漏写迁移/漏升版本时该测试直接失败
  - 坑记录：① Room schema JSON 的 createSql 里表名是 `${TABLE_NAME}` 占位符，测试需替换为真实表名；② Robolectric 不合并测试源集 assets，`MigrationTestHelper` 读不到 schema → 改用 Room 自身运行时校验（RoomOpenHelper.validateMigration），不依赖 helper；③ Room 2.6.1 的 Gradle 插件没有 `checkSchema` 任务，schema 漂移靠迁移测试兜底（升级 Room 后可补）
  - 验收：✅ `assembleDebug` 产物含 schema JSON；迁移测试通过（2 用例）

---

## P1 — 功能对齐竞品（差异化收益最高）

- [x] **多级提前提醒**（2026-08-14 完成）
  - 数据：`advanceDays` 单值 → `List<Int>`（逗号分隔 TEXT + TypeConverter），v2→v3 迁移重建表（旧单值 CAST 原样转多级，语义不变：3 → [3]），与置顶共享一次迁移
  - 调度：每级别一个精确闹钟（requestCode = id×32+索引，记录间不冲突），取消时全级别 + 兼容旧单闹钟码；同一记录多级别通知共用 notificationId（后发覆盖不刷屏）
  - UI：新增/编辑页预设 chips 改多选（再点取消），自定义 dialog 支持连续添加、已添加列表可删除（上限 10 个、0~365 校验）；详情页文案"当天 · 提前3天提醒"
  - 备份：FORMAT_VERSION 2（advanceDays 数组 + pinned），decode 兼容 v1 整数格式
  - 验收：✅ 单测 22 个调度用例 + 模拟器真机双闹钟（提前3天 + 当天各一个）

- [x] **置顶功能**（2026-08-14 完成）
  - 实现：`Birthday` 加 `pinned` 字段（v2→v3 迁移新增列 DEFAULT 0，与多级提醒共享）；首页排序第一维度（置顶 → 暂停 → 倒计时）；卡片 📌 徽标；详情页 TopAppBar 置顶开关；备份 v2 格式含 pinned（判重 key 不变）
  - 验收：✅ 模拟器验证置顶记录跨重启保持在列表顶部（真机升级 v2→v3 后数据完整）

- [x] **小组件配置页**（2026-08-14 完成）
  - 实现：`birth_widget_info.xml` 加 `android:configure` → 新增 `WidgetConfigureActivity`（首次拖到桌面自动打开）：默认"自动（最近记录）"或指定某条记录，按 appWidgetId 存 SharedPreferences（`WidgetConfigStore`），选择即保存并刷新小组件；`BirthWidgetReceiver.onDeleted` 清理配置
  - 尺寸：新增 4×4 大尺寸（LARGE 250×250，显示 6 行），2×2/4×2 保持（3 行/单条大字）；xml `maxResizeHeight` 提到 400dp
  - 测试：`WidgetConfigStoreTest` 3 用例（默认 auto、按实例隔离、清除回默认）；模拟器验证配置页 UI 与存储（选"小明"→ prefs `widget_selection=2`）；小组件实际渲染需真机桌面拖拽确认（模拟器 launcher 无法自动化拖拽）
  - 验收：✅ 配置页选择 → 存储 → 自动关闭；4×4 尺寸声明生效

- [x] **月历视图**（2026-08-14 完成）
  - 实现：首页「列表/月历」SegmentedButton 切换（搜索态强制列表）；自绘月历（周一起始、每天农历标注、今天高亮、事件圆点 ≤3+「+N」、上/下月切换、标题带农历月名）；点日期弹窗列出当日事件可进详情
  - 数据：复用 `HomeViewModel.displayBirthdays`（继承关系/类型筛选），落月映射抽成 `EventCalc.eventsInMonth` 纯函数（从月初前一天往后找，天然处理农历腊月/冬月跨公历年），配 4 个单测
  - 零第三方依赖：农历标注走 `LunarCalendar.solarToLunar` + 新增 `lunarDayName`
  - 验收：✅ 模拟器验证 2026-08 农历标注（8/13 初一）、8/14 小明圆点、弹窗进详情、切月（9月·农历八月初一）

---

## P2 — 工程与体验现代化

- [x] **GitHub Actions CI**（2026-08-14 完成）
  - `.github/workflows/ci.yml`：push/PR 自动跑全部单测 + 构建 Debug 包（含 APK 产物上传）
  - `.github/workflows/release.yml`：打 `v*` tag 自动签名构建正式包并发布 GitHub Release（签名密钥走 repo Secrets：KEYSTORE_BASE64/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD）
  - 配套：`gradlew`/`gradlew.bat`/`gradle-wrapper.jar` 入库（原仓库缺失）
  - 坑记录：阿里云镜像在海外网络返回 502 导致 CI 解析失败 → `settings.gradle.kts` 按环境分流（CI=`true` 走官方源 / 本地走阿里云镜像加速）
  - 注意：wrapper 默认官方下载地址，国内网络慢时可改 gradle-wrapper.properties 为腾讯镜像

- [ ] **i18n 字符串抽取（中/英）**
  - 现状：全部文案硬编码中文，`strings.xml` 仅 4 条；`attachBaseContext` 强设中文屏蔽了 Android 13+ 按应用语言设置
  - 方案：文案迁入 `strings.xml`（中/英双语），移除强设中文逻辑
  - 参照：MemoD 中英双语跟随系统

- [x] **edge-to-edge + Material You 动态取色**（2026-08-14 完成）
  - 实现：`MainActivity` 调 `enableEdgeToEdge()`（targetSdk 35 下 Android 15 强制），`Theme.kt` 删除已废弃的 `statusBarColor` 手动涂色（保留图标明暗控制）；`ThemeStore` 加 `dynamicColor` 开关（prefs 持久化，默认关保持品牌 Coral/Teal 配色），`BirthAppTheme` 在 Android 12+ 且开启时用 `dynamicLightColorScheme/dynamicDarkColorScheme`；设置页深夜模式卡片加"动态取色"Switch（SDK<31 隐藏）
  - 验收：✅ 模拟器（Android 15）状态栏与内容衔接正常、无遮挡；开关写入 prefs 即时生效、无崩溃

- [ ] **@Preview 与 Compose UI 测试**
  - 问题：38 个主源码文件零 @Preview，UI 迭代全靠真机
  - 方案：为首页/详情/设置核心界面补 @Preview；用 Robolectric + in-memory Room 补 DAO/ViewModel 测试
  - 验收：关键界面都有 @Preview；ViewModel 核心状态变换有测试

- [ ] **分享卡片**
  - 方案：一键生成带农历文案与倒计时的图片分享卡片（缅怀类沿用庄重文案体系）
  - 参照：MemoD 带背景图分享卡

---

## P3 — 远期可选（做前先确认定位与需求）

- [x] **文档同步检测机制**（2026-08-14 完成）
  - 本地 pre-push 钩子 + GitHub Actions 云端检测，功能代码变更而 README/TODO 未同步时提醒（不阻断）
  - 脚本入库：`tools/check-docs-sync.sh`（共享判定逻辑）、`tools/install-hooks.sh`（一键安装钩子）
- [ ] **云同步**（WebDAV 自托管）——与「数据仅存本机」隐私定位冲突，需用户确认
- [ ] **私密事件密码保护**——参照 CountdownDay 模糊遮盖
- [ ] **自动更新检查**——参照 JeffGu98（GitHub Releases + SHA-256 校验 + Wi-Fi 下自动下载）
- [ ] **通知设置页**——声音/振动/频道可选（当前频道参数写死在 `BirthApp.kt`）
- [ ] **version catalog 重构**——版本号收敛到 `libs.versions.toml`
- [ ] **依赖升级**——Room 2.6.1（2023）→ 最新稳定版，与 Compose BOM 2024.12 对齐
- [ ] **备份增强**——导入逐条预览/冲突解决 UI（参照 JeffGu98 覆盖/改名/跳过）；备份包含主题设置

---

## 维护说明

- 完成一项 → 勾选 checkbox 并提交，commit message 注明「TODO: xxx」
- 新发现的缺陷直接补充到对应优先级，不另开会话
- 优先级可随时根据真实使用反馈调整
- **功能代码变更后**：本地 pre-push 钩子与 GitHub Actions（`.github/workflows/docs-sync-check.yml`）会自动检测 README.md / TODO.md 是否同步，未同步时提醒但不阻断；新电脑首次克隆后运行 `bash tools/install-hooks.sh` 安装本地钩子
