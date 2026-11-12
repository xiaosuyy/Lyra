package com.lyra.player.ui.viewmodel.playlist

/*
 * LyraPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 LyraPlayer developers
 * https://github.com/cwuom/Lyra
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
 * File: com.lyra.player.ui.viewmodel.playlist/LocalPlaylistDetailViewModel
 * Updated: 2026/3/23
 */


import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyra.player.R
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.api.search.CustomChannelSong
import com.lyra.player.core.download.DownloadedSong
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.source.MusicSourceIds
import com.lyra.player.data.local.audioimport.LocalAudioImportManager
import com.lyra.player.data.local.audioimport.LocalAudioImportResult
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.data.local.playlist.system.LocalFilesPlaylist
import com.lyra.player.data.local.playlist.model.LocalPlaylist
import com.lyra.player.data.local.playlist.LocalPlaylistRepository
import com.lyra.player.data.local.playlist.LocalPlaylistDeleteResult
import com.lyra.player.data.local.playlist.LocalPlaylistSongDeleteResult
import com.lyra.player.data.local.playlist.runLocalPlaylistMutationSafely
import com.lyra.player.data.local.playlist.sync.NeteaseLikeSyncResult
import com.lyra.player.data.local.playlist.sync.NeteaseRemotePlaylist
import com.lyra.player.data.local.media.LocalSongSupport
import com.lyra.player.data.model.SongIdentity
import com.lyra.player.data.model.SongItem
import com.lyra.player.data.model.identity
import com.lyra.player.data.model.stableKey
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.ui.viewmodel.tab.parseNeteaseHomeSongs
import com.lyra.player.ui.viewmodel.tab.parseNeteaseLikedPlaylistId
import com.lyra.player.ui.viewmodel.tab.toSongItem
import org.json.JSONObject
import java.util.Locale

data class LocalPlaylistDetailUiState(
    val playlist: LocalPlaylist? = null,
    val isResolved: Boolean = false,
    val initializationFailed: Boolean = false,
    val requestedPlaylistId: Long? = null
)

data class LocalAudioImportUiResult(
    val importedCount: Int,
    val failedCount: Int,
    val addedSongs: List<SongItem> = emptyList(),
    val createdPlaylist: LocalPlaylist? = null
)

data class LocalFilesDownloadedSongDeleteUiResult(
    val deletedCount: Int,
    val notDeletedCount: Int
)

data class LocalScanPreviewState(
    val visible: Boolean = false,
    val isScanning: Boolean = false,
    val songs: List<SongItem> = emptyList(),
    val query: String = "",
    val metadataOnly: Boolean = false,
    val hideExistingLocalPlaylistSongs: Boolean = false,
    val existingLocalPlaylistKeys: Set<String> = emptySet(),
    val hideDuplicateMetadataSongs: Boolean = false,
    val duplicateMetadataKeys: Set<String> = emptySet(),
    val selectedKeys: Set<String> = emptySet()
)

/** 我喜欢的音乐里可参与导入 / 同步的已登录账号。 */
data class FavoritesChannelAccountInfo(
    val channelId: String,
    val displayName: String,
    val avatarUrl: String?
)

data class FavoritesChannelActionResult(
    val channelId: String,
    val added: Int = 0,
    val skippedExisting: Int = 0,
    val skippedUnsupported: Int = 0,
    val failed: Int = 0,
    val message: String? = null,
    val error: String? = null
)

internal fun scannedSongKeysAlreadyInLocalPlaylists(
    scannedSongs: List<SongItem>,
    localPlaylists: List<LocalPlaylist>
): Set<String> {
    if (scannedSongs.isEmpty() || localPlaylists.isEmpty()) return emptySet()

    val existingAddedLocalSongs = localPlaylists
        .asSequence()
        .flatMap { it.songs.asSequence() }
        .filter { LocalSongSupport.isLocalSong(it, null) }
        .toList()
    val existingAddedLocalIndex = LocalScanDuplicateIndex(existingAddedLocalSongs)
    return scannedSongs.asSequence()
        .filter { song ->
            LocalSongSupport.isLocalSong(song, null) && existingAddedLocalIndex.contains(song)
        }
        .mapTo(LinkedHashSet()) { it.stableKey() }
}

internal fun duplicateScannedSongKeysByMetadata(scannedSongs: List<SongItem>): Set<String> {
    if (scannedSongs.size < 2) return emptySet()

    val seenMetadata = HashSet<LocalScanMetadataFingerprint>(scannedSongs.size)
    return buildSet {
        scannedSongs.forEach { song ->
            val fingerprint = song.localScanMetadataFingerprint() ?: return@forEach
            if (!seenMetadata.add(fingerprint)) {
                add(song.stableKey())
            }
        }
    }
}

private data class LocalScanMetadataFingerprint(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

private fun SongItem.localScanMetadataFingerprint(): LocalScanMetadataFingerprint? {
    val title = normalizeLocalScanMetadataText(name)
    val artist = normalizeLocalScanMetadataText(artist)
    val album = normalizeLocalScanMetadataText(album)
    if (
        title.isBlank() ||
        artist.isBlank() ||
        album.isBlank() ||
        album == LocalSongSupport.LOCAL_ALBUM_IDENTITY ||
        durationMs <= 0L
    ) {
        return null
    }
    return LocalScanMetadataFingerprint(
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs
    )
}

private fun normalizeLocalScanMetadataText(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(LOCAL_SCAN_METADATA_WHITESPACE, " ")
}

private val LOCAL_SCAN_METADATA_WHITESPACE = Regex("\\s+")

private class LocalScanDuplicateIndex(songs: List<SongItem>) {
    private val identities = HashSet<SongIdentity>(songs.size)
    private val localKeys = HashSet<String>()

    init {
        songs.forEach { song ->
            identities += song.identity()
            localKeys += LocalSongSupport.localDuplicateKeys(
                song = song,
                includeMetadataFallback = true
            )
        }
    }

    fun contains(song: SongItem): Boolean {
        return song.identity() in identities ||
            LocalSongSupport.localDuplicateKeys(
                song = song,
                includeMetadataFallback = true
            ).any(localKeys::contains)
    }
}

data class LocalMetadataProcessingState(
    val isProcessing: Boolean = false,
    val playlistId: Long? = null,
    val processedCount: Int = 0,
    val totalCount: Int = 0
)

@Suppress("unused")
class LocalPlaylistDetailViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val TAG = "LocalPlaylistScanVM"
        const val MAX_IMPORT_SONGS = 5_000
    }

    private val app = application
    private val repo = LocalPlaylistRepository.getInstance(application)

    private val _uiState = MutableStateFlow(LocalPlaylistDetailUiState())
    val uiState: StateFlow<LocalPlaylistDetailUiState> = _uiState

    private val _scanPreviewState = MutableStateFlow(LocalScanPreviewState())
    val scanPreviewState: StateFlow<LocalScanPreviewState> = _scanPreviewState

    private val _metadataProcessingState = MutableStateFlow(LocalMetadataProcessingState())
    val metadataProcessingState: StateFlow<LocalMetadataProcessingState> = _metadataProcessingState

    private var playlistId: Long = 0L
    private var playlistCollectJob: Job? = null
    private var scanJob: Job? = null
    private var metadataRefreshJob: Job? = null
    private var metadataRefreshSessionId: Long = 0L
    private var scanSessionId: Long = 0L

    fun start(id: Long) {
        if (playlistId == id && _uiState.value.playlist?.id == id) return
        playlistId = id
        playlistCollectJob?.cancel()
        _uiState.value = LocalPlaylistDetailUiState(requestedPlaylistId = id)
        playlistCollectJob = viewModelScope.launch {
            if (!repo.awaitInitialized()) {
                if (playlistId != id) return@launch
                _uiState.value = LocalPlaylistDetailUiState(
                    initializationFailed = true,
                    requestedPlaylistId = id
                )
                return@launch
            }
            repo.playlists.collect { list ->
                _uiState.value = LocalPlaylistDetailUiState(
                    playlist = list.firstOrNull { it.id == id },
                    isResolved = true,
                    requestedPlaylistId = id
                )
            }
        }
    }

    fun rename(newName: String) {
        launchPlaylistMutation("renamePlaylist") {
            repo.renamePlaylist(playlistId, newName)
        }
    }

    fun scanDeviceSongs(onResult: (LocalAudioImportResult) -> Unit) {
        startLocalAudioScan(onResult) {
            LocalAudioImportManager.scanDeviceSongs(app)
        }
    }

    fun scanFolderSongs(folderUri: Uri, onResult: (LocalAudioImportResult) -> Unit) {
        startLocalAudioScan(onResult) {
            LocalAudioImportManager.scanFolderSongs(app, folderUri)
        }
    }

    private fun startLocalAudioScan(
        onResult: (LocalAudioImportResult) -> Unit,
        scanAction: suspend () -> LocalAudioImportResult
    ) {
        if (_scanPreviewState.value.isScanning) {
            _scanPreviewState.value = _scanPreviewState.value.copy(visible = true)
            return
        }

        scanJob?.cancel()
        val sessionId = ++scanSessionId
        val scanStartedAt = SystemClock.elapsedRealtime()
        _scanPreviewState.value = LocalScanPreviewState(visible = true, isScanning = true)
        LyraLogger.d(TAG, "start scan session=$sessionId")

        lateinit var currentJob: Job
        currentJob = viewModelScope.launch {
            try {
                val result = scanAction()
                if (!isActiveScanSession(sessionId, currentJob)) return@launch
                val scanElapsedMs = SystemClock.elapsedRealtime() - scanStartedAt
                LyraLogger.d(
                    TAG,
                    "scan action finished: session=$sessionId, songs=${result.songs.size}, failed=${result.failedCount}, completed=${result.completed}, elapsed=${scanElapsedMs}ms"
                )
                _scanPreviewState.value = if (result.completed) {
                    val prepareStartedAt = SystemClock.elapsedRealtime()
                    val localPlaylists = repo.playlists.value.toList()
                    val preparedState = withContext(Dispatchers.Default) {
                        val preparedSongs = prepareScannedSongs(result.songs)
                        LocalScanPreviewState(
                            visible = true,
                            isScanning = false,
                            songs = preparedSongs,
                            existingLocalPlaylistKeys = scannedSongKeysAlreadyInLocalPlaylists(
                                scannedSongs = preparedSongs,
                                localPlaylists = localPlaylists
                            ),
                            duplicateMetadataKeys = duplicateScannedSongKeysByMetadata(
                                preparedSongs
                            ),
                            selectedKeys = preparedSongs.mapTo(LinkedHashSet(preparedSongs.size)) {
                                it.stableKey()
                            }
                        )
                    }
                    val prepareElapsedMs = SystemClock.elapsedRealtime() - prepareStartedAt
                    LyraLogger.d(
                        TAG,
                        "prepareScannedSongs finished: session=$sessionId, input=${result.songs.size}, output=${preparedState.songs.size}, elapsed=${prepareElapsedMs}ms"
                    )
                    preparedState
                } else {
                    LocalScanPreviewState()
                }
                onResult(result)
            } catch (_: CancellationException) {
                // 用户主动返回时直接取消，不再回调已经离开的界面
                LyraLogger.d(TAG, "scan cancelled: session=$sessionId")
            } finally {
                if (scanJob === currentJob) {
                    scanJob = null
                }
                if (scanSessionId == sessionId && _scanPreviewState.value.isScanning) {
                    _scanPreviewState.value = _scanPreviewState.value.copy(isScanning = false)
                }
                LyraLogger.d(
                    TAG,
                    "scan session finished: session=$sessionId, totalElapsed=${SystemClock.elapsedRealtime() - scanStartedAt}ms"
                )
            }
        }
        scanJob = currentJob
    }

    fun cancelDeviceSongScan() {
        scanSessionId += 1
        scanJob?.cancel()
        scanJob = null
        if (_scanPreviewState.value.isScanning) {
            _scanPreviewState.value = _scanPreviewState.value.copy(isScanning = false)
        }
    }

    fun updateScanPreviewQuery(query: String) {
        _scanPreviewState.value = _scanPreviewState.value.copy(query = query)
    }

    fun updateScanPreviewSelection(selectedKeys: Set<String>) {
        _scanPreviewState.value = _scanPreviewState.value.copy(selectedKeys = selectedKeys)
    }

    fun updateScanPreviewMetadataOnly(metadataOnly: Boolean) {
        val current = _scanPreviewState.value
        if (current.metadataOnly == metadataOnly) return
        val selectedKeys = if (metadataOnly) {
            val metadataKeys = current.songs
                .asSequence()
                .filter(::hasMeaningfulScanMetadata)
                .mapTo(LinkedHashSet()) { it.stableKey() }
            current.selectedKeys.intersect(metadataKeys)
        } else {
            current.selectedKeys
        }
        _scanPreviewState.value = current.copy(
            metadataOnly = metadataOnly,
            selectedKeys = selectedKeys
        )
    }

    fun updateScanPreviewHideExistingLocalPlaylistSongs(
        hideExistingLocalPlaylistSongs: Boolean
    ) {
        val current = _scanPreviewState.value
        if (current.hideExistingLocalPlaylistSongs == hideExistingLocalPlaylistSongs) return
        _scanPreviewState.value = current.copy(
            hideExistingLocalPlaylistSongs = hideExistingLocalPlaylistSongs,
            selectedKeys = if (hideExistingLocalPlaylistSongs) {
                current.selectedKeys - current.existingLocalPlaylistKeys
            } else {
                current.selectedKeys
            }
        )
    }

    fun updateScanPreviewHideDuplicateMetadataSongs(hideDuplicateMetadataSongs: Boolean) {
        val current = _scanPreviewState.value
        if (current.hideDuplicateMetadataSongs == hideDuplicateMetadataSongs) return
        _scanPreviewState.value = current.copy(
            hideDuplicateMetadataSongs = hideDuplicateMetadataSongs,
            selectedKeys = if (hideDuplicateMetadataSongs) {
                current.selectedKeys - current.duplicateMetadataKeys
            } else {
                current.selectedKeys
            }
        )
    }

    fun clearScanPreview(cancelScan: Boolean) {
        if (cancelScan) {
            cancelDeviceSongScan()
        }
        _scanPreviewState.value = LocalScanPreviewState()
    }

    fun applyScannedSongs(
        songs: List<SongItem>,
        onResult: (LocalAudioImportUiResult) -> Unit
    ) {
        viewModelScope.launch {
            runLocalPlaylistMutationSafely("applyScannedSongs") {
                repo.addScannedSongsToLocalFilesPlaylistAndCount(songs)
            }.onSuccess { importedCount ->
                scheduleScannedMetadataRefresh(LocalFilesPlaylist.SYSTEM_ID, songs)
                onResult(
                    LocalAudioImportUiResult(
                        importedCount = importedCount,
                        failedCount = 0
                    )
                )
            }.onFailure {
                onResult(LocalAudioImportUiResult(importedCount = 0, failedCount = songs.size))
            }
        }
    }

    fun createPlaylistWithScannedSongs(
        name: String,
        songs: List<SongItem>,
        onResult: (LocalAudioImportUiResult) -> Unit
    ) {
        viewModelScope.launch {
            runLocalPlaylistMutationSafely("createPlaylistWithScannedSongs") {
                repo.createPlaylistWithScannedSongs(name, songs)
            }.onSuccess { playlist ->
                scheduleScannedMetadataRefresh(playlist.id, songs)
                onResult(
                    LocalAudioImportUiResult(
                        importedCount = playlist.songs.size,
                        failedCount = 0,
                        addedSongs = playlist.songs.toList(),
                        createdPlaylist = playlist
                    )
                )
            }.onFailure {
                onResult(LocalAudioImportUiResult(importedCount = 0, failedCount = songs.size))
            }
        }
    }

    fun addScannedSongsToPlaylist(
        targetPlaylistId: Long,
        songs: List<SongItem>,
        onResult: (LocalAudioImportUiResult) -> Unit
    ) {
        viewModelScope.launch {
            runLocalPlaylistMutationSafely("addScannedSongsToPlaylist") {
                repo.addScannedSongsToPlaylistWithResult(targetPlaylistId, songs)
            }.onSuccess { addResult ->
                scheduleScannedMetadataRefresh(targetPlaylistId, songs)
                onResult(
                    LocalAudioImportUiResult(
                        importedCount = addResult.addedCount,
                        failedCount = 0,
                        addedSongs = addResult.addedSongs
                    )
                )
            }.onFailure {
                onResult(LocalAudioImportUiResult(importedCount = 0, failedCount = songs.size))
            }
        }
    }

    fun removeSongs(
        songs: List<SongItem>,
        onResult: (Result<List<LocalPlaylistSongDeleteResult>>) -> Unit = {}
    ) {
        if (songs.isEmpty()) {
            onResult(Result.success(emptyList()))
            return
        }
        viewModelScope.launch {
            onResult(
                runLocalPlaylistMutationSafely("removeSongs") {
                    repo.removeSongsFromPlaylistByIdentityWithResult(playlistId, songs)
                }
            )
        }
    }

    fun deleteDownloadedSongs(
        songs: List<DownloadedSong>,
        onResult: (LocalFilesDownloadedSongDeleteUiResult) -> Unit
    ) {
        if (songs.isEmpty()) {
            onResult(LocalFilesDownloadedSongDeleteUiResult(0, 0))
            return
        }
        viewModelScope.launch {
            withContext(NonCancellable) {
                val deletion = GlobalDownloadManager.deleteDownloadedSongsWithResult(
                    context = app,
                    songs = songs
                )
                onResult(
                    LocalFilesDownloadedSongDeleteUiResult(
                        deletedCount = deletion.deletedSongs.size,
                        notDeletedCount = deletion.failedSongs.size
                    )
                )
            }
        }
    }

    fun clearSongs(
        onResult: (Result<List<LocalPlaylistSongDeleteResult>>) -> Unit = {}
    ) {
        viewModelScope.launch {
            onResult(
                runLocalPlaylistMutationSafely("clearSongs") {
                    repo.clearPlaylistSongsWithResult(playlistId)
                }
            )
        }
    }

    fun delete(onResult: (Result<List<LocalPlaylistDeleteResult>>) -> Unit) {
        viewModelScope.launch {
            onResult(
                runLocalPlaylistMutationSafely("deletePlaylist") {
                    repo.deletePlaylistsWithResult(listOf(playlistId))
                }
            )
        }
    }

    fun moveSong(from: Int, to: Int) {
        launchPlaylistMutation("moveSong") { repo.moveSong(playlistId, from, to) }
    }

    fun reorderSongs(newOrder: List<SongIdentity>) {
        launchPlaylistMutation("reorderSongs") { repo.reorderSongs(playlistId, newOrder) }
    }

    fun removeSong(songId: Long) {
        launchPlaylistMutation("removeSong") { repo.removeSongFromPlaylist(playlistId, songId) }
    }

    fun syncFavoritesToNeteaseLiked(onResult: (NeteaseLikeSyncResult) -> Unit) {
        viewModelScope.launch {
            val result = repo.syncFavoritesToNeteaseLiked(AppContainer.neteaseClient)
            onResult(result)
        }
    }

    fun syncSongsToNeteaseLiked(
        songs: List<SongItem>,
        onResult: (NeteaseLikeSyncResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repo.syncSongsToNeteaseLiked(AppContainer.neteaseClient, songs)
            onResult(result)
        }
    }

    fun fetchNeteaseRemotePlaylists(
        onResult: (Result<List<NeteaseRemotePlaylist>>) -> Unit
    ): Job {
        return viewModelScope.launch {
            onResult(
                runCatching {
                    repo.fetchNeteaseRemotePlaylists(AppContainer.neteaseClient)
                }
            )
        }
    }

    fun syncSongsToNeteasePlaylist(
        targetPlaylistId: Long,
        songs: List<SongItem>,
        onResult: (NeteaseLikeSyncResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repo.syncSongsToNeteasePlaylist(
                client = AppContainer.neteaseClient,
                targetPlaylistId = targetPlaylistId,
                songs = songs
            )
            onResult(result)
        }
    }

    fun loadFavoritesLinkedAccounts(onResult: (List<FavoritesChannelAccountInfo>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val accounts = buildList {
                val netease = AppContainer.neteaseClient
                if (runCatching { netease.hasLogin() }.getOrDefault(false)) {
                    val profile = runCatching {
                        JSONObject(netease.getCurrentUserAccount()).optJSONObject("profile")
                    }.getOrNull()
                    val nickname = profile?.optString("nickname")?.trim().orEmpty()
                    val avatar = profile?.optString("avatarUrl")
                        .orEmpty()
                        .ifBlank { profile?.optString("avatarImgUrl").orEmpty() }
                        .trim()
                    add(
                        FavoritesChannelAccountInfo(
                            channelId = MusicSourceIds.NETEASE,
                            displayName = nickname.ifBlank {
                                app.getString(R.string.settings_home_source_netease)
                            },
                            avatarUrl = avatar.takeIf { it.isNotBlank() }
                        )
                    )
                }
                val qqSession = AppContainer.qqMusicSessionRepo.getSessionOnce()
                if (qqSession.isLoggedIn) {
                    val profile = runCatching {
                        AppContainer.qqMusicAccountClient.fetchProfile(
                            cookie = qqSession.cookie,
                            uin = qqSession.uin,
                            musicKey = qqSession.musicKey
                        )
                    }.getOrNull()
                    add(
                        FavoritesChannelAccountInfo(
                            channelId = MusicSourceIds.TENCENT,
                            displayName = profile?.nickname?.takeIf { it.isNotBlank() }
                                ?: app.getString(R.string.library_tab_qqmusic),
                            avatarUrl = profile?.avatarUrl
                        )
                    )
                }
            }
            withContext(Dispatchers.Main) { onResult(accounts) }
        }
    }

    fun importFavoritesFromChannels(
        channelIds: Set<String>,
        onResult: (List<FavoritesChannelActionResult>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = channelIds.map { channelId ->
                runCatching {
                    when (channelId) {
                        MusicSourceIds.NETEASE -> importNeteaseLikedSongs()
                        MusicSourceIds.TENCENT -> importQQMusicLikedSongs()
                        else -> FavoritesChannelActionResult(
                            channelId = channelId,
                            error = "unsupported channel"
                        )
                    }
                }.getOrElse { error ->
                    LyraLogger.e(TAG, "import favorites from $channelId failed", error)
                    FavoritesChannelActionResult(
                        channelId = channelId,
                        error = error.message ?: error::class.java.simpleName
                    )
                }
            }
            withContext(Dispatchers.Main) { onResult(results) }
        }
    }

    fun pushFavoritesToChannels(
        channelIds: Set<String>,
        songs: List<SongItem>,
        onResult: (List<FavoritesChannelActionResult>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = channelIds.map { channelId ->
                runCatching {
                    when (channelId) {
                        MusicSourceIds.NETEASE -> pushFavoritesToNetease(songs)
                        MusicSourceIds.TENCENT -> pushFavoritesToQQMusic(songs)
                        else -> FavoritesChannelActionResult(
                            channelId = channelId,
                            error = "unsupported channel"
                        )
                    }
                }.getOrElse { error ->
                    LyraLogger.e(TAG, "push favorites to $channelId failed", error)
                    FavoritesChannelActionResult(
                        channelId = channelId,
                        error = error.message ?: error::class.java.simpleName
                    )
                }
            }
            withContext(Dispatchers.Main) { onResult(results) }
        }
    }

    private suspend fun importNeteaseLikedSongs(): FavoritesChannelActionResult {
        val client = AppContainer.neteaseClient
        if (!client.hasLogin()) {
            return FavoritesChannelActionResult(
                channelId = MusicSourceIds.NETEASE,
                error = getApplication<Application>().getString(R.string.playback_login_required)
            )
        }
        var raw = client.getLikedPlaylistId(0)
        var likedPlaylistId = parseNeteaseLikedPlaylistId(raw)
        if (likedPlaylistId == null && JSONObject(raw).optInt("code", -1) == 301) {
            runCatching { client.ensureWeapiSession() }
            raw = client.getLikedPlaylistId(0)
            likedPlaylistId = parseNeteaseLikedPlaylistId(raw)
        }
        if (likedPlaylistId == null) {
            return FavoritesChannelActionResult(
                channelId = MusicSourceIds.NETEASE,
                error = "无法读取网易云「我喜欢的音乐」"
            )
        }
        val songs = parseNeteaseHomeSongs(client.getPlaylistDetail(likedPlaylistId))
        if (songs.isEmpty()) {
            return FavoritesChannelActionResult(channelId = MusicSourceIds.NETEASE)
        }
        val added = repo.addSongsToPlaylistWithResult(
            playlistId = FavoritesPlaylist.SYSTEM_ID,
            songs = songs
        ).addedCount
        return FavoritesChannelActionResult(
            channelId = MusicSourceIds.NETEASE,
            added = added,
            skippedExisting = (songs.size - added).coerceAtLeast(0)
        )
    }

    private suspend fun importQQMusicLikedSongs(): FavoritesChannelActionResult {
        val session = AppContainer.qqMusicSessionRepo.getSessionOnce()
        if (!session.isLoggedIn) {
            return FavoritesChannelActionResult(
                channelId = MusicSourceIds.TENCENT,
                error = getApplication<Application>().getString(R.string.playback_login_required)
            )
        }
        val client = AppContainer.qqMusicFavoriteClient
        val allSongs = ArrayList<CustomChannelSong>()
        var begin = 0
        var total = Int.MAX_VALUE
        while (begin < total && allSongs.size < MAX_IMPORT_SONGS) {
            val page = client.fetchLikedSongs(session, songBegin = begin)
            if (page.songs.isEmpty()) break
            allSongs += page.songs
            begin += page.songs.size
            total = page.total
        }
        if (allSongs.isEmpty()) {
            return FavoritesChannelActionResult(channelId = MusicSourceIds.TENCENT)
        }
        val app = getApplication<Application>()
        val songItems = allSongs.map { it.toSongItem(app, MusicSourceIds.TENCENT) }
        val added = repo.addSongsToPlaylistWithResult(
            playlistId = FavoritesPlaylist.SYSTEM_ID,
            songs = songItems
        ).addedCount
        return FavoritesChannelActionResult(
            channelId = MusicSourceIds.TENCENT,
            added = added,
            skippedExisting = (songItems.size - added).coerceAtLeast(0)
        )
    }

    private suspend fun pushFavoritesToNetease(songs: List<SongItem>): FavoritesChannelActionResult {
        val client = AppContainer.neteaseClient
        if (!client.hasLogin()) {
            return FavoritesChannelActionResult(
                channelId = MusicSourceIds.NETEASE,
                error = getApplication<Application>().getString(R.string.playback_login_required)
            )
        }
        val result = repo.syncSongsToNeteaseLiked(client, songs)
        return FavoritesChannelActionResult(
            channelId = MusicSourceIds.NETEASE,
            added = result.added,
            skippedExisting = result.skippedExisting,
            skippedUnsupported = result.skippedUnsupported,
            failed = result.failed,
            message = result.message
        )
    }

    private suspend fun pushFavoritesToQQMusic(songs: List<SongItem>): FavoritesChannelActionResult {
        val session = AppContainer.qqMusicSessionRepo.getSessionOnce()
        if (!session.isLoggedIn) {
            return FavoritesChannelActionResult(
                channelId = MusicSourceIds.TENCENT,
                error = getApplication<Application>().getString(R.string.playback_login_required)
            )
        }
        val supported = songs.filter {
            it.channelId.equals(MusicSourceIds.TENCENT, ignoreCase = true) &&
                !it.audioId.isNullOrBlank()
        }
        if (supported.isEmpty()) {
            return FavoritesChannelActionResult(channelId = MusicSourceIds.TENCENT)
        }
        val client = AppContainer.qqMusicFavoriteClient
        val existingMids = runCatching { fetchAllQQMusicLikedMids(session) }
            .onFailure { LyraLogger.w(TAG, "read QQ liked mids failed: ${it.message}") }
            .getOrNull()
        val pending = if (existingMids == null) {
            supported
        } else {
            supported.filter { it.audioId !in existingMids }
        }
        val write = client.addFavorites(
            session = session,
            songMids = pending.mapNotNull { it.audioId }
        )
        return FavoritesChannelActionResult(
            channelId = MusicSourceIds.TENCENT,
            added = write.added,
            skippedExisting = supported.size - pending.size,
            skippedUnsupported = songs.size - supported.size,
            failed = write.failed
        )
    }

    private suspend fun fetchAllQQMusicLikedMids(
        session: com.lyra.player.data.auth.qqmusic.QQMusicSession
    ): Set<String> {
        val client = AppContainer.qqMusicFavoriteClient
        val mids = LinkedHashSet<String>()
        var begin = 0
        var total = Int.MAX_VALUE
        while (begin < total && mids.size < MAX_IMPORT_SONGS) {
            val page = client.fetchLikedSongs(session, songBegin = begin)
            if (page.songs.isEmpty()) break
            page.songs.mapNotNullTo(mids) { it.platformId.takeIf(String::isNotBlank) }
            begin += page.songs.size
            total = page.total
        }
        return mids
    }

    private fun isActiveScanSession(sessionId: Long, currentJob: Job): Boolean {
        return scanJob === currentJob && scanSessionId == sessionId
    }

    private fun scheduleScannedMetadataRefresh(targetPlaylistId: Long, songs: List<SongItem>) {
        val localSongs = songs.filter { LocalSongSupport.isLocalSong(it, app) }
        if (localSongs.isEmpty()) {
            if (_metadataProcessingState.value.playlistId == targetPlaylistId) {
                _metadataProcessingState.value = LocalMetadataProcessingState()
            }
            return
        }

        metadataRefreshJob?.cancel()
        val sessionId = ++metadataRefreshSessionId
        _metadataProcessingState.value = LocalMetadataProcessingState(
            isProcessing = true,
            playlistId = targetPlaylistId,
            processedCount = 0,
            totalCount = localSongs.size
        )
        metadataRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                runLocalPlaylistMutationSafely("refreshScannedLocalSongMetadata") {
                    repo.refreshScannedLocalSongMetadata(
                        songs = localSongs,
                        includeEmbeddedAssets = false
                    ) { processed, total ->
                        if (metadataRefreshSessionId == sessionId) {
                            _metadataProcessingState.value = LocalMetadataProcessingState(
                                isProcessing = processed < total,
                                playlistId = targetPlaylistId,
                                processedCount = processed,
                                totalCount = total
                            )
                        }
                    }
                }
            } finally {
                if (metadataRefreshSessionId == sessionId) {
                    _metadataProcessingState.value = LocalMetadataProcessingState()
                }
            }
        }
    }

    private fun launchPlaylistMutation(
        operation: String,
        mutation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            runLocalPlaylistMutationSafely(operation, mutation)
        }
    }

    private fun prepareScannedSongs(songs: List<SongItem>): List<SongItem> {
        return songs.sortedWith(localScanSongComparator())
    }

    private fun localScanSongComparator(): Comparator<SongItem> {
        return compareByDescending<SongItem> { metadataRichnessScore(it) }
            .thenByDescending { it.durationMs.coerceAtLeast(0L) }
            .thenBy { it.name.lowercase() }
            .thenBy { it.artist.lowercase() }
            .thenBy { it.album.lowercase() }
            .thenBy { it.localFilePath.orEmpty().lowercase() }
            .thenBy { it.stableKey() }
    }

    private fun metadataRichnessScore(song: SongItem): Int {
        val fileTitle = song.localFileName
            ?.substringBeforeLast('.', song.localFileName)
            ?.trim()
            .orEmpty()
        var score = 0

        val hasMeaningfulTitle = song.name.isNotBlank()
        if (hasMeaningfulTitle) {
            score += if (fileTitle.isNotBlank() && !song.name.equals(fileTitle, ignoreCase = true)) 3 else 1
        }

        if (song.artist.isMeaningfulMetadata(app.getString(com.lyra.player.R.string.music_unknown_artist))) {
            score += 2
        }
        if (song.album.isMeaningfulAlbum(app)) {
            score += 2
        }
        if (song.durationMs > 0L) {
            score += 1
        }
        if (!song.coverUrl.isNullOrBlank() || !song.originalCoverUrl.isNullOrBlank()) {
            score += 1
        }
        if (!song.originalName.isNullOrBlank() && !song.originalName.equals(fileTitle, ignoreCase = true)) {
            score += 1
        }
        if (!song.originalArtist.isNullOrBlank()) {
            score += 1
        }

        return score
    }

    private fun hasMeaningfulScanMetadata(song: SongItem): Boolean {
        val unknownArtist = app.getString(com.lyra.player.R.string.music_unknown_artist)
        val fileTitle = song.localFileName
            ?.substringBeforeLast('.', song.localFileName)
            ?.trim()
            .orEmpty()
        val hasTitleMetadata = song.name.isNotBlank() &&
            (fileTitle.isBlank() || !song.name.equals(fileTitle, ignoreCase = true))
        return hasTitleMetadata ||
            song.artist.isMeaningfulMetadata(unknownArtist) ||
            song.album.isMeaningfulAlbum(app) ||
            !song.coverUrl.isNullOrBlank() ||
            !song.originalCoverUrl.isNullOrBlank()
    }
}

private fun String?.isMeaningfulMetadata(unknownArtist: String): Boolean {
    val value = this?.trim().orEmpty()
    return value.isNotBlank() && !value.equals(unknownArtist, ignoreCase = true)
}

private fun String?.isMeaningfulAlbum(application: Application): Boolean {
    val value = this?.trim().orEmpty()
    if (value.isBlank()) return false
    if (value == LocalSongSupport.LOCAL_ALBUM_IDENTITY) return false
    return !LocalFilesPlaylist.matches(value, application)
}
