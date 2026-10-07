package com.app.easyremind.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {

    @Query("SELECT * FROM classes ORDER BY startMin ASC")
    fun observeAll(): Flow<List<ClassEntity>>

    @Query("SELECT * FROM classes ORDER BY startMin ASC")
    suspend fun getAll(): List<ClassEntity>

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ClassEntity?

    @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ClassEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cl: ClassEntity): Long

    @Update
    suspend fun update(cl: ClassEntity)

    @Delete
    suspend fun delete(cl: ClassEntity)

    @Query("DELETE FROM classes")
    suspend fun clearAll()

    @Query("DELETE FROM classes WHERE id = :id")
    suspend fun deleteById(id: Long)
}