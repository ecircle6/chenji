package com.birthapp.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import com.birthapp.lunar.LunarCalendar
import com.birthapp.util.EventCalc
import com.birthapp.util.EventTextUtils
import com.birthapp.util.ZodiacUtils
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

/**
 * 分享卡片生成器：Canvas 直绘 1080×1080 PNG。
 *
 * 选 Canvas 而不是 Compose 截图（依赖真机渲染不可单测）或第三方卡片库
 * （违反项目零第三方依赖约定）。文字布局走纯函数，可单测。
 *
 * 卡片内容：渐变背景（按事件类型配色，缅怀用素净灰蓝）→ 头像圈 →
 * 姓名 → 信息行（复用 EventTextUtils）→ 大号倒计时 → 辰记水印。
 */
object ShareCardGenerator {

    private const val SIZE = 1080
    private const val MARGIN = 72

    /** 生成卡片位图并保存到 cacheDir/share/，返回文件（FileProvider 分享用） */
    fun generate(context: Context, birthday: Birthday): File {
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), birthday)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "share_${birthday.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** 绘制过程本身：位图绘制是纯本地的，JVM（Robolectric）也能跑 */
    fun draw(canvas: Canvas, b: Birthday) {
        val solemn = EventType.isSolemn(b.eventType)
        val bgColors = if (solemn) {
            intArrayOf(0xFF6B7280.toInt(), 0xFF374151.toInt())
        } else {
            val accent = accentColor(b.eventType)
            intArrayOf(lighten(accent), accent)
        }
        canvas.drawRect(
            0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, SIZE.toFloat(), bgColors[0], bgColors[1], Shader.TileMode.CLAMP
                )
            }
        )

        // 头像圈：生日用姓名首字，其余类型用类型 emoji
        val avatarCenter = SIZE / 2f
        val avatarRadius = 150f
        canvas.drawCircle(
            avatarCenter, 260f, avatarRadius,
            Paint().apply {
                color = 0x33FFFFFF.toInt()
                isAntiAlias = true
            }
        )
        val avatarText = if (b.eventType == EventType.BIRTHDAY) {
            b.name.take(1)
        } else {
            EventType.emoji(b.eventType)
        }
        drawText(
            canvas, avatarText, avatarCenter, 260f,
            Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 130f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            },
            centerVertical = true
        )

        // 姓名
        drawText(
            canvas, b.name, avatarCenter, 520f,
            Paint().apply {
                color = 0xFFFFFFFF.toInt()
                textSize = 72f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
        )

        // 信息行：类型 · 日期 · 属相年龄（与首页同一套文案）
        val today = LocalDate.now()
        val age = ZodiacUtils.getAge(b.birthYear, today.year)
        val dateLabel = if (b.calendarType == "lunar") {
            "农历${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)}"
        } else {
            "${b.birthMonth}月${b.birthDay}日"
        }
        val info = EventTextUtils.infoLine(
            eventType = b.eventType,
            calendarType = b.calendarType,
            dateLabel = dateLabel,
            zodiacEmoji = ZodiacUtils.getZodiacEmoji(b.birthYear),
            zodiac = ZodiacUtils.getZodiacName(b.birthYear),
            age = age
        )
        drawText(
            canvas, info, avatarCenter, 620f,
            Paint().apply {
                color = 0xCCFFFFFF.toInt()
                textSize = 40f
                textAlign = Paint.Align.CENTER
            }
        )

        // 大号倒计时
        val countdown = EventCalc.countdown(b, today)
        if (countdown == 0) {
            val banner = EventTextUtils.cardBanner(b.eventType, b.name, age)
            drawText(
                canvas, banner, avatarCenter, 780f,
                Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 76f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
            )
        } else {
            drawText(
                canvas, "$countdown", avatarCenter, 770f,
                Paint().apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = 180f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
            )
            drawText(
                canvas, "天后", avatarCenter, 890f,
                Paint().apply {
                    color = 0xCCFFFFFF.toInt()
                    textSize = 44f
                    textAlign = Paint.Align.CENTER
                }
            )
        }

        // 水印
        drawText(
            canvas, "辰记 · 记录每一个重要的日子", SIZE / 2f, SIZE - 80f,
            Paint().apply {
                color = 0x66FFFFFF.toInt()
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
        )
    }

    /** 事件类型的主色（与 App 内 eventAccent 同源的一套暖色） */
    private fun accentColor(eventType: String): Int = when (eventType) {
        EventType.LOVE -> 0xFFE8625C.toInt()
        EventType.BABY -> 0xFF2FA8A0.toInt()
        EventType.MARRIAGE -> 0xFF7C6FD8.toInt()
        EventType.OTHER -> 0xFFE8A33D.toInt()
        else -> 0xFFEF7759.toInt()
    }

    private fun lighten(color: Int): Int {
        val r = (color shr 16 and 0xFF) + 60
        val g = (color shr 8 and 0xFF) + 60
        val b = (color and 0xFF) + 60
        return 0xFF000000.toInt() or (r.coerceAtMost(255) shl 16) or
            (g.coerceAtMost(255) shl 8) or b.coerceAtMost(255)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) y - (paint.ascent() + paint.descent()) / 2 else y
        canvas.drawText(text, x, baseline, paint)
    }
}
