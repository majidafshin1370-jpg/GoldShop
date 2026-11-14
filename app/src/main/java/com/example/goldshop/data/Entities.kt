package com.example.goldshop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weight: Double,
    val karat: Int,
    val feeTomanPerGram: Long,
    val price: Long,
    val imageUri: String? = null,
    val quantity: Int = 1,          // موجودی (تعداد) در انبار
    val minQuantity: Int = 0,       // حد هشدار کم‌بودن موجودی
    val barcode: String = ""        // بارکد / QR محصول
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerPhone: String = "",
    val dateMillis: Long,
    val itemCount: Int,
    val subtotal: Long,
    val feeTotal: Long,
    val taxTotal: Long,
    val total: Long,
    val paidAmount: Long = 0L       // مبلغ نقد دریافتی هنگام صدور؛ باقی‌مانده = طلب مشتری
)

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productName: String,
    val weight: Double,
    val karat: Int,
    val gramPrice: Long,
    val feePerGram: Long,
    val lineTotal: Long,
    val productId: Long = 0L        // برای کم کردن/برگرداندن موجودی انبار
)

// خرید طلای کهنه از مشتری
@Entity(tableName = "old_gold")
data class OldGoldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phone: String,
    val weight: Double,
    val karat: Int,
    val lossPercent: Double,
    val gramPrice: Long,
    val pureWeight: Double,
    val total: Long,
    val dateMillis: Long
)

// تراکنش صندوق: هزینه یا درآمد
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // "income" یا "expense"
    val category: String,
    val amount: Long,
    val note: String,
    val dateMillis: Long
)

// چک دریافتی یا پرداختی
@Entity(tableName = "cheques")
data class ChequeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,          // "receivable" = دریافتی | "payable" = پرداختی
    val partyName: String,
    val phone: String = "",
    val chequeNumber: String = "",
    val bank: String = "",
    val amount: Long,
    val dueMillis: Long,            // تاریخ سررسید
    val status: String = "pending", // pending | cashed | bounced
    val note: String = "",
    val createdMillis: Long,
    val settledMillis: Long = 0L
)

// طلب (مشتری به ما بدهکار است) یا بدهی (ما به مشتری بدهکاریم)
@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val direction: String,          // "receivable" = طلب از مشتری | "payable" = بدهی به مشتری
    val partyName: String,
    val phone: String = "",
    val amount: Long,
    val note: String = "",
    val dateMillis: Long,
    val dueMillis: Long = 0L,       // سررسید قسط/تسویه (۰ = ندارد)
    val invoiceId: Long = 0L        // اگر از فروش نسیه ساخته شده، شماره فاکتور
)

// پرداخت/دریافت‌های جزئی هر طلب یا بدهی
@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val note: String = "",
    val dateMillis: Long
)

// تراکنش‌های صندوق که از چک/طلب ساخته می‌شوند با این پیشوند ثبت می‌شوند
// تا در «سود تقریبی» دوباره‌شماری نشوند (فروش قبلاً در فاکتور حساب شده است)
const val LEDGER_PREFIX = "حساب: "

fun TransactionEntity.isLedger(): Boolean = category.startsWith(LEDGER_PREFIX)

// پرونده مشتری
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val birthMillis: Long = 0L,     // تاریخ تولد (۰ = ثبت نشده)
    val note: String = "",
    val createdMillis: Long
)

// گزارش تغییرات: چه کسی، چه چیزی را، کی تغییر داد
@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val actor: String,
    val section: String,
    val action: String,
    val details: String
)
