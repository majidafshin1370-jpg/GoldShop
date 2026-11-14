package com.example.goldshop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProductEntity)

    @Delete
    suspend fun delete(p: ProductEntity)

    @Query("SELECT * FROM products ORDER BY name")
    fun all(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun allOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun byId(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :code AND barcode != '' LIMIT 1")
    suspend fun byBarcode(code: String): ProductEntity?

    @Query("UPDATE products SET quantity = :q WHERE id = :id")
    suspend fun setQuantity(id: Long, q: Int)
}

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices ORDER BY dateMillis DESC")
    fun all(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices ORDER BY dateMillis DESC")
    suspend fun allOnce(): List<InvoiceEntity>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    fun items(id: Long): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    suspend fun itemsOnce(id: Long): List<InvoiceItemEntity>

    @Query("DELETE FROM invoice_items WHERE invoiceId = :id")
    suspend fun deleteItems(id: Long)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun byId(id: Long): InvoiceEntity?
}

@Dao
interface OldGoldDao {
    @Insert
    suspend fun insert(record: OldGoldEntity)

    @Delete
    suspend fun delete(record: OldGoldEntity)

    @Query("SELECT * FROM old_gold ORDER BY dateMillis DESC")
    fun all(): Flow<List<OldGoldEntity>>

    @Query("SELECT * FROM old_gold ORDER BY dateMillis DESC")
    suspend fun allOnce(): List<OldGoldEntity>
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(txn: TransactionEntity)

    @Delete
    suspend fun delete(txn: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun all(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun allOnce(): List<TransactionEntity>
}

@Dao
interface ChequeDao {
    @Insert
    suspend fun insert(c: ChequeEntity): Long

    @Update
    suspend fun update(c: ChequeEntity)

    @Delete
    suspend fun delete(c: ChequeEntity)

    @Query("SELECT * FROM cheques ORDER BY dueMillis ASC")
    fun all(): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques ORDER BY dueMillis ASC")
    suspend fun allOnce(): List<ChequeEntity>
}

@Dao
interface DebtDao {
    @Insert
    suspend fun insert(d: DebtEntity): Long

    @Delete
    suspend fun delete(d: DebtEntity)

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId")
    suspend fun deletePayments(debtId: Long)

    @Insert
    suspend fun insertPayment(p: DebtPaymentEntity)

    @Query("SELECT * FROM debts ORDER BY dateMillis DESC")
    fun all(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY dateMillis DESC")
    fun allPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debts ORDER BY dateMillis DESC")
    suspend fun allOnce(): List<DebtEntity>

    @Query("SELECT * FROM debt_payments ORDER BY dateMillis DESC")
    suspend fun allPaymentsOnce(): List<DebtPaymentEntity>

    @Query("SELECT * FROM debts WHERE invoiceId = :invoiceId LIMIT 1")
    fun byInvoice(invoiceId: Long): Flow<DebtEntity?>

    @Query("SELECT * FROM debts WHERE invoiceId = :invoiceId LIMIT 1")
    suspend fun byInvoiceOnce(invoiceId: Long): DebtEntity?
}

@Dao
interface CustomerDao {
    @Insert
    suspend fun insert(c: CustomerEntity): Long

    @Update
    suspend fun update(c: CustomerEntity)

    @Delete
    suspend fun delete(c: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY name")
    fun all(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name")
    suspend fun allOnce(): List<CustomerEntity>
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(a: AuditEntity)

    @Query("SELECT * FROM audit_log ORDER BY timeMillis DESC LIMIT 600")
    fun recent(): Flow<List<AuditEntity>>
}
