package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = WarmAmberSecondary,
    onSecondary = WarmAmberOnSecondary,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = WarmAmberOnSecondaryContainer,
    tertiary = FreshTealTertiary,
    onTertiary = FreshTealOnTertiary,
    tertiaryContainer = FreshTealContainer,
    onTertiaryContainer = FreshTealOnTertiaryContainer,
    background = CreamBackground,
    onBackground = InkOnSurface,
    surface = CreamSurface,
    onSurface = InkOnSurface,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = MintOutline,
    error = SemanticError,
    errorContainer = SemanticErrorContainer,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = WarmAmberDark,
    onSecondary = WarmAmberOnDark,
    secondaryContainer = WarmAmberContainerDark,
    onSecondaryContainer = WarmAmberOnContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun BukuSembakoTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
