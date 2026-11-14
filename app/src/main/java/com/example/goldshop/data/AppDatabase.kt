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
        ChequeEntity::class, DebtEntity::class, DebtPaymentEntity::class,
        CustomerEntity::class, AuditEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun oldGoldDao(): OldGoldDao
    abstract fun transactionDao(): TransactionDao
    abstract fun chequeDao(): ChequeDao
    abstract fun debtDao(): DebtDao
    abstract fun customerDao(): CustomerDao
    abstract fun auditDao(): AuditDao

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

        // نسخه ۵: موجودی انبار، بارکد، فروش نسیه (پیوند فاکتور به طلب)، سررسید قسط،
        // پرونده مشتری و گزارش تغییرات. داده‌های قبلی کاملاً حفظ می‌شوند.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN minQuantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE invoices ADD COLUMN paidAmount INTEGER NOT NULL DEFAULT 0")
                // فاکتورهای قدیمی نقدی فرض می‌شوند
                db.execSQL("UPDATE invoices SET paidAmount = total")

                db.execSQL("ALTER TABLE invoice_items ADD COLUMN productId INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE debts ADD COLUMN dueMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debts ADD COLUMN invoiceId INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `customers` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `phone` TEXT NOT NULL, " +
                        "`birthMillis` INTEGER NOT NULL, `note` TEXT NOT NULL, " +
                        "`createdMillis` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `audit_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timeMillis` INTEGER NOT NULL, `actor` TEXT NOT NULL, " +
                        "`section` TEXT NOT NULL, `action` TEXT NOT NULL, " +
                        "`details` TEXT NOT NULL)"
                )

                // ساخت خودکار پرونده برای مشتریان موجود در فاکتور، کهنه، طلب/بدهی و چک
                db.execSQL(
                    "INSERT INTO customers (name, phone, birthMillis, note, createdMillis) " +
                        "SELECT customerName, MAX(customerPhone), 0, '', MIN(dateMillis) FROM invoices " +
                        "WHERE TRIM(customerName) != '' GROUP BY customerName"
                )
                db.execSQL(
                    "INSERT INTO customers (name, phone, birthMillis, note, createdMillis) " +
                        "SELECT customerName, MAX(phone), 0, '', MIN(dateMillis) FROM old_gold " +
                        "WHERE TRIM(customerName) != '' AND customerName NOT IN (SELECT name FROM customers) " +
                        "GROUP BY customerName"
                )
                db.execSQL(
                    "INSERT INTO customers (name, phone, birthMillis, note, createdMillis) " +
                        "SELECT partyName, MAX(phone), 0, '', MIN(dateMillis) FROM debts " +
                        "WHERE TRIM(partyName) != '' AND partyName NOT IN (SELECT name FROM customers) " +
                        "GROUP BY partyName"
                )
                db.execSQL(
                    "INSERT INTO customers (name, phone, birthMillis, note, createdMillis) " +
                        "SELECT partyName, MAX(phone), 0, '', MIN(createdMillis) FROM cheques " +
                        "WHERE TRIM(partyName) != '' AND partyName NOT IN (SELECT name FROM customers) " +
                        "GROUP BY partyName"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    // فقط برای کاهش نسخه؛ خطای مهاجرت هرگز بی‌صدا داده‌ها را پاک نمی‌کند
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }

        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
