package com.lyra.player.ui.melox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lyra.player.ui.melox.glass.MeloxGlassSpec
import com.lyra.player.ui.melox.glass.meloxGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 迷你播放器横滑预览用的曲目快照。 */
data class MeloxMiniTrack(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)

private const val MiniSwipeCommitScreenFraction = 0.28f
private const val MiniSwipeMaxScreenFraction = 0.86f
private const val MiniSwipeSettleTimeoutMs = 1_200L

private fun miniSwipeSmoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Floating glass bar used above the tab dock.
 * [compactProgress] morphs it from the wide card into the inline pill beside the tab circle.
 *
 * 左右滑动封面/歌名区域可切换上下首：松手越过阈值后当前条目滑出、相邻条目滑入，
 * 底部播放按钮保持不动。
 */
@Composable
fun MeloxMiniPlayer(
    title: String,
    artist: String,
    coverUrl: String?,
    isPlaying: Boolean,
    playPauseEnabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    compactProgress: Float = 0f,
    onPrevious: () -> Unit = {},
    previousTrack: MeloxMiniTrack? = null,
    nextTrack: MeloxMiniTrack? = null,
    onCoverBoundsChanged: ((Rect) -> Unit)? = null,
) {
    val compact = compactProgress.coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(lerp(24.dp, 22.dp, compact))
    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    // 相邻条目按内容区自身宽度贴着当前条目排布；用屏幕宽度会在中间留出空白。
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    val swipeSpanPx = if (rowWidthPx > 0f) rowWidthPx else screenWidthPx
    val commitThresholdPx = swipeSpanPx * MiniSwipeCommitScreenFraction
    val scope = rememberCoroutineScope()

    val contentOffset = remember { Animatable(0f) }
    var accumulatedPx by remember { mutableFloatStateOf(0f) }
    var pendingDirection by remember { mutableIntStateOf(0) }
    var committedTrack by remember { mutableStateOf<MeloxMiniTrack?>(null) }
    var committedDirection by remember { mutableIntStateOf(0) }

    val currentTrackKey = "$title|$artist|$coverUrl"
    val liveDirection = when {
        contentOffset.value > 1f -> 1
        contentOffset.value < -1f -> -1
        else -> 0
    }
    val previewDirection = if (pendingDirection != 0) committedDirection else liveDirection
    val previewTrack = when {
        pendingDirection != 0 -> committedTrack
        liveDirection > 0 -> previousTrack
        liveDirection < 0 -> nextTrack
        else -> null
    }
    val adjacentAlpha = miniSwipeSmoothStep(
        abs(contentOffset.value) / (swipeSpanPx * 0.5f)
    )

    fun resetSwipe() {
        pendingDirection = 0
        committedTrack = null
        committedDirection = 0
        scope.launch {
            contentOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            )
        }
    }

    // 相邻曲目已落到播放状态：把新条目钉回原位，撤掉预览层。
    LaunchedEffect(currentTrackKey) {
        contentOffset.snapTo(0f)
        pendingDirection = 0
        committedTrack = null
        committedDirection = 0
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(lerp(48.dp, 44.dp, compact))
            .meloxGlassSurface(
                shape = barShape,
                surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                spec = MeloxGlassSpec(blurRadius = 8.dp, lensRadius = 20.dp, refractionHeight = 10.dp),
            )
            .clickable(onClick = onExpand)
            .padding(horizontal = lerp(6.dp, 5.dp, compact)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val coverSize = lerp(36.dp, 34.dp, compact)
        val coverShape = RoundedCornerShape(lerp(10.dp, coverSize / 2f, compact))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .onGloballyPositioned { rowWidthPx = it.size.width.toFloat() }
                .pointerInput(swipeSpanPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            accumulatedPx = 0f
                            if (pendingDirection != 0) {
                                pendingDirection = 0
                                committedTrack = null
                                committedDirection = 0
                            }
                            scope.launch {
                                contentOffset.stop()
                                contentOffset.snapTo(0f)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedPx += dragAmount
                            val target = accumulatedPx.coerceIn(
                                -swipeSpanPx * MiniSwipeMaxScreenFraction,
                                swipeSpanPx * MiniSwipeMaxScreenFraction,
                            )
                            scope.launch {
                                contentOffset.snapTo(target)
                            }
                        },
                        onDragCancel = {
                            accumulatedPx = 0f
                            resetSwipe()
                        },
                        onDragEnd = {
                            val direction = when {
                                accumulatedPx <= -commitThresholdPx -> -1
                                accumulatedPx >= commitThresholdPx -> 1
                                else -> 0
                            }
                            accumulatedPx = 0f
                            val target = when {
                                direction > 0 -> previousTrack
                                direction < 0 -> nextTrack
                                else -> null
                            }
                            if (direction != 0 && target != null) {
                                committedTrack = target
                                committedDirection = direction
                                pendingDirection = direction
                                scope.launch {
                                    contentOffset.animateTo(
                                        targetValue = direction * swipeSpanPx,
                                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 380f),
                                    )
                                    if (direction > 0) onPrevious() else onNext()
                                    // 条目滑入后若曲目仍未切换（无上一首/加载失败），回弹。
                                    delay(MiniSwipeSettleTimeoutMs)
                                    if (pendingDirection != 0) resetSwipe()
                                }
                            } else {
                                resetSwipe()
                            }
                        },
                    )
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = contentOffset.value },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(coverSize)
                        .onGloballyPositioned { coordinates ->
                            // 未真正落位时会报 (0,0,0,0)；这种值会把飞行落点带到屏幕角落。
                            val bounds = coordinates.boundsInWindow()
                            if (bounds.width > 0.5f && bounds.height > 0.5f) {
                                onCoverBoundsChanged?.invoke(bounds)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    MiniPlayerArtwork(
                        coverUrl = coverUrl,
                        coverShape = coverShape,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = lerp(10.dp, 9.dp, compact)),
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier,
                    )
                    if (compact < 0.99f) {
                        Text(
                            text = artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 1f - compact,
                            ),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            val preview = previewTrack
            if (preview != null) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = adjacentAlpha
                            translationX = contentOffset.value - previewDirection * swipeSpanPx
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(coverSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiniPlayerArtwork(
                            coverUrl = preview.coverUrl,
                            coverShape = coverShape,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = lerp(10.dp, 9.dp, compact)),
                    ) {
                        Text(
                            text = preview.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (compact < 0.99f) {
                            Text(
                                text = preview.artist,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 1f - compact,
                                ),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        MeloxTransportButton(
            onClick = onPlayPause,
            enabled = playPauseEnabled,
            size = 38.dp,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(lerp(22.dp, 20.dp, compact)),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (compact < 0.99f) {
            MeloxTransportButton(
                onClick = onNext,
                size = (38.dp * (1f - compact)).coerceAtLeast(0.dp),
                clip = true,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { alpha = 1f - compact },
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
            }
        }
        Box(Modifier.width(2.dp))
    }
}

@Composable
private fun MiniPlayerArtwork(
    coverUrl: String?,
    coverShape: RoundedCornerShape,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(coverShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (coverUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun MeloxTransportButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    clip: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .then(if (clip) Modifier.clipToBounds() else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
