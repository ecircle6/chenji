package com.birthapp.ui.home

import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Test

/** 首页筛选规则测试：关系+类型叠加、搜索跳出筛选、类型胶囊的智能显示条件 */
class HomeFilterTest {

    private fun record(
        name: String,
        relation: String = "family",
        eventType: String = EventType.BIRTHDAY,
        notes: String = "",
    ) = Birthday(
        name = name, birthYear = 1990, birthMonth = 6, birthDay = 15,
        calendarType = "solar", relation = relation, eventType = eventType, notes = notes,
    )

    private val data = listOf(
        record("爸爸", relation = "family", eventType = EventType.BIRTHDAY),
        record("爷爷", relation = "family", eventType = EventType.MEMORIAL),
        record("小王", relation = "friend", eventType = EventType.BIRTHDAY),
        record("我们", relation = "other", eventType = EventType.LOVE, notes = "在一起的日子"),
    )

    @Test
    fun `全部加全部_原样返回`() {
        assertEquals(4, HomeFilter.apply(data, "all", "all", "").size)
    }

    @Test
    fun `只筛关系`() {
        assertEquals(
            listOf("爸爸", "爷爷"),
            HomeFilter.apply(data, "family", "all", "").map { it.name }
        )
    }

    @Test
    fun `只筛类型`() {
        assertEquals(
            listOf("爸爸", "小王"),
            HomeFilter.apply(data, "all", EventType.BIRTHDAY, "").map { it.name }
        )
    }

    @Test
    fun `关系和类型叠加_两个条件同时满足`() {
        assertEquals(
            listOf("爷爷"),
            HomeFilter.apply(data, "family", EventType.MEMORIAL, "").map { it.name }
        )
    }

    @Test
    fun `叠加筛到空_返回空列表`() {
        assertEquals(0, HomeFilter.apply(data, "friend", EventType.MEMORIAL, "").size)
    }

    @Test
    fun `搜索时跳出所有筛选_按姓名和备注全局找`() {
        // 停在"家人+缅怀"上搜朋友的名字，也要能搜到
        assertEquals(
            listOf("小王"),
            HomeFilter.apply(data, "family", EventType.MEMORIAL, "小王").map { it.name }
        )
        // 备注也参与搜索
        assertEquals(
            listOf("我们"),
            HomeFilter.apply(data, "all", "all", "在一起").map { it.name }
        )
    }

    @Test
    fun `类型胶囊_按固定顺序列出实际出现过的类型`() {
        // 数据里有 生日/缅怀/情侣 三种，顺序应是 生日、情侣、缅怀
        assertEquals(
            listOf(EventType.BIRTHDAY, EventType.LOVE, EventType.MEMORIAL),
            HomeFilter.availableTypes(data)
        )
    }

    @Test
    fun `类型胶囊_只有一种类型时列表长度为1`() {
        // 界面按"两种以上才显示"判断，这里保证统计本身正确
        val onlyBirthday = listOf(record("甲"), record("乙"))
        assertEquals(listOf(EventType.BIRTHDAY), HomeFilter.availableTypes(onlyBirthday))
    }

    @Test
    fun `类型胶囊_老数据的结婚宝宝类型也能出现`() {
        val legacy = listOf(
            record("甲", eventType = EventType.MARRIAGE),
            record("乙", eventType = EventType.BABY),
        )
        assertEquals(
            listOf(EventType.MARRIAGE, EventType.BABY),
            HomeFilter.availableTypes(legacy)
        )
    }

    @Test
    fun `类型胶囊_不认识的类型不进胶囊`() {
        val weird = listOf(record("甲", eventType = "alien"), record("乙"))
        assertEquals(listOf(EventType.BIRTHDAY), HomeFilter.availableTypes(weird))
    }
}
