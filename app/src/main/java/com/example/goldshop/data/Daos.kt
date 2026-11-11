package com.example.goldshop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: ProductEntity)

    @Delete
    suspend fun delete(p: ProductEntity)

    @Query("SELECT * FROM products ORDER BY name")
    fun all(): Flow<List<ProductEntity>>
}

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("SELECT * FROM invoices ORDER BY dateMillis DESC")
    fun all(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    fun items(id: Long): Flow<List<InvoiceItemEntity>>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id")
    suspend fun itemsOnce(id: Long): List<InvoiceItemEntity>

    @Query("DELETE FROM invoice_items WHERE invoiceId = :id")
    suspend fun deleteItems(id: Long)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface OldGoldDao {
    @Insert
    suspend fun insert(record: OldGoldEntity)

    @Delete
    suspend fun delete(record: OldGoldEntity)

    @Query("SELECT * FROM old_gold ORDER BY dateMillis DESC")
    fun all(): Flow<List<OldGoldEntity>>
}

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(txn: TransactionEntity)

    @Delete
    suspend fun delete(txn: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun all(): Flow<List<TransactionEntity>>
}
