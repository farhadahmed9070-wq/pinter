package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.bluetooth.DitheredImageResult
import com.example.data.model.DitherMode
import com.example.data.model.PrinterSettings
import com.example.data.model.TextAlignOption
import com.example.ui.components.SubScreenTopBar
import com.example.ui.theme.ThermalPaperBackground
import kotlin.math.roundToInt

@Composable
fun PrintImageScreen(
  settings: PrinterSettings,
  ditheredResult: DitheredImageResult?,
  ditherMode: DitherMode,
  brightness: Float,
  imageAlign: TextAlignOption,
  isPrinting: Boolean,
  onBack: () -> Unit,
  onImagePicked: (Uri) -> Unit,
  onDitherModeChange: (DitherMode) -> Unit,
  onBrightnessChange: (Float) -> Unit,
  onAlignChange: (TextAlignOption) -> Unit,
  onPrintImage: () -> Unit,
) {
  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
      onResult = { uri -> uri?.let(onImagePicked) },
    )

  Scaffold(
    topBar = {
      SubScreenTopBar(title = stringResource(R.string.print_image_title), onBack = onBack)
    },
    bottomBar = {
      Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1f).height(52.dp).testTag("pick_gallery_image_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.select_image))
          }

          Button(
            onClick = onPrintImage,
            enabled = ditheredResult != null && !isPrinting,
            modifier = Modifier.weight(1f).height(52.dp).testTag("print_image_submit_button"),
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
              Spacer(modifier = Modifier.width(6.dp))
            }
            Text(stringResource(R.string.print_now))
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
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      if (ditheredResult == null) {
        Card(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth().testTag("empty_image_picker_card"),
          shape = RoundedCornerShape(22.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier =
                Modifier.size(64.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = stringResource(R.string.no_image_selected),
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.no_image_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        // Dither Mode Selector
        Text(text = stringResource(R.string.dither_mode), style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          DitherMode.entries.forEach { mode ->
            FilterChip(
              selected = ditherMode == mode,
              onClick = { onDitherModeChange(mode) },
              label = { Text(mode.label) },
            )
          }
        }

        // Alignment
        Text(text = stringResource(R.string.alignment), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextAlignOption.entries.forEach { option ->
            FilterChip(
              selected = imageAlign == option,
              onClick = { onAlignChange(option) },
              label = { Text(option.label) },
            )
          }
        }

        // Brightness Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = stringResource(R.string.brightness),
              style = MaterialTheme.typography.labelLarge,
            )
            Text(
              text = "${brightness.roundToInt()}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = -80f..80f,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        // Live Thermal Raster Preview
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = stringResource(R.string.live_preview), style = MaterialTheme.typography.labelLarge)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = "${settings.paperWidth.mm}mm • ${ditheredResult.width}×${ditheredResult.height}px",
              style = MaterialTheme.typography.labelSmall,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = ThermalPaperBackground),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          val ratio =
            (ditheredResult.width.toFloat() / ditheredResult.height.toFloat().coerceAtLeast(1f))
              .coerceIn(0.35f, 3f)
          Image(
            bitmap = ditheredResult.previewBitmap.asImageBitmap(),
            contentDescription = "Thermal Dithered Preview",
            modifier =
              Modifier.fillMaxWidth()
                .padding(16.dp)
                .aspectRatio(ratio),
            contentScale = ContentScale.Fit,
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
