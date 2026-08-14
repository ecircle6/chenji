package com.birthapp.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthapp.BirthApp
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.alarm.calculateNextTriggerTime
import com.birthapp.alarm.normalizeAdvanceLevels
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
import kotlinx.coroutines.withContext
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
    /** 换算成另一种历法的日期，和“记录”一样指出生（发生）那一天 */
    val convertedDate: String = "",
    /** 下一次事件发生的阳历日期。农历记录尤其需要——不然不知道今年到底是哪天 */
    val nextDate: String = "",
    /** 生日类型是“属相 · N 岁”，纪念日类型是“第 N 周年” */
    val ageLine: String = "",
    val countdown: Int = 0,
    val isToday: Boolean = false,
    val nextReminderText: String = "",
    val advanceText: String = "",
    val reminderTime: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val isPinned: Boolean = false
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BirthApp).database
    private val scheduler = AlarmScheduler(application, database)

    private val _id = MutableStateFlow(0L)

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** 分享卡片的一次性事件：文件 uri 交给界面拉起分享面板 */
    private val _shareEvent = MutableSharedFlow<android.net.Uri>()
    val shareEvent: SharedFlow<android.net.Uri> = _shareEvent.asSharedFlow()

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
                scheduler.cancelBirthdayReminder(b)
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
            database.birthdayDao().getById(id)?.let { scheduler.cancelBirthdayReminder(it) }
            database.birthdayDao().deleteById(id)
            _deleted.value = true
            WidgetRefresher.refresh(getApplication())
        }
    }

    /** 切换置顶：固定在首页列表顶部，跨重启保持（存在数据库里） */
    fun togglePinned() {
        val id = _id.value
        if (id <= 0) return
        viewModelScope.launch {
            val b = database.birthdayDao().getById(id) ?: return@launch
            database.birthdayDao().update(
                b.copy(pinned = !b.pinned, updatedAt = System.currentTimeMillis())
            )
        }
    }

    /** 生成分享卡片（Canvas 直绘 PNG）并交给界面拉起分享面板 */
    fun shareCard() {
        val id = _id.value
        if (id <= 0) return
        viewModelScope.launch {
            val result = runCatching {
                val b = database.birthdayDao().getById(id)
                    ?: throw IllegalArgumentException("记录不存在")
                val file = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.birthapp.ui.share.ShareCardGenerator.generate(getApplication(), b)
                }
                androidx.core.content.FileProvider.getUriForFile(
                    getApplication(), "com.birthapp.fileprovider", file
                )
            }
            result
                .onSuccess { _shareEvent.emit(it) }
                .onFailure { _shareEvent.emit(android.net.Uri.EMPTY) }
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

        // 换算行：两个方向都必须拿出生当天去换算，不能拿下次生日那天——
        // 紧挨着“记录 xxxx年x月x日”显示时，读者默认看到的就是出生那天
        // 换出来的日子。之前农历这边错拿了下次生日的阳历，“记录 农历
        // 1997年冬月廿四”旁边显示成“对应阳历 2027年1月1日”，像算错了一样。
        // 下次日期用户同样关心，但单独放一行“下次”去显示，不和出生日期混在一起
        val convertedDate = if (calendarType == "lunar") {
            runCatching {
                val solar = LunarCalendar.lunarToSolar(birthYear, birthMonth, birthDay, isLeapMonth)
                "对应阳历 ${DateUtils.formatSolarDate(solar.year, solar.month, solar.day)}"
            }.getOrDefault("")
        } else {
            runCatching {
                val lunar = LunarCalendar.solarToLunar(birthYear, birthMonth, birthDay)
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

        // 用闹钟自己算的时刻，保证页面显示的和真正会响的是同一个时间；
        // 多级提醒时取所有级别里最早触发的那个
        val nextReminderText = if (!isActive) {
            "提醒已暂停"
        } else {
            val triggerMillis = advanceDays
                .mapNotNull { calculateNextTriggerTime(this, it) }
                .minOrNull()
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
            nextDate = "阳历 ${DateUtils.formatSolarDate(nextSolar.year, nextSolar.month, nextSolar.day)}",
            ageLine = ageLine,
            countdown = countdown,
            isToday = countdown == 0,
            nextReminderText = nextReminderText,
            advanceText = advanceText(advanceDays),
            reminderTime = DateUtils.formatReminderTime(reminderHour, reminderMinute),
            notes = notes,
            isActive = isActive,
            isPinned = pinned
        )
    }

    /** 多级提前提醒文案：单级显示"提前 N 天提醒 / 当天提醒"，多级用 · 连接 */
    private fun advanceText(levels: List<Int>): String {
        val normalized = normalizeAdvanceLevels(levels)
        return if (normalized.size == 1 && normalized.first() == 0) {
            "当天提醒"
        } else {
            normalized.joinToString(" · ") { if (it == 0) "当天" else "提前${it}天" } + "提醒"
        }
    }
}
