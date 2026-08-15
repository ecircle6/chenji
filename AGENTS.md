# AGENTS.md — 辰记 (chenji)

记录生日与纪念日的 Android 提醒应用：Kotlin + Jetpack Compose（Material 3）+ Room + AlarmManager + Glance 小组件，自研农历换算（无第三方依赖）。包名 `com.birthapp`，代码注释与 UI 文案全部中文，数据仅存本机、无网络权限。

## 常用命令（需 JDK 17+ 与 Android SDK）

- 构建 Debug 包：`./gradlew assembleDebug`（产物：`app/build/outputs/apk/debug/辰记_v{versionName}.apk`）
- 运行全部单元测试：`./gradlew testDebugUnitTest`（`app/src/test/`，19 个测试类 140 用例：农历/备份/筛选/事件计算/闹钟调度/迁移/Compose UI/ViewModel 等）
- 上模拟器验证：`bash tools/verify-on-emulator.sh`（一键：构建 → 启动模拟器 chenji_test → 安装（自动处理签名不匹配）→ 启动 App → 截图留档到 `app/build/verify/`）

## 目录结构（app/src/main/java/com/birthapp/）

- `data/`：Room 数据库与实体；`ui/`：各页面（home / detail / add / settings，每页 Screen + ViewModel）
- `alarm/`：AlarmManager 精确闹钟调度与开机重排；`widget/`：Glance 桌面小组件
- `backup/`：备份编解码与合并；`lunar/`：农历换算；`util/`：日期计算与文案；`notification/`、`settings/`

## 必须遵守的约定

- **依赖仓库环境分流**：`settings.gradle.kts` 在 `CI=true`（GitHub Actions）走官方源，本地走阿里云镜像（阿里云在海外返回 502）。不要改动。
- **签名密钥**：`keystore.properties` 不入库（.gitignore 已忽略）；无此文件时 release 自动退回未签名（不报错）。任何情况下不要提交密钥。
- **发版**：`versionCode` 每次 +1，`versionName` 与 GitHub release tag 一致；release 开启 R8 混淆。
- **更新说明**：每次发版必须在 `settings/Changelog.kt` 的 `all` 列表头部加一条新条目（`version` 与 `versionName` 一致），写本版新增功能/修复。Android 侧载安装器不显示升级说明，该 changelog 由升级后首启弹窗 + 设置页「版本更新说明」展示。
- **模拟器验证**：每次更改或实现功能后，必须先上模拟器实际验证效果——运行 `bash tools/verify-on-emulator.sh`，人工确认界面与交互；pre-push 钩子检测到功能代码变更时会提醒（只提醒不阻断）。签名不匹配时脚本自动卸载重装（应用数据会清空）
- **文档同步**：改 `app/src/main/` 功能代码后须同步更新 README.md / TODO.md；本地 pre-push 钩子（`bash tools/install-hooks.sh` 安装）与 CI docs-sync-check 只提醒、不阻断。
- **TODO.md 是路线图**（P0 缺陷 → P1 功能 → P2 工程 → P3 远期）：动手前先读，避免与既定方向冲突；完成任务后勾选 checkbox 并提交（"TODO: xxx"）。
- **UI 文案**：i18n 未做，文案硬编码中文（strings.xml 仅 4 条），新增沿用该写法。数据库 schema 变更必须写迁移（Room 2.6.1，exportSchema 目前为 false）。

## 已知坑

- 本地单测里 `org.json` 是 Android 框架空壳，测试已单独引入 `org.json:json`（只进测试，不进安装包）。
- Robolectric 限制：① emoji 字形与「 天后」前导空格文本节点尺寸为 0，Compose 测试断言改用存在性 + substring；② FileProvider 路径根解析不生效，shareBackup 测试只断言文件内容与事件已发出；③ UI 测试需 `@Config(application = Application::class)` 绕开 `BirthApp.onCreate` 的数据库访问（否则 "Illegal connection pointer"）
- `SwipeToDeleteBox` 底层删除图标层必须用 `matchParentSize()` 而非 `fillMaxSize()`：LazyColumn 列表项是无限高度约束，fillMaxSize 会塌缩贴顶导致图标从卡片右上角透出
- Glance 小组件运行在 RemoteViews 中，只能用 Glance 组件，不能直接搬 Compose UI。
- `design-mockup.html` / `design-card-demo.html` 为根目录的设计稿参考；分享卡片以 `design-card-demo.html` 为定稿规范。
- wrapper 默认官方下载地址，国内网络慢可改 `gradle-wrapper.properties` 为腾讯镜像。
- adb/emulator 不在 PATH：用 `local.properties` 的 `sdk.dir`（或 `tools/verify-on-emulator.sh` 自动定位）；Git Bash 下 adb 参数需 `MSYS_NO_PATHCONV=1` + `cygpath -w`（中文文件名 APK 尤其如此）。
