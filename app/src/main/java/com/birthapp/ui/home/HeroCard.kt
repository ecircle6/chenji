package com.birthapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthapp.data.EventType
import com.birthapp.ui.preview.PreviewData
import com.birthapp.ui.preview.previewBirthdays
import com.birthapp.ui.theme.*

/**
 * Hero 聚焦卡候选：聚焦「最近日期里的庆祝事件」。
 *
 * 规则：
 * - 从非暂停记录里取最小倒计时（"最近的日期"）；同一天的多条记录 countdown 相同，视为同一组
 * - 组内存在庆祝事件（非缅怀）→ 取排序第一条作为 Hero；组内全是缅怀 → null（悼念不放大）
 * - 空列表或全暂停 → null（调用方决定不显示 Hero）
 *
 * 注意取「countdown 最小」而不是「列表第一条」——置顶记录列表排最前，
 * 但可能不是最近（效果图 hero 是「在一起三周年 7 天」而非置顶的「小明 364 天」）。
 * 调用方展示 Hero 时须把它从列表去重（每条记录只出现一次）。
 */
fun heroCandidate(birthdays: List<BirthdayDisplay>): BirthdayDisplay? {
    val active = birthdays.filter { !it.isPaused }
    val min = active.minOfOrNull { it.countdown } ?: return null
    val nearest = active.filter { it.countdown == min }
    return nearest.firstOrNull { !it.isSolemn }
}

/**
 * 取 Hero 渐变配色：浅色/深色 + 按事件类型分流。
 * 缅怀固定冷灰蓝，其余生日/情侣/其他各自一支。
 */
private fun heroGradient(eventType: String, darkTheme: Boolean): Brush {
    val (start, end) = when (eventType) {
        EventType.LOVE, EventType.MARRIAGE ->
            if (darkTheme) HeroLoveDarkStart to HeroLoveDarkEnd else HeroLoveStart to HeroLoveEnd
        EventType.MEMORIAL ->
            if (darkTheme) HeroMemorialDarkStart to HeroMemorialDarkEnd else HeroMemorialStart to HeroMemorialEnd
        EventType.OTHER ->
            if (darkTheme) HeroOtherDarkStart to HeroOtherDarkEnd else HeroOtherStart to HeroOtherEnd
        else ->
            if (darkTheme) HeroBirthdayDarkStart to HeroBirthdayDarkEnd else HeroBirthdayStart to HeroBirthdayEnd
    }
    return Brush.linearGradient(listOf(start, end))
}

@Composable
fun HeroCard(
    hero: BirthdayDisplay?,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    hero ?: return
    val isDark = LocalDarkTheme.current
    val isToday = hero.isToday

    Card(
        onClick = { onItemClick(hero.birthday.id) },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient(hero.eventType, isDark))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // 「即将到来」 / 「就是今天」
                    Text(
                        text = if (isToday) "就是今天" else "即将到来",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hero.birthday.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hero.dateLabel,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${hero.countdown}",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            lineHeight = 52.sp
                        )
                        Text(
                            text = if (isToday) "" else " 天后",
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                // 右侧类型 emoji 水印：低透明度、右对齐，不抢大数字焦点
                Text(
                    text = hero.typeEmoji,
                    fontSize = 44.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(14.dp)
                )
            }

            // 紧急窗口（≤7 天）时在卡内底部显示提醒进度条，与紧急卡进度语义一致：
            // 进入 7 天窗口后每天推进 1/7，直观传达"时间在流逝"
            if (hero.countdown <= 7) {
                HeroProgressBar(
                    countdown = hero.countdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }
    }
}

/**
 * Hero 卡底部进度条：白字白条，与渐变底融合；复用 [HomeTier] 的进度纯函数
 * （progressOf/elapsedDays 已被 HomeTierTest 锁定语义，两处保持一致）。
 */
@Composable
private fun HeroProgressBar(
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val progress = HomeTier.progressOf(countdown)
    val elapsed = HomeTier.elapsedDays(countdown)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "提醒进度",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = "已过去 $elapsed 天",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, Color.White.copy(alpha = 0.6f))
                        )
                    )
            )
        }
    }
}

// ==================== Previews ====================

@Preview(showBackground = true, locale = "zh-rCN", name = "Hero 卡 · 浅色（紧急层带进度条）")
@Composable
private fun HeroPreview() {
    BirthAppTheme {
        HeroCard(hero = heroCandidate(previewBirthdays()), onItemClick = {})
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Hero 卡 · 深色")
@Composable
private fun HeroPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        HeroCard(hero = heroCandidate(previewBirthdays()), onItemClick = {})
    }
}

@Preview(showBackground = true, locale = "zh-rCN", name = "Hero 卡 · 标准层（>7 天无进度条）")
@Composable
private fun HeroPreviewNormal() {
    BirthAppTheme {
        val hero = PreviewData.display(
            PreviewData.birthday(id = 1, name = "妈妈生日", month = 9, day = 4),
            countdown = 18
        )
        HeroCard(hero = heroCandidate(listOf(hero)), onItemClick = {})
    }
}