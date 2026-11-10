package com.lyra.player.ui.component.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.lyra.player.ui.melox.theme.MeloxType

internal data class SongActionMenuItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color = Color.Unspecified,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * 歌曲「更多」统一菜单：锚定在触发按钮旁的小窗，右对齐并优先向下展开。
 * 按住滑动时高亮行有遮罩反馈，标签完整换行展示，溢出可滚动。
 */
@Composable
internal fun SongActionsMenu(
    expanded: Boolean,
    actions: List<SongActionMenuItem>,
    onDismissRequest: () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded
    if (!transitionState.currentState && !transitionState.targetState && transitionState.isIdle) {
        return
    }

    val density = LocalDensity.current
    val gapPx = with(density) { 6.dp.roundToPx() }
    val marginPx = with(density) { 10.dp.roundToPx() }
    val positionProvider = remember(gapPx, marginPx) {
        AnchoredMenuPositionProvider(gapPx = gapPx, marginPx = marginPx)
    }

    val scrollState = rememberScrollState()
    var hoveredIndex by remember { mutableIntStateOf(-1) }
    val rowBounds = remember { mutableStateMapOf<Int, Rect>() }
    val currentActions by rememberUpdatedState(actions)
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val shape = RoundedCornerShape(20.dp)
    val separatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val transformOrigin = remember { TransformOrigin(1f, 0f) }
    val textMeasurer = rememberTextMeasurer()
    val menuWidth = remember(actions, density, textMeasurer) {
        val labelWidthPx = actions.maxOfOrNull { item ->
            textMeasurer.measure(
                text = item.label,
                style = MeloxType.body,
                maxLines = 1,
                softWrap = false
            ).size.width
        } ?: 0
        val horizontalPaddingPx = with(density) { (16.dp * 2 + 20.dp + 14.dp).roundToPx() }
        with(density) {
            (labelWidthPx + horizontalPaddingPx).toDp().coerceIn(176.dp, 288.dp)
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                scaleIn(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    initialScale = 0.9f,
                    transformOrigin = transformOrigin
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 90)) +
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                    targetScale = 0.94f,
                    transformOrigin = transformOrigin
                )
        ) {
            Surface(
                modifier = Modifier
                    .width(menuWidth)
                    .heightIn(max = 360.dp),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                )
            ) {
                Column(
                    Modifier
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                        .verticalScroll(scrollState)
                ) {
                    val rowIndexAt: (Offset) -> Int = { position ->
                        rowBounds.entries.firstOrNull { it.value.contains(position) }?.key ?: -1
                    }
                    Column(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val pointer = event.changes.firstOrNull()
                                        hoveredIndex = if (pointer == null || !pointer.pressed) {
                                            -1
                                        } else {
                                            rowIndexAt(pointer.position)
                                        }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    var pressedRowIndex = -1
                                    var slidAway = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                        val change = event.changes.firstOrNull() ?: continue
                                        val currentRowIndex = rowIndexAt(change.position)
                                        when {
                                            change.pressed && !change.previousPressed -> {
                                                pressedRowIndex = currentRowIndex
                                                slidAway = false
                                            }
                                            change.pressed -> {
                                                if (currentRowIndex != pressedRowIndex) {
                                                    slidAway = true
                                                }
                                            }
                                            change.previousPressed -> {
                                                if (
                                                    scrollState.maxValue == 0 &&
                                                    slidAway &&
                                                    currentRowIndex >= 0
                                                ) {
                                                    val item = currentActions[currentRowIndex]
                                                    if (item.enabled) {
                                                        change.consume()
                                                        currentDismiss()
                                                        item.onClick()
                                                    }
                                                }
                                                pressedRowIndex = -1
                                                slidAway = false
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        actions.forEachIndexed { index, item ->
                            SongActionRow(
                                item = item,
                                showTopSeparator = index > 0,
                                hovered = index == hoveredIndex,
                                separatorColor = separatorColor,
                                onBoundsChange = { rowBounds[index] = it },
                                onSelect = {
                                    onDismissRequest()
                                    item.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private class AnchoredMenuPositionProvider(
    private val gapPx: Int,
    private val marginPx: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - marginPx)
            .coerceAtLeast(marginPx)
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(marginPx, maxX)

        val maxY = (windowSize.height - popupContentSize.height - marginPx)
            .coerceAtLeast(marginPx)
        val below = anchorBounds.bottom + gapPx
        val above = anchorBounds.top - gapPx - popupContentSize.height
        val y = when {
            below <= maxY -> below
            above >= marginPx -> above
            else -> below.coerceIn(marginPx, maxY)
        }
        return IntOffset(x, y)
    }
}

@Composable
private fun SongActionRow(
    item: SongActionMenuItem,
    showTopSeparator: Boolean,
    hovered: Boolean,
    separatorColor: Color,
    onBoundsChange: (Rect) -> Unit,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val highlight by animateColorAsState(
        targetValue = if (pressed || hovered) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 120),
        label = "song-action-highlight"
    )
    val iconTint = if (item.tint == Color.Unspecified) {
        MaterialTheme.colorScheme.primary
    } else {
        item.tint
    }
    val contentAlpha = if (item.enabled) 1f else 0.38f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .onGloballyPositioned { onBoundsChange(it.boundsInParent()) }
            .drawBehind {
                if (showTopSeparator) {
                    drawLine(
                        color = separatorColor,
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            .background(highlight)
            .then(
                if (item.enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSelect
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint.copy(alpha = iconTint.alpha * contentAlpha)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = item.label,
            style = MeloxType.body,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            maxLines = Int.MAX_VALUE
        )
    }
}
