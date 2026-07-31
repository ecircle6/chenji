package com.birthapp

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import com.birthapp.alarm.AlarmScheduler
import com.birthapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BirthApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        rescheduleAllAlarms()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "生日提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "家人朋友的生日提醒通知"
            enableVibration(true)
            // 锁屏下隐藏通知内容，防止生日隐私信息被旁人看到
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun rescheduleAllAlarms() {
        applicationScope.launch {
            val birthdays = database.birthdayDao().getAllActive().first()
            val scheduler = AlarmScheduler(this@BirthApp, database)
            for (birthday in birthdays) {
                scheduler.scheduleBirthdayReminder(birthday)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "birthday_reminder"
    }
}
