package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.EscPosBuilder
import com.example.data.local.PrintOrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaperWidth
import com.example.ui.components.ThermalPaperPreview
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.RoseError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(
  orders: List<PrintOrderEntity>,
  onNewOrder: () -> Unit,
  onReprintOrder: (PrintOrderEntity) -> Unit,
  onDeleteOrder: (Long) -> Unit,
  initialPreviewOrder: PrintOrderEntity? = null,
  onDismissInitialPreview: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
  var previewOrder by remember(initialPreviewOrder) { mutableStateOf(initialPreviewOrder) }

  val filteredOrders =
    remember(orders, searchQuery, selectedStatusFilter) {
      orders.filter { order ->
        val matchesStatus =
          selectedStatusFilter == null || OrderStatus.fromName(order.status) == selectedStatusFilter
        val q = searchQuery.trim()
        val matchesQuery =
          q.isEmpty() ||
            order.orderNumber.contains(q, ignoreCase = true) ||
            order.businessName.contains(q, ignoreCase = true) ||
            order.customerName.contains(q, ignoreCase = true) ||
            order.previewText.contains(q, ignoreCase = true)
        matchesStatus && matchesQuery
      }
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onNewOrder,
        containerColor = CobaltBlue,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("orders_new_order_fab"),
      ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_order))
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = stringResource(R.string.orders_title),
          style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text(stringResource(R.string.search_orders)) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("orders_search_input"),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = selectedStatusFilter == null,
            onClick = { selectedStatusFilter = null },
            label = { Text(stringResource(R.string.filter_all)) },
            modifier = Modifier.testTag("filter_all_chip"),
          )
          FilterChip(
            selected = selectedStatusFilter == OrderStatus.PRINTED,
            onClick = { selectedStatusFilter = OrderStatus.PRINTED },
            label = { Text(stringResource(R.string.filter_printed)) },
            modifier = Modifier.testTag("filter_printed_chip"),
          )
          FilterChip(
            selected = selectedStatusFilter == OrderStatus.SAVED,
            onClick = { selectedStatusFilter = OrderStatus.SAVED },
            label = { Text(stringResource(R.string.filter_saved)) },
            modifier = Modifier.testTag("filter_saved_chip"),
          )
          FilterChip(
            selected = selectedStatusFilter == OrderStatus.FAILED,
            onClick = { selectedStatusFilter = OrderStatus.FAILED },
            label = { Text(stringResource(R.string.filter_failed)) },
            modifier = Modifier.testTag("filter_failed_chip"),
          )
        }
      }

      if (filteredOrders.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.no_orders_yet),
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.no_orders_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(filteredOrders, key = { it.id }) { order ->
            OrderHistoryCard(
              order = order,
              onPreview = { previewOrder = order },
              onReprint = { onReprintOrder(order) },
              onDelete = { onDeleteOrder(order.id) },
            )
          }
        }
      }
    }
  }

  previewOrder?.let { order ->
    AlertDialog(
      onDismissRequest = {
        previewOrder = null
        onDismissInitialPreview()
      },
      title = { Text("${order.orderNumber} • ${order.businessName}") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          ThermalPaperPreview(
            text = order.previewText,
            paperWidth = PaperWidth.fromMm(order.paperWidthMm),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onReprintOrder(order)
            previewOrder = null
            onDismissInitialPreview()
          }
        ) {
          Text(stringResource(R.string.reprint))
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            previewOrder = null
            onDismissInitialPreview()
          }
        ) {
          Text(stringResource(R.string.close))
        }
      },
    )
  }
}

@Composable
private fun OrderHistoryCard(
  order: PrintOrderEntity,
  onPreview: () -> Unit,
  onReprint: () -> Unit,
  onDelete: () -> Unit,
) {
  val status = OrderStatus.fromName(order.status)
  val orderType = OrderType.fromName(order.orderType)
  val statusColor =
    when (status) {
      OrderStatus.PRINTED -> EmeraldConnected
      OrderStatus.SAVED -> CobaltBlue
      OrderStatus.FAILED -> RoseError
    }
  val dateStr =
    SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(order.createdAt))
  val itemCount = order.parsedItems().sumOf { it.quantity }

  Card(
    onClick = onPreview,
    modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = order.orderNumber,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = orderType.label,
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusColor.copy(alpha = 0.14f),
        ) {
          Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = order.businessName,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (order.customerName.isNotBlank()) {
        Text(
          text = "Customer: ${order.customerName}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text =
          buildString {
            append(dateStr)
            append(" • ${order.paperWidthMm}mm")
            if (itemCount > 0) append(" • $itemCount items")
          },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        if (order.totalAmount > 0.0) {
          Text(
            text = EscPosBuilder.formatMoney(order.totalAmount, order.currencySymbol),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
        } else {
          Text(
            text = order.printerName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onPreview, modifier = Modifier.testTag("preview_order_${order.id}")) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = stringResource(R.string.preview),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_order_${order.id}")) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = stringResource(R.string.delete),
              tint = MaterialTheme.colorScheme.error,
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          FilledTonalButton(
            onClick = onReprint,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          ) {
            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.reprint))
          }
        }
      }
    }
  }
}
