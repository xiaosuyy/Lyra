package com.lyra.player.ui.melox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lyra.player.ui.melox.glass.MeloxGlassSpec
import com.lyra.player.ui.melox.glass.meloxGlassSurface

/**
 * Floating glass bar used above the tab dock.
 * [compactProgress] morphs it from the wide card into the inline pill beside the tab circle.
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
) {
    val compact = compactProgress.coerceIn(0f, 1f)
    val barShape = RoundedCornerShape(lerp(24.dp, 22.dp, compact))
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
        Box(
            modifier = Modifier
                .size(coverSize)
                .clip(RoundedCornerShape(lerp(10.dp, coverSize / 2f, compact)))
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
