package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PaperWidth
import com.example.data.model.PrinterConnectionState
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.ReceiptMonoFontFamily
import com.example.ui.theme.ThermalPaperBackground
import com.example.ui.theme.ThermalPaperInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenTopBar(
  title: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable () -> Unit = {},
) {
  CenterAlignedTopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    navigationIcon = {
      IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.back),
        )
      }
    },
    actions = { actions() },
    colors =
      TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.background
      ),
    modifier = modifier,
  )
}

@Composable
fun PrinterStatusCard(
  connectionState: PrinterConnectionState,
  paperWidth: PaperWidth,
  onFindPrinter: () -> Unit,
  onDisconnect: () -> Unit,
  onTestPrint: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isConnected = connectionState is PrinterConnectionState.Connected
  val isConnecting = connectionState is PrinterConnectionState.Connecting

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isConnected) EmeraldConnected.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
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
              Modifier.size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                  if (isConnected) EmeraldContainer
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
          ) {
            if (isConnecting) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
              )
            } else {
              Icon(
                imageVector =
                  if (isConnected) Icons.Default.BluetoothConnected
                  else Icons.Default.BluetoothDisabled,
                contentDescription = "Printer connection status",
                tint =
                  if (isConnected) EmeraldConnected
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            when (connectionState) {
              is PrinterConnectionState.Connected -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier =
                      Modifier.size(8.dp).clip(CircleShape).background(EmeraldConnected)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = stringResource(R.string.printer_connected),
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldConnected,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
                Text(
                  text = connectionState.device.name,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = connectionState.device.address,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              is PrinterConnectionState.Connecting -> {
                Text(
                  text = stringResource(R.string.printer_connecting),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = connectionState.device.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              is PrinterConnectionState.Error -> {
                Text(
                  text = stringResource(R.string.printer_disconnected),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = connectionState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.error,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              PrinterConnectionState.Disconnected -> {
                Text(
                  text = stringResource(R.string.printer_disconnected),
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = stringResource(R.string.printer_tap_to_connect),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "${paperWidth.mm}mm",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      if (isConnected) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onTestPrint,
            modifier = Modifier.weight(1f).testTag("status_test_print_button"),
            shape = RoundedCornerShape(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Print,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.print_test_page))
          }
          OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.weight(1f).testTag("status_disconnect_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
          ) {
            Text(stringResource(R.string.disconnect))
          }
        }
      } else if (connectionState is PrinterConnectionState.Error) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
          onClick = onFindPrinter,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
        ) {
          Text(stringResource(R.string.find_printer))
        }
      }
    }
  }
}

@Composable
fun QuickActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  iconContainerColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Box(
        modifier =
          Modifier.size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(iconContainerColor),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconTint,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ThermalPaperPreview(
  text: String,
  paperWidth: PaperWidth,
  modifier: Modifier = Modifier,
) {
  val borderColor = MaterialTheme.colorScheme.outlineVariant
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.live_preview),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Text(
          text = "${paperWidth.mm}mm • ${paperWidth.columns} cols",
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    Surface(
      modifier =
        Modifier.fillMaxWidth(if (paperWidth == PaperWidth.MM_58) 0.92f else 1f),
      color = Color.Transparent,
      shadowElevation = 2.dp,
    ) {
      Column {
        SerratedEdge(isTop = true, paperColor = ThermalPaperBackground, strokeColor = borderColor)
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .background(ThermalPaperBackground)
              .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Text(
            text = text.ifBlank { " " },
            fontFamily = ReceiptMonoFontFamily,
            color = ThermalPaperInk,
            fontSize = if (paperWidth == PaperWidth.MM_58) 12.sp else 10.5.sp,
            lineHeight = if (paperWidth == PaperWidth.MM_58) 17.sp else 15.sp,
          )
        }
        SerratedEdge(isTop = false, paperColor = ThermalPaperBackground, strokeColor = borderColor)
      }
    }
  }
}

@Composable
private fun SerratedEdge(
  isTop: Boolean,
  paperColor: Color,
  strokeColor: Color,
) {
  Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
    val teethCount = 24
    val toothWidth = size.width / teethCount
    val path = Path()
    if (isTop) {
      path.moveTo(0f, size.height)
      for (i in 0 until teethCount) {
        val xStart = i * toothWidth
        path.lineTo(xStart + toothWidth / 2f, 0f)
        path.lineTo(xStart + toothWidth, size.height)
      }
      path.close()
    } else {
      path.moveTo(0f, 0f)
      for (i in 0 until teethCount) {
        val xStart = i * toothWidth
        path.lineTo(xStart + toothWidth / 2f, size.height)
        path.lineTo(xStart + toothWidth, 0f)
      }
      path.close()
    }
    drawPath(path = path, color = paperColor)
  }
}
