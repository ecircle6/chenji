# 辰记

记录生日与纪念日的 Android 提醒应用。支持农历生日换算、到日提醒通知、桌面小组件与一键数据备份，帮助你不忘每一个重要的日子。

![CI](https://github.com/ecircle6/chenji/actions/workflows/ci.yml/badge.svg)

## 功能

- **多类型记录**：生日 / 缅怀 / 纪念日，支持农历与阳历
- **首页全新改版**：底部「首页/日历」双页签，日历独立成页展示全部记录；页首问候语 + 最近事件 Hero 聚焦卡 + 紧凑卡片布局（类型色条 / 类型标签 / 日期·关系）
- **多维筛选**：快捷胶囊（全部/关系/类型）+「更多筛选」底部面板，支持关系、类型、生肖三维叠加；支持置顶固定重要记录
- **提醒通知**：多级提前提醒（如提前 3 天 + 当天），点通知直达记录详情页，重启、改时间后自动重排闹钟；通知设置页可调默认提醒时间与总开关
- **桌面小组件**：2×2 / 4×2 / 4×4 三档尺寸，可配置展示最近记录或指定某人
- **数据备份**：导出 / 分享 / 导入备份文件，导入前逐条预览（跳过/覆盖/导入），备份附带主题设置，方便换机同步
- **主题与图标**：弯月星辰图标，支持浅色/深色/跟随系统与动态取色（Material You）
- **分享卡片**：1080×1920 竖版精美卡片，极光毛玻璃（生日/纪念日）与深夜烛火（缅怀）双风格，一键发微信/朋友圈/小红书
- **版本更新说明**：升级后首次打开自动弹窗展示本版更新内容，设置页「关于」可随时查看全部版本历史

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- Room 数据库 + ViewModel
- AlarmManager 精确闹钟 + 桌面小组件（AppWidget）
- 原生实现农历换算，无第三方依赖

## 构建

```bash
# 需要 JDK 17+ 与 Android SDK
./gradlew assembleDebug
```

## 持续集成与发布

- **CI**（`.github/workflows/ci.yml`）：每次 push/PR 自动运行全部单元测试（农历/备份/筛选/事件计算/闹钟调度/迁移/Compose UI/ViewModel 共 22 个测试类 152 用例）+ 构建 Debug 包
- **Release**（`.github/workflows/release.yml`）：打 `v*` tag 自动签名构建正式包并发布 GitHub Release：

```bash
git tag v2.2.0 && git push origin v2.2.0   # 版本号与 app/build.gradle.kts 中的 versionName 一致
```

- 签名密钥通过仓库 Secrets 注入（`KEYSTORE_BASE64` / `STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`），不入版本库

> 签名密钥（`*.jks` / `keystore.properties`）不入版本库，换机开发时请单独备份，否则无法对已安装的 App 做覆盖升级。

## 项目结构

```
app/src/main/java/com/birthapp/
├── data/        数据库与实体（Room）
├── ui/          各页面（首页 / 详情 / 新增 / 设置）
├── alarm/       精确闹钟调度与开机重排
├── widget/      桌面小组件
├── backup/      备份编解码与合并
├── lunar/       农历换算
└── util/        日期计算与文案工具
```

## 文档同步检测

每次提交功能代码（`app/src/main/` 等）时，自动检测 **README.md / TODO.md** 是否同步更新，防止文档过时。**只提醒，不阻断**。

- **本地钩子**（推送前即时提醒）：每台电脑首次克隆后运行一次 `bash tools/install-hooks.sh`
- **云端 CI**（push/PR 自动检测，任何电脑推送都生效）：见 `.github/workflows/docs-sync-check.yml`，提醒显示在 Actions 摘要页
- 共享逻辑：`tools/check-docs-sync.sh`（本地与 CI 同一份判定规则）

## 开发待办

完善路线图见 [TODO.md](TODO.md)（P0 缺陷修复 → P1 功能补齐 → P2 工程现代化 → P3 远期）。
