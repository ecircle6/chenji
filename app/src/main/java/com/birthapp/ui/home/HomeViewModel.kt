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

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _selectedTab = MutableStateFlow("all")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    // 类型筛选（生日/情侣/缅怀…），与关系标签叠加生效
    private val _selectedType = MutableStateFlow("all")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索态跟关键词必须放在同一层。之前它记在页面上，
    // 点搜索结果进详情页再返回，页面状态被重置而关键词还留着，
    // 于是出现“标签栏已经回来了、列表却还在按关键词过滤”的矛盾界面
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _birthdays = database.birthdayDao().getAll()

    // 记录里实际出现过的类型，界面据此决定类型胶囊行是否显示（两种以上才显）
    val availableTypes: StateFlow<List<String>> =
        _birthdays.map { HomeFilter.availableTypes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 选中的类型被删光时自动回到“全部”：胶囊会随之隐藏，
        // 不能让列表停在一个看不见、也取消不掉的筛选上
        viewModelScope.launch {
            combine(availableTypes, _selectedType) { types, type -> type != "all" && type !in types }
                .collect { stale -> if (stale) _selectedType.value = "all" }
        }
    }

    val displayBirthdays: StateFlow<List<BirthdayDisplay>> =
        combine(_birthdays, _selectedTab, _selectedType, _searchQuery) { list, tab, type, query ->
            // 筛选规则全部在 HomeFilter 里（搜索跳出筛选、关系+类型叠加）
            val filtered = HomeFilter.apply(list, tab, type, query)
            // 已暂停的一律沉到底部：它们不会提醒，不该跟正常记录抢“最近”的位置
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

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun selectType(type: String) {
        _selectedType.value = type
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
