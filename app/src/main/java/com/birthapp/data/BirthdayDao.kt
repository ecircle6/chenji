package com.birthapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {

    // 首页用这个：已暂停的记录仍要在列表里灰显出来，所以不过滤 isActive
    @Query("SELECT * FROM birthdays ORDER BY name")
    fun getAll(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE isActive = 1 ORDER BY name")
    fun getAllActive(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE id = :id")
    fun observeById(id: Long): Flow<Birthday?>

    @Query("SELECT * FROM birthdays WHERE isActive = 1 AND relation = :relation ORDER BY name")
    fun getByRelation(relation: String): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE id = :id")
    suspend fun getById(id: Long): Birthday?

    @Query("SELECT * FROM birthdays WHERE isActive = 1")
    suspend fun getAllActiveOnce(): List<Birthday>

    // 备份导出/导入判重用：一次性取全部记录，已暂停的也要带上
    @Query("SELECT * FROM birthdays")
    suspend fun getAllOnce(): List<Birthday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(birthday: Birthday): Long

    @Update
    suspend fun update(birthday: Birthday)

    @Query("UPDATE birthdays SET nextReminderDate = :date WHERE id = :id")
    suspend fun updateNextReminderDate(id: Long, date: String?)

    @Query("UPDATE birthdays SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean, updatedAt: Long)

    @Delete
    suspend fun delete(birthday: Birthday)

    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun deleteById(id: Long)
}
