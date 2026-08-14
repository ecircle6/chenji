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

- [ ] **开启 Room exportSchema 并补迁移测试**
  - 问题：`AppDatabase.kt` 中 `exportSchema = false`，schema 变更无自动化校验手段
  - 方案：打开导出，把 schema 产物纳入版本库；为 v1→v2 迁移补测试（Room MigrationTestHelper）
  - 验收：`assembleDebug` 产物含 schema JSON；迁移测试通过

---

## P1 — 功能对齐竞品（差异化收益最高）

- [ ] **多级提前提醒**
  - 现状：`advanceDays` 单值，只能设一个提前天数
  - 方案：数据模型改为列表（如「提前 3 天 + 提前 1 天 + 当天」），v2→v3 数据库迁移兼容老数据，UI 支持添加/删除多级
  - 参照：MemoD「提前 N 天支持空格分隔」

- [ ] **置顶功能**
  - 方案：`Birthday` 加 `isPinned` 字段，首页列表置顶优先排序；详情页提供置顶开关
  - 参照：CountdownDay、MemoD 均有
  - 验收：置顶记录固定在列表顶部，跨重启保持

- [ ] **小组件配置页**
  - 现状：小组件只能自动展示最近记录，不能指定某人
  - 方案：`birth_widget_info.xml` 加 `configure` 属性 → 配置页选择展示哪条记录；同步补 4×4 尺寸
  - 参照：MemoD 支持 2×2/4×2/4×4 且可指定展示条目

- [ ] **月历视图**
  - 方案：首页加月历 tab（当月标记有记录的日期，农历标注，点击查看当日条目）
  - 参照：MemoD 月历视图；符合「辰记」"星/辰"产品气质
  - 注意：优先复用现有 `HomeViewModel` 数据流，避免复制查询逻辑

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

- [ ] **edge-to-edge + Material You 动态取色**
  - 问题：`Theme.kt:73-77` 手动 `statusBarColor` 在 Android 15 已废弃/失效
  - 方案：适配系统栏 insets；深色档支持 `dynamicLightColorScheme`/`dynamicDarkColorScheme` 动态取色（可设开关）
  - 验收：Android 15 真机状态栏与内容衔接正确

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
