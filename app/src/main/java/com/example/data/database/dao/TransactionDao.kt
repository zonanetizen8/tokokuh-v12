package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import com.example.data.database.entity.TransactionReturnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transactionNumber = :txNumber LIMIT 1")
    suspend fun getTransactionByNumber(txNumber: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    fun getTransactionsBetweenFlow(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getTransactionsBetween(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    // Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TransactionItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsByTransactionId(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsByTransactionIdFlow(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllTransactionItems(): List<TransactionItemEntity>

    @Update
    suspend fun updateItem(item: TransactionItemEntity)

    // Returns
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnEntity: TransactionReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturns(returns: List<TransactionReturnEntity>)

    @Query("SELECT * FROM transaction_returns ORDER BY createdAt DESC")
    fun getAllReturnsFlow(): Flow<List<TransactionReturnEntity>>

    @Query("SELECT * FROM transaction_returns WHERE transactionId = :transactionId ORDER BY createdAt DESC")
    suspend fun getReturnsByTransactionId(transactionId: Long): List<TransactionReturnEntity>

    @Query("SELECT * FROM transaction_returns")
    suspend fun getAllReturns(): List<TransactionReturnEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun clearItems()

    @Query("DELETE FROM transaction_returns")
    suspend fun clearReturns()
}
