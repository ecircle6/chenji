package com.birthapp.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.ui.common.BirthdayCard
import com.birthapp.ui.common.DistantRow
import com.birthapp.ui.common.EmptyBirthdayList
import com.birthapp.ui.common.EmptySearchResult
import com.birthapp.ui.common.SwipeToDeleteBox
import com.birthapp.ui.common.UrgentCard
import com.birthapp.ui.preview.previewBirthdays
import com.birthapp.ui.theme.BirthAppTheme
import com.birthapp.ui.theme.Coral500
import com.birthapp.ui.theme.LocalDarkTheme
import com.birthapp.util.Greeting
import java.time.LocalDate

// 快捷行的关系胶囊（固定四类，与历史关系取值同源）；「全部」单独一颗
private val QUICK_RELATIONS = listOf(
    "family" to "家人",
    "friend" to "朋友",
    "colleague" to "同事",
    "other" to "其他"
)

// 筛选面板里生肖的可选项（与 ZodiacUtils.getZodiacName 输出的中文串同源）
private val ZODIACS = listOf(
    "鼠", "牛", "虎", "兔", "龙", "蛇",
    "马", "羊", "猴", "鸡", "狗", "猪"
)

// 面板类型组：固定四类（老数据里的结婚/宝宝类型仍在快捷行/全部里可见）
private val PANEL_TYPES = listOf(
    EventType.BIRTHDAY, EventType.LOVE, EventType.MEMORIAL, EventType.OTHER
)

/** 星期中文名，给页首问候语行用 */
private val WEEKDAY_CN = mapOf(
    java.time.DayOfWeek.MONDAY to "周一", java.time.DayOfWeek.TUESDAY to "周二",
    java.time.DayOfWeek.WEDNESDAY to "周三", java.time.DayOfWeek.THURSDAY to "周四",
    java.time.DayOfWeek.FRIDAY to "周五", java.time.DayOfWeek.SATURDAY to "周六",
    java.time.DayOfWeek.SUNDAY to "周日"
)

/**
 * 首页入口（薄壳）：只做 ViewModel 状态收集与回调转发，
 * 渲染逻辑全部在无状态的 [HomeContent] 里，便于 @Preview 与 UI 测试直接驱动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val birthdays by viewModel.displayBirthdays.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val availableTypes by viewModel.availableTypes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    HomeContent(
        birthdays = birthdays,
        filter = filter,
        availableTypes = availableTypes,
        searchQuery = searchQuery,
        isSearching = isSearching,
        onAddClick = onAddClick,
        onItemClick = onItemClick,
        onSettingsClick = onSettingsClick,
        onQuickFilter = { dim, value -> viewModel.quickFilter(dim, value) },
        onUpdateFilter = { dim, value -> viewModel.updateFilter(dim, value) },
        onClearFilters = { viewModel.clearFilters() },
        onSearchChange = { viewModel.updateSearchQuery(it) },
        onEnterSearch = { viewModel.enterSearch() },
        onExitSearch = { viewModel.exitSearch() },
        onDeleteBirthday = { viewModel.deleteBirthday(it) },
        bottomBar = bottomBar
    )
}

/**
 * 首页纯渲染：状态 + 回调，不感知 ViewModel，可 @Preview / UI 测试。
 *
 * 布局（自上而下）：
 *   TopAppBar（搜索态内嵌输入框）
 *   问候语行（日期 · 问候语）        ← 非搜索态
 *   快捷筛选行（胶囊 + ⋯ 更多筛选）   ← 非搜索态
 *   Hero 聚焦卡                      ← 非搜索态
 *   列表 / 空态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    birthdays: List<BirthdayDisplay>,
    filter: FilterState,
    availableTypes: List<String>,
    searchQuery: String,
    isSearching: Boolean,
    onAddClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onQuickFilter: (String, String) -> Unit,
    onUpdateFilter: (String, String) -> Unit,
    onClearFilters: () -> Unit,
    onSearchChange: (String) -> Unit,
    onEnterSearch: () -> Unit,
    onExitSearch: () -> Unit,
    onDeleteBirthday: (Birthday) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var deleteTargetId by remember { mutableLongStateOf(-1L) }
    // 更多筛选底部面板开关
    var showFilterSheet by remember { mutableStateOf(false) }
    // 卡片背景色必须跟随当前主题深浅（含 App 内强制的浅/深），否则深色下会变成浅底浅字看不清
    val isDark = LocalDarkTheme.current
    val searchFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // 搜索态下的返回键先退出搜索，而不是直接退出 App
    BackHandler(enabled = isSearching) { onExitSearch() }

    LaunchedEffect(isSearching) {
        // 只在刚进搜索（还没输关键词）时自动弹键盘；
        // 从详情页返回时关键词还在，这时用户是要看结果，不该再被键盘挡住半屏。
        // 推迟到下一帧再请求：首帧焦点系统可能还没挂上 FocusRequester，
        // 立即 requestFocus 会抛「FocusRequester is not initialized」
        if (isSearching && searchQuery.isEmpty()) {
            withFrameNanos { searchFocus.requestFocus() }
        }
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
                            onValueChange = onSearchChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                            placeholder = { Text("搜姓名或备注") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
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
                        IconButton(onClick = onExitSearch) {
                            Icon(Icons.Default.Close, contentDescription = "退出搜索")
                        }
                    } else {
                        IconButton(onClick = onEnterSearch) {
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
        bottomBar = bottomBar,
        floatingActionButton = {
            // 搜索时隐藏 FAB：它会遮住结果，而且此时用户意图是找不是新增
            if (!isSearching) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = Coral500,
                    modifier = Modifier
                        .width(44.dp)
                        .height(44.dp)
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
            // 问候语行：日期 · 寄语（搜索态是全局查找，不展示，保持界面聚焦）
            if (!isSearching) {
                GreetingRow()
            }

            // 快捷筛选行 + 更多筛选面板
            if (!isSearching) {
                QuickFilterRow(
                    filter = filter,
                    availableTypes = availableTypes,
                    onQuickFilter = onQuickFilter,
                    onClearFilters = onClearFilters,
                    onMoreClick = { showFilterSheet = true }
                )
            }

            if (birthdays.isEmpty()) {
                // 搜索无结果不能用"添加第一个记录"的空页，否则会让人以为数据没了
                if (searchQuery.isNotBlank()) {
                    EmptySearchResult(keyword = searchQuery.trim())
                } else if (filter.isActive) {
                    // 筛选到空同理：数据还在，只是这个组合下没有记录
                    EmptyFilterResult()
                } else {
                    EmptyBirthdayList(onAddClick = onAddClick)
                }
            } else {
                // 搜索态：普通卡片列表（不分层）；非搜索态：三层化（紧急/标准/远景）
                val items: List<HomeListItem>? =
                    if (isSearching) null else HomeTier.buildRows(birthdays)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    // Hero 聚焦卡：只放列表首项，不参与滚动懒加载的索引语义
                    item(key = "hero") {
                        if (!isSearching) {
                            HeroCard(birthdays = birthdays, onItemClick = onItemClick)
                        }
                    }

                    if (items != null) {
                        // 分层渲染：MonthHeader / UrgentCard / BirthdayCard(标准) / DistantRow
                        items(items.size, key = { idx ->
                            when (val li = items[idx]) {
                                // 分组标题必须用 yearMonth 做 key：跨年时多个月份 label 都是「YYYY 年」，
                                // 用 label 会导致重复 key，LazyColumn 组合期抛异常
                                is HomeListItem.MonthHeader -> "header-${li.yearMonth}"
                                is HomeListItem.Card -> li.display.birthday.id
                            }
                        }) { idx ->
                            when (val li = items[idx]) {
                                is HomeListItem.MonthHeader ->
                                    MonthHeaderRow(label = li.label)

                                is HomeListItem.Card ->
                                    SwipeToDeleteBox(
                                        onDelete = { deleteTargetId = li.display.birthday.id }
                                    ) {
                                        when (li.tier) {
                                            CardTier.URGENT ->
                                                UrgentCard(
                                                    display = li.display,
                                                    onClick = { onItemClick(li.display.birthday.id) }
                                                )

                                            CardTier.NORMAL ->
                                                BirthdayCard(
                                                    display = li.display,
                                                    index = idx,
                                                    onClick = { onItemClick(li.display.birthday.id) },
                                                    darkTheme = isDark
                                                )

                                            CardTier.DISTANT ->
                                                DistantRow(
                                                    display = li.display,
                                                    onClick = { onItemClick(li.display.birthday.id) }
                                                )
                                        }
                                    }
                            }
                        }
                    } else {
                        // 搜索态：普通列表（不分层）
                        itemsIndexed(birthdays, key = { _, item -> item.birthday.id }) { index, display ->
                            SwipeToDeleteBox(
                                onDelete = { deleteTargetId = display.birthday.id }
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
    }

    // 更多筛选：底部面板，关系/类型/生肖三组叠加
    if (showFilterSheet) {
        FilterSheet(
            filter = filter,
            onUpdateFilter = onUpdateFilter,
            onClearFilters = {
                onClearFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
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
                        target?.let { onDeleteBirthday(it.birthday) }
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

/** 页首问候语行：例如「8月17日 周一 · 每一个日子都值得铭记」 */
@Composable
private fun GreetingRow() {
    val today = LocalDate.now()
    Text(
        text = "${today.monthValue}月${today.dayOfMonth}日 ${WEEKDAY_CN[today.dayOfWeek] ?: ""} · ${Greeting.today(today)}",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    )
}

/**
 * 快捷筛选行：横向滚动的胶囊 + 行尾固定「⋯ 更多筛选」。
 * 胶囊 = 「全部」+ 关系（家人/朋友/同事/其他）+ 实际出现的类型；
 * 点击走 onQuickFilter（单维切换、清空其他维）。
 */
@Composable
private fun QuickFilterRow(
    filter: FilterState,
    availableTypes: List<String>,
    onQuickFilter: (String, String) -> Unit,
    onClearFilters: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !filter.isActive,
                onClick = onClearFilters,
                label = "全部"
            )
            QUICK_RELATIONS.forEach { (key, label) ->
                FilterChip(
                    selected = filter.relation == key,
                    onClick = { onQuickFilter(FilterDim.RELATION, key) },
                    label = label
                )
            }
            availableTypes.forEach { type ->
                FilterChip(
                    selected = filter.type == type,
                    onClick = { onQuickFilter(FilterDim.TYPE, type) },
                    label = "${EventType.emoji(type)} ${EventType.label(type)}"
                )
            }
        }
        // 行尾按钮不参与滚动，始终可见
        IconButton(onClick = onMoreClick) {
            Icon(
                Icons.Default.FilterList,
                contentDescription = "更多筛选",
                tint = Coral500
            )
        }
    }
}

/** 快捷行胶囊：选中实心 + 半透明描边，未选中空心，与历史「关系/类型」胶囊观感一致 */
@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Coral500
        else MaterialTheme.colorScheme.surface,
        border = if (selected) null
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}

/**
 * 更多筛选底部面板：关系 / 类型 / 生肖三组，组内单选、组间叠加。
 * 面板内点选走 onUpdateFilter（只更新该维），与快捷行的单维切换语义区分
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: FilterState,
    onUpdateFilter: (String, String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 标题 + 当前筛选摘要 + 清除
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("更多筛选", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = summaryText(filter),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                TextButton(onClick = onClearFilters) {
                    Text("清除", color = Coral500)
                }
            }

            // 关系组
            FilterGroup(
                title = "关系",
                options = listOf("all" to "全部") +
                        QUICK_RELATIONS.map { (k, v) -> k to v },
                selected = filter.relation,
                onSelect = { onUpdateFilter(FilterDim.RELATION, it ?: "all") }
            )
            // 类型组
            FilterGroup(
                title = "类型",
                options = listOf("all" to "全部") +
                        PANEL_TYPES.map { it to EventType.label(it) },
                selected = filter.type,
                onSelect = { onUpdateFilter(FilterDim.TYPE, it ?: "all") }
            )
            // 生肖组
            FilterGroup(
                title = "生肖",
                options = listOf(null to "不限") +
                        ZODIACS.map { it to it },
                selected = filter.zodiac,
                onSelect = { onUpdateFilter(FilterDim.ZODIAC, it ?: "all") }
            )
        }
    }
}

/** 当前筛选摘要，如「家人 · 生日 · 虎」，全默认时显示「未筛选」 */
private fun summaryText(filter: FilterState): String {
    val parts = buildList {
        if (filter.relation != "all") add(
            com.birthapp.util.ZodiacUtils.getRelationLabel(filter.relation)
        )
        if (filter.type != "all") add(EventType.label(filter.type))
        filter.zodiac?.let { add(it) }
    }
    return if (parts.isEmpty()) "未筛选" else parts.joinToString(" · ")
}

/** 面板内的单选组：FlowRow 布局胶囊 */
@Composable
private fun FilterGroup(
    title: String,
    options: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (key, label) ->
                val isSelected = selected == key
                Surface(
                    onClick = { onSelect(key) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Coral500.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Coral500
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Coral500
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// 筛选组合下没有记录时的空态。不能复用"添加第一个记录"那张空页，
// 否则用户会以为数据丢了；这里只说明"这个筛选下没有"
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
            text = "换个条件看看，或点“全部”回到完整列表",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 远景层月份分隔标题：「10 月」或「2027 年」 */
@Composable
private fun MonthHeaderRow(label: String) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
    )
}

@Preview(showBackground = true, locale = "zh-rCN", name = "首页 · 浅色")
@Composable
private fun HomeContentPreview() {
    BirthAppTheme {
        HomeContent(
            birthdays = previewBirthdays(),
            filter = FilterState(),
            availableTypes = listOf("birthday", "love"),
            searchQuery = "",
            isSearching = false,
            onAddClick = {},
            onItemClick = {},
            onSettingsClick = {},
            onQuickFilter = { _, _ -> },
            onUpdateFilter = { _, _ -> },
            onClearFilters = {},
            onSearchChange = {},
            onEnterSearch = {},
            onExitSearch = {},
            onDeleteBirthday = {}
        )
    }
}

@Preview(showBackground = true, locale = "zh-rCN", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "首页 · 深色")
@Composable
private fun HomeContentPreviewDark() {
    BirthAppTheme(darkTheme = true) {
        HomeContent(
            birthdays = previewBirthdays(),
            filter = FilterState(),
            availableTypes = emptyList(),
            searchQuery = "",
            isSearching = false,
            onAddClick = {},
            onItemClick = {},
            onSettingsClick = {},
            onQuickFilter = { _, _ -> },
            onUpdateFilter = { _, _ -> },
            onClearFilters = {},
            onSearchChange = {},
            onEnterSearch = {},
            onExitSearch = {},
            onDeleteBirthday = {}
        )
    }
}