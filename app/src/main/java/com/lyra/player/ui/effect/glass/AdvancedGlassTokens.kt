package com.lyra.player.ui.effect.glass

import com.lyra.player.data.settings.EnhancedAdvancedBlurPreference
import com.lyra.player.data.settings.MAX_ENHANCED_ADVANCED_BLUR_RADIUS_DP
import com.lyra.player.data.settings.MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP

internal data class AdvancedGlassTokens(
    val blurRadiusDp: Float,
    val tintAlpha: Float,
    val edgeAlpha: Float,
    val samplesBackdrop: Boolean = true
)

/**
 * 模糊度越低，玻璃越清透，越容易看见底层画面；模糊度越高，磨砂感和着色越强。
 */
private fun glassTintAlpha(baseAlpha: Float, adjustableRadiusDp: Float?): Float {
    if (adjustableRadiusDp == null) return baseAlpha
    val progress = (
        (adjustableRadiusDp - MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP) /
            (MAX_ENHANCED_ADVANCED_BLUR_RADIUS_DP - MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP)
        ).coerceIn(0f, 1f)
    return baseAlpha * (0.25f + 0.75f * progress)
}

internal fun advancedGlassTokens(
    role: AdvancedGlassRole,
    isDarkTheme: Boolean,
    enhancedBlurRadiusDp: Float? = null
): AdvancedGlassTokens {
    val adjustableRadiusDp = enhancedBlurRadiusDp?.let(EnhancedAdvancedBlurPreference::normalize)
    fun tint(baseAlpha: Float): Float = glassTintAlpha(baseAlpha, adjustableRadiusDp)
    return when (role) {
        AdvancedGlassRole.MiniPlayer -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = tint(if (isDarkTheme) 0.30f else 0.36f),
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.BottomNavigation -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 28f,
            tintAlpha = tint(0.75f),
            edgeAlpha = 0.12f
        )
        AdvancedGlassRole.ScreenTopTab -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 22f,
            tintAlpha = tint(if (isDarkTheme) 0.16f else 0.18f),
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.SettingsGroup -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 26f,
            tintAlpha = tint(if (isDarkTheme) 0.30f else 0.34f),
            edgeAlpha = 0.12f
        )
        AdvancedGlassRole.SettingsHeader -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = tint(if (isDarkTheme) 0.28f else 0.32f),
            edgeAlpha = 0.12f
        )
        AdvancedGlassRole.SettingsSection -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = tint(if (isDarkTheme) 0.28f else 0.32f),
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.PlaylistSheet -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 24f,
            tintAlpha = tint(if (isDarkTheme) 0.24f else 0.18f),
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.SemanticCard -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 22f,
            tintAlpha = tint(if (isDarkTheme) 0.42f else 0.46f),
            edgeAlpha = 0.10f
        )
        AdvancedGlassRole.ExploreTag -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 18f,
            tintAlpha = tint(if (isDarkTheme) 0.34f else 0.38f),
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.ThemeModeToggle -> AdvancedGlassTokens(
            blurRadiusDp = adjustableRadiusDp ?: 18f,
            tintAlpha = tint(if (isDarkTheme) 0.36f else 0.40f),
            edgeAlpha = 0.08f
        )
        AdvancedGlassRole.InlineControl -> AdvancedGlassTokens(
            blurRadiusDp = 0f,
            tintAlpha = 1f,
            edgeAlpha = 0f,
            samplesBackdrop = false
        )
    }
}
