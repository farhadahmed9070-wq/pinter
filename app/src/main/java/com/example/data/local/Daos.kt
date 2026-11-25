package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintOrderDao {
  @Query("SELECT * FROM print_orders ORDER BY createdAt DESC")
  fun observeAllOrders(): Flow<List<PrintOrderEntity>>

  @Query("SELECT * FROM print_orders ORDER BY createdAt DESC LIMIT :limit")
  fun observeRecentOrders(limit: Int): Flow<List<PrintOrderEntity>>

  @Query("SELECT * FROM print_orders WHERE id = :id")
  suspend fun getOrderById(id: Long): PrintOrderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrder(order: PrintOrderEntity): Long

  @Update
  suspend fun updateOrder(order: PrintOrderEntity)

  @Query("DELETE FROM print_orders WHERE id = :id")
  suspend fun deleteOrderById(id: Long)

  @Query("DELETE FROM print_orders")
  suspend fun clearAllOrders()

  @Query("SELECT COUNT(*) FROM print_orders")
  suspend fun countOrders(): Int
}

@Dao
interface ReceiptTemplateDao {
  @Query("SELECT * FROM receipt_templates ORDER BY isDefault DESC, updatedAt DESC")
  fun observeAllTemplates(): Flow<List<ReceiptTemplateEntity>>

  @Query("SELECT * FROM receipt_templates WHERE id = :id")
  suspend fun getTemplateById(id: Long): ReceiptTemplateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTemplate(template: ReceiptTemplateEntity): Long

  @Update
  suspend fun updateTemplate(template: ReceiptTemplateEntity)

  @Query("DELETE FROM receipt_templates WHERE id = :id")
  suspend fun deleteTemplateById(id: Long)

  @Query("UPDATE receipt_templates SET isDefault = 0")
  suspend fun clearDefaults()

  @Query("UPDATE receipt_templates SET isDefault = 1 WHERE id = :id")
  suspend fun markDefault(id: Long)

  @Query("SELECT COUNT(*) FROM receipt_templates")
  suspend fun countTemplates(): Int
}
