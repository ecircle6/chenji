package com.birthapp.ui.add

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.birthapp.awaitValue
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.settings.ReminderSettings
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
 * AddEditViewModel 状态变换测试：默认提醒时间来自设置、多级提前提醒增删、
 * 保存写库、编辑加载。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AddEditViewModelTest {

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

    @Test
    fun `默认提醒时间_来自设置里的默认值`() {
        ReminderSettings(app).setDefaultTime(7, 30)
        val vm = AddEditViewModel(app, db)
        assertEquals(7, vm.uiState.value.reminderHour)
        assertEquals(30, vm.uiState.value.reminderMinute)
    }

    @Test
    fun `提前提醒_预设多选与再点取消`() {
        val vm = AddEditViewModel(app, db)
        vm.toggleAdvanceDay(3)
        assertTrue(3 in vm.uiState.value.advanceDays)
        vm.toggleAdvanceDay(3)
        assertTrue(3 !in vm.uiState.value.advanceDays)
        // 0（当天）始终保留：移除空列表时退化为 [0]
        assertEquals(listOf(0), vm.uiState.value.advanceDays)
    }

    @Test
    fun `提前提醒_自定义添加去重`() {
        val vm = AddEditViewModel(app, db)
        vm.addCustomAdvanceDay(15)
        vm.addCustomAdvanceDay(15)
        assertTrue(15 in vm.uiState.value.advanceDays)
        assertEquals(2, vm.uiState.value.advanceDays.size) // [0, 15]
    }

    @Test
    fun `保存_新记录写入数据库`() {
        val vm = AddEditViewModel(app, db)
        vm.updateName("小明")
        vm.updateBirthMonth(8)
        vm.updateBirthDay(14)
        vm.updateNotes("喜欢打篮球")
        vm.save()
        vm.uiState.awaitValue({ it.saved }, "保存完成")

        val all = runBlocking { db.birthdayDao().getAllOnce() }
        assertEquals(1, all.size)
        val saved = all.first()
        assertEquals("小明", saved.name)
        assertEquals(8, saved.birthMonth)
        assertEquals(14, saved.birthDay)
        assertEquals("喜欢打篮球", saved.notes)
    }

    @Test
    fun `编辑_加载已有记录进入编辑模式`() {
        val id = runBlocking {
            db.birthdayDao().insert(
                Birthday(
                    name = "爷爷",
                    birthYear = 1945,
                    birthMonth = 7,
                    birthDay = 15,
                    calendarType = "lunar"
                )
            )
        }
        val vm = AddEditViewModel(app, db)
        vm.loadBirthday(id)
        vm.uiState.awaitValue({ it.isEditMode }, "编辑加载")
        assertEquals("爷爷", vm.uiState.value.name)
        assertEquals("lunar", vm.uiState.value.calendarType)
        assertEquals(id, vm.uiState.value.id)
    }
}
