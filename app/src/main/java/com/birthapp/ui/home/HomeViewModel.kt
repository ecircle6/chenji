package com.birthapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.lunar.LunarCalendar
import com.birthapp.lunar.SolarDate
import com.birthapp.util.DateUtils
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
    val isToday: Boolean
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _selectedTab = MutableStateFlow("all")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _birthdays = database.birthdayDao().getAllActive()

    val displayBirthdays: StateFlow<List<BirthdayDisplay>> =
        combine(_birthdays, _selectedTab) { list, tab ->
            val filtered = if (tab == "all") list else list.filter { it.relation == tab }
            filtered.map { it.toDisplay() }.sortedBy { it.countdown }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: String) {
        _selectedTab.value = tab
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
            isToday = countdown == 0
        )
    }
}
