package com.lyra.player.ui.screen.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.data.local.media.displayAlbum
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.ui.screen.playlist.PlaylistGlassMaskInk
import com.lyra.player.ui.screen.playlist.playlistModernListErrorContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListPrimaryContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListSecondaryContentColor
import com.lyra.player.ui.util.rememberSongDisplayCoverUrl
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.util.format.formatDuration
import com.lyra.player.util.media.offlineCachedImageRequest

private val ArtistRowHeight = 66.dp
private val ArtistRowCoverSize = 44.dp
private val ArtistRowCoverShape = RoundedCornerShape(6.dp)

@Composable
private fun ArtistInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 18.dp),
        thickness = 0.6.dp,
        color = playlistModernListPrimaryContentColor().copy(alpha = 0.12f)
    )
}

@Composable
internal fun ArtistFollowButton(
    followed: Boolean,
    updating: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (followed) Color.White else PlaylistGlassMaskInk
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(
                if (followed) {
                    Color.White.copy(alpha = 0.18f)
                } else {
                    Color.White.copy(alpha = 0.94f)
                },
                CircleShape
            )
            .border(
                width = 0.75.dp,
                color = if (followed) Color.White.copy(alpha = 0.26f) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(enabled = !updating, onClick = onToggle)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (updating) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else {
            Icon(
                imageVector = if (followed) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (followed) R.string.artist_followed else R.string.artist_follow
            ),
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun ArtistSongRow(
    song: SongItem,
    onClick: () -> Unit,
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    val coverUrl = rememberSongDisplayCoverUrl(song)
    val displayArtist = song.displayArtist().takeIf { it.isNotBlank() }
    val displayAlbum = song.displayAlbum(context)
        .replace("Netease", "")
        .takeIf { it.isNotBlank() }
    val subtitle = buildList {
        displayArtist?.let(::add)
        displayAlbum
            ?.takeIf { !it.equals(displayArtist, ignoreCase = true) }
            ?.let(::add)
    }.joinToString(" · ")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ArtistRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = offlineCachedImageRequest(
                    context = context,
                    data = coverUrl,
                    offlineMode = offlineMode
                ),
                contentDescription = song.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ArtistRowCoverSize)
                    .clip(ArtistRowCoverShape)
                    .background(playlistModernListPrimaryContentColor().copy(alpha = 0.10f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = song.displayName(),
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = playlistModernListPrimaryContentColor()
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = playlistModernListSecondaryContentColor()
                    )
                }
            }
            if (song.durationMs > 0L) {
                Text(
                    text = formatDuration(song.durationMs),
                    fontSize = 13.sp,
                    color = playlistModernListSecondaryContentColor().copy(alpha = 0.8f)
                )
            }
        }
        ArtistInsetDivider()
    }
}

@Composable
internal fun ArtistAlbumRow(
    album: AlbumSummary,
    onClick: () -> Unit,
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ArtistRowHeight)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = offlineCachedImageRequest(
                    context = context,
                    data = album.picUrl,
                    offlineMode = offlineMode
                ),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ArtistRowCoverSize)
                    .clip(ArtistRowCoverShape)
                    .background(playlistModernListPrimaryContentColor().copy(alpha = 0.10f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = album.name,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = playlistModernListPrimaryContentColor()
                )
                Text(
                    text = pluralStringResource(R.plurals.count_songs_format, album.size, album.size),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = playlistModernListSecondaryContentColor()
                )
            }
        }
        ArtistInsetDivider()
    }
}

@Composable
internal fun LoadingBlock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = playlistModernListPrimaryContentColor())
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.playlist_loading_content),
            color = playlistModernListPrimaryContentColor()
        )
    }
}

@Composable
internal fun ErrorBlock(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(error, color = playlistModernListErrorContentColor())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(
                stringResource(R.string.action_retry),
                color = playlistModernListPrimaryContentColor()
            )
        }
    }
}

@Composable
internal fun EmptyBlock(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = playlistModernListSecondaryContentColor())
    }
}

@Composable
internal fun LoadMoreButton(loading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TextButton(onClick = onClick, enabled = !loading) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = playlistModernListPrimaryContentColor()
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                stringResource(R.string.artist_load_more),
                color = playlistModernListPrimaryContentColor()
            )
        }
    }
}
