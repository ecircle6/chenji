package com.birthapp.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
 * 分享卡片生成器：Canvas 直绘 1080×1920 竖版 PNG，与设计稿 design-card-demo.html 逐项一致
 * （CSS 值按 1080/320 = 3.375 倍换算）。
 *
 * A · 极光毛玻璃（生日/纪念日/情侣纪念）：整卡 135° 深色渐变底（#0c1020 → #1a0f2e → #0d1f1f）
 *   + 右上青绿 / 左下淡紫极光光斑；内容自上而下：品牌栏（Logo 左、CHENJI 右）→ 毛玻璃信息卡
 *   （标签/名字/日期 + 剩余空间垂直居中的大号倒计时）→ 底部三栏（月份/日期/星期）。
 * B · 深夜烛火（缅怀）：纯黑底 #0f0f12 + 顶部居中烛光晕；居中紧凑竖排：🕯️ → IN MEMORY →
 *   纪念文案 → 翻页数字 → 分隔线 + 斜体诗句 → 日期 → 贴底品牌。
 * 两种风格圆角 24（此处 81px）外区域透明。
 */
object ShareCardGenerator {

    private const val W = 1080
    private const val H = 1920
    private const val RADIUS = 81f        // CSS 24px × 3.375
    private const val PAD = 88f           // CSS 26px（左右）× 3.375
    private const val PAD_TOP = 115f      // CSS 34px（上下）× 3.375

    // A 风格配色
    private const val COLOR_A_BG1 = 0xFF0C1020.toInt()
    private const val COLOR_A_BG2 = 0xFF1A0F2E.toInt()
    private const val COLOR_A_BG3 = 0xFF0D1F1F.toInt()
    private const val COLOR_TITLE = 0xFFFFFFFF.toInt()
    private const val COLOR_SUB = 0xFF8899AA.toInt()
    private const val COLOR_TAG = 0xFF66DDAA.toInt()
    private const val COLOR_TAG_MARRIAGE = 0xFFFF88AA.toInt()
    private const val COLOR_TAG_LOVE = 0xFFFF88CC.toInt()
    private const val COLOR_MONTH = 0xFFAA88FF.toInt()
    private const val COLOR_DAY = 0xFF66CCFF.toInt()
    private const val COLOR_WEEK = 0xFF66DDAA.toInt()
    private const val COLOR_TRI_KEY = 0xFF667788.toInt()

    // B 风格配色
    private const val COLOR_B_BG = 0xFF0F0F12.toInt()
    private const val COLOR_MEM_TOP = 0xFFCC9977.toInt()
    private const val COLOR_MEM_TEXT = 0xFF888888.toInt()
    private const val COLOR_POEM_B = 0xFFAAAAAA.toInt()
    private const val COLOR_MEM_DATE = 0xFF666666.toInt()
    private const val COLOR_BRAND_B = 0xFF555555.toInt()
    private const val COLOR_DIGIT = 0xFFE8C080.toInt()
    private const val COLOR_DIGIT_BG_TOP = 0xFF2A2A2A.toInt()
    private const val COLOR_DIGIT_BG_BOTTOM = 0xFF1A1A1A.toInt()

    /** 生成卡片位图（1080×1920，圆角外透明），保存到 cacheDir/share/ */
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
    // A · 极光毛玻璃（1080×1920 竖版）
    // ================================================================

    private fun drawAurora(canvas: Canvas, b: Birthday) {
        val cardRect = RectF(0f, 0f, W.toFloat(), H.toFloat())

        // 0. 圆角裁剪：渐变底与极光光斑只出现在圆角内，角外保持透明
        val clip = Path().apply { addRoundRect(cardRect, RADIUS, RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)

        // 1. 整卡背景：135° 渐变 #0c1020 → #1a0f2e(50%) → #0d1f1f
        canvas.drawRect(cardRect, Paint().apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(),
                intArrayOf(COLOR_A_BG1, COLOR_A_BG2, COLOR_A_BG3),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        })

        // 2. 极光光斑：右上青绿 rgba(0,255,200,.15)、左下淡紫 rgba(150,100,255,.12)
        drawGlow(canvas, 878f, 135f, 675f, 0x2600FFC8.toInt())
        drawGlow(canvas, 202f, 1751f, 608f, 0x1F9664FF.toInt())
        canvas.restore()

        // 3. 卡片描边 rgba(255,255,255,.06)
        canvas.drawRoundRect(inset(cardRect, 1.5f), RADIUS, RADIUS, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x0FFFFFFF.toInt()
        })

        // 4. 品牌栏：Logo 左（36→122 圆角 12→40，白渐变底），CHENJI 右（字距 2px→0.17em）
        val logoRect = RectF(PAD, PAD_TOP, PAD + 122f, PAD_TOP + 122f)
        canvas.drawRoundRect(logoRect, 40f, 40f, Paint().apply {
            shader = LinearGradient(logoRect.left, logoRect.top, logoRect.right, logoRect.bottom,
                intArrayOf(0x1AFFFFFF.toInt(), 0x0DFFFFFF.toInt()), null, Shader.TileMode.CLAMP)
        })
        canvas.drawRoundRect(inset(logoRect, 1.5f), 40f, 40f, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x14FFFFFF.toInt()
        })
        drawCenteredText(canvas, "🌙", logoRect.centerX(), logoRect.centerY(),
            Paint().apply { textSize = 60f }, centerVertical = true)
        val brandPaint = Paint().apply { color = COLOR_SUB; textSize = 40f; letterSpacing = 0.17f }
        brandPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("CHENJI", W - PAD - brandPaint.letterSpacing * brandPaint.textSize,
            centerBaseline(logoRect.centerY(), brandPaint), brandPaint)
        brandPaint.textAlign = Paint.Align.LEFT

        // 5. 毛玻璃信息卡：flex:1 撑满品牌栏与三栏之间的剩余空间
        val glassTop = PAD_TOP + 122f + 94f        // 品牌行下边距 28px→94
        val glassBottom = H - PAD_TOP - 280f       // 留出三栏行（行距 20px→67 + 高 213）
        val glassRect = RectF(PAD, glassTop, W - PAD, glassBottom)
        canvas.drawRoundRect(glassRect, 68f, 68f, Paint().apply { color = 0x0AFFFFFF.toInt() })
        canvas.drawRoundRect(inset(glassRect, 1.5f), 68f, 68f, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x14FFFFFF.toInt()
        })

        // 内容（内边距 94 水平 / 81 垂直，标签/名字/日期左对齐堆叠）
        val innerLeft = glassRect.left + 94f
        var top = glassRect.top + 81f
        drawTextTop(canvas, tagText(b.eventType), innerLeft, top,
            Paint().apply { color = tagColor(b.eventType); textSize = 44f })
        top += 55f + 40f   // 标签行 + 下边距 12px→40
        drawTextTop(canvas, b.name, innerLeft, top, Paint().apply {
            color = COLOR_TITLE; textSize = 88f; typeface = Typeface.DEFAULT_BOLD
        })
        top += 110f + 27f  // 名字行 + 下边距 8px→27
        drawTextTop(canvas, dateLine(b), innerLeft, top, Paint().apply { color = COLOR_SUB; textSize = 44f })
        top += 55f + 67f   // 日期行 + 下边距 20px→67

        // 倒计时在剩余空间垂直居中（CSS margin-top/bottom:auto）
        val innerBottom = glassRect.bottom - 81f
        val countCenterY = top + (innerBottom - top) / 2f
        val countdown = EventCalc.countdown(b)
        if (countdown == 0) {
            drawCenteredText(canvas, "🎉 就是今天", innerLeft, countCenterY, Paint().apply {
                color = COLOR_TITLE; textSize = 88f; typeface = Typeface.DEFAULT_BOLD
            }, centerVertical = true, alignLeft = true)
        } else {
            val numPaint = Paint().apply {
                color = COLOR_TITLE; textSize = 162f; typeface = Typeface.DEFAULT_BOLD
            }
            val unitPaint = Paint().apply { color = COLOR_SUB; textSize = 47f }
            val baseline = centerBaseline(countCenterY, numPaint)
            canvas.drawText("$countdown", innerLeft, baseline, numPaint)
            canvas.drawText("天后", innerLeft + numPaint.measureText("$countdown") + 27f, baseline, unitPaint)
        }

        // 6. 底部三栏：月份/日期/星期
        val triTop = glassRect.bottom + 67f
        val colGap = 34f
        val colW = (W - PAD * 2 - colGap * 2) / 3f
        val nextDate = EventCalc.nextSolarDate(b).toLocalDate()
        val cols = listOf(
            Triple("${nextDate.monthValue}月", "月份", COLOR_MONTH),
            Triple("${nextDate.dayOfMonth}日", "日期", COLOR_DAY),
            Triple(weekdayName(nextDate.dayOfWeek), "星期", COLOR_WEEK)
        )
        cols.forEachIndexed { i, (value, key, colColor) ->
            val left = PAD + i * (colW + colGap)
            val colRect = RectF(left, triTop, left + colW, triTop + 213f)
            canvas.drawRoundRect(colRect, 40f, 40f, Paint().apply { color = 0x08FFFFFF.toInt() })
            canvas.drawRoundRect(inset(colRect, 1.5f), 40f, 40f, Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x0FFFFFFF.toInt()
            })
            val vPaint = Paint().apply {
                this.color = colColor; textSize = 61f; typeface = Typeface.DEFAULT_BOLD
            }
            drawCenteredText(canvas, value, colRect.centerX(), colRect.top + 78.5f, vPaint, centerVertical = true)
            drawCenteredText(canvas, key, colRect.centerX(), colRect.top + 149f,
                Paint().apply { color = COLOR_TRI_KEY; textSize = 37f }, centerVertical = true)
        }
    }

    // ================================================================
    // B · 深夜烛火（1080×1920 竖版）
    // ================================================================

    private fun drawMemorial(canvas: Canvas, b: Birthday) {
        val cardRect = RectF(0f, 0f, W.toFloat(), H.toFloat())
        val clip = Path().apply { addRoundRect(cardRect, RADIUS, RADIUS, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clip)

        // 1. 纯黑底 #0f0f12 + 顶中烛光晕 rgba(255,160,60,.12)
        canvas.drawRect(cardRect, Paint().apply { color = COLOR_B_BG })
        drawGlow(canvas, W / 2f, 0f, 450f, 0x1EFFA03C.toInt())
        canvas.restore()

        canvas.drawRoundRect(inset(cardRect, 1.5f), RADIUS, RADIUS, Paint().apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x0DFFFFFF.toInt()
        })

        // 内容：内边距 48px→162，全部居中
        val cx = W / 2f
        val padV = 162f
        var top = padV

        // 🕯️ 32px→108
        drawCenteredText(canvas, "🕯️", cx, top, Paint().apply { textSize = 108f })
        top += 135f + 40f
        // IN MEMORY 11px→37 字距 4px→0.36em #cc9977
        drawCenteredText(canvas, "IN MEMORY", cx, top,
            Paint().apply { color = COLOR_MEM_TOP; textSize = 37f; letterSpacing = 0.36f })
        top += 46f + 148f
        // "xxx离开我们已经" 13px→44 #888
        drawCenteredText(canvas, "${b.name}离开我们已经", cx, top,
            Paint().apply { color = COLOR_MEM_TEXT; textSize = 44f })
        top += 55f + 40f

        // 翻页数字 44×52→148×176（相邻间距 1px×2→7），#2a2a2a→#1a1a1a 渐变底，金字 #e8c080
        val digits = EventCalc.countdown(b).toString()
        val digitW = 148f; val digitH = 176f; val digitGap = 7f
        val totalW = digits.length * digitW + (digits.length - 1) * digitGap
        var dx = cx - totalW / 2f
        for (ch in digits) {
            val rect = RectF(dx, top, dx + digitW, top + digitH)
            canvas.drawRoundRect(rect, 34f, 34f, Paint().apply {
                shader = LinearGradient(0f, rect.top, 0f, rect.bottom,
                    intArrayOf(COLOR_DIGIT_BG_TOP, COLOR_DIGIT_BG_BOTTOM), null, Shader.TileMode.CLAMP)
            })
            canvas.drawRoundRect(inset(rect, 1.5f), 34f, 34f, Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x14FFFFFF.toInt()
            })
            drawCenteredText(canvas, ch.toString(), rect.centerX(), rect.centerY(),
                Paint().apply { color = COLOR_DIGIT; textSize = 94f; typeface = Typeface.DEFAULT_BOLD },
                centerVertical = true)
            dx += digitW + digitGap
        }
        // 「天」18px→61 #666，与数字底对齐（下边距 10px→34）
        val unitPaint = Paint().apply { color = COLOR_MEM_DATE; textSize = 61f }
        canvas.drawText("天", cx + totalW / 2f + 20f, top + digitH - 34f - unitPaint.descent(), unitPaint)
        top += digitH + 148f

        // 分隔线 60% 宽（527）居中
        canvas.drawLine(cx - 263.5f, top, cx + 263.5f, top, Paint().apply {
            this.color = 0x0FFFFFFF.toInt(); strokeWidth = 3f
        })
        top += 67f + 67f   // 线盒 padding-top 20px→67 + 诗句 margin-top 20px→67

        // 诗句 14px→47 斜体 #aaa 行高 1.8→85
        val poemPaint = Paint().apply {
            color = COLOR_POEM_B; textSize = 47f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        drawCenteredText(canvas, "有些人离开了", cx, top, poemPaint)
        drawCenteredText(canvas, "但永远活在记忆里", cx, top + 85f, poemPaint)
        top += 85f * 2 + 54f

        // 日期 11px→37 #666
        drawCenteredText(canvas, memorialDate(b), cx, top,
            Paint().apply { color = COLOR_MEM_DATE; textSize = 37f })

        // 贴底品牌 10px→34 #555 字距 3px→0.3em（padding-top 24px→81）
        val brandPaint = Paint().apply { color = COLOR_BRAND_B; textSize = 34f; letterSpacing = 0.3f }
        val fm = brandPaint.fontMetrics
        drawCenteredText(canvas, "辰 记", cx, H - padV - (fm.bottom - fm.top) / 2f, brandPaint, centerVertical = true)
    }

    private fun memorialDate(b: Birthday): String = if (b.calendarType == "lunar") {
        "农历${LunarCalendar.formatLunarDate(b.birthMonth, b.birthDay)} · ${b.birthYear}年"
    } else {
        "${b.birthMonth}月${b.birthDay}日 · ${b.birthYear}年"
    }

    // ================================================================
    // 工具
    // ================================================================

    /** 柔和光斑：径向渐变由 color 渐隐到透明，模拟 CSS 的 radial-gradient + blur */
    private fun drawGlow(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        canvas.drawCircle(cx, cy, radius, Paint().apply {
            shader = RadialGradient(cx, cy, radius,
                intArrayOf(color, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        })
    }

    /** 左对齐文本，文字顶对齐 y */
    private fun drawTextTop(canvas: Canvas, text: String, xLeft: Float, y: Float, paint: Paint) {
        canvas.drawText(text, xLeft, y - paint.fontMetrics.top, paint)
    }

    /** 水平居中文本；centerVertical=true 时 y 为行盒垂直中心，alignLeft=true 时左对齐 */
    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, y: Float, paint: Paint,
                                 centerVertical: Boolean = false, alignLeft: Boolean = false) {
        paint.textAlign = if (alignLeft) Paint.Align.LEFT else Paint.Align.CENTER
        val baseline = if (centerVertical) centerBaseline(y, paint) else y - paint.fontMetrics.top
        canvas.drawText(text, cx, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /** 行盒垂直中心对应的基线 */
    private fun centerBaseline(centerY: Float, paint: Paint): Float =
        centerY - (paint.ascent() + paint.descent()) / 2

    /** 向内收 d 的矩形（用于让描边与填充同边界时描边不溢出） */
    private fun inset(rect: RectF, d: Float): RectF =
        RectF(rect.left + d, rect.top + d, rect.right - d, rect.bottom - d)

    private fun tagColor(eventType: String): Int = when (eventType) {
        EventType.MARRIAGE -> COLOR_TAG_MARRIAGE   // 纪念日 暖粉
        EventType.LOVE -> COLOR_TAG_LOVE           // 情侣 粉紫
        else -> COLOR_TAG                          // 默认 青绿
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
