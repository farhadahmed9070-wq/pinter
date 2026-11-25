package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.data.model.TextSizeOption
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.ThermalPaperPreview

@Composable
fun PrintTextScreen(
  settings: PrinterSettings,
  isPrinting: Boolean,
  onBack: () -> Unit,
  onPrintText: (
    text: String,
    align: TextAlignOption,
    size: TextSizeOption,
    bold: Boolean,
    underline: Boolean,
  ) -> Unit,
) {
  var text by remember {
    mutableStateOf("EXPRESS PARCEL\nOrder #4821\nFragile - Handle With Care\nPriority Same-Day Delivery")
  }
  var align by remember { mutableStateOf(TextAlignOption.CENTER) }
  var size by remember { mutableStateOf(TextSizeOption.NORMAL) }
  var bold by remember { mutableStateOf(false) }
  var underline by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      SubScreenTopBar(title = stringResource(R.string.print_text_title), onBack = onBack)
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Button(
          onClick = { onPrintText(text, align, size, bold, underline) },
          enabled = !isPrinting && text.isNotBlank(),
          modifier =
            Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("print_text_submit_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          if (isPrinting) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
          } else {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
          }
          Text(stringResource(R.string.print_now))
        }
      }
    },
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Quick Preset Chips
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AssistChip(
          onClick = {
            text = "SHIPPING LABEL\nTo: Alex Mercer\n742 Evergreen Terrace\nApt 4B • ZIP 90210"
            align = TextAlignOption.LEFT
            bold = true
          },
          label = { Text("Shipping Label") },
        )
        AssistChip(
          onClick = {
            text = "TABLE #12\n2x Iced Latte\n1x Avocado Toast\n--- Kitchen Ticket ---"
            align = TextAlignOption.CENTER
            bold = true
          },
          label = { Text("Kitchen Ticket") },
        )
        AssistChip(
          onClick = {
            text = "GUEST WI-FI\nNetwork: Studio_5G\nPass: Welcome2026"
            align = TextAlignOption.CENTER
            size = TextSizeOption.DOUBLE_HEIGHT
          },
          label = { Text("Wi-Fi Card") },
        )
        AssistChip(
          onClick = { text = "" },
          label = { Text("Clear") },
        )
      }

      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.print_text_title)) },
        placeholder = { Text(stringResource(R.string.enter_text_hint)) },
        minLines = 5,
        maxLines = 12,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("print_text_input"),
      )

      // Alignment & Style
      Text(text = stringResource(R.string.alignment), style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TextAlignOption.entries.forEach { option ->
          val icon =
            when (option) {
              TextAlignOption.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
              TextAlignOption.CENTER -> Icons.Default.FormatAlignCenter
              TextAlignOption.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
            }
          FilterChip(
            selected = align == option,
            onClick = { align = option },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text(option.label) },
          )
        }
      }

      Text(text = stringResource(R.string.font_size), style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TextSizeOption.entries.forEach { option ->
          FilterChip(
            selected = size == option,
            onClick = { size = option },
            label = { Text(option.label) },
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FilterChip(
          selected = bold,
          onClick = { bold = !bold },
          leadingIcon = {
            Icon(Icons.Default.FormatBold, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          label = { Text(stringResource(R.string.bold)) },
          modifier = Modifier.testTag("bold_toggle_chip"),
        )
        FilterChip(
          selected = underline,
          onClick = { underline = !underline },
          leadingIcon = {
            Icon(
              Icons.Default.FormatUnderlined,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          },
          label = { Text(stringResource(R.string.underline)) },
          modifier = Modifier.testTag("underline_toggle_chip"),
        )
      }

      ThermalPaperPreview(text = text, paperWidth = settings.paperWidth)
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
