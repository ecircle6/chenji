package com.birthapp.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.birthapp.BirthApp
import com.birthapp.MainActivity
import com.birthapp.R
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.util.EventTextUtils
import com.birthapp.util.ZodiacUtils
import java.time.LocalDate

class NotificationHelper(private val context: Context) {

    fun showBirthdayNotification(
        birthdayId: Long,
        name: String,
        advanceDays: Int,
        calendarType: String,
        birthMonth: Int,
        birthDay: Int,
        birthYear: Int = 0,
        eventType: String = EventType.BIRTHDAY
    ) {
        val currentYear = LocalDate.now().year
        // 生日类就是年龄，纪念日类就是第几周年，算法一致
        val years = if (birthYear > 0) ZodiacUtils.getAge(birthYear, currentYear) else 0
    
        val dateInfo = if (calendarType == "lunar") {
            "农历${LunarCalendar.formatLunarDate(birthMonth, birthDay)}"
        } else {
            "${birthMonth}月${birthDay}日"
        }
    
        val title = if (advanceDays == 0) {
            EventTextUtils.notificationTitleToday(eventType, name, years)
        } else {
            EventTextUtils.notificationTitleAdvance(eventType, name, years)
        }
        val text = EventTextUtils.notificationText(eventType, dateInfo, advanceDays)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("birthday_id", birthdayId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            birthdayId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BirthApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // 锁屏只显示“有通知”，不暴露姓名和生日内容
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(birthdayId.toInt(), notification)
    }
}
