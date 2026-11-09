package com.lyra.player.ui.viewmodel.artist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lyra.player.R
import com.lyra.player.core.api.search.CustomChannelArtist
import com.lyra.player.core.api.search.CustomChannelArtistAlbum
import com.lyra.player.core.api.search.CustomChannelArtistDetail
import com.lyra.player.core.api.search.CustomChannelSearchApi
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.api.search.tencentArtistAvatarUrl
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.toSongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LYRA-TencentArtistVM"
private const val SONG_PAGE_SIZE = 50
private const val ALBUM_PAGE_SIZE = 30

internal data class TencentArtistHeader(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val brief: String,
    val fanCount: Int,
    val songCount: Int,
    val albumCount: Int
)

internal data class TencentAlbumSongsState(
    val album: CustomChannelArtistAlbum,
    val loading: Boolean = true,
    val songs: List<SongItem> = emptyList(),
    val error: String? = null
)

internal data class TencentArtistDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val header: TencentArtistHeader? = null,
    val songs: List<SongItem> = emptyList(),
    val albums: List<CustomChannelArtistAlbum> = emptyList(),
    val songsHasMore: Boolean = false,
    val albumsHasMore: Boolean = false,
    val songsLoadingMore: Boolean = false,
    val albumsLoadingMore: Boolean = false,
    val albumSongs: TencentAlbumSongsState? = null
)

internal class TencentArtistDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val api = CustomChannelSearchApi(AppContainer.sharedOkHttpClient)
    private val app = getApplication<Application>()
    private val _uiState = MutableStateFlow(TencentArtistDetailUiState())
    val uiState: StateFlow<TencentArtistDetailUiState> = _uiState

    private var artistId: String = ""
    private var currentArtist: CustomChannelArtist? = null
    private var songOffset = 0
    private var albumOffset = 0
    private var loadJob: Job? = null

    fun start(artist: CustomChannelArtist, forceRefresh: Boolean = false) {
        if (!forceRefresh && artistId == artist.id && _uiState.value.header != null) return

        currentArtist = artist
        artistId = artist.id
        songOffset = 0
        albumOffset = 0
        loadJob?.cancel()
        _uiState.value = TencentArtistDetailUiState(
            loading = true,
            header = TencentArtistHeader(
                id = artist.id,
                name = artist.name,
                avatarUrl = tencentArtistAvatarUrl(artist.id),
                brief = "",
                fanCount = 0,
                songCount = 0,
                albumCount = 0
            )
        )

        loadJob = viewModelScope.launch {
            try {
                val detail = withContext(Dispatchers.IO) { api.fetchTencentArtist(artist.id) }
                if (artistId != artist.id) return@launch
                val songs = mapSongs(detail.songs)
                songOffset = detail.songs.size
                albumOffset = detail.albums.size
                _uiState.value = TencentArtistDetailUiState(
                    loading = false,
                    header = detail.toHeader(fallback = artist),
                    songs = songs,
                    albums = detail.albums,
                    songsHasMore = detail.songCount > detail.songs.size,
                    albumsHasMore = detail.albumCount > detail.albums.size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LyraLogger.e(TAG, "load tencent artist failed", e)
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = app.getString(
                            R.string.artist_load_failed,
                            e.message ?: e.javaClass.simpleName
                        )
                    )
                }
            }
        }
    }

    fun retry() {
        currentArtist?.let { start(it, forceRefresh = true) }
    }

    fun loadMoreSongs() {
        val state = _uiState.value
        if (artistId.isBlank() || state.songsLoadingMore || !state.songsHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(songsLoadingMore = true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.fetchTencentArtistSongs(artistId, songOffset, SONG_PAGE_SIZE)
                }
            }.onSuccess { page ->
                songOffset += page.items.size
                _uiState.update {
                    it.copy(
                        songs = it.songs + mapSongs(page.items),
                        songsHasMore = page.hasMore,
                        songsLoadingMore = false
                    )
                }
            }.onFailure { error ->
                LyraLogger.e(TAG, "load more tencent artist songs failed", error)
                _uiState.update { it.copy(songsLoadingMore = false) }
            }
        }
    }

    fun loadMoreAlbums() {
        val state = _uiState.value
        if (artistId.isBlank() || state.albumsLoadingMore || !state.albumsHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(albumsLoadingMore = true) }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.fetchTencentArtistAlbums(artistId, albumOffset, ALBUM_PAGE_SIZE)
                }
            }.onSuccess { page ->
                albumOffset += page.items.size
                _uiState.update {
                    it.copy(
                        albums = it.albums + page.items,
                        albumsHasMore = page.hasMore,
                        albumsLoadingMore = false
                    )
                }
            }.onFailure { error ->
                LyraLogger.e(TAG, "load more tencent artist albums failed", error)
                _uiState.update { it.copy(albumsLoadingMore = false) }
            }
        }
    }

    fun openAlbum(album: CustomChannelArtistAlbum) {
        _uiState.update { it.copy(albumSongs = TencentAlbumSongsState(album = album)) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { api.fetchTencentAlbumSongs(album.mid) }
            }.onSuccess { songs ->
                _uiState.update { state ->
                    val sheet = state.albumSongs ?: return@update state
                    if (sheet.album.mid != album.mid) return@update state
                    state.copy(
                        albumSongs = sheet.copy(loading = false, songs = mapSongs(songs))
                    )
                }
            }.onFailure { error ->
                LyraLogger.e(TAG, "load tencent album songs failed", error)
                _uiState.update { state ->
                    val sheet = state.albumSongs ?: return@update state
                    state.copy(
                        albumSongs = sheet.copy(
                            loading = false,
                            error = app.getString(
                                R.string.artist_load_failed,
                                error.message ?: error.javaClass.simpleName
                            )
                        )
                    )
                }
            }
        }
    }

    fun dismissAlbum() {
        _uiState.update { it.copy(albumSongs = null) }
    }

    private fun mapSongs(songs: List<CustomChannelSong>): List<SongItem> {
        return songs.map { it.toSongItem(app, MusicSourceIds.TENCENT) }
    }

    private fun CustomChannelArtistDetail.toHeader(
        fallback: CustomChannelArtist
    ): TencentArtistHeader {
        return TencentArtistHeader(
            id = id,
            name = name.ifBlank { fallback.name },
            avatarUrl = avatarUrl.orEmpty().ifBlank { tencentArtistAvatarUrl(fallback.id) },
            brief = brief,
            fanCount = fanCount,
            songCount = songCount,
            albumCount = albumCount
        )
    }
}
