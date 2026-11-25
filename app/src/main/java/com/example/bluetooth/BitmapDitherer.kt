package com.example.bluetooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.DitherMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class DitheredImageResult(
  val previewBitmap: Bitmap,
  val escPosData: ByteArray,
  val width: Int,
  val height: Int,
  val widthBytes: Int,
)

object BitmapDitherer {
  suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
      runCatching {
        val raw =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
              decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
              decoder.isMutableRequired = true
            }
          } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
          }
        if (raw.config != Bitmap.Config.ARGB_8888) {
          raw.copy(Bitmap.Config.ARGB_8888, false)
        } else {
          raw
        }
      }.getOrNull()
    }

  suspend fun processForThermalPrinter(
    source: Bitmap,
    targetWidthDots: Int,
    ditherMode: DitherMode,
    brightnessOffset: Float, // -80f .. +80f
  ): DitheredImageResult =
    withContext(Dispatchers.Default) {
      val width = (targetWidthDots / 8) * 8
      val aspectRatio = source.height.toFloat() / source.width.toFloat().coerceAtLeast(1f)
      val height = (width * aspectRatio).roundToInt().coerceIn(8, 2400)
      val scaled = Bitmap.createScaledBitmap(source, width, height, true)

      val pixels = IntArray(width * height)
      scaled.getPixels(pixels, 0, width, 0, 0, width, height)

      // Grayscale luminance array (composite transparent pixels over white)
      val gray = FloatArray(width * height)
      for (i in pixels.indices) {
        val c = pixels[i]
        val alpha = Color.alpha(c) / 255f
        val r = Color.red(c) * alpha + 255f * (1f - alpha)
        val g = Color.green(c) * alpha + 255f * (1f - alpha)
        val b = Color.blue(c) * alpha + 255f * (1f - alpha)
        val lum = 0.299f * r + 0.587f * g + 0.114f * b + brightnessOffset
        gray[i] = lum.coerceIn(0f, 255f)
      }

      val isBlack = BooleanArray(width * height)

      when (ditherMode) {
        DitherMode.THRESHOLD -> {
          for (i in gray.indices) {
            isBlack[i] = gray[i] < 128f
          }
        }
        DitherMode.FLOYD_STEINBERG -> {
          for (y in 0 until height) {
            for (x in 0 until width) {
              val idx = y * width + x
              val oldPixel = gray[idx]
              val newPixel = if (oldPixel < 128f) 0f else 255f
              isBlack[idx] = newPixel == 0f
              val err = oldPixel - newPixel
              if (x + 1 < width) gray[idx + 1] += err * (7f / 16f)
              if (y + 1 < height) {
                if (x - 1 >= 0) gray[(y + 1) * width + (x - 1)] += err * (3f / 16f)
                gray[(y + 1) * width + x] += err * (5f / 16f)
                if (x + 1 < width) gray[(y + 1) * width + (x + 1)] += err * (1f / 16f)
              }
            }
          }
        }
        DitherMode.ATKINSON -> {
          for (y in 0 until height) {
            for (x in 0 until width) {
              val idx = y * width + x
              val oldPixel = gray[idx]
              val newPixel = if (oldPixel < 128f) 0f else 255f
              isBlack[idx] = newPixel == 0f
              val err = (oldPixel - newPixel) / 8f
              if (x + 1 < width) gray[idx + 1] += err
              if (x + 2 < width) gray[idx + 2] += err
              if (y + 1 < height) {
                if (x - 1 >= 0) gray[(y + 1) * width + (x - 1)] += err
                gray[(y + 1) * width + x] += err
                if (x + 1 < width) gray[(y + 1) * width + (x + 1)] += err
              }
              if (y + 2 < height) {
                gray[(y + 2) * width + x] += err
              }
            }
          }
        }
      }

      val previewPixels = IntArray(width * height)
      val widthBytes = width / 8
      val escPosData = ByteArray(widthBytes * height)

      for (y in 0 until height) {
        for (byteCol in 0 until widthBytes) {
          var b = 0
          for (bit in 0 until 8) {
            val x = byteCol * 8 + bit
            val idx = y * width + x
            if (isBlack[idx]) {
              b = b or (1 shl (7 - bit))
              previewPixels[idx] = Color.rgb(24, 24, 27)
            } else {
              previewPixels[idx] = Color.rgb(255, 253, 248)
            }
          }
          escPosData[y * widthBytes + byteCol] = b.toByte()
        }
      }

      val previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      previewBitmap.setPixels(previewPixels, 0, width, 0, 0, width, height)

      if (scaled !== source) {
        scaled.recycle()
      }

      DitheredImageResult(
        previewBitmap = previewBitmap,
        escPosData = escPosData,
        width = width,
        height = height,
        widthBytes = widthBytes,
      )
    }
}
