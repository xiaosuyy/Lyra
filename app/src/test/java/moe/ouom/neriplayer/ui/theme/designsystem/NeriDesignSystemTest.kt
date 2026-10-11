package moe.ouom.neriplayer.ui.theme.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeriDesignSystemTest {

    @Test
    fun `spacing tokens are ordered and positive`() {
        val values = listOf(
            NeriSpacing.XS, NeriSpacing.SM, NeriSpacing.MD,
            NeriSpacing.LG, NeriSpacing.XL, NeriSpacing.XXL
        )
        assertTrue(values.all { it > 0.dp })
        assertEquals(values.sortedBy { it.value }, values)
    }

    @Test
    fun `radius tokens are ordered and positive`() {
        val values = listOf(
            NeriRadius.Small, NeriRadius.Medium, NeriRadius.Large, NeriRadius.XLarge
        )
        assertTrue(values.all { it > 0.dp })
        assertEquals(values.sortedBy { it.value }, values)
    }

    @Test
    fun `icon touch target is at least 48dp`() {
        assertTrue(NeriIconSize.TouchTarget >= 48.dp)
        assertTrue(NeriIconSize.XLarge > NeriIconSize.Large)
        assertTrue(NeriIconSize.Large > NeriIconSize.Medium)
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
