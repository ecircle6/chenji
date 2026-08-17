package com.birthapp.ui.calendar

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
 * 日历页 ViewModel：全量记录（含暂停），不做任何筛选。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CalendarViewModelTest {

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
        eventType: String = EventType.BIRTHDAY,
        isActive: Boolean = true
    ) = Birthday(
        name = name,
        birthYear = 1998,
        birthMonth = 8,
        birthDay = 14,
        calendarType = "solar",
        relation = "family",
        eventType = eventType,
        isActive = isActive
    )

    @Test
    fun `全量记录_包含暂停与各种类型_不做筛选`() {
        runBlocking {
            db.birthdayDao().insert(birthday("小明"))
            db.birthdayDao().insert(birthday("爷爷", eventType = EventType.MEMORIAL, isActive = false))
        }
        val vm = CalendarViewModel(app, db)
        val list = vm.allBirthdays.awaitValue({ it.size == 2 }, "全量加载")
        assertEquals(2, list.size)
        assertTrue("暂停记录也在全量里", list.any { it.name == "爷爷" && !it.isActive })
    }
}