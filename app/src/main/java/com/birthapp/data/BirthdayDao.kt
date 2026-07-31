package com.birthapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {

    @Query("SELECT * FROM birthdays WHERE isActive = 1 ORDER BY name")
    fun getAllActive(): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE isActive = 1 AND relation = :relation ORDER BY name")
    fun getByRelation(relation: String): Flow<List<Birthday>>

    @Query("SELECT * FROM birthdays WHERE id = :id")
    suspend fun getById(id: Long): Birthday?

    @Query("SELECT * FROM birthdays WHERE isActive = 1")
    suspend fun getAllActiveOnce(): List<Birthday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(birthday: Birthday): Long

    @Update
    suspend fun update(birthday: Birthday)

    @Query("UPDATE birthdays SET nextReminderDate = :date WHERE id = :id")
    suspend fun updateNextReminderDate(id: Long, date: String?)

    @Delete
    suspend fun delete(birthday: Birthday)

    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun deleteById(id: Long)
}
