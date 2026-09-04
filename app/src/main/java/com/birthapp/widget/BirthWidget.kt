package com.birthapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.birthapp.BirthApp
import com.birthapp.MainActivity
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.util.EventCalc
import com.birthapp.util.Greeting
import com.birthapp.util.ZodiacUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 小组件上一行要显示的东西。桌面空间有限，只留最必要的几项 */
data class WidgetItem(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val countdown: Int,
    val isSolemn: Boolean,
    val eventType: String = EventType.BIRTHDAY,
    val avatarText: String = name.take(1),
    val typeLabel: String = EventType.label(eventType),
    val dateLabel: String = "",
    val relationLabel: String = ""
)

/**
 * 一次取数的结果：items 是倒计时最近的前 N 条，total 是真实总数
 * （大档「共 N 个日子」用——取数有上限后不能拿 items.size 顶替）。
 */
data class WidgetData(val total: Int, val items: List<WidgetItem>)

/**
 * 桌面小组件 — 纸笺辰刻（Paper Slip, Time Carved）。
 *
 * SizeMode.Exact：LocalSize 是系统实际尺寸（整数 dp），组合订阅尺寸变化——
 * resize 时 Glance 会重新组合并拿到新尺寸，档位/行数随真实宽高实时切换
 * （曾用 SizeMode.Single + 组合内读 options 的方案：Single 下 LocalSize 恒为
 * manifest 静态 110dp、组合不订阅尺寸，resize 后组合复用导致档位锁死在旧值，
 * 表现为「缩到最小还是多条」「放大回不去多条」。Exact 修复此链路）。
 * 布局按真实宽高分流（阈值与行数换算见 WidgetLayout）：
 * 窄（宽 <200dp）：日历撕页单焦，NEXT + 头像 + 倒计时
 * 宽而矮：问候语头部 + N 行纸笺行（N 按实际高度算），行框均分剩余高度铺满
 * 宽而高（高 ≥210dp）：Hero 渐变头 + 「其他近期」+ 列表行，同样均分铺满
 * 行框均分（defaultWeight）是关键：内容始终填满整个组件，不留沉底或夹心的空白段。
 * 首次添加会打开配置页（android:configure），可指定只展示某一条记录。
 */
class BirthWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 坐在 Flow 上订阅而不是进来时读一次快照：provideContent 之前的代码
        // 只在会话重建时跑，App 里喊 refresh 不会重新执行它——之前就是因此
        // 增删记录后桌面一直停在旧数据上。改成 Flow 后数据库一变自动重画
        val db = (context.applicationContext as BirthApp).database
        // 配置页选择：auto 或指定记录 id（指定记录被删后自动退回空态，不崩）
        val appWidgetId = (id as? AppWidgetId)?.appWidgetId ?: -1
        val selection = if (appWidgetId > 0) {
            WidgetConfigStore.get(context, appWidgetId)
        } else {
            WidgetConfigStore.AUTO
        }
        val selectedId = selection.toLongOrNull()
        val itemsFlow = db.birthdayDao().getAllActive().map { list ->
            val filtered = if (selectedId != null) list.filter { it.id == selectedId } else list
            WidgetData(
                total = filtered.size,
                items = filtered.map {
                val isSolemn = EventType.isSolemn(it.eventType)
                val emoji = it.emoji.ifBlank { EventType.emoji(it.eventType) }
                val avatarText = if (it.eventType == EventType.BIRTHDAY) {
                    it.name.take(1).ifBlank { "🎂" }
                } else {
                    emoji
                }
                val dateLabel = if (it.calendarType == "lunar") {
                    "农历${LunarCalendar.formatLunarDate(it.birthMonth, it.birthDay)}"
                } else {
                    "${it.birthMonth}月${it.birthDay}日"
                }
                WidgetItem(
                    id = it.id,
                    name = it.name,
                    emoji = emoji,
                    countdown = EventCalc.countdown(it),
                    isSolemn = isSolemn,
                    eventType = it.eventType,
                    avatarText = avatarText,
                    typeLabel = EventType.label(it.eventType),
                    dateLabel = dateLabel,
                    relationLabel = ZodiacUtils.getRelationLabel(it.relation)
                )
}
                    .sortedBy { it.countdown }
                    .take(MAX_ITEMS)
            )
        }
        // 开画之前先等到第一批真实数据，让第一帧就是对的。
        // 若拿空列表当 initial，第一帧会先把「还没有记录」画上桌面，
        // 真机（尤其省电激进的机型）很可能在第二帧画出来之前就把
        // 小组件的后台会话掐掉，桌面从此定格在空状态上
        val firstData = itemsFlow.first()
        provideContent {
            val data by itemsFlow.collectAsState(initial = firstData)
            WidgetBody(data.items, data.total, appWidgetId)
        }
    }
}

// 取数上限：宽档最多 5 行、大档 Hero+3 行，统一取 5 条已够各档渲染
private const val MAX_ITEMS = 5

// 小组件颜色全部收敛在 [WidgetTheme]（日/夜两套 + 对比度修正），这里只留简短别名
private val BgColor get() = WidgetTheme.bg
private val RowBgProvider get() = WidgetTheme.rowBg
private val NameColor get() = WidgetTheme.name
private val SubColor get() = WidgetTheme.sub
private val AccentColor get() = WidgetTheme.coral
private val WhiteProvider get() = WidgetTheme.white
private val WhiteAlpha90 get() = WidgetTheme.white90
private val WhiteAlpha85 get() = WidgetTheme.white85

private fun widgetAccent(item: WidgetItem) = WidgetTheme.accent(item.eventType, item.isSolemn)

private fun avatarBg(item: WidgetItem) = WidgetTheme.wash(item.eventType)

/**
 * 真实尺寸（dp）。SizeMode.Exact 下 LocalSize 就是系统实际尺寸（整数 dp，
 * 竖屏语义，launcher 放置/resize 时驱动），且组合订阅尺寸变化——resize 时
 * Glance 重新组合、必拿到新值，档位随真实宽高切换（不再依赖手工 update
 * 与组合内现读 options 的旧链路）。这里不 remember，任何路径的重绘都现读。
 *
 * LocalSize 异常（理论不会：Exact 下恒有值）时兜底读系统 options
 * OPTION_APPWIDGET_MIN_WIDTH × MIN_HEIGHT（放置/resize 由 launcher 写入）。
 */
@Composable
private fun realWidgetSize(appWidgetId: Int): Pair<Dp, Dp> {
    // SizeMode.Exact：LocalSize 是系统实际尺寸（整数 dp），且组合订阅尺寸变化——
    // resize 时 Glance 会重新组合，这里每次都能拿到新值（Single 模式下 LocalSize
    // 恒为 manifest 静态 110dp 且组合不订阅尺寸，正是 resize 不更新的根因）。
    val local = LocalSize.current
    if (local.width.value > 0 && local.height.value > 0) return local.width to local.height
    // 兜底：LocalSize 异常时现读系统 options（放置/resize 时由 launcher 写入）
    if (appWidgetId > 0) {
        val options = AppWidgetManager.getInstance(LocalContext.current)
            .getAppWidgetOptions(appWidgetId)
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        if (width > 0 && height > 0) return width.dp to height.dp
    }
    return LocalSize.current.width to LocalSize.current.height
}

@Composable
private fun WidgetBody(items: List<WidgetItem>, total: Int, appWidgetId: Int) {
    // 真实尺寸从系统读（见 realWidgetSize）：按宽度/高度分流，不猜格子数
    val (width, height) = realWidgetSize(appWidgetId)
    val tier = WidgetLayout.tierOf(width.value.roundToInt(), height.value.roundToInt())
    // 窄高时可用空间极小，需更小内边距避免内容溢出裁切
    val outerPadding = when (tier) {
        WidgetLayout.Tier.LARGE -> WidgetLayout.LARGE_OUTER_PADDING.dp
        WidgetLayout.Tier.COMPACT -> 8.dp
        WidgetLayout.Tier.WIDE -> WidgetLayout.WIDE_OUTER_PADDING.dp
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(24.dp)
            .padding(outerPadding)
            // 点空白处进 App（行与按钮的 clickable 会覆盖它）
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when {
            items.isEmpty() -> EmptyBody()
            tier == WidgetLayout.Tier.LARGE && items.size > 1 ->
                LargeBody(items, total, height)
            tier == WidgetLayout.Tier.WIDE && items.size > 1 ->
                WideBody(items, height)
            else -> CompactBody(items.first())
        }
    }
}

@Composable
private fun EmptyBody() {
    val ctx = LocalContext.current
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp 圆，wash 底（日夜透明度分流见 WidgetTheme）
        Box(
            modifier = GlanceModifier
                .width(44.dp).height(44.dp)
                .background(WidgetTheme.wash(EventType.BIRTHDAY))
                .cornerRadius(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🎂", style = TextStyle(fontSize = 18.sp))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "还没有记录",
            style = TextStyle(color = NameColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "点这里添加第一个重要的日子",
            style = TextStyle(color = SubColor, fontSize = 11.sp, textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Box(
            modifier = GlanceModifier
                .background(AccentColor)
                .cornerRadius(20.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable(actionStartActivityIntent(openAddIntent(ctx))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "＋ 添加", style = TextStyle(color = WhiteProvider, fontSize = 12.sp, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun WideBody(items: List<WidgetItem>, height: Dp) {
    val ctx = LocalContext.current
    val greeting = rememberCompactGreeting()
    // 行数随真实高度自适应（chrome 扣减与换算见 WidgetLayout.wideListHeight）；
    // 行框均分剩余高度、行内容垂直居中：组件拉到多高都铺满，没有沉底或夹心空白段
    val listHeight = WidgetLayout.wideListHeight(height.value.roundToInt())
    val maxRows = WidgetLayout.rowsFor(listHeight).coerceAtMost(items.size)
    val dense = WidgetLayout.rowNeedsDense(listHeight, maxRows)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greeting,
                maxLines = 1,
                style = TextStyle(color = SubColor, fontSize = 10.sp),
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            // 20dp 圆形＋（原 26dp）：4×2 只有 ~135dp 高，问候行要为纸笺行省纵向空间
            Box(
                modifier = GlanceModifier
                    .width(20.dp).height(20.dp)
                    .background(AccentColor)
                    .cornerRadius(10.dp)
                    .clickable(actionStartActivityIntent(openAddIntent(ctx))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "＋", style = TextStyle(color = WhiteProvider, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
            }
        }
        Spacer(modifier = GlanceModifier.height(WidgetLayout.GREETING_GAP.dp))
        items.take(maxRows).forEach { item ->
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                WidgetRow(item = item, dense = dense)
            }
        }
    }
}

@Composable
private fun LargeBody(items: List<WidgetItem>, total: Int, height: Dp) {
    // Hero 取最近且非缅怀的那条，其余进列表；调用方保证 items 至少 2 条，hero 必非空
    val hero = items.filter { !it.isSolemn }.minByOrNull { it.countdown } ?: items.firstOrNull()
        ?: return
    val rest = items.filterNot { it.id == hero.id }
    // 行数随真实高度自适应（chrome 扣减见 WidgetLayout.largeListHeight）；
    // 行框均分剩余高度、行内容垂直居中：高度富余时行距自然拉开，整面铺满无空白
    val listHeight = WidgetLayout.largeListHeight(height.value.roundToInt())
    val maxRows = WidgetLayout.rowsFor(listHeight).coerceAtMost(rest.size)
    val dense = WidgetLayout.rowNeedsDense(listHeight, maxRows)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        HeroWidgetHeader(hero)
        Spacer(modifier = GlanceModifier.height(WidgetLayout.LARGE_HEADER_GAP.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "其他近期", style = TextStyle(color = SubColor, fontSize = 10.sp, fontWeight = FontWeight.Medium))
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(text = "共 $total 个日子", style = TextStyle(color = SubColor, fontSize = 10.sp))
        }
        Spacer(modifier = GlanceModifier.height(WidgetLayout.LARGE_LIST_GAP.dp))
        rest.take(maxRows).forEach { item ->
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                WidgetRow(item = item, compact = true, dense = dense)
            }
        }
    }
}

@Composable
private fun HeroWidgetHeader(item: WidgetItem) {
    val ctx = LocalContext.current
    // Hero 用与 App 内 HeroCard 同源的渐变底（drawable 日/夜两份），白字
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetTheme.heroImage(item.eventType))
            .cornerRadius(14.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivityIntent(detailIntent(ctx, item.id))),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "🌙 NEXT UP", style = TextStyle(color = WhiteAlpha90, fontSize = 10.sp))
                Spacer(modifier = GlanceModifier.height(2.dp))
                // 头高压到 60dp（大档 chrome 让位给第 4 行列表，4×4 即显示 5 条）：
                // 日期·关系在列表行里已有，Hero 只留名称 + 右侧倒计时
                Text(text = item.name, maxLines = 1, style = TextStyle(color = WhiteProvider, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${item.countdown}", style = TextStyle(color = WhiteProvider, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                Text(text = if (item.countdown == 0) "就是今天" else "天后", style = TextStyle(color = WhiteAlpha90, fontSize = 10.sp, textAlign = TextAlign.Center))
            }
        }
    }
}

@Composable
private fun WidgetRow(item: WidgetItem, compact: Boolean = false, dense: Boolean = false) {
    val ctx = LocalContext.current
    val isUrgent = !item.isSolemn && item.countdown <= 7
    val accent = widgetAccent(item)
    val solidProvider = WidgetTheme.accentSolid(item.eventType, item.isSolemn)
    val detail = detailIntent(ctx, item.id)
    // 紧凑串：行框被压到 52dp 以下时（常见 4×2 两行）再缩一档，防文字/头像溢出裁切
    val avatarSize = if (dense) 32.dp else 36.dp
    val innerVPad = when { dense -> 6.dp; compact -> 7.dp; else -> 8.dp }
    val nameSize = if (dense) 12.sp else 13.sp
    val subSize = if (dense) 10.sp else 11.sp
    val countSize = when { dense -> 16.sp; compact -> 15.sp; else -> 18.sp }

    // 呼吸边框：Glance 无 border，用外层 accent 底 + 2dp padding 模拟 1.5dp 描边
    val outerBg = if (isUrgent) accent else RowBgProvider
    val outerPadding = if (isUrgent) 2.dp else 0.dp
    val innerRadius = if (isUrgent) 12.dp else 14.dp

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(outerBg)
            .cornerRadius(14.dp)
            .padding(outerPadding)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(RowBgProvider)
                .cornerRadius(innerRadius)
                .padding(horizontal = 10.dp, vertical = innerVPad)
                .clickable(actionStartActivityIntent(detail))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左 3.5dp 色条
                Box(
                    modifier = GlanceModifier
                        .width(4.dp).height(avatarSize)
                        .background(accent)
                        .cornerRadius(3.dp)
                ) {}
                Spacer(modifier = GlanceModifier.width(10.dp))
                // 36dp 圆头像：类型 wash 底（日 10% / 夜 18%）
                Box(
                    modifier = GlanceModifier
                        .width(avatarSize).height(avatarSize)
                        .background(avatarBg(item))
                        .cornerRadius(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.avatarText,
                        style = TextStyle(color = solidProvider, fontSize = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    )
                }
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            // 名字 Medium：数字 Bold 是行内唯一焦点（字重层级）
                            style = TextStyle(color = NameColor, fontSize = nameSize, fontWeight = FontWeight.Medium)
                        )
                        if (!compact && !item.isSolemn && item.countdown <= 7) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = "● 急",
                                style = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        if (!compact) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = item.typeLabel,
                                style = TextStyle(color = accent, fontSize = 10.sp)
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "${item.dateLabel} · ${item.relationLabel}",
                        maxLines = 1,
                        style = TextStyle(color = SubColor, fontSize = subSize)
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = countdownText(item.countdown),
                        style = TextStyle(color = accent, fontSize = countSize, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    )
                    if (!compact) {
                        Text(text = "天后", style = TextStyle(color = SubColor, fontSize = 9.sp, textAlign = TextAlign.End))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactBody(item: WidgetItem) {
    val ctx = LocalContext.current
    val accent = widgetAccent(item)
    val solidProvider = WidgetTheme.accentSolid(item.eventType, item.isSolemn)
    val isSolemn = item.isSolemn
    // 2×2可用高度仅~96dp（120-12*2），外加圆角裁切，纵向必须极度收紧；同时横向宽度窄，文字易被椭圆遮罩裁掉末字
    Column(
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 将“NEXT”字号缩至 8sp，避免被顶边圆角裁切；Glance下超宽文字横向裁切敏感，越短越安全
        Text(text = "🌙 NEXT", style = TextStyle(color = SubColor, fontSize = 8.sp, textAlign = TextAlign.Center))
        Spacer(modifier = GlanceModifier.height(3.dp))
        Box(
            modifier = GlanceModifier
                .width(42.dp).height(42.dp)
                .background(avatarBg(item))
                .cornerRadius(21.dp)
                .clickable(actionStartActivityIntent(detailIntent(ctx, item.id))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.avatarText,
                style = TextStyle(color = solidProvider, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            )
        }
        Spacer(modifier = GlanceModifier.height(3.dp))
        Text(
            text = item.name,
            maxLines = 1,
            style = TextStyle(color = NameColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(1.dp))
        Text(
            text = "${item.dateLabel} · ${item.relationLabel}",
            maxLines = 1,
            style = TextStyle(color = SubColor, fontSize = 9.sp, textAlign = TextAlign.Center)
        )
        Spacer(modifier = GlanceModifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.clickable(actionStartActivityIntent(detailIntent(ctx, item.id)))
        ) {
            // 倒计时适度收小至 26sp，避免与底部圆角/文字裁切重叠
            Text(
                text = "${item.countdown}",
                style = TextStyle(color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )
            Spacer(modifier = GlanceModifier.width(3.dp))
            Text(text = "天后", style = TextStyle(color = SubColor, fontSize = 10.sp))
        }
        // 2×2可用高度极窄，不再放进度文字，避免纵向溢出被底边圆角吃掉
    }
}

private fun countdownText(countdown: Int): String = when (countdown) {
    0 -> "就是今天"
    1 -> "明天"
    else -> "$countdown"
}

@Composable
private fun rememberGreeting(): String {
    val today = LocalDate.now()
    val datePart = try {
        val fmt = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
        today.format(fmt)
    } catch (_: Exception) {
        "${today.monthValue}月${today.dayOfMonth}日"
    }
    return "$datePart · ${Greeting.today(today)}"
}

@Composable
private fun rememberCompactGreeting(): String {
    // 4×2可用宽度仅 222dp，完整问候语易溢出被截断，需优先缩短
    val today = LocalDate.now()
    val datePart = try {
        val fmt = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.SIMPLIFIED_CHINESE)
        today.format(fmt)
    } catch (_: Exception) {
        "${today.monthValue}月${today.dayOfMonth}日"
    }
    val g = Greeting.today(today)
    // 取前 6 字并加省略号，避免“岁月漫长，值得…”占满导致“天后”被挤压
    // 用 Greeting 的短句时 10sp 仍可能溢出，但比之前 11sp+4字更稳
    // 前缀 🌙 呼应「怀月藏星」图标母题
    return "🌙 $datePart · $g"
}

/**
 * 加号的跳转 intent。
 *
 * 必须带上独立的 action：两个 PendingIntent 只有 extra 不同的话系统会认为是同一个，
 * 结果点加号跳到的还是首页。
 */
private fun openAddIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_ADD)

private fun detailIntent(context: Context, id: Long): Intent =
    Intent(context, MainActivity::class.java)
        .setAction("com.birthapp.action.OPEN_DETAIL_$id")
        .putExtra(AlarmScheduler.EXTRA_BIRTHDAY_ID, id)
