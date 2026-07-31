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
    val advanceDays: Int = 0,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val relation: String = "family",  // "family"|"friend"|"colleague"|"other"
    // v2 新增。defaultValue 必须与迁移脚本里的 DEFAULT 一致，否则升级后 Room 校验 schema 会崩
    @ColumnInfo(defaultValue = EventType.BIRTHDAY)
    val eventType: String = EventType.BIRTHDAY,  // 见 EventType
    val notes: String = "",
    val isActive: Boolean = true,
    val nextReminderDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
