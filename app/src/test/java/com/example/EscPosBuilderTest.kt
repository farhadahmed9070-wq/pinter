package com.example

import com.example.bluetooth.EscPosBuilder
import com.example.bluetooth.ReceiptPrintPayload
import com.example.data.model.OrderLineItem
import com.example.data.model.PaperWidth
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.data.model.TextSizeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EscPosBuilderTest {
  @Test
  fun formatColumns_respects58mmAnd80mmWidth() {
    val line58 = EscPosBuilder.formatColumns("TOTAL", "$12.50", PaperWidth.MM_58.columns)
    val line80 = EscPosBuilder.formatColumns("TOTAL", "$12.50", PaperWidth.MM_80.columns)
    assertEquals(32, line58.length)
    assertEquals(48, line80.length)
  }

  @Test
  fun buildTextBytes_startsWithEscInitAndContainsText() {
    val bytes =
      EscPosBuilder.buildTextBytes(
        text = "Hello Printer",
        align = TextAlignOption.CENTER,
        size = TextSizeOption.DOUBLE_HEIGHT,
        bold = true,
        underline = false,
        settings = PrinterSettings(paperWidth = PaperWidth.MM_58),
      )
    assertEquals(0x1B.toByte(), bytes[0])
    assertEquals(0x40.toByte(), bytes[1])
    assertTrue(String(bytes, Charsets.UTF_8).contains("Hello Printer"))
  }

  @Test
  fun buildReceiptBytes_formatsItemsAndTotalsFor80mm() {
    val payload =
      ReceiptPrintPayload(
        businessName = "TEST SHOP",
        businessAddress = "123 Main",
        businessPhone = "555-0100",
        headerText = "RECEIPT",
        orderNumber = "ORD-1001",
        customerName = "Sam",
        dateTimeText = "2026-11-24 12:00",
        items = listOf(OrderLineItem("1", "Espresso", 2, 4.00)),
        subtotal = 8.00,
        taxPercent = 10.0,
        taxAmount = 0.80,
        discountAmount = 0.0,
        totalAmount = 8.80,
        currencySymbol = "$",
        notes = "",
        footerText = "Thank you!",
        includeQrFooter = true,
      )
    val preview = EscPosBuilder.formatReceiptPreview(payload, PaperWidth.MM_80)
    assertTrue(preview.contains("TEST SHOP"))
    assertTrue(preview.contains("2x Espresso"))
    assertTrue(preview.contains("$8.80"))
    assertTrue(preview.contains("[ QR CODE ]"))

    val bytes = EscPosBuilder.buildReceiptBytes(payload, PrinterSettings(paperWidth = PaperWidth.MM_80))
    assertTrue(bytes.size > 60)
    // Ends with partial cut GS V 66 0 when autoCut=true on 80mm
    val tail = bytes.takeLast(4)
    assertEquals(listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x00.toByte()), tail)
  }
}
