package com.birthapp.data

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
    val notes: String = "",
    val isActive: Boolean = true,
    val nextReminderDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
