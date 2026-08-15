package com.birthapp.ui.detail

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.birthapp.awaitValue
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
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
 * DetailViewModel 状态变换测试：in-memory Room 注入，验证加载/未找到、
 * 置顶与暂停开关写库、删除。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DetailViewModelTest {

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

    private fun insertXiaoMing(): Long = runBlocking {
        db.birthdayDao().insert(
            Birthday(
                name = "小明",
                birthYear = 1998,
                birthMonth = 8,
                birthDay = 14,
                calendarType = "solar",
                notes = "喜欢打篮球"
            )
        )
    }

    @Test
    fun `加载_存在的记录填充状态`() {
        val id = insertXiaoMing()
        val vm = DetailViewModel(app, db)
        vm.load(id)
        val state = vm.uiState.awaitValue({ !it.loading && it.name == "小明" }, "详情加载")
        assertEquals(id, state.id)
        assertTrue(state.countdown > 0)
        assertTrue(state.primaryDate.contains("8月14日"))
        assertEquals("喜欢打篮球", state.notes)
    }

    @Test
    fun `加载_不存在的记录标记未找到`() {
        val vm = DetailViewModel(app, db)
        vm.load(999)
        val state = vm.uiState.awaitValue({ it.notFound }, "未找到")
        assertEquals(false, state.loading)
    }

    @Test
    fun `置顶_开关写库`() {
        val id = insertXiaoMing()
        val vm = DetailViewModel(app, db)
        vm.load(id)
        vm.uiState.awaitValue({ !it.loading && it.id == id }, "详情加载")

        vm.togglePinned()
        val pinned = vm.uiState.awaitValue({ it.isPinned }, "置顶开启")
        assertTrue(pinned.isPinned)
        assertEquals(true, runBlocking { db.birthdayDao().getById(id)?.pinned })

        vm.togglePinned()
        val unpinned = vm.uiState.awaitValue({ !it.isPinned }, "取消置顶")
        assertEquals(false, unpinned.isPinned)
        assertEquals(false, runBlocking { db.birthdayDao().getById(id)?.pinned })
    }

    @Test
    fun `暂停开关_写库且清空下次提醒日期`() {
        val id = insertXiaoMing()
        val vm = DetailViewModel(app, db)
        vm.load(id)
        vm.uiState.awaitValue({ !it.loading && it.id == id }, "详情加载")

        vm.toggleActive(false)
        val paused = vm.uiState.awaitValue({ !it.isActive }, "暂停")
        assertEquals(false, paused.isActive)
        val b = runBlocking { db.birthdayDao().getById(id) }
        assertEquals(false, b?.isActive)
        assertEquals(null, b?.nextReminderDate)
    }

    @Test
    fun `删除_触发deleted信号且记录移除`() {
        val id = insertXiaoMing()
        val vm = DetailViewModel(app, db)
        vm.load(id)
        vm.uiState.awaitValue({ !it.loading && it.id == id }, "详情加载")

        vm.delete()
        vm.deleted.awaitValue({ it }, "删除信号")
        assertEquals(0, runBlocking { db.birthdayDao().getAllOnce().size })
    }
}
