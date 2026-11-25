package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BitmapDitherer
import com.example.bluetooth.BluetoothPrinterManager
import com.example.bluetooth.DitheredImageResult
import com.example.bluetooth.EscPosBuilder
import com.example.bluetooth.ReceiptPrintPayload
import com.example.data.local.AppDatabase
import com.example.data.local.PrintOrderEntity
import com.example.data.local.PrinterDataRepository
import com.example.data.local.ReceiptTemplateEntity
import com.example.data.local.SettingsRepository
import com.example.data.model.AppLanguage
import com.example.data.model.BluetoothPrinterDevice
import com.example.data.model.DitherMode
import com.example.data.model.OrderLineItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaperWidth
import com.example.data.model.PrintDensity
import com.example.data.model.PrinterConnectionState
import com.example.data.model.PrinterEncoding
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.data.model.TextSizeOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BluetoothPrintViewModel(application: Application) : AndroidViewModel(application) {
  private val database = AppDatabase.getInstance(application)
  private val dataRepository =
    PrinterDataRepository(database.printOrderDao(), database.receiptTemplateDao())
  private val settingsRepository = SettingsRepository(application)
  val bluetoothManager = BluetoothPrinterManager(application)

  val settings: StateFlow<PrinterSettings> =
    settingsRepository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = PrinterSettings(),
    )

  val orders: StateFlow<List<PrintOrderEntity>> =
    dataRepository.allOrders.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val recentOrders: StateFlow<List<PrintOrderEntity>> =
    dataRepository.recentOrders(4).stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val templates: StateFlow<List<ReceiptTemplateEntity>> =
    dataRepository.allTemplates.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val connectionState: StateFlow<PrinterConnectionState> = bluetoothManager.connectionState
  val pairedDevices: StateFlow<List<BluetoothPrinterDevice>> = bluetoothManager.pairedDevices
  val discoveredDevices: StateFlow<List<BluetoothPrinterDevice>> =
    bluetoothManager.discoveredDevices
  val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning
  val isBluetoothSupported: StateFlow<Boolean> = bluetoothManager.isBluetoothSupported
  val isBluetoothEnabled: StateFlow<Boolean> = bluetoothManager.isBluetoothEnabled
  val hasBluetoothPermissions: StateFlow<Boolean> = bluetoothManager.hasPermissions

  private val _isPrinting = MutableStateFlow(false)
  val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val messages: SharedFlow<String> = _messages.asSharedFlow()

  // Image printing state
  private val _selectedSourceBitmap = MutableStateFlow<Bitmap?>(null)
  val selectedSourceBitmap: StateFlow<Bitmap?> = _selectedSourceBitmap.asStateFlow()

  private val _ditheredPreview = MutableStateFlow<DitheredImageResult?>(null)
  val ditheredPreview: StateFlow<DitheredImageResult?> = _ditheredPreview.asStateFlow()

  private val _ditherMode = MutableStateFlow(DitherMode.FLOYD_STEINBERG)
  val ditherMode: StateFlow<DitherMode> = _ditherMode.asStateFlow()

  private val _imageBrightness = MutableStateFlow(0f)
  val imageBrightness: StateFlow<Float> = _imageBrightness.asStateFlow()

  private val _imageAlign = MutableStateFlow(TextAlignOption.CENTER)
  val imageAlign: StateFlow<TextAlignOption> = _imageAlign.asStateFlow()

  init {
    viewModelScope.launch {
      dataRepository.ensureStarterData()
      val currentSettings = settingsRepository.settingsFlow.first()
      if (
        currentSettings.autoReconnect &&
          currentSettings.lastPrinterAddress.isNotBlank() &&
          bluetoothManager.checkPermissions() &&
          bluetoothManager.isBluetoothEnabled.value
      ) {
        bluetoothManager.connect(
          BluetoothPrinterDevice(
            name = currentSettings.lastPrinterName.ifBlank { "Saved Printer" },
            address = currentSettings.lastPrinterAddress,
            isPaired = true,
            isPrinterClass = true,
          )
        )
      }
    }
  }

  fun refreshBluetoothState() {
    bluetoothManager.refreshState()
  }

  fun startScan() {
    val started = bluetoothManager.startScan()
    if (!started) {
      viewModelScope.launch {
        _messages.emit("Unable to start scan. Check Bluetooth & permissions.")
      }
    }
  }

  fun stopScan() {
    bluetoothManager.stopScan()
  }

  fun connectToPrinter(device: BluetoothPrinterDevice) {
    viewModelScope.launch {
      val result = bluetoothManager.connect(device)
      result.onSuccess { connected ->
        settingsRepository.saveLastConnectedPrinter(connected.name, connected.address)
        _messages.emit("Connected to ${connected.name}")
      }
      result.onFailure { err ->
        _messages.emit(err.message ?: "Failed to connect to ${device.name}")
      }
    }
  }

  fun disconnectPrinter() {
    viewModelScope.launch {
      bluetoothManager.disconnect()
      _messages.emit("Printer disconnected")
    }
  }

  fun printTestPage() {
    viewModelScope.launch {
      val currentSettings = settings.value
      val printer = connectedDevice()
      val printerName = printer?.name ?: "Thermal Printer"
      val bytes = EscPosBuilder.buildTestPageBytes(printerName, currentSettings)
      sendBytesAndRecord(
        bytes = bytes,
        orderNumber = "TEST-${System.currentTimeMillis() % 10000}",
        orderType = OrderType.TEST_PAGE,
        customerName = "",
        businessName = "Printer Self-Test",
        items = emptyList(),
        subtotal = 0.0,
        taxPercent = 0.0,
        taxAmount = 0.0,
        discountAmount = 0.0,
        totalAmount = 0.0,
        currencySymbol = "$",
        notes = "Paper: ${currentSettings.paperWidth.mm}mm | Density: ${currentSettings.printDensity.label}",
        previewText =
          "BLUETOOTH PRINT\nPRINTER SELF-TEST\nPaper: ${currentSettings.paperWidth.mm}mm (${currentSettings.paperWidth.columns} cols)\nDensity: ${currentSettings.printDensity.label}\nEncoding: ${currentSettings.encoding.label}",
        saveWhenDisconnected = false,
      )
    }
  }

  fun printText(
    text: String,
    align: TextAlignOption,
    size: TextSizeOption,
    bold: Boolean,
    underline: Boolean,
  ) {
    if (text.isBlank()) {
      viewModelScope.launch { _messages.emit("Enter some text to print first.") }
      return
    }
    viewModelScope.launch {
      val currentSettings = settings.value
      val bytes = EscPosBuilder.buildTextBytes(text, align, size, bold, underline, currentSettings)
      val orderNo = dataRepository.nextOrderNumber()
      sendBytesAndRecord(
        bytes = bytes,
        orderNumber = orderNo,
        orderType = OrderType.TEXT,
        customerName = "",
        businessName = "Text Print",
        items = emptyList(),
        subtotal = 0.0,
        taxPercent = 0.0,
        taxAmount = 0.0,
        discountAmount = 0.0,
        totalAmount = 0.0,
        currencySymbol = "$",
        notes = "${align.label} • ${size.label}",
        previewText = text.trim(),
        saveWhenDisconnected = true,
      )
    }
  }

  fun onImagePicked(uri: Uri) {
    viewModelScope.launch {
      val bitmap = BitmapDitherer.loadBitmapFromUri(getApplication(), uri)
      if (bitmap == null) {
        _messages.emit("Could not load selected image.")
        return@launch
      }
      _selectedSourceBitmap.value = bitmap
      reprocessDitheredImage()
    }
  }

  fun updateDitherMode(mode: DitherMode) {
    _ditherMode.value = mode
    viewModelScope.launch { reprocessDitheredImage() }
  }

  fun updateImageBrightness(brightness: Float) {
    _imageBrightness.value = brightness
    viewModelScope.launch { reprocessDitheredImage() }
  }

  fun updateImageAlign(align: TextAlignOption) {
    _imageAlign.value = align
  }

  fun refreshDitherForPaperWidth() {
    viewModelScope.launch { reprocessDitheredImage() }
  }

  private suspend fun reprocessDitheredImage() {
    val source = _selectedSourceBitmap.value ?: return
    val targetDots = settings.value.paperWidth.dotWidth
    val result =
      BitmapDitherer.processForThermalPrinter(
        source = source,
        targetWidthDots = targetDots,
        ditherMode = _ditherMode.value,
        brightnessOffset = _imageBrightness.value,
      )
    _ditheredPreview.value = result
  }

  fun printCurrentImage() {
    val raster = _ditheredPreview.value
    if (raster == null) {
      viewModelScope.launch { _messages.emit("Select an image from gallery first.") }
      return
    }
    viewModelScope.launch {
      val currentSettings = settings.value
      val bytes = EscPosBuilder.buildRasterImageBytes(raster, _imageAlign.value, currentSettings)
      val orderNo = dataRepository.nextOrderNumber()
      sendBytesAndRecord(
        bytes = bytes,
        orderNumber = orderNo,
        orderType = OrderType.IMAGE,
        customerName = "",
        businessName = "Image Print (${raster.width}x${raster.height}px)",
        items = emptyList(),
        subtotal = 0.0,
        taxPercent = 0.0,
        taxAmount = 0.0,
        discountAmount = 0.0,
        totalAmount = 0.0,
        currencySymbol = "$",
        notes = "${_ditherMode.value.label} • ${_imageAlign.value.label}",
        previewText = "[Raster Bitmap ${raster.width}x${raster.height} dots • ${_ditherMode.value.label}]",
        saveWhenDisconnected = true,
      )
    }
  }

  fun saveOrPrintOrder(
    orderNumber: String,
    orderType: OrderType,
    customerName: String,
    businessName: String,
    businessAddress: String,
    businessPhone: String,
    headerText: String,
    footerText: String,
    items: List<OrderLineItem>,
    taxPercent: Double,
    discountAmount: Double,
    currencySymbol: String,
    notes: String,
    headerAlign: TextAlignOption,
    includeQrFooter: Boolean,
    dateTimeText: String,
    shouldPrintNow: Boolean,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      val currentSettings = settings.value
      val cleanItems = items.filter { it.name.isNotBlank() && it.quantity > 0 }
      val subtotal = cleanItems.sumOf { it.lineTotal }
      val taxable = (subtotal - discountAmount).coerceAtLeast(0.0)
      val taxAmount = taxable * (taxPercent / 100.0)
      val totalAmount = (taxable + taxAmount).coerceAtLeast(0.0)

      val payload =
        ReceiptPrintPayload(
          businessName = businessName,
          businessAddress = businessAddress,
          businessPhone = businessPhone,
          headerText = headerText,
          orderNumber = orderNumber,
          customerName = customerName,
          dateTimeText =
            dateTimeText.ifBlank {
              SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            },
          items = cleanItems,
          subtotal = subtotal,
          taxPercent = taxPercent,
          taxAmount = taxAmount,
          discountAmount = discountAmount,
          totalAmount = totalAmount,
          currencySymbol = currencySymbol.ifBlank { "$" },
          notes = notes,
          footerText = footerText,
          headerAlign = headerAlign,
          includeQrFooter = includeQrFooter,
        )

      val preview = EscPosBuilder.formatReceiptPreview(payload, currentSettings.paperWidth)

      if (shouldPrintNow) {
        val bytes = EscPosBuilder.buildReceiptBytes(payload, currentSettings)
        sendBytesAndRecord(
          bytes = bytes,
          orderNumber = orderNumber,
          orderType = orderType,
          customerName = customerName,
          businessName = businessName.ifBlank { "Order $orderNumber" },
          items = cleanItems,
          subtotal = subtotal,
          taxPercent = taxPercent,
          taxAmount = taxAmount,
          discountAmount = discountAmount,
          totalAmount = totalAmount,
          currencySymbol = payload.currencySymbol,
          notes = notes,
          previewText = preview,
          saveWhenDisconnected = true,
        )
      } else {
        dataRepository.saveOrder(
          PrintOrderEntity(
            orderNumber = orderNumber,
            orderType = orderType.name,
            customerName = customerName,
            businessName = businessName.ifBlank { "Order $orderNumber" },
            itemsJson = OrderLineItem.listToJson(cleanItems),
            subtotal = subtotal,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            currencySymbol = payload.currencySymbol,
            notes = notes,
            previewText = preview,
            printerName = connectedDevice()?.name ?: "Saved Locally",
            printerAddress = connectedDevice()?.address ?: "",
            paperWidthMm = currentSettings.paperWidth.mm,
            status = OrderStatus.SAVED.name,
          )
        )
        _messages.emit("Order $orderNumber saved")
      }
      onDone()
    }
  }

  fun reprintOrder(order: PrintOrderEntity) {
    viewModelScope.launch {
      val currentSettings = settings.value
      val orderType = OrderType.fromName(order.orderType)
      val bytes =
        if (order.parsedItems().isNotEmpty()) {
          val payload =
            ReceiptPrintPayload(
              businessName = order.businessName,
              businessAddress = "",
              businessPhone = "",
              headerText = if (orderType == OrderType.RECEIPT) "RECEIPT" else "ORDER SLIP",
              orderNumber = order.orderNumber,
              customerName = order.customerName,
              dateTimeText =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(order.createdAt)),
              items = order.parsedItems(),
              subtotal = order.subtotal,
              taxPercent = order.taxPercent,
              taxAmount = order.taxAmount,
              discountAmount = order.discountAmount,
              totalAmount = order.totalAmount,
              currencySymbol = order.currencySymbol,
              notes = order.notes,
              footerText = "Reprinted via Bluetooth Print",
              headerAlign = TextAlignOption.CENTER,
            )
          EscPosBuilder.buildReceiptBytes(payload, currentSettings)
        } else {
          EscPosBuilder.buildTextBytes(
            text = order.previewText,
            align = TextAlignOption.LEFT,
            size = TextSizeOption.NORMAL,
            bold = false,
            underline = false,
            settings = currentSettings,
          )
        }

      val printer = connectedDevice()
      if (printer == null) {
        _messages.emit("No printer connected. Tap Find Printer to connect before printing.")
        return@launch
      }

      _isPrinting.value = true
      val result = bluetoothManager.sendPrintData(bytes)
      _isPrinting.value = false
      result.onSuccess {
        dataRepository.updateOrder(
          order.copy(
            status = OrderStatus.PRINTED.name,
            printerName = printer.name,
            printerAddress = printer.address,
            paperWidthMm = currentSettings.paperWidth.mm,
          )
        )
        _messages.emit("Reprinted ${order.orderNumber} on ${printer.name}")
      }
      result.onFailure { err ->
        dataRepository.updateOrder(order.copy(status = OrderStatus.FAILED.name))
        _messages.emit(err.message ?: "Reprint failed")
      }
    }
  }

  fun deleteOrder(id: Long) {
    viewModelScope.launch {
      dataRepository.deleteOrder(id)
      _messages.emit("Order deleted")
    }
  }

  suspend fun getTemplateById(id: Long): ReceiptTemplateEntity? =
    dataRepository.getTemplateById(id)

  suspend fun nextOrderNumber(): String = dataRepository.nextOrderNumber()

  fun saveTemplate(template: ReceiptTemplateEntity, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      dataRepository.saveTemplate(template)
      _messages.emit("Template \"${template.templateName}\" saved")
      onSaved()
    }
  }

  fun duplicateTemplate(template: ReceiptTemplateEntity) {
    viewModelScope.launch {
      dataRepository.saveTemplate(
        template.copy(
          id = 0L,
          templateName = "${template.templateName} (Copy)",
          isDefault = false,
        )
      )
      _messages.emit("Template duplicated")
    }
  }

  fun setDefaultTemplate(id: Long) {
    viewModelScope.launch {
      dataRepository.setDefaultTemplate(id)
      _messages.emit("Default template updated")
    }
  }

  fun deleteTemplate(id: Long) {
    viewModelScope.launch {
      dataRepository.deleteTemplate(id)
      _messages.emit("Template deleted")
    }
  }

  // Settings actions
  fun setPaperWidth(width: PaperWidth) {
    viewModelScope.launch {
      settingsRepository.updatePaperWidth(width)
      reprocessDitheredImage()
    }
  }

  fun setPrintDensity(density: PrintDensity) {
    viewModelScope.launch { settingsRepository.updatePrintDensity(density) }
  }

  fun setAutoCut(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.updateAutoCut(enabled) }
  }

  fun setCashDrawer(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.updateCashDrawer(enabled) }
  }

  fun setFeedLines(lines: Int) {
    viewModelScope.launch { settingsRepository.updateFeedLines(lines) }
  }

  fun setAutoReconnect(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.updateAutoReconnect(enabled) }
  }

  fun setEncoding(encoding: PrinterEncoding) {
    viewModelScope.launch { settingsRepository.updateEncoding(encoding) }
  }

  fun setLanguage(language: AppLanguage) {
    viewModelScope.launch { settingsRepository.updateLanguage(language) }
  }

  private fun connectedDevice(): BluetoothPrinterDevice? =
    (connectionState.value as? PrinterConnectionState.Connected)?.device

  private suspend fun sendBytesAndRecord(
    bytes: ByteArray,
    orderNumber: String,
    orderType: OrderType,
    customerName: String,
    businessName: String,
    items: List<OrderLineItem>,
    subtotal: Double,
    taxPercent: Double,
    taxAmount: Double,
    discountAmount: Double,
    totalAmount: Double,
    currencySymbol: String,
    notes: String,
    previewText: String,
    saveWhenDisconnected: Boolean,
  ) {
    val currentSettings = settings.value
    val printer = connectedDevice()
    if (printer == null) {
      if (saveWhenDisconnected) {
        dataRepository.saveOrder(
          PrintOrderEntity(
            orderNumber = orderNumber,
            orderType = orderType.name,
            customerName = customerName,
            businessName = businessName,
            itemsJson = OrderLineItem.listToJson(items),
            subtotal = subtotal,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            currencySymbol = currencySymbol,
            notes = notes,
            previewText = previewText,
            printerName = "Not Printed",
            printerAddress = "",
            paperWidthMm = currentSettings.paperWidth.mm,
            status = OrderStatus.SAVED.name,
          )
        )
        _messages.emit("Saved $orderNumber. Connect a printer via Find Printer to print.")
      } else {
        _messages.emit("No printer connected. Tap Find Printer to connect first.")
      }
      return
    }

    _isPrinting.value = true
    val result = bluetoothManager.sendPrintData(bytes)
    _isPrinting.value = false

    val finalStatus = if (result.isSuccess) OrderStatus.PRINTED else OrderStatus.FAILED
    dataRepository.saveOrder(
      PrintOrderEntity(
        orderNumber = orderNumber,
        orderType = orderType.name,
        customerName = customerName,
        businessName = businessName,
        itemsJson = OrderLineItem.listToJson(items),
        subtotal = subtotal,
        taxPercent = taxPercent,
        taxAmount = taxAmount,
        discountAmount = discountAmount,
        totalAmount = totalAmount,
        currencySymbol = currencySymbol,
        notes = notes,
        previewText = previewText,
        printerName = printer.name,
        printerAddress = printer.address,
        paperWidthMm = currentSettings.paperWidth.mm,
        status = finalStatus.name,
      )
    )

    result.onSuccess { _messages.emit("Printed $orderNumber on ${printer.name}") }
    result.onFailure { err ->
      _messages.emit(err.message ?: "Print failed — saved to Orders")
    }
  }

  override fun onCleared() {
    super.onCleared()
    bluetoothManager.release()
  }
}
