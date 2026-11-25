package com.example.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.BluetoothPrinterManager
import com.example.data.model.BluetoothPrinterDevice
import com.example.data.model.PrinterConnectionState
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.EmeraldContainer

@Composable
fun PrinterDiscoveryScreen(
  isBluetoothSupported: Boolean,
  isBluetoothEnabled: Boolean,
  hasPermissions: Boolean,
  isScanning: Boolean,
  pairedDevices: List<BluetoothPrinterDevice>,
  discoveredDevices: List<BluetoothPrinterDevice>,
  connectionState: PrinterConnectionState,
  onBack: () -> Unit,
  onPermissionsResult: () -> Unit,
  onStartScan: () -> Unit,
  onStopScan: () -> Unit,
  onConnect: (BluetoothPrinterDevice) -> Unit,
  onDisconnect: () -> Unit,
  onTestPrint: () -> Unit,
) {
  val context = LocalContext.current

  val permissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
      onPermissionsResult()
    }

  val enableBtLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      onPermissionsResult()
    }

  DisposableEffect(Unit) {
    onPermissionsResult()
    onDispose { onStopScan() }
  }

  Scaffold(
    topBar = {
      SubScreenTopBar(
        title = stringResource(R.string.printer_discovery_title),
        onBack = onBack,
        actions = {
          IconButton(
            onClick = {
              context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            modifier = Modifier.testTag("open_bt_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = stringResource(R.string.open_bt_settings),
            )
          }
        },
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Visual Hero Banner
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column {
            Image(
              painter = painterResource(id = R.drawable.img_printer_banner),
              contentDescription = "Bluetooth Thermal Printer",
              modifier = Modifier.fillMaxWidth().height(145.dp),
              contentScale = ContentScale.Crop,
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    when (connectionState) {
                      is PrinterConnectionState.Connected ->
                        "Connected: ${connectionState.device.name}"
                      is PrinterConnectionState.Connecting ->
                        "Connecting to ${connectionState.device.name}…"
                      else -> "58mm & 80mm ESC/POS Ready"
                    },
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text =
                    when (connectionState) {
                      is PrinterConnectionState.Connected -> connectionState.device.address
                      is PrinterConnectionState.Error -> connectionState.message
                      else -> "Turn on your Bluetooth printer before scanning"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color =
                    if (connectionState is PrinterConnectionState.Error)
                      MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (connectionState is PrinterConnectionState.Connected) {
                FilledTonalButton(
                  onClick = onTestPrint,
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("discovery_test_print_button"),
                ) {
                  Text(stringResource(R.string.print_test_page))
                }
              }
            }
          }
        }
      }

      // Hardware / Permission / Adapter Checks
      if (!isBluetoothSupported) {
        item {
          InfoActionCard(
            title = "Bluetooth Unavailable",
            description = stringResource(R.string.bluetooth_unavailable),
            buttonLabel = stringResource(R.string.open_bt_settings),
            onAction = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            testTag = "bt_unavailable_settings_btn",
          )
        }
      } else if (!hasPermissions) {
        item {
          InfoActionCard(
            title = stringResource(R.string.bluetooth_permission_required),
            description = stringResource(R.string.bluetooth_permission_rationale),
            buttonLabel = stringResource(R.string.grant_permission),
            onAction = {
              permissionLauncher.launch(BluetoothPrinterManager.requiredPermissions())
            },
            testTag = "grant_bt_permission_button",
          )
        }
      } else if (!isBluetoothEnabled) {
        item {
          InfoActionCard(
            title = stringResource(R.string.bluetooth_off_title),
            description = stringResource(R.string.bluetooth_off_desc),
            buttonLabel = stringResource(R.string.enable_bluetooth),
            onAction = {
              enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
            testTag = "enable_bt_button",
          )
        }
      } else {
        // Large Scan / Stop Button
        item {
          Button(
            onClick = { if (isScanning) onStopScan() else onStartScan() },
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("scan_printers_button"),
            shape = RoundedCornerShape(16.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = if (isScanning) MaterialTheme.colorScheme.secondary else CobaltBlue
              ),
          ) {
            if (isScanning) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onSecondary,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(stringResource(R.string.scanning_printers))
              Spacer(modifier = Modifier.width(8.dp))
              Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop_scan))
            } else {
              Icon(Icons.Default.BluetoothSearching, contentDescription = null)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.scan_nearby),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      // Paired Devices Section
      item {
        Text(
          text = stringResource(R.string.paired_printers),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (pairedDevices.isEmpty()) {
        item {
          EmptyDeviceCard(text = stringResource(R.string.no_paired_devices))
        }
      } else {
        items(pairedDevices, key = { "paired_${it.address}" }) { device ->
          PrinterDeviceRow(
            device = device,
            connectionState = connectionState,
            onConnect = { onConnect(device) },
            onDisconnect = onDisconnect,
          )
        }
      }

      // Discovered Devices Section
      item {
        Text(
          text = stringResource(R.string.nearby_printers),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      if (discoveredDevices.isEmpty()) {
        item {
          EmptyDeviceCard(
            text =
              if (isScanning) stringResource(R.string.scanning_printers)
              else stringResource(R.string.no_nearby_devices)
          )
        }
      } else {
        items(discoveredDevices, key = { "disc_${it.address}" }) { device ->
          PrinterDeviceRow(
            device = device,
            connectionState = connectionState,
            onConnect = { onConnect(device) },
            onDisconnect = onDisconnect,
          )
        }
      }
    }
  }
}

@Composable
private fun InfoActionCard(
  title: String,
  description: String,
  buttonLabel: String,
  onAction: () -> Unit,
  testTag: String,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Button(
        onClick = onAction,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag(testTag),
      ) {
        Text(buttonLabel)
      }
    }
  }
}

@Composable
private fun EmptyDeviceCard(text: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(16.dp),
    )
  }
}

@Composable
private fun PrinterDeviceRow(
  device: BluetoothPrinterDevice,
  connectionState: PrinterConnectionState,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
) {
  val isConnected =
    connectionState is PrinterConnectionState.Connected &&
      connectionState.device.address == device.address
  val isConnecting =
    connectionState is PrinterConnectionState.Connecting &&
      connectionState.device.address == device.address

  Card(
    modifier = Modifier.fillMaxWidth().testTag("device_row_${device.address}"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isConnected) EmeraldConnected.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                if (isConnected) EmeraldContainer
                else if (device.isPrinterClass) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector =
              when {
                isConnected -> Icons.Default.BluetoothConnected
                device.isPrinterClass -> Icons.Default.Print
                else -> Icons.Default.Bluetooth
              },
            contentDescription = device.name,
            tint =
              when {
                isConnected -> EmeraldConnected
                device.isPrinterClass -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = device.name,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (device.isPrinterClass) {
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "Printer",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text =
              buildString {
                append(device.address)
                device.rssi?.let { append(" • ${it} dBm") }
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      when {
        isConnecting -> {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        }
        isConnected -> {
          OutlinedButton(
            onClick = onDisconnect,
            shape = RoundedCornerShape(10.dp),
            colors =
              ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          ) {
            Text(stringResource(R.string.disconnect))
          }
        }
        else -> {
          Button(
            onClick = onConnect,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          ) {
            Text(stringResource(R.string.connect))
          }
        }
      }
    }
  }
}
