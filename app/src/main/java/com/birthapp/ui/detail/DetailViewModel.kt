package com.birthapp.ui.detail

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
import com.birthapp.util.ZodiacUtils
import com.birthapp.widget.WidgetRefresher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class DetailUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val eventType: String = EventType.BIRTHDAY,
    val eventLabel: String = "",
    val typeEmoji: String = "",
    val isSolemn: Boolean = false,
    val relationLabel: String = "",
    val relationEmoji: String = "",
    /** 记录本身的日期，按录入时的历法显示 */
    val primaryDate: String = "",
    /** 换算成另一种历法的日期，农历记录尤其需要——不然不知道今年到底是哪天 */
    val convertedDate: String = "",
    /** 生日类型是“属相 · N 岁”，纪念日类型是“第 N 周年” */
    val ageLine: String = "",
    val countdown: Int = 0,
    val isToday: Boolean = false,
    val nextReminderText: String = "",
    val advanceText: String = "",
    val reminderTime: String = "",
    val notes: String = "",
    val isActive: Boolean = true
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _id = MutableStateFlow(0L)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DetailUiState> = _id
        .flatMapLatest { id ->
            // id 还没从导航参数传进来时停在 loading，
            // 不能当成 notFound，否则页面一打开就会自己退回去
            if (id <= 0) {
                flowOf(DetailUiState())
            } else {
                database.birthdayDao().observeById(id).map { b ->
                    b?.toDetailState() ?: DetailUiState(loading = false, notFound = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    fun load(id: Long) {
        _id.value = id
    }

    /**
     * 切换提醒开关。关闭时必须真的把闹钟撤掉，只改数据库字段的话闹钟照响。
     */
    fun toggleActive(active: Boolean) {
        val id = _id.value
        if (id <= 0) return
        viewModelScope.launch {
            database.birthdayDao().setActive(id, active, System.currentTimeMillis())
            val b = database.birthdayDao().getById(id) ?: return@launch
            if (active) {
                scheduler.scheduleBirthdayReminder(b)
            } else {
                scheduler.cancelBirthdayReminder(id)
                // 顺手清掉缓存的下次提醒日期，避免详情页显示一个不会到来的日子
                database.birthdayDao().updateNextReminderDate(id, null)
            }
            // 暂停的记录不该再占着桌面小组件的位置
            WidgetRefresher.refresh(getApplication())
        }
    }

    fun delete() {
        val id = _id.value
        if (id <= 0) return
        viewModelScope.launch {
            scheduler.cancelBirthdayReminder(id)
            database.birthdayDao().deleteById(id)
            _deleted.value = true
            WidgetRefresher.refresh(getApplication())
        }
    }

    private fun Birthday.toDetailState(): DetailUiState {
        val today = LocalDate.now()
        val currentYear = today.year

        // 下一次事件发生的阳历日期：今年的已经过了就取明年
        val nextSolar = EventCalc.nextSolarDate(this, today)
        val countdown = DateUtils.daysUntilDate(nextSolar.toLocalDate())

        val primaryDate = if (calendarType == "lunar") {
            val leapPrefix = if (isLeapMonth) "闰" else ""
            "农历 ${birthYear}年$leapPrefix${LunarCalendar.formatLunarDate(birthMonth, birthDay)}"
        } else {
            "阳历 ${DateUtils.formatSolarDate(birthYear, birthMonth, birthDay)}"
        }

        // 农历记录换算成今年的阳历；阳历记录反过来换算成农历
        val convertedDate = if (calendarType == "lunar") {
            "对应阳历 ${DateUtils.formatSolarDate(nextSolar.year, nextSolar.month, nextSolar.day)}"
        } else {
            runCatching {
                val lunar = LunarCalendar.solarToLunar(nextSolar.year, nextSolar.month, nextSolar.day)
                val leapPrefix = if (lunar.isLeapMonth) "闰" else ""
                "对应农历 $leapPrefix${LunarCalendar.formatLunarDate(lunar.month, lunar.day)}"
            }.getOrDefault("")
        }

        val age = ZodiacUtils.getAge(birthYear, currentYear)
        val ageLine = if (EventType.usesAge(eventType)) {
            "${ZodiacUtils.getZodiacEmoji(birthYear)} 属${ZodiacUtils.getZodiacName(birthYear)} · ${age}岁"
        } else {
            "第 $age 周年"
        }

        // 用闹钟自己算的时刻，保证页面显示的和真正会响的是同一个时间
        val nextReminderText = if (!isActive) {
            "提醒已暂停"
        } else {
            val triggerMillis = scheduler.calculateNextTriggerTime(this)
            if (triggerMillis == null) {
                "暂无提醒计划"
            } else {
                val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(triggerMillis), ZoneId.systemDefault())
                val days = DateUtils.daysUntilDate(dt.toLocalDate())
                val whenText = when {
                    days == 0 -> "今天"
                    days == 1 -> "明天"
                    else -> "还有 $days 天"
                }
                "${DateUtils.formatSolarDate(dt.year, dt.monthValue, dt.dayOfMonth)} " +
                        "${DateUtils.formatReminderTime(dt.hour, dt.minute)}（$whenText）"
            }
        }

        return DetailUiState(
            loading = false,
            notFound = false,
            id = id,
            name = name,
            eventType = eventType,
            eventLabel = EventType.label(eventType),
            typeEmoji = EventType.emoji(eventType),
            isSolemn = EventType.isSolemn(eventType),
            relationLabel = ZodiacUtils.getRelationLabel(relation),
            relationEmoji = ZodiacUtils.getRelationEmoji(relation),
            primaryDate = primaryDate,
            convertedDate = convertedDate,
            ageLine = ageLine,
            countdown = countdown,
            isToday = countdown == 0,
            nextReminderText = nextReminderText,
            advanceText = if (advanceDays == 0) "当天提醒" else "提前 $advanceDays 天提醒",
            reminderTime = DateUtils.formatReminderTime(reminderHour, reminderMinute),
            notes = notes,
            isActive = isActive
        )
    }
}
