package com.birthapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.util.DateUtils
import com.birthapp.util.EventCalc
import com.birthapp.util.EventTextUtils
import com.birthapp.util.ZodiacUtils
import com.birthapp.widget.WidgetRefresher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 筛选维度键：快捷胶囊与筛选面板共用，单维更新/切换都靠它定位 */
object FilterDim {
    const val RELATION = "relation"
    const val TYPE = "type"
    const val ZODIAC = "zodiac"
}

data class BirthdayDisplay(
    val birthday: Birthday,
    val countdown: Int,
    val age: Int,
    val zodiac: String,
    val zodiacEmoji: String,
    val dateLabel: String,
    val relationLabel: String,
    val relationEmoji: String,
    val isToday: Boolean,
    val isPaused: Boolean,
    val isPinned: Boolean,
    // 以下四项由 eventType 派生，统一在 EventTextUtils 里组装，避免各入口文案不一致
    val eventType: String,
    val typeEmoji: String,
    val isSolemn: Boolean,
    val infoLine: String,
    val todayBanner: String
)

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val database: AppDatabase = (application as BirthApp).database
) : AndroidViewModel(application) {
    private val scheduler = AlarmScheduler(application, database)

    // 三个筛选维度收敛成单一不可变状态。之前是 selectedTab/selectedType 两个流，
    // 加一个维就要多加流+多个 popUpTo 同步；现在一律走一个 FilterState
    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索态跟关键词必须放在同一层。之前它记在页面上，
    // 点搜索结果进详情页再返回，页面状态被重置而关键词还留着，
    // 于是出现"标签栏已经回来了、列表却还在按关键词过滤"的矛盾界面
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _birthdays = database.birthdayDao().getAll()

    // 记录里实际出现过的类型，界面据此决定快捷行的类型胶囊（两种以上才显）
    val availableTypes: StateFlow<List<String>> =
        _birthdays.map { HomeFilter.availableTypes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 选中的类型被删光时自动回到"全部"：胶囊会随之隐藏，
        // 不能让列表停在一个看不见、也取消不掉的筛选上
        viewModelScope.launch {
            combine(availableTypes, _filter) { types, filter -> filter.type != "all" && filter.type !in types }
                .collect { stale -> if (stale) _filter.update { it.copy(type = "all") } }
        }
    }

    val displayBirthdays: StateFlow<List<BirthdayDisplay>> =
        combine(_birthdays, _filter, _searchQuery) { list, filter, query ->
            // 筛选规则全部在 HomeFilter 里（搜索跳出筛选、三维叠加）
            val filtered = HomeFilter.apply(list, filter, query)
            // 已暂停的一律沉到底部：它们不会提醒，不该跟正常记录抢"最近"的位置
            filtered.map { it.toDisplay() }
                // 排序：置顶固定在最上面（暂停的置顶记录仍置顶，只是灰显）→ 已暂停沉底 → 按倒计时
                .sortedWith(
                    compareBy(
                        { if (it.isPinned) 0 else 1 },
                        { if (it.isPaused) 1 else 0 },
                        { it.countdown }
                    )
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 快捷胶囊点击：单维切换，同时清空其他维。
     * 点「家人」→ relation=family 且 type/zodiac 复位；再点「家人」→ 该维取消。
     * 这样快捷行永远只表达一个筛选意图，不跟面板的多维叠加混淆
     */
    fun quickFilter(dim: String, value: String) {
        val current = _filter.value
        _filter.value = when (dim) {
            FilterDim.RELATION -> current.copy(
                relation = if (current.relation == value) "all" else value,
                type = "all", zodiac = null
            )
            FilterDim.TYPE -> current.copy(
                relation = "all",
                type = if (current.type == value) "all" else value,
                zodiac = null
            )
            FilterDim.ZODIAC -> current.copy(
                relation = "all", type = "all",
                zodiac = if (current.zodiac == value) null else value
            )
            else -> current
        }
    }

    /**
     * 筛选面板点选：只更新该维，不动其他维（面板内三维叠加生效）。
     * value == "all"（或 zodiac 的 null）表示清除该维
     */
    fun updateFilter(dim: String, value: String) {
        val current = _filter.value
        _filter.value = when (dim) {
            FilterDim.RELATION -> current.copy(relation = value)
            FilterDim.TYPE -> current.copy(type = value)
            FilterDim.ZODIAC -> current.copy(zodiac = value.takeUnless { it == "all" })
            else -> current
        }
    }

    /** 回全默认：快捷行的「全部」与面板的「清除」 */
    fun clearFilters() {
        _filter.value = FilterState()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun enterSearch() {
        _isSearching.value = true
    }

    // 退出搜索时顺手清空关键词，不然下次进搜索会看到上次的残留结果
    fun exitSearch() {
        _isSearching.value = false
        _searchQuery.value = ""
    }

    fun deleteBirthday(birthday: Birthday) {
        viewModelScope.launch {
            scheduler.cancelBirthdayReminder(birthday)
            database.birthdayDao().delete(birthday)
            WidgetRefresher.refresh(getApplication())
        }
    }

    private fun Birthday.toDisplay(): BirthdayDisplay {
        val today = LocalDate.now()
        val currentYear = today.year

        val countdown = EventCalc.countdown(this, today)

        val age = ZodiacUtils.getAge(birthYear, currentYear)
        val zodiac = ZodiacUtils.getZodiacName(birthYear)
        val zodiacEmoji = ZodiacUtils.getZodiacEmoji(birthYear)

        val dateLabel = if (calendarType == "lunar") {
            "农历${LunarCalendar.formatLunarDate(birthMonth, birthDay)}"
        } else {
            "${birthMonth}月${birthDay}日"
        }

        return BirthdayDisplay(
            birthday = this,
            countdown = countdown,
            age = age,
            zodiac = zodiac,
            zodiacEmoji = zodiacEmoji,
            dateLabel = dateLabel,
            relationLabel = ZodiacUtils.getRelationLabel(relation),
            relationEmoji = ZodiacUtils.getRelationEmoji(relation),
            isToday = countdown == 0,
            isPaused = !isActive,
            isPinned = pinned,
            eventType = eventType,
            typeEmoji = EventType.emoji(eventType),
            isSolemn = EventType.isSolemn(eventType),
            infoLine = EventTextUtils.infoLine(
                eventType = eventType,
                calendarType = calendarType,
                dateLabel = dateLabel,
                zodiacEmoji = zodiacEmoji,
                zodiac = zodiac,
                age = age
            ),
            todayBanner = EventTextUtils.cardBanner(eventType, name, age)
        )
    }
}