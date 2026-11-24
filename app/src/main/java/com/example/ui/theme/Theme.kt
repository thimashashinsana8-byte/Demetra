package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DemetraLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SoftWhite,
    primaryContainer = SageTint,
    onPrimaryContainer = DeepGreen,
    secondary = SageGreen,
    onSecondary = SoftWhite,
    secondaryContainer = GoldTint,
    onSecondaryContainer = DarkText,
    tertiary = HarvestGold,
    onTertiary = DarkText,
    tertiaryContainer = GoldTint,
    onTertiaryContainer = DarkText,
    background = WarmCream,
    onBackground = DarkText,
    surface = SoftCreamSurface,
    onSurface = DarkText,
    surfaceVariant = SageTint,
    onSurfaceVariant = MutedText,
    outline = CreamCardBorder,
    outlineVariant = CreamCardBorder,
    error = ErrorTerracotta,
    onError = SoftWhite,
    errorContainer = TerracottaTint,
    onErrorContainer = ErrorTerracotta
)

val DemetraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DemetraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DemetraLightColorScheme,
        typography = DemetraTypography,
        shapes = DemetraShapes,
        content = content
    )
}
