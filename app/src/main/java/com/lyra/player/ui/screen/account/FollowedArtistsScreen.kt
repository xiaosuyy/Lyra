package com.lyra.player.ui.screen.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.playlist.favorite.FAVORITE_SOURCE_NETEASE_ARTIST
import com.lyra.player.data.playlist.favorite.FavoritePlaylist
import com.lyra.player.data.playlist.favorite.FavoritePlaylistRepository
import com.lyra.player.ui.haptic.performHapticFeedback
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.melox.theme.MeloxType
import com.lyra.player.util.media.fastScrollableImageRequest

/** WebDAV 账号里的关注艺人列表，随账号同步。 */
@Composable
fun FollowedArtistsScreen(
    onBack: () -> Unit,
    onOpenArtist: (NeteaseArtistSummary) -> Unit,
) {
    val context = LocalContext.current
    val favoriteRepo = remember(context) { FavoritePlaylistRepository.getInstance(context) }
    val favorites by favoriteRepo.favorites.collectAsStateWithLifecycle()
    val artists = remember(favorites) {
        favorites.filter { it.source == FAVORITE_SOURCE_NETEASE_ARTIST }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            MeloxShapes.circle
                        )
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.account_followed_artists),
                    style = MeloxType.title2,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (artists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.library_no_favorite_artist),
                            style = MeloxType.subheadline,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.library_favorite_artist_hint),
                            style = MeloxType.caption,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 120.dp
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(artists, key = { "${it.source}:${it.id}" }) { artist ->
                        Column {
                            FollowedArtistRow(
                                artist = artist,
                                onClick = {
                                    onOpenArtist(
                                        NeteaseArtistSummary(id = artist.id, name = artist.name)
                                    )
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp, end = 18.dp),
                                thickness = 0.6.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedArtistRow(
    artist: FavoritePlaylist,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clickable {
                context.performHapticFeedback()
                onClick()
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!artist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fastScrollableImageRequest(
                        context = context,
                        data = artist.coverUrl,
                        sizePx = 160
                    ),
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = followedArtistSubtitle(artist),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.46f)
            )
        }
    }
}

@Composable
private fun followedArtistSubtitle(artist: FavoritePlaylist): String {
    val songCount = pluralStringResource(
        R.plurals.artist_song_count,
        artist.trackCount,
        artist.trackCount
    )
    if (artist.albumCount <= 0) return songCount
    val albumCount = pluralStringResource(
        R.plurals.artist_album_count,
        artist.albumCount,
        artist.albumCount
    )
    return "$songCount · $albumCount"
}