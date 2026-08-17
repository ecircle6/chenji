package com.birthapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthapp.data.EventType
import com.birthapp.ui.home.BirthdayDisplay
import com.birthapp.ui.preview.previewBirthdays
import com.birthapp.ui.theme.*

/**
 * 列表卡片（紧凑版，首页改版效果图基准）：
 * 左 4dp 类型色条 → 圆角图标块 → 名称/类型标签 + 日期·关系 → 右侧大倒计时。
 * 移除 ⏰ 提醒时刻徽标与 infoLine（详情页仍展示）；头像圈改圆角方块。
 */
@Composable
fun BirthdayCard(
    display: BirthdayDisplay,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false
) {
    val isPaused = display.isPaused
    // 暂停的记录整体压暗，但「已暂停」标记本身不降透明度，不然标记也跟着看不清了
    val contentAlpha = if (isPaused) 0.5f else 1f

    val accentColor = if (isPaused) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else if (display.isSolemn) {
        SlateInk
    } else {
        eventAccent(display.eventType)
    }

    // 头像/图标块内容：生日用姓名首字，其余类型用 emoji，一眼分辨
    val isBirthday = display.eventType == EventType.BIRTHDAY

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .shadow(
                elevation = if (isPaused) 0.dp else 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左色条：类型强调色，4dp 圆角竖条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))

            // 圆角图标块（46dp）：eventAccent 12% 底色
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBirthday) display.birthday.name.first().toString()
                    else display.typeEmoji,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = if (isBirthday) 17.sp else 16.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // 中间：名称行（名称 + 置顶/暂停/类型标签）+ 日期·关系小字
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = display.birthday.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (display.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📌", fontSize = 11.sp)
                    }
                    if (isPaused) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "已暂停",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // 类型标签：soft 底 + eventAccent 色
                    Text(
                        text = EventType.label(display.eventType),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "${display.dateLabel} · ${display.relationLabel}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            // 右侧：倒计时 / 今天横幅
            if (display.isToday) {
                val (bannerBg, bannerFg) = eventBannerColors(display.eventType, darkTheme)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = bannerBg
                ) {
                    Text(
                        text = "  ${display.todayBanner}",
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerFg,
                        maxLines = 2
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${display.countdown}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            lineHeight = 26.sp
                        )
                    }
                    Text(
                        text = " 天后",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySearchResult(keyword: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Teal500.copy(alpha = 0.1f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "\uD83D\uDD0D", fontSize = 42.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "没有找到「$keyword」",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "可以试试姓名或备注里的关键字",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyBirthdayList(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Decorative circle
        Surface(
            shape = CircleShape,
            color = Coral500.copy(alpha = 0.1f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = "\uD83C\uDF82", fontSize = 56.sp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "还没有任何记录哦",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "点击下方按钮，添加生日或纪念日\n让每一个重要的日子都不再被遗忘",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = Coral500),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp)
        ) {
            Text("添加第一个记录", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==================== Previews ====================

@Preview(showBackground = true, locale = "zh-rCN", name = "生日卡片 · 浅色")
@Composable
private fun BirthdayCardPreview() {
    BirthAppTheme {
        BirthdayCard(display = previewBirthdays()[0], index = 0, onClick = {}, darkTheme = false)
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "生日卡片 · 深色")
@Composable
private fun BirthdayCardPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        BirthdayCard(display = previewBirthdays()[0], index = 0, onClick = {}, darkTheme = true)
    }
}

@Preview(showBackground = true, locale = "zh-rCN", name = "空列表")
@Composable
private fun EmptyBirthdayListPreview() {
    BirthAppTheme { EmptyBirthdayList(onAddClick = {}) }
}

@Preview(showBackground = true, locale = "zh-rCN", name = "空搜索结果")
@Composable
private fun EmptySearchResultPreview() {
    BirthAppTheme { EmptySearchResult(keyword = "张三") }
}