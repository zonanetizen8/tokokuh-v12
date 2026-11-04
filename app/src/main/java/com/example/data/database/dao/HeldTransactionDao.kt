package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.HeldTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeldTransactionDao {
    @Query("SELECT * FROM held_transactions ORDER BY createdAt DESC")
    fun getAllHeldFlow(): Flow<List<HeldTransactionEntity>>

    @Query("SELECT * FROM held_transactions WHERE id = :id LIMIT 1")
    suspend fun getHeldById(id: Long): HeldTransactionEntity?

    @Query("SELECT * FROM held_transactions")
    suspend fun getAllHeld(): List<HeldTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeld(held: HeldTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHeld(heldList: List<HeldTransactionEntity>)

    @Query("DELETE FROM held_transactions WHERE id = :id")
    suspend fun deleteHeld(id: Long)

    @Query("DELETE FROM held_transactions")
    suspend fun clearAll()
}
