package com.birthapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.birthapp.ui.common.BirthdayCard
import com.birthapp.ui.common.EmptyBirthdayList
import com.birthapp.ui.common.EmptySearchResult
import com.birthapp.ui.theme.Coral500

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
    onEditClick: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val birthdays by viewModel.displayBirthdays.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var isSearching by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableLongStateOf(-1L) }
    // 卡片背景色必须跟随系统深色模式，否则深色下会变成浅底浅字看不清
    val isDark = isSystemInDarkTheme()
    val searchFocus = remember { FocusRequester() }

    // 退出搜索态时顺手清空关键词，不然下次进搜索会看到上次的残留结果
    fun exitSearch() {
        isSearching = false
        viewModel.clearSearch()
    }

    // 搜索态下的返回键先退出搜索，而不是直接退出 App
    BackHandler(enabled = isSearching) { exitSearch() }

    LaunchedEffect(isSearching) {
        if (isSearching) searchFocus.requestFocus()
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
                        IconButton(onClick = { exitSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "退出搜索")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
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

            // List or empty state
            if (birthdays.isEmpty()) {
                // 搜索无结果不能用“添加第一个记录”的空页，否则会让人以为数据没了
                if (searchQuery.isNotBlank()) {
                    EmptySearchResult(keyword = searchQuery.trim())
                } else {
                    EmptyBirthdayList(onAddClick = onAddClick)
                }
            } else {
                LazyColumn(
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
                                }
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
                                onClick = { onEditClick(display.birthday.id) },
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
