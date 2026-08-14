# 辰记

记录生日与纪念日的 Android 提醒应用。支持农历生日换算、到日提醒通知、桌面小组件与一键数据备份，帮助你不忘每一个重要的日子。

## 功能

- **多类型记录**：生日 / 缅怀 / 纪念日，支持农历与阳历
- **首页筛选**：按类型胶囊与关系标签快速过滤
- **提醒通知**：提前 / 当天通知，重启、改时间后自动重排闹钟
- **桌面小组件**：首页直接查看最近的生日与纪念日
- **数据备份**：导出 / 分享 / 导入备份文件，方便换机同步
- **主题与图标**：弯月星辰图标，支持主题配色

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
