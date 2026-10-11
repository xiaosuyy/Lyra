package moe.ouom.neriplayer.ui.screen.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import moe.ouom.neriplayer.ui.component.playback.resolveMiniPlayerProgressFraction
import moe.ouom.neriplayer.ui.theme.designsystem.LocalNeriColors
import moe.ouom.neriplayer.ui.theme.designsystem.LocalNeriTextStyles
import moe.ouom.neriplayer.ui.theme.designsystem.NeriSpacing
import moe.ouom.neriplayer.ui.theme.designsystem.neriColorsFrom
import moe.ouom.neriplayer.ui.theme.designsystem.neriTextStylesFrom/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: moe.ouom.neriplayer.ui.screen.nowplaying/NowPlayingScaffold
 * Created: 2026/9/28
 */

/**
 * 计算封面边长（Dp）。
 *
 * 响应式：竖屏时封面占可用宽度/高度的较大比例，横屏时更小以避免挤压控制区。
 * 仅做尺寸推导，不涉及任何播放业务。
 */
internal fun resolveNowPlayingCoverSizeDp(
    availableWidthDp: Float,
    availableHeightDp: Float,
    isLandscape: Boolean = false
): Float {
    val widthFraction = if (isLandscape) 0.42f else 0.74f
    val heightFraction = if (isLandscape) 0.58f else 0.50f
    return minOf(
        availableWidthDp.coerceAtLeast(0f) * widthFraction,
        availableHeightDp.coerceAtLeast(0f) * heightFraction
    )
}

/**
 * 竖屏 Full Player 进度比例（0..1）。
 * 直接复用 Mini Player 已验证过的换算 / clamp 逻辑，避免重复算法。
 */
internal fun resolveNowPlayingProgressRatio(positionMs: Long, durationMs: Long): Float =
    resolveMiniPlayerProgressFraction(positionMs, durationMs)

/** 竖屏辅助操作区（Secondary Actions）的状态汇总，便于统一判定与测试。 */
internal data class NowPlayingSecondaryActionState(
    val lyricsEnabled: Boolean,
    val lyricsActive: Boolean,
    val timerActive: Boolean
)

/**
 * 依据歌词数量 / 歌词页可见 / 定时器状态推导 Secondary Actions 的启用与激活状态。
 * 只做纯状态推导，不涉及任何业务。
 */
internal fun resolveNowPlayingSecondaryActionState(
    lyricsCount: Int,
    showLyricsScreen: Boolean,
    sleepTimerActive: Boolean
): NowPlayingSecondaryActionState = NowPlayingSecondaryActionState(
    lyricsEnabled = lyricsCount > 0,
    lyricsActive = lyricsCount > 0 && showLyricsScreen,
    timerActive = sleepTimerActive
)

/**
 * Full Player 的极轻动态氛围底：顶部一层低透明强调色过渡到页面背景。
 * 不做高饱和/霓虹/强渐变，保证 Light / Dark 下文字可读。
 */
@Composable
fun NowPlayingAmbientBackground(modifier: Modifier = Modifier) {
    val colors = LocalNeriColors.current
    // 换歌时 Accent 平滑过渡（非常轻的 crossfade，不做高频/高饱和变化）
    val animatedAccent by animateColorAsState(
        targetValue = colors.accent,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "now_playing_ambient_accent"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = 0.10f),
                        colors.background
                    )
                )
            )
    )
}

/**
 * 封面包容槽：根据可用空间给出响应式尺寸，内容自行决定如何渲染封面。
 */
@Composable
fun NowPlayingCoverSlot(
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    content: @Composable (size: Dp) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val sizeDp = resolveNowPlayingCoverSizeDp(
            availableWidthDp = maxWidth.value,
            availableHeightDp = maxHeight.value,
            isLandscape = isLandscape
        ).dp
        Box(
            modifier = Modifier.size(sizeDp),
            contentAlignment = Alignment.Center
        ) {
            content(sizeDp)
        }
    }
}

/**
 * Full Player 视觉骨架（槽位化）。
 *
 * - 传入 [content] 时优先渲染整段内容（供竖屏在最小改动下复用现有内容）。
 * - 否则按槽位顺序渲染：顶部栏 → 封面 → 歌曲信息 → 进度 → 主轴控制 → 歌词 → 底部操作。
 *
 * 只负责背景、层级与顺序，不承载任何播放业务；进出场修饰（insets / padding / 手势）由 [modifier] 承载。
 */
@Composable
fun NowPlayingScaffold(
    modifier: Modifier = Modifier,
    drawBackground: Boolean = true,
    background: (@Composable BoxScope.() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    cover: (@Composable ColumnScope.() -> Unit)? = null,
    titleBlock: (@Composable ColumnScope.() -> Unit)? = null,
    progress: (@Composable ColumnScope.() -> Unit)? = null,
    primaryControls: (@Composable ColumnScope.() -> Unit)? = null,
    lyrics: (@Composable ColumnScope.() -> Unit)? = null,
    bottomActions: (@Composable ColumnScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = LocalNeriColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (drawBackground) Modifier.background(colors.background) else Modifier)
    ) {
        background?.invoke(this)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (content != null) {
                content()
                return@Column
            }

            topBar?.invoke()

            Spacer(Modifier.height(NeriSpacing.SM))

            cover?.invoke(this)

            Spacer(Modifier.height(NeriSpacing.LG))

            titleBlock?.invoke(this)

            progress?.invoke(this)

            primaryControls?.invoke(this)

            lyrics?.invoke(this)

            Spacer(Modifier.weight(1f))

            bottomActions?.invoke(this)
        }
    }
}

@Preview(showBackground = true, name = "NowPlayingScaffold")
@Composable
private fun NowPlayingScaffoldPreview() {
    val scheme = lightColorScheme()
    val typography = Typography()
    CompositionLocalProvider(
        LocalNeriColors provides neriColorsFrom(scheme, isDark = false),
        LocalNeriTextStyles provides neriTextStylesFrom(typography)
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            NowPlayingScaffold(
                topBar = {
                    Text(text = "正在播放", style = LocalNeriTextStyles.current.title)
                },
                cover = {
                    NowPlayingCoverSlot(isLandscape = false) { size ->
                        Box(
                            modifier = Modifier
                                .size(size)
                                .background(color = LocalNeriColors.current.elevatedSurface)
                        )
                    }
                },
                titleBlock = {
                    Text(text = "歌曲名称", style = LocalNeriTextStyles.current.largeTitle)
                    Text(text = "艺术家", style = LocalNeriTextStyles.current.bodySecondary)
                },
                progress = {
                    Text(text = "0:42 / 3:56", style = LocalNeriTextStyles.current.captionSecondary)
                },
                primaryControls = {
                    Text(text = "◀  ▶Ⅱ  ▶", style = LocalNeriTextStyles.current.title)
                },
                bottomActions = {
                    Text(text = "♡   ≋   ···", style = LocalNeriTextStyles.current.caption)
                }
            )
        }
    }
}
