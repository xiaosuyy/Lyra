package com.lyra.player.ui.screen.artist

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.screen.playlist.PlaylistGlassMaskInk
import com.lyra.player.ui.screen.playlist.PlaylistModernTopBarCircleButton
import com.lyra.player.ui.screen.playlist.rememberPlaylistModernHeroBackgroundColor
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistDetailUiState
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistDetailViewModel
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistHeader
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.util.media.offlineCachedImageRequest

@Composable
fun NeteaseArtistDetailScreen(
    artist: NeteaseArtistSummary,
    onBack: () -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    onAlbumClick: (AlbumSummary) -> Unit = {},
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    val viewModel: NeteaseArtistDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                NeteaseArtistDetailViewModel(context.applicationContext as Application)
            }
        }
    )
    val ui by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable(artist.id) { mutableIntStateOf(0) }
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val listState = rememberSaveable(artist.id, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    LaunchedEffect(artist.id) {
        viewModel.start(artist)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(modifier = Modifier.fillMaxSize()) {
            ArtistContent(
                ui = ui,
                artistName = artist.name,
                listState = listState,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onRetry = viewModel::retry,
                onToggleFollow = viewModel::toggleFollow,
                onLoadMoreSongs = viewModel::loadMoreSongs,
                onLoadMoreAlbums = viewModel::loadMoreAlbums,
                onSongClick = onSongClick,
                onAlbumClick = onAlbumClick,
                offlineMode = offlineMode,
                isTabletLayout = isTabletLayout
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistModernTopBarCircleButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistContent(
    ui: NeteaseArtistDetailUiState,
    artistName: String,
    listState: LazyListState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit,
    onLoadMoreSongs: () -> Unit,
    onLoadMoreAlbums: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    offlineMode: Boolean,
    isTabletLayout: Boolean
) {
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val horizontalInset = if (isTabletLayout) 36.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .widthIn(max = 1080.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalInset,
                end = horizontalInset,
                bottom = 40.dp + miniPlayerHeight
            )
        ) {
        item {
            ArtistHero(
                header = ui.header,
                fallbackName = artistName,
                followUpdating = ui.followUpdating,
                offlineMode = offlineMode,
                isTabletLayout = isTabletLayout,
                onToggleFollow = onToggleFollow
            )
        }

        ui.header?.briefDesc?.takeIf { it.isNotBlank() }?.let { description ->
            item {
                Text(
                    text = description,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (ui.error != null && !ui.loading && (ui.songs.isNotEmpty() || ui.albums.isNotEmpty())) {
            item {
                Text(
                    text = ui.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        if (ui.loading && ui.songs.isEmpty() && ui.albums.isEmpty()) {
            item {
                LoadingBlock()
            }
            return@LazyColumn
        }

        if (ui.error != null && ui.songs.isEmpty() && ui.albums.isEmpty()) {
            item {
                ErrorBlock(error = ui.error, onRetry = onRetry)
            }
            return@LazyColumn
        }

        item {
            ArtistTabChips(
                selectedTab = selectedTab,
                onSelect = onTabSelected,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        if (selectedTab == 0) {
            if (ui.songs.isEmpty()) {
                item { EmptyBlock(text = stringResource(R.string.artist_songs_empty)) }
            } else {
                itemsIndexed(ui.songs, key = { _, item -> item.id }) { index, song ->
                    ArtistSongRow(
                        song = song,
                        onClick = { onSongClick(ui.songs, index) },
                        offlineMode = offlineMode
                    )
                }
            }
            if (ui.songsHasMore) {
                item {
                    LoadMoreButton(
                        loading = ui.songsLoadingMore,
                        onClick = onLoadMoreSongs
                    )
                }
            }
        } else {
            if (ui.albums.isEmpty()) {
                item { EmptyBlock(text = stringResource(R.string.artist_albums_empty)) }
            } else {
                itemsIndexed(ui.albums, key = { _, item -> item.id }) { _, album ->
                    ArtistAlbumRow(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        offlineMode = offlineMode
                    )
                }
            }
            if (ui.albumsHasMore) {
                item {
                    LoadMoreButton(
                        loading = ui.albumsLoadingMore,
                        onClick = onLoadMoreAlbums
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ArtistHero(
    header: NeteaseArtistHeader?,
    fallbackName: String,
    followUpdating: Boolean,
    offlineMode: Boolean,
    isTabletLayout: Boolean,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = header?.coverUrl?.takeIf { it.isNotBlank() } ?: header?.avatarUrl.orEmpty()
    val heroColor = rememberPlaylistModernHeroBackgroundColor(
        coverUrl = coverUrl,
        offlineMode = offlineMode
    )
    val background = MaterialTheme.colorScheme.background
    val avatarModel = header?.avatarUrl?.takeIf { it.isNotBlank() } ?: coverUrl
    val avatarSize = if (isTabletLayout) 176.dp else 148.dp
    val followed = header?.followed == true
    val followContentColor = if (followed) Color.White else PlaylistGlassMaskInk

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to heroColor,
                    0.72f to heroColor,
                    1f to background
                )
            )
            .statusBarsPadding()
            .padding(top = 24.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.22f),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (avatarModel.isNotBlank()) {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = avatarModel,
                        sizePx = 480,
                        allowHardware = false,
                        crossfade = true,
                        offlineMode = offlineMode
                    ),
                    contentDescription = header?.name ?: fallbackName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Text(
            text = header?.name?.takeIf { it.isNotBlank() } ?: fallbackName,
            modifier = Modifier.padding(top = 20.dp, start = 24.dp, end = 24.dp),
            color = Color.White,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!header?.alias.isNullOrBlank()) {
            Text(
                text = header.alias.orEmpty(),
                modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (header != null) {
            val meta = listOf(
                pluralStringResource(
                    R.plurals.artist_song_count,
                    header.musicSize,
                    header.musicSize
                ),
                pluralStringResource(
                    R.plurals.artist_album_count,
                    header.albumSize,
                    header.albumSize
                )
            ).joinToString(" · ")
            Text(
                text = meta,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
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
                        color = if (followed) {
                            Color.White.copy(alpha = 0.26f)
                        } else {
                            Color.Transparent
                        },
                        shape = CircleShape
                    )
                    .clickable(enabled = !followUpdating, onClick = onToggleFollow)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (followUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        strokeWidth = 2.dp,
                        color = followContentColor
                    )
                } else {
                    Icon(
                        imageVector = if (followed) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.StarOutline
                        },
                        contentDescription = null,
                        tint = followContentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (followed) {
                        stringResource(R.string.artist_followed)
                    } else {
                        stringResource(R.string.artist_follow)
                    },
                    color = followContentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ArtistTabChips(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MeloxShapes.capsule
    val labels = listOf(
        R.string.artist_tab_songs to 0,
        R.string.artist_tab_albums to 1
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f), shape)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEach { (labelRes, index) ->
            val isSelected = selectedTab == index
            val highlight by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = 200),
                label = "artist_tab_chip"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(highlight)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(labelRes),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
                    }
                )
            }
        }
    }
}
