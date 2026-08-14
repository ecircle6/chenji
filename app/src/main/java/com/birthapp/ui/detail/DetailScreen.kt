package com.birthapp.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.data.EventType
import com.birthapp.ui.common.eventAccent
import com.birthapp.ui.common.eventBannerColors
import com.birthapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    birthdayId: Long,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: DetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val darkTheme = LocalDarkTheme.current

    LaunchedEffect(birthdayId) { viewModel.load(birthdayId) }

    // 两个信号都会要求退出这个页面：自己点删除（deleted）、记录在别处
    // 被删掉（notFound，比如在编辑页里删的）。但自己删除时两者几乎同时
    // 变 true，各退一次会连列表页也弹掉、留下一屏空白——必须合并成
    // 一个闸门，保证整个页面生命周期里只退一次
    var backFired by remember { mutableStateOf(false) }
    LaunchedEffect(deleted, state.notFound) {
        if ((deleted || state.notFound) && !backFired) {
            backFired = true
            onBack()
        }
    }

    // 分享卡片：生成 PNG 后拉起系统分享面板
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { uri ->
            if (uri != Uri.EMPTY) {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "分享这张卡片"))
            }
        }
    }

    // 从编辑页改完名字/类型返回时，observeById 会自动把新值推过来，这里不需要手动刷新

    var showDeleteDialog by remember { mutableStateOf(false) }

    val accent = eventAccent(state.eventType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 置顶：固定在首页列表顶部。选中态用主题色，未选中用灰
                    IconButton(
                        onClick = { viewModel.togglePinned() },
                        enabled = state.id > 0
                    ) {
                        Icon(
                            if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (state.isPinned) "取消置顶" else "置顶",
                            tint = if (state.isPinned) Coral500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 分享卡片：生成一张带倒计时的图片发到微信/朋友圈
                    IconButton(onClick = { viewModel.shareCard() }, enabled = state.id > 0) {
                        Icon(Icons.Default.Share, contentDescription = "分享卡片")
                    }
                    IconButton(onClick = { onEditClick(state.id) }, enabled = state.id > 0) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }, enabled = state.id > 0) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = Coral500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (state.notFound) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "这条记录已经不存在了",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 头部：头像 + 名字 + 类型/关系标签
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.18f),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.eventType == EventType.BIRTHDAY) {
                                state.name.take(1)
                            } else {
                                state.typeEmoji
                            },
                            fontSize = if (state.eventType == EventType.BIRTHDAY) 34.sp else 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = state.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(text = "${state.typeEmoji} ${state.eventLabel}", color = accent)
                    TagPill(text = "${state.relationEmoji} ${state.relationLabel}", color = Teal500)
                    if (!state.isActive) {
                        TagPill(text = "已暂停", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 倒计时
            if (state.isToday) {
                val (bannerBg, bannerFg) = eventBannerColors(state.eventType, darkTheme)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bannerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "就是今天",
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = bannerFg,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${state.countdown}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 46.sp,
                            color = if (state.isSolemn) {
                                if (darkTheme) SlateInkLight else SlateInk
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text(
                            text = " 天后",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // 日期信息
            DetailCard(title = "日期") {
                InfoRow(label = "记录", value = state.primaryDate)
                if (state.convertedDate.isNotEmpty()) {
                    InfoRow(label = "换算", value = state.convertedDate)
                }
                // 不叫“下次生日”：缅怀、纪念日类型也用这一行
                if (state.nextDate.isNotEmpty()) {
                    InfoRow(label = "下次", value = state.nextDate)
                }
                InfoRow(
                    label = if (EventType.usesAge(state.eventType)) "年龄" else "周年",
                    value = state.ageLine
                )
            }

            // 提醒设置
            DetailCard(title = "提醒") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isActive) "提醒已开启" else "提醒已暂停",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.isActive) {
                                "关掉后不再收到通知，记录会保留"
                            } else {
                                "这条记录不会提醒，但仍留在列表里"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isActive,
                        onCheckedChange = { viewModel.toggleActive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                // 暂停时上面的开关标题已经写了“提醒已暂停”，这里不必再重复一遍
                if (state.isActive) {
                    InfoRow(label = "下次", value = state.nextReminderText)
                }
                InfoRow(label = "方式", value = "${state.advanceText} · ${state.reminderTime}")
            }

            // 备注
            DetailCard(title = "备注") {
                Text(
                    text = state.notes.ifBlank { "还没有写备注" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.notes.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Button(
                onClick = { onEditClick(state.id) },
                enabled = state.id > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text("编辑", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定要删除「${state.name}」的记录吗？\n删除后将不再提醒。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("删除", color = Coral500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
