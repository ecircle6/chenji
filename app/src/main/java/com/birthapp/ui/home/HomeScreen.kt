package com.birthapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.data.EventType
import com.birthapp.ui.common.BirthdayCard
import com.birthapp.ui.common.EmptyBirthdayList
import com.birthapp.ui.common.EmptySearchResult
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.LocalDarkTheme

private val TABS = listOf(
    "all" to "全部",
    "family" to "家人",
    "friend" to "朋友",
    "colleague" to "同事",
    "other" to "其他"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val birthdays by viewModel.displayBirthdays.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val availableTypes by viewModel.availableTypes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    var deleteTargetId by remember { mutableLongStateOf(-1L) }
    // 列表/月历视图切换（页面级状态，旋转屏幕后保持）
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    // 卡片背景色必须跟随当前主题深浅（含 App 内强制的浅/深），否则深色下会变成浅底浅字看不清
    val isDark = LocalDarkTheme.current
    val searchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // 搜索态下的返回键先退出搜索，而不是直接退出 App
    BackHandler(enabled = isSearching) { viewModel.exitSearch() }

    LaunchedEffect(isSearching) {
        // 只在刚进搜索（还没输关键词）时自动弹键盘；
        // 从详情页返回时关键词还在，这时用户是要看结果，不该再被键盘挡住半屏
        if (isSearching && searchQuery.isEmpty()) searchFocus.requestFocus()
    }

    // 搜索态或关键词一变，列表内容已经不是同一批了，
    // 要回到顶部；不然退出搜索后会莫名停在半中间，看不到最近的那几条
    LaunchedEffect(isSearching, searchQuery) {
        listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                            placeholder = { Text("搜姓名或备注") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Coral500,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            "辰记",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { viewModel.exitSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出搜索")
                        }
                    } else {
                        IconButton(onClick = { viewModel.enterSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "设置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            // 搜索时隐藏 FAB：它会遮住结果，而且此时用户意图是找不是新增
            if (!isSearching) {
                LargeFloatingActionButton(
                    onClick = onAddClick,
                    containerColor = Coral500
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加记录",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 列表 / 月历切换（搜索态是全局查找，只保留列表）
            if (!isSearching) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    SegmentedButton(
                        selected = !showCalendar,
                        onClick = { showCalendar = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("列表", fontSize = 13.sp)
                    }
                    SegmentedButton(
                        selected = showCalendar,
                        onClick = { showCalendar = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("月历", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Pill-style tabs（搜索是跳出关系筛选的全局查找，所以搜索态不显示标签）
            if (!isSearching) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(TABS) { (key, label) ->
                        val isSelected = selectedTab == key
                        Surface(
                            onClick = { viewModel.selectTab(key) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Coral500
                            else MaterialTheme.colorScheme.surface,
                            border = if (isSelected) null
                            else androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 10.dp
                                ),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 类型筛选胶囊：只在记录里出现了两种以上类型时才显示——
            // 只记生日的用户看不到这一排，界面保持干净；与关系标签叠加生效
            if (!isSearching && availableTypes.size >= 2) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(listOf("all") + availableTypes) { type ->
                        val isSelected = selectedType == type
                        Surface(
                            onClick = { viewModel.selectType(type) },
                            shape = RoundedCornerShape(16.dp),
                            // 用描边胶囊区分于上面实心的关系标签，避免两排长得一样分不清
                            color = if (isSelected) Coral500.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Coral500
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (type == "all") "全部类型"
                                else "${EventType.emoji(type)} ${EventType.label(type)}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Coral500
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 月历视图（复用同一份筛选结果；搜索态强制列表）
            if (showCalendar && !isSearching) {
                CalendarScreen(
                    birthdays = birthdays,
                    onItemClick = onItemClick
                )
            } else if (birthdays.isEmpty()) {
                // 搜索无结果不能用“添加第一个记录”的空页，否则会让人以为数据没了
                if (searchQuery.isNotBlank()) {
                    EmptySearchResult(keyword = searchQuery.trim())
                } else if (selectedTab != "all" || selectedType != "all") {
                    // 筛选到空同理：数据还在，只是这个组合下没有记录
                    EmptyFilterResult()
                } else {
                    EmptyBirthdayList(onAddClick = onAddClick)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(birthdays, key = { _, item -> item.birthday.id }) { index, display ->
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        deleteTargetId = display.birthday.id
                                        false
                                    } else false
                                },
                                // 默认滑一小段就算删除，手指上下滑动时带的横向偏移
                                // 很容易误触；改成要滑过卡片一半宽度才弹删除确认
                                positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                            ),
                            backgroundContent = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = Coral500
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false
                        ) {
                            BirthdayCard(
                                display = display,
                                index = index,
                                onClick = { onItemClick(display.birthday.id) },
                                darkTheme = isDark
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deleteTargetId > 0) {
        val target = birthdays.find { it.birthday.id == deleteTargetId }
        AlertDialog(
            onDismissRequest = { deleteTargetId = -1L },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${target?.birthday?.name}」的记录吗？\n删除后将不再提醒。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        target?.let { viewModel.deleteBirthday(it.birthday) }
                        deleteTargetId = -1L
                    }
                ) {
                    Text("删除", color = Coral500)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetId = -1L }) {
                    Text("取消")
                }
            }
        )
    }
}

// 筛选组合下没有记录时的空态。不能复用“添加第一个记录”那张空页，
// 否则用户会以为数据丢了；这里只说明“这个筛选下没有”
@Composable
private fun EmptyFilterResult() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "\uD83C\uDF43", fontSize = 42.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "这个筛选下没有记录",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "换个标签看看，或点“全部”回到完整列表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
