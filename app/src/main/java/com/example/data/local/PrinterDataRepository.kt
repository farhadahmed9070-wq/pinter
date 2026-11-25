package com.example.data.local

import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.TextAlignOption
import kotlinx.coroutines.flow.Flow

class PrinterDataRepository(
  private val orderDao: PrintOrderDao,
  private val templateDao: ReceiptTemplateDao,
) {
  val allOrders: Flow<List<PrintOrderEntity>> = orderDao.observeAllOrders()

  fun recentOrders(limit: Int = 4): Flow<List<PrintOrderEntity>> =
    orderDao.observeRecentOrders(limit)

  val allTemplates: Flow<List<ReceiptTemplateEntity>> = templateDao.observeAllTemplates()

  suspend fun ensureStarterData() {
    if (templateDao.countTemplates() == 0) {
      val starterTemplates =
        listOf(
          ReceiptTemplateEntity(
            templateName = "Classic Retail POS",
            businessName = "NORTHSIDE MARKET",
            businessAddress = "240 Oak Street, Suite 4B",
            businessPhone = "Tel: (555) 234-8910",
            headerText = "SALES RECEIPT",
            footerText = "Thank you for shopping with us!\nReturns accepted within 14 days.",
            currencySymbol = "$",
            defaultTaxPercent = 8.0,
            paperWidthMm = 58,
            showDateTime = true,
            showOrderNumber = true,
            showBarcodeFooter = true,
            barcodeData = "NS-2026-001",
            headerAlign = TextAlignOption.CENTER.name,
            isDefault = true,
          ),
          ReceiptTemplateEntity(
            templateName = "Cafe & Bakery",
            businessName = "BLUEBIRD ROASTERS",
            businessAddress = "88 Harbor Lane, Downtown",
            businessPhone = "IG: @bluebirdroasters",
            headerText = "COUNTER ORDER",
            footerText = "Freshly roasted daily.\nWi-Fi: BluebirdGuest",
            currencySymbol = "$",
            defaultTaxPercent = 5.0,
            paperWidthMm = 58,
            showDateTime = true,
            showOrderNumber = true,
            showBarcodeFooter = false,
            barcodeData = "",
            headerAlign = TextAlignOption.CENTER.name,
            isDefault = false,
          ),
          ReceiptTemplateEntity(
            templateName = "80mm Dispatch Slip",
            businessName = "METRO LOGISTICS HUB",
            businessAddress = "Warehouse 12, Industrial Park",
            businessPhone = "Support: +1 800 555 0199",
            headerText = "DELIVERY & PACKING SLIP",
            footerText = "Please inspect items upon delivery.\nSignature: ______________________",
            currencySymbol = "$",
            defaultTaxPercent = 0.0,
            paperWidthMm = 80,
            showDateTime = true,
            showOrderNumber = true,
            showBarcodeFooter = true,
            barcodeData = "MLH-DISPATCH",
            headerAlign = TextAlignOption.CENTER.name,
            isDefault = false,
          ),
        )
      starterTemplates.forEach { templateDao.insertTemplate(it) }
    }

    if (orderDao.countOrders() == 0) {
      val sampleItems =
        listOf(
          OrderLineItem("1", "Artisan Espresso", 2, 4.50),
          OrderLineItem("2", "Almond Croissant", 1, 3.75),
        )
      val subtotal = sampleItems.sumOf { it.lineTotal }
      val tax = subtotal * 0.05
      val total = subtotal + tax
      orderDao.insertOrder(
        PrintOrderEntity(
          orderNumber = "ORD-1001",
          orderType = OrderType.RECEIPT.name,
          customerName = "Walk-in Guest",
          businessName = "BLUEBIRD ROASTERS",
          itemsJson = OrderLineItem.listToJson(sampleItems),
          subtotal = subtotal,
          taxPercent = 5.0,
          taxAmount = tax,
          discountAmount = 0.0,
          totalAmount = total,
          currencySymbol = "$",
          notes = "Counter pickup",
          previewText =
            "BLUEBIRD ROASTERS\n88 Harbor Lane, Downtown\n--------------------------------\n2x Artisan Espresso        $9.00\n1x Almond Croissant        $3.75\n--------------------------------\nSubtotal                  $12.75\nTax (5.0%)                 $0.64\nTOTAL                     $13.39",
          printerName = "Sample Template",
          printerAddress = "Saved Locally",
          paperWidthMm = 58,
          status = OrderStatus.SAVED.name,
          createdAt = System.currentTimeMillis() - 3600_000L,
        )
      )
    }
  }

  suspend fun getOrderById(id: Long): PrintOrderEntity? = orderDao.getOrderById(id)

  suspend fun saveOrder(order: PrintOrderEntity): Long = orderDao.insertOrder(order)

  suspend fun updateOrder(order: PrintOrderEntity) = orderDao.updateOrder(order)

  suspend fun deleteOrder(id: Long) = orderDao.deleteOrderById(id)

  suspend fun getTemplateById(id: Long): ReceiptTemplateEntity? = templateDao.getTemplateById(id)

  suspend fun saveTemplate(template: ReceiptTemplateEntity): Long {
    if (template.isDefault) {
      templateDao.clearDefaults()
    }
    return templateDao.insertTemplate(template.copy(updatedAt = System.currentTimeMillis()))
  }

  suspend fun setDefaultTemplate(id: Long) {
    templateDao.clearDefaults()
    templateDao.markDefault(id)
  }

  suspend fun deleteTemplate(id: Long) = templateDao.deleteTemplateById(id)

  suspend fun nextOrderNumber(): String {
    val count = orderDao.countOrders() + 1001
    return "ORD-$count"
  }
}
