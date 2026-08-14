package com.birthapp.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.alarm.normalizeAdvanceLevels
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.settings.ReminderSettings
import com.birthapp.widget.WidgetRefresher
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
    /** 多级提前提醒：0=当天，升序去重，保存时再 normalize 一遍 */
    val advanceDays: List<Int> = listOf(0),
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val relation: String = "family",
    val eventType: String = EventType.BIRTHDAY,
    val notes: String = "",
    // 编辑一条已暂停的记录时要把这个状态带回去，不能静默恢复提醒
    val isActive: Boolean = true,
    val isEditMode: Boolean = false,
    val saved: Boolean = false
)

class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        // 新记录的默认提醒时间取设置里的默认值（编辑已有记录不受影响，
        // loadBirthday 会用自己的时间覆盖）
        val reminder = ReminderSettings(getApplication())
        _uiState.value = AddEditUiState(
            reminderHour = reminder.defaultHour,
            reminderMinute = reminder.defaultMinute
        )
    }

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
                eventType = b.eventType,
                notes = b.notes,
                isActive = b.isActive,
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

    /**
     * 多选切换一个提前级别（预设 chips 与自定义值共用）。
     * 再点一次已选中的级别 = 移除；移除到空列表时退化为 [0]（当天）
     */
    fun toggleAdvanceDay(day: Int) {
        val state = _uiState.value
        val current = state.advanceDays
        val updated = if (day in current) current - day else current + day
        _uiState.value = state.copy(advanceDays = normalizeAdvanceLevels(updated))
    }

    /** 自定义 dialog 添加一个级别（0..365，normalize 内会去重、限 10 个） */
    fun addCustomAdvanceDay(day: Int) {
        if (day !in 0..365) return
        val state = _uiState.value
        _uiState.value = state.copy(advanceDays = normalizeAdvanceLevels(state.advanceDays + day))
    }

    /** 自定义 dialog 移除一个级别 */
    fun removeAdvanceDay(day: Int) = toggleAdvanceDay(day)
    fun updateReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            reminderHour = hour.coerceIn(0, 23),
            reminderMinute = minute.coerceIn(0, 59)
        )
    }
    fun updateRelation(relation: String) { _uiState.value = _uiState.value.copy(relation = relation) }
    fun updateEventType(eventType: String) { _uiState.value = _uiState.value.copy(eventType = eventType) }
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
                advanceDays = normalizeAdvanceLevels(state.advanceDays),
                reminderHour = state.reminderHour,
                reminderMinute = state.reminderMinute,
                relation = state.relation,
                // 必须显式带上：不传的话 Birthday 会用默认值 birthday，
                // 编辑一条缅怀记录再保存就会默默变回生日
                eventType = state.eventType,
                notes = state.notes,
                isActive = state.isActive,
                updatedAt = System.currentTimeMillis()
            )

            val savedId = if (state.isEditMode) {
                database.birthdayDao().update(birthday)
                birthday.id
            } else {
                database.birthdayDao().insert(birthday)
            }

            // 调度提醒：已暂停的记录不能因为保存一次就把闹钟重新排上
            val saved = database.birthdayDao().getById(savedId)
            if (saved != null) {
                if (saved.isActive) {
                    scheduler.scheduleBirthdayReminder(saved)
                } else {
                    scheduler.cancelBirthdayReminder(saved)
                }
            }

            _uiState.value = _uiState.value.copy(saved = true)
            WidgetRefresher.refresh(getApplication())
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.id <= 0) return
        viewModelScope.launch {
            database.birthdayDao().getById(state.id)?.let { scheduler.cancelBirthdayReminder(it) }
            database.birthdayDao().deleteById(state.id)
            _uiState.value = _uiState.value.copy(saved = true)
            WidgetRefresher.refresh(getApplication())
        }
    }

    companion object {
        // 输入长度上限：防止超长内容写入数据库和通知
        private const val MAX_NAME_LENGTH = 50
        private const val MAX_NOTES_LENGTH = 500
    }
}
