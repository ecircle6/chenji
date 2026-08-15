package com.birthapp.ui.home

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.birthapp.awaitValue
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.data.EventType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HomeViewModel 状态变换测试：in-memory Room 注入真实数据库，
 * 验证排序（置顶→暂停→倒计时）、类型筛选、搜索与删除写库。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class HomeViewModelTest {

    private lateinit var db: AppDatabase
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun birthday(
        name: String,
        month: Int = 8,
        day: Int = 14,
        eventType: String = EventType.BIRTHDAY,
        calendarType: String = "solar",
        relation: String = "family",
        isActive: Boolean = true,
        pinned: Boolean = false,
        notes: String = ""
    ) = Birthday(
        name = name,
        birthYear = 1998,
        birthMonth = month,
        birthDay = day,
        calendarType = calendarType,
        relation = relation,
        eventType = eventType,
        notes = notes,
        isActive = isActive,
        pinned = pinned
    )

    private fun insertAll(vararg bs: Birthday) = runBlocking {
        bs.forEach { db.birthdayDao().insert(it) }
    }

    @Test
    fun `排序_置顶最前_暂停沉底_其余按倒计时`() {
        // 今天 2026-08-15：小明 8/14 = 364 天后（置顶）、小红 8/22 = 7 天后、爷爷农历七月十五（暂停）
        insertAll(
            birthday("小明", pinned = true),
            birthday("小红", month = 8, day = 22),
            birthday("爷爷", eventType = EventType.MEMORIAL, calendarType = "lunar", month = 7, day = 15, isActive = false)
        )
        val vm = HomeViewModel(app, db)
        val list = vm.displayBirthdays.awaitValue({ it.size == 3 }, "列表加载")
        assertEquals(listOf("小明", "小红", "爷爷"), list.map { it.birthday.name })
        assertTrue(list[0].isPinned)
        assertTrue(list[2].isPaused)
    }

    @Test
    fun `类型筛选_选中类型只显示对应记录`() {
        insertAll(
            birthday("小明"),
            birthday("情侣纪念", eventType = EventType.LOVE, relation = "other")
        )
        val vm = HomeViewModel(app, db)
        vm.displayBirthdays.awaitValue({ it.size == 2 }, "列表加载")
        assertEquals(listOf("birthday", "love"), vm.availableTypes.value)

        vm.selectType(EventType.LOVE)
        val list = vm.displayBirthdays.awaitValue({ it.size == 1 && it.first().eventType == EventType.LOVE }, "类型筛选")
        assertEquals("情侣纪念", list.first().birthday.name)
    }

    @Test
    fun `搜索_跳出关系筛选全局查找`() {
        insertAll(
            birthday("小明", relation = "family", notes = "喜欢篮球"),
            birthday("小红", relation = "friend")
        )
        val vm = HomeViewModel(app, db)
        vm.displayBirthdays.awaitValue({ it.size == 2 }, "列表加载")

        // 先停在「朋友」标签，再搜「篮球」——搜索是全局的，应命中家人标签下的小明
        vm.selectTab("friend")
        vm.updateSearchQuery("篮球")
        val list = vm.displayBirthdays.awaitValue({ it.size == 1 }, "搜索")
        assertEquals("小明", list.first().birthday.name)
    }

    @Test
    fun `搜索状态_进出搜索切换关键词`() {
        val vm = HomeViewModel(app, db)
        vm.enterSearch()
        assertTrue(vm.isSearching.value)
        vm.updateSearchQuery("小明")
        assertEquals("小明", vm.searchQuery.value)
        vm.exitSearch()
        assertTrue(!vm.isSearching.value)
        assertEquals("", vm.searchQuery.value)
    }

    @Test
    fun `删除记录_数据库同步移除`() {
        insertAll(birthday("小明"))
        val vm = HomeViewModel(app, db)
        val list = vm.displayBirthdays.awaitValue({ it.size == 1 }, "列表加载")

        vm.deleteBirthday(list.first().birthday)
        vm.displayBirthdays.awaitValue({ it.isEmpty() }, "删除后列表")
        val remaining = runBlocking { db.birthdayDao().getAllOnce() }
        assertEquals(0, remaining.size)
    }
}
