package com.example.bluetooth

import com.example.data.model.OrderLineItem
import com.example.data.model.PaperWidth
import com.example.data.model.PrintDensity
import com.example.data.model.PrinterEncoding
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.data.model.TextSizeOption
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptPrintPayload(
  val businessName: String,
  val businessAddress: String,
  val businessPhone: String,
  val headerText: String,
  val orderNumber: String,
  val customerName: String,
  val dateTimeText: String,
  val items: List<OrderLineItem>,
  val subtotal: Double,
  val taxPercent: Double,
  val taxAmount: Double,
  val discountAmount: Double,
  val totalAmount: Double,
  val currencySymbol: String,
  val notes: String,
  val footerText: String,
  val headerAlign: TextAlignOption = TextAlignOption.CENTER,
  val includeQrFooter: Boolean = false,
  val qrData: String = "",
)

object EscPosBuilder {
  private val ESC_INIT = byteArrayOf(0x1B, 0x40)
  private val FEED_CUT_PARTIAL = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
  private val CASH_DRAWER_PULSE = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

  private fun charsetFor(encoding: PrinterEncoding): Charset =
    runCatching { Charset.forName(encoding.charsetName) }.getOrDefault(Charsets.UTF_8)

  private fun ByteArrayOutputStream.writeCommand(vararg bytes: Int) {
    bytes.forEach { write(it and 0xFF) }
  }

  private fun ByteArrayOutputStream.applyInitAndSettings(settings: PrinterSettings) {
    write(ESC_INIT)
    // Character code table
    writeCommand(0x1B, 0x74, settings.encoding.escPosCodeTable)
    // Print density (ESC 7 n1 n2 n3) - heating dots, heating time, heating interval
    val density = settings.printDensity
    writeCommand(0x1B, 0x37, 7, density.heatTime, density.heatInterval)
  }

  private fun ByteArrayOutputStream.applyFinish(settings: PrinterSettings) {
    if (settings.feedLines > 0) {
      writeCommand(0x1B, 0x64, settings.feedLines.coerceIn(0, 10))
    }
    if (settings.autoCut && settings.paperWidth == PaperWidth.MM_80) {
      write(FEED_CUT_PARTIAL)
    } else if (settings.autoCut) {
      // Extra feed for tear bar on 58mm portable printers
      writeCommand(0x1B, 0x64, 2)
    }
    if (settings.cashDrawer) {
      write(CASH_DRAWER_PULSE)
    }
  }

  private fun ByteArrayOutputStream.writeLine(text: String, charset: Charset) {
    write(text.toByteArray(charset))
    write('\n'.code)
  }

  private fun ByteArrayOutputStream.setAlign(align: TextAlignOption) {
    writeCommand(0x1B, 0x61, align.escPosCode)
  }

  private fun ByteArrayOutputStream.setBold(enabled: Boolean) {
    writeCommand(0x1B, 0x45, if (enabled) 1 else 0)
  }

  private fun ByteArrayOutputStream.setUnderline(enabled: Boolean) {
    writeCommand(0x1B, 0x2D, if (enabled) 1 else 0)
  }

  private fun ByteArrayOutputStream.setTextSize(size: TextSizeOption) {
    writeCommand(0x1D, 0x21, size.escPosParam)
  }

  private fun ByteArrayOutputStream.writeQrCode(data: String, charset: Charset) {
    if (data.isBlank()) return
    val payload = data.toByteArray(charset)
    val length = payload.size + 3
    val pL = length and 0xFF
    val pH = (length shr 8) and 0xFF
    // Model 2
    writeCommand(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)
    // Module size 5
    writeCommand(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05)
    // Error correction L
    writeCommand(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30)
    // Store data
    writeCommand(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30)
    write(payload)
    // Print QR
    writeCommand(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
    write('\n'.code)
  }

  fun buildTextBytes(
    text: String,
    align: TextAlignOption,
    size: TextSizeOption,
    bold: Boolean,
    underline: Boolean,
    settings: PrinterSettings,
  ): ByteArray {
    val charset = charsetFor(settings.encoding)
    val out = ByteArrayOutputStream()
    out.applyInitAndSettings(settings)
    out.setAlign(align)
    out.setBold(bold)
    out.setUnderline(underline)
    out.setTextSize(size)
    text.lines().forEach { line -> out.writeLine(line, charset) }
    out.setBold(false)
    out.setUnderline(false)
    out.setTextSize(TextSizeOption.NORMAL)
    out.setAlign(TextAlignOption.LEFT)
    out.applyFinish(settings)
    return out.toByteArray()
  }

  fun buildRasterImageBytes(
    raster: DitheredImageResult,
    align: TextAlignOption,
    settings: PrinterSettings,
  ): ByteArray {
    val out = ByteArrayOutputStream()
    out.applyInitAndSettings(settings)
    out.setAlign(align)

    // GS v 0 m xL xH yL yH d1...dk (split into slices <= 255 rows for reliable printer buffer)
    val bytesPerRow = raster.widthBytes
    val xL = bytesPerRow and 0xFF
    val xH = (bytesPerRow shr 8) and 0xFF
    val maxSliceHeight = 128
    var yOffset = 0
    while (yOffset < raster.height) {
      val sliceHeight = minOf(maxSliceHeight, raster.height - yOffset)
      val yL = sliceHeight and 0xFF
      val yH = (sliceHeight shr 8) and 0xFF
      out.writeCommand(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH)
      val start = yOffset * bytesPerRow
      val len = sliceHeight * bytesPerRow
      out.write(raster.escPosData, start, len)
      yOffset += sliceHeight
    }

    out.setAlign(TextAlignOption.LEFT)
    out.applyFinish(settings)
    return out.toByteArray()
  }

  fun buildReceiptBytes(payload: ReceiptPrintPayload, settings: PrinterSettings): ByteArray {
    val charset = charsetFor(settings.encoding)
    val cols = settings.paperWidth.columns
    val divider = "-".repeat(cols)
    val out = ByteArrayOutputStream()
    out.applyInitAndSettings(settings)

    // Business Header
    out.setAlign(payload.headerAlign)
    if (payload.businessName.isNotBlank()) {
      out.setBold(true)
      out.setTextSize(TextSizeOption.DOUBLE_HEIGHT)
      out.writeLine(payload.businessName.trim(), charset)
      out.setTextSize(TextSizeOption.NORMAL)
      out.setBold(false)
    }
    if (payload.businessAddress.isNotBlank()) {
      out.writeLine(payload.businessAddress.trim(), charset)
    }
    if (payload.businessPhone.isNotBlank()) {
      out.writeLine(payload.businessPhone.trim(), charset)
    }
    if (payload.headerText.isNotBlank()) {
      out.setBold(true)
      out.writeLine(payload.headerText.trim(), charset)
      out.setBold(false)
    }

    out.setAlign(TextAlignOption.LEFT)
    out.writeLine(divider, charset)

    if (payload.orderNumber.isNotBlank()) {
      out.writeLine(formatColumns("Order:", payload.orderNumber, cols), charset)
    }
    if (payload.dateTimeText.isNotBlank()) {
      out.writeLine(formatColumns("Date:", payload.dateTimeText, cols), charset)
    }
    if (payload.customerName.isNotBlank()) {
      out.writeLine(formatColumns("Customer:", payload.customerName, cols), charset)
    }

    out.writeLine(divider, charset)

    // Line items
    payload.items.forEach { item ->
      val priceStr = formatMoney(item.lineTotal, payload.currencySymbol)
      val left = "${item.quantity}x ${item.name}"
      if (left.length + priceStr.length + 1 <= cols) {
        out.writeLine(formatColumns(left, priceStr, cols), charset)
      } else {
        out.writeLine(left, charset)
        val unitLine = "  @ ${formatMoney(item.unitPrice, payload.currencySymbol)}"
        out.writeLine(formatColumns(unitLine, priceStr, cols), charset)
      }
    }

    out.writeLine(divider, charset)
    out.writeLine(
      formatColumns("Subtotal", formatMoney(payload.subtotal, payload.currencySymbol), cols),
      charset,
    )
    if (payload.discountAmount > 0.001) {
      out.writeLine(
        formatColumns(
          "Discount",
          "-${formatMoney(payload.discountAmount, payload.currencySymbol)}",
          cols,
        ),
        charset,
      )
    }
    if (payload.taxPercent > 0.0 || payload.taxAmount > 0.001) {
      out.writeLine(
        formatColumns(
          "Tax (${formatPercent(payload.taxPercent)}%)",
          formatMoney(payload.taxAmount, payload.currencySymbol),
          cols,
        ),
        charset,
      )
    }

    out.setBold(true)
    out.setTextSize(TextSizeOption.DOUBLE_HEIGHT)
    out.writeLine(
      formatColumns("TOTAL", formatMoney(payload.totalAmount, payload.currencySymbol), cols),
      charset,
    )
    out.setTextSize(TextSizeOption.NORMAL)
    out.setBold(false)

    if (payload.notes.isNotBlank()) {
      out.writeLine(divider, charset)
      out.writeLine("Note: ${payload.notes.trim()}", charset)
    }

    out.writeLine(divider, charset)
    if (payload.footerText.isNotBlank()) {
      out.setAlign(TextAlignOption.CENTER)
      payload.footerText.lines().forEach { out.writeLine(it, charset) }
    }

    if (payload.includeQrFooter) {
      out.setAlign(TextAlignOption.CENTER)
      val qrContent =
        payload.qrData.ifBlank { "${payload.businessName} | ${payload.orderNumber} | ${formatMoney(payload.totalAmount, payload.currencySymbol)}" }
      out.writeQrCode(qrContent, charset)
    }

    out.setAlign(TextAlignOption.LEFT)
    out.applyFinish(settings)
    return out.toByteArray()
  }

  fun buildTestPageBytes(printerName: String, settings: PrinterSettings): ByteArray {
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val cols = settings.paperWidth.columns
    val divider = "=".repeat(cols)
    val charset = charsetFor(settings.encoding)
    val out = ByteArrayOutputStream()
    out.applyInitAndSettings(settings)
    out.setAlign(TextAlignOption.CENTER)
    out.setBold(true)
    out.setTextSize(TextSizeOption.LARGE)
    out.writeLine("BLUETOOTH PRINT", charset)
    out.setTextSize(TextSizeOption.NORMAL)
    out.writeLine("PRINTER SELF-TEST", charset)
    out.setBold(false)
    out.setAlign(TextAlignOption.LEFT)
    out.writeLine(divider, charset)
    out.writeLine(formatColumns("Printer:", printerName.take(cols - 9), cols), charset)
    out.writeLine(formatColumns("Paper:", "${settings.paperWidth.mm}mm ($cols cols)", cols), charset)
    out.writeLine(formatColumns("Density:", settings.printDensity.label, cols), charset)
    out.writeLine(formatColumns("Encoding:", settings.encoding.label, cols), charset)
    out.writeLine(formatColumns("Time:", now, cols), charset)
    out.writeLine(divider, charset)
    out.setAlign(TextAlignOption.CENTER)
    out.writeLine("0123456789 ABCDEFGHIJKLMNOP", charset)
    out.writeLine("Connection & ESC/POS Ready!", charset)
    out.setAlign(TextAlignOption.LEFT)
    out.applyFinish(settings)
    return out.toByteArray()
  }

  fun formatReceiptPreview(payload: ReceiptPrintPayload, paperWidth: PaperWidth): String {
    val cols = paperWidth.columns
    val divider = "-".repeat(cols)
    return buildString {
      if (payload.businessName.isNotBlank()) {
        appendLine(alignPreviewLine(payload.businessName.trim(), cols, payload.headerAlign))
      }
      if (payload.businessAddress.isNotBlank()) {
        appendLine(alignPreviewLine(payload.businessAddress.trim(), cols, payload.headerAlign))
      }
      if (payload.businessPhone.isNotBlank()) {
        appendLine(alignPreviewLine(payload.businessPhone.trim(), cols, payload.headerAlign))
      }
      if (payload.headerText.isNotBlank()) {
        appendLine(alignPreviewLine(payload.headerText.trim(), cols, payload.headerAlign))
      }
      appendLine(divider)
      if (payload.orderNumber.isNotBlank()) {
        appendLine(formatColumns("Order:", payload.orderNumber, cols))
      }
      if (payload.dateTimeText.isNotBlank()) {
        appendLine(formatColumns("Date:", payload.dateTimeText, cols))
      }
      if (payload.customerName.isNotBlank()) {
        appendLine(formatColumns("Customer:", payload.customerName, cols))
      }
      appendLine(divider)
      payload.items.forEach { item ->
        val priceStr = formatMoney(item.lineTotal, payload.currencySymbol)
        val left = "${item.quantity}x ${item.name}"
        if (left.length + priceStr.length + 1 <= cols) {
          appendLine(formatColumns(left, priceStr, cols))
        } else {
          appendLine(left.take(cols))
          appendLine(formatColumns("  @ ${formatMoney(item.unitPrice, payload.currencySymbol)}", priceStr, cols))
        }
      }
      appendLine(divider)
      appendLine(formatColumns("Subtotal", formatMoney(payload.subtotal, payload.currencySymbol), cols))
      if (payload.discountAmount > 0.001) {
        appendLine(
          formatColumns(
            "Discount",
            "-${formatMoney(payload.discountAmount, payload.currencySymbol)}",
            cols,
          )
        )
      }
      if (payload.taxPercent > 0.0 || payload.taxAmount > 0.001) {
        appendLine(
          formatColumns(
            "Tax (${formatPercent(payload.taxPercent)}%)",
            formatMoney(payload.taxAmount, payload.currencySymbol),
            cols,
          )
        )
      }
      appendLine(formatColumns("TOTAL", formatMoney(payload.totalAmount, payload.currencySymbol), cols))
      if (payload.notes.isNotBlank()) {
        appendLine(divider)
        appendLine("Note: ${payload.notes.trim()}")
      }
      appendLine(divider)
      if (payload.footerText.isNotBlank()) {
        payload.footerText.lines().forEach { line ->
          appendLine(alignPreviewLine(line.trim(), cols, TextAlignOption.CENTER))
        }
      }
      if (payload.includeQrFooter) {
        appendLine(alignPreviewLine("[ QR CODE ]", cols, TextAlignOption.CENTER))
      }
    }.trimEnd()
  }

  fun formatColumns(left: String, right: String, totalWidth: Int): String {
    val cleanLeft = left.trim()
    val cleanRight = right.trim()
    val spaceCount = (totalWidth - cleanLeft.length - cleanRight.length).coerceAtLeast(1)
    return if (cleanLeft.length + cleanRight.length + 1 > totalWidth) {
      val maxLeft = (totalWidth - cleanRight.length - 1).coerceAtLeast(4)
      "${cleanLeft.take(maxLeft)} $cleanRight"
    } else {
      "$cleanLeft${" ".repeat(spaceCount)}$cleanRight"
    }
  }

  private fun alignPreviewLine(line: String, cols: Int, align: TextAlignOption): String {
    val trimmed = line.take(cols)
    return when (align) {
      TextAlignOption.LEFT -> trimmed
      TextAlignOption.CENTER -> {
        val pad = ((cols - trimmed.length) / 2).coerceAtLeast(0)
        " ".repeat(pad) + trimmed
      }
      TextAlignOption.RIGHT -> {
        val pad = (cols - trimmed.length).coerceAtLeast(0)
        " ".repeat(pad) + trimmed
      }
    }
  }

  fun formatMoney(amount: Double, currencySymbol: String): String =
    String.format(Locale.US, "%s%.2f", currencySymbol, amount)

  private fun formatPercent(percent: Double): String =
    if (percent % 1.0 == 0.0) percent.toInt().toString()
    else String.format(Locale.US, "%.1f", percent)
}
