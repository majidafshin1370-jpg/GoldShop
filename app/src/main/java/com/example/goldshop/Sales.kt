package com.example.goldshop

import android.content.Context
import androidx.room.withTransaction
import com.example.goldshop.data.AppDatabase
import com.example.goldshop.data.DebtEntity
import com.example.goldshop.data.InvoiceEntity
import com.example.goldshop.data.InvoiceItemEntity
import com.example.goldshop.data.LEDGER_PREFIX
import com.example.goldshop.data.TransactionEntity

data class DraftLine(
    val productId: Long,
    val name: String,
    val weight: Double,
    val karat: Int,
    val gramPrice: Long,
    val feePerGram: Long
) {
    val lineTotal: Long get() = ((gramPrice + feePerGram) * weight).toLong()
    val lineFee: Long get() = (feePerGram * weight).toLong()
    val lineSubtotal: Long get() = (gramPrice * weight).toLong()
}

data class InvoiceTotals(val subtotal: Long, val feeTotal: Long, val tax: Long, val total: Long)

object Sales {

    fun totals(lines: List<DraftLine>, taxPercent: Double): InvoiceTotals {
        val subtotal = lines.sumOf { it.lineSubtotal }
        val fee = lines.sumOf { it.lineFee }
        val tax = ((subtotal + fee) * taxPercent / 100).toLong()
        return InvoiceTotals(subtotal, fee, tax, subtotal + fee + tax)
    }

    // صدور فاکتور: ثبت اقلام، کم کردن موجودی، و ساخت طلب مشتری برای مبلغ نسیه
    suspend fun createInvoice(
        context: Context,
        customer: String,
        phone: String,
        lines: List<DraftLine>,
        taxPercent: Double,
        cashPaid: Long,
        dueMillis: Long,
        cashToRegister: Boolean
    ): Long {
        val db = AppDatabase.get(context)
        val t = totals(lines, taxPercent)
        val paid = cashPaid.coerceIn(0L, t.total)
        val remaining = t.total - paid
        val now = System.currentTimeMillis()

        val invId = db.withTransaction {
            val id = db.invoiceDao().insert(
                InvoiceEntity(
                    customerName = customer.trim(),
                    customerPhone = phone.trim(),
                    dateMillis = now,
                    itemCount = lines.size,
                    subtotal = t.subtotal,
                    feeTotal = t.feeTotal,
                    taxTotal = t.tax,
                    total = t.total,
                    paidAmount = paid
                )
            )
            db.invoiceDao().insertItems(
                lines.map {
                    InvoiceItemEntity(
                        invoiceId = id,
                        productName = it.name,
                        weight = it.weight,
                        karat = it.karat,
                        gramPrice = it.gramPrice,
                        feePerGram = it.feePerGram,
                        lineTotal = it.lineTotal,
                        productId = it.productId
                    )
                }
            )
            // کم شدن خودکار موجودی
            val counts = lines.filter { it.productId > 0 }.groupingBy { it.productId }.eachCount()
            for ((pid, cnt) in counts) {
                val p = db.productDao().byId(pid)
                if (p != null) db.productDao().setQuantity(pid, (p.quantity - cnt).coerceAtLeast(0))
            }
            if (remaining > 0) {
                db.debtDao().insert(
                    DebtEntity(
                        direction = "receivable",
                        partyName = customer.trim(),
                        phone = phone.trim(),
                        amount = remaining,
                        note = "فاکتور شماره $id",
                        dateMillis = now,
                        dueMillis = dueMillis,
                        invoiceId = id
                    )
                )
            }
            if (paid > 0 && cashToRegister) {
                db.transactionDao().insert(
                    TransactionEntity(
                        type = "income",
                        category = LEDGER_PREFIX + "دریافت نقد فاکتور " + id,
                        amount = paid,
                        note = customer.trim(),
                        dateMillis = now
                    )
                )
            }
            id
        }
        CustomerStore.ensure(db, customer, phone)
        Audit.log(
            context, "فاکتور", "ثبت",
            "فاکتور $invId برای ${customer.trim()} — مبلغ ${t.total}" +
                if (remaining > 0) " (نسیه: $remaining)" else ""
        )
        return invId
    }

    // حذف فاکتور: برگرداندن موجودی و حذف طلب مرتبط (با اقساطش)
    suspend fun deleteInvoice(context: Context, invoice: InvoiceEntity) {
        val db = AppDatabase.get(context)
        db.withTransaction {
            val items = db.invoiceDao().itemsOnce(invoice.id)
            val counts = items.filter { it.productId > 0 }.groupingBy { it.productId }.eachCount()
            for ((pid, cnt) in counts) {
                val p = db.productDao().byId(pid)
                if (p != null) db.productDao().setQuantity(pid, p.quantity + cnt)
            }
            val debt = db.debtDao().byInvoiceOnce(invoice.id)
            if (debt != null) {
                db.debtDao().deletePayments(debt.id)
                db.debtDao().delete(debt)
            }
            db.invoiceDao().deleteItems(invoice.id)
            db.invoiceDao().delete(invoice.id)
        }
        Audit.log(context, "فاکتور", "حذف", "فاکتور ${invoice.id} مشتری ${invoice.customerName} — مبلغ ${invoice.total}")
    }
}
