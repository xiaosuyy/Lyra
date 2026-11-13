package com.lyra.player.ui.screen.artist

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.lyra.player.core.api.search.CustomChannelArtist
import com.lyra.player.core.api.search.CustomChannelArtistAlbum
import com.lyra.player.data.local.media.displayAlbum
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.stableKey
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.component.overlay.DensityScaledModalBottomSheet as ModalBottomSheet
import com.lyra.player.ui.screen.playlist.PlaylistModernTopBarCircleButton
import com.lyra.player.ui.screen.playlist.PlaylistModernVisualColorsProvider
import com.lyra.player.ui.screen.playlist.playlistModernListErrorContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListPrimaryContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListSecondaryContentColor
import com.lyra.player.ui.screen.playlist.rememberPlaylistModernHeroBackgroundColor
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.ui.viewmodel.artist.TencentArtistDetailUiState
import com.lyra.player.ui.viewmodel.artist.TencentArtistDetailViewModel
import com.lyra.player.ui.viewmodel.artist.TencentArtistHeader
import com.lyra.player.util.format.formatPlayCount
import com.lyra.player.util.media.offlineCachedImageRequest
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TencentArtistDetailScreen(
    artist: CustomChannelArtist,
    onBack: () -> Unit = {},
    onSongClick: (List<SongItem>, Int) -> Unit = { _, _ -> },
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    val viewModel: TencentArtistDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TencentArtistDetailViewModel(context.applicationContext as Application)
            }
        }
    )
    val ui by viewModel.uiState.collectAsState()
    var selectedTab by rememberSaveable(artist.id) { mutableIntStateOf(0) }
    var searchVisible by rememberSaveable(artist.id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(artist.id) { mutableStateOf("") }
    val focusRequester = remember(artist.id) { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberSaveable(artist.id, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(artist.id) {
        viewModel.start(artist)
    }

    LaunchedEffect(searchVisible) {
        if (searchVisible) {
            delay(120)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = searchVisible) {
        searchVisible = false
        searchQuery = ""
    }

    val headerCoverUrl = ui.header?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: artist.coverUrl.orEmpty()

    PlaylistModernVisualColorsProvider(
        coverUrl = headerCoverUrl,
        offlineMode = offlineMode
    ) {
        val pageColor = rememberPlaylistModernHeroBackgroundColor(
            coverUrl = headerCoverUrl,
            offlineMode = offlineMode
        )
        Surface(modifier = Modifier.fillMaxSize(), color = pageColor) {
            Box(modifier = Modifier.fillMaxSize()) {
                TencentArtistContent(
                    ui = ui,
                    artistName = artist.name,
                    pageColor = pageColor,
                    listState = listState,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    searchActive = searchVisible,
                    searchQuery = searchQuery,
                    onRetry = viewModel::retry,
                    onLoadMoreSongs = viewModel::loadMoreSongs,
                    onLoadMoreAlbums = viewModel::loadMoreAlbums,
                    onAlbumClick = viewModel::openAlbum,
                    onSongClick = onSongClick,
                    onToggleFollow = viewModel::toggleFollow,
                    offlineMode = offlineMode,
                    isTabletLayout = isTabletLayout
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlaylistModernTopBarCircleButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (searchVisible) {
                        ArtistTopSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = stringResource(R.string.artist_search_hint),
                            focusRequester = focusRequester,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    PlaylistModernTopBarCircleButton(onClick = {
                        if (searchVisible) {
                            searchVisible = false
                            searchQuery = ""
                        } else {
                            searchVisible = true
                            selectedTab = 0
                        }
                    }) {
                        Icon(
                            if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    val albumSheet = ui.albumSongs
    if (albumSheet != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAlbum,
            sheetState = sheetState
        ) {
            Text(
                text = albumSheet.album.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            HorizontalDivider()
            when {
                albumSheet.loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                albumSheet.error != null -> {
                    ErrorBlock(
                        error = albumSheet.error,
                        onRetry = { viewModel.openAlbum(albumSheet.album) }
                    )
                }

                albumSheet.songs.isEmpty() -> {
                    EmptyBlock(text = stringResource(R.string.artist_songs_empty))
                }

                else -> {
                    val songs = albumSheet.songs
                    val miniPlayerHeight = LocalMiniPlayerHeight.current
                    var swipeExpandedSongKey by remember { mutableStateOf<String?>(null) }
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp + miniPlayerHeight
                        )
                    ) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            val songKey = song.stableKey()
                            ArtistSwipeSongRow(
                                song = song,
                                offlineMode = offlineMode,
                                expanded = swipeExpandedSongKey == songKey,
                                onExpandedChange = { open ->
                                    swipeExpandedSongKey = if (open) songKey else null
                                },
                                onClick = {
                                    onSongClick(songs, index)
                                    viewModel.dismissAlbum()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TencentArtistContent(
    ui: TencentArtistDetailUiState,
    artistName: String,
    pageColor: Color,
    listState: LazyListState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onRetry: () -> Unit,
    onLoadMoreSongs: () -> Unit,
    onLoadMoreAlbums: () -> Unit,
    onAlbumClick: (CustomChannelArtistAlbum) -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    onToggleFollow: () -> Unit,
    offlineMode: Boolean,
    isTabletLayout: Boolean
) {
    val context = LocalContext.current
    val miniPlayerHeight = LocalMiniPlayerHeight.current
    val horizontalInset = if (isTabletLayout) 36.dp else 0.dp
    val normalizedQuery = searchQuery.trim()
    val isFiltering = searchActive && normalizedQuery.isNotEmpty()
    var swipeExpandedSongKey by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = swipeExpandedSongKey != null) { swipeExpandedSongKey = null }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) swipeExpandedSongKey = null
    }
    val filteredSongs = remember(ui.songs, normalizedQuery, searchActive) {
        if (!isFiltering) {
            ui.songs
        } else {
            ui.songs.filter { song ->
                song.displayName().contains(normalizedQuery, ignoreCase = true) ||
                    song.displayArtist().contains(normalizedQuery, ignoreCase = true) ||
                    song.displayAlbum(context).contains(normalizedQuery, ignoreCase = true)
            }
        }
    }
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
                TencentArtistHero(
                    header = ui.header,
                    fallbackName = artistName,
                    pageColor = pageColor,
                    followUpdating = ui.followUpdating,
                    offlineMode = offlineMode,
                    isTabletLayout = isTabletLayout,
                    onToggleFollow = onToggleFollow
                )
            }

            ui.header?.brief?.takeIf { it.isNotBlank() }?.let { description ->
                item {
                    Text(
                        text = description,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = playlistModernListSecondaryContentColor()
                    )
                }
            }

            if (ui.error != null && !ui.loading && (ui.songs.isNotEmpty() || ui.albums.isNotEmpty())) {
                item {
                    Text(
                        text = ui.error,
                        color = playlistModernListErrorContentColor(),
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

            if (!searchActive) {
                item {
                    ArtistTabChips(
                        selectedTab = selectedTab,
                        onSelect = onTabSelected,
                        modifier = Modifier.padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 8.dp)
                    )
                }
            }

            if (searchActive || selectedTab == 0) {
                val songs = if (isFiltering) filteredSongs else ui.songs
                if (songs.isEmpty()) {
                    item {
                        EmptyBlock(
                            text = stringResource(
                                if (isFiltering) {
                                    R.string.artist_songs_search_empty
                                } else {
                                    R.string.artist_songs_empty
                                }
                            )
                        )
                    }
                } else {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        val songKey = song.stableKey()
                        ArtistSwipeSongRow(
                            song = song,
                            onClick = { onSongClick(songs, index) },
                            expanded = swipeExpandedSongKey == songKey,
                            onExpandedChange = { open ->
                                swipeExpandedSongKey = if (open) songKey else null
                            },
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
                    itemsIndexed(ui.albums, key = { _, album -> album.mid }) { _, album ->
                        TencentAlbumRow(
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
private fun TencentArtistHero(
    header: TencentArtistHeader?,
    fallbackName: String,
    pageColor: Color,
    followUpdating: Boolean,
    offlineMode: Boolean,
    isTabletLayout: Boolean,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current
    val avatarUrl = header?.avatarUrl.orEmpty()
    val avatarSize = if (isTabletLayout) 176.dp else 148.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(pageColor)
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
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = offlineCachedImageRequest(
                        context = context,
                        data = avatarUrl,
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

        val fanText = header?.fanCount
            ?.takeIf { it > 0 }
            ?.let { stringResource(R.string.artist_fans, formatPlayCount(context, it.toLong())) }
        if (fanText != null) {
            Text(
                text = fanText,
                modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (header != null && (header.songCount > 0 || header.albumCount > 0)) {
            val meta = listOf(
                pluralStringResource(
                    R.plurals.artist_song_count,
                    header.songCount,
                    header.songCount
                ),
                pluralStringResource(
                    R.plurals.artist_album_count,
                    header.albumCount,
                    header.albumCount
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
        }

        if (header != null) {
            Spacer(Modifier.height(16.dp))
            ArtistFollowButton(
                followed = header.followed,
                updating = followUpdating,
                onToggle = onToggleFollow
            )
        }
    }
}

@Composable
private fun TencentAlbumRow(
    album: CustomChannelArtistAlbum,
    onClick: () -> Unit,
    offlineMode: Boolean = false
) {
    val context = LocalContext.current
    val subtitle = buildList {
        if (album.songCount > 0) {
            add(
                pluralStringResource(
                    R.plurals.count_songs_format,
                    album.songCount,
                    album.songCount
                )
            )
        }
        album.publishDate
            ?.takeIf { it.isNotBlank() }
            ?.let(::add)
    }.joinToString(" · ")
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = offlineCachedImageRequest(
                    context = context,
                    data = album.coverUrl,
                    offlineMode = offlineMode
                ),
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
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
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 68.dp, end = 18.dp),
            thickness = 0.6.dp,
            color = playlistModernListPrimaryContentColor().copy(alpha = 0.12f)
        )
    }
}
