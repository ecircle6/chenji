package com.birthapp.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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
 * 分享卡片生成器：Canvas 直绘 PNG。
 *
 * 按事件类型分流两套规格：
 * - A · 毛玻璃信息卡（生日/纪念日等）：840×640 横版独立卡片组件——没有外部
 *   背景/极光/边距，卡片即画面；内部三段均匀撑满：品牌栏 → 标签/名字/日期 →
 *   倒计时（上下段正中心）→ 底部三栏（月/日/星期）
 * - B · 深夜烛火（缅怀）：1080×1920 竖版，纯黑底 + 顶部烛光 + 翻页数字 + 诗句
 */
object ShareCardGenerator {

    // A 卡（横版 840×640）
    private const val AW = 840
    private const val AH = 640

    // B 卡（竖版 1080×1920）
    private const val BW = 1080
    private const val BH = 1920

    // ---- A 风格配色 ----
    private const val COLOR_TITLE = 0xFFFFFFFF.toInt()
    private const val COLOR_SUB = 0xFF8899AA.toInt()
    private const val COLOR_TAG = 0xFF66DDAA.toInt()
    private const val COLOR_MONTH = 0xFFAA88FF.toInt()
    private const val COLOR_DAY = 0xFF66CCFF.toInt()
    private const val COLOR_WEEK = 0xFF66DDAA.toInt()
    private const val COLOR_TRI_KEY = 0xFF667788.toInt()

    // ---- B 风格配色（严格按 Prompt B）----
    private const val COLOR_BG_B = 0xFF0F0F12.toInt()
    private const val COLOR_GOLD_WARM = 0xFFD4A574.toInt()
    private const val COLOR_GOLD_DIM_B = 0xFFA08060.toInt()
    private const val COLOR_MEM_TEXT = 0xFF777777.toInt()
    private const val COLOR_POEM_B = 0xFF888888.toInt()
    private const val COLOR_MEM_DATE = 0xFF555555.toInt()
    private const val COLOR_MEM_BRAND = 0xFF444444.toInt()
    private const val COLOR_DIGIT_BG_TOP = 0xFF2A2A2A.toInt()
    private const val COLOR_DIGIT_BG_BOTTOM = 0xFF1A1A1A.toInt()

    /** 生成卡片位图并保存到 cacheDir/share/，返回文件（FileProvider 分享用） */
    fun generate(context: Context, birthday: Birthday): File {
        val (w, h) = if (EventType.isSolemn(birthday.eventType)) BW to BH else AW to AH
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), birthday)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "share_${birthday.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** 按事件类型分流：缅怀走深夜烛火（竖版），其余走毛玻璃信息卡（横版） */
    fun draw(canvas: Canvas, b: Birthday) {
        if (EventType.isSolemn(b.eventType)) {
            drawMemorial(canvas, b)
        } else {
            drawAurora(canvas, b)
        }
    }

    // ================================================================
    // A · 毛玻璃信息卡（840×640 独立组件，无外部背景/极光/边距）
    // ================================================================

    private fun drawAurora(canvas: Canvas, b: Birthday) {
        // 1. 卡片本体：圆角 24、边框 rgba(255,255,255,0.08)、毛玻璃底色。
        //    PNG 无法携带 backdrop-filter，纯 rgba(255,255,255,0.04) 在透明底上几乎
        //    不可见且白字会在浅色场景失读——用半透明深色（#0c1020 @55%）呈现
        //    「深夜毛玻璃」：白字清晰、嵌入浅/深背景都成立
        val cardRect = RectF(0f, 0f, AW.toFloat(), AH.toFloat())
        canvas.drawRoundRect(cardRect, 24f, 24f, Paint().apply { color = 0x8C0C1020.toInt() })
        canvas.drawRoundRect(
            cardRect, 24f, 24f,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x14FFFFFF.toInt() }
        )

        // 2. 上段：品牌栏（距顶 36，左内边距 48，左对齐）
        val padL = 48f
        val brandTop = 36f
        canvas.drawRoundRect(
            RectF(padL, brandTop, padL + 36f, brandTop + 36f),
            10f, 10f, Paint().apply { color = 0x14FFFFFF.toInt() } // rgba(255,255,255,0.08)
        )
        canvas.drawRoundRect(
            RectF(padL, brandTop, padL + 36f, brandTop + 36f),
            10f, 10f,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x1AFFFFFF.toInt() } // 0.1
        )
        drawCenteredText(canvas, "🌙", padL + 18f, brandTop + 18f,
            Paint().apply { textSize = 18f }, centerVertical = true)
        val chPaint = Paint().apply { color = COLOR_SUB; textSize = 12f; letterSpacing = 0.18f }
        canvas.drawText(
            "CHENJI", padL + 36f + 10f,
            brandTop + 18f - (chPaint.ascent() + chPaint.descent()) / 2, chPaint
        )

        // 3. 上中段（距品牌栏 20，左对齐，左内边距 48）：标签 → 名字 → 日期
        var cursor = brandTop + 36f + 20f
        drawTextAtLeftTop(canvas, tagText(b.eventType), padL, cursor,
            Paint().apply { color = COLOR_TAG; textSize = 13f })
        cursor += 13f + 10f
        drawTextAtLeftTop(canvas, b.name, padL, cursor,
            Paint().apply { color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD })
        cursor += 32f + 8f
        drawTextAtLeftTop(canvas, dateLine(b), padL, cursor,
            Paint().apply { color = COLOR_SUB; textSize = 13f })
        val dateBottom = cursor + 13f

        // 4. 下段：三栏（距卡片底 36，左右内边距 48，栏间距 16）
        val colGap = 16f
        val colW = (AW - padL * 2 - colGap * 2) / 3f  // 237.3
        val colH = 76f
        val triTop = AH - 36f - colH  // 528
        val nextDate = EventCalc.nextSolarDate(b).toLocalDate()
        val cols = listOf(
            Triple("${nextDate.monthValue}月", "月份", COLOR_MONTH),
            Triple("${nextDate.dayOfMonth}日", "日期", COLOR_DAY),
            Triple(weekdayName(nextDate.dayOfWeek), "星期", COLOR_WEEK)
        )
        cols.forEachIndexed { i, (value, key, colColor) ->
            val left = padL + i * (colW + colGap)
            val colRect = RectF(left, triTop, left + colW, triTop + colH)
            canvas.drawRoundRect(colRect, 12f, 12f, Paint().apply { color = 0x08FFFFFF.toInt() }) // 0.03
            canvas.drawRoundRect(
                colRect, 12f, 12f,
                Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x0FFFFFFF.toInt() } // 0.06
            )
            drawCenteredText(canvas, value, colRect.centerX(), colRect.centerY() - 8f,
                Paint().apply { this.color = colColor; textSize = 18f; typeface = Typeface.DEFAULT_BOLD })
            drawCenteredText(canvas, key, colRect.centerX(), colRect.centerY() + 26f,
                Paint().apply { color = COLOR_TRI_KEY; textSize = 11f })
        }

        // 5. 中段：倒计时，垂直居中于「日期底」与「三栏顶」之间的正中心（均匀撑满）
        val countdown = EventCalc.countdown(b)
        val blockH = if (countdown == 0) 32f else 56f
        val countTop = dateBottom + (triTop - dateBottom - blockH) / 2f
        if (countdown == 0) {
            drawTextAtLeftTop(canvas, "🎉 就是今天", padL, countTop,
                Paint().apply { color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD })
        } else {
            val numText = "$countdown"
            val numPaint = Paint().apply { color = COLOR_TITLE; textSize = 56f; typeface = Typeface.DEFAULT_BOLD }
            val unitPaint = Paint().apply { color = COLOR_SUB; textSize = 14f }
            val numBaseline = countTop - numPaint.fontMetrics.top
            canvas.drawText(numText, padL, numBaseline, numPaint)
            // 天后与数字底部基线对齐，间距 8
            canvas.drawText(
                "天后", padL + numPaint.measureText(numText) + 8f,
                numBaseline, unitPaint
            )
        }
    }

    /** 类型标签文案（对应设计稿 A 的标签适配表，不加粗） */
    private fun tagText(eventType: String): String = when (eventType) {
        EventType.MARRIAGE -> "💕 纪念日提醒"
        EventType.BABY -> "🍼 宝宝生日"
        EventType.LOVE -> "💑 情侣纪念"
        EventType.OTHER -> "📌 纪念日提醒"
        else -> "🎂 生日提醒"
    }

    /** A 卡日期行：`1998年8月14日 · 阳历` / `农历1950年六月十五 · 农历` */
    private fun dateLine(b: Birthday): String {
        return if (b.calendarType == "lunar") {
            "农历${b.birthYear}年${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)} · 农历"
        } else {
            "${b.birthYear}年${b.birthMonth}月${b.birthDay}日 · 阳历"
        }
    }

    private fun weekdayName(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"; DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"; DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

    // ================================================================
    // B · 深夜烛火（Prompt B 修正版，缅怀）
    // ================================================================

    private fun drawMemorial(canvas: Canvas, b: Birthday) {
        // 1. 纯黑底，无渐变
        canvas.drawColor(COLOR_BG_B)

        // 2. 顶部中央暖色光晕：椭圆 300×150，中心距顶 60，rgba(255,160,60,0.08)
        val glowRect = RectF((BW / 2f) - 150f, 60f - 75f, (BW / 2f) + 150f, 60f + 75f)
        canvas.drawOval(glowRect, Paint().apply {
            shader = RadialGradient(BW / 2f, 60f, 150f, 0x14FFA03C.toInt(), 0x00FFA03C.toInt(), Shader.TileMode.CLAMP)
        })

        // 3. 第一组：纪念符号（距顶约 280，居中）
        // 🕯️ 36px
        var cursor = 280f
        drawTextAtTop(canvas, "🕯️", cursor, Paint().apply { textSize = 36f })
        // In Memory：+16，11px，#a08060，字距 4px，全大写
        cursor += 36f + 16f
        drawTextAtTop(canvas, "IN MEMORY", cursor, Paint().apply {
            color = COLOR_GOLD_DIM_B; textSize = 11f; letterSpacing = 0.35f
        })
        // 下方留 60px
        cursor += 11f + 60f

        // 4. 第二组：纪念文案
        // 第一行：+20 后翻页数字
        drawTextAtTop(canvas, "${b.name}离开我们已经", cursor, Paint().apply {
            color = COLOR_MEM_TEXT; textSize = 14f
        })
        cursor += 14f + 20f

        // 翻页数字：方块 56×64 无间距，数字 #d4a574 32px 加粗居中；「天」与方块底部对齐
        val countdown = EventCalc.countdown(b)
        val digits = countdown.toString()
        val digitW = 56f
        val digitH = 64f
        val totalW = digits.length * digitW
        var dx = (BW / 2f) - totalW / 2f
        val digitsTop = cursor
        for (ch in digits) {
            val rect = RectF(dx, digitsTop, dx + digitW, digitsTop + digitH)
            canvas.drawRoundRect(rect, 10f, 10f, Paint().apply {
                shader = LinearGradient(
                    0f, rect.top, 0f, rect.bottom,
                    intArrayOf(COLOR_DIGIT_BG_TOP, COLOR_DIGIT_BG_BOTTOM),
                    null, Shader.TileMode.CLAMP
                )
            })
            canvas.drawRoundRect(
                rect, 10f, 10f,
                Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x14FFFFFF.toInt() } // 0.08
            )
            drawCenteredText(canvas, ch.toString(), rect.centerX(), rect.centerY(),
                Paint().apply { color = COLOR_GOLD_WARM; textSize = 32f; typeface = Typeface.DEFAULT_BOLD },
                centerVertical = true)
            dx += digitW
        }
        // 「天」18px #555555，方块右侧 6px，与方块底部对齐
        val unitPaint = Paint().apply { color = COLOR_MEM_DATE; textSize = 18f }
        canvas.drawText(
            "天", (BW / 2f) + totalW / 2f + 6f,
            digitsTop + digitH - unitPaint.fontMetrics.bottom,
            unitPaint
        )
        // 下方留 80px
        cursor = digitsTop + digitH + 80f

        // 5. 第三组：分隔线与诗句
        // 分隔线：宽 240，高 1，居中
        drawHorizontalLine(canvas, (BW / 2f) - 120f, (BW / 2f) + 120f, cursor, 0x0FFFFFFF.toInt(), 1f)
        // 诗句：+24，15px 斜体 #888888，行高 2.0
        cursor += 24f
        val poemPaint = Paint().apply {
            color = COLOR_POEM_B
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        drawTextAtTop(canvas, "有些人离开了", cursor, poemPaint)
        drawTextAtTop(canvas, "但永远活在记忆里", cursor + 30f, poemPaint) // 行高 2.0 = 30px
        // 日期：诗句底 +20，11px #555555
        cursor += 30f + 30f + 20f
        drawTextAtTop(canvas, memorialDate(b), cursor, Paint().apply { color = COLOR_MEM_DATE; textSize = 11f })

        // 6. 第四组：品牌（距底部 120），10px #444444 字距 3px
        drawTextAtBottom(canvas, "辰记", BH - 120f, Paint().apply {
            color = COLOR_MEM_BRAND; textSize = 10f; letterSpacing = 0.3f
        })
    }

    /** B 卡日期：农历/阳历 + 年份 */
    private fun memorialDate(b: Birthday): String {
        return if (b.calendarType == "lunar") {
            "农历${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)} · ${b.birthYear}年"
        } else {
            "${b.birthMonth}月${b.birthDay}日 · ${b.birthYear}年"
        }
    }

    // ================================================================
    // 通用绘制工具
    // ================================================================

    private fun drawHorizontalLine(canvas: Canvas, x1: Float, x2: Float, y: Float, color: Int, width: Float) {
        canvas.drawLine(x1, y, x2, y, Paint().apply {
            this.color = color
            strokeWidth = width
        })
    }

    /** 左对齐（x 为左边缘）、文字顶对齐 yTop 的文本 */
    private fun drawTextAtLeftTop(canvas: Canvas, text: String, xLeft: Float, yTop: Float, paint: Paint) {
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(text, xLeft, yTop - paint.fontMetrics.top, paint)
    }

    /** 水平居中、文字顶对齐 y 的文本 */
    private fun drawTextAtTop(canvas: Canvas, text: String, yTop: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, BW / 2f, yTop - paint.fontMetrics.top, paint)
    }

    /** 水平居中、文字底对齐 y 的文本（底部品牌用） */
    private fun drawTextAtBottom(canvas: Canvas, text: String, yBottom: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, BW / 2f, yBottom - paint.fontMetrics.bottom, paint)
    }

    /** 水平居中、垂直居中于 (cx, cy) 的文本 */
    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) cy - (paint.ascent() + paint.descent()) / 2 else cy
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, baseline, paint)
    }
}
