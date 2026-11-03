package com.example.coffeestock.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.coffeestock.data.local.dao.MovementDao
import com.example.coffeestock.data.local.dao.ProductDao
import com.example.coffeestock.data.local.dao.SyncDao
import com.example.coffeestock.data.local.entity.ConflictEntity
import com.example.coffeestock.data.local.entity.MovementEntity
import com.example.coffeestock.data.local.entity.ProductEntity
import com.example.coffeestock.data.local.entity.SyncMetaEntity

@Database(
    entities = [
        ProductEntity::class,
        MovementEntity::class,
        ConflictEntity::class,
        SyncMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun movementDao(): MovementDao

    abstract fun syncDao(): SyncDao

    companion object {

        // Phiên bản 2 không đổi cấu trúc bảng; migration này giữ nguyên dữ liệu
        // của database phiên bản 1 đã cài trên thiết bị.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes in version 2.
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coffeestock_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}