package com.birthapp.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.birthapp.ui.home.BirthdayDisplay
import com.birthapp.ui.theme.*

/** 卡片柔色背景循环 */
private val cardColors = listOf(CardPeach, CardMint, CardLavender)
private val cardColorsDark = listOf(CardPeachDark, CardMintDark, CardLavenderDark)

@Composable
fun BirthdayCard(
    display: BirthdayDisplay,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false
) {
    val bgColor = if (display.isToday) {
        if (darkTheme) CardTodayDark else CardToday
    } else {
        val palette = if (darkTheme) cardColorsDark else cardColors
        palette[index % palette.size]
    }

    val tagColor = when (display.birthday.relation) {
        "family" -> Coral500
        "friend" -> Teal500
        "colleague" -> SunnyYellow700
        else -> Coral400
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = bgColor,
                spotColor = bgColor
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Row 1: Name + relation tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle
                    Surface(
                        shape = CircleShape,
                        color = tagColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = display.birthday.name.first().toString(),
                                fontWeight = FontWeight.Bold,
                                color = tagColor,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = display.birthday.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Relation tag pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = tagColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = display.relationLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = tagColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Date + zodiac + age
            Text(
                text = "${if (display.birthday.calendarType == "lunar") "\uD83C\uDF19" else "☀️"} ${display.dateLabel}  ·  ${display.zodiacEmoji}属${display.zodiac} · ${display.age}岁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 3: Countdown or today highlight
            if (display.isToday) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SunnyYellow.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "  \uD83C\uDF82 今天 ${display.age} 岁生日！",
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Coral700
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${display.countdown}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 40.sp
                        )
                        Text(
                            text = " 天后",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Teal500.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "  ⏰ ${String.format("%02d:%02d", display.birthday.reminderHour, display.birthday.reminderMinute)}  ",
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
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
            text = "还没有记录任何生日哦",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "点击下方按钮，添加家人朋友的生日\n让每一个重要的日子都不再被遗忘",
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
            Text("添加第一个生日", fontWeight = FontWeight.SemiBold)
        }
    }
}
