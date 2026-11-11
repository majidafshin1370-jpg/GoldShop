package com.example.goldshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class, InvoiceEntity::class, InvoiceItemEntity::class,
        OldGoldEntity::class, TransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun oldGoldDao(): OldGoldDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "goldshop.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN customerPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `old_gold` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`customerName` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`weight` REAL NOT NULL, `karat` INTEGER NOT NULL, " +
                        "`lossPercent` REAL NOT NULL, `gramPrice` INTEGER NOT NULL, " +
                        "`pureWeight` REAL NOT NULL, `total` INTEGER NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `transactions` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                        "`amount` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`dateMillis` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
