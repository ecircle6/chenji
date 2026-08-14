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
import java.time.LocalDate

/**
 * 分享卡片生成器：Canvas 直绘 1080×1920 竖版 PNG。
 *
 * 两套风格（按事件类型分流）：
 * - A · 极光毛玻璃（生日/纪念日/情侣纪念等）：深色底 + 青绿/紫极光晕染 +
 *   毛玻璃信息卡（标签/标题/日期/倒计时）+ 底部三栏（月份/日期/星期）
 * - B · 深夜烛火（缅怀）：纯黑底 + 顶部烛光 + 翻页数字 + 斜体诗句，庄重克制
 *
 * 毛玻璃在 Canvas 里没有 backdrop-filter，用「半透明白底 + 卡片内极光微光」
 * 模拟磨砂透光质感。文字布局走纯函数，可单测。
 */
object ShareCardGenerator {

    private const val W = 1080
    private const val H = 1920

    // ---- A 风格配色 ----
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_MUTED = 0xFF8899AA.toInt()
    private const val COLOR_LABEL_GREEN = 0xFF66DDAA.toInt()
    private const val COLOR_MONTH_PURPLE = 0xFFAA88FF.toInt()
    private const val COLOR_DAY_BLUE = 0xFF66CCFF.toInt()
    private const val COLOR_KEY = 0xFF667788.toInt()

    // ---- B 风格配色 ----
    private const val COLOR_BLACK = 0xFF0F0F12.toInt()
    private const val COLOR_GOLD = 0xFFE8C080.toInt()
    private const val COLOR_GOLD_DIM = 0xFFCC9977.toInt()
    private const val COLOR_POEM = 0xFFAAAAAA.toInt()
    private const val COLOR_MEM_GRAY = 0xFF888888.toInt()
    private const val COLOR_MEM_DIM = 0xFF666666.toInt()
    private const val COLOR_MEM_FADE = 0xFF555555.toInt()

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
    // A · 极光毛玻璃
    // ================================================================

    private fun drawAurora(canvas: Canvas, b: Birthday) {
        // 1. 背景：深色三色渐变
        canvas.drawRect(
            0f, 0f, W.toFloat(), H.toFloat(),
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, W.toFloat(), H.toFloat(),
                    intArrayOf(0xFF0C1020.toInt(), 0xFF1A0F2E.toInt(), 0xFF0D1F1F.toInt()),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
        )

        // 2. 极光晕染：右上青绿、左下淡紫（径向渐变本身柔和，无需 blur）
        drawAuroraBlob(canvas, W + 100f, 60f, 640f, 0x2600FFC8)
        drawAuroraBlob(canvas, -40f, H + 120f, 580f, 0x1F9664FF)

        // 3. 顶部品牌栏
        val padTop = 100f
        val padSide = 78f
        val logoSize = 74f
        // logo：圆角方块（半透明白渐变 + 边框）+ 🌙
        canvas.drawRoundRect(
            RectF(padSide, padTop, padSide + logoSize, padTop + logoSize), 24f, 24f,
            Paint().apply {
                shader = LinearGradient(
                    padSide, padTop, padSide + logoSize, padTop + logoSize,
                    intArrayOf(0x1AFFFFFF.toInt(), 0x0DFFFFFF.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
        )
        canvas.drawRoundRect(
            RectF(padSide, padTop, padSide + logoSize, padTop + logoSize), 24f, 24f,
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x14FFFFFF.toInt()
            }
        )
        drawCenteredText(
            canvas, "🌙", padSide + logoSize / 2, padTop + logoSize / 2,
            Paint().apply { textSize = 38f }, centerVertical = true
        )
        // CHENJI
        drawText(
            canvas, "CHENJI", W - padSide, padTop + logoSize / 2,
            Paint().apply {
                color = COLOR_MUTED
                textSize = 26f
                letterSpacing = 0.18f
                textAlign = Paint.Align.RIGHT
            },
            centerVertical = true
        )

        // 4. 毛玻璃主卡片
        val glassTop = padTop + logoSize + 56f
        val glassBottom = H - 250f
        val glassRect = RectF(padSide, glassTop, W - padSide, glassBottom)
        // 4a. 卡片内极光微光（模拟 backdrop-filter 透出的底色，用 Path 裁剪圆角）
        val glassPath = Path().apply { addRoundRect(glassRect, 40f, 40f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(glassPath)
        drawAuroraBlob(canvas, glassRect.left + 100f, glassRect.top + 40f, 420f, 0x1200FFC8)
        drawAuroraBlob(canvas, glassRect.right - 60f, glassRect.bottom - 40f, 380f, 0x109664FF)
        canvas.restore()
        // 4b. 半透明白底 + 边框
        canvas.drawRoundRect(glassRect, 40f, 40f, Paint().apply { color = 0x0AFFFFFF.toInt() })
        canvas.drawRoundRect(
            glassRect, 40f, 40f,
            Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x14FFFFFF.toInt()
            }
        )

        // 4c. 卡片内信息（标签 / 标题 / 日期 / 倒计时）
        val innerLeft = glassRect.left + 48f
        val innerTop = glassRect.top + 56f
        val (tagText, tagColor) = tagFor(b.eventType)
        drawText(
            canvas, tagText, innerLeft, innerTop,
            Paint().apply { color = tagColor; textSize = 26f; typeface = Typeface.DEFAULT_BOLD }
        )
        drawText(
            canvas, b.name, innerLeft, innerTop + 84f,
            Paint().apply { color = COLOR_WHITE; textSize = 54f; typeface = Typeface.DEFAULT_BOLD }
        )
        drawText(
            canvas, dateLine(b), innerLeft, innerTop + 150f,
            Paint().apply { color = COLOR_MUTED; textSize = 26f }
        )
        // 倒计时（垂直居中于卡片剩余空间）
        val countY = glassRect.bottom - 120f
        val countdown = EventCalc.countdown(b)
        if (countdown == 0) {
            drawText(
                canvas, "🎉 就是今天", innerLeft, countY,
                Paint().apply { color = COLOR_WHITE; textSize = 52f; typeface = Typeface.DEFAULT_BOLD }
            )
        } else {
            drawText(
                canvas, "$countdown", innerLeft, countY,
                Paint().apply { color = COLOR_WHITE; textSize = 96f; typeface = Typeface.DEFAULT_BOLD }
            )
            drawText(
                canvas, "天后", innerLeft + measureWidth("$countdown", 96f) + 18f, countY + 12f,
                Paint().apply { color = COLOR_MUTED; textSize = 28f }
            )
        }

        // 5. 底部三栏：月份 / 日期 / 星期（下一次事件的阳历日期）
        val triTop = glassBottom + 40f
        val triBottom = triTop + 170f
        val gap = 20f
        val colWidth = (W - padSide * 2 - gap * 2) / 3f
        val nextDate = EventCalc.nextSolarDate(b).toLocalDate()
        val cols = listOf(
            Triple("${nextDate.monthValue}月", "月份", COLOR_MONTH_PURPLE),
            Triple("${nextDate.dayOfMonth}日", "日期", COLOR_DAY_BLUE),
            Triple(weekdayName(nextDate.dayOfWeek), "星期", COLOR_LABEL_GREEN)
        )
        cols.forEachIndexed { i, (value, key, colColor) ->
            val left = padSide + i * (colWidth + gap)
            val colRect = RectF(left, triTop, left + colWidth, triBottom)
            canvas.drawRoundRect(colRect, 24f, 24f, Paint().apply { color = 0x08FFFFFF.toInt() })
            canvas.drawRoundRect(
                colRect, 24f, 24f,
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = 0x0FFFFFFF.toInt()
                }
            )
            val cx = colRect.centerX()
            drawCenteredText(
                canvas, value, cx, colRect.centerY() - 20f,
                Paint().apply { this.color = colColor; textSize = 36f; typeface = Typeface.DEFAULT_BOLD }
            )
            drawCenteredText(
                canvas, key, cx, colRect.centerY() + 44f,
                Paint().apply { color = COLOR_KEY; textSize = 22f }
            )
        }
    }

    /** 极光晕染块：一个柔和径向渐变圆 */
    private fun drawAuroraBlob(canvas: Canvas, cx: Float, cy: Float, radius: Float, argb: Int) {
        canvas.drawCircle(
            cx, cy, radius,
            Paint().apply {
                shader = RadialGradient(cx, cy, radius, argb, 0x00FFFFFF.toInt(), Shader.TileMode.CLAMP)
            }
        )
    }

    /** 类型标签文案与颜色（对应设计稿 A 的标签适配表） */
    private fun tagFor(eventType: String): Pair<String, Int> = when (eventType) {
        EventType.MARRIAGE -> "💕 纪念日提醒" to 0xFFFF88AA.toInt()
        EventType.BABY -> "🍼 宝宝生日" to 0xFFFF88AA.toInt()
        EventType.LOVE -> "💑 情侣纪念" to 0xFFFF88CC.toInt()
        EventType.OTHER -> "📌 纪念日提醒" to COLOR_DAY_BLUE
        else -> "🎂 生日提醒" to COLOR_LABEL_GREEN
    }

    /** A 卡日期行：`1998年8月14日 · 阳历` / `1950年六月十五 · 农历` */
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
    // B · 深夜烛火（缅怀）
    // ================================================================

    private fun drawMemorial(canvas: Canvas, b: Birthday) {
        // 1. 纯黑底
        canvas.drawColor(COLOR_BLACK)

        // 2. 顶部烛光晕染（柔和椭圆扩散）
        canvas.drawOval(
            RectF(W / 2f - 240f, -140f, W / 2f + 240f, 340f),
            Paint().apply {
                shader = RadialGradient(
                    W / 2f, 0f, 320f,
                    0x1FFFFFA0.toInt(), 0x00FFA03C.toInt(), Shader.TileMode.CLAMP
                )
            }
        )

        // 3. 内容区（大量留白、居中）
        val cx = W / 2f
        // 🕯️
        drawCenteredText(canvas, "🕯️", cx, 300f, Paint().apply { textSize = 66f })
        // IN MEMORY
        drawCenteredText(
            canvas, "IN MEMORY", cx, 400f,
            Paint().apply { color = COLOR_GOLD_DIM; textSize = 24f; letterSpacing = 0.35f }
        )
        // 纪念文案
        drawCenteredText(
            canvas, "${b.name}离开我们已经", cx, 560f,
            Paint().apply { color = COLOR_MEM_GRAY; textSize = 28f }
        )

        // 翻页数字（天数逐位方块）
        val countdown = EventCalc.countdown(b)
        val digits = countdown.toString()
        val digitW = 88f
        val digitH = 104f
        val digitGap = 4f
        val totalW = digits.length * digitW + (digits.length - 1) * digitGap
        var dx = cx - totalW / 2f
        val digitsTop = 640f
        for (ch in digits) {
            val rect = RectF(dx, digitsTop, dx + digitW, digitsTop + digitH)
            canvas.drawRoundRect(
                rect, 20f, 20f,
                Paint().apply {
                    shader = LinearGradient(
                        0f, rect.top, 0f, rect.bottom,
                        intArrayOf(0xFF2A2A2A.toInt(), 0xFF1A1A1A.toInt()),
                        null, Shader.TileMode.CLAMP
                    )
                }
            )
            canvas.drawRoundRect(
                rect, 20f, 20f,
                Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    color = 0x14FFFFFF.toInt()
                }
            )
            drawCenteredText(
                canvas, ch.toString(), rect.centerX(), rect.centerY(),
                Paint().apply { color = COLOR_GOLD; textSize = 56f; typeface = Typeface.DEFAULT_BOLD },
                centerVertical = true
            )
            dx += digitW + digitGap
        }
        // 单位「天」
        drawText(
            canvas, "天", cx + totalW / 2f + 18f, digitsTop + digitH - 12f,
            Paint().apply { color = COLOR_MEM_DIM; textSize = 36f }
        )

        // 分隔线（60% 宽）
        val dividerW = W * 0.6f
        drawHorizontalLine(canvas, cx - dividerW / 2f, cx + dividerW / 2f, 900f, 0x0FFFFFFF.toInt(), 2f)

        // 斜体诗句
        val poemPaint = Paint().apply {
            color = COLOR_POEM
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        drawCenteredText(canvas, "有些人离开了", cx, 980f, poemPaint)
        drawCenteredText(canvas, "但永远活在记忆里", cx, 1035f, poemPaint)

        // 日期
        drawCenteredText(
            canvas, memorialDate(b), cx, 1140f,
            Paint().apply { color = COLOR_MEM_DIM; textSize = 24f }
        )

        // 底部品牌
        drawCenteredText(
            canvas, "辰记", cx, H - 120f,
            Paint().apply { color = COLOR_MEM_FADE; textSize = 22f; letterSpacing = 0.3f }
        )
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

    private fun measureWidth(text: String, textSize: Float): Float =
        Paint().apply { this.textSize = textSize }.measureText(text)

    private fun drawHorizontalLine(canvas: Canvas, x1: Float, x2: Float, y: Float, color: Int, width: Float) {
        canvas.drawLine(x1, y, x2, y, Paint().apply {
            this.color = color
            strokeWidth = width
        })
    }

    /** 左对齐文本（x 为左边缘） */
    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) y - (paint.ascent() + paint.descent()) / 2 else y
        canvas.drawText(text, x, baseline, paint)
    }

    /** 水平居中文本（paint 需为每次调用新建，textAlign 会被改写为 CENTER） */
    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, y: Float, paint: Paint, centerVertical: Boolean = false) {
        val baseline = if (centerVertical) y - (paint.ascent() + paint.descent()) / 2 else y
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, baseline, paint)
    }
}
