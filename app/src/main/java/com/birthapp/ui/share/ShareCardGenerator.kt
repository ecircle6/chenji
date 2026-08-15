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
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek

/**
 * 分享卡片生成器：Canvas 直绘 840×640 PNG（两种风格统一尺寸）。
 *
 * A · 毛玻璃信息卡（生日/纪念日等）：深色毛玻璃底 + 品牌 → 标签/名字/日期 →
 *   倒计时（卡片垂直中心）→ 底部三栏，全部左对齐（三栏内居中）。
 *   圆角外区域透明，无背景/极光/边距，卡片即画布。
 *
 * B · 深夜烛火（缅怀）：纯黑底 → 🕯️ In Memory → 纪念文案+翻页数字 →
 *   分隔线+斜体诗句 → 日期 → 品牌。全部居中紧凑排列，安静克制。
 */
object ShareCardGenerator {

    private const val W = 840
    private const val H = 640

    // A 风格配色
    private const val COLOR_TITLE = 0xFFFFFFFF.toInt()
    private const val COLOR_SUB = 0xFF8899AA.toInt()
    private const val COLOR_TAG = 0xFF66DDAA.toInt()
    private const val COLOR_MONTH = 0xFFAA88FF.toInt()
    private const val COLOR_DAY = 0xFF66CCFF.toInt()
    private const val COLOR_WEEK = 0xFF66DDAA.toInt()
    private const val COLOR_TRI_KEY = 0xFF667788.toInt()

    // B 风格配色
    private const val COLOR_GOLD_WARM = 0xFFD4A574.toInt()
    private const val COLOR_GOLD_DIM_B = 0xFFA08060.toInt()
    private const val COLOR_MEM_TEXT = 0xFF777777.toInt()
    private const val COLOR_POEM_B = 0xFF888888.toInt()
    private const val COLOR_MEM_DATE = 0xFF555555.toInt()
    private const val COLOR_MEM_BRAND = 0xFF444444.toInt()
    private const val COLOR_DIGIT_BG_TOP = 0xFF2A2A2A.toInt()
    private const val COLOR_DIGIT_BG_BOTTOM = 0xFF1A1A1A.toInt()

    /** 生成卡片位图（840×640，圆角外透明），保存到 cacheDir/share/ */
    fun generate(context: Context, birthday: Birthday): File {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), birthday)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "share_${birthday.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    fun draw(canvas: Canvas, b: Birthday) {
        if (EventType.isSolemn(b.eventType)) drawMemorial(canvas, b)
        else drawAurora(canvas, b)
    }

    // ================================================================
    // A · 毛玻璃信息卡（840×640，无外部背景/极光/边距，圆角外透明）
    // ================================================================

    private fun drawAurora(canvas: Canvas, b: Birthday) {
        val cardRect = RectF(0f, 0f, W.toFloat(), H.toFloat())

        // 1. 卡片底色：rgba(13,16,32,0.85) + 极淡白遮罩 rgba(255,255,255,0.03)
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply { color = 0xD90D1020.toInt() })
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply { color = 0x08FFFFFF.toInt() })
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x14FFFFFF.toInt()
        })

        // 2. 第一段：品牌栏（距顶 36，左对齐，左内边距 48）
        val padL = 48f
        val brandTop = 36f
        canvas.drawRoundRect(RectF(padL, brandTop, padL + 36f, brandTop + 36f),
            10f, 10f, Paint().apply { color = 0x14FFFFFF.toInt() })
        canvas.drawRoundRect(RectF(padL, brandTop, padL + 36f, brandTop + 36f),
            10f, 10f, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x1AFFFFFF.toInt() })
        drawAtLeftTop(canvas, "🌙", padL + 18f, brandTop + 18f, Paint().apply { textSize = 16f }, centerY = true)
        val chPaint = Paint().apply { color = COLOR_SUB; textSize = 12f; letterSpacing = 0.18f }
        canvas.drawText("CHENJI", padL + 36f + 10f,
            brandTop + 18f - (chPaint.ascent() + chPaint.descent()) / 2, chPaint)

        // 3. 第二段：信息区（距品牌底 20，左对齐）
        var cursor = brandTop + 36f + 20f
        drawAtLeftTop(canvas, tagText(b.eventType), padL, cursor, Paint().apply { color = COLOR_TAG; textSize = 13f })
        cursor += 13f + 10f
        drawAtLeftTop(canvas, b.name, padL, cursor, Paint().apply {
            color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
        })
        cursor += 32f + 8f
        drawAtLeftTop(canvas, dateLine(b), padL, cursor, Paint().apply { color = COLOR_SUB; textSize = 13f })

        // 4. 第三段：倒计时（位于卡片垂直中心点，左对齐）
        val countdown = EventCalc.countdown(b)
        val countCenterY = H / 2f  // 320
        if (countdown == 0) {
            drawAtLeftTop(canvas, "🎉 就是今天", padL, countCenterY - 16f, Paint().apply {
                color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
            })
        } else {
            val numPaint = Paint().apply { color = COLOR_TITLE; textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
            val unitPaint = Paint().apply { color = COLOR_SUB; textSize = 14f }
            val numBaseline = countCenterY + numPaint.fontMetrics.bottom - 14f
            canvas.drawText("$countdown", padL, numBaseline, numPaint)
            canvas.drawText("天后", padL + numPaint.measureText("$countdown") + 8f, numBaseline, unitPaint)
        }

        // 5. 第四段：三栏（距底 36，整体占 744 宽左起 48，栏间距 16）
        val colGap = 16f
        val colW = (W - padL * 2 - colGap * 2) / 3f
        val colH = 76f
        val triTop = H - 36f - colH
        val triStartX = padL
        val nextDate = EventCalc.nextSolarDate(b).toLocalDate()
        val cols = listOf(
            Triple("${nextDate.monthValue}月", "月份", COLOR_MONTH),
            Triple("${nextDate.dayOfMonth}日", "日期", COLOR_DAY),
            Triple(weekdayName(nextDate.dayOfWeek), "星期", COLOR_WEEK)
        )
        cols.forEachIndexed { i, (value, key, colColor) ->
            val left = triStartX + i * (colW + colGap)
            val colRect = RectF(left, triTop, left + colW, triTop + colH)
            canvas.drawRoundRect(colRect, 12f, 12f, Paint().apply { color = 0x08FFFFFF.toInt() })
            canvas.drawRoundRect(colRect, 12f, 12f, Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x0FFFFFFF.toInt()
            })
            drawCenteredText(canvas, value, colRect.centerX(), colRect.centerY() - 8f,
                Paint().apply { this.color = colColor; textSize = 18f; typeface = Typeface.DEFAULT_BOLD })
            drawCenteredText(canvas, key, colRect.centerX(), colRect.centerY() + 26f,
                Paint().apply { color = COLOR_TRI_KEY; textSize = 11f })
        }
    }

    // ================================================================
    // B · 深夜烛火（840×640，无烛光晕染，全部居中紧凑，安静克制）
    // ================================================================

    private fun drawMemorial(canvas: Canvas, b: Birthday) {
        // 1. 卡片：纯黑 #0f0f12，只画圆角矩形（角外由 Bitmap alpha=0 保持透明）
        val cardRect = RectF(0f, 0f, W.toFloat(), H.toFloat())
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply { color = 0xFF0F0F12.toInt() })
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x0DFFFFFF.toInt() // 0.05
        })

        val cx = W / 2f  // 420

        // 2. 第一段：纪念标识（距顶 40，居中）
        // 🕯️ 28px
        drawCenteredText(canvas, "🕯️", cx, 40f, Paint().apply { textSize = 28f })
        // In Memory：+10，11px #a08060 字距4
        drawCenteredText(canvas, "IN MEMORY", cx, 40f + 28f + 10f,
            Paint().apply { color = COLOR_GOLD_DIM_B; textSize = 11f; letterSpacing = 0.35f })

        // 3. 第二段：纪念信息（距标识底 28px，居中）
        // 文案 13px #777
        var cursor = 40f + 28f + 10f + 11f + 28f
        drawCenteredText(canvas, "${b.name}离开我们已经", cx, cursor,
            Paint().apply { color = COLOR_MEM_TEXT; textSize = 13f })

        // 翻页数字：+16，方块 52×60 无间距，数字 #d4a574 28px
        cursor += 13f + 16f
        val countdown = EventCalc.countdown(b)
        val digits = countdown.toString()
        val digitW = 52f; val digitH = 60f
        val totalW = digits.length * digitW
        var dx = cx - totalW / 2f
        for (ch in digits) {
            val rect = RectF(dx, cursor, dx + digitW, cursor + digitH)
            canvas.drawRoundRect(rect, 10f, 10f, Paint().apply {
                shader = LinearGradient(0f, rect.top, 0f, rect.bottom,
                    intArrayOf(COLOR_DIGIT_BG_TOP, COLOR_DIGIT_BG_BOTTOM), null, Shader.TileMode.CLAMP)
            })
            canvas.drawRoundRect(rect, 10f, 10f, Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x14FFFFFF.toInt()
            })
            drawCenteredText(canvas, ch.toString(), rect.centerX(), rect.centerY(),
                Paint().apply { color = COLOR_GOLD_WARM; textSize = 28f; typeface = Typeface.DEFAULT_BOLD },
                centerVertical = true)
            dx += digitW
        }
        // 「天」与方块底部对齐，右侧 6px
        val unitPaint = Paint().apply { color = COLOR_MEM_DATE; textSize = 16f }
        canvas.drawText("天", cx + totalW / 2f + 6f, cursor + digitH - unitPaint.fontMetrics.bottom, unitPaint)

        // 4. 第三段：诗句与日期（距数字底 32px，居中）
        cursor += digitH + 32f
        // 线 200px 居中
        drawHorizontalLine(canvas, cx - 100f, cx + 100f, cursor, 0x0FFFFFFF.toInt(), 1f)
        // 诗句 +16，14px #888 斜体行高 1.8（行距 25.2）
        cursor += 16f
        val poemPaint = Paint().apply {
            color = COLOR_POEM_B; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        drawCenteredText(canvas, "有些人离开了", cx, cursor, poemPaint)
        drawCenteredText(canvas, "但永远活在记忆里", cx, cursor + 25.2f, poemPaint)
        // 日期 +12
        cursor += 25.2f * 2 + 12f
        drawCenteredText(canvas, memorialDate(b), cx, cursor,
            Paint().apply { color = COLOR_MEM_DATE; textSize = 11f })

        // 5. 第四段：品牌（距底 32px，居中）
        drawCenteredText(canvas, "辰记", cx, H - 32f,
            Paint().apply { color = COLOR_MEM_BRAND; textSize = 10f; letterSpacing = 0.3f })
    }

    private fun memorialDate(b: Birthday): String = if (b.calendarType == "lunar") {
        "农历${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)} · ${b.birthYear}年"
    } else {
        "${b.birthMonth}月${b.birthDay}日 · ${b.birthYear}年"
    }

    // ================================================================
    // 工具
    // ================================================================

    /** 左对齐文本，文字顶对齐 yTop；centerY=true 时垂直居中于 yTop */
    private fun drawAtLeftTop(canvas: Canvas, text: String, xLeft: Float, y: Float,
                               paint: Paint, centerY: Boolean = false) {
        val baseline = if (centerY) y - (paint.ascent() + paint.descent()) / 2 else y - paint.fontMetrics.top
        canvas.drawText(text, xLeft, baseline, paint)
    }

    /** 水平居中文本 */
    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, y: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) y - (paint.ascent() + paint.descent()) / 2 else y - paint.fontMetrics.top
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, baseline, paint)
    }

    private fun drawHorizontalLine(canvas: Canvas, x1: Float, x2: Float, y: Float, color: Int, width: Float) {
        canvas.drawLine(x1, y, x2, y, Paint().apply { this.color = color; strokeWidth = width })
    }

    private fun tagText(eventType: String): String = when (eventType) {
        EventType.MARRIAGE -> "💕 纪念日提醒"
        EventType.BABY -> "🍼 宝宝生日"
        EventType.LOVE -> "💑 情侣纪念"
        EventType.OTHER -> "📌 纪念日提醒"
        else -> "🎂 生日提醒"
    }

    private fun dateLine(b: Birthday): String = if (b.calendarType == "lunar") {
        "农历${b.birthYear}年${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)} · 农历"
    } else "${b.birthYear}年${b.birthMonth}月${b.birthDay}日 · 阳历"

    private fun weekdayName(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"; DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"; DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
}
