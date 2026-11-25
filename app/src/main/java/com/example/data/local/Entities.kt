package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.OrderLineItem

@Entity(tableName = "print_orders")
data class PrintOrderEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val orderNumber: String,
  val orderType: String,
  val customerName: String,
  val businessName: String,
  val itemsJson: String,
  val subtotal: Double,
  val taxPercent: Double,
  val taxAmount: Double,
  val discountAmount: Double,
  val totalAmount: Double,
  val currencySymbol: String,
  val notes: String,
  val previewText: String,
  val printerName: String,
  val printerAddress: String,
  val paperWidthMm: Int,
  val status: String,
  val createdAt: Long = System.currentTimeMillis(),
) {
  fun parsedItems(): List<OrderLineItem> = OrderLineItem.listFromJson(itemsJson)
}

@Entity(tableName = "receipt_templates")
data class ReceiptTemplateEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val templateName: String,
  val businessName: String,
  val businessAddress: String,
  val businessPhone: String,
  val headerText: String,
  val footerText: String,
  val currencySymbol: String,
  val defaultTaxPercent: Double,
  val paperWidthMm: Int,
  val showDateTime: Boolean,
  val showOrderNumber: Boolean,
  val showBarcodeFooter: Boolean,
  val barcodeData: String,
  val headerAlign: String,
  val isDefault: Boolean = false,
  val updatedAt: Long = System.currentTimeMillis(),
)
