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
 * Room 迁移测试：用真实 SQLite（Robolectric 在 JVM 上提供）从历史版本的
 * schema JSON 建库并插入老数据，然后通过 Room.databaseBuilder 打开（当前版本 3）——
 * Room 会执行迁移链并在打开时校验 schema 与当前实体一致，不一致直接抛异常。
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
    fun v1迁移到v3_老数据完整且eventType默认birthday() {
        createDatabase("migration-v1.db", 1) { db ->
            db.execSQL(
                "INSERT INTO birthdays (name, birthYear, birthMonth, birthDay, calendarType," +
                    " isLeapMonth, advanceDays, reminderHour, reminderMinute, relation," +
                    " notes, isActive, nextReminderDate, createdAt, updatedAt)" +
                    " VALUES ('爷爷', 1950, 6, 15, 'lunar', 0, 3, 8, 0, 'family', '老数据'," +
                    " 1, '2026-02-02', 1000, 2000)"
            )
        }

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-v1.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        roomDb.openHelper.writableDatabase.query("SELECT * FROM birthdays").use { cursor ->
            assertTrue("迁移后应有 1 条记录", cursor.moveToFirst())
            assertEquals(1, cursor.count)
            assertEquals("爷爷", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("lunar", cursor.getString(cursor.getColumnIndexOrThrow("calendarType")))
            assertEquals("2026-02-02", cursor.getString(cursor.getColumnIndexOrThrow("nextReminderDate")))
            // v1 -> v2 新增列按迁移脚本默认值填充
            assertEquals("birthday", cursor.getString(cursor.getColumnIndexOrThrow("eventType")))
            // v2 -> v3：INTEGER 单值 3 转成 TEXT 多级列表 "3"，语义不变
            assertEquals("3", cursor.getString(cursor.getColumnIndexOrThrow("advanceDays")))
            // v3 新增置顶列默认 0
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("pinned")))
            // v4 新增 emoji 列默认空字符串（自动头像）
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("emoji")))
        }
        roomDb.close()
    }

    @Test
    fun v2迁移到v3_老数据完整且pinned默认0() {
        createDatabase("migration-v2.db", 2) { db ->
            db.execSQL(
                "INSERT INTO birthdays (name, birthYear, birthMonth, birthDay, calendarType," +
                    " isLeapMonth, advanceDays, reminderHour, reminderMinute, relation, eventType," +
                    " notes, isActive, nextReminderDate, createdAt, updatedAt)" +
                    " VALUES ('小明', 1998, 8, 14, 'solar', 0, 3, 8, 0, 'friend', 'birthday'," +
                    " '多级迁移验证', 1, NULL, 1000, 1000)"
            )
        }

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-v2.db")
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()

        roomDb.openHelper.writableDatabase.query("SELECT * FROM birthdays").use { cursor ->
            assertTrue("迁移后应有 1 条记录", cursor.moveToFirst())
            assertEquals("小明", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("birthday", cursor.getString(cursor.getColumnIndexOrThrow("eventType")))
            assertEquals("多级迁移验证", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
            // 旧单值 3 原样转成 TEXT "3"（多级列表 [3] 的存储形态）
            assertEquals("3", cursor.getString(cursor.getColumnIndexOrThrow("advanceDays")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("pinned")))
            // v4 新增 emoji 列默认空字符串
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("emoji")))
        }
        roomDb.close()
    }

    @Test
    fun v1空库迁移到v3_表结构校验通过() {
        createDatabase("migration-empty.db", 1)
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-empty.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
        roomDb.openHelper.writableDatabase.query("SELECT COUNT(*) FROM birthdays").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        roomDb.close()
    }

    @Test
    fun v3迁移到v4_emoji列默认空字符串_老数据完整() {
        createDatabase("migration-v3.db", 3) { db ->
            db.execSQL(
                "INSERT INTO birthdays (name, birthYear, birthMonth, birthDay, calendarType," +
                    " isLeapMonth, advanceDays, reminderHour, reminderMinute, relation, eventType," +
                    " notes, isActive, pinned, nextReminderDate, createdAt, updatedAt)" +
                    " VALUES ('小红', 2001, 7, 15, 'solar', 0, '0,3', 8, 0, 'friend', 'birthday'," +
                    " 'v3数据校验', 1, 1, NULL, 1000, 1000)"
            )
        }

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, "migration-v3.db")
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .build()

        roomDb.openHelper.writableDatabase.query("SELECT * FROM birthdays").use { cursor ->
            assertTrue("迁移后应有 1 条记录", cursor.moveToFirst())
            assertEquals("小红", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("v3数据校验", cursor.getString(cursor.getColumnIndexOrThrow("notes")))
            // v3 的老字段原样保留
            assertEquals("0,3", cursor.getString(cursor.getColumnIndexOrThrow("advanceDays")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("pinned")))
            // v4 新增 emoji 列默认空字符串（自动头像）
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("emoji")))
        }
        roomDb.close()
    }

    /**
     * 按指定版本 schema JSON 里的建表 SQL 建库（每次全新，避免残留旧库影响校验）
     */
    private fun createDatabase(name: String, version: Int, insert: (SupportSQLiteDatabase) -> Unit = {}) {
        context.getDatabasePath(name).delete()

        // Room schema 的 createSql 里表名是 ${TABLE_NAME} 占位符，替换成真实表名
        val createSql = JSONObject(
            File("src/test/assets/com.birthapp.data.AppDatabase/$version.json").readText()
        )
            .getJSONObject("database")
            .getJSONArray("entities")
            .getJSONObject(0)
            .getString("createSql")
            .replace("\${TABLE_NAME}", "birthdays")

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                // 版本由 Callback(version) 携带
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
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
