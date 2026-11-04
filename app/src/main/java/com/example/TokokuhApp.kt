package com.example

import android.app.Application
import com.example.data.database.TokokuhDatabase
import com.example.data.database.entity.CategoryEntity
import com.example.data.database.entity.StoreSettingsEntity
import com.example.data.repository.TokokuhRepository
import com.example.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TokokuhApp : Application() {
    val database: TokokuhDatabase by lazy { TokokuhDatabase.getInstance(this) }
    val repository: TokokuhRepository by lazy { TokokuhRepository(this, database) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        // Seed initial default configuration and categories if brand new
        applicationScope.launch {
            seedInitialDataIfNeeded()
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val settings = database.storeSettingsDao().getSettings()
        if (settings == null) {
            database.storeSettingsDao().insertOrUpdateSettings(
                StoreSettingsEntity(
                    id = 1,
                    storeName = "",
                    storeAddress = "",
                    storePhone = "",
                    storeWebsite = "",
                    receiptHeaderMessage = "",
                    receiptFooterMessage = "Terima Kasih Atas Kunjungan Anda",
                    selectedCashierName = ""
                )
            )
        }

        val categories = database.categoryDao().getAllCategories()
        if (categories.isEmpty()) {
            database.categoryDao().insertCategories(
                listOf(
                    CategoryEntity(name = "Makanan", iconName = "restaurant", colorHex = "#FF9800"),
                    CategoryEntity(name = "Minuman", iconName = "local_cafe", colorHex = "#03A9F4"),
                    CategoryEntity(name = "Sembako", iconName = "shopping_basket", colorHex = "#4CAF50"),
                    CategoryEntity(name = "Snack & Cemilan", iconName = "fastfood", colorHex = "#E91E63"),
                    CategoryEntity(name = "ATK & Lainnya", iconName = "category", colorHex = "#9C27B0")
                )
            )
        }

    }
}
