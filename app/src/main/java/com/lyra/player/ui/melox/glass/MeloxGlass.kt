package com.lyra.player.ui.melox.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.lyra.player.data.settings.MAX_ENHANCED_ADVANCED_BLUR_RADIUS_DP
import com.lyra.player.data.settings.MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP
import com.lyra.player.ui.effect.glass.LocalAdvancedGlassController
import com.lyra.player.ui.melox.theme.isMeloxDarkTheme

val LocalMeloxBackdrop = staticCompositionLocalOf<Backdrop?> { null }

enum class MeloxGlassMaterial { Clear, Regular }

data class MeloxGlassSpec(
    val blurRadius: Dp = 18.dp,
    val lensRadius: Dp = 24.dp,
    val refractionHeight: Dp = 12.dp,
    val useLens: Boolean = true,
    // 底栏按压透镜用不到景深：它会额外跑一遍取样，整段按住期间持续重绘。
    val useDepthEffect: Boolean = true,
)

/**
 * Kyant0's backdrop renderer needs AGSL (API 33+) and a hardware-accelerated
 * window. Below that threshold the caller must receive the solid fallback,
 * because the native shader path can abort the process rather than throw.
 */
@Composable
fun meloxGlassAvailable(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    if (!LocalView.current.isHardwareAccelerated) return false
    return LocalMeloxBackdrop.current != null
}

@Composable
fun meloxGlassBackdrop(): Backdrop? =
    if (meloxGlassAvailable()) LocalMeloxBackdrop.current else null

@Composable
fun Modifier.meloxGlassSurface(
    shape: Shape,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    pressProgress: Float = 0f,
    spec: MeloxGlassSpec = MeloxGlassSpec(),
): Modifier {
    val backdrop = meloxGlassBackdrop()
    val alphaScale = if (enabled) 1f else 0.48f
    val dark = isMeloxDarkTheme()
    val controller = LocalAdvancedGlassController.current
    val adjustableBlurRadiusDp = if (controller.isBaseBlurEnabled) {
        controller.normalizedBlurAmountDp
    } else {
        null
    }
    val effectiveSpec = if (adjustableBlurRadiusDp != null) {
        spec.copy(blurRadius = adjustableBlurRadiusDp.dp)
    } else {
        // “高级模糊效果”关闭时不再对底栏采样做模糊，保留轻微玻璃质感即可。
        spec.copy(blurRadius = 0.dp)
    }
    val effectiveSurfaceColor = if (
        adjustableBlurRadiusDp != null &&
        surfaceColor != Color.Unspecified
    ) {
        val progress = (
            (adjustableBlurRadiusDp - MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP) /
                (MAX_ENHANCED_ADVANCED_BLUR_RADIUS_DP - MIN_ENHANCED_ADVANCED_BLUR_RADIUS_DP)
            ).coerceIn(0f, 1f)
        // 模糊度越低，表面越清透，越容易看到底栏后的内容。
        surfaceColor.copy(alpha = surfaceColor.alpha * (0.25f + 0.75f * progress))
    } else {
        surfaceColor
    }
    val p = if (pressProgress.isNaN() || pressProgress.isInfinite()) {
        0f
    } else {
        pressProgress.coerceIn(0f, 1f)
    }
    val isPlain = effectiveSurfaceColor == Color.Transparent && tint == Color.Unspecified
    if (isPlain) return this

    if (backdrop == null) {
        val fallbackAlpha = if (dark) 0.84f else 0.88f
        val tintAlphaFloor = if (dark) 0.18f else 0.22f
        val stableSurface = when {
            effectiveSurfaceColor != Color.Unspecified ->
                effectiveSurfaceColor.copy(alpha = effectiveSurfaceColor.alpha * alphaScale)
            tint == Color.Unspecified -> MaterialTheme.colorScheme.surface.copy(alpha = fallbackAlpha * alphaScale)
            else -> tint.copy(alpha = maxOf(tint.alpha, tintAlphaFloor) * alphaScale)
        }
        return background(stableSurface, shape)
    }

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(effectiveSpec.blurRadius.toPx())
            if (effectiveSpec.useLens) {
                lens(
                    effectiveSpec.lensRadius.toPx(),
                    effectiveSpec.refractionHeight.toPx(),
                    depthEffect = effectiveSpec.useDepthEffect && p > 0.01f,
                    chromaticAberration = false,
                )
            }
        },
        highlight = {
            Highlight.Default.copy(
                alpha = ((if (dark) 0.32f else 0.48f) + 0.30f * p).coerceAtMost(1f),
            )
        },
        shadow = {
            Shadow(
                radius = 24.dp,
                color = Color.Black.copy(alpha = 0.12f),
                alpha = (0.08f + 0.22f * p) * if (enabled) 1f else 0.35f,
            )
        },
        innerShadow = {
            InnerShadow(
                radius = 4.dp + 8.dp * p,
                color = Color.Black.copy(alpha = 0.12f),
                alpha = (0.10f + 0.30f * p) * if (enabled) 1f else 0.35f,
            )
        },
        onDrawSurface = {
            drawRect(
                Color.White.copy(alpha = if (dark) 0.045f else 0.12f),
                blendMode = BlendMode.Screen,
            )
            if (p > 0.001f) {
                drawRect(Color.White.copy(alpha = 0.08f * p), blendMode = BlendMode.Plus)
            }
            if (tint != Color.Unspecified && tint.alpha > 0.001f) {
                drawRect(tint.copy(alpha = tint.alpha * alphaScale), blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = tint.alpha * 0.75f * alphaScale))
            }
            if (effectiveSurfaceColor != Color.Unspecified) {
                drawRect(effectiveSurfaceColor.copy(alpha = effectiveSurfaceColor.alpha * alphaScale))
            }
        },
    )
}

/**
 * 播放页下拉收起时的玻璃底：叠在每个下拉帧上采样下方页面层，
 * 刚下拉时磨砂重，越往下越清透。presence / clarity 只在绘制阶段读取，
 * 拖动过程不触发重组。backdrop 为 null（低版本或不支持）时退化为半透明暗膜。
 */
fun Modifier.meloxDragGlassVeil(
    backdrop: Backdrop?,
    presence: () -> Float,
    clarity: () -> Float,
    maxBlurRadius: Dp = 24.dp,
): Modifier {
    if (backdrop == null) {
        return drawBehind {
            val p = presence().coerceIn(0f, 1f)
            if (p <= 0.001f) return@drawBehind
            val frost = 1f - clarity().coerceIn(0f, 1f)
            drawRect(Color.Black.copy(alpha = 0.50f * frost * p))
            drawRect(Color.White.copy(alpha = 0.05f * frost * p))
        }
    }
    return graphicsLayer {
        alpha = presence().coerceIn(0f, 1f)
    }.drawBackdrop(
        backdrop = backdrop,
        shape = { RectangleShape },
        effects = {
            val frost = 1f - clarity().coerceIn(0f, 1f)
            if (frost > 0.01f) blur((maxBlurRadius * frost).toPx())
        },
        highlight = null,
        shadow = null,
        innerShadow = null,
        onDrawBackdrop = { draw ->
            if (presence() > 0.001f) draw()
        },
        onDrawSurface = {
            val frost = 1f - clarity().coerceIn(0f, 1f)
            if (frost > 0.001f) {
                drawRect(
                    Color.White.copy(alpha = 0.06f * frost),
                    blendMode = BlendMode.Screen,
                )
                drawRect(Color.Black.copy(alpha = 0.42f * frost))
            }
        },
    )
}

/**
 * Wraps page content in a layer backdrop and exposes it to the floating chrome.
 * Chrome must be a sibling drawn after the sampled page so it never samples itself.
 */
@Composable
fun MeloxGlassHost(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop = rememberLayerBackdrop(),
    page: @Composable () -> Unit,
    chrome: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            page()
        }
        CompositionLocalProvider(LocalMeloxBackdrop provides backdrop) {
            chrome()
        }
    }
}
