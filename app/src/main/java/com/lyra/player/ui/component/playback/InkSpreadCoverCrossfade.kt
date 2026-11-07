package com.lyra.player.ui.component.playback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * 墨水扩散换图，移植自 LeiBox "Ink Spread Carousel" 的墨迹遮罩路径
 * （https://leibox.pantoria.cn/docs/animations/ink-spread-carousel，设计参考 Lorenzo Cabra）。
 * 原实现为 WebGL 噪声场 + 透镜折射，这里保留不规则墨迹边缘与缓出节奏。
 */
private const val InkSpreadDurationMs = 700
private const val InkSpreadRoughness = 0.6f
private const val InkSpreadSeed = 1f
private const val InkSpreadWobbleSteps = 128

private val InkSpreadEasing = Easing { fraction -> 1f - (1f - fraction).pow(1.7f) }

@Composable
internal fun InkSpreadCoverCrossfade(
    targetUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cover: @Composable (url: String, contentDescription: String?, modifier: Modifier) -> Unit
) {
    var fromUrl by remember { mutableStateOf<String?>(null) }
    var toUrl by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(targetUrl) {
        val previous = toUrl
        if (previous == targetUrl) return@LaunchedEffect
        if (previous == null || targetUrl == null) {
            progress.snapTo(1f)
            fromUrl = null
            toUrl = targetUrl
            return@LaunchedEffect
        }
        fromUrl = previous
        toUrl = targetUrl
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = InkSpreadDurationMs, easing = InkSpreadEasing)
        )
        fromUrl = null
    }

    Box(modifier = modifier) {
        val from = fromUrl
        val to = toUrl
        when {
            to == null -> Unit
            from == null -> cover(to, contentDescription, Modifier.fillMaxSize())
            else -> {
                cover(from, null, Modifier.fillMaxSize())
                cover(
                    to,
                    contentDescription,
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipPath(inkSpreadRevealPath(size, progress.value)) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )
            }
        }
    }
}

internal fun inkSpreadRevealPath(
    size: Size,
    progress: Float,
    roughness: Float = InkSpreadRoughness,
    seed: Float = InkSpreadSeed
): Path {
    val width = size.width
    val height = size.height
    val path = Path()
    if (width <= 0f || height <= 0f) return path

    // 从左上角起笔：半径以高度为单位，需覆盖整条对角线。
    val reach = hypot(width, height) / height
    val growth = -0.12f + (reach + 0.12f) * progress
    val strength = roughness * sin(progress * PI.toFloat())
    val centerX = 0f
    val centerY = 0f
    for (index in 0..InkSpreadWobbleSteps) {
        val angle = index.toFloat() / InkSpreadWobbleSteps * 2f * PI.toFloat()
        val wobble = (
            sin(angle * 4f + seed) * 0.04f +
                cos(angle * 7f + seed * 0.71f) * 0.035f +
                sin(angle * 13f + progress * 5f) * 0.015f
            ) * strength
        val radius = max(0f, growth + wobble) * height
        val x = centerX + cos(angle) * radius
        val y = centerY + sin(angle) * radius
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
