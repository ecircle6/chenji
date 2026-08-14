package com.birthapp.data

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Room 迁移测试：用真实 SQLite（Robolectric 在 JVM 上提供）从 1.json 的建表 SQL
 * 建 v1 库并插入老数据，然后通过 Room.databaseBuilder 打开（version 2）——
 * Room 会执行 MIGRATION_1_2 并在打开时校验 schema 与当前实体一致，不一致直接抛异常。
 * 这套校验路径和生产环境完全一致（RoomOpenHelper.validateMigration）。
 *
 * 不用 MigrationTestHelper：Robolectric 不合并测试源集 assets，而 schema JSON
 * 读的是磁盘文件（src/test/assets），普通文件 IO 即可。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun v1迁移到v2_老数据完整保留且eventType默认birthday() {
        createV1Database("migration-test.db") { db ->
            db.execSQL(
                "INSERT INTO birthdays (name, birthYear, birthMonth, birthDay, calendarType," +
                    " isLeapMonth, advanceDays, reminderHour, reminderMinute, relation," +
                    " notes, isActive, nextReminderDate, createdAt, updatedAt)" +
                    " VALUES ('爷爷', 1950, 6, 15, 'lunar', 0, 3, 8, 0, 'family', '老数据'," +
                    " 1, '2026-02-02', 1000, 2000)"
            )
        }

        // 打开即触发 MIGRATION_1_2 + schema 校验；迁移写错这里会直接抛异常
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-test.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        roomDb.openHelper.writableDatabase.query("SELECT * FROM birthdays").use { cursor ->
            assertTrue("迁移后应有 1 条记录", cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("爷爷", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("lunar", cursor.getString(cursor.getColumnIndexOrThrow("calendarType")))
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("advanceDays")))
            assertEquals("2026-02-02", cursor.getString(cursor.getColumnIndexOrThrow("nextReminderDate")))
            // 新增列按迁移脚本默认值填充
            assertEquals("birthday", cursor.getString(cursor.getColumnIndexOrThrow("eventType")))
        }
        roomDb.close()
    }

    @Test
    fun v1空库迁移到v2_表结构校验通过() {
        createV1Database("migration-empty.db")

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-empty.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        roomDb.openHelper.writableDatabase.query("SELECT COUNT(*) FROM birthdays").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        roomDb.close()
    }

    /**
     * 按 1.json 里的建表 SQL 创建 v1 数据库（每次全新，避免残留旧库影响校验）
     */
    private fun createV1Database(name: String, insert: (SupportSQLiteDatabase) -> Unit = {}) {
        context.getDatabasePath(name).delete()

        // Room schema 的 createSql 里表名是 ${TABLE_NAME} 占位符，替换成真实表名
        val createSql = JSONObject(File("src/test/assets/com.birthapp.data.AppDatabase/1.json").readText())
            .getJSONObject("database")
            .getJSONArray("entities")
            .getJSONObject(0)
            .getString("createSql")
            .replace("\${TABLE_NAME}", "birthdays")

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                // 版本由 Callback(1) 携带
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(createSql)
                        insert(db)
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int
                    ) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.use { }
    }
}
