# UI 改版工具与技能选型调研报告

> 调研日期：2026-09-05。数据核实方式见附录 C。约束前提：仅免费/开源工具；目标应用为「辰记」（Kotlin + Jetpack Compose + Material 3）；设计工具偏好为「不引入，Agent 直接出码」。

---

## 1. TL;DR 结论摘要

1. **本次调研最重要的发现是生态错位**：当前热门的 AI UI 生成产品（v0、Lovable、bolt.new、Figma Make、Onlook、Framer 等）全部只输出 Web 技术栈（HTML/CSS/React），没有任何一款能直接产出 Jetpack Compose 代码；GitHub 上「截图/设计稿转 Compose」方向也没有成熟的开源项目（经 GitHub 检索实证，见 §6）。
2. 因此对辰记这类纯 Compose 应用，**可行路径收敛为「本地 Agent + 设计技能包直接改 Compose 代码」**，设计稿/建议类工具只作为上游灵感与评审环节。
3. **推荐主线（方案 A）**：沿用仓库既有的「HTML 效果图 → Compose 落地」工作流（根目录已有 9 份设计稿且 `Color.kt` 与之同源），用 ZCode + 本地已装技能（android-compose-design / material-3 / edge-to-edge）改代码，每轮走 `tools/verify-on-emulator.sh` 截图 + 视觉评审闭环，上线前用 Google Accessibility Scanner 与 slackhq/compose-lints 收尾。**全程零新增成本。**
4. **推荐执行顺序（吸收方案 C）**：先审计出问题清单（截图基线 + Accessibility Scanner + compose-lints），再按方案 A 逐页改版——比一次性全局重写风险更低。
5. **明确排除项**：Motiff/妙多已于 2026-07-31 关停（一票否决）；material-theme-builder 已归档；FigmaToCompose、design-lint、compose-material3-datetime-pickers 等已停更，均不可选型。
6. **不需要补充任何信息**：技术栈版本、主题体系、设计稿现状均已从仓库直接查明（§2）；改版实施时的基线截图用 `tools/verify-on-emulator.sh` 生成即可。
7. **后续想动手优化时怎么操作**：一次性准备（compose-lints / MaterialKolor / superdesign-skill / Scanner）与逐页改版的可复制指令模板见 §9；其中 §9.1 已于 2026-09-05 执行完毕，结论直接可查。

---

## 2. 现状底稿（选型的前提事实）

### 2.1 技术栈版本（来源：`build.gradle.kts`，无 version catalog）

| 项 | 版本 | 备注 |
|---|---|---|
| AGP | 8.7.3 | 根 `build.gradle.kts:2` |
| Kotlin | 2.0.21（含 Compose 编译器插件） | 根 `build.gradle.kts:3-4` |
| Compose BOM | 2024.12.01 | `app/build.gradle.kts:98`；material3 走 BOM（本机 AAR 实测 1.3.1） |
| minSdk / targetSdk / compileSdk | 26 / 35 / 35 | `app/build.gradle.kts:27,31-32` |
| glance-appwidget | 1.1.1 | `app/build.gradle.kts:134` |
| versionCode / versionName | 23 / "2.1.20" | `app/build.gradle.kts:35-36` |

对选型的影响：引入任何新 UI 库前需确认与 BOM 2024.12.01 / Kotlin 2.0.21 的兼容性；版本号硬编码在各 gradle 文件中（TODO.md P3 已列 version catalog 重构待办）。

### 2.2 UI 体系现状

- **页面**：home（`HomeScreen.kt` 727 行）/ detail（457）/ add（`AddEditScreen.kt` 1,080，全库最大）/ settings（745）/ calendar（348）/ share（`ShareCardGenerator.kt` 401，画布生成 1080×1920 分享卡）；`ui/` 共 24 文件约 6,600 行，Glance 小组件 10 文件约 880 行。
- **共享组件**：`ui/common/SharedComponents.kt`（BirthdayCard / UrgentCard / DistantRow / 空态）、`SwipeToDeleteBox.kt`、`Motion.kt`（错峰入场 + odometer 数字滚动 + 「今天」呼吸光晕）、`EventTypeStyle.kt`（类型→配色映射）。
- **主题体系已高度系统化**（`ui/theme/`）：亮/暗两套手工全量 `colorScheme`（品牌三色 Coral #FF6B6B / Teal #00BFA5 / SunnyYellow #FFD93D + 暖纸底）；语义 Shapes 四档（8/12/16/28dp，注释明确「全局圆角只允许走这里」）；自定义 Typography（display 字阶 + 倒计时数字 tabular-nums）；动态取色已支持但默认关（`Theme.kt:101,107-109`、`settings/ThemeStore.kt:32`）；Glance 小组件独立配色（`widget/WidgetTheme.kt`，day/night 双值 ColorProvider）。
- **i18n**：`values/strings.xml` 与 `values-en/strings.xml` 各 283 条一一对应；界面文案 198 处 `stringResource`，残余中文字面量多为农历术语与 @Preview 标签。改版新增文案需同步维护双语资源。

### 2.3 既定工作流（选型的核心依据）

仓库根目录已有 **9 份 HTML 设计稿**：`design-mockup.html`（改版方案定稿）、`design-card-demo.html`（分享卡定稿）、`home-redesign-mockup.html`、`home-filter-mockup.html`、`time_memory_home.html`、`hero-redesign-mockup.html`、`design-optimization-preview.html` 等，且 `Color.kt` 的颜色常量与 `design-mockup.html` 的 CSS 变量同源、Hero 渐变注明「home-redesign-mockup.html 定稿值」。

**结论：「HTML 效果图（定稿）→ Agent/人工转译 Compose → 模拟器验证」已经是被验证过的协作方式**，选型应放大这条路径而不是另起炉灶。TODO.md 当前无挂起的 UI/视觉待办，本次改版属新立项。

---

## 3. 类别一：可直接生成/改版代码的工具

| 工具 | 核心能力 | 输出技术栈 | Agent 工作流集成 | 授权与成本 | 上手门槛 | 结论 |
|---|---|---|---|---|---|---|
| **ZCode + 本地技能包**（android-compose-design / material-3 / edge-to-edge，已安装） | 直接读改 Compose 代码；android-compose-design 专治「AI 默认审美」，material-3 覆盖 tokens/30+ 组件/自适应布局 | **Kotlin/Compose（原生）** | 已在本工作区内，零集成成本 | 技能文件随本地环境，无订阅 | 无（已在用） | **首选** |
| **Android Studio 内 Gemini（Agent Mode）** | IDE 内多步骤任务：改 UI、跑构建、修错（[官方文档](https://developer.android.com/studio/gemini/agent-mode)） | **Kotlin/Compose（原生）** | Android Studio 原生，可配第三方模型 key | 个人开发者/学生免费档（配额有限，具体数字未能核实） | 低 | **次选/备用** |
| **Google Stitch**（[官网](https://stitch.withgoogle.com/)） | 文本/图片生成高保真 UI 设计稿 + 前端代码（[官方公告](https://developers.googleblog.com/stitch-a-new-way-to-design-uis/)） | 设计稿 + HTML/CSS/Tailwind（仅 Web）；可导出 Figma | 无官方 MCP（第三方说法未能核实）；产物可作 HTML 稿喂给 Agent | 当前完全免费（Google Labs 实验，限额未能核实） | 低 | **灵感/效果图源** |
| v0（[定价](https://v0.app/pricing)）/ Lovable / bolt.new | 聊天生成全栈 Web 应用 | 仅 React/Next 等 Web | — | v0 免费 $5 credits/月；bolt.new 免费 30 万 tokens/天 | 低 | ❌ 技术栈不匹配 |
| Onlook（[GitHub](https://github.com/onlook-dev/onlook)，26,642★，Apache-2.0，活跃） | 「设计师的 Cursor」，可视化编辑真实 React 应用 | 仅 React | 本地桌面应用 | 开源免费 | 中 | ❌ 仅 Web |
| Figma Make（[定价](https://www.figma.com/pricing/)） | 提示词生成 web app 原型 | 仅 Web | Figma 生态 | 仅含在付费 Full seat（$16/月起） | 中 | ❌ 不匹配且超预算 |

**选择理由**：类别一的关键筛选条件是「能否产出 Kotlin/Compose」。只有本地 Agent（ZCode 已装技能）与 Android Studio Gemini 满足；Stitch 虽不产 Compose，但其免费产物恰好落进仓库既有的「HTML 稿→Compose」工作流上游，故列为灵感源而非直接出码工具。

---

## 4. 类别二：优化建议与设计评审工具

| 工具 | 核心能力 | 集成方式 | 授权与成本 | 结论 |
|---|---|---|---|---|
| **ZCode 内置视觉评审 + `tools/verify-on-emulator.sh` 截图闭环** | 每轮改版后自动构建→装模拟器→截图留档 `app/build/verify/`，配合视觉评审给出改稿意见 | 已在本仓库脚本内 | 零成本 | **改版主评审手段（首选）** |
| **Google Accessibility Scanner**（[Play 商店](https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor)、[使用说明](https://support.google.com/accessibility/android/answer/6376570)） | 扫描已安装 App 当前界面，给出触控目标尺寸、对比度、内容标签等无障碍改进建议 | 独立 Android App，扫真机/模拟器上的辰记 | 免费（Google 出品） | **上线前审计（推荐）** |
| **superdesign-skill**（[GitHub](https://github.com/superdesigndev/superdesign-skill)，512★，MIT，推送至 2026-08-21） | 给 coding agent 注入设计判断力（风格探索、布局与层次 critique），生成 HTML 原型稿 | `npx skills add` 装入 agent 技能目录 | MIT 开源免费 | **可选增强（推荐试装）** |
| **anthropics/skills 官方技能**（[GitHub](https://github.com/anthropics/skills)，174,374★） | `frontend-design` / `theme-factory` / `canvas-design` / `brand-guidelines` 四个设计类 skill（已逐一确认存在） | 直接放入 agent skills 目录 | 仓库 license 字段为 null（未标注协议），复用注意 | 参考借鉴 |
| Figma 插件 Design Lint（[插件页](https://www.figma.com/community/plugin/801195587640428208/design-lint)，源码 [destefanis/design-lint](https://github.com/destefanis/design-lint) 518★） | 查设计稿缺失样式/错误圆角等 | Figma 插件 | 免费开源，**2024-01 后停更** | 仅在有 Figma 环节时用 |
| Figma 插件 Stark（[定价](https://www.getstark.co/pricing/)） | 对比度/焦点顺序/视觉障碍模拟 | Figma 插件 | 免费档功能有限，付费 $198/人/年起 | 不推荐（超预算） |

**选择理由**：评审环节的价值取决于「能否看到真实渲染结果」。模拟器截图闭环直接评审真实 Compose 渲染，Accessibility Scanner 提供权威的无障碍基线，两者组合已覆盖视觉与可达性两类问题；superdesign-skill 是唯一活跃且协议干净的第三方设计判断力 skill，成本低可试装。

---

## 5. 类别三：设计规范与组件库（开源）

| 项目 | 链接 | Stars | 最近推送 | 协议 | 对辰记的价值 |
|---|---|---|---|---|---|
| **Material 3 官方体系**（material-3 技能已装） | [developer.android.com](https://developer.android.com/develop/ui/compose/designsystems/material3) | — | 随 AOSP | — | **设计语言基准**；实测 material3 1.3.1 AAR 已内嵌 TonalPalette 等动态取色核心，`dynamicLight/DarkColorScheme` 开箱即用 |
| **MaterialKolor**（jordond） | [github.com/jordond/MaterialKolor](https://github.com/jordond/MaterialKolor) | 933 | 2026-09-04（活跃） | MIT | **改版换色首选**：种子色一键生成全套 M3 亮/暗 colorScheme，是 material-color-utilities 的 Kotlin 独立实现；可直接替换 `ui/theme/Color.kt` 手工调色 |
| **Now in Android**（Google 官方） | [github.com/android/nowinandroid](https://github.com/android/nowinandroid) | 21,766 | 2026-09-05（当日仍推送） | Apache-2.0 | **设计系统实践标杆**：主题分层、组件用法、暗色与动态取色的官方参考实现 |
| **compose-lints**（Slack） | [github.com/slackhq/compose-lints](https://github.com/slackhq/compose-lints) | 511 | 2026-09-04（活跃） | Apache-2.0 | Compose 代码健康度 lint，改版重构时防回归 |
| **ComposeCookBook**（Gurupreet） | [github.com/Gurupreet/ComposeCookBook](https://github.com/Gurupreet/ComposeCookBook) | 6,876 | 2026-06-28 | MIT | 组件/动效示例大合集，找实现灵感 |
| colorpicker-compose（skydoves） | [github.com/skydoves/colorpicker-compose](https://github.com/skydoves/colorpicker-compose) | 758 | 2026-09-02 | Apache-2.0 | 若改版加「主题色自定义」功能的取色组件 |
| material-color-utilities（官方） | [github.com/material-foundation/material-color-utilities](https://github.com/material-foundation/material-color-utilities) | 2,258 | 2026-08-21 | Apache-2.0 | 完整 MCU 算法类（Hct/Quantize 等）未随 M3 AAR 发布，需要全量算法时用它或 MaterialKolor |

日期/时间选择器补充（辰记是生日/纪念日 App）：**优先用 M3 自带 DatePicker/TimePicker**（material3 1.3.1 已确认包含）；第三方活跃的少——compose-material-dialogs（608★，2025-12）、WheelPickerCompose（622★，2024-05 停更）、compose-material3-datetime-pickers（158★，2024-10 停更）仅在前两者不够时考虑。

**选择理由**：辰记主题层已系统化，改版需要的是「更权威的基准 + 更高效的配色工具」而不是引入组件库框架。Material 3 官方 + Now in Android 提供基准，MaterialKolor 解决改版最费时的调色环节，compose-lints 兜住重构质量——四者均零成本、活跃维护、协议宽松。

---

## 6. 不匹配与风险清单（选型避坑）

### 6.1 一票否决（关停/归档）
| 项 | 状态 | 依据 |
|---|---|---|
| **Motiff / 妙多** | **已停运**：国内版 2026-07-31 停止服务，国际版 2026-06-23 关闭，2026-11-01 未导出数据永久删除 | [关停公告](https://miaoduo.com/help/others/489912186378811) |
| material-theme-builder | **已归档**（GitHub archived） | GitHub API，597★ |
| Google Relay（Figma→Compose） | 已淡出（无活跃官方方案） | 与 FigmaToCompose 现状互证 |

### 6.2 技术栈不匹配（仅 Web，不能产出 Compose）
abi/screenshot-to-code（73,388★，MIT，活跃）、wandb/openui（22,536★，Apache-2.0，活跃）、onlook-dev/onlook（26,642★）、stackblitz-labs/bolt.diy（19,846★，7 个月未推送趋冷）、v0、Lovable、bolt.new、Framer AI、Uizard（2024 年被 Miro 收购，产品仍独立运营，Pro $12/月）、Figma Make、Pixso（React/Vue 输出）、即时设计、21st.dev Magic MCP（仅 React）。它们对辰记的价值仅限「交互思路参考」（如 screenshot-to-code 的多候选生成 + 视觉比对流程）。

**专项结论：「截图/设计稿 → Compose 代码」无成熟开源方案**——GitHub 检索「screenshot to jetpack compose」「figma to compose」仅命中 CaelumF/FigmaToCompose（451★，2023-10 停更，无 license）、google/automotive-design-compose（206★，Apache-2.0，活跃但面向车载场景，普通 App 接入成本高）。

### 6.3 协议/维护风险项
- **anthropics/skills**（174k★）：仓库 license 字段为 null（未标注开源协议），复用其 skill 内容需注意权利保留；
- **MasterGo MCP 仓库**（mastergo-design/mastergo-magic-mcp，283★）：无 license 标注；且 MasterGo AI 功能按积分付费（¥125/月起），与本次免费约束不符；
- **superdesign 主仓库**（superdesigndev/superdesign，6,934★）：license 为 Other/NOASSERTION，2026-06 后更新放缓；其 **superdesign-skill 子仓库（MIT）不受影响**；
- **Penpot MCP**（penpot/penpot-mcp，480★，MPL-2.0）：2026-03 后更新放缓；
- wandb/openui、bolt.diy 等虽 star 高，活跃度分层已在 §6.2 标注。

---

## 7. 组合方案与推荐结论

### 方案 A「零成本 Agent 原生改版」——推荐主线
**适用**：想一次成体系地刷新全局视觉，且接受由 Agent 直接产出 Compose 代码。
**组成**：存量 HTML 设计稿（必要时 Google Stitch 补灵感稿）→ ZCode + 已装技能改 `ui/theme` 与各页面 → 每轮模拟器截图评审迭代 → Accessibility Scanner + compose-lints 收尾。
**成本**：0 元。**风险**：Agent 改稿质量依赖技能与评审闭环，靠第 8 节的迭代机制兜底。

### 方案 B「设计稿先行 + MCP 喂稿」——备选
**适用**：希望先在可视化工具里反复比稿再落码。
**组成**：Penpot（免费开源）+ 官方 MCP（MPL-2.0）或 Framelink MCP（[GLips/Figma-Context-MCP](https://github.com/GLips/Figma-Context-MCP)，15,780★，MIT，免费）读取设计稿结构 → Agent 转 Compose；Figma 官方 Dev Mode MCP 全席位可连但 View/Collab 席位仅 6-20 次/月，Dev seat $12/月才实用（[官方文档](https://developers.figma.com/docs/figma-mcp-server/)），**超出本次免费预算，仅作参考**。
**成本**：0 元（Penpot/Framelink 路径）。**风险**：多一道「设计稿↔代码」同步成本；且与你「不引入设计工具」的偏好相悖，故仅备选。

### 方案 C「审计驱动渐进优化」——稳妥补线（并入主线执行）
**适用**：不想一次大改，按版本节奏逐步推进。
**组成**：先产出问题清单——① 模拟器全页面截图基线（`tools/verify-on-emulator.sh`）② Accessibility Scanner 扫描 ③ compose-lints 接入 ④（可选）superdesign-skill 做风格 critique → 按优先级排序 → 逐项以方案 A 的方式改版，每版一条 changelog。
**成本**：0 元。

### ★ 推荐结论
**以方案 A 为主线、方案 C 的审计步骤作为起点：先审计出清单，再按 A 执行。** 理由：
1. 预算约束为免费/开源，且组合内所有环节（技能、脚本、开源库、Scanner）均零成本、已验证活跃；
2. Compose 技术栈使全部主流 Web 系 AI UI 产品失效，「截图转 Compose」也无开源方案，本地 Agent + 设计技能是唯一能直接产出生产代码的路径；
3. 仓库已具备该路径的全部基础设施（9 份 HTML 稿的既定工作流、verify-on-emulator.sh、体系化的 `ui/theme`），边际成本最低。

---

## 8. 实施步骤（推荐组合的具体落地清单）

1. **审计基线**：运行 `bash tools/verify-on-emulator.sh` 生成各页面截图留档；模拟器装 Google Accessibility Scanner 扫一轮现有 App；接入 slackhq/compose-lints 跑一次基线报告 → 汇总成改版问题清单（P0 对比度/触控目标 → P1 视觉层次 → P2 细节）。
2. **风格定义**：整理根目录 9 份 HTML 稿为目标视觉规范；缺口部分（如整体氛围、新页面）用 Google Stitch 免费生成 2-3 套灵感稿，人工挑选后仿照 `design-mockup.html` 的做法固化为定稿 HTML（CSS 变量与 `Color.kt` 保持同源）。
3. **主题层先行**：只改 `ui/theme/`（Color/Type/Shape 单一来源）。换配色用 MaterialKolor 以品牌 Coral #FF6B6B 为种子色生成全套亮/暗 scheme，人工微调后写入；保持语义 Shapes 与 CompositionLocal 不旁路。
4. **逐页改版**：按「首页 → 新增/编辑 → 详情 → 设置 → 日历 → 分享卡 → 小组件」顺序，每页用 ZCode + android-compose-design / material-3 技能改代码；新增文案同步 `strings.xml`（中/英 283×2）。
5. **每轮评审闭环**：每完成一页跑 `tools/verify-on-emulator.sh`，截图 + 视觉评审 + Accessibility Scanner 复扫该页，不达标回到第 4 步。
6. **收尾**：全页通过后按 AGENTS.md 约定走发版流程（versionCode +1、Changelog.kt 新条目、README/TODO 同步）；TODO.md 立项「UI 全局改版」条目记录本轮范围与遗留项。

---

## 9. 落地使用手册（后续优化时怎么用）

> 本节回答「选型确定后，实际开工时怎么操作」：§9.1 是一次性准备（全部可选，跳过也能直接开工），§9.2 是每次改版时可直接照抄的指令模板。

### 9.1 一次性准备（✅ 2026-09-05 已执行，以下为实际结论）

1. **compose-lints —— ✅ 已接入，版本锁定 1.4.2**
   - 版本实证：1.6.0 与 1.5.5 均面向 AGP 9.3 工具链构建（1.6.0 release 原文 "requires AGP/lint 9.3+"；1.5.5 build against lint 32.3.2），辰记 AGP 8.7.3 不可用；**1.4.2**（release 注明 build against lint 31.7.1 + Kotlin 2.0.21）与辰记完全匹配。项目未来升级 AGP 9.x 后可升 1.6.x。
   - `app/build.gradle.kts` 已加：`lintChecks("com.slack.lint.compose:compose-lint-checks:1.4.2")`。
   - **lint 基线（2026-09-05，`./gradlew lintDebug`，共 75 项）**：
     * compose-lints 新增发现 24 项（14 error + 10 warning）：`ComposeModifierMissing`×8（各页根组件缺 modifier 参数，AddEditScreen:85 / CalendarScreenPage:37 / DetailScreen:98 / HomeScreen:131 / MainNavigationBar:28 / SettingsScreen:177 / SharedComponents:197、235）、`Modifier.composed` 性能反模式×2（Motion.kt:63、142）、`ContentEmitter`×2（AddEditScreen:776、858）、`ComposeParamOrder`×1（AddEditScreen:45）、`ComposeModifierWithoutDefault`×1（BirthWidget:252）、`UnstableCollections`×9（List 参数稳定性）、`ComposeCompositionLocalUsage`×1（Theme.kt:92 LocalDarkTheme，有意设计）；
     * Android 官方 lint 存量 51 项（6 error + 45 warning），其中 **P0 苗头（改版前值得先修）**：`StringFormatMatches`×3——`date_solar_full_en` / `share_date_line_en` 英文格式串与参数类型/数量不匹配（DateUtils.kt:52、ShareCardGenerator.kt:383，疑似真 bug）、`MissingSuperCall`（BirthWidgetReceiver.kt:97 未调 `super.onAppWidgetOptionsChanged`，影响小组件尺寸变化回调）；其余为 PluralsCandidate×21（复数化候选）、UnusedResources×12、UnusedAttribute×4、DefaultLocale×3 等。
   - ⚠️ 存量 error 会使 `./gradlew lintDebug` 失败中止（接入前即如此——CI 只跑单测所以未暴露）。是否建 `lint { baseline = ... }` 快照或调整严重级，留到改版阶段决策，本次未改 lint 配置。
2. **MaterialKolor —— ✅ 兼容性已实证：仅 1.7.1 可用**
   - 实测（2026-09-05，AGP 8.7.3 / Kotlin 2.0.21 / BOM 2024.12.01，临时加依赖试装后已还原）：
     * **5.0.1 ❌**：传递依赖 Compose 1.12.0，`checkDebugAarMetadata` 直接失败（要求 AGP 9.1.0+），且其 Kotlin 2.4.x metadata 亦不兼容 Kotlin 2.0.21；
     * **1.7.1 ✅**：Kotlin 2.0.20 构建，`assembleDebug` 通过，无任何冲突；
     * 2.x 全线不兼容（2.0.2 = Kotlin 2.1.10 起）。
   - 改版换色时三选一：① 引入 1.7.1（`implementation("com.materialkolor:material-kolor:1.7.1")`，`rememberDynamicColorScheme(seedColor, isDark)`，功能够用）；② 用 M3 自带 `dynamicLight/DarkColorScheme`（零依赖，`Theme.kt` 已预留开关）；③ 升级项目 Kotlin/AGP 后用新版（工程量大，需另立项）。
3. **superdesign-skill —— ⏸ 跳过（装法留档）**：需 npm 全局 CLI + 外部服务账号登录，与「不引入设计工具」的偏好不符。要装时：`npm install -g @superdesign/cli@latest` → `superdesign login` → `npx skills add superdesigndev/superdesign-skill`，以 `/superdesign <需求>` 显式调用。
4. **Google Accessibility Scanner —— 路径已定案：单测兜底 + 真机可选**：chenji_test 模拟器为 google_apis 镜像（config.ini `PlayStore.enabled=no`、`image.sysdir.1=...google_apis...`），无 Play 商店，无法从 Play 安装 Scanner。改版审计改用 Compose/Robolectric 无障碍断言兜底（`ui-test-junit4` 已在依赖，具体接入第 1 步审计时按官方文档核实落地）；真机侧装 Scanner（`com.google.android.apps.accessibility.auditor`）作为可选增强（辰记 APK 本就是侧载流程）。

### 9.2 每次改版的操作流程（可照抄的指令模板）

**技能触发说明**：`android-compose-design` / `material-3` / `edge-to-edge` 三个本地技能会在「构建/优化 Compose UI」类请求中自动加载；也可在指令里点名（如「按 android-compose-design 技能…」）强制触发。改版节奏永远是：**先主题后页面、每页一轮验证**。

- **第 1 步 · 审计出清单**（先跑 `bash tools/verify-on-emulator.sh` 留档截图，再对我说）：
  > 读取 app/build/verify 下最新截图，对辰记现有界面做视觉审计：按对比度、视觉层次、间距一致性、暗色适配逐页找问题，输出 P0/P1/P2 问题清单，先不改代码。
- **第 2 步 · 风格定稿**：
  > 修改根目录 design-mockup.html：<具体调整>，保持 CSS 变量与 ui/theme/Color.kt 同源，输出后我用浏览器预览定稿。

  缺灵感时用 Google Stitch（免费）生成 2-3 套，人工挑一张后仿照 `design-mockup.html` 的格式固化为仓库内定稿。
- **第 3 步 · 主题层先行**（只改 `ui/theme/` 单一来源）：
  > 按 android-compose-design 技能，以 Coral #FF6B6B 为种子色重新生成亮/暗两套 colorScheme 并更新 Theme.kt/Color.kt；EventTypeStyle 的类型色语义保持不变；语义 Shapes 与 Typography 不旁路。
- **第 4 步 · 逐页改版**（每页一轮，顺序：首页 → 新增/编辑 → 详情 → 设置 → 日历 → 分享卡 → 小组件）：
  > 把 <设计稿文件名>.html 落地为 <页面名> 的 Compose 实现：只改 ui/<页面>/ 相关文件；新文案走 stringResource 并同步 values/strings.xml 与 values-en 两份；沿用 ui/theme 的语义 Shapes 与 Typography；改完跑 bash tools/verify-on-emulator.sh 截图给我对比。
- **第 5 步 · 每轮验证闭环**：脚本截图 → 我做视觉评审并给出修改点 → 确认后迭代；每页完成后跑 `./gradlew testDebugUnitTest`（README 记载全库 220 用例）确保不回归。
- **第 6 步 · 收尾发版**：按 AGENTS.md 约定执行——`versionCode` +1、`Changelog.kt` 的 `all` 列表头部加新条目、README.md / TODO.md 同步更新、TODO.md 立项条目勾选并提交。

## 附录 A：链接与协议汇总表

| 名称 | 链接 | 协议/价格 | 状态（2026-09-05） |
|---|---|---|---|
| Google Stitch | https://stitch.withgoogle.com/ | 免费（Google Labs 实验） | 运营中，实验性质 |
| Figma Dev Mode MCP | https://developers.figma.com/docs/figma-mcp-server/ | 全席位可连，限额 6-600 次/天；Dev seat $12/月起 | 运营中 |
| Framelink Figma-Context-MCP | https://github.com/GLips/Figma-Context-MCP | MIT | 15,780★，活跃 |
| MasterGo Magic MCP | https://github.com/mastergo-design/mastergo-magic-mcp | **无 license 标注** | 283★，更新中；AI 积分 ¥125/月起 |
| Penpot MCP | https://github.com/penpot/penpot-mcp | MPL-2.0 | 480★，2026-03 后放缓 |
| chrome-devtools-mcp | https://github.com/ChromeDevTools/chrome-devtools-mcp | Apache-2.0 | 51,000★，活跃（仅 Web/WebView 场景） |
| 21st.dev Magic MCP | https://github.com/21st-dev/magic-mcp | ISC（服务订阅收费） | 5,798★，仅 React |
| superdesign-skill | https://github.com/superdesigndev/superdesign-skill | MIT | 512★，活跃 |
| superdesign（主仓库） | https://github.com/superdesigndev/superdesign | Other/NOASSERTION | 6,934★，放缓 |
| anthropics/skills | https://github.com/anthropics/skills | **license 字段 null** | 174,374★，活跃 |
| awesome-claude-skills | https://github.com/ComposioHQ/awesome-claude-skills | 无 license | 74,511★，清单类 |
| abi/screenshot-to-code | https://github.com/abi/screenshot-to-code | MIT | 73,388★，活跃（仅 Web） |
| wandb/openui | https://github.com/wandb/openui | Apache-2.0 | 22,536★，活跃（仅 Web） |
| onlook-dev/onlook | https://github.com/onlook-dev/onlook | Apache-2.0 | 26,642★，活跃（仅 React） |
| stackblitz-labs/bolt.diy | https://github.com/stackblitz-labs/bolt.diy | MIT | 19,846★，7 个月未推送 |
| android/nowinandroid | https://github.com/android/nowinandroid | Apache-2.0 | 21,766★，活跃 |
| Gurupreet/ComposeCookBook | https://github.com/Gurupreet/ComposeCookBook | MIT | 6,876★，活跃 |
| jordond/MaterialKolor | https://github.com/jordond/MaterialKolor | MIT | 933★，活跃（最新 5.0.1 需 AGP 9.1+/Kotlin 2.4，**本项目实测可用 1.7.1**，2026-09-05） |
| skydoves/colorpicker-compose | https://github.com/skydoves/colorpicker-compose | Apache-2.0 | 758★，活跃 |
| slackhq/compose-lints | https://github.com/slackhq/compose-lints | Apache-2.0 | 511★，活跃（1.6.0/1.5.x 需 AGP 9.3+，**本项目已接入 1.4.2**，2026-09-05） |
| material-foundation/material-color-utilities | https://github.com/material-foundation/material-color-utilities | Apache-2.0 | 2,258★，活跃 |
| material-foundation/material-theme-builder | https://github.com/material-foundation/material-theme-builder | Apache-2.0 | 597★，**已归档** |
| CaelumF/FigmaToCompose | https://github.com/CaelumF/FigmaToCompose | 无 license | 451★，**2023-10 停更** |
| google/automotive-design-compose | https://github.com/google/automotive-design-compose | Apache-2.0 | 206★，活跃（车载场景） |
| destefanis/design-lint | https://github.com/destefanis/design-lint | MIT | 518★，**2024-01 停更** |
| Google Accessibility Scanner | https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.auditor | 免费 | 在架 |
| Android Studio Gemini | https://developer.android.com/studio/gemini/overview | 个人免费档（配额未核实） | 运营中 |
| Motiff/妙多 | https://miaoduo.com/help/others/489912186378811 | — | **已关停（2026-07-31）** |
| Uizard | https://uizard.io/pricing/ | Free 档 / Pro $12/月 | 运营中（Miro 旗下） |
| v0 / Lovable / bolt.new / Framer | https://v0.app/pricing 等 | 各有免费档 | 运营中（均仅 Web） |

## 附录 B：未能核实项（如实声明）
- Google Stitch 官方生成限额数字与「官方 MCP」是否存在（第三方博客说法，未获官方文档佐证）；
- Android Studio Gemini 免费版 Agent Mode 的具体配额数字；
- 即时设计付费版具体价格（定价页 JS 渲染无法抓取）、Lovable Pro 官方美元定价、MasterGo 官方页席位具体数字、Onlook 云付费档价格；
- Figma Make 输出框架（官方页仅称 "web app"，React 为社区说法）。

## 附录 C：数据核实方法
- GitHub 数据（stars/最近推送/协议/archived）：2026-09-05 通过 GitHub API（`api.github.com/repos/*`）逐仓抓取；MaterialKolor、Now in Android、Figma-Context-MCP 三项经第二轮独立抓取交叉验证，数值一致；
- 产品能力/价格：WebSearch + 官方定价页/文档页抓取（2026-09-05 快照）；Google 系域名与部分 JS 渲染页无法直抓的，以官方页搜索摘要核实并标注；
- 代码库事实：本仓库 `build.gradle.kts`、`ui/`、`res/`、根目录 HTML 稿直接读取；material3 动态取色内嵌情况通过对本机 gradle 缓存中 material3-android 1.3.1 AAR 的只读解包验证；
- 停更判定基准：GitHub `pushed_at` 早于 2025-03 标注「疑似停更」，早于 2025-09 标注「放缓/趋冷」。
