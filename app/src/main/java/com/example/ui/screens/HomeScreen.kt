package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.EscPosBuilder
import com.example.data.local.PrintOrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.PrinterConnectionState
import com.example.data.model.PrinterSettings
import com.example.ui.components.PrinterStatusCard
import com.example.ui.components.QuickActionCard
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CobaltBlueDark
import com.example.ui.theme.CobaltBlueLight
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.RoseError
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.VioletContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  connectionState: PrinterConnectionState,
  settings: PrinterSettings,
  recentOrders: List<PrintOrderEntity>,
  onFindPrinter: () -> Unit,
  onDisconnect: () -> Unit,
  onTestPrint: () -> Unit,
  onNewOrder: () -> Unit,
  onPrintText: () -> Unit,
  onPrintImages: () -> Unit,
  onPrintReceipts: () -> Unit,
  onViewAllOrders: () -> Unit,
  onReprintOrder: (PrintOrderEntity) -> Unit,
  onOpenOrderPreview: (PrintOrderEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Top Brand Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Print,
              contentDescription = stringResource(R.string.app_name),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }

    // Active Printer Status Card
    item {
      PrinterStatusCard(
        connectionState = connectionState,
        paperWidth = settings.paperWidth,
        onFindPrinter = onFindPrinter,
        onDisconnect = onDisconnect,
        onTestPrint = onTestPrint,
      )
    }

    // Large Blue "Find Printer" Hero Button
    item {
      Surface(
        onClick = onFindPrinter,
        modifier = Modifier.fillMaxWidth().testTag("find_printer_button"),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        color = Color.Transparent,
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                brush = Brush.horizontalGradient(colors = listOf(CobaltBlue, CobaltBlueDark))
              )
              .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier =
                  Modifier.size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.BluetoothSearching,
                  contentDescription = stringResource(R.string.find_printer),
                  tint = Color.White,
                  modifier = Modifier.size(28.dp),
                )
              }
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = stringResource(R.string.find_printer),
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = stringResource(R.string.find_printer_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.88f),
                )
              }
            }
            Icon(
              imageVector = Icons.Default.ChevronRight,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(26.dp),
            )
          }
        }
      }
    }

    // Quick Print Actions Grid (2x2)
    item {
      Text(
        text = stringResource(R.string.quick_actions),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp),
      )
      Spacer(modifier = Modifier.height(10.dp))
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          QuickActionCard(
            title = stringResource(R.string.action_new_order),
            subtitle = stringResource(R.string.action_new_order_desc),
            icon = Icons.Default.AddShoppingCart,
            iconTint = CobaltBlue,
            iconContainerColor = CobaltBlueLight,
            onClick = onNewOrder,
            testTag = "quick_new_order_card",
            modifier = Modifier.weight(1f),
          )
          QuickActionCard(
            title = stringResource(R.string.action_print_text),
            subtitle = stringResource(R.string.action_print_text_desc),
            icon = Icons.Default.Description,
            iconTint = EmeraldConnected,
            iconContainerColor = EmeraldContainer,
            onClick = onPrintText,
            testTag = "quick_print_text_card",
            modifier = Modifier.weight(1f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          QuickActionCard(
            title = stringResource(R.string.action_print_images),
            subtitle = stringResource(R.string.action_print_images_desc),
            icon = Icons.Default.Image,
            iconTint = VioletAccent,
            iconContainerColor = VioletContainer,
            onClick = onPrintImages,
            testTag = "quick_print_images_card",
            modifier = Modifier.weight(1f),
          )
          QuickActionCard(
            title = stringResource(R.string.action_print_receipts),
            subtitle = stringResource(R.string.action_print_receipts_desc),
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            iconTint = AmberAccent,
            iconContainerColor = AmberContainer,
            onClick = onPrintReceipts,
            testTag = "quick_print_receipts_card",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // Recent Orders Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.recent_orders),
          style = MaterialTheme.typography.titleMedium,
        )
        TextButton(
          onClick = onViewAllOrders,
          modifier = Modifier.testTag("home_view_all_orders_button"),
        ) {
          Text(stringResource(R.string.view_all))
        }
      }
    }

    if (recentOrders.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.no_orders_yet),
              style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = stringResource(R.string.no_orders_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(recentOrders, key = { it.id }) { order ->
        HomeRecentOrderCard(
          order = order,
          onClick = { onOpenOrderPreview(order) },
          onReprint = { onReprintOrder(order) },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@Composable
private fun HomeRecentOrderCard(
  order: PrintOrderEntity,
  onClick: () -> Unit,
  onReprint: () -> Unit,
) {
  val status = OrderStatus.fromName(order.status)
  val statusColor =
    when (status) {
      OrderStatus.PRINTED -> EmeraldConnected
      OrderStatus.SAVED -> CobaltBlue
      OrderStatus.FAILED -> RoseError
    }
  val dateStr =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(order.createdAt))

  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().testTag("recent_order_${order.id}"),
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = order.orderNumber,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.12f),
          ) {
            Text(
              text = status.label,
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = order.businessName,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text =
            buildString {
              append(dateStr)
              if (order.totalAmount > 0.0) {
                append(" • ")
                append(EscPosBuilder.formatMoney(order.totalAmount, order.currencySymbol))
              }
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      FilledTonalButton(
        onClick = onReprint,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors =
          ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
          ),
        modifier = Modifier.testTag("reprint_order_${order.id}"),
      ) {
        Icon(
          imageVector = Icons.Default.Replay,
          contentDescription = stringResource(R.string.reprint),
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.reprint), style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}
