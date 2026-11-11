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
    val imageUri: String? = null
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
    val total: Long
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
    val lineTotal: Long
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
