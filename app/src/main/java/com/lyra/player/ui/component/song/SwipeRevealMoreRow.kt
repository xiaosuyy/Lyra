package com.lyra.player.ui.component.song

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 左滑露出「更多」的行容器：默认不显示更多图标，左滑时整行左移、前导数字/封面淡出，
 * 标题与其它行的前导列对齐，右侧滑入统一的圆形更多按钮。
 * 松手按 45% 阈值吸附/回弹，同一时刻仅一行展开，由调用方持有展开 key。
 */
@Composable
internal fun SwipeRevealMoreRow(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    revealWidth: Dp,
    slideWidth: Dp,
    swipeEnabled: Boolean,
    rowModifier: Modifier,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    moreTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    moreContentDescription: String? = null,
    leading: (@Composable () -> Unit)? = null,
    moreMenu: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val revealPx = with(density) { revealWidth.toPx() }
    val slidePx = with(density) { slideWidth.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var crossedThreshold by remember { mutableStateOf(false) }

    fun animateTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = offsetPx,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 260f)
            ) { value, _ -> offsetPx = value }
        }
    }

    LaunchedEffect(expanded, swipeEnabled) {
        if (!swipeEnabled) {
            settleJob?.cancel()
            offsetPx = 0f
        } else {
            animateTo(if (expanded) revealPx else 0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .pointerInput(swipeEnabled, revealPx) {
                if (!swipeEnabled || revealPx <= 0f) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        crossedThreshold = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = (offsetPx - dragAmount).coerceAtLeast(0f)
                        offsetPx = if (proposed <= revealPx) {
                            proposed
                        } else {
                            revealPx + (proposed - revealPx) * 0.18f
                        }
                        val crossed = offsetPx >= revealPx * 0.45f
                        if (crossed != crossedThreshold) {
                            crossedThreshold = crossed
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDragCancel = {
                        animateTo(if (expanded) revealPx else 0f)
                    },
                    onDragEnd = {
                        val expand = offsetPx >= revealPx * 0.45f
                        animateTo(if (expand) revealPx else 0f)
                        onExpandedChange(expand)
                    }
                )
            }
    ) {
        Row(
            modifier = rowModifier.offset {
                val progress = if (revealPx <= 0f) 0f else (offsetPx / revealPx).coerceIn(0f, 1f)
                IntOffset(-(progress * slidePx).roundToInt(), 0)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Row(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (revealPx <= 0f) {
                            1f
                        } else {
                            (1f - offsetPx / revealPx).coerceIn(0f, 1f)
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leading()
                }
            }
            content()
        }

        if (swipeEnabled && offsetPx > 0.5f) {
            val progress = (offsetPx / revealPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = revealWidth)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onExpandedChange(false) }
            )
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(revealWidth)
                        .fillMaxHeight()
                        .graphicsLayer {
                            alpha = progress
                            val buttonScale = 0.72f + 0.28f * progress
                            scaleX = buttonScale
                            scaleY = buttonScale
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.wrapContentSize()) {
                        val chipSize = (revealWidth - 4.dp).coerceAtMost(40.dp)
                        Box(
                            modifier = Modifier
                                .size(chipSize)
                                .clip(CircleShape)
                                .background(moreTint.copy(alpha = 0.10f))
                                .border(0.5.dp, moreTint.copy(alpha = 0.18f), CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onMoreClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = moreContentDescription,
                                modifier = Modifier.size(20.dp),
                                tint = moreTint
                            )
                        }
                        moreMenu?.invoke()
                    }
                }
            }
        }
    }
}
