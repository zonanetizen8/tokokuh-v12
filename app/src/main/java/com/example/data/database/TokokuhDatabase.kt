package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.AuditLogDao
import com.example.data.database.dao.CategoryDao
import com.example.data.database.dao.EmployeeDao
import com.example.data.database.dao.ExpenseDao
import com.example.data.database.dao.HeldTransactionDao
import com.example.data.database.dao.ProductDao
import com.example.data.database.dao.ShiftDao
import com.example.data.database.dao.StockMovementDao
import com.example.data.database.dao.StoreSettingsDao
import com.example.data.database.dao.TransactionDao
import com.example.data.database.entity.AuditLogEntity
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.EmployeeEntity
import com.example.data.database.entity.ExpenseEntity
import com.example.data.database.entity.HeldTransactionEntity
import com.example.data.database.entity.ProductEntity
import com.example.data.database.entity.ShiftEntity
import com.example.data.database.entity.StockMovementEntity
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.database.entity.TransactionEntity
import com.example.data.database.entity.TransactionItemEntity
import com.example.data.database.entity.TransactionReturnEntity

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        TransactionReturnEntity::class,
        ShiftEntity::class,
        ExpenseEntity::class,
        EmployeeEntity::class,
        StockMovementEntity::class,
        AuditLogEntity::class,
        HeldTransactionEntity::class,
        StoreSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TokokuhDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun shiftDao(): ShiftDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun heldTransactionDao(): HeldTransactionDao
    abstract fun storeSettingsDao(): StoreSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: TokokuhDatabase? = null

        fun getInstance(context: Context): TokokuhDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TokokuhDatabase::class.java,
                    "tokokuh_pos.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
