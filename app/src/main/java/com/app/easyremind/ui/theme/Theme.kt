package com.app.easyremind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = SurfaceLight,
    primaryContainer = SecondaryLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = SecondaryLight,
    onSecondary = TextPrimaryLight,
    tertiary = AccentLight,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = BgDark,
    primaryContainer = SecondaryDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = BgDark,
    tertiary = AccentDark,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = BgDark,
)

@Composable
fun EasyRemindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

private val AppShapes = androidx.compose.material3.Shapes(
    extraSmall = SmallShape,
    small = SmallShape,
    medium = MediumShape,
    large = LargeShape,
    extraLarge = HeroShape,
)