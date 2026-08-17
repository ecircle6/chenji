package com.birthapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// exportSchema = true：把 schema 版本产物（app/src/test/assets/）纳入版本库，
// 配合 MigrationTest 迁移测试，schema 变更不再裸奔
@Database(entities = [Birthday::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun birthdayDao(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2：新增 eventType 字段以支持纪念日类型。
         * 用 ALTER TABLE 增量升级，老数据全部保留并默认为生日。
         * 这里绝不能用 fallbackToDestructiveMigration，那会清空用户所有记录。
         * internal：供 MigrationTest 用真实 SQLite 验证迁移正确性。
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE birthdays ADD COLUMN eventType TEXT NOT NULL DEFAULT '${EventType.BIRTHDAY}'"
                )
            }
        }

        /**
         * v2 -> v3：多级提前提醒 + 置顶。
         * advanceDays 从 INTEGER（单值）变 TEXT（逗号分隔列表），SQLite 不能改列类型，
         * 只能重建表：新表把旧值 CAST 成 TEXT（3 -> "3"，语义不变），顺带加 pinned 列。
         * 12 步迁移：建新表 -> 拷数据 -> 删旧表 -> 改名。
         */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `birthdays_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `birthYear` INTEGER NOT NULL,
                        `birthMonth` INTEGER NOT NULL,
                        `birthDay` INTEGER NOT NULL,
                        `calendarType` TEXT NOT NULL,
                        `isLeapMonth` INTEGER NOT NULL,
                        `advanceDays` TEXT NOT NULL,
                        `reminderHour` INTEGER NOT NULL,
                        `reminderMinute` INTEGER NOT NULL,
                        `relation` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL DEFAULT '${EventType.BIRTHDAY}',
                        `notes` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `pinned` INTEGER NOT NULL DEFAULT 0,
                        `nextReminderDate` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `birthdays_new` (
                        `id`, `name`, `birthYear`, `birthMonth`, `birthDay`, `calendarType`,
                        `isLeapMonth`, `advanceDays`, `reminderHour`, `reminderMinute`, `relation`,
                        `eventType`, `notes`, `isActive`, `pinned`, `nextReminderDate`,
                        `createdAt`, `updatedAt`
                    )
                    SELECT
                        `id`, `name`, `birthYear`, `birthMonth`, `birthDay`, `calendarType`,
                        `isLeapMonth`, CAST(`advanceDays` AS TEXT), `reminderHour`, `reminderMinute`,
                        `relation`, `eventType`, `notes`, `isActive`, 0, `nextReminderDate`,
                        `createdAt`, `updatedAt`
                    FROM `birthdays`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `birthdays`")
                db.execSQL("ALTER TABLE `birthdays_new` RENAME TO `birthdays`")
            }
        }

        /**
         * v3 -> v4：新增 emoji 列（每条记录的专属 Emoji 头像）。
         * 增量升级，空字符串默认值 = 自动（生日→姓名首字、其他→类型 emoji）。
         */
        internal val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE birthdays ADD COLUMN emoji TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthapp.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
