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
 * 分享卡片生成器：Canvas 直绘 1080×1920 竖版 PNG。
 *
 * 按设计稿（Prompt A/B 修正版）1:1 像素实现：
 * - A · 极光毛玻璃（生日/纪念日等）：纯深蓝底 + 极微弱深紫渐变 + 右上青绿/左下紫
 *   雾状光斑（径向渐变多层模拟高斯模糊）→ 720×900 毛玻璃卡片垂直偏上居中 →
 *   内部全部水平居中：品牌 → 标签 → 标题 → 日期 → 倒计时 → 三栏
 * - B · 深夜烛火（缅怀）：纯黑底 + 顶部暖色光晕 → 居中大留白：🕯️ → In Memory →
 *   纪念文案 → 翻页数字方块 → 分隔线 → 斜体诗句 → 日期 → 底部品牌
 */
object ShareCardGenerator {

    private const val W = 1080
    private const val H = 1920
    private const val CX = W / 2f  // 540

    // ---- A 风格配色（严格按 Prompt A）----
    private const val COLOR_BG_A = 0xFF0C1020.toInt()
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
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), birthday)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "share_${birthday.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** 按事件类型分流：缅怀走深夜烛火，其余走极光毛玻璃 */
    fun draw(canvas: Canvas, b: Birthday) {
        if (EventType.isSolemn(b.eventType)) {
            drawMemorial(canvas, b)
        } else {
            drawAurora(canvas, b)
        }
    }

    // ================================================================
    // A · 极光毛玻璃（Prompt A 修正版）
    // ================================================================

    private fun drawAurora(canvas: Canvas, b: Birthday) {
        // 1. 背景：纯 #0c1020
        canvas.drawColor(COLOR_BG_A)
        // 2. 左上→右下极微弱深紫渐变（#1a0f2e，不透明度 ≤20%）
        canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
            shader = LinearGradient(
                0f, 0f, W.toFloat(), H.toFloat(),
                intArrayOf(0x331A0F2E.toInt(), 0x001A0F2E.toInt()),
                null, Shader.TileMode.CLAMP
            )
        })
        // 3. 背景极光光斑：中心在画布外，只露出角部切入，高斯模糊雾状
        //    右上 rgba(0,255,200,0.15) 直径 400 中心 (900,-100)
        drawFog(canvas, 900f, -100f, 200f, 0xFF00FFC8.toInt(), 0x26)
        //    左下 rgba(150,100,255,0.12) 直径 350 中心 (200,1800)
        drawFog(canvas, 200f, 1800f, 175f, 0xFF9664FF.toInt(), 0x1F)

        // 4. 卡片容器：720×960，水平垂直均居中（四周留大量深色边距）
        val cardW = 720f
        val cardH = 960f
        val cardLeft = CX - cardW / 2f
        val cardTop = (H - cardH) / 2f  // 480
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardW, cardTop + cardH)
        // 4a. 柔和的深色投影（卡片下方偏移）
        canvas.drawRoundRect(
            RectF(cardLeft + 18f, cardTop + 26f, cardLeft + cardW + 18f, cardTop + cardH + 26f),
            32f, 32f, Paint().apply { color = 0x20000000.toInt() }
        )
        // 4b. 毛玻璃：底层极光透过（卡片内虚化光斑）+ 极淡白底
        val cardPath = Path().apply { addRoundRect(cardRect, 32f, 32f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(cardPath)
        drawFog(canvas, cardLeft + 100f, cardTop + 140f, 220f, 0xFF00FFC8.toInt(), 0x14)
        drawFog(canvas, cardLeft + cardW - 80f, cardTop + cardH - 60f, 190f, 0xFF9664FF.toInt(), 0x10)
        canvas.restore()
        canvas.drawRoundRect(cardRect, 32f, 32f, Paint().apply { color = 0x0AFFFFFF.toInt() }) // rgba(255,255,255,0.04)
        canvas.drawRoundRect(
            cardRect, 32f, 32f,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x14FFFFFF.toInt() } // 边框 0.08
        )

        // 5. 卡片内部：品牌左对齐，其余元素居中；间距 40/36/16/12/48/48；三栏贴卡片底 40
        val innerLeft = cardLeft + 40f

        // 5a. 品牌栏：月亮方块 + CHENJI，卡片内顶部距顶 40，左对齐
        val brandTop = cardTop + 40f
        val chPaint = Paint().apply { color = COLOR_SUB; textSize = 12f; letterSpacing = 0.18f }
        canvas.drawRoundRect(
            RectF(innerLeft, brandTop, innerLeft + 44f, brandTop + 44f),
            12f, 12f, Paint().apply { color = 0x14FFFFFF.toInt() } // rgba(255,255,255,0.08)
        )
        canvas.drawRoundRect(
            RectF(innerLeft, brandTop, innerLeft + 44f, brandTop + 44f),
            12f, 12f,
            Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x1AFFFFFF.toInt() } // 0.1
        )
        drawCenteredText(canvas, "🌙", innerLeft + 22f, brandTop + 22f,
            Paint().apply { textSize = 20f }, centerVertical = true)
        canvas.drawText(
            "CHENJI", innerLeft + 44f + 10f,
            brandTop + 22f - (chPaint.ascent() + chPaint.descent()) / 2, chPaint
        )

        // 5b~5e. 标签 → 名字 → 日期（顶部紧凑，固定间距）
        var cursor = brandTop + 44f + 36f
        drawTextAtTop(canvas, tagText(b.eventType), cursor, Paint().apply { color = COLOR_TAG; textSize = 13f })
        cursor += 13f + 16f
        drawTextAtTop(canvas, b.name, cursor, Paint().apply {
            color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
        })
        cursor += 32f + 12f
        drawTextAtTop(canvas, dateLine(b), cursor, Paint().apply { color = COLOR_SUB; textSize = 13f })
        val dateBottom = cursor + 13f

        // 5f. 三栏：卡片内底部，距下边缘 40，作为最后一个元素
        val colW = 200f
        val colGap = 20f
        val colH = 96f
        val triBottom = cardRect.bottom - 40f
        val triTop = triBottom - colH
        val triTotalW = colW * 3 + colGap * 2
        val triStartX = CX - triTotalW / 2f

        // 5g. 倒计时：垂直居中于「日期底+48」到「三栏顶-48」之间（吸收中间空白，无大片空隙）
        val countdown = EventCalc.countdown(b)
        val countBlockH = if (countdown == 0) 32f else 72f
        val countTop = dateBottom + 48f +
            ((triTop - 48f) - (dateBottom + 48f) - countBlockH) / 2f
        if (countdown == 0) {
            drawTextAtTop(canvas, "🎉 就是今天", countTop, Paint().apply {
                color = COLOR_TITLE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
            })
        } else {
            val numText = "$countdown"
            val numPaint = Paint().apply { color = COLOR_TITLE; textSize = 72f; typeface = Typeface.DEFAULT_BOLD }
            val unitPaint = Paint().apply { color = COLOR_SUB; textSize = 16f }
            val numW = numPaint.measureText(numText)
            val unitW = unitPaint.measureText("天后")
            val blockW = numW + 8f + unitW
            val numBaseline = countTop - numPaint.fontMetrics.top
            canvas.drawText(numText, CX - blockW / 2f, numBaseline, numPaint)
            // 与数字底部基线对齐
            canvas.drawText("天后", CX - blockW / 2f + numW + 8f, numBaseline, unitPaint)
        }

        // 5h. 三栏内容（值 18px 加粗紫/蓝/绿 + 键 11px #667788）
        val nextDate = EventCalc.nextSolarDate(b).toLocalDate()
        val cols = listOf(
            Triple("${nextDate.monthValue}月", "月份", COLOR_MONTH),
            Triple("${nextDate.dayOfMonth}日", "日期", COLOR_DAY),
            Triple(weekdayName(nextDate.dayOfWeek), "星期", COLOR_WEEK)
        )
        cols.forEachIndexed { i, (value, key, colColor) ->
            val left = triStartX + i * (colW + colGap)
            val colRect = RectF(left, triTop, left + colW, triBottom)
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
    }

    /** 雾状光斑：多层径向渐变模拟高斯模糊，边缘柔和如雾气 */
    private fun drawFog(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, maxAlpha: Int) {
        val base = color and 0x00FFFFFF
        // 外层大而淡、内层小而浓，叠加出柔和衰减
        val layers = listOf(
            radius to maxAlpha,
            radius * 0.72f to (maxAlpha * 0.7).toInt(),
            radius * 0.45f to (maxAlpha * 0.5).toInt()
        )
        for ((r, a) in layers) {
            canvas.drawCircle(cx, cy, r, Paint().apply {
                shader = RadialGradient(cx, cy, r, (a shl 24) or base, 0x00FFFFFF, Shader.TileMode.CLAMP)
            })
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
        val glowRect = RectF(CX - 150f, 60f - 75f, CX + 150f, 60f + 75f)
        canvas.drawOval(glowRect, Paint().apply {
            shader = RadialGradient(CX, 60f, 150f, 0x14FFA03C.toInt(), 0x00FFA03C.toInt(), Shader.TileMode.CLAMP)
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
        var dx = CX - totalW / 2f
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
            "天", CX + totalW / 2f + 6f,
            digitsTop + digitH - unitPaint.fontMetrics.bottom,
            unitPaint
        )
        // 下方留 80px
        cursor = digitsTop + digitH + 80f

        // 5. 第三组：分隔线与诗句
        // 分隔线：宽 240，高 1，居中
        drawHorizontalLine(canvas, CX - 120f, CX + 120f, cursor, 0x0FFFFFFF.toInt(), 1f)
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
        drawTextAtBottom(canvas, "辰记", H - 120f, Paint().apply {
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

    /** 水平居中、文字顶对齐 y 的文本 */
    private fun drawTextAtTop(canvas: Canvas, text: String, yTop: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, CX, yTop - paint.fontMetrics.top, paint)
    }

    /** 水平居中、文字底对齐 y 的文本（底部品牌用） */
    private fun drawTextAtBottom(canvas: Canvas, text: String, yBottom: Float, paint: Paint) {
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, CX, yBottom - paint.fontMetrics.bottom, paint)
    }

    /** 水平居中、垂直居中于 (cx, cy) 的文本 */
    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) cy - (paint.ascent() + paint.descent()) / 2 else cy
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, baseline, paint)
    }
}
