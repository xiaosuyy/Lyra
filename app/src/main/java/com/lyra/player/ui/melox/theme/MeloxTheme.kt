package com.lyra.player.ui.melox.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

internal val MeloxLightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    error = Color(0xFFFF3B30),
    background = Color(0xFFF7F7FA),
    surface = Color(0xFFFDFDFE),
    onPrimary = Color.White,
    onBackground = Color(0xFF17171A),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF5D5D66),
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF00427C),
    secondary = Color(0xFF5D5D66),
    secondaryContainer = Color(0xFFECECF1),
    onSecondaryContainer = Color(0xFF17171A),
    tertiary = Color(0xFF5D5D66),
    tertiaryContainer = Color(0xFFECECF1),
    onTertiaryContainer = Color(0xFF17171A),
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF8A1C16),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFD5D5DA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FC),
    surfaceContainer = Color(0xFFF2F2F6),
    surfaceContainerHigh = Color(0xFFECECF1),
    surfaceContainerHighest = Color(0xFFE5E5EA),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E5EA),
    inverseSurface = Color(0xFF17171A),
    inverseOnSurface = Color(0xFFF5F5F7),
    inversePrimary = Color(0xFF9CCBFF),
)

internal val MeloxDarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    error = Color(0xFFFF453A),
    background = Color(0xFF0B0B0D),
    surface = Color(0xFF151518),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = Color(0xFF252529),
    onSurfaceVariant = Color(0xFFB8B8C0),
    primaryContainer = Color(0xFF00427C),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFFB8B8C0),
    secondaryContainer = Color(0xFF2C2C31),
    onSecondaryContainer = Color(0xFFF5F5F7),
    tertiary = Color(0xFFB8B8C0),
    tertiaryContainer = Color(0xFF2C2C31),
    onTertiaryContainer = Color(0xFFF5F5F7),
    errorContainer = Color(0xFF5E1410),
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF3A3A3C),
    surfaceContainerLowest = Color(0xFF08080A),
    surfaceContainerLow = Color(0xFF111114),
    surfaceContainer = Color(0xFF1A1A1E),
    surfaceContainerHigh = Color(0xFF232327),
    surfaceContainerHighest = Color(0xFF2C2C31),
    surfaceBright = Color(0xFF2C2C31),
    surfaceDim = Color(0xFF08080A),
    inverseSurface = Color(0xFFF5F5F7),
    inverseOnSurface = Color(0xFF17171A),
    inversePrimary = Color(0xFF00427C),
)

/** Material3 slots only accept [androidx.compose.foundation.shape.CornerBasedShape]. */
internal val MeloxMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun MeloxTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    colorScheme: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme ?: if (darkTheme) MeloxDarkColors else MeloxLightColors,
        typography = MeloxMaterialTypography,
        shapes = MeloxMaterialShapes,
        content = content,
    )
}

/** Replaces the accent roles of the MeloX palette while keeping its neutral surfaces. */
internal fun meloxColorSchemeWithAccent(base: ColorScheme, accent: Color): ColorScheme {
    val isDark = base.background.luminance() < 0.5f
    val onAccent = if (accent.luminance() > 0.55f) Color.Black else Color.White
    val containerBase = if (isDark) Color.Black else Color.White
    val container = lerp(accent, containerBase, 0.72f)
    val onContainer = lerp(accent, if (isDark) Color.White else Color.Black, 0.42f)
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = accent,
        surfaceTint = accent,
        primaryFixed = accent,
        primaryFixedDim = accent,
        onPrimaryFixed = onAccent,
        onPrimaryFixedVariant = onContainer,
    )
}

/** Effective appearance of the Melox surface, independent of the device theme. */
@Composable
fun isMeloxDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f
