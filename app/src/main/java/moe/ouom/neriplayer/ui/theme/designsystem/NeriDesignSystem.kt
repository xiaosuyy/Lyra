package moe.ouom.neriplayer.ui.theme.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: moe.ouom.neriplayer.ui.theme.designsystem/NeriDesignSystem
 * Created: 2026/9/28
 */

// ---------------- Spacing ----------------

/** 统一间距 Token。页面请使用这些值，避免零散 dp。 */
object NeriSpacing {
    val XS: Dp = 4.dp
    val SM: Dp = 8.dp
    val MD: Dp = 12.dp
    val LG: Dp = 16.dp
    val XL: Dp = 24.dp
    val XXL: Dp = 32.dp
}

// ---------------- Radius ----------------

/** 统一圆角 Token。 */
object NeriRadius {
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val XLarge: Dp = 24.dp
    val Full: Dp = 999.dp
}

// ---------------- Elevation ----------------

/** 克制的层级：优先用 Surface/半透明/模糊，而不是重阴影。 */
object NeriElevation {
    val None: Dp = 0.dp
    val Low: Dp = 1.dp
    val Medium: Dp = 3.dp
    val High: Dp = 6.dp
}

// ---------------- Icon ----------------

/** 统一图标尺寸与触控目标。 */
object NeriIconSize {
    val Small: Dp = 16.dp
    val Medium: Dp = 20.dp
    val Large: Dp = 24.dp
    val XLarge: Dp = 32.dp
    /** 推荐最小触控目标。 */
    val TouchTarget: Dp = 48.dp
}

// ---------------- Color ----------------

/** 语义化颜色 Token（从 Material ColorScheme 归一，页面不直接写 Color.White/Black/Gray）。 */
@Immutable
data class NeriColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val divider: Color,
    val accent: Color,
    val error: Color,
    val scrim: Color
)

/** 由 ColorScheme 推导语义色；`isDark` 用于微调层级。 */
fun neriColorsFrom(scheme: ColorScheme, isDark: Boolean): NeriColors = NeriColors(
    background = scheme.background,
    surface = scheme.surface,
    elevatedSurface = if (isDark) scheme.surfaceContainerHigh else scheme.surfaceContainerLow,
    primaryText = scheme.onSurface,
    secondaryText = scheme.onSurfaceVariant,
    tertiaryText = scheme.onSurfaceVariant.copy(alpha = 0.6f),
    divider = scheme.outlineVariant,
    accent = scheme.primary,
    error = scheme.error,
    scrim = scheme.scrim
)

val LocalNeriColors = staticCompositionLocalOf { neriColorsFrom(lightColorScheme(), isDark = false) }

// ---------------- Typography ----------------

/** 语义文字层级（从 [Typography] 归一，供组件统一使用）。 */
@Immutable
data class NeriTextStyles(
    val display: TextStyle,
    val largeTitle: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val caption: TextStyle,
    val captionSecondary: TextStyle
)

/**
 * 建立 App 统一字体层级：现代、简洁、高可读性，标题与正文层级明显，避免过度粗体。
 */
fun neriTypography(base: Typography = Typography()): Typography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Normal),
    bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Normal),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium)
)

/** 从 [Typography] 推导语义层级。 */
fun neriTextStylesFrom(t: Typography): NeriTextStyles = NeriTextStyles(
    display = t.displaySmall,
    largeTitle = t.headlineLarge,
    title = t.titleLarge,
    headline = t.headlineSmall,
    body = t.bodyLarge,
    bodySecondary = t.bodyMedium,
    caption = t.labelLarge,
    captionSecondary = t.labelMedium
)

val NeriTypography = neriTypography()

val LocalNeriTextStyles = staticCompositionLocalOf { neriTextStylesFrom(Typography()) }
