package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkCobaltBlue,
    onPrimary = Color(0xFF00205C),
    primaryContainer = DarkCobaltContainer,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF0F3D38),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF3B1D7A),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0A0F1C),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF182238),
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF233047),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CobaltBlue,
    onPrimary = Color.White,
    primaryContainer = CobaltBlueLight,
    onPrimaryContainer = CobaltBlueDark,
    secondary = EmeraldConnected,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = VioletAccent,
    onTertiary = Color.White,
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = Color(0xFF3B0764),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBFF),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFF0F4FA),
    outline = LightOutline,
    outlineVariant = Color(0xFFE5ECF4),
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseContainer,
    onErrorContainer = Color(0xFF7F1D1D),
  )

val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun BluetoothPrintTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
