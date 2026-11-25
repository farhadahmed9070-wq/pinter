package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.PaperWidth
import com.example.data.model.PrintDensity
import com.example.data.model.PrinterConnectionState
import com.example.data.model.PrinterEncoding
import com.example.data.model.PrinterSettings
import com.example.util.LocaleHelper
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: PrinterSettings,
  connectionState: PrinterConnectionState,
  onFindPrinter: () -> Unit,
  onPaperWidthChange: (PaperWidth) -> Unit,
  onPrintDensityChange: (PrintDensity) -> Unit,
  onAutoCutChange: (Boolean) -> Unit,
  onCashDrawerChange: (Boolean) -> Unit,
  onFeedLinesChange: (Int) -> Unit,
  onAutoReconnectChange: (Boolean) -> Unit,
  onEncodingChange: (PrinterEncoding) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onTestPrint: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = stringResource(R.string.settings_title),
        style = MaterialTheme.typography.headlineMedium,
      )
    }

    // Bluetooth Settings Section
    item {
      SettingsCard(title = stringResource(R.string.section_bluetooth), icon = Icons.Default.Bluetooth) {
        val statusText =
          when (connectionState) {
            is PrinterConnectionState.Connected ->
              "Connected: ${connectionState.device.name} (${connectionState.device.address})"
            is PrinterConnectionState.Connecting -> "Connecting to ${connectionState.device.name}…"
            is PrinterConnectionState.Error -> connectionState.message
            PrinterConnectionState.Disconnected ->
              if (settings.lastPrinterName.isNotBlank())
                "Last printer: ${settings.lastPrinterName} (${settings.lastPrinterAddress})"
              else "No printer connected"
          }
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.auto_reconnect),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Switch(
            checked = settings.autoReconnect,
            onCheckedChange = onAutoReconnectChange,
            modifier = Modifier.testTag("auto_reconnect_switch"),
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = onFindPrinter,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("settings_find_printer_btn"),
          ) {
            Text(stringResource(R.string.find_printer))
          }
          OutlinedButton(
            onClick = { LocaleHelper.openBluetoothSettings(context) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("settings_system_bt_btn"),
          ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("BT Panel")
          }
        }
      }
    }

    // Printer Configuration Section
    item {
      SettingsCard(
        title = stringResource(R.string.section_printer_config),
        icon = Icons.Default.Print,
      ) {
        Text(
          text = stringResource(R.string.paper_size),
          style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PaperWidth.entries.forEach { width ->
            FilterChip(
              selected = settings.paperWidth == width,
              onClick = { onPaperWidthChange(width) },
              label = { Text(width.label) },
              modifier = Modifier.testTag("paper_size_${width.mm}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = stringResource(R.string.print_density),
          style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PrintDensity.entries.forEach { density ->
            FilterChip(
              selected = settings.printDensity == density,
              onClick = { onPrintDensityChange(density) },
              label = { Text(density.label) },
              modifier = Modifier.testTag("density_${density.name}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Character Encoding", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PrinterEncoding.entries.forEach { enc ->
            FilterChip(
              selected = settings.encoding == enc,
              onClick = { onEncodingChange(enc) },
              label = { Text(enc.label) },
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.feed_lines),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = "${settings.feedLines} lines",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = settings.feedLines.toFloat(),
          onValueChange = { onFeedLinesChange(it.roundToInt()) },
          valueRange = 0f..8f,
          steps = 7,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.auto_cut),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Switch(checked = settings.autoCut, onCheckedChange = onAutoCutChange)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.cash_drawer),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
          )
          Switch(checked = settings.cashDrawer, onCheckedChange = onCashDrawerChange)
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
          onClick = onTestPrint,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("settings_test_print_button"),
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.print_test_page))
        }
      }
    }

    // Language Section
    item {
      SettingsCard(title = stringResource(R.string.section_language), icon = Icons.Default.Language) {
        Text(
          text = stringResource(R.string.app_language),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AppLanguage.entries.forEach { lang ->
            FilterChip(
              selected = settings.language == lang,
              onClick = { onLanguageChange(lang) },
              label = { Text(lang.nativeName) },
              modifier = Modifier.testTag("lang_${lang.code}"),
            )
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun SettingsCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}
