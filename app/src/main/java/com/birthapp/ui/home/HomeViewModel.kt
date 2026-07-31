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
import com.birthapp.lunar.SolarDate
import com.birthapp.util.DateUtils
import com.birthapp.util.EventTextUtils
import com.birthapp.util.ZodiacUtils
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索态跟关键词必须放在同一层。之前它记在页面上，
    // 点搜索结果进详情页再返回，页面状态被重置而关键词还留着，
    // 于是出现“标签栏已经回来了、列表却还在按关键词过滤”的矛盾界面
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _birthdays = database.birthdayDao().getAll()

    val displayBirthdays: StateFlow<List<BirthdayDisplay>> =
        combine(_birthdays, _selectedTab, _searchQuery) { list, tab, query ->
            val keyword = query.trim()
            val filtered = if (keyword.isNotEmpty()) {
                // 搜索时不再叠加关系标签：用户搜名字时，
                // 不应该因为当前停在“同事”标签上而搜不到家人
                list.filter {
                    it.name.contains(keyword, ignoreCase = true) ||
                            it.notes.contains(keyword, ignoreCase = true)
                }
            } else if (tab == "all") {
                list
            } else {
                list.filter { it.relation == tab }
            }
            // 已暂停的一律沉到底部：它们不会提醒，不该跟正常记录抢“最近”的位置
            filtered.map { it.toDisplay() }
                .sortedWith(compareBy({ if (it.isPaused) 1 else 0 }, { it.countdown }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: String) {
        _selectedTab.value = tab
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
            scheduler.cancelBirthdayReminder(birthday.id)
            database.birthdayDao().delete(birthday)
        }
    }

    private fun Birthday.toDisplay(): BirthdayDisplay {
        val today = LocalDate.now()
        val currentYear = today.year

        val solarDate: SolarDate = if (calendarType == "lunar") {
            try {
                LunarCalendar.getNextLunarBirthdayInSolar(birthMonth, birthDay, isLeapMonth, currentYear)
            } catch (_: Exception) {
                SolarDate(currentYear, birthMonth, birthDay)
            }
        } else {
            SolarDate(currentYear, birthMonth, birthDay)
        }

        var countdown = DateUtils.daysUntilDate(solarDate.toLocalDate())
        if (countdown < 0) {
            // 今年已过，算明年
            val nextSolar = if (calendarType == "lunar") {
                LunarCalendar.getNextLunarBirthdayInSolar(birthMonth, birthDay, isLeapMonth, currentYear + 1)
            } else {
                SolarDate(currentYear + 1, birthMonth, birthDay)
            }
            countdown = DateUtils.daysUntilDate(nextSolar.toLocalDate())
        }

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
