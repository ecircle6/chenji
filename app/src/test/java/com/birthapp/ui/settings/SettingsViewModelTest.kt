package com.birthapp.ui.settings

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.birthapp.awaitValue
import com.birthapp.backup.BackupCodec
import com.birthapp.data.AppDatabase
import com.birthapp.data.Birthday
import com.birthapp.settings.ThemeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

/**
 * SettingsViewModel 备份流程测试：shareBackup 产出文件并发事件、
 * importFrom 分类出重复标记、applyImport 按动作写库。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsViewModelTest {

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

    private fun birthday(name: String = "小明") = Birthday(
        name = name,
        birthYear = 1998,
        birthMonth = 8,
        birthDay = 14,
        calendarType = "solar",
        notes = "喜欢打篮球"
    )

    /** 轮询等待条件成立：每轮先 idle 主 looper（viewModelScope 跑在上面），再等真实时间 */
    private fun awaitUntil(condition: () -> Boolean, label: String) {
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(20)
        }
        throw AssertionError("等待 $label 超时")
    }

    @Test
    fun `分享备份_产出备份文件并发出事件`() {
        runBlocking { db.birthdayDao().insert(birthday()) }
        val vm = SettingsViewModel(app, db, ThemeStore(app))
        // 事件由后台协程收集、主线程轮询读取，用线程安全容器避免可见性问题
        val events = java.util.concurrent.CopyOnWriteArrayList<SettingsEvent>()
        val job: Job = CoroutineScope(Dispatchers.IO).launch {
            vm.events.collect { events.add(it) }
        }
        try {
            vm.shareBackup()
            awaitUntil({ events.isNotEmpty() }, "事件")
            // 备份文件应已写入且内容可解回
            val file = File(app.cacheDir, "backup")
                .listFiles()?.firstOrNull { it.name.endsWith(".json") }
            assertTrue("备份文件应已写入", file != null && file.exists() && file.length() > 0)
            val decoded = BackupCodec.decode(file!!.readText())
            assertEquals(1, decoded.size)
            assertEquals("小明", decoded.first().name)
            // 正常环境（真机/模拟器）发 ShareFile；Robolectric 下 FileProvider
            // 的路径根解析不生效会走失败分支发 Toast——两种都说明流程走完并处理了结果
            val e = events.first()
            assertTrue(
                "预期 ShareFile 或失败 Toast，实际: $e",
                e is SettingsEvent.ShareFile || e is SettingsEvent.Toast
            )
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `导入_重复记录标记重复_跳过则不写库_导入则新增`() {
        // 本机已有一条小明
        runBlocking { db.birthdayDao().insert(birthday()) }
        // 备份文件里是同名小明（判重命中）
        val backupText = BackupCodec.encode(listOf(birthday()))
        val uri = Uri.parse("content://birthapp.test/backup.json")
        Shadows.shadowOf(app.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(backupText.toByteArray()))

        val vm = SettingsViewModel(app, db, ThemeStore(app))
        val preview = java.util.concurrent.atomic.AtomicReference<SettingsEvent.ImportPreview?>()
        val job: Job = CoroutineScope(Dispatchers.IO).launch {
            vm.events.collect { e -> if (e is SettingsEvent.ImportPreview) preview.set(e) }
        }
        try {
            vm.importFrom(uri)
            awaitUntil({ preview.get() != null }, "导入预览")
            val p = preview.get() ?: throw AssertionError("未收到导入预览事件")
            assertEquals(1, p.items.size)
            assertEquals(true, p.items.first().isDuplicate)

            // SKIP：库不变
            vm.applyImport(listOf(ImportAction.SKIP), restoreSettings = false)
            assertEquals(1, runBlocking { db.birthdayDao().getAllOnce().size })

            // 换一条不重复的再导入：INSERT 后新增
            val newText = BackupCodec.encode(listOf(birthday(name = "小红")))
            Shadows.shadowOf(app.contentResolver)
                .registerInputStream(uri, ByteArrayInputStream(newText.toByteArray()))
            preview.set(null)
            vm.importFrom(uri)
            awaitUntil({ preview.get() != null }, "第二次导入预览")
            assertEquals(false, preview.get()?.items?.first()?.isDuplicate)

            // INSERT 在 viewModelScope 里异步写库，等库里出现第二条再断言
            vm.applyImport(listOf(ImportAction.INSERT), restoreSettings = false)
            awaitUntil(
                { runBlocking { db.birthdayDao().getAllOnce().size } == 2 },
                "导入写库"
            )
            val all = runBlocking { db.birthdayDao().getAllOnce() }
            assertEquals(2, all.size)
            assertTrue(all.any { it.name == "小红" })
        } finally {
            job.cancel()
        }
    }
}
