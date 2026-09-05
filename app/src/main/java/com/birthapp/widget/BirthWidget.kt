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
import com.birthapp.R
import com.birthapp.data.EventType
import com.birthapp.util.EventCalc
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 小组件一行要显示的东西。桌面空间有限，只留最必要的几项 */
data class WidgetItem(
    val name: String,
    val emoji: String,
    val countdown: Int,
    val isSolemn: Boolean
)

/**
 * 桌面小组件 — 白底列表（还原「版本 A」显示形态）。
 *
 * 机制：SizeMode.Exact + 系统 options 读值（竖屏语义，见 [realWidgetSize]）。
 * 尺寸是可观察状态（[WidgetSizeCache]）：拖拽缩放的重画由 Receiver 防抖触发
 * ——拖动中不换画面（避免桌面端切换新旧档位时的叠影），停稳后把新尺寸写入
 * 状态流，运行中的组合会话据此自动重组一次。之所以走状态流而非 update()：
 * Glance 长活会话期间 update() 只刷 state 不重组，档位会锁死在会话开始时。
 * 显示与交互完整还原 A 版（v2.1.9，白底列表时代）：
 * - 窄（宽 <200dp）：2×2 居中大字——emoji + 名字 + 「N 天后」整句，倒计时按类型配色
 * - 宽（宽 ≥200dp、高 <210dp）：「辰记」品牌头 + 右侧「＋」+ 3 行弹性列表
 *   （行框 defaultWeight 均分高度，无纸笺行色条/头像块/日期等）
 * - 大（高 ≥210dp）：同上但 5 行；记录不足时空白行垫底，防单条悬空
 * - 空态两行字无按钮；行不可点（整卡点开 App，「＋」直达新增页）
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
            filtered.map {
                WidgetItem(
                    name = it.name,
                    // 优先展示记录的专属 Emoji，空值时退回类型 Emoji
                    emoji = it.emoji.ifBlank { EventType.emoji(it.eventType) },
                    countdown = EventCalc.countdown(it),
                    isSolemn = EventType.isSolemn(it.eventType)
                )
            }
                .sortedBy { it.countdown }
                .take(MAX_ITEMS)
        }
        // 开画之前先等到第一批真实数据，让第一帧就是对的。
        // 若拿空列表当 initial，第一帧会先把「还没有记录」画上桌面，
        // 真机（尤其省电激进的机型）很可能在第二帧画出来之前就把
        // 小组件的后台会话掐掉，桌面从此定格在空状态上
        val firstItems = itemsFlow.first()
        provideContent {
            val items by itemsFlow.collectAsState(initial = firstItems)
            WidgetBody(items, appWidgetId)
        }
    }
}

// 取数上限：大档显示 5 行，宽档在 WidgetBody 里再截断
private const val MAX_ITEMS = 5

// 小组件颜色全部收敛在 [WidgetTheme]（日/夜两套 + 对比度修正），这里只留简短别名
private val BgColor get() = WidgetTheme.bg
private val NameColor get() = WidgetTheme.name
private val SubColor get() = WidgetTheme.sub
private val AccentColor get() = WidgetTheme.coral
private val NormalColor get() = WidgetTheme.teal
private val SolemnColor get() = WidgetTheme.solemn

/**
 * 真实尺寸（dp）。优先读系统 options（OPTION_APPWIDGET_MIN_WIDTH × MIN_HEIGHT，
 * 竖屏语义，launcher 放置/resize 时写入），保证档位换算与单测锁定的尺寸语义
 * 一致；options 未写入（id=-1 / 值为 0）时回退 LocalSize（Exact 下即真实尺寸，
 * 不再是 Single 时代的静态 110dp）。
 *
 * resize 的重画由 Receiver 防抖触发（拖动中不重绘、停稳后一次应用，见
 * BirthWidgetReceiver），读取放在 composition 内、不 remember：无论哪条路径
 * 触发的重绘都现读，拿到的就是当下值。
 */
@Composable
private fun realWidgetSize(appWidgetId: Int): Pair<Dp, Dp> {
    // 尺寸是可观察状态（见 WidgetSizeCache）：Receiver 防抖停稳后写入，运行中
    // 的组合会话 collect 它自动重组——Glance 长活会话期间 update() 只刷 state
    // 不重组，纯读值链路的档位会锁死在会话开始时的尺寸上
    val observed = WidgetSizeCache.stateFor(appWidgetId).size.collectAsState().value
        ?.takeIf { it.width > 0 && it.height > 0 }
    if (observed != null) {
        dbg("realWidgetSize id=$appWidgetId 来源=状态流 ${observed.width}x${observed.height}")
        return observed.width.dp to observed.height.dp
    }
    val ctx = LocalContext.current
    if (appWidgetId > 0) {
        val opts = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(appWidgetId)
        val width = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val height = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        if (width > 0 && height > 0) {
            dbg("realWidgetSize id=$appWidgetId 来源=持久化 options ${width}x$height")
            return width.dp to height.dp
        }
    }
    dbg(
        "realWidgetSize id=$appWidgetId 来源=LocalSize 兜底 " +
            "${LocalSize.current.width.value}x${LocalSize.current.height.value}"
    )
    return LocalSize.current.width to LocalSize.current.height
}

@Composable
private fun WidgetBody(items: List<WidgetItem>, appWidgetId: Int) {
    // 真实尺寸从系统 options 读（见 realWidgetSize）：按宽度/高度分流，不猜格子数
    val (width, height) = realWidgetSize(appWidgetId)
    val tier = WidgetLayout.tierOf(width.value.roundToInt(), height.value.roundToInt())
    dbg(
        "WidgetBody id=$appWidgetId ${width.value.roundToInt()}x${height.value.roundToInt()} " +
            "tier=$tier items=${items.size}"
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(20.dp)
            .padding(12.dp)
            // 点空白处进 App（行没有独立点击，整卡都进 App；「＋」的 clickable 会覆盖它）
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when {
            items.isEmpty() -> EmptyBody()
            // 只有一条时不摆列表：单行列表下面空一大片很难看，
            // 改成居中放大的大字排版，看起来是故意设计的（A 版行为）
            tier != WidgetLayout.Tier.COMPACT && items.size > 1 -> ListBody(items, tier, height)
            else -> CompactBody(items.first())
        }
    }
}

@Composable
private fun EmptyBody() {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 空态两行字、无按钮（A 版行为）：点了整卡进 App 自己加
        Text(
            text = LocalContext.current.getString(R.string.widget_empty),
            style = TextStyle(color = SubColor, fontSize = 13.sp, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun ListBody(items: List<WidgetItem>, tier: WidgetLayout.Tier, height: Dp) {
    val ctx = LocalContext.current
    // 目标行数（宽 3 / 大 5）与高度护栏的交集：行框 defaultWeight 均分高度，
    // 真实尺寸过矮（如 4×1）时按 WidgetLayout.maxRowsFor 降行数，防文字裁切
    val rows = WidgetLayout.maxRowsFor(height.value.roundToInt(), tier)
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalContext.current.getString(R.string.app_name),
                style = TextStyle(
                    color = AccentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            // 直接进新增页，省掉“开 App 再找加号”这一步
            Text(
                text = "＋",
                style = TextStyle(
                    color = AccentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 10.dp, vertical = 2.dp)
                    .clickable(actionStartActivityIntent(openAddIntent(ctx)))
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        items.take(rows).forEach { item ->
            ListRow(item, GlanceModifier.fillMaxWidth().defaultWeight())
        }
        // 记录不足行数时用空白行补齐：否则仅有的几行会被平均拉高到整个卡片，
        // 悬在卡片正中上下各空一大段
        repeat(rows - minOf(items.size, rows)) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun ListRow(item: WidgetItem, modifier: GlanceModifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.emoji, style = TextStyle(color = SubColor, fontSize = 14.sp))
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = item.name,
            maxLines = 1,
            style = TextStyle(color = NameColor, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = countdownText(LocalContext.current, item.countdown),
            style = TextStyle(
                color = countdownColor(item),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun CompactBody(item: WidgetItem) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = item.emoji, style = TextStyle(color = SubColor, fontSize = 24.sp))
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = item.name,
            maxLines = 1,
            style = TextStyle(color = NameColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = countdownText(LocalContext.current, item.countdown),
            style = TextStyle(
                color = countdownColor(item),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** 「N 天后」整句：与数字同色同放在一处，A 版观感 */
private fun countdownText(context: Context, countdown: Int): String = when (countdown) {
    0 -> context.getString(R.string.widget_countdown_today)
    1 -> context.getString(R.string.widget_countdown_tomorrow)
    else -> context.getString(R.string.widget_countdown_days, countdown)
}

/** 倒计时配色（A 版规则）：缅怀灰蓝不放大、今天珊瑚、其余青绿 */
private fun countdownColor(item: WidgetItem) = when {
    item.isSolemn -> SolemnColor
    item.countdown == 0 -> AccentColor
    else -> NormalColor
}

/**
 * 加号的跳转 intent。
 *
 * 必须带上独立的 action：两个 PendingIntent 只有 extra 不同的话系统会认为是同一个，
 * 结果点加号跳到的还是首页。
 */
private fun openAddIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_ADD)