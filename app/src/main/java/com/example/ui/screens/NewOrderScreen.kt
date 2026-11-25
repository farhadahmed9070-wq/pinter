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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
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

private data class EditableLineItem(
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
fun NewOrderScreen(
  settings: PrinterSettings,
  templates: List<ReceiptTemplateEntity>,
  nextOrderNumberProvider: suspend () -> String,
  onBack: () -> Unit,
  onSaveOrPrint: (
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
  var orderNumber by remember { mutableStateOf("ORD-1002") }
  var customerName by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var selectedTemplate by remember { mutableStateOf<ReceiptTemplateEntity?>(null) }
  var businessName by remember { mutableStateOf("MY STORE") }
  var currencySymbol by remember { mutableStateOf("$") }
  var taxPercentText by remember { mutableStateOf("5") }
  var discountText by remember { mutableStateOf("0.00") }

  val lineItems = remember {
    mutableStateListOf(
      EditableLineItem(name = "Product A", quantity = "1", price = "12.00"),
      EditableLineItem(name = "Service Fee", quantity = "1", price = "3.50"),
    )
  }

  LaunchedEffect(Unit) { orderNumber = nextOrderNumberProvider() }

  LaunchedEffect(templates) {
    if (selectedTemplate == null && templates.isNotEmpty()) {
      val def = templates.firstOrNull { it.isDefault } ?: templates.first()
      selectedTemplate = def
      businessName = def.businessName
      currencySymbol = def.currencySymbol
      taxPercentText = def.defaultTaxPercent.toString()
    }
  }

  val parsedItems = lineItems.map { it.toOrderItem() }.filter { it.name.isNotBlank() }
  val subtotal = parsedItems.sumOf { it.lineTotal }
  val discount = discountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val taxPercent = taxPercentText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val taxable = (subtotal - discount).coerceAtLeast(0.0)
  val taxAmount = taxable * (taxPercent / 100.0)
  val total = taxable + taxAmount
  val nowStr = remember {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
  }

  val previewText =
    EscPosBuilder.formatReceiptPreview(
      ReceiptPrintPayload(
        businessName = businessName,
        businessAddress = selectedTemplate?.businessAddress ?: "",
        businessPhone = selectedTemplate?.businessPhone ?: "",
        headerText = "ORDER SUMMARY",
        orderNumber = orderNumber,
        customerName = customerName,
        dateTimeText = nowStr,
        items = parsedItems,
        subtotal = subtotal,
        taxPercent = taxPercent,
        taxAmount = taxAmount,
        discountAmount = discount,
        totalAmount = total,
        currencySymbol = currencySymbol,
        notes = notes,
        footerText = selectedTemplate?.footerText ?: "Thank you!",
        headerAlign = TextAlignOption.CENTER,
      ),
      settings.paperWidth,
    )

  Scaffold(
    topBar = {
      SubScreenTopBar(title = stringResource(R.string.new_order_title), onBack = onBack)
    },
    bottomBar = {
      Surface(
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = {
              onSaveOrPrint(
                orderNumber,
                OrderType.ORDER,
                customerName,
                businessName,
                selectedTemplate?.businessAddress ?: "",
                selectedTemplate?.businessPhone ?: "",
                "ORDER SUMMARY",
                selectedTemplate?.footerText ?: "Thank you!",
                parsedItems,
                taxPercent,
                discount,
                currencySymbol,
                notes,
                TextAlignOption.CENTER,
                false,
                nowStr,
                false,
              )
            },
            modifier = Modifier.weight(1f).height(50.dp).testTag("save_order_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.save_order))
          }

          Button(
            onClick = {
              onSaveOrPrint(
                orderNumber,
                OrderType.ORDER,
                customerName,
                businessName,
                selectedTemplate?.businessAddress ?: "",
                selectedTemplate?.businessPhone ?: "",
                "ORDER SUMMARY",
                selectedTemplate?.footerText ?: "Thank you!",
                parsedItems,
                taxPercent,
                discount,
                currencySymbol,
                notes,
                TextAlignOption.CENTER,
                false,
                nowStr,
                true,
              )
            },
            modifier = Modifier.weight(1.2f).height(50.dp).testTag("save_and_print_order_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.save_and_print))
          }
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
              selected = selectedTemplate?.id == tpl.id,
              onClick = {
                selectedTemplate = tpl
                businessName = tpl.businessName
                currencySymbol = tpl.currencySymbol
                taxPercentText = tpl.defaultTaxPercent.toString()
              },
              label = { Text(tpl.templateName) },
            )
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = orderNumber,
          onValueChange = { orderNumber = it },
          label = { Text(stringResource(R.string.order_number)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("order_number_input"),
        )
        OutlinedTextField(
          value = businessName,
          onValueChange = { businessName = it },
          label = { Text(stringResource(R.string.business_name)) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1.4f),
        )
      }

      OutlinedTextField(
        value = customerName,
        onValueChange = { customerName = it },
        label = { Text(stringResource(R.string.customer_name)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("order_customer_input"),
      )

      // Order Items Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = "Order Items", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { lineItems.add(EditableLineItem()) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_order_item_button"),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(stringResource(R.string.add_item))
        }
      }

      lineItems.forEachIndexed { index, item ->
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
                value = item.name,
                onValueChange = { lineItems[index] = item.copy(name = it) },
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("order_item_name_$index"),
              )
              if (lineItems.size > 1) {
                IconButton(onClick = { lineItems.removeAt(index) }) {
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
                value = item.quantity,
                onValueChange = { lineItems[index] = item.copy(quantity = it) },
                label = { Text(stringResource(R.string.qty)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = item.price,
                onValueChange = { lineItems[index] = item.copy(price = it) },
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

      // Tax, Discount & Summary Card
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
          label = { Text("${stringResource(R.string.discount)} ($currencySymbol)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text(stringResource(R.string.order_notes)) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Card(
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          SummaryRow(stringResource(R.string.subtotal), EscPosBuilder.formatMoney(subtotal, currencySymbol))
          if (discount > 0.0) {
            SummaryRow(stringResource(R.string.discount), "-${EscPosBuilder.formatMoney(discount, currencySymbol)}")
          }
          SummaryRow(stringResource(R.string.tax), EscPosBuilder.formatMoney(taxAmount, currencySymbol))
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.total),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = EscPosBuilder.formatMoney(total, currencySymbol),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      ThermalPaperPreview(text = previewText, paperWidth = settings.paperWidth)
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun SummaryRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
    Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
  }
}
