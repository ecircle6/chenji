package com.birthapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// exportSchema = true：把 schema 版本产物（app/schemas/）纳入版本库，
// 配合 MigrationTestHelper 迁移测试与 CI 的 checkSchema 任务，schema 变更不再裸奔
@Database(entities = [Birthday::class], version = 2, exportSchema = true)
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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "birthapp.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
