package com.birthapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "birthdays")
data class Birthday(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val calendarType: String,   // "lunar" | "solar"
    val isLeapMonth: Boolean = false,
    /**
     * 提前提醒天数列表（v3 起）。0=当天，升序去重，逗号分隔存 TEXT。
     * v2 及以前是单个 Int（3 = 提前 3 天），迁移时原样转成单元素列表，
     * 语义不变；0 还是 [0]（当天）
     */
    val advanceDays: List<Int> = listOf(0),
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val relation: String = "family",  // "family"|"friend"|"colleague"|"other"
    // v2 新增。defaultValue 必须与迁移脚本里的 DEFAULT 一致，否则升级后 Room 校验 schema 会崩
    @ColumnInfo(defaultValue = EventType.BIRTHDAY)
    val eventType: String = EventType.BIRTHDAY,  // 见 EventType
    val notes: String = "",
    val isActive: Boolean = true,
    // v3 新增：置顶记录固定在首页列表顶部（暂停的置顶记录仍置顶，只是灰显）
    val pinned: Boolean = false,
    // v4 新增：每条记录的专属 Emoji 头像（空=自动：生日→姓名首字，其他→类型 emoji）
    @ColumnInfo(defaultValue = "")
    val emoji: String = "",
    val nextReminderDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
