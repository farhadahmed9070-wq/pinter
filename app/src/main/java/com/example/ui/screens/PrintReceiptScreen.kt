package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.OrderType
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.ThermalPaperPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class ReceiptLineDraft(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "",
  val quantity: String = "1",
  val price: String = "0.00",
) {
  fun toOrderItem(): OrderLineItem =
    OrderLineItem(
      id = id,
      name = name.trim(),
      quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1,
      unitPrice = price.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
    )
}

@Composable
fun PrintReceiptScreen(
  settings: PrinterSettings,
  templates: List<ReceiptTemplateEntity>,
  initialTemplateId: Long?,
  isPrinting: Boolean,
  nextOrderNumberProvider: suspend () -> String,
  onBack: () -> Unit,
  onPrintReceipt: (
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
  ) -> Unit,
) {
  var selectedTemplateId by remember { mutableStateOf(initialTemplateId) }
  var businessName by remember { mutableStateOf("NORTHSIDE MARKET") }
  var businessAddress by remember { mutableStateOf("240 Oak Street, Suite 4B") }
  var businessPhone by remember { mutableStateOf("Tel: (555) 234-8910") }
  var headerText by remember { mutableStateOf("SALES RECEIPT") }
  var footerText by remember { mutableStateOf("Thank you for shopping with us!") }
  var orderNumber by remember { mutableStateOf("RCP-1001") }
  var dateTimeText by remember {
    mutableStateOf(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
  }
  var currencySymbol by remember { mutableStateOf("$") }
  var taxPercentText by remember { mutableStateOf("8.0") }
  var discountText by remember { mutableStateOf("0.00") }
  var includeQr by remember { mutableStateOf(false) }
  var headerAlign by remember { mutableStateOf(TextAlignOption.CENTER) }

  val items = remember {
    mutableStateListOf(
      ReceiptLineDraft(name = "Coffee Beans 250g", quantity = "1", price = "14.50"),
      ReceiptLineDraft(name = "Oat Milk", quantity = "2", price = "3.25"),
    )
  }

  LaunchedEffect(Unit) {
    orderNumber = nextOrderNumberProvider().replace("ORD", "RCP")
  }

  LaunchedEffect(templates, initialTemplateId) {
    val tpl =
      templates.firstOrNull { it.id == selectedTemplateId }
        ?: templates.firstOrNull { it.isDefault }
        ?: templates.firstOrNull()
    if (tpl != null) {
      selectedTemplateId = tpl.id
      businessName = tpl.businessName
      businessAddress = tpl.businessAddress
      businessPhone = tpl.businessPhone
      headerText = tpl.headerText
      footerText = tpl.footerText
      currencySymbol = tpl.currencySymbol
      taxPercentText = tpl.defaultTaxPercent.toString()
      includeQr = tpl.showBarcodeFooter
      headerAlign = TextAlignOption.fromName(tpl.headerAlign)
    }
  }

  val parsedItems = items.map { it.toOrderItem() }.filter { it.name.isNotBlank() }
  val subtotal = parsedItems.sumOf { it.lineTotal }
  val discount = discountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val taxPercent = taxPercentText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val taxable = (subtotal - discount).coerceAtLeast(0.0)
  val taxAmount = taxable * (taxPercent / 100.0)
  val total = taxable + taxAmount

  val previewText =
    EscPosBuilder.formatReceiptPreview(
      ReceiptPrintPayload(
        businessName = businessName,
        businessAddress = businessAddress,
        businessPhone = businessPhone,
        headerText = headerText,
        orderNumber = orderNumber,
        customerName = "",
        dateTimeText = dateTimeText,
        items = parsedItems,
        subtotal = subtotal,
        taxPercent = taxPercent,
        taxAmount = taxAmount,
        discountAmount = discount,
        totalAmount = total,
        currencySymbol = currencySymbol,
        notes = "",
        footerText = footerText,
        headerAlign = headerAlign,
        includeQrFooter = includeQr,
      ),
      settings.paperWidth,
    )

  Scaffold(
    topBar = {
      SubScreenTopBar(title = stringResource(R.string.print_receipt_title), onBack = onBack)
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Button(
          onClick = {
            onPrintReceipt(
              orderNumber,
              OrderType.RECEIPT,
              "",
              businessName,
              businessAddress,
              businessPhone,
              headerText,
              footerText,
              parsedItems,
              taxPercent,
              discount,
              currencySymbol,
              "",
              headerAlign,
              includeQr,
              dateTimeText,
              true,
            )
          },
          enabled = !isPrinting && parsedItems.isNotEmpty(),
          modifier =
            Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("print_receipt_submit_button"),
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (templates.isNotEmpty()) {
        Text(
          text = stringResource(R.string.select_template),
          style = MaterialTheme.typography.labelLarge,
        )
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          templates.forEach { tpl ->
            FilterChip(
              selected = selectedTemplateId == tpl.id,
              onClick = {
                selectedTemplateId = tpl.id
                businessName = tpl.businessName
                businessAddress = tpl.businessAddress
                businessPhone = tpl.businessPhone
                headerText = tpl.headerText
                footerText = tpl.footerText
                currencySymbol = tpl.currencySymbol
                taxPercentText = tpl.defaultTaxPercent.toString()
                includeQr = tpl.showBarcodeFooter
                headerAlign = TextAlignOption.fromName(tpl.headerAlign)
              },
              label = { Text(tpl.templateName) },
            )
          }
        }
      }

      OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text(stringResource(R.string.business_name)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("receipt_business_name_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = orderNumber,
          onValueChange = { orderNumber = it },
          label = { Text(stringResource(R.string.order_number)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = dateTimeText,
          onValueChange = { dateTimeText = it },
          label = { Text(stringResource(R.string.receipt_date)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1.4f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = "Items", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { items.add(ReceiptLineDraft()) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("receipt_add_item_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(stringResource(R.string.add_item))
        }
      }

      items.forEachIndexed { idx, line ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              OutlinedTextField(
                value = line.name,
                onValueChange = { items[idx] = line.copy(name = it) },
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              if (items.size > 1) {
                IconButton(onClick = { items.removeAt(idx) }) {
                  Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = line.quantity,
                onValueChange = { items[idx] = line.copy(quantity = it) },
                label = { Text(stringResource(R.string.qty)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = line.price,
                onValueChange = { items[idx] = line.copy(price = it) },
                label = { Text("${stringResource(R.string.price)} ($currencySymbol)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.4f),
              )
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = taxPercentText,
          onValueChange = { taxPercentText = it },
          label = { Text("${stringResource(R.string.tax)} %") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = discountText,
          onValueChange = { discountText = it },
          label = { Text(stringResource(R.string.discount)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = stringResource(R.string.include_qr), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = includeQr, onCheckedChange = { includeQr = it })
      }

      ThermalPaperPreview(text = previewText, paperWidth = settings.paperWidth)
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
