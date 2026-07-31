package com.birthapp.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
// Intent 版的 actionStartActivity 只在 glance-appwidget 里，跟上面那个同名，所以起个别名区分
import androidx.glance.appwidget.action.actionStartActivity as actionStartActivityIntent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
// 日/夜双色的 ColorProvider 在 color 包里，不是 androidx.glance.unit 下那个单色重载
import androidx.glance.color.ColorProvider
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
import com.birthapp.data.EventType
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.SlateInk
import com.birthapp.ui.theme.SlateInkLight
import com.birthapp.ui.theme.SurfaceDark
import com.birthapp.ui.theme.Teal500
import com.birthapp.ui.theme.TextOnDark
import com.birthapp.ui.theme.TextOnDarkSecondary
import com.birthapp.ui.theme.TextPrimary
import com.birthapp.ui.theme.TextSecondary
import com.birthapp.util.EventCalc
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** 小组件上一行要显示的东西。桌面空间有限，只留最必要的几项 */
data class WidgetItem(
    val name: String,
    val emoji: String,
    val countdown: Int,
    val isSolemn: Boolean
)

/**
 * 桌面小组件。
 *
 * 一个 provider 用 SizeMode.Responsive 同时支持两种尺寸：
 * 宽的（4×2）列出最近 3 条，方的（2×2）只放最近 1 条但字更大。
 * 这样在桌面上拖拽缩放就能切换，不必在小组件列表里看到两个「辰记」不知道该选哪个。
 */
class BirthWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 坐在 Flow 上订阅而不是进来时读一次快照：provideContent 之前的代码
        // 只在会话重建时跑，App 里喊 refresh 不会重新执行它——之前就是因此
        // 增删记录后桌面一直停在旧数据上。改成 Flow 后数据库一变自动重画
        val db = (context.applicationContext as BirthApp).database
        val itemsFlow = db.birthdayDao().getAllActive().map { list ->
            list.map {
                WidgetItem(
                    name = it.name,
                    emoji = EventType.emoji(it.eventType),
                    countdown = EventCalc.countdown(it),
                    isSolemn = EventType.isSolemn(it.eventType)
                )
            }
                .sortedBy { it.countdown }
                .take(MAX_VISIBLE_ROWS)
        }
        // 开画之前先等到第一批真实数据，让第一帧就是对的。
        // 若拿空列表当 initial，第一帧会先把「还没有记录」画上桌面，
        // 真机（尤其省电激进的机型）很可能在第二帧画出来之前就把
        // 小组件的后台会话掐掉，桌面从此定格在空状态上
        val firstItems = itemsFlow.first()
        provideContent {
            val items by itemsFlow.collectAsState(initial = firstItems)
            WidgetBody(items)
        }
    }

    companion object {
        private val COMPACT = DpSize(120.dp, 120.dp)
        private val WIDE = DpSize(250.dp, 120.dp)
    }
}

// WideBody 补空白行时也要用，而 companion 里的私有常量到不了顶层函数，单独抽一个
private const val MAX_VISIBLE_ROWS = 3

// 小组件不跟随 App 内的 Material 主题，得自己给日/夜两套颜色
private val BgColor = ColorProvider(day = Color.White, night = SurfaceDark)
private val NameColor = ColorProvider(day = TextPrimary, night = TextOnDark)
private val SubColor = ColorProvider(day = TextSecondary, night = TextOnDarkSecondary)
private val AccentColor = ColorProvider(day = Coral500, night = Coral500)
private val NormalColor = ColorProvider(day = Teal500, night = Teal500)
private val SolemnColor = ColorProvider(day = SlateInk, night = SlateInkLight)

@Composable
private fun WidgetBody(items: List<WidgetItem>) {
    // 按宽度而不是格子数判断：各家桌面一格的实际宽度差别很大
    val isWide = LocalSize.current.width >= 200.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(20.dp)
            .padding(12.dp)
            // 点空白处进 App
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when {
            items.isEmpty() -> EmptyBody()
            // 只有一条时不摆列表：单行列表下面空一大片很难看，
            // 改成居中放大的卡片排版，看起来是故意设计的
            isWide && items.size > 1 -> WideBody(items)
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
        Text(
            text = "还没有记录\n点这里添加",
            style = TextStyle(color = SubColor, fontSize = 13.sp, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun WideBody(items: List<WidgetItem>) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "辰记",
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
                    .clickable(actionStartActivityIntent(openAddIntent(LocalContext.current)))
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        // 每行摊掉一份剩余高度，否则 3 行会挤在顶部、下面空一大片。
        // defaultWeight 只在 Column 的 lambda 里有（它是 ColumnScope 的扩展），
        // 所以得在这里算好再交给 WideRow
        items.forEach { item ->
            WideRow(item, GlanceModifier.fillMaxWidth().defaultWeight())
        }
        // 记录不足 3 条时用空白行补齐：否则仅有的几行会被平均拉高到整个卡片，
        // 只有一条记录时它会孤零零地悬在卡片正中，上下各空一大段
        repeat(MAX_VISIBLE_ROWS - items.size) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun WideRow(item: WidgetItem, modifier: GlanceModifier) {
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
            text = countdownText(item.countdown),
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
            text = countdownText(item.countdown),
            style = TextStyle(
                color = countdownColor(item),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun countdownText(countdown: Int): String = when (countdown) {
    0 -> "就是今天"
    1 -> "明天"
    else -> "$countdown 天后"
}

private fun countdownColor(item: WidgetItem) = when {
    // 缅怀用素净的灰蓝，不跟着高亮成暖色
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
