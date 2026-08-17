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
    val todayBanner: String,
    // v4 新增：展示用 Emoji（空字符串时自动：生日→姓名首字，其他→类型 emoji）
    val displayEmoji: String,
    // 下一次事件的阳历（年,月）：远景分层按月分组用
    val nextEventYear: Int,
    val nextEventMonth: Int
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

    // 不做「选中类型被删光时自动回退到全部」：面板里点选一个当前没有卡片的类型时，
    // 这种回退会把刚选中的类型瞬间重置回「全部」，让按钮看起来"点了没反应"。
    // 类型行行为与关系/生肖保持一致：选中即保持，空列表由「这个筛选下没有记录」
    // 空态说明，用户随时可用快捷行「全部」胶囊或面板「清除」恢复，无需自动兜底。

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

        // 下一次事件的阳历日期：远景行月份分组用（只算一次）
        val nextDate = EventCalc.nextSolarDate(this, today)

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
            todayBanner = EventTextUtils.cardBanner(eventType, name, age),
            // v4 新增字段
            displayEmoji = emoji.ifBlank {
                if (eventType == EventType.BIRTHDAY) name.first().toString()
                else EventType.emoji(eventType)
            },
            nextEventYear = nextDate.year,
            nextEventMonth = nextDate.month
        )
    }
}