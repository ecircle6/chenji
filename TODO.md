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

- [x] **小组件尺寸变化后档位锁死：缩到最小仍多条 / 放大回不去多条**（2026-09-04 修复，v2.1.19 后）
  - 现象（用户实测 + 模拟器复现）：① 小组件缩到最小（宽 <200dp）后**仍显示多行列表**而非单焦；② 缩到最小后再放大，**恢复不到多条状态**，依旧只有单条内容占满小组件；③ 其他变体：放大后行数与高度错位、行距被拉出巨大空档——本质都是渲染停在旧档位、被 launcher 拉伸
  - 根因（logcat + Glance 1.1.1 反编译实锤）：v2.1.19 的尺寸链路是 `SizeMode.Single` + 「composition 内现读 `getAppWidgetOptions`」。但 Single 模式下 `LocalSize` 恒为 manifest 静态 110dp、**组合不订阅尺寸状态**；Glance 的组合只在 `glanceState`/订阅状态变化时重跑，resize 且数据未变时 update 复用旧组合——**组合内读 options 的代码根本不重新执行**（logcat 证据：`onAppWidgetOptionsChanged` 已收到新值 minW=172，但 WidgetBody 无任何重新组合日志，桌面呈现的仍是旧档位布局的拉伸结果）。v2.1.19「读取放在 composition 内保证每次重绘拿到最新」的前提（每次 resize 都会重组合）不成立
  - 修复：`SizeMode.Single → SizeMode.Exact`；`realWidgetSize` 改为优先读 `LocalSize.current`（Exact 下即系统实际尺寸整数 dp，且组合订阅尺寸变化 → resize 自动重新组合拿新值），`getAppWidgetOptions` 保留作 LocalSize 异常时的兜底；`onAppWidgetOptionsChanged → updateAll` 保留为额外保险（Exact 已自动重组合，实测 options 变更后组合先于 updateAll 重跑）
  - 追加（2026-09-04 用户要求大档 5 条）：压缩大档 chrome（Hero 头 70→60dp 去掉次要日期·关系行、外边距 14→8、标题行 14→12、两处间距 6/4→3/2），chore 总量 122→93dp，4×4(高281) 可用高 188dp 恰好放 **4 行列表 = Hero + 4 = 完整 5 条**（行高 44dp 不变，行内容不受影响）；`WidgetLayoutTest` 同步改 `largeRows(281)=4 / largeRows(355)=5`
  - 验收：✅ 全量单测绿 + assembleDebug 通过；模拟器实测——覆盖安装后 2×4 渲染 COMPACT 单焦（LocalSize=172dp 生效）；放大宽度→组合自动重跑 `266.28×464dp → LARGE` Hero 恢复；「共 5 个日子」总数正确、Hero 取最近非缅怀、列表行渲染正常。4 行列表的物理展开未在模拟器测到（launcher 对 motionevent 高度拖动时灵时不灵，属模拟器问题），行数由单测锁定。多轮连拖与真机长尾留待用户实测

- [x] **小组件按真实尺寸渲染（当前核心功能损坏：任意尺寸都渲染 2×2 单焦）**（2026-08-31 修复，v2.1.19）
  - 现状（2026-08-30 复测）：`dfac4d7` 版本桌面小组件在 4×2 / 4×4 任意尺寸下都只渲染「🌙NEXT + 单条记录」的单焦布局，大组件大面积空白。上一条 v2.1.18 的验收记录写的「模拟器实测小组件铺满」与复测不符，该次验收不充分，以此为准修正
  - 根因（logcat 实锤）：布局行数用 `LocalSize.current` 计算，而 Glance 1.1.1 `SizeMode.Single` 下 `LocalSize` 不反映实例真实尺寸，恒为 manifest 静态 fallback `min(minWidth,minResizeWidth)×min(minHeight,minResizeHeight)` = 110×110dp；110 < 200dp 宽度阈值 → 恒走 2×2 紧凑分支。`SizeMode.Responsive` 同样不可用：系统估算尺寸（竖屏 266×135 / 横屏 465×224）与三档集合互不匹配时退化到最小档被拉伸——两条系统路径都拿不到真实尺寸
  - 功能需求：
    1. 尺寸来源改为自读系统真实值：`AppWidgetManager.getAppWidgetOptions(id)` 的 `OPTION_APPWIDGET_MIN_WIDTH × MIN_HEIGHT`（竖屏语义，放置/resize 时由系统写入）；读取必须放在 composition 内——Glance 的 resize 快速路径只重组、不重跑 provideGlance，读在外面尺寸不会更新
    2. 行数随真实高度自适应（行高 48dp + 行间 6dp，上限 5 行）：4×2 两行、4×3 三行、≥4×4 Hero 聚焦头 + 三行；记录多时铺满不留沉底空白，记录少于行数时底部余白是数据事实、可接受
    3. 验收标准：模拟器实测 266×135（4×2）渲染问候行 + 2 行；resize 后 266×281 渲染 Hero + 行列表；真机 4×4 四条以上记录铺满无沉底空白；全量单测绿
  - 修复（2026-08-31，按上述需求重做）：reflog `68565eb` 的旧补丁只在出问题的那台机器的 reflog 里（`git show` 确认本地无此对象），不可恢复，重建如下
    - 新增 `widget/WidgetLayout.kt` 纯函数（阈值与换算集中、可单测）：档位分流（宽 <200dp 紧凑 / 高 ≥210dp 大档，含横屏 465×224 样例）；行数换算 `rowsFor`（行高 44dp + 行距 4dp，n ≤ (可用高+行距)/(行高+行距) 取下整，1..5）——48/6 档在 4×2(高135) 配问候行时数学上放不下两行，按验收尺寸反推改 44/4；宽/大档 chrome 扣减（外边距/问候行/Hero 头）常量与 `wideRows/largeRows` 便捷函数；`rowNeedsDense`（行框 <52dp 切紧凑行防裁切）
    - `BirthWidget.realWidgetSize(appWidgetId)`：composition 内**现读** `getAppWidgetOptions`（不 remember，重绘时必拿最新值），无效时回退 LocalSize（最坏 = 旧紧凑行为）；`MAX_ITEMS` 3→5（宽档最多 5 行、大档 Hero+3 行）；「共 N 个日子」用真实总数（新增 `WidgetData(total, items)`，取数上限后不再拿 items.size 顶替）；宽档外边距 10→6、问候行 ＋ 按钮 26→20dp、Hero 内边距 10→8；`WidgetRow` 加 `dense` 紧凑档（32dp 头像 + 更小内边距/字号）
    - **实测推翻 TODO 原假设**：这台 launcher 上 resize 后小组件**不会**自动重绘（logcat 无任何动作，桌面内容停在旧尺寸布局）——「resize 快速路径只重组、不重跑 provideGlance」的断言不成立。兜底方案落地为 `BirthWidgetReceiver.onAppWidgetOptionsChanged` 覆写 → 主动 `updateAll` 重绘（无实例时安全 no-op），覆写后 resize 即时生效；读取放 composition 内保证任何路径触发的重绘都拿到最新 options
  - 测试：新增 `WidgetLayoutTest` 20 用例（110×110 静态回退→紧凑、4×2/4×3/4×4 验收入档、行数边界 92/140/236、上限 5、dense 判定）；全量单测绿 + assembleDebug 通过
  - 验收：✅ 模拟器实测三档全过——3×2「🌙问候语 + 两行（小明 5 急 / 小美 20）」（uiautomator 逐字核对）；resize 4 行高「🌙 NEXT UP 小明 / 其他近期 · 共 5 个日子 / 小美·外婆·大哥三行」；resize 2×2「🌙 NEXT 小明单条」；resize 即时重绘（→3 行宽档）与数据变更重绘（清库→导入→重绘）双链路过；App 首页基线回归正常；真机 4×4 铺满留待用户实测

- [x] **用户实测三项优化：首页返回重播动效 / 图标星贴边 / 小组件沉底空白**（2026-08-30 修复，v2.1.18）
  - 现象：① 保存/删除后返回首页，列表慢悠悠整体重播错峰入场（约 1.3s 才完全呈现）；② 图标月亮偏大（占可见区 65.5%）、三颗星贴着 33dp 安全区（主星顶点 33.1 微超、副星外缘 34.8 出界）；③ 小组件被拉高时内容沉底、下方堆一截空白
  - 根因：① `staggeredAppear` 用 `remember` 存 Animatable，导航返回时首页离开组合状态丢失、全量重放（v2.1.16 引入）；② f27fff4 只缩了元素尺寸没动星星坐标；③ Responsive 档位内容固定高度顶对齐，被 launcher 拉伸后剩余空间全部沉底，且 LargeBody 末行多一个 Spacer、`rest.take(4)` 满行时溢出裁切
  - 修复：① `staggeredAppear` 加 `enabled` + `rememberStaggerPlay`（rememberSaveable + SideEffect：冷启动/筛选变化播放、页间返回不播），错峰参数 55ms×12 → 40ms×8；删除链路取消闹钟与删库并行、保存链路闹钟调度挪进 `NonCancellable` 后台段（`saved` 置位提前）；② 图标几何重排版：月亮 scale 2.62→2.3（径 41.4dp/57.5% 居中）、主星 group 缩 0.88 内收、副星坐标收进安全区（最远 29.8），foreground/monochrome 同步；③ 小组件 `SizeMode.Responsive`（预设三档不知实际高度）改 `SizeMode.Single`：按桌面实际尺寸算行数（可用高/行高 50dp 下限），行框 `defaultWeight()` 均分剩余高度、行内容居中其中——拉高多显示记录、行距均匀铺满整面；首版"上下弹性居中"方案会让问候行与内容之间出现夹心空隙（用户实测反馈），已废弃
  - 验收：✅ 全量单测绿 + `assembleRelease` 通过；SVG 三视图预览核对安全区几何（`app/build/icon-preview.html`）；模拟器实测返回即时呈现/图标比例/小组件铺满

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

- [x] **首页卡片右上角露出删除图标**（2026-08-15 修复）
  - 现象：卡片右上角（圆角切口处）透出一个红色垃圾桶图标
  - 根因：`SwipeToDeleteBox` 底层删除图标层用 `fillMaxSize()`，LazyColumn 列表项是无限高度约束，fillMaxSize 塌缩成图标自身高度、贴顶对齐 → 图标落在卡片右上角并从圆角透明区透出
  - 修复：改用 Box 的 `matchParentSize()`（铺满实际卡片尺寸），图标回到右侧垂直居中、被卡片完全盖住，仅左滑时露出
  - 验收：✅ 全量单测 140 用例通过（Compose 探针验证图标 bounds 从右上角 y=192 回到卡片中线 y=268）

- [x] **首页筛选面板「类型」行：无卡片类型点击无效果**（2026-08-17 修复）
  - 现象：「更多筛选」面板里点选当前没有对应卡片的类型（如「情侣纪念」），选中瞬间被重置回「全部」，表现成"点了没反应"；「关系」「生肖」行无此问题（无卡片也能点，显示空态）
  - 根因：`HomeViewModel.init` 里 `combine(availableTypes, _filter)` 的自动回退——`filter.type` 不在 `availableTypes`（数据里实际出现的类型）时立即 `_filter.type = "all"`。面板点选一个当前无卡片的类型正好命中该条件，刚选中就被重置；关系/生肖没有这层回退
  - 方案：删除该自动回退块，类型行行为与关系/生肖对齐：点选→保持选中→列表空态「这个筛选下没有记录」→「全部」胶囊/面板「清除」可恢复
  - 为什么可以不删则删：它防的是「选中类型被删光后快捷胶囊消失、取消不掉」，但快捷行永远有「全部」、面板能看见选中态，实际可恢复；而它对"用户主动选无卡片类型"造成破坏性体验
  - 验收：✅ 单测补「面板 updateFilter 选无卡片类型后 filter.type 保持选中、displayBirthdays 为空」（全量 153 用例绿）；模拟器实测面板选「情侣纪念」摘要即时更新、关闭面板后首页显示「这个筛选下没有记录」

- [x] **新建页退出慢、退出动画期间可误点**（2026-08-17 修复）
  - 现象：点「+」新建再返回，关闭较慢；若返回后立刻快速点击，可能点中新加页里的控件（如「提醒时间」）弹出 TimePicker
  - 根因：`MainActivity` 的 NavHost 未配置转场 → 用 navigation-compose 2.8.5 默认 `fadeIn/fadeOut(tween(700))`（700ms）。退出淡出期间被退出的 AddEditScreen 仍在组合中且完全可交互，快速点击落在其控件上；弹出的 Dialog 属于该页面，随页面销毁消失，但会干扰本次点击
  - 方案：NavHost 显式配 4 个转场参数，统一 `fadeIn/fadeOut(tween(200))`（enterTransition/exitTransition/popEnterTransition/popExitTransition）。关闭感知快 3.5 倍，误点窗口缩到 200ms；一处 API 覆盖 add/detail/settings 全部全屏页，顺带提速首页/日历 tab 切换
  - 备选（不用）：转场期间屏蔽输入（需观察 transition 状态+pointerInput，侵入所有页面）；pop 用 tween(0)（无过渡生硬）
  - 验收：✅ 模拟器返回新建页后零停顿 dump 已是完整首页（问候语/胶囊/Hero 正常，无新建页淡出残留），快速返回不会再点到「提醒时间」

- [x] **导入备份预览无法滚动**（2026-08-17 修复）
  - 现象：新装 App「导入备份」弹出「导入预览（N 条）」后无法下滑，条目多时最下方记录看不到
  - 根因：`SettingsScreen.kt` 导入预览 AlertDialog 的 `text` 槽是普通 `Column`，无 `verticalScroll`；M3 AlertDialog 内容默认不可滚动，超高内容被直接裁剪
  - 方案：该 Column 加 `Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())`——条目多时在有限高度内滚动，标题与「导入/取消」按钮始终可见；同文件「版本更新说明」对话框（513 行附近）已有 verticalScroll 先例可参照
  - 验收：✅ 模拟器导入 40 条备份实测：预览可逐屏上滑、最末「滚动测试记录40号」可达，标题「导入预览（40 条）」与「导入/取消」按钮全程可见

- [x] **首页远景行跨年月份分组崩溃（LazyColumn 重复 key）**（2026-08-17 审核 a26398d 时发现并修复）
  - 现象：远期记录（>30 天）落在未来年份的**不同月份**时，首页 LazyColumn 组合期抛 `IllegalArgumentException: Key "header-2027 年" was already used` 直接崩溃。复现：明年 1 月与 12 月各一条远景记录
  - 根因：`HomeTier.buildRows` 给同一未来年份的不同月份各生成一个分组标题，但 `HomeTier.monthLabel` 跨年一律显示「YYYY 年」→ `HomeScreen` 用分组标题的 label 做 LazyColumn key（`"header-${li.label}"`），不同月份组的 label 相同 → key 碰撞
  - 方案：`HomeListItem.MonthHeader` 增加稳定唯一标识 `yearMonth`（年×100+月），LazyColumn key 改用 `"header-${li.yearMonth}"`；label 保持「YYYY 年」展示语义不变
  - 配套：补 `HomeTierTest`（分层/进度/跨年分组 key 唯一等 8 用例）与 `HomeScreenTest` 跨年双分组渲染用例；顺带补齐 a26398d 测试计划里遗漏的 `EventTypeStyleTest`（类型配色）、`BackupCodecTest` emoji 编解码、`MigrationTest` v3→v4、`AddEditViewModelTest` emoji 保存/编辑用例，并清理 Emoji 选项重复项（👧/💍）与重复 import
  - 验收：✅ 全量单测 173 用例 / 24 测试类全绿；模拟器注入两条跨年远景记录实测：两个「2027 年」分组头正常渲染、滚动不崩

- [x] **首页远景迷你行透出删除图标**（2026-08-17 用户实测反馈并修复）
  - 现象：远景迷你行（透明背景）右侧常驻一个红色垃圾桶图标，露在行上很碍眼
  - 根因：`SwipeToDeleteBox` 底层删除图标常驻、靠卡片不透明白底遮挡（v2.1.7 只对不透明卡片生效）；远景行无底色，图标直接透出
  - 方案：图标改「静止时 alpha=0、随左滑进度淡入，滑过 1/3 宽即全显」（抽 `deleteIconAlpha` 纯函数 + 5 个单测）；对三层卡片行为统一，静止时均不露图标
  - 验收：✅ 全量单测 182 用例 / 24 测试类绿；模拟器像素级验证远景行右侧图标位 0 个珊瑚色像素

- [x] **Emoji 头像选择器改底部面板（表单被 24 个图标撑满）**（2026-08-17 用户实测反馈并优化）
  - 现象：添加/编辑页 25 个 Emoji 方块平铺在表单里，页面被撑得很长、视觉杂乱
  - 方案：表单只留「当前头像预览 + 自动/恢复自动 + 选择 Emoji 按钮」一行；全量选项收纳进 `ModalBottomSheet`（与筛选项面板同款交互），点选即生效并收起；预览圆片实时按自动规则展示（与首页卡片一致）
  - 测试：新增 4 个 AddEditScreenTest 用例（弹出/点选回调/清空/恢复自动入口）
  - 验收：✅ 全量单测 182 用例 / 24 测试类绿；模拟器实测点选 🐶 后面板收起、预览更新、按钮转「恢复自动」

- [x] **长按图标添加小组件会打开 App**（2026-08-18 用户实测反馈并修复，v2.1.13）
  - 现象：桌面长按图标点「添加小组件」，App 主界面被整个拉起并留在前台——快捷方式只需弹放置框，不该打开 App
  - 根因：快捷方式 intent 指向 `MainActivity`，静态快捷方式的本质就是启动目标 Activity，App 随之被带到前台
  - 方案：新增透明中转页 `WidgetPinRequestActivity`（exported/noHistory/excludeFromRecents/taskAffinity=""，透明主题 Theme.WidgetRequest）：在 `onResume`（满足 requestPinAppWidget「前台调用」的硬性约束）发起放置请求后，onPause 或 600ms 超时先到先 finish；放置框由 system/launcher 持有，不随页面销毁。shortcuts.xml 的 targetClass 改指它；MainActivity 移除 ACTION_REQUEST_WIDGET 全套
  - 验收：✅ 单测 `WidgetPinRequestActivityTest` 3 用例 + 全量 204 用例绿；模拟器实测：直达中转页后放置框弹出、`topResumedActivity` 不是 MainActivity（App 未打开）、finish 后放置框仍在

- [x] **设置页添加小组件成功无提示，用户重复点击加了两个**（2026-08-18 用户实测反馈并修复，v2.1.13）
  - 现象：设置页「添加桌面小组件」放置成功后没有任何反馈，用户以为没反映又点了一遍，结果桌面上加了两个小组件
  - 根因：`requestPinAppWidget` 的「成功回调 PendingIntent」完全没用上；API 语义是放置成功必发、用户取消不发——正是反馈的唯一正解
  - 方案：`WidgetPinner.request` 增加 `onAdded` 参数 + `successPendingIntent(context)` 工具（固定 requestCode + FLAG_UPDATE_CURRENT|IMMUTABLE）；新增 `WidgetPinSuccessReceiver`（exported=false）弹 Toast「小组件已添加到桌面」；设置页与长按图标两条入口都带上它
  - 验收：✅ 单测 `WidgetPinSuccessReceiverTest` + 全量 204 用例绿；模拟器实测：放置框确认后 widget 上桌面且出现「小组件已添加到桌面」Toast；取消放置则无提示

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

- [x] **首页改版：底部双 tab + 分类筛选面板 + Hero 卡 + 紧凑卡片 + FAB 优化**（2026-08-17 完成）
  - 实现摘要：`MainNavigationBar` 底部「首页/日历」双 tab（各自 Scaffold 内传 bottomBar，detail/add/edit/settings 全屏不变）；日历独立成页（`ui/calendar/CalendarViewModel` + `CalendarScreenPage`，全量含暂停）；`FilterState` 三维筛选（关系/类型/生肖，零迁移）+ `quickFilter`（快捷单维切换）/`updateFilter`（面板叠加）；`HeroCard` 渐变聚焦卡（countdown 最小=最近事件）+ `Greeting` 问候语；`BirthdayCard` 紧凑化（类型色条/类型标签/日期·关系）；FAB 44dp
  - 测试：新增 CalendarViewModelTest/CalendarScreenTest/GreetingTest + 更新 HomeFilter/HomeViewModel/HomeScreen/SharedComponents 测试，全量 **152 用例 / 22 测试类全绿**；versionCode 11 / v2.1.8 + Changelog 已加
  - 待补：`tools/verify-on-emulator.sh` 模拟器实测清单（底部 tab 切换/日历全量/Hero 点击/面板叠加/生肖筛选等）
  - 背景：参考根目录 `date_reminder_app_ui.html` 排版（Hero 聚焦卡 + 分类胶囊 + 紧凑倒计时卡片 + 底部导航），适配辰记现有功能；已确认效果图：`home-redesign-mockup.html`（v2 底部导航版）、`home-filter-mockup.html`（v3 分类面板版，含「快捷行 + 扩展面板」交互演示），实现时以这两份效果图为视觉基准
  - 已确认决策：底部「首页/日历」双 tab；日历独立页显示**全部记录**；卡片第二行 =「日期 · 关系」；移除 ⏰ 提醒时刻徽标（详情页仍显示）；生肖筛选维度**本期实现**；FAB 收窄 44dp
  - 调研结论：全库无 NavigationBar/bottomBar 代码（零冲突引入）；FAB 仅首页 1 处 Large 型；月历无 UI 测试保护（独立成页需新增测试）；`ZodiacUtils.getZodiacName(year)` 已存在（生肖零迁移）；Detail/AddEdit/Settings 均为「topBar 纯 Scaffold + padding(paddingValues)」标准模式，加底栏不遮挡；material-icons-extended 已在依赖（Home/CalendarMonth 图标可用）；navigation-compose 2.8.5、Compose BOM 2024.12.01

  - **1. 底部导航「首页/日历」双 tab**
    - 改动文件：`MainActivity.kt`（BirthAppNav）、`HomeScreen.kt`（Scaffold 加 bottomBar 参数）、新增 `ui/navigation/MainNavigationBar.kt`
    - 关键实现：**不用外层再包 Scaffold**（嵌套 Scaffold 会因 contentWindowInsets 双重 padding 出问题）。做法：提取公共 `MainNavigationBar(currentRoute: String?, onTabSelected: (String) -> Unit)` 组件（两个 `NavigationBarItem`：`Icons.Filled.Home`「首页」/ `Icons.Filled.CalendarMonth`「日历」），作为 **HomeScreen 与 CalendarScreenPage 各自 Scaffold 的 `bottomBar` 参数传入**——Scaffold 自动把 bottomBar 高度并入 innerPadding，无遮挡、无双重 insets，detail/add/edit/settings 不传则全屏不变
    - BirthAppNav 改动：
      ```kotlin
      val backStackEntry by navController.currentBackStackEntryAsState()
      val currentRoute = backStackEntry?.destination?.route
      composable("home") {
          HomeScreen(
              onAddClick = { navController.navigate("add") },
              onItemClick = { id -> navController.navigate("detail/$id") },
              onSettingsClick = { navController.navigate("settings") },
              bottomBar = {
                  MainNavigationBar(currentRoute, { r -> navController.navigate(r) {
                      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                  } })
              }
          )
      }
      composable("calendar") { CalendarScreenPage(同样传 bottomBar) }
      ```
    - HomeScreen 签名加 `bottomBar: @Composable () -> Unit = {}`（默认空，现有测试/调用零改动），传给内部 Scaffold
    - 注意：detail/add/edit/settings 路由不传 bottomBar → 全屏压栈不变；tab 状态由 saveState/restoreState 保持（切走再切回不丢列表滚动/搜索）

  - **2. 日历独立页（显示全部记录）**
    - 新增：`ui/calendar/CalendarViewModel.kt`、`ui/calendar/CalendarScreenPage.kt`（CalendarScreen.kt 建议 git mv 进 `ui/calendar/` 并改 package，保持语义一致，同步更新 import）
    - CalendarViewModel（沿用 @JvmOverloads + in-memory 注入模式，测试可注入）：
      ```kotlin
      class CalendarViewModel @JvmOverloads constructor(
          application: Application,
          private val database: AppDatabase = (application as BirthApp).database
      ) : AndroidViewModel(application) {
          val allBirthdays: StateFlow<List<Birthday>> =
              database.birthdayDao().getAll()   // 全量，不做任何筛选
                  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
      }
      ```
    - CalendarScreenPage：薄壳 = collect + Scaffold（TopAppBar「月历」+ actions 里 ⚙️ 设置入口 + 44dp FAB）+ `bottomBar` 参数 + `CalendarScreen(birthdays, onItemClick, Modifier.fillMaxSize().padding(paddingValues))`
    - CalendarScreen 签名收紧：`List<BirthdayDisplay>` → `List<Birthday>`（月历只需要 Birthday；`EventCalc.eventsInMonth(birthdays, displayedMonth)` 直接接收，`birthdays.map { it.birthday }` 这层去掉）；内部 eventsByDay/DayCell 弹窗直接用 Birthday，其余翻月/农历小字/圆点/当日弹窗逻辑不变
    - HomeScreen 删除：`showCalendar` rememberSaveable、`SingleChoiceSegmentedButtonRow`（列表/月历切换）整块、CalendarScreen 调用与 import
    - 注意：`EventCalc.eventsInMonth` 纯函数不动 → util 层 4 个「月历落点」测试不受影响

  - **3. 分类筛选：快捷行 + 底部面板 + 配置驱动**（核心改动，先改模型再改 UI）
    - 改动文件：`HomeViewModel.kt`、`HomeFilter.kt`、`HomeScreen.kt`、`HomeFilterTest.kt`、`HomeViewModelTest.kt`、`HomeScreenTest.kt`
    - FilterState（替代散落的 selectedTab/selectedType 两个流，单一不可变对象，扩展维度=加字段）：
      ```kotlin
      data class FilterState(
          val relation: String = "all",   // "all" | "family" | "friend" | "colleague" | "other"
          val type: String = "all",       // "all" | EventType 常量（birthday/love/memorial/other…）
          val zodiac: String? = null      // null=不限 | 生肖中文名（鼠/牛/虎/兔/龙/蛇/马/羊/猴/鸡/狗/猪）
      )
      ```
    - HomeFilter.apply 改为配置驱动（搜索语义保持：非空全局查找、跳出筛选）：
      ```kotlin
      fun apply(list: List<Birthday>, filter: FilterState, keyword: String): List<Birthday> {
          val kw = keyword.trim()
          if (kw.isNotEmpty()) return list.filter {
              it.name.contains(kw, ignoreCase = true) || it.notes.contains(kw, ignoreCase = true)
          }
          return list
              .filter { filter.relation == "all" || it.relation == filter.relation }
              .filter { filter.type == "all" || it.eventType == filter.type }
              .filter { filter.zodiac == null || ZodiacUtils.getZodiacName(it.birthYear) == filter.zodiac }
      }
      ```
    - HomeViewModel：`_filter = MutableStateFlow(FilterState())`；`fun quickFilter(dim: String, value: String)`（单维切换、清空其他维，对应快捷胶囊与面板点选）；`fun clearFilters()`（回全默认）；`displayBirthdays` combine 改读 `_filter`；`availableTypes` 逻辑不变
    - UI（HomeContent）：一行快捷胶囊（全部/家人/朋友/同事/生日/情侣纪念/缅怀 + 行尾固定「⋯ 更多筛选」），横向滚动 + 右缘淡出（`Modifier` + `horizontalScroll`，行尾按钮不参与滚动不遮挡）；`var showFilterSheet by remember { mutableStateOf(false) }` 控制 `ModalBottomSheet`（material3 自带，`@OptIn(ExperimentalMaterial3Api::class)`）：关系/类型/生肖三组（组内单选、组间叠加），顶部当前筛选摘要（拼「家人 · 生日 · 虎」）+ 清除；点选直接调 `onQuickFilter` 生效，面板收起由关闭/外部点击完成，无需额外状态流
    - 生肖维度：`ZodiacUtils.getZodiacName(birthYear)` 派生，**零数据库迁移**；选项固定 12 生肖中文名
    - 注意：快捷胶囊点击 = 单维切换（清其他维）；面板内点选 = 只更新该维（叠加）；「全部」/「清除」= clearFilters；搜索态隐藏 Hero/快捷行/面板（保持现状）

  - **4. Hero 聚焦卡**
    - 新增 `ui/home/HeroCard.kt` + `Color.kt` 加渐变常量
    - 派生：`val hero = birthdays.filter { !it.isPaused }.minByOrNull { it.countdown }`（**取倒计时最小 = 最近事件，不要取列表第一条**——置顶记录列表排最前但可能不是最近，效果图 hero 是「妈妈生日 60 天」而非置顶的「小明 364 天」）；null（空/全暂停）或搜索态不显示
    - 组件：label「即将到来」/ isToday 时「就是今天」、名称、dateLabel、56sp 大倒计时 +「天后」、右侧类型 emoji 水印（`EventType.emoji`，opacity 低、右对齐）；`Surface(onClick)` 或 `Card(onClick)` + `Brush.linearGradient`；点击进详情
    - 渐变配色（效果图值，Color.kt 常量）：
      ```
      HeroBirthday      = #FFC98A → #FF8A5C    HeroBirthdayDark   = #B06A2E → #C74F2A
      HeroLove          = #B5ACFF → #7C6BFF    HeroLoveDark       = #6A5BC4 → #4A3B9E
      HeroMemorial      = #8E9EAB → #5B6B7A    HeroMemorialDark   = #4A545E → #31383F
      HeroOther         = #5EEAD4 → #00BFA5    HeroOtherDark      = #1E7A6E → #0F5F54
      ```
    - @Preview：浅/深 × 各类型（生日/情侣/缅怀/其他）

  - **5. 问候语行**（TopAppBar 下方）
    - 新增 `util/Greeting.kt`：按年积日取文案池一句，当天稳定、纯函数可单测：
      ```kotlin
      object Greeting {
          private val POOL = listOf("愿你被时光温柔以待", "每一个日子都值得铭记", "时光不老，我们不散",
                                    "珍惜当下，铭记过往", "岁月漫长，值得等待")
          fun today(date: LocalDate = LocalDate.now()): String = POOL[(date.dayOfYear - 1) % POOL.size]
      }
      ```
    - HomeContent 顶部行：「8月16日 周六」（`DateUtils.formatSolarMonthDay` + 星期）· 问候语；搜索态隐藏

  - **6. 卡片紧凑化**（`ui/common/SharedComponents.kt` 直接改 BirthdayCard 内部，签名不变）
    - 新布局（效果图基准）：`Card(onClick, 圆角 20dp, 白/深底)` 内 `Row`：左 4dp 类型色条（`Modifier.width(4.dp).fillMaxHeight()` + eventAccent 色）→ 圆角图标块 46dp/14dp（生日=姓名首字、其余=类型 emoji，eventAccent 12% 底）→ 名称（15sp bold）+ 📌 置顶徽标 + 已暂停灰显徽标 + **类型标签**（由「关系」改「事件类型」：生日/情侣纪念/缅怀/其他，soft 底 + eventAccent 色）→ 第二行小字「`dateLabel` · `relationLabel`」（11sp，text2）→ 右侧倒计时 26sp ExtraBold（eventAccent 色）+「天后」
    - 保留：今天横幅（isToday 时替换倒计时区，`eventBannerColors`）、已暂停整体 alpha 0.5、深色变体
    - 移除：⏰ 提醒时刻徽标、`infoLine`（原第二行长文案）、头像 Circle 改圆角方块
    - 注意：`EventTextUtils.infoLine/cardBanner` 仍被详情页/通知用，**不要动 EventTextUtils**；卡片文本变化只影响 SharedComponentsTest

  - **7. FAB 优化**
    - HomeScreen：`LargeFloatingActionButton` → `FloatingActionButton(containerColor = Coral500, modifier = Modifier.size(44.dp))`（搜索态隐藏保留）
    - `LazyColumn contentPadding = PaddingValues(vertical = 8.dp, bottom = 64.dp)`（44dp + 间距），保证最后一张卡片滚过 FAB 完全露出
    - CalendarScreenPage 同款 44dp FAB（新增入口）

  - 测试计划：
    - 更新 `HomeScreenTest`（render 参数改 `filter: FilterState = FilterState()` + onQuickFilter/onClearFilters 回调；删月历切换相关——现状本无此断言；新增：Hero 卡显示最近事件名「在一起三周年」及「即将到来」label、点 Hero 触发 onItemClick、快捷胶囊「家人」点击回调、列表头「共 N 个日子」）
    - 更新 `SharedComponentsTest`（新卡片布局：类型标签「生日」存在、第二行「8月14日 · 家人」substring、26sp 倒计时「364」、无 ⏰ 断言）
    - 更新 `HomeFilterTest`（apply(list, FilterState, query) 新签名适配 9 个既有用例 + 新增生肖用例：birthYear 1945→鸡、1998→虎）
    - 更新 `HomeViewModelTest`（filter/quickFilter/clearFilters 状态变化）
    - 新增 `CalendarViewModelTest`（in-memory Room 插 2 条含暂停 → allBirthdays 全量含暂停、无筛选）
    - 新增日历页 UI 测试（渲染 `CalendarScreen(List<Birthday>)`：标题「2026年X月」、点「上个月」contentDescription 切月、有事件日期弹窗进详情——现状月历零 UI 测试保护，必须补）
    - 新增 `GreetingTest`（同日同值、不同日不同/循环）
  - 发版：versionCode 10 → 11、versionName 2.1.7 → 2.1.8；`Changelog.kt` 头部加 v2.1.8 条目（首页改版/双 tab/分类面板/生肖筛选/卡片紧凑化/Hero 卡）；README 更新功能与测试统计
  - 验证：`./gradlew testDebugUnitTest` 全绿 + `assembleDebug` 通过；`bash tools/verify-on-emulator.sh` 模拟器实测清单：底部 tab 切换（含切走再切回状态保持）、日历页全量记录（含暂停）、Hero 卡点击进详情、面板叠加筛选（家人∩生日∩属虎）、生肖筛选、快捷胶囊单维切换、列表底卡片不被 FAB 遮挡、深色模式、搜索/空态回归
  - 不做：详情页/添加页/设置页、HomeViewModel 排序与搜索核心语义、滑动删除、数据模型（生肖零迁移）、备份、小组件全部不动

- [x] **首页卡片体系按 time_memory_home.html 重构：类型配色 + 三层卡片 + 专属 Emoji 头像 + 月份分组/远景折叠**（2026-08-17 完成）
  - 背景：用户反馈——①生日与情侣纪念同色（都是橙红）②同一类型只有一种颜色，生日居多导致首页一片同色单调。已确认按根目录 `time_memory_home.html` 设计稿方向重构（定稿：生日暖橙/纪念紫/缅怀灰蓝；紧急卡进度条「已过去 X 天」；专属 Emoji 头像；月份分组+远景迷你行）
  - **设计决策（与现状的关系）**：Hero 卡保留在顶部；置顶记录→标准卡置顶显示；暂停记录→灰显标准卡沉底；搜索态保持普通卡片列表不分层（聚焦结果）；筛选态照常分层；`SwipeToDeleteBox` 滑删对三层均保留；分享卡是定稿规范不动
  - **改动一：类型固定配色（修「生日/情侣同色」）**——`ui/common/EventTypeStyle.kt` 的 `eventAccent()`：生日保持 `Coral500`（品牌主色、最常见类型）；`LOVE → Violet500`（与 Hero 情侣渐变 HeroLove、MARRIAGE 的紫一致）；缅怀 `SlateInk`、其他 `SunnyYellow700` 不变；`eventBannerColors()` LOVE 分支同步改 violet 系（今天横幅与色条同色）。类型选择器/详情页共用该函数自动跟随
  - **改动二：卡片三层化（按倒计时分层）**
    - 紧急层 0-7 天 `UrgentCard`（新组件）：2dp 类型色边框 + 呼吸光晕（`rememberInfiniteTransition` 边框 alpha 0.5↔1，2.8s）+ 左 5dp 类型色竖条；52dp 圆形 Emoji 头像（类型色 10% 底 + 20% 描边）；名称 17sp + meta 小字（复用 `EventTextUtils.infoLine`：日历/日期/属相·岁数）；右侧 38sp 大倒计时（countdown 0 →「今天」）；**底部进度条**：label「提醒进度 · 已过去 X 天」+ 5dp 进度条（fill=类型色渐变），`已过去 = 7 - countdown`，进度 `= 已过去/7`（今天 100%）——照设计稿 4/7=57% 的语义：进入 7 天窗口后每天推进 1/7，直观传达"时间在流逝"
    - 标准层 8-30 天 `StandardCard`（现 `BirthdayCard` 微调）：44dp 圆形 Emoji 头像（类型色 10% 底）替代圆角方块（生日首字/类型 emoji）；左 3dp 类型色条 + 类型标签 pill + 名称 + 「日期 · 关系」小字 + 右侧 24sp 倒计时
    - 远景层 >30 天 `DistantRow`（新组件）+ 月份分隔标题：按月分组（`EventCalc.nextSolarDate` 的 年·月），本年 →「X 月」、跨年 →「YYYY 年」灰色小标题；行 = 7dp 类型色圆点 + 「名字 · M月d日 · N岁」+「N 天后」，整行 opacity 0.55，点击进详情（即"展开详情"）
  - **改动三：专属 Emoji 头像（数据字段 + 迁移 + 选择器）**
    - `Birthday` 加 `emoji: String = ""`（`@ColumnInfo(defaultValue = "")`；空=自动：生日→姓名首字、其他→类型 emoji）
    - 数据库 v3→v4：`ALTER TABLE birthdays ADD COLUMN emoji TEXT NOT NULL DEFAULT ''`；补 `app/src/test/assets/4.json` schema + `MigrationTest` v3→v4 用例
    - 备份 `FORMAT_VERSION 3→4`：`encode` 加 `put("emoji")`，`decode` 加 `optString("emoji")`（老 v3 备份缺字段→默认空）；补编解码测试
    - 添加/编辑页「头像 Emoji」改**底部面板选择**（用户实测反馈表单被 24 个图标撑满后优化）：表单只有「当前头像预览 + 自动/恢复自动 + 选择 Emoji 按钮」一行，全量 24 个预设进 `ModalBottomSheet` 网格（与筛选项面板同款交互，点选即生效并收起），预览圆片按自动规则实时展示（与首页卡片一致）；约 28 个预设 Emoji 已去重为 24 个，+「自动」chip 清空，点选高亮（类型色描边）
    - 首页三层卡片头像统一取 `emoji.ifBlank { 自动 }`，头像底 = 类型色 10% tint
    - 桌面小组件：`WidgetItem.emoji` 改取 `emoji.ifBlank { EventType.emoji(eventType) }`（一行）
    - 详情页现无头像位不动；分享卡是定稿规范不动
  - **改动四：分层/分组纯函数**——新建 `ui/home/HomeTier.kt`：`tierOf(countdown)`（0-7 紧急 / 8-30 标准 / >30 远景）、`progressOf/elapsedDays`、`buildRows(list)`（产出 `List<HomeListItem>` 含月份 Header，纯函数可单测）；`HomeViewModel.displayBirthdays` 排序保持（置顶→暂停→倒计时），`HomeScreen` LazyColumn 按 `buildRows` 渲染异构行（key：Header 用 "header-年-月"，卡片用 birthday.id）
  - 测试计划：新增 `HomeTierTest`（分层/进度/分组纯函数）、`EventTypeStyleTest`（生日≠情侣色、Love=Violet）；更新 `HomeViewModelTest`（无卡片类型保持选中、分层结构）、`HomeScreenTest`/`SharedComponentsTest`（三层卡片/头像 emoji/进度条断言）、`MigrationTest`（v3→v4）、`BackupCodecTest`（emoji 编解码）、`AddEditViewModelTest`（emoji 保存）
  - 坑点：Robolectric 下 emoji 字形尺寸为 0，Compose 断言用存在性+substring；新增列 defaultValue 必须与实体 `@ColumnInfo(defaultValue)` 一致否则 Room schema 校验崩；备份 FORMAT 升 4 后老版本 App 读新备份会提示"版本不支持"（与既有前向兼容策略一致）
  - 验收：全量单测绿 + assembleDebug 通过；模拟器实测清单——类型面板无卡片可点、紧急卡进度条宽度/「已过去 X 天」、Emoji 头像选择与三层展示、月份分组/远景折叠、返回动画提速、导入预览滚动、深色模式、筛选/搜索/置顶/暂停回归
  - 发版：versionCode 11→12、versionName 2.1.8→2.1.9；`Changelog.kt` 头部加 v2.1.9 条目（首页分层卡片/专属 Emoji 头像/紧急进度条/类型配色区分/月份分组/筛选面板修复/导入预览滚动/返回动画提速）；README 更新功能与测试统计

- [x] **Hero 聚焦卡规则重构：列表去重 + 缅怀不放大 + ≤7 天 Hero 带进度条（含同日多卡处理矩阵）**（2026-08-17 完成；效果图：`hero-redesign-mockup.html` 7 场景）
  - 动因（用户实测反馈）：① 最近记录在 Hero 与列表各出现一次（同一记录两个卡片）；② 进度条只在 0-7 天 UrgentCard 有、Hero 没有，造成「有的卡片有点的没有」；③ 缅怀若恰为最近，会被庆祝式大卡放大（即将到来+大字），与「悼念不该被放大」矛盾
  - **已确认方向（用户拍板）**：列表去重（每条记录只出现一次）＋ 最近全是缅怀则隐藏整个 Hero ＋ 同日缅怀与庆典并存时，Hero 让位给同天的庆祝事件
  - **核心规则（`heroCandidate` 一条算法覆盖全部情形）**：
    ```
    活跃 = 非暂停记录；活跃为空 → 无 Hero
    minCount = 活跃记录的最小 countdown（"最近的日期"，当天/同日同值）
    最近日期组 = countdown == minCount 的记录（同一天可能多条）
    → 组内存在庆祝（非缅怀：生日/情侣/结婚/其他）→ Hero = 组内第一条庆祝
        （沿用 displayBirthdays 排序：置顶优先；同排位稳定序，确定性可测）
    → 组内全是缅怀 → 无 Hero
    列表去重：只移除 Hero 展示的那一条；其余（含同天其他庆祝/缅怀）原样保留
    ```
  - **同日组合决策矩阵**（设该日期即最近日期）：
    | 同天组合 | 最近日期组 | Hero | 列表表现 |
    |---|---|---|---|
    | 多个缅怀 | 全缅怀 | 无 | 各灰蓝缅怀卡按倒计时相邻（同 tier），每卡自带日期；本期不做同天分组标题 |
    | 多个生日 | 有庆祝 | 排序第一条生日 | 其余生日仍在列表（不合并、信息不丢）|
    | 生日+情侣/结婚纪念 | 有庆祝（庆祝类不互斥）| 排序第一条 | 另一条仍在列表 |
    | 生日+缅怀 | 有庆祝 | 生日（= 场景④）| 缅怀保留在列表灰蓝呈现 |
    | 生日+情侣+缅怀 | 有庆祝 | 排序第一条庆祝 | 其余两条在列表 |
    | 单缅怀+其余均为暂停 | 全缅怀（暂停不算活跃）| 无 | 缅怀卡 + 暂停卡灰显沉底 |
    | 全部暂停 / 空数据 | 空 | 无 | 暂停卡灰显 / 空态 |
    | 当天（countdown=0）任一组合 | 按组内类型同上 | 同上 | 同上 |
  - **其他设计决定**：
    - 同日多条庆祝 Hero 取谁：按现有排序取第一条（置顶优先），不确定时稳定序兜底；不把所有同日庆祝塞进 Hero
    - Hero 进度条：仅当 `hero.countdown ∈ 0..7` 时显示「提醒进度 · 已过去 X 天」+ 5dp 渐变条（复用 `HomeTier.elapsedDays/progressOf`；缅怀不进 Hero，进度条恒为庆祝色）
    - 文案边界：countdown=7（刚进窗口）→「已过去 0 天」；若观感不佳改「刚进入提醒窗口」（实现时定，单测只锁语义）
    - 列表同日分组标题：本期不做——同天记录因倒计时相同天然相邻、每卡自带日期，分组标题收益低；列为 P2 可选增强候选项
    - 搜索态维持现状（无 Hero、不分层）；暂停/空 → 无 Hero（现状已如此）
  - **改动文件（实现时）**：`HeroCard.kt`（`heroBirthday`→`heroCandidate` + 进度条块）、`HomeScreen.kt`（非搜索态计算一次 hero、传给 HeroCard 与列表去重过滤）、`HomeTier.kt` 不动（去重在其上游）
  - **测试（实现时）**：新增 `HeroCandidateTest`（庆典最近 >7/≤7→该庆典；最近全缅怀→null；同日缅怀+庆典→庆典；同日多条庆祝→排序第一条；全暂停→null）；更新 `HomeScreenTest`（「在一起三周年」出现 2→1 次、hero 点击不变、全暂停/全缅怀断言无 Hero、≤7 天 Hero 进度条断言）；`HomeTierTest` 不动 + `SwipeDirectionTest` 不动
  - 验收：全量单测绿 + `assembleDebug` 通过；模拟器按 `hero-redesign-mockup.html` 的 7 个场景逐一实测对照
  - **实现与验收**（2026-08-17）：`heroCandidate` 纯函数（活跃→最近日期组→组内第一条非缅怀）落地于 `HeroCard.kt`，替换旧 `heroBirthday`（只排暂停+取最小）；`HeroCard` 签名改为 `(hero: BirthdayDisplay?, ...)` 纯展示，候选逻辑收敛到调用方；`HomeScreen` 非搜索态计算一次 hero、对列表 `filterNot` 去重（buildRows 之前）、搜索态保持无 Hero 不分层；Hero 卡 `countdown <= 7` 时底部加进度条（复用 `HomeTier.progressOf/elapsedDays`，白字白条与渐变底融合，与紧急卡进度语义一致）；新增 `HeroCandidateTest` 11 用例（空/全暂停→null、非置顶当选、>7/≤7 均当选、最近全缅怀→null、更远庆祝不救场、同日缅怀+庆典取庆典、同日多庆祝取排序第一条、暂停不参与、情侣纪念也是庆祝）+ `HomeScreenTest` 更新（去重计数 2→1、全缅怀无 Hero、进度条有/无、同日缅怀+庆典聚焦庆典且缅怀留列表）；跨年分组 key 崩溃测试改用缅怀数据构造（缅怀不进 Hero 不被去重，保留两分组验证意图）。全量 **198 用例 / 26 测试类全绿** + `assembleDebug` 通过；模拟器 7 场景实测对照效果图；versionCode 13 / v2.1.10 + Changelog 已加

- [x] **小组件一键添加入口**（2026-08-18 完成）
  - 需求：添加小组件要进桌面小组件列表翻找，不方便 → 长按应用图标即可添加
  - 方案：静态快捷方式（`res/xml/shortcuts.xml`，挂 MainActivity）+ `AppWidgetManager.requestPinAppWidget()`（API 26 = minSdk，零新权限），弹系统「添加到主屏幕」放置框，确认后自动走既有 WidgetConfigureActivity 配置页
  - 选型对比：系统小组件选择器 deep-link（Android 8+ 禁第三方唤起）✗；动态快捷方式（静态足够，无需生命周期管理）✗；可拖拽快捷方式（拖出的是磁贴非 widget）✗；无障碍自动放置（需系统级权限，违背隐私定位）✗；依赖 launcher 自带长按入口（国产桌面普遍缺失，正是痛点）✗
  - 实现：`WidgetPinner.request(context)` 统一入口（绑定 `BirthWidgetReceiver`）；MainActivity `ACTION_REQUEST_WIDGET` 双通道（cold onCreate / warm onNewIntent，仿 ACTION_OPEN_ADD）；返回 false（桌面不支持 pin）时 Toast 引导「长按桌面空白处 → 小组件 → 辰记」；设置页「关于」区新增「添加桌面小组件」行复用同一入口做兜底
  - 测试：`WidgetPinnerTest` 2 用例（Robolectric 返回值契约：支持 true / 不支持 false）；`SettingsScreenTest` 补「关于区显示添加行、点击触发回调」；全量 **200 用例 / 26 测试类全绿** + assembleDebug 通过
  - 收尾：versionCode 15 / v2.1.12 + Changelog 已加；README 功能列表同步
  - 已知行为：从快捷方式冷启动首次会顺带弹通知/电池优化引导（与小组件「＋」冷启动一致，非本次新增）
  - 不做：长按菜单不放「新增记录/打开日历」；不做专属 widget 预览图（远期可选）
  - 验收：✅ 模拟器实测 `am start -a com.birthapp.action.REQUEST_WIDGET`（冷/热启动）与设置页入口三路均弹系统「添加到主屏幕」放置框（含组件说明与尺寸）；`cmd shortcut get-shortcuts` 确认系统已注册 `id=add_widget`（label/icon/action 正确）；长按弹窗视觉效果留待真机人工确认（模拟器 launcher 会把长按识别成拖拽，AGENTS 已知限制）

- [x] **图标与小组件焕新：怀月藏星 2.0 × 纸笺辰刻**（2026-08-28 完成，v2.1.14；设计提案：`icon-widget-design-proposal.html`）
  - 需求：现图标多色黄星在 Themed 单色下消失、副星小尺寸糊点；小组件白底列表与首页纸笺体系脱节、头部占 13%/行内等权无主次/单条悬空
  - 方案：按 `icon-widget-design-proposal.html` 最小改动路径两步落地——① 图标 Themed 单色定稿（仅保留黑白，形体区分主次）② 小组件纸笺辰刻三档重构（复用现有 Token，不新增色）
  - 图标：新增 `drawable/ic_launcher_monochrome.xml`（`Color 0xFF000000` 单色矢量，主星 4 尖/副星实心圆 2.1/1.6dp，3-4-5 三角星群，66dp 安全区）；`ic_launcher_foreground.xml` 副星 path→实心圆；`mipmap-anydpi-v26/ic_launcher.xml` monochrome 指向新文件（浅壁纸黑 on 白/深壁纸白 on 黑自动 tint）
  - 小组件：`BirthWidget.kt` 全量重构——`WidgetItem` 补 id/eventType/avatarText/typeLabel/dateLabel/relationLabel；2×2 紧凑改日历撕页单焦（NEXT + 48dp 头像 + 34sp 倒计时，缅怀去进度保持庄重）；4×2 宽改问候语头部（`Greeting.today()` + 日期）+ 纸笺行（左 3.5dp 色条 + 36dp 头像 + 名称/类型/急标签 + 右侧 20sp 数字，≤7 天呼吸边框双层 Box 模拟）；4×4 大改 Hero 纯色头（accent 底白字，NEXT UP + 30sp 倒计时）+ 副标题「其他近期·共 N 个日子」+ 列表；空态明确「＋添加」按钮；暗色 `#242422` 行底；点击分流三路（空白→首页/行→详情独立 `OPEN_DETAIL_$id` + `EXTRA_BIRTHDAY_ID`/＋→新增 `ACTION_OPEN_ADD`，沿用 PendingIntent 独立 action 避合并）
  - 约束：Glance 1.1.1 仅 `Box/Row/Column/Text + ColorProvider + cornerRadius`（无阴影/毛玻璃/自定义字体/border）；DayNight `ColorProvider(day,night)` 不支持 `copy(alpha)` 故头像 10% 底用预计算 `0x1A` 前缀固定色；Hero 本期纯色不做位图渐变；MainActivity 的 `handleDetailIntent` 复用 `EXTRA_BIRTHDAY_ID` 已支持小组件直达详情
  - 测试：`assembleDebug` 通过；全量单测 189 用例 / 28 测试类绿（新增/改动仅视觉层，无数据迁移）
  - 发版：versionCode 16→17 / versionName 2.1.13→2.1.14；`Changelog.kt` 头部加 v2.1.14 条目；README 图标与小组件描述同步；适配器图标小尺寸/深浅壁纸 Themed 需真机验证，三档小组件需真机桌面拖拽确认（模拟器仅能 `verify-on-emulator.sh` 截图）

---

## P2 — 工程与体验现代化

- [x] **GitHub Actions CI**（2026-08-14 完成）
  - `.github/workflows/ci.yml`：push/PR 自动跑全部单测 + 构建 Debug 包（含 APK 产物上传）
  - `.github/workflows/release.yml`：打 `v*` tag 自动签名构建正式包并发布 GitHub Release（签名密钥走 repo Secrets：KEYSTORE_BASE64/STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD）
  - 配套：`gradlew`/`gradlew.bat`/`gradle-wrapper.jar` 入库（原仓库缺失）
  - 坑记录：阿里云镜像在海外网络返回 502 导致 CI 解析失败 → `settings.gradle.kts` 按环境分流（CI=`true` 走官方源 / 本地走阿里云镜像加速）
  - 注意：wrapper 默认官方下载地址，国内网络慢时可改 gradle-wrapper.properties 为腾讯镜像

- [ ] **小组件显示窗口还原「版本 A 白底列表」形态**（2026-08-31 拍板，列 TODO 待实施；对比页 `widget-versions-compare.html` 可复看三版本×三档位效果）
  - 决策：用户对照对比页后拍板，把小组件显示窗口还原到 8.29 前（v2.1.9，白底列表时代）的功能状态
  - 还原内容：2×2 居中大字（emoji + 名字 + 「N 天后」整句，倒计时按类型配色）；4×2「辰记」品牌头 + 右侧「＋」+ **3 行**弹性列表行（emoji + 名字 + 倒计时文字，`defaultWeight` 均分高度）；大档**6 行**（MAX_ITEMS 6，空行垫底防单条悬空）；空态两行字无按钮；**行不可点**（整卡点开 App，移除行→详情直达）；随之移除：Hero 渐变头 / 🌙问候语 / 纸笺行（色条·头像块·类型·急·日期·关系）/「共 N 个日子」
  - **勘误（对比页初版标注有误）**：A 版 4×4 的 6 行是弹性均分（行变矮），**不是**溢出裁切；A 版真正已知缺陷是 Responsive 三档在尺寸不匹配时退最小档被拉伸（v2.1.18 实锤，正是其改 Single 的动因）
  - 路线论证（实施前再确认）：**推荐「A 显示 × 当前可靠机制」**——保留 v2.1.19 的 Single + options 真实尺寸 + Receiver 覆写即时重绘，渲染与交互完整还原 A（所见即 A 且任意桌面尺寸正常）；不推荐恢复 Responsive（会把退档拉伸缺陷带回来，模拟器验证大概率失败——A/B 时代小组件渲染从未在模拟器验证过）
  - 收尾（实施时）：v2.1.20 / versionCode 23 + Changelog 条目 + README 第 13 行改回列表形态 + 对比页更新（勘误 + 新增「版本 D · 还原 A」行）+ 模拟器实测清单（3×2 三行 / 4 行高六行 / 2×2 大字 / 点行开 App / 空态 / 深色）

- [ ] **i18n 字符串抽取（中/英）**
  - 现状：全部文案硬编码中文，`strings.xml` 仅 4 条；`attachBaseContext` 强设中文屏蔽了 Android 13+ 按应用语言设置
  - 方案：文案迁入 `strings.xml`（中/英双语），移除强设中文逻辑
  - 参照：MemoD 中英双语跟随系统

- [x] **edge-to-edge + Material You 动态取色**（2026-08-14 完成）
  - 实现：`MainActivity` 调 `enableEdgeToEdge()`（targetSdk 35 下 Android 15 强制），`Theme.kt` 删除已废弃的 `statusBarColor` 手动涂色（保留图标明暗控制）；`ThemeStore` 加 `dynamicColor` 开关（prefs 持久化，默认关保持品牌 Coral/Teal 配色），`BirthAppTheme` 在 Android 12+ 且开启时用 `dynamicLightColorScheme/dynamicDarkColorScheme`；设置页深夜模式卡片加"动态取色"Switch（SDK<31 隐藏）
  - 验收：✅ 模拟器（Android 15）状态栏与内容衔接正常、无遮挡；开关写入 prefs 即时生效、无崩溃

- [x] **@Preview 与 Compose UI 测试**（2026-08-15 完成）
  - 无状态化重构：4 个页面抽 `XxxContent(state, callbacks)` 纯渲染（薄壳 `XxxScreen(viewModel)` 只做状态收集与回调转发，导航零改动）；Settings 页消除 `applicationContext as BirthApp` 强转与 SharedPreferences 直读，新增 `SettingsUiState`/`SettingsCallbacks`
  - @Preview：首页/详情/添加编辑/设置 4 页 Content + 月历 + BirthdayCard/空态等共享组件，浅色/深色双预览（`ui/preview/PreviewData.kt` 提供样例数据）
  - Compose UI 测试（Robolectric 本地跑，`ui-test-junit4` + `@GraphicsMode(NATIVE)`）：5 个测试类 26 用例，语义树断言文本/状态切换/点击回调
  - ViewModel 测试（in-memory Room 注入，`@JvmOverloads` 保持 `viewModel()` 工厂不变）：Home（排序/类型筛选/搜索/删除）、Detail（加载/置顶/暂停/删除）、AddEdit（默认时间/提前提醒/保存/编辑加载）、Settings（分享备份/导入预览三选）共 17 用例
  - 坑记录：① 页面 Composable 直接注入 AndroidViewModel 无法预览，需先无状态化（对比过假 VM 子类/previewState 参数两方案，均否）；② Robolectric 下 emoji 字形与「 天后」前导空格文本节点尺寸为 0，断言改用存在性 + substring；③ Robolectric 下 FileProvider 路径根解析不生效，shareBackup 测试只断言文件内容与事件已发出；④ UI 测试需 `@Config(application = Application::class)` 绕开 BirthApp.onCreate 的数据库访问
  - 验收：✅ 单测 97 → 140 用例全绿；`assembleDebug` 通过；IDE @Preview 面板 4 页 + 组件全部可渲染

- [x] **分享卡片**（2026-08-14 首版；2026-08-15 改版为 1080×1920 竖版双风格）
  - 改版后：竖版 1080×1920（9:16，适配朋友圈/小红书）；按事件类型分流两套风格：
    - **A · 极光毛玻璃**（生日/纪念日/情侣纪念等）：深色三色渐变底 + 青绿/紫极光晕染（径向渐变模拟模糊）+ 毛玻璃信息卡（半透明白底 + 卡片内极光微光模拟 backdrop-filter）→ 标签（类型适配：生日青绿/纪念日暖粉/情侣粉紫）→ 标题 → 日期 → 倒计时/「就是今天」→ 底部三栏（月份紫/日期蓝/星期青绿）
    - **B · 深夜烛火**（缅怀）：纯黑底 + 顶部暖橙烛光 + 🕯️/IN MEMORY →「名字离开我们已经」→ 翻页数字方块（暖金 #e8c080，渐变深灰底）+ 斜体诗句 + 农历/阳历日期，无鲜艳色无装饰
  - 设计稿：根目录 `design-card-demo.html`（HTML 高保真定稿，已入库作为分享卡片设计规范）
  - 测试：`ShareCardGeneratorTest` 5 用例（竖版尺寸、双风格生成、各类型绘制）
  - 验收：✅ 模拟器真实生成两风格 PNG（小明 A 风格 / 王奶奶 B 风格缅怀 276 天），分享面板正常
  - 2026-08-15：实现与 `design-card-demo.html` 逐项对齐（此前一度跑偏为 840×640 横版已纠正），像素级校验：品牌行左 Logo 右 CHENJI、倒计时垂直居中、分隔线/诗句/翻页数字位置均按 CSS 值 ×3.375 换算
  - 2026-08-15：圆角外角部由透明改为铺中性深色底（设计稿页面底色 #14161c）——相册/浅色背景预览不再出现白色角；深色背景观感不变，圆角弧线保留

- [x] **UI 风格体系化：等宽数字字阶 + 语义圆角 + 对比度达标**（2026-08-30 完成，v2.1.15；审计与提案：`design-optimization-preview.html`）
  - 背景：material-3 audit 自查约 54/100——形状（57 处 RoundedCornerShape 魔法数字、MaterialTheme.shapes 使用 0 次）、排版（92 处硬编码 fontSize、倒计时数字非等宽）、无障碍（浅色次要文字 #8E8E8E 对暖纸底仅 3.06:1）三大短板
  - 主题层：`Type.kt` 补 displayLarge/Medium/Small（56/44/34sp ExtraBold + `fontFeatureSettings="tnum"` 等宽）与 titleSmall/labelSmall，titleMedium 定为卡片姓名（600，让数字成为唯一焦点），新增扩展 token `CountdownCompact`（26sp 紧凑卡数字）；`Theme.kt` 定义 `Shapes` 四档（4/8/12/16 + extraLarge 28）与 `SheetShape`（顶部弹层 28），light/dark scheme 补全 `surfaceContainer` 五档、`outline`/`outlineVariant` 分角色；`Color.kt` 次要文字 #8E8E8E→#6E6E6E（4.6:1 达标）并新增暖色容器阶梯/边界色常量
  - 页面收敛：6 个页面 57 处 `RoundedCornerShape(N.dp)` 全部替换为语义引用（16→large、12→medium、8/10→small、3/4/5→extraSmall、20/24→large/extraLarge、筛选胶囊→`RoundedCornerShape(50)`、底部面板→`SheetShape`）；首页问候语/月历表头/远景行等半透明灰字改 `onSurfaceVariant`；Hero/详情/列表卡的姓名、倒计时、辅助行、进度标签全部接入 Typography 角色
  - 验收：✅ `compileDebugKotlin` + 全量单测通过；模拟器实测见 tools/verify 留档

- [x] **动效系统：错峰入场 + 倒计时滚动 + 今天呼吸光晕**（2026-08-30 完成，v2.1.16）
  - 背景：audit 中动效仅 2/10（UrgentCard 边框呼吸是唯一既有动效），skill 评价为 "flat and static"
  - 基建：新增 `ui/common/Motion.kt`——`Modifier.staggeredAppear(index)`（Animatable + spring(dampingRatio .62) 上浮回弹，55ms/项错峰，懒加载浮现/筛选切换重播）；`AnimatedCountdownText`（AnimatedContent 上下滚动换值，配合 tnum 等宽不横移）；`Modifier.breathingGlow(color, cornerRadius)`（drawBehind 外圈扩散描边 2.6s/周期，不受宿主裁剪）
  - 接入：Home 列表项/Hero/搜索结果全部错峰入场（Box 包裹保留 SwipeToDeleteBox 结构）；Hero 与详情页倒计时数字接滚动；BirthdayCard 今天卡、详情「就是今天」横幅接呼吸光晕（UrgentCard 原有边框呼吸保留）
  - 坑记录：drawBehind 光晕必须放在 padding/shadow 链之后，否则描边画在 padding 区域外扩 16dp；LazyColumn 项用 id 做 key，滚动复用不重播
  - 验收：✅ 全量单测通过（含既有 Compose UI 测试对无限动画兼容）；模拟器实测入场/滚动/光晕

- [x] **小组件「纸笺辰刻」打磨：调色板收敛 + 字重层级 + 夜间适配 + Hero 渐变**（2026-08-30 完成，v2.1.17）
  - 背景：audit 发现小组件全部文字 Bold 无层级、五个类型 wash 的 day/night 值完全相同（10% 透明度在深底上几乎隐形）、Violet500 文字对比度 3.7:1 不达标、色值散落
  - 调色板：新增 `widget/WidgetTheme.kt`——基础面/文字、类型强调（日 700 档 / 夜 400·300 档）、类型 wash（日 0x1A / 夜 0x2E）全部集中；BirthWidget 旧常量改为别名转发，页面不再散落硬编码
  - Hero 渐变：4×4 头从纯色 accent 升级为与 App 内 HeroCard 同源渐变（生日/情侣/其他/缅怀 × 日/夜共 8 份 shape drawable），走 `drawable/` + `drawable-night/` 限定符自动切换——Glance 1.1 的 ImageProvider 没有 day/night 双 res 构造，标准资源限定符是正解
  - 字重层级：姓名/空态标题/＋按钮/头像字 Bold→Medium，倒计时数字与 Hero 名字保持 Bold，「数字是行内唯一焦点」
  - 母题：4×2 问候语与 2×2 "NEXT" 前缀加 🌙、4×4 Hero "🌙 NEXT UP"，呼应「怀月藏星」
  - 坑记录：Glance DayNight ColorProvider 不支持 copy(alpha)，wash 用预计算 ARGB；`androidx.glance.color.ColorProvider` 是工厂函数，类型引用必须写全 `androidx.glance.unit.ColorProvider`
  - 验收：✅ 编译 + 全量单测通过；组件需真机/模拟器桌面添加后目检（模拟器截图见 tools/verify 留档）

---

## P3 — 远期可选（做前先确认定位与需求）

- [x] **文档同步检测机制**（2026-08-14 完成）
  - 本地 pre-push 钩子 + GitHub Actions 云端检测，功能代码变更而 README/TODO 未同步时提醒（不阻断）
  - 脚本入库：`tools/check-docs-sync.sh`（共享判定逻辑）、`tools/install-hooks.sh`（一键安装钩子）
- [ ] **云同步**（WebDAV 自托管）——与「数据仅存本机」隐私定位冲突，需用户确认
- [ ] **私密事件密码保护**——参照 CountdownDay 模糊遮盖
- [ ] **自动更新检查**——参照 JeffGu98（GitHub Releases + SHA-256 校验 + Wi-Fi 下自动下载）
- [x] **通知设置页**（2026-08-14 完成）
  - 实现：设置页新增「通知」分组（复用现有卡片模式）：默认提醒时间（TimePicker → prefs，新增记录默认值读取）；提醒总开关（关闭后 `scheduleBirthdayReminder` 直接取消该记录闹钟，设置页切换后全量重排）；「系统通知设置」入口（跳系统页）
  - 说明：声音/震动/锁屏不做 App 内开关——`NotificationChannel` 创建后属性被系统锁定，App 内改无效，只能引导去系统设置，这是选「入口」而非「配置」的根因
  - 验收：✅ 模拟器验证：默认时间 08→07 生效于新增页；总开关关闭后 `dumpsys alarm` 清空、重开恢复；系统通知设置页跳转成功
- [ ] **version catalog 重构**——版本号收敛到 `libs.versions.toml`
- [ ] **依赖升级**——Room 2.6.1（2023）→ 最新稳定版，与 Compose BOM 2024.12 对齐
- [x] **备份增强**（2026-08-14 完成）
  - 导入预览：`BackupMerge.classify` 逐条标记重复 → 导入前弹「逐条三选（跳过/覆盖/导入）」对话框（重复默认跳过、新记录默认导入；覆盖按判重 key 保留原 id 更新），不再无提示直接导入
  - 备份含主题设置：FORMAT_VERSION 3，encode 附 `settings` 块（themeMode/dynamicColor），导入时可勾选「同时恢复备份中的主题设置」；decodeSettings 兼容老版本（无 settings 返回 null）
  - 参照 JeffGu98 三选；「改名」需内嵌键盘编辑、交互复杂收益低，未做
  - 验收：✅ 28 个备份单测（classify 3 + settings 编解码 3）；模拟器全流程：分享备份→删除记录→导入预览→三选生效（小明恢复/重复跳过）→勾选恢复主题设置（dynamic_color 复原）

---

## 维护说明

- 完成一项 → 勾选 checkbox 并提交，commit message 注明「TODO: xxx」
- 新发现的缺陷直接补充到对应优先级，不另开会话
- 优先级可随时根据真实使用反馈调整
- **功能代码变更后**：本地 pre-push 钩子与 GitHub Actions（`.github/workflows/docs-sync-check.yml`）会自动检测 README.md / TODO.md 是否同步，未同步时提醒但不阻断；新电脑首次克隆后运行 `bash tools/install-hooks.sh` 安装本地钩子
