package com.lyra.player.ui.melox.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.lyra.player.ui.melox.glass.MeloxGlassSpec
import com.lyra.player.ui.melox.glass.meloxGlassSurface
import com.lyra.player.ui.melox.theme.MeloxShapes

data class MeloxTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector? = null,
)

/**
 * 参考播放器的悬浮底栏：展开态是 [标签胶囊][搜索圆钮]；
 * 内容向下滚动后，胶囊收成当前页图标圆，迷你播放器滑入同一行。
 * 两个状态之间是连续形变，不做整块替换。
 */
@Composable
fun MeloxBottomChrome(
    items: List<MeloxTabItem>,
    selectedRoute: String?,
    collapsed: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    miniPlayer: (@Composable (compactProgress: Float) -> Unit)? = null,
    miniPlayerVisible: Boolean = miniPlayer != null,
    onSearchClick: (() -> Unit)? = null,
    // 搜索是独立页面：选中它时三个标签都回到未选中态，
    // 收起后左侧圆钮仍显示"最后进入的那个标签"的图标，而不是空格。
    searchActive: Boolean = false,
    compactRoute: String? = selectedRoute,
) {
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
    val collapse by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.92f, stiffness = 420f),
        label = "melox_chrome_collapse",
    )
    val miniVisibility by animateFloatAsState(
        targetValue = if (miniPlayerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "melox_chrome_mini_visibility",
    )
    // 0-55%：标签胶囊收成当前图标圆，迷你播放器缩成行内尺寸；
    // 45-100%：迷你播放器落到同一行，底栏整体高度收紧。
    val shrink = smoothStep(0f, 0.55f, collapse)
    val drop = smoothStep(0.45f, 1f, collapse)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val searchSize = 48.dp
            val gap = 7.dp
            val collapsedTabSize = 52.dp
            val collapsedMiniWidth = (maxWidth - collapsedTabSize - 8.dp - gap - searchSize)
                .coerceAtLeast(0.dp)
            val capsuleWidth = lerp(maxWidth - searchSize - gap, collapsedTabSize, shrink)
            val capsuleHeight = lerp(57.dp, collapsedTabSize, shrink)
            val miniWidth = lerp(maxWidth, collapsedMiniWidth, shrink)
            val miniHeight = lerp(48.dp, 44.dp, drop)
            val containerHeight = lerp(
                capsuleHeight + 55.dp * miniVisibility,
                collapsedTabSize,
                drop,
            )
            val miniX = lerp(0.dp, collapsedTabSize + 8.dp, shrink)
            val miniY = lerp(0.dp, (collapsedTabSize - miniHeight) / 2f, drop)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(containerHeight)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(capsuleHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MeloxTabCapsule(
                        items = items,
                        selectedRoute = selectedRoute,
                        onSelect = onSelect,
                        compact = collapse,
                        glassColor = glassColor,
                        compactRoute = compactRoute,
                        searchActive = searchActive,
                        modifier = Modifier
                            .width(capsuleWidth)
                            .fillMaxHeight(),
                    )
                    Spacer(Modifier.weight(1f))
                    if (onSearchClick != null) {
                        MeloxSearchButton(
                            size = searchSize,
                            active = searchActive,
                            onClick = onSearchClick,
                        )
                    }
                }
                if (miniPlayer != null) {
                    Box(
                        modifier = Modifier
                            .offset(x = miniX, y = miniY)
                            .width(miniWidth)
                            .height(miniHeight)
                    ) {
                        miniPlayer(drop)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeloxTabCapsule(
    items: List<MeloxTabItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    compact: Float,
    glassColor: Color,
    compactRoute: String?,
    searchActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val capsuleShape = MeloxShapes.capsule
    // MeloX 口径：展开层在收起过半时淡出，收起层随后淡入；
    // 标签比图标更早消失，避免文字在窄槽里被挤压重排。
    val expandedAlpha = 1f - smoothStep(0.43f, 0.72f, compact)
    val compactAlpha = smoothStep(0.52f, 0.82f, compact)
    val labelAlpha = smoothStep(0f, 0.35f, 1f - compact)
    Box(
        modifier = modifier.meloxGlassSurface(
            shape = capsuleShape,
            surfaceColor = glassColor,
            spec = MeloxGlassSpec(blurRadius = 8.dp, lensRadius = 24.dp, refractionHeight = 12.dp),
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(capsuleShape)
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                // -1 = 当前页不在三个标签里（搜索页）：水珠与选中态一起隐藏。
                val selectedIndex = items.indexOfFirst { it.route == selectedRoute }
                val tabWidth = if (items.isEmpty()) 0.dp else maxWidth / items.size
                // 水珠位置：弹簧移动，速度驱动横向拉伸 / 纵向压缩（MeloX 的关键帧观感）。
                // 初值直接取当前选中项，避免冷启动落在非首页时水珠先滑一段。
                val pillPosition = remember {
                    Animatable(
                        items.indexOfFirst { it.route == selectedRoute }
                            .coerceAtLeast(0)
                            .toFloat()
                    )
                }
                val pillAlpha by animateFloatAsState(
                    targetValue = if (selectedIndex >= 0) 1f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "melox_tab_pill_alpha",
                )
                LaunchedEffect(selectedIndex) {
                    if (selectedIndex >= 0) {
                        pillPosition.animateTo(
                            targetValue = selectedIndex.toFloat(),
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f),
                        )
                    }
                }
                if (expandedAlpha > 0.01f && pillAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = pillPosition.value * tabWidth.toPx()
                                val velocity = (pillPosition.velocity / 10f)
                                scaleX = 1f / (1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f))
                                scaleY = 1f - (velocity * 0.25f).coerceIn(-0.1f, 0.1f)
                            }
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(horizontal = 5.dp, vertical = 6.dp)
                            .alpha(expandedAlpha * pillAlpha)
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                shape = capsuleShape,
                            ),
                    )
                }
                // 展开层：等宽三槽，始终不改变权重，避免每帧重新测量文字。
                Row(Modifier.fillMaxSize()) {
                    items.forEach { item ->
                        MeloxTab(
                            item = item,
                            selected = item.route == selectedRoute,
                            onClick = { onSelect(item.route) },
                            iconAlpha = expandedAlpha,
                            labelAlpha = labelAlpha * expandedAlpha,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // 收起层：只保留"最后进入的标签"图标；搜索页里该图标回到未选中色。
                val compactItem = items.firstOrNull { it.route == compactRoute }
                    ?: items.getOrNull(selectedIndex)
                    ?: items.firstOrNull()
                if (compactAlpha > 0.01f && compactItem != null) {
                    val compactActivated = !searchActive && selectedIndex >= 0
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(compactAlpha),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (compactActivated) {
                                compactItem.selectedIcon ?: compactItem.icon
                            } else {
                                compactItem.icon
                            },
                            contentDescription = compactItem.label,
                            modifier = Modifier.size(24.dp),
                            tint = if (compactActivated) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeloxSearchButton(
    size: androidx.compose.ui.unit.Dp,
    active: Boolean,
    onClick: () -> Unit,
) {
    // 搜索作为独立页时享受和标签一样的反馈：图标放大 + 强调色，只是没有水珠胶囊。
    val scale by animateFloatAsState(
        targetValue = if (active) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "melox_search_scale",
    )
    val tint by animateColorAsState(
        targetValue = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "melox_search_tint",
    )
    Box(
        modifier = Modifier
            .size(size)
            .meloxGlassSurface(
                shape = MeloxShapes.circle,
                surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
                spec = MeloxGlassSpec(blurRadius = 8.dp, lensRadius = 24.dp, refractionHeight = 12.dp),
            )
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search",
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = tint,
        )
    }
}

@Composable
private fun MeloxTab(
    item: MeloxTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    iconAlpha: Float,
    labelAlpha: Float,
    modifier: Modifier = Modifier,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "melox_tab_scale",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
        },
        animationSpec = tween(durationMillis = 220),
        label = "melox_tab_tint",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            // MeloX 的 tab 只有水珠高亮，不再叠一层 Material 涟漪。
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = when {
                    !selected -> item.icon
                    else -> item.selectedIcon ?: item.icon
                },
                contentDescription = item.label,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = iconAlpha
                    },
                tint = iconTint,
            )
            if (labelAlpha > 0.01f) {
                // MeloX 口径：图标 24dp、文字 11sp/13sp 行高，中间不再留额外间距。
                Text(
                    text = item.label,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f)
                    },
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.alpha(labelAlpha),
                )
            }
        }
    }
}

private fun smoothStep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Shared surface shape for the floating dock; kept here so all chrome matches. */
val MeloxDockShape: Shape = MeloxShapes.capsule

internal val MeloxDockTint: Color = Color.White.copy(alpha = 0.12f)

/** Kept for callers that animate a rounded-rect into the dock capsule. */
internal fun meloxMiniPlayerShape(compactProgress: Float): Shape =
    RoundedCornerShape(lerp(16.dp, 22.dp, compactProgress.coerceIn(0f, 1f)))
