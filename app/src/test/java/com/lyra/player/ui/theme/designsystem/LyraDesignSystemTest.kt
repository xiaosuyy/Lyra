package com.lyra.player.ui.theme.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyraDesignSystemTest {

    @Test
    fun `spacing tokens are ordered and positive`() {
        val values = listOf(
            LyraSpacing.XS, LyraSpacing.SM, LyraSpacing.MD,
            LyraSpacing.LG, LyraSpacing.XL, LyraSpacing.XXL
        )
        assertTrue(values.all { it > 0.dp })
        assertEquals(values.sortedBy { it.value }, values)
    }

    @Test
    fun `radius tokens are ordered and positive`() {
        val values = listOf(
            LyraRadius.Small, LyraRadius.Medium, LyraRadius.Large, LyraRadius.XLarge
        )
        assertTrue(values.all { it > 0.dp })
        assertEquals(values.sortedBy { it.value }, values)
    }

    @Test
    fun `icon touch target is at least 48dp`() {
        assertTrue(LyraIconSize.TouchTarget >= 48.dp)
        assertTrue(LyraIconSize.XLarge > LyraIconSize.Large)
        assertTrue(LyraIconSize.Large > LyraIconSize.Medium)
    }

    @Test
    fun `color tokens derive from color scheme`() {
        val scheme = lightColorScheme()
        val colors = neriColorsFrom(scheme, isDark = false)
        assertEquals(scheme.background, colors.background)
        assertEquals(scheme.primary, colors.accent)
        assertEquals(scheme.onSurface, colors.primaryText)
        assertEquals(scheme.onSurfaceVariant, colors.secondaryText)
        assertEquals(scheme.outlineVariant, colors.divider)
        assertEquals(scheme.error, colors.error)
    }

    @Test
    fun `dark color tokens use dark scheme`() {
        val dark = darkColorScheme()
        val colors = neriColorsFrom(dark, isDark = true)
        assertEquals(dark.background, colors.background)
        assertEquals(dark.surfaceContainerHigh, colors.elevatedSurface)
    }

    @Test
    fun `typography establishes clear hierarchy`() {
        val t = neriTypography()
        assertEquals(FontWeight.SemiBold, t.titleLarge.fontWeight)
        assertEquals(FontWeight.SemiBold, t.headlineSmall.fontWeight)
        assertTrue(t.displayLarge.fontSize.value > t.bodyLarge.fontSize.value)

        val styles = neriTextStylesFrom(t)
        assertEquals(t.displaySmall, styles.display)
        assertEquals(t.titleLarge, styles.title)
        assertEquals(t.bodyLarge, styles.body)
        assertEquals(t.bodyMedium, styles.bodySecondary)
    }
}
