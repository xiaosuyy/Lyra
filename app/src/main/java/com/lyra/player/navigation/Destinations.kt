package com.lyra.player.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
 * See the GNU General Public License for more details
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * File: com.lyra.player.navigation/Destinations
 * Created: 2025/8/8
 */

sealed class Destinations(val route: String, val labelResId: Int) {
    // TAB
    data object Home : Destinations("home", com.lyra.player.R.string.nav_home)
    data object Explore : Destinations("explore", com.lyra.player.R.string.nav_explore)
    data object Library : Destinations("library", com.lyra.player.R.string.nav_library)
    data object Settings : Destinations("settings", com.lyra.player.R.string.nav_settings)
    object Recent {
        const val ROUTE = "recent"
        val route: String
            get() = ROUTE
    }

    object PlaybackStats {
        const val ROUTE = "playback_stats"
        val route: String
            get() = ROUTE
    }

    // DEBUG
    data object Debug : Destinations("debug", com.lyra.player.R.string.nav_debug)
    data object DebugUsbExclusive : Destinations("debug/usb_exclusive", com.lyra.player.R.string.debug_usb_exclusive_title)
    data object DebugYouTube : Destinations("debug/youtube", com.lyra.player.R.string.common_youtube)
    data object DebugBili : Destinations("debug/bili", com.lyra.player.R.string.debug_bili_api)
    data object DebugNetease : Destinations("debug/netease", com.lyra.player.R.string.debug_netease_api)
    data object DebugSearch : Destinations("debug/search", com.lyra.player.R.string.debug_search_api)
    data object DebugLogsList : Destinations("debug_logs_list", com.lyra.player.R.string.log_list)
    data object DebugCrashLogsList : Destinations("debug_crash_logs_list", com.lyra.player.R.string.log_list)

    // 网易云歌单详情路由
    data object PlaylistDetail : Destinations("playlist_detail/{playlistJson}", com.lyra.player.R.string.playlist_detail)

    // 网易云专辑详情路由
    data object NeteaseAlbumDetail : Destinations("netease_album_detail/{playlistJson}", com.lyra.player.R.string.common_album_detail)

    // 网易云歌手详情路由
    data object NeteaseArtistDetail : Destinations("netease_artist_detail/{artistJson}", com.lyra.player.R.string.artist_detail)

    // B 站收藏夹详情路由
    data object BiliPlaylistDetail : Destinations("bili_playlist_detail/{playlistJson}", com.lyra.player.R.string.playlist_detail)

    // B 站 UP 主详情路由
    data object BiliUploaderDetail : Destinations("bili_uploader_detail/{uploaderJson}", com.lyra.player.R.string.bili_uploader_detail)

    // YouTube Music 创作者详情路由
    data object YouTubeMusicCreatorDetail : Destinations("youtube_music_creator_detail/{creatorJson}", com.lyra.player.R.string.artist_detail)

    // YouTube Music 播放列表或专辑详情路由
    data object YouTubeMusicPlaylistDetail : Destinations("youtube_music_playlist_detail/{playlistJson}", com.lyra.player.R.string.playlist_detail)

    // 本地歌单详情路由
    data object LocalPlaylistDetail : Destinations("local_playlist_detail/{playlistId}", com.lyra.player.R.string.playlist_local_detail)

    // 下载管理器路由
    data object DownloadManager : Destinations("download_manager", com.lyra.player.R.string.download_manager)

    // 下载进度路由
    data object DownloadProgress : Destinations("download_progress", com.lyra.player.R.string.download_progress)

    data object DebugLogViewer : Destinations("debug_log_viewer/{filePath}", com.lyra.player.R.string.log_view) {
        fun createRoute(filePath: String): String {
            val encodedPath = URLEncoder.encode(filePath, StandardCharsets.UTF_8.name())
            return "debug_log_viewer/$encodedPath"
        }
    }
}
