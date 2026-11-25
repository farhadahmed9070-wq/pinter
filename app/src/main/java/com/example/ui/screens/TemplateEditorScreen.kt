package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.EscPosBuilder
import com.example.bluetooth.ReceiptPrintPayload
import com.example.data.local.ReceiptTemplateEntity
import com.example.data.model.OrderLineItem
import com.example.data.model.PaperWidth
import com.example.data.model.TextAlignOption
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.ThermalPaperPreview

@Composable
fun TemplateEditorScreen(
  templateId: Long,
  loadTemplate: suspend (Long) -> ReceiptTemplateEntity?,
  onBack: () -> Unit,
  onSaveTemplate: (ReceiptTemplateEntity) -> Unit,
) {
  var templateName by remember { mutableStateOf("") }
  var businessName by remember { mutableStateOf("") }
  var businessAddress by remember { mutableStateOf("") }
  var businessPhone by remember { mutableStateOf("") }
  var headerText by remember { mutableStateOf("SALES RECEIPT") }
  var footerText by remember { mutableStateOf("Thank you for your business!") }
  var currencySymbol by remember { mutableStateOf("$") }
  var defaultTax by remember { mutableStateOf("5.0") }
  var paperWidth by remember { mutableStateOf(PaperWidth.MM_58) }
  var headerAlign by remember { mutableStateOf(TextAlignOption.CENTER) }
  var showDateTime by remember { mutableStateOf(true) }
  var showOrderNumber by remember { mutableStateOf(true) }
  var showBarcodeFooter by remember { mutableStateOf(false) }
  var barcodeData by remember { mutableStateOf("POS-VERIFIED") }
  var isDefault by remember { mutableStateOf(false) }

  LaunchedEffect(templateId) {
    if (templateId > 0L) {
      loadTemplate(templateId)?.let { tpl ->
        templateName = tpl.templateName
        businessName = tpl.businessName
        businessAddress = tpl.businessAddress
        businessPhone = tpl.businessPhone
        headerText = tpl.headerText
        footerText = tpl.footerText
        currencySymbol = tpl.currencySymbol
        defaultTax = tpl.defaultTaxPercent.toString()
        paperWidth = PaperWidth.fromMm(tpl.paperWidthMm)
        headerAlign = TextAlignOption.fromName(tpl.headerAlign)
        showDateTime = tpl.showDateTime
        showOrderNumber = tpl.showOrderNumber
        showBarcodeFooter = tpl.showBarcodeFooter
        barcodeData = tpl.barcodeData
        isDefault = tpl.isDefault
      }
    }
  }

  val taxPct = defaultTax.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val sampleItems =
    listOf(
      OrderLineItem("1", "Sample Item One", 2, 7.50),
      OrderLineItem("2", "Sample Item Two", 1, 5.00),
    )
  val sampleSubtotal = sampleItems.sumOf { it.lineTotal }
  val sampleTax = sampleSubtotal * (taxPct / 100.0)

  val previewText =
    EscPosBuilder.formatReceiptPreview(
      ReceiptPrintPayload(
        businessName = businessName.ifBlank { "YOUR BUSINESS NAME" },
        businessAddress = businessAddress,
        businessPhone = businessPhone,
        headerText = headerText,
        orderNumber = if (showOrderNumber) "ORD-1001" else "",
        customerName = "",
        dateTimeText = if (showDateTime) "2026-11-24 14:30" else "",
        items = sampleItems,
        subtotal = sampleSubtotal,
        taxPercent = taxPct,
        taxAmount = sampleTax,
        discountAmount = 0.0,
        totalAmount = sampleSubtotal + sampleTax,
        currencySymbol = currencySymbol.ifBlank { "$" },
        notes = "",
        footerText = footerText,
        headerAlign = headerAlign,
        includeQrFooter = showBarcodeFooter,
        qrData = barcodeData,
      ),
      paperWidth,
    )

  Scaffold(
    topBar = {
      SubScreenTopBar(
        title =
          if (templateId > 0L) stringResource(R.string.edit_template)
          else stringResource(R.string.create_template),
        onBack = onBack,
      )
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Button(
          onClick = {
            onSaveTemplate(
              ReceiptTemplateEntity(
                id = if (templateId > 0L) templateId else 0L,
                templateName = templateName.ifBlank { businessName.ifBlank { "Custom Template" } },
                businessName = businessName.ifBlank { "MY STORE" },
                businessAddress = businessAddress,
                businessPhone = businessPhone,
                headerText = headerText,
                footerText = footerText,
                currencySymbol = currencySymbol.ifBlank { "$" },
                defaultTaxPercent = taxPct,
                paperWidthMm = paperWidth.mm,
                showDateTime = showDateTime,
                showOrderNumber = showOrderNumber,
                showBarcodeFooter = showBarcodeFooter,
                barcodeData = barcodeData,
                headerAlign = headerAlign.name,
                isDefault = isDefault,
              )
            )
          },
          modifier =
            Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("save_template_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Default.Save, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.save_template))
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedTextField(
        value = templateName,
        onValueChange = { templateName = it },
        label = { Text(stringResource(R.string.template_name)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("template_name_input"),
      )

      OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text(stringResource(R.string.business_name)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("template_business_input"),
      )

      OutlinedTextField(
        value = businessAddress,
        onValueChange = { businessAddress = it },
        label = { Text(stringResource(R.string.business_address)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = businessPhone,
        onValueChange = { businessPhone = it },
        label = { Text(stringResource(R.string.business_phone)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = headerText,
        onValueChange = { headerText = it },
        label = { Text("Header Label") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = currencySymbol,
          onValueChange = { currencySymbol = it.take(3) },
          label = { Text(stringResource(R.string.currency_symbol)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = defaultTax,
          onValueChange = { defaultTax = it },
          label = { Text(stringResource(R.string.default_tax)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1.3f),
        )
      }

      Text(text = stringResource(R.string.paper_size), style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaperWidth.entries.forEach { width ->
          FilterChip(
            selected = paperWidth == width,
            onClick = { paperWidth = width },
            label = { Text(width.label) },
          )
        }
      }

      Text(text = stringResource(R.string.alignment), style = MaterialTheme.typography.labelLarge)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextAlignOption.entries.forEach { align ->
          FilterChip(
            selected = headerAlign == align,
            onClick = { headerAlign = align },
            label = { Text(align.label) },
          )
        }
      }

      OutlinedTextField(
        value = footerText,
        onValueChange = { footerText = it },
        label = { Text(stringResource(R.string.footer_message)) },
        minLines = 2,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      TemplateSwitchRow("Show Date & Time", showDateTime) { showDateTime = it }
      TemplateSwitchRow("Show Order Number", showOrderNumber) { showOrderNumber = it }
      TemplateSwitchRow("Include QR Verification Code", showBarcodeFooter) {
        showBarcodeFooter = it
      }
      if (showBarcodeFooter) {
        OutlinedTextField(
          value = barcodeData,
          onValueChange = { barcodeData = it },
          label = { Text("QR Code Content / URL") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      TemplateSwitchRow("Set as Default Template", isDefault) { isDefault = it }

      Spacer(modifier = Modifier.height(6.dp))
      ThermalPaperPreview(text = previewText, paperWidth = paperWidth)
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun TemplateSwitchRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
