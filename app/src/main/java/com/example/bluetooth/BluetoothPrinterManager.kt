package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.BluetoothPrinterDevice
import com.example.data.model.PrinterConnectionState
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BluetoothPrinterManager(private val context: Context) {
  companion object {
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun requiredPermissions(): Array<String> =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
      } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
      }
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val ioMutex = Mutex()

  private val bluetoothManager: BluetoothManager? =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter?
    get() = bluetoothManager?.adapter

  private var activeSocket: BluetoothSocket? = null
  private var receiverRegistered = false

  private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
  val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

  private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
  val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

  private val _hasPermissions = MutableStateFlow(checkPermissions())
  val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

  private val _isScanning = MutableStateFlow(false)
  val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

  private val _pairedDevices = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
  val pairedDevices: StateFlow<List<BluetoothPrinterDevice>> = _pairedDevices.asStateFlow()

  private val _discoveredDevices = MutableStateFlow<List<BluetoothPrinterDevice>>(emptyList())
  val discoveredDevices: StateFlow<List<BluetoothPrinterDevice>> = _discoveredDevices.asStateFlow()

  private val _connectionState =
    MutableStateFlow<PrinterConnectionState>(PrinterConnectionState.Disconnected)
  val connectionState: StateFlow<PrinterConnectionState> = _connectionState.asStateFlow()

  private val discoveryReceiver =
    object : BroadcastReceiver() {
      @SuppressLint("MissingPermission")
      override fun onReceive(ctx: Context?, intent: Intent?) {
        when (intent?.action) {
          BluetoothAdapter.ACTION_STATE_CHANGED -> {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
            if (state == BluetoothAdapter.STATE_OFF) {
              _isScanning.value = false
              scope.launch { disconnect() }
            } else if (state == BluetoothAdapter.STATE_ON) {
              refreshPairedDevices()
            }
          }
          BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
            _isScanning.value = true
          }
          BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
            _isScanning.value = false
          }
          BluetoothDevice.ACTION_FOUND -> {
            if (!hasConnectPermission()) return
            val device: BluetoothDevice? =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
              }
            val rssi =
              intent
                .getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                .takeIf { it != Short.MIN_VALUE }
                ?.toInt()
            device?.let { btDevice ->
              val mapped = btDevice.toModel(rssi = rssi)
              val current = _discoveredDevices.value.toMutableList()
              val existingIndex = current.indexOfFirst { it.address == mapped.address }
              if (existingIndex >= 0) {
                current[existingIndex] = mapped
              } else {
                current.add(mapped)
              }
              _discoveredDevices.value =
                current.sortedWith(
                  compareByDescending<BluetoothPrinterDevice> { it.isPrinterClass }
                    .thenBy { it.name.lowercase() }
                )
            }
          }
        }
      }
    }

  init {
    registerReceiverIfNeeded()
    refreshState()
  }

  fun refreshState() {
    val adapter = bluetoothAdapter
    _isBluetoothSupported.value = adapter != null
    _isBluetoothEnabled.value = adapter?.isEnabled == true
    _hasPermissions.value = checkPermissions()
    if (_hasPermissions.value && _isBluetoothEnabled.value) {
      refreshPairedDevices()
    }
  }

  fun checkPermissions(): Boolean =
    requiredPermissions().all { perm ->
      ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

  private fun hasScanPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    }

  private fun hasConnectPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
        PackageManager.PERMISSION_GRANTED
    }

  private fun registerReceiverIfNeeded() {
    if (receiverRegistered) return
    val filter =
      IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        addAction(BluetoothDevice.ACTION_FOUND)
      }
    ContextCompat.registerReceiver(
      context,
      discoveryReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
    receiverRegistered = true
  }

  @SuppressLint("MissingPermission")
  fun refreshPairedDevices() {
    val adapter = bluetoothAdapter ?: return
    if (!hasConnectPermission() || !adapter.isEnabled) {
      _pairedDevices.value = emptyList()
      return
    }
    runCatching {
      val bonded = adapter.bondedDevices.orEmpty()
      _pairedDevices.value =
        bonded
          .map { it.toModel(isPairedOverride = true) }
          .sortedWith(
            compareByDescending<BluetoothPrinterDevice> { it.isPrinterClass }
              .thenBy { it.name.lowercase() }
          )
    }
  }

  @SuppressLint("MissingPermission")
  fun startScan(): Boolean {
    refreshState()
    val adapter = bluetoothAdapter ?: return false
    if (!hasScanPermission() || !hasConnectPermission() || !adapter.isEnabled) {
      return false
    }
    registerReceiverIfNeeded()
    refreshPairedDevices()
    _discoveredDevices.value = emptyList()
    return runCatching {
      if (adapter.isDiscovering) {
        adapter.cancelDiscovery()
      }
      val started = adapter.startDiscovery()
      _isScanning.value = started
      started
    }.getOrDefault(false)
  }

  @SuppressLint("MissingPermission")
  fun stopScan() {
    val adapter = bluetoothAdapter ?: return
    if (!hasScanPermission()) return
    runCatching {
      if (adapter.isDiscovering) {
        adapter.cancelDiscovery()
      }
    }
    _isScanning.value = false
  }

  @SuppressLint("MissingPermission")
  suspend fun connect(device: BluetoothPrinterDevice): Result<BluetoothPrinterDevice> =
    withContext(Dispatchers.IO) {
      ioMutex.withLock {
        val adapter =
          bluetoothAdapter
            ?: return@withContext Result.failure(
              IllegalStateException("Bluetooth hardware is not available on this device.")
            )
        if (!hasConnectPermission()) {
          return@withContext Result.failure(
            SecurityException("Bluetooth permission is required to connect.")
          )
        }
        if (!adapter.isEnabled) {
          return@withContext Result.failure(IllegalStateException("Bluetooth is turned off."))
        }

        _connectionState.value = PrinterConnectionState.Connecting(device)
        closeSocketQuietly()

        if (hasScanPermission() && adapter.isDiscovering) {
          runCatching { adapter.cancelDiscovery() }
          _isScanning.value = false
        }

        try {
          val remoteDevice = adapter.getRemoteDevice(device.address)
          val socket = openRfcommSocket(remoteDevice)
          activeSocket = socket
          val connectedModel = remoteDevice.toModel()
          _connectionState.value = PrinterConnectionState.Connected(connectedModel)
          refreshPairedDevices()
          Result.success(connectedModel)
        } catch (e: Exception) {
          closeSocketQuietly()
          val msg =
            e.localizedMessage?.takeIf { it.isNotBlank() }
              ?: "Could not connect to ${device.name}. Ensure printer is powered on and in range."
          _connectionState.value = PrinterConnectionState.Error(msg, device)
          Result.failure(IOException(msg, e))
        }
      }
    }

  @SuppressLint("MissingPermission")
  private fun openRfcommSocket(device: BluetoothDevice): BluetoothSocket {
    return try {
      val primary = device.createRfcommSocketToServiceRecord(SPP_UUID)
      primary.connect()
      primary
    } catch (primaryErr: IOException) {
      // Fallback for Chinese/OEM thermal printers with non-standard SDP records
      val method =
        device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
      val fallback = method.invoke(device, 1) as BluetoothSocket
      fallback.connect()
      fallback
    }
  }

  suspend fun disconnect() {
    withContext(Dispatchers.IO) {
      ioMutex.withLock {
        closeSocketQuietly()
        _connectionState.value = PrinterConnectionState.Disconnected
      }
    }
  }

  suspend fun sendPrintData(bytes: ByteArray): Result<Unit> =
    withContext(Dispatchers.IO) {
      ioMutex.withLock {
        val socket = activeSocket
        if (socket == null || !socket.isConnected) {
          return@withContext Result.failure(
            IllegalStateException("No printer connected. Tap Find Printer to connect first.")
          )
        }
        try {
          val outputStream = socket.outputStream
          val chunkSize = 512
          var offset = 0
          while (offset < bytes.size) {
            val count = minOf(chunkSize, bytes.size - offset)
            outputStream.write(bytes, offset, count)
            outputStream.flush()
            offset += count
            if (offset < bytes.size) {
              delay(12)
            }
          }
          Result.success(Unit)
        } catch (e: Exception) {
          closeSocketQuietly()
          _connectionState.value =
            PrinterConnectionState.Error("Print transmission failed: ${e.message ?: "Connection lost"}")
          Result.failure(e)
        }
      }
    }

  private fun closeSocketQuietly() {
    runCatching { activeSocket?.close() }
    activeSocket = null
  }

  fun release() {
    stopScan()
    closeSocketQuietly()
    if (receiverRegistered) {
      runCatching { context.unregisterReceiver(discoveryReceiver) }
      receiverRegistered = false
    }
  }

  @SuppressLint("MissingPermission")
  private fun BluetoothDevice.toModel(
    rssi: Int? = null,
    isPairedOverride: Boolean? = null,
  ): BluetoothPrinterDevice {
    val safeName =
      if (hasConnectPermission()) {
        name?.takeIf { it.isNotBlank() } ?: "Unknown Printer"
      } else {
        "Bluetooth Device"
      }
    val majorClass =
      if (hasConnectPermission()) bluetoothClass?.majorDeviceClass else null
    val looksLikePrinter =
      majorClass == BluetoothClass.Device.Major.IMAGING ||
        safeName.contains("print", ignoreCase = true) ||
        safeName.contains("pos", ignoreCase = true) ||
        safeName.contains("mpt", ignoreCase = true) ||
        safeName.contains("xp-", ignoreCase = true) ||
        safeName.contains("rp", ignoreCase = true) ||
        safeName.contains("mtp", ignoreCase = true) ||
        safeName.contains("thermal", ignoreCase = true)

    val bonded =
      isPairedOverride
        ?: if (hasConnectPermission()) bondState == BluetoothDevice.BOND_BONDED else false

    return BluetoothPrinterDevice(
      name = safeName,
      address = address,
      isPaired = bonded,
      isPrinterClass = looksLikePrinter,
      rssi = rssi,
    )
  }
}
