package com.birthapp.backup

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 备份文件编解码测试。
 *
 * 核心保障：一台手机 encode 出来的文本，另一台手机 decode 回去后，
 * 用户输入过的每个字段都原样保留（id、下次提醒日期这类运行时状态除外）。
 */
class BackupCodecTest {

    private fun sample(
        name: String = "张三",
        calendarType: String = "solar",
        eventType: String = EventType.BIRTHDAY,
    ) = Birthday(
        name = name,
        birthYear = 1990,
        birthMonth = 6,
        birthDay = 15,
        calendarType = calendarType,
        eventType = eventType,
    )

    @Test
    fun `编码再解码_用户字段原样保留`() {
        val original = listOf(
            // 覆盖各种边角：农历闰月、缅怀类型、已暂停、自定义提醒时间、多级提醒、置顶、专属 Emoji 和备注
            Birthday(
                name = "王奶奶",
                birthYear = 1944, birthMonth = 4, birthDay = 12,
                calendarType = "lunar", isLeapMonth = true,
                advanceDays = listOf(0, 3), reminderHour = 20, reminderMinute = 30,
                relation = "family", eventType = EventType.MEMORIAL,
                notes = "每年回老家", isActive = false, pinned = true, emoji = "🕯️",
            ),
            sample(name = "李四", eventType = EventType.LOVE),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))

        assertEquals(original.size, decoded.size)
        for (i in original.indices) {
            val a = original[i]
            val b = decoded[i]
            assertEquals(a.name, b.name)
            assertEquals(a.birthYear, b.birthYear)
            assertEquals(a.birthMonth, b.birthMonth)
            assertEquals(a.birthDay, b.birthDay)
            assertEquals(a.calendarType, b.calendarType)
            assertEquals(a.isLeapMonth, b.isLeapMonth)
            assertEquals(a.advanceDays, b.advanceDays)
            assertEquals(a.reminderHour, b.reminderHour)
            assertEquals(a.reminderMinute, b.reminderMinute)
            assertEquals(a.relation, b.relation)
            assertEquals(a.eventType, b.eventType)
            assertEquals(a.notes, b.notes)
            assertEquals(a.isActive, b.isActive)
            assertEquals(a.pinned, b.pinned)
            // v4：专属 Emoji 头像原样往返
            assertEquals(a.emoji, b.emoji)
            // 运行时状态不进备份：导入方数据库重新分配 id、重算提醒
            assertEquals(0L, b.id)
            assertEquals(null, b.nextReminderDate)
        }
    }

    @Test
    fun `空列表也能往返_records为空数组`() {
        assertEquals(0, BackupCodec.decode(BackupCodec.encode(emptyList())).size)
    }

    @Test
    fun `不是JSON_报不是有效的备份文件`() {
        assertThrowsMessage("不是有效的备份文件") { BackupCodec.decode("随便一段文字") }
    }

    @Test
    fun `别的App的JSON_报不是辰记的备份`() {
        val alien = """{"app":"com.other.app","format":1,"records":[]}"""
        assertThrowsMessage("不是辰记导出的备份文件") { BackupCodec.decode(alien) }
    }

    @Test
    fun `缺app标记_同样拒绝`() {
        assertThrowsMessage("不是辰记导出的备份文件") {
            BackupCodec.decode("""{"format":1,"records":[]}""")
        }
    }

    @Test
    fun `未来版本号_报版本不支持`() {
        val future = """{"app":"com.birthapp","format":99,"records":[]}"""
        assertThrowsMessage("备份文件版本不支持") { BackupCodec.decode(future) }
    }

    @Test
    fun `没有records字段_报没有记录数据`() {
        assertThrowsMessage("备份文件里没有记录数据") {
            BackupCodec.decode("""{"app":"com.birthapp","format":1}""")
        }
    }

    @Test
    fun `记录缺姓名或日期_指出第几条不完整`() {
        val bad = """
            {"app":"com.birthapp","format":1,"records":[
              {"name":"好人","birthYear":1990,"birthMonth":1,"birthDay":1},
              {"name":"","birthYear":1990,"birthMonth":1,"birthDay":1}
            ]}
        """.trimIndent()
        assertThrowsMessage("第 2 条记录不完整") { BackupCodec.decode(bad) }
    }

    @Test
    fun `月份日期越界_视为记录不完整`() {
        val bad = """
            {"app":"com.birthapp","format":1,"records":[
              {"name":"甲","birthYear":1990,"birthMonth":13,"birthDay":1}
            ]}
        """.trimIndent()
        assertThrowsMessage("第 1 条记录不完整") { BackupCodec.decode(bad) }
    }

    @Test
    fun `可选字段缺失_用安全默认值兜底`() {
        val minimal = """
            {"app":"com.birthapp","format":1,"records":[
              {"name":"甲","birthYear":1990,"birthMonth":6,"birthDay":15}
            ]}
        """.trimIndent()
        val b = BackupCodec.decode(minimal).single()
        assertEquals("solar", b.calendarType)
        assertEquals(false, b.isLeapMonth)
        assertEquals(listOf(0), b.advanceDays)
        assertEquals(8, b.reminderHour)
        assertEquals(0, b.reminderMinute)
        assertEquals("family", b.relation)
        assertEquals(EventType.BIRTHDAY, b.eventType)
        assertEquals("", b.notes)
        assertEquals(true, b.isActive)
        assertEquals(false, b.pinned)
    }

    @Test
    fun `脏数据被拉回合法范围`() {
        val dirty = """
            {"app":"com.birthapp","format":1,"records":[
              {"name":"甲","birthYear":1990,"birthMonth":6,"birthDay":15,
               "calendarType":"weird","advanceDays":999,
               "reminderHour":30,"reminderMinute":-5}
            ]}
        """.trimIndent()
        val b = BackupCodec.decode(dirty).single()
        assertEquals("solar", b.calendarType)   // 不认识的历法一律按公历
        assertEquals(listOf(365), b.advanceDays)
        assertEquals(23, b.reminderHour)
        assertEquals(0, b.reminderMinute)
    }

    @Test
    fun `v1旧格式_单值提前天数兼容解码`() {
        // v1 备份里 advanceDays 是单个整数（3 = 提前 3 天），新版本必须能读
        val v1 = """
            {"app":"com.birthapp","format":1,"records":[
              {"name":"老张","birthYear":1990,"birthMonth":6,"birthDay":15,
               "advanceDays":3,"isActive":true}
            ]}
        """.trimIndent()
        val b = BackupCodec.decode(v1).single()
        assertEquals(listOf(3), b.advanceDays)
        assertEquals(false, b.pinned)
    }

    @Test
    fun `v2格式_多级数组与置顶往返`() {
        val original = Birthday(
            name = "多级", birthYear = 1990, birthMonth = 6, birthDay = 15,
            calendarType = "solar", advanceDays = listOf(0, 7, 30),
            pinned = true,
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(original))).single()
        assertEquals(listOf(0, 7, 30), decoded.advanceDays)
        assertEquals(true, decoded.pinned)
    }

    @Test
    fun `v2格式_提前级别数组为空时退化为当天`() {
        val empty = """
            {"app":"com.birthapp","format":2,"records":[
              {"name":"甲","birthYear":1990,"birthMonth":6,"birthDay":15,"advanceDays":[]}
            ]}
        """.trimIndent()
        assertEquals(listOf(0), BackupCodec.decode(empty).single().advanceDays)
    }

    @Test
    fun `v3老备份缺emoji字段_解码为自动空字符串`() {
        // 模拟 v3 时代导出的备份：记录里没有 emoji 键，导入后应回落到空（自动头像）
        val old = """
            {"app":"com.birthapp","format":4,"records":[
              {"name":"丙","birthYear":1992,"birthMonth":3,"birthDay":8,"calendarType":"solar"}
            ]}
        """.trimIndent()
        assertEquals("", BackupCodec.decode(old).single().emoji)
    }

    @Test
    fun `v4格式_专属Emoji头像往返保留`() {
        val original = Birthday(
            name = "丁", birthYear = 2001, birthMonth = 8, birthDay = 1,
            calendarType = "solar", emoji = "🐶"
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(listOf(original))).single()
        assertEquals("🐶", decoded.emoji)
    }

    // ==================== settings（备份含主题设置，v3）====================

    @Test
    fun `v3格式_主题设置随备份编码并解析`() {
        val text = BackupCodec.encode(
            listOf(sample()),
            themeMode = "DARK",
            dynamicColor = true
        )
        val settings = BackupCodec.decodeSettings(text)
        assertEquals("DARK", settings?.themeMode)
        assertEquals(true, settings?.dynamicColor)
        // 记录部分不受 settings 影响
        assertEquals(1, BackupCodec.decode(text).size)
    }

    @Test
    fun `老版本备份_没有settings块时解析为null`() {
        val old = """
            {"app":"com.birthapp","format":2,"records":[
              {"name":"甲","birthYear":1990,"birthMonth":6,"birthDay":15}
            ]}
        """.trimIndent()
        assertEquals(null, BackupCodec.decodeSettings(old))
    }

    @Test
    fun `v3编码_未提供设置时不写settings块`() {
        val text = BackupCodec.encode(listOf(sample()))
        assertEquals(null, BackupCodec.decodeSettings(text))
    }

    private fun assertThrowsMessage(expectedPart: String, block: () -> Unit) {
        try {
            block()
            fail("应当抛出 IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "错误信息应包含「$expectedPart」，实际是：${e.message}",
                e.message.orEmpty().contains(expectedPart)
            )
        }
    }
}
