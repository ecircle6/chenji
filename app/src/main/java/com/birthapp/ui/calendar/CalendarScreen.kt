package com.birthapp.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.ui.theme.BirthAppTheme
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.SlateInk
import com.birthapp.ui.theme.Teal500
import com.birthapp.util.EventCalc
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 自绘月历视图（不引第三方日历库，符合项目零第三方依赖的约定）：
 * - 周一起始，每天标注农历日子
 * - 有事件的日子标圆点（最多 3 个 +「+N」）
 * - 点日期弹窗列出当日事件，可点击进详情
 * - 独立页展示全部记录（含暂停），不做任何筛选；落月映射走 EventCalc（处理农历跨年）
 */
@Composable
fun CalendarScreen(
    birthdays: List<Birthday>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    val eventsByDay = remember(displayedMonth, birthdays) {
        EventCalc.eventsInMonth(birthdays, displayedMonth)
    }

    // 点开的日期：pair(day, 当日事件列表)
    var selectedDay by remember { mutableStateOf<Pair<Int, List<Birthday>>?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 月份切换行：◀ 2026年8月（农历X月） ▶
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${displayedMonth.year}年${displayedMonth.monthValue}月",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                // 当月农历月名：取月中那天换算（月中不会跨农历月）
                Text(
                    lunarMonthName(displayedMonth),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 星期头（周一开头，中国习惯）
        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAYS.forEach { w ->
                Text(
                    w,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 网格：当月 1 号前的空位用上月日期占位（置灰），行数按需补足
        val firstOffset = (displayedMonth.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonth = displayedMonth.lengthOfMonth()
        val rows = (firstOffset + daysInMonth + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellDay = row * 7 + col - firstOffset + 1
                    val date = if (cellDay in 1..daysInMonth) {
                        LocalDate.of(displayedMonth.year, displayedMonth.monthValue, cellDay)
                    } else null
                    DayCell(
                        date = date,
                        isToday = date == today,
                        events = if (date != null) eventsByDay[cellDay].orEmpty() else emptyList(),
                        onDayClick = { d ->
                            val day = d.dayOfMonth
                            eventsByDay[day]?.takeIf { it.isNotEmpty() }?.let {
                                selectedDay = day to it
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 点开的日期弹出当日事件列表
    selectedDay?.let { (day, dayEvents) ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("${displayedMonth.monthValue}月${day}日", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayEvents.forEach { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedDay = null
                                    onItemClick(e.id)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(EventType.emoji(e.eventType), fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(e.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    EventType.label(e.eventType),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) { Text("关闭") }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/** 当月农历月名（如「农历六月」），换算失败返回空串 */
private fun lunarMonthName(month: YearMonth): String = runCatching {
    val mid = month.atDay(15)
    val lunar = LunarCalendar.solarToLunar(mid.year, mid.monthValue, mid.dayOfMonth)
    "农历${LunarCalendar.formatLunarDate(lunar.month, 1)}"
}.getOrDefault("")

@Composable
private fun RowScope.DayCell(
    date: LocalDate?,
    isToday: Boolean,
    events: List<Birthday>,
    onDayClick: (LocalDate) -> Unit
) {
    val dim = date == null
    val lunarLabel = if (date != null) {
        runCatching {
            LunarCalendar.lunarDayName(
                LunarCalendar.solarToLunar(date.year, date.monthValue, date.dayOfMonth).day
            )
        }.getOrDefault("")
    } else ""

    Box(
        modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = date != null && events.isNotEmpty()) {
                date?.let(onDayClick)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 阳历日（今天用主题色圆形高亮）
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isToday) Coral500 else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date?.dayOfMonth?.toString() ?: "",
                    fontSize = 13.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isToday -> Color.White
                        dim -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            // 农历小字
            Text(
                lunarLabel,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dim) 0.15f else 0.5f)
            )
            // 事件圆点：最多 3 个，超出显示 +N
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                events.take(3).forEach { e ->
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                // 缅怀固定灰蓝，其余用青色，与列表卡片配色一致
                                if (EventType.isSolemn(e.eventType)) SlateInk
                                else Teal500
                            )
                    )
                }
                if (events.size > 3) {
                    Text(
                        "+${events.size - 3}",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private val WEEKDAYS = listOf("一", "二", "三", "四", "五", "六", "日")

// ==================== Previews ====================

private fun previewCalendarBirthdays(): List<Birthday> = listOf(
    com.birthapp.ui.preview.PreviewData.birthday(id = 1, name = "小明"),
    com.birthapp.ui.preview.PreviewData.birthday(
        id = 2, name = "在一起三周年",
        eventType = EventType.LOVE, relation = "other", year = 2023
    )
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, locale = "zh-rCN", name = "日历 · 浅色")
@androidx.compose.runtime.Composable
private fun CalendarScreenPreview() {
    BirthAppTheme {
        CalendarScreen(birthdays = previewCalendarBirthdays(), onItemClick = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    locale = "zh-rCN",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    name = "日历 · 深色"
)
@androidx.compose.runtime.Composable
private fun CalendarScreenPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        CalendarScreen(birthdays = previewCalendarBirthdays(), onItemClick = {})
    }
}