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
import com.birthapp.ui.preview.previewBirthdays
import com.birthapp.ui.theme.*

/**
 * Hero 聚焦卡：取最近一个事件（倒计时最小）做视觉焦点。
 * 注意取「countdown 最小」而不是「列表第一条」——置顶记录列表排最前，
 * 但可能不是最近（效果图 hero 是「在一起三周年 7 天」而非置顶的「小明 364 天」）。
 * 暂停的记录不参与：它们不提醒，不该占焦点。
 * 空列表或全暂停时返回 null（调用方决定不显示 Hero）。
 */
fun heroBirthday(birthdays: List<BirthdayDisplay>): BirthdayDisplay? =
    birthdays.filter { !it.isPaused }.minByOrNull { it.countdown }

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
    birthdays: List<BirthdayDisplay>,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = heroBirthday(birthdays) ?: return
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
        }
    }
}

// ==================== Previews ====================

@Preview(showBackground = true, locale = "zh-rCN", name = "Hero 卡 · 浅色")
@Composable
private fun HeroPreview() {
    BirthAppTheme {
        HeroCard(birthdays = previewBirthdays(), onItemClick = {})
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Hero 卡 · 深色")
@Composable
private fun HeroPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        HeroCard(birthdays = previewBirthdays(), onItemClick = {})
    }
}