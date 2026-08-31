package com.birthapp.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.R
import com.birthapp.data.EventType
import com.birthapp.ui.common.AnimatedCountdownText
import com.birthapp.ui.common.breathingGlow
import com.birthapp.ui.common.eventAccent
import com.birthapp.ui.common.eventBannerColors
import com.birthapp.ui.preview.previewDetailState
import com.birthapp.ui.theme.*

/**
 * 详情页入口（薄壳）：收集 ViewModel 状态、处理「删除/未找到退出门」和分享面板拉起，
 * 渲染逻辑全部在无状态的 [DetailContent] 里，便于 @Preview 与 UI 测试直接驱动。
 */
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
                context.startActivity(Intent.createChooser(send, context.getString(R.string.detail_share_chooser)))
            }
        }
    }

    DetailContent(
        state = state,
        onBack = onBack,
        onEditClick = onEditClick,
        onTogglePinned = { viewModel.togglePinned() },
        onShare = { viewModel.shareCard() },
        onDelete = { viewModel.delete() },
        onToggleActive = { viewModel.toggleActive(it) }
    )
}

/** 详情页纯渲染：状态 + 回调，不感知 ViewModel，可 @Preview / UI 测试 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onTogglePinned: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val darkTheme = LocalDarkTheme.current

    // 从编辑页改完名字/类型返回时，observeById 会自动把新值推过来，这里不需要手动刷新
    var showDeleteDialog by remember { mutableStateOf(false) }

    val accent = eventAccent(state.eventType)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title), fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    // 置顶：固定在首页列表顶部。选中态用主题色，未选中用灰
                    IconButton(
                        onClick = onTogglePinned,
                        enabled = state.id > 0
                    ) {
                        Icon(
                            if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (state.isPinned) {
                                stringResource(R.string.detail_unpin)
                            } else {
                                stringResource(R.string.detail_pin)
                            },
                            tint = if (state.isPinned) Coral500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // 分享卡片：生成一张带倒计时的图片发到微信/朋友圈
                    IconButton(onClick = onShare, enabled = state.id > 0) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.detail_share_card))
                    }
                    IconButton(onClick = { onEditClick(state.id) }, enabled = state.id > 0) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.detail_edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, enabled = state.id > 0) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = Coral500)
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
                    text = stringResource(R.string.detail_not_found),
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
                        TagPill(
                            text = stringResource(R.string.home_paused_badge),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 倒计时
            if (state.isToday) {
                val (bannerBg, bannerFg) = eventBannerColors(state.eventType, darkTheme)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = bannerBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .breathingGlow(bannerFg)
                ) {
                    Text(
                        text = stringResource(R.string.common_just_today),
                        modifier = Modifier.padding(vertical = 20.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = bannerFg,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AnimatedCountdownText(
                            count = state.countdown,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (state.isSolemn) {
                                if (darkTheme) SlateInkLight else SlateInk
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text(
                            text = " " + stringResource(R.string.common_days_later),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            // 日期信息
            DetailCard(title = stringResource(R.string.detail_section_date)) {
                InfoRow(label = stringResource(R.string.detail_row_record), value = state.primaryDate)
                if (state.convertedDate.isNotEmpty()) {
                    InfoRow(label = stringResource(R.string.detail_row_converted), value = state.convertedDate)
                }
                // 不叫“下次生日”：缅怀、纪念日类型也用这一行
                if (state.nextDate.isNotEmpty()) {
                    InfoRow(label = stringResource(R.string.detail_row_next), value = state.nextDate)
                }
                InfoRow(
                    label = if (EventType.usesAge(state.eventType)) {
                        stringResource(R.string.detail_row_age)
                    } else {
                        stringResource(R.string.detail_row_anniversary)
                    },
                    value = state.ageLine
                )
            }

            // 提醒设置
            DetailCard(title = stringResource(R.string.detail_section_reminder)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isActive) {
                                stringResource(R.string.detail_reminder_on)
                            } else {
                                stringResource(R.string.detail_reminder_off)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (state.isActive) {
                                stringResource(R.string.detail_reminder_on_hint)
                            } else {
                                stringResource(R.string.detail_reminder_off_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                // 暂停时上面的开关标题已经写了“提醒已暂停”，这里不必再重复一遍
                if (state.isActive) {
                    InfoRow(label = stringResource(R.string.detail_row_next), value = state.nextReminderText)
                }
                InfoRow(label = stringResource(R.string.detail_row_way), value = "${state.advanceText} · ${state.reminderTime}")
            }

            // 备注
            DetailCard(title = stringResource(R.string.detail_section_notes)) {
                Text(
                    text = state.notes.ifBlank { stringResource(R.string.detail_notes_empty) },
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
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(stringResource(R.string.detail_edit), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text = { Text(stringResource(R.string.detail_delete_text, state.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.common_delete), color = Coral500)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.15f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
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
            shape = MaterialTheme.shapes.large,
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

@Preview(showBackground = true, locale = "zh-rCN", name = "详情页 · 浅色")
@Composable
private fun DetailContentPreview() {
    BirthAppTheme { DetailContent(state = previewDetailState(), onBack = {}, onEditClick = {}, onTogglePinned = {}, onShare = {}, onDelete = {}, onToggleActive = {}) }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "详情页 · 深色")
@Composable
private fun DetailContentPreviewDark() {
    BirthAppTheme(darkTheme = true) { DetailContent(state = previewDetailState(), onBack = {}, onEditClick = {}, onTogglePinned = {}, onShare = {}, onDelete = {}, onToggleActive = {}) }
}
