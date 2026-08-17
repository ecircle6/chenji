package com.birthapp.ui.preview

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.ui.detail.DetailUiState
import com.birthapp.ui.home.BirthdayDisplay
import com.birthapp.util.EventCalc
import java.time.LocalDate

/**
 * 各页面 @Preview 与 UI 测试共用的样例数据。
 * 只做静态样例，不依赖 ViewModel / 数据库，保证预览与测试环境都能直接使用。
 */
object PreviewData {

    fun birthday(
        id: Long = 0,
        name: String,
        year: Int = 1998,
        month: Int = 8,
        day: Int = 14,
        calendarType: String = "solar",
        eventType: String = EventType.BIRTHDAY,
        relation: String = "family",
        pinned: Boolean = false,
        advanceDays: List<Int> = listOf(0),
        notes: String = ""
    ) = Birthday(
        id = id,
        name = name,
        birthYear = year,
        birthMonth = month,
        birthDay = day,
        calendarType = calendarType,
        advanceDays = advanceDays,
        relation = relation,
        eventType = eventType,
        notes = notes,
        pinned = pinned
    )

    fun display(b: Birthday, countdown: Int = 100, isToday: Boolean = false, isPaused: Boolean = false): BirthdayDisplay {
        val today = LocalDate.now()
        val nextDate = EventCalc.nextSolarDate(b, today)
        val displayEmoji = b.emoji.ifBlank {
            if (b.eventType == EventType.BIRTHDAY) b.name.first().toString()
            else EventType.emoji(b.eventType)
        }
        return BirthdayDisplay(
            birthday = b,
            countdown = countdown,
            age = 27,
            zodiac = "虎",
            zodiacEmoji = "\uD83D\uDC05",
            dateLabel = if (b.calendarType == "lunar") "农历七月十五" else "8月14日",
            relationLabel = when (b.relation) {
                "friend" -> "朋友"
                "colleague" -> "同事"
                "other" -> "其他"
                else -> "家人"
            },
            relationEmoji = when (b.relation) {
                "friend" -> "👋"
                "colleague" -> "💼"
                "other" -> "⭐"
                else -> "🏠"
            },
            isToday = isToday,
            isPaused = isPaused,
            isPinned = b.pinned,
            eventType = b.eventType,
            typeEmoji = EventType.emoji(b.eventType),
            isSolemn = EventType.isSolemn(b.eventType),
            infoLine = "生日 · 农历七月十五",
            todayBanner = "",
            displayEmoji = displayEmoji,
            nextEventYear = nextDate.year,
            nextEventMonth = nextDate.month
        )
    }

    /** 首页列表样例：置顶生日 + 情侣纪念 + 缅怀各一条（id 必须不同，LazyColumn key 用） */
    fun birthdays(): List<BirthdayDisplay> = listOf(
        display(
            birthday(id = 1, name = "小明", pinned = true, notes = "喜欢打篮球"),
            countdown = 364
        ),
        display(
            birthday(id = 2, name = "在一起三周年", eventType = EventType.LOVE, relation = "other", year = 2023),
            countdown = 7
        ),
        display(
            birthday(id = 3, name = "爷爷", eventType = EventType.MEMORIAL, calendarType = "lunar", year = 1945, month = 7, day = 15),
            countdown = 12
        )
    )

    /** 详情页样例：加载完成的一条生日记录 */
    fun detailState(): DetailUiState = DetailUiState(
        loading = false,
        id = 1,
        name = "小明",
        eventType = EventType.BIRTHDAY,
        eventLabel = "生日提醒",
        typeEmoji = "🎂",
        relationLabel = "家人",
        relationEmoji = "🏠",
        primaryDate = "1998年8月14日 · 阳历",
        convertedDate = "农历六月廿三",
        nextDate = "2027年8月14日",
        ageLine = "属虎 · 29 岁",
        countdown = 364,
        isToday = false,
        nextReminderText = "2027年8月14日 08:00",
        advanceText = "当天",
        reminderTime = "08:00",
        notes = "喜欢打篮球，生日送篮球主题礼物",
        isActive = true,
        isPinned = true
    )
}

/** 首页 @Preview 用样例列表 */
fun previewBirthdays(): List<BirthdayDisplay> = PreviewData.birthdays()

/** 详情页 @Preview 用样例状态 */
fun previewDetailState(): DetailUiState = PreviewData.detailState()
