package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

enum class PaperWidth(val mm: Int, val label: String, val columns: Int, val dotWidth: Int) {
  MM_58(58, "58mm (2\")", 32, 384),
  MM_80(80, "80mm (3\")", 48, 576);

  companion object {
    fun fromMm(mm: Int): PaperWidth = entries.firstOrNull { it.mm == mm } ?: MM_58
  }
}

enum class PrintDensity(
  val level: Int,
  val label: String,
  val heatTime: Int,
  val heatInterval: Int,
) {
  LIGHT(0, "Light", 60, 2),
  MEDIUM(1, "Medium", 95, 2),
  DARK(2, "Dark", 130, 3),
  EXTRA_DARK(3, "Extra Dark", 170, 4);

  companion object {
    fun fromLevel(level: Int): PrintDensity = entries.firstOrNull { it.level == level } ?: MEDIUM
  }
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
  ENGLISH("en", "English", "English"),
  SPANISH("es", "Spanish", "Español"),
  FRENCH("fr", "French", "Français"),
  ARABIC("ar", "Arabic", "العربية"),
  BENGALI("bn", "Bangla", "বাংলা");

  companion object {
    fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: ENGLISH
  }
}

enum class TextAlignOption(val escPosCode: Int, val label: String) {
  LEFT(0, "Left"),
  CENTER(1, "Center"),
  RIGHT(2, "Right");

  companion object {
    fun fromName(name: String): TextAlignOption =
      entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CENTER
  }
}

enum class TextSizeOption(val label: String, val widthMult: Int, val heightMult: Int) {
  NORMAL("Normal", 1, 1),
  DOUBLE_HEIGHT("Tall", 1, 2),
  DOUBLE_WIDTH("Wide", 2, 1),
  LARGE("Large 2x", 2, 2);

  val escPosParam: Int
    get() = (((widthMult - 1) shl 4) or (heightMult - 1))
}

enum class DitherMode(val label: String) {
  FLOYD_STEINBERG("Floyd-Steinberg"),
  ATKINSON("Atkinson"),
  THRESHOLD("Sharp B&W"),
}

enum class OrderStatus(val label: String) {
  PRINTED("Printed"),
  SAVED("Saved"),
  FAILED("Failed");

  companion object {
    fun fromName(name: String): OrderStatus =
      entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SAVED
  }
}

enum class OrderType(val label: String) {
  ORDER("Order"),
  RECEIPT("Receipt"),
  TEXT("Text"),
  IMAGE("Image"),
  TEST_PAGE("Test Page");

  companion object {
    fun fromName(name: String): OrderType =
      entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ORDER
  }
}

enum class PrinterEncoding(val label: String, val charsetName: String, val escPosCodeTable: Int) {
  UTF_8("UTF-8", "UTF-8", 0),
  CP437("CP437 (USA)", "Cp437", 0),
  CP850("CP850 (Multilingual)", "Cp850", 2),
  CP1252("Windows-1252", "Cp1252", 16);

  companion object {
    fun fromName(name: String): PrinterEncoding =
      entries.firstOrNull { it.name == name } ?: UTF_8
  }
}

data class OrderLineItem(
  val id: String,
  val name: String,
  val quantity: Int,
  val unitPrice: Double,
) {
  val lineTotal: Double
    get() = quantity * unitPrice

  companion object {
    fun listToJson(items: List<OrderLineItem>): String {
      val array = JSONArray()
      items.forEach { item ->
        array.put(
          JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("quantity", item.quantity)
            put("unitPrice", item.unitPrice)
          }
        )
      }
      return array.toString()
    }

    fun listFromJson(json: String): List<OrderLineItem> {
      if (json.isBlank()) return emptyList()
      return runCatching {
        val array = JSONArray(json)
        buildList {
          for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
              OrderLineItem(
                id = obj.optString("id", i.toString()),
                name = obj.optString("name", ""),
                quantity = obj.optInt("quantity", 1),
                unitPrice = obj.optDouble("unitPrice", 0.0),
              )
            )
          }
        }
      }.getOrDefault(emptyList())
    }
  }
}

data class BluetoothPrinterDevice(
  val name: String,
  val address: String,
  val isPaired: Boolean,
  val isPrinterClass: Boolean,
  val rssi: Int? = null,
)

sealed interface PrinterConnectionState {
  data object Disconnected : PrinterConnectionState

  data class Connecting(val device: BluetoothPrinterDevice) : PrinterConnectionState

  data class Connected(val device: BluetoothPrinterDevice) : PrinterConnectionState

  data class Error(val message: String, val device: BluetoothPrinterDevice? = null) :
    PrinterConnectionState
}

data class PrinterSettings(
  val paperWidth: PaperWidth = PaperWidth.MM_58,
  val printDensity: PrintDensity = PrintDensity.MEDIUM,
  val autoCut: Boolean = true,
  val cashDrawer: Boolean = false,
  val feedLines: Int = 3,
  val autoReconnect: Boolean = false,
  val encoding: PrinterEncoding = PrinterEncoding.UTF_8,
  val language: AppLanguage = AppLanguage.ENGLISH,
  val lastPrinterName: String = "",
  val lastPrinterAddress: String = "",
)
