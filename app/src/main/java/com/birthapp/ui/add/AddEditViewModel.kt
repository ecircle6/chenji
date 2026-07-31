package com.birthapp.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.lunar.LunarCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditUiState(
    val id: Long = 0,
    val name: String = "",
    val birthYear: Int = 2000,
    val birthMonth: Int = 1,
    val birthDay: Int = 1,
    val calendarType: String = "solar",
    val isLeapMonth: Boolean = false,
    val advanceDays: Int = 0,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val relation: String = "family",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val saved: Boolean = false
)

class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun loadBirthday(id: Long) {
        if (id <= 0) return
        viewModelScope.launch {
            val b = database.birthdayDao().getById(id) ?: return@launch
            _uiState.value = AddEditUiState(
                id = b.id,
                name = b.name,
                birthYear = b.birthYear,
                birthMonth = b.birthMonth,
                birthDay = b.birthDay,
                calendarType = b.calendarType,
                isLeapMonth = b.isLeapMonth,
                advanceDays = b.advanceDays,
                reminderHour = b.reminderHour,
                reminderMinute = b.reminderMinute,
                relation = b.relation,
                notes = b.notes,
                isEditMode = true
            )
        }
    }

    fun updateName(name: String) { _uiState.value = _uiState.value.copy(name = name.take(MAX_NAME_LENGTH)) }
    fun updateBirthYear(year: Int) { _uiState.value = _uiState.value.copy(birthYear = year) }
    fun updateBirthMonth(month: Int) { _uiState.value = _uiState.value.copy(birthMonth = month) }
    fun updateBirthDay(day: Int) { _uiState.value = _uiState.value.copy(birthDay = day) }
    fun updateCalendarType(type: String) {
        val state = _uiState.value
        if (type == state.calendarType) return

        // 切换历法 = 换一种方式表达同一天，日期自动换算；换算失败则保留原数字
        val converted = runCatching {
            if (type == "lunar") {
                val lunar = LunarCalendar.solarToLunar(state.birthYear, state.birthMonth, state.birthDay)
                state.copy(
                    calendarType = type,
                    birthYear = lunar.year,
                    birthMonth = lunar.month,
                    birthDay = lunar.day,
                    isLeapMonth = lunar.isLeapMonth
                )
            } else {
                val solar = LunarCalendar.lunarToSolar(state.birthYear, state.birthMonth, state.birthDay, state.isLeapMonth)
                state.copy(
                    calendarType = type,
                    birthYear = solar.year,
                    birthMonth = solar.month,
                    birthDay = solar.day,
                    isLeapMonth = false
                )
            }
        }.getOrElse { state.copy(calendarType = type, isLeapMonth = false) }

        _uiState.value = converted
    }
    fun updateLeapMonth(isLeap: Boolean) { _uiState.value = _uiState.value.copy(isLeapMonth = isLeap) }
    fun updateAdvanceDays(days: Int) { _uiState.value = _uiState.value.copy(advanceDays = days.coerceIn(0, 365)) }
    fun updateReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            reminderHour = hour.coerceIn(0, 23),
            reminderMinute = minute.coerceIn(0, 59)
        )
    }
    fun updateRelation(relation: String) { _uiState.value = _uiState.value.copy(relation = relation) }
    fun updateNotes(notes: String) { _uiState.value = _uiState.value.copy(notes = notes.take(MAX_NOTES_LENGTH)) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return

        viewModelScope.launch {
            val birthday = Birthday(
                id = state.id,
                name = state.name.trim(),
                birthYear = state.birthYear,
                birthMonth = state.birthMonth,
                birthDay = state.birthDay,
                calendarType = state.calendarType,
                isLeapMonth = state.isLeapMonth,
                advanceDays = state.advanceDays,
                reminderHour = state.reminderHour,
                reminderMinute = state.reminderMinute,
                relation = state.relation,
                notes = state.notes,
                updatedAt = System.currentTimeMillis()
            )

            val savedId = if (state.isEditMode) {
                database.birthdayDao().update(birthday)
                birthday.id
            } else {
                database.birthdayDao().insert(birthday)
            }

            // 调度提醒
            val saved = database.birthdayDao().getById(savedId)
            if (saved != null) {
                scheduler.scheduleBirthdayReminder(saved)
            }

            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.id <= 0) return
        viewModelScope.launch {
            scheduler.cancelBirthdayReminder(state.id)
            database.birthdayDao().deleteById(state.id)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    companion object {
        // 输入长度上限：防止超长内容写入数据库和通知
        private const val MAX_NAME_LENGTH = 50
        private const val MAX_NOTES_LENGTH = 500
    }
}
