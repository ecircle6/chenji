package com.birthapp.backup

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 导入合并去重的测试：已有的跳过、新的追加、文件内自重复只留一条 */
class BackupMergeTest {

    private fun person(
        name: String,
        calendarType: String = "solar",
        year: Int = 1990, month: Int = 6, day: Int = 15,
        isLeapMonth: Boolean = false,
        eventType: String = EventType.BIRTHDAY,
        reminderHour: Int = 8,
        notes: String = "",
    ) = Birthday(
        name = name, birthYear = year, birthMonth = month, birthDay = day,
        calendarType = calendarType, isLeapMonth = isLeapMonth,
        eventType = eventType, reminderHour = reminderHour, notes = notes,
    )

    @Test
    fun `已有的跳过_新的保留`() {
        val existing = listOf(person("张三"))
        val incoming = listOf(person("张三"), person("李四"))
        val fresh = BackupMerge.filterNew(existing, incoming)
        assertEquals(listOf("李四"), fresh.map { it.name })
    }

    @Test
    fun `提醒时间和备注不同_仍算同一条`() {
        // 同一个人在两台手机上设了不同提醒时间，不能重复导入
        val existing = listOf(person("张三", reminderHour = 8, notes = "老同学"))
        val incoming = listOf(person("张三", reminderHour = 20, notes = ""))
        assertTrue(BackupMerge.filterNew(existing, incoming).isEmpty())
    }

    @Test
    fun `同名不同日期_算两个人`() {
        val existing = listOf(person("张三", day = 15))
        val incoming = listOf(person("张三", day = 16))
        assertEquals(1, BackupMerge.filterNew(existing, incoming).size)
    }

    @Test
    fun `同名同日期不同历法_不算重复`() {
        val existing = listOf(person("张三", calendarType = "solar"))
        val incoming = listOf(person("张三", calendarType = "lunar"))
        assertEquals(1, BackupMerge.filterNew(existing, incoming).size)
    }

    @Test
    fun `同人不同事件类型_不算重复`() {
        // 同一天既是生日又是纪念日的场景
        val existing = listOf(person("张三", eventType = EventType.BIRTHDAY))
        val incoming = listOf(person("张三", eventType = EventType.MEMORIAL))
        assertEquals(1, BackupMerge.filterNew(existing, incoming).size)
    }

    @Test
    fun `闰月标记不同_不算重复`() {
        val existing = listOf(person("张三", calendarType = "lunar", isLeapMonth = false))
        val incoming = listOf(person("张三", calendarType = "lunar", isLeapMonth = true))
        assertEquals(1, BackupMerge.filterNew(existing, incoming).size)
    }

    @Test
    fun `备份文件内部自重复_只留第一条`() {
        val incoming = listOf(person("张三"), person("张三"), person("李四"))
        val fresh = BackupMerge.filterNew(emptyList(), incoming)
        assertEquals(listOf("张三", "李四"), fresh.map { it.name })
    }

    @Test
    fun `本机为空_全部导入`() {
        val incoming = listOf(person("张三"), person("李四"))
        assertEquals(2, BackupMerge.filterNew(emptyList(), incoming).size)
    }
}
