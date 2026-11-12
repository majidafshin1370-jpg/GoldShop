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
        OldGoldEntity::class, TransactionEntity::class,
        ChequeEntity::class, DebtEntity::class, DebtPaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun oldGoldDao(): OldGoldDao
    abstract fun transactionDao(): TransactionDao
    abstract fun chequeDao(): ChequeDao
    abstract fun debtDao(): DebtDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cheques` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`direction` TEXT NOT NULL, `partyName` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `chequeNumber` TEXT NOT NULL, " +
                        "`bank` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`dueMillis` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, `createdMillis` INTEGER NOT NULL, " +
                        "`settledMillis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`direction` TEXT NOT NULL, `partyName` TEXT NOT NULL, " +
                        "`phone` TEXT NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debt_payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`debtId` INTEGER NOT NULL, `amount` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `dateMillis` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
