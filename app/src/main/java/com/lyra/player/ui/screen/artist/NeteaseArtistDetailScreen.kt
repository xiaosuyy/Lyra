package com.lyra.player.ui.screen.artist

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.lyra.player.R
import com.lyra.player.data.local.media.displayAlbum
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.stableKey
import com.lyra.player.ui.LocalMiniPlayerHeight
import com.lyra.player.ui.melox.theme.MeloxShapes
import com.lyra.player.ui.screen.playlist.PlaylistGlassMaskColor
import com.lyra.player.ui.screen.playlist.PlaylistGlassMaskInk
import com.lyra.player.ui.screen.playlist.PlaylistModernTopBarCircleButton
import com.lyra.player.ui.screen.playlist.PlaylistModernVisualColorsProvider
import com.lyra.player.ui.screen.playlist.playlistModernListErrorContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListPrimaryContentColor
import com.lyra.player.ui.screen.playlist.playlistModernListSecondaryContentColor
import com.lyra.player.ui.screen.playlist.rememberPlaylistModernHeroBackgroundColor
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistDetailUiState
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistDetailViewModel
import com.lyra.player.ui.viewmodel.artist.NeteaseArtistHeader
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.util.currentWindowWidthDp
import com.lyra.player.util.media.offlineCachedImageRequest
import kotlinx.coroutines.delay

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
    var searchVisible by rememberSaveable(artist.id) { mutableStateOf(false) }
    var searchQuery by rememberSaveable(artist.id) { mutableStateOf("") }
    val focusRequester = remember(artist.id) { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isTabletLayout = currentWindowWidthDp() >= 720.dp
    val listState = rememberSaveable(artist.id, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

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

    val headerCoverUrl = ui.header?.coverUrl?.takeIf { it.isNotBlank() }
        ?: ui.header?.avatarUrl.orEmpty()

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
                ArtistContent(
                    ui = ui,
                    artistName = artist.name,
                    pageColor = pageColor,
                    listState = listState,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    searchActive = searchVisible,
                    searchQuery = searchQuery,
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
}

@Composable
internal fun ArtistTopSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val ink = PlaylistGlassMaskInk
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(PlaylistGlassMaskColor)
            .border(0.75.dp, Color.Black.copy(alpha = 0.08f), shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = ink.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = ink.copy(alpha = 0.42f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ink,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_clear),
                        tint = ink.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
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
    pageColor: Color,
    listState: LazyListState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    searchActive: Boolean,
    searchQuery: String,
    onRetry: () -> Unit,
    onToggleFollow: () -> Unit,
    onLoadMoreSongs: () -> Unit,
    onLoadMoreAlbums: () -> Unit,
    onSongClick: (List<SongItem>, Int) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
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
            ArtistHero(
                header = ui.header,
                fallbackName = artistName,
                pageColor = pageColor,
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
                itemsIndexed(songs, key = { _, item -> item.id }) { index, song ->
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
    pageColor: Color,
    followUpdating: Boolean,
    offlineMode: Boolean,
    isTabletLayout: Boolean,
    onToggleFollow: () -> Unit
) {
    val context = LocalContext.current
    val coverUrl = header?.coverUrl?.takeIf { it.isNotBlank() } ?: header?.avatarUrl.orEmpty()
    val avatarModel = header?.avatarUrl?.takeIf { it.isNotBlank() } ?: coverUrl
    val avatarSize = if (isTabletLayout) 176.dp else 148.dp
    val followed = header?.followed == true

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
            ArtistFollowButton(
                followed = followed,
                updating = followUpdating,
                onToggle = onToggleFollow
            )
        }
    }
}

@Composable
internal fun ArtistTabChips(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MeloxShapes.capsule
    val primaryContent = playlistModernListPrimaryContentColor()
    val secondaryContent = playlistModernListSecondaryContentColor()
    val labels = listOf(
        R.string.artist_tab_songs to 0,
        R.string.artist_tab_albums to 1
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(shape)
            .background(primaryContent.copy(alpha = 0.07f), shape)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEach { (labelRes, index) ->
            val isSelected = selectedTab == index
            val highlight by animateColorAsState(
                targetValue = if (isSelected) {
                    primaryContent.copy(alpha = 0.16f)
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
                        primaryContent
                    } else {
                        secondaryContent
                    }
                )
            }
        }
    }
}
