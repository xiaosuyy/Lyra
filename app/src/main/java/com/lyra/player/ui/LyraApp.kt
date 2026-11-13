package com.lyra.player.ui

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
 * File: com.lyra.player.ui/LyraApp
 * Created: 2025/8/8
 */

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import com.lyra.player.ui.melox.components.MeloxBottomChrome
import com.lyra.player.ui.melox.components.MeloxMiniPlayer
import com.lyra.player.ui.melox.components.MeloxMiniTrack
import com.lyra.player.ui.melox.components.MeloxTabItem
import com.lyra.player.ui.component.overlay.AppUpdateStartupHost
import com.lyra.player.ui.component.overlay.DensityScaledAlertDialog as AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.Coil
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.lyra.player.R
import com.lyra.player.core.api.bili.BiliClient
import com.lyra.player.core.api.search.CustomChannelArtist
import com.lyra.player.core.api.search.MusicPlatform
import com.lyra.player.core.api.youtube.YouTubeMusicCreatorSummary
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.download.GlobalDownloadManager
import com.lyra.player.core.download.ManagedDownloadStorage
import com.lyra.player.core.player.effects.AudioReactive
import com.lyra.player.core.player.PlayerManager
import com.lyra.player.core.player.metadata.PlayerLyricsProvider
import com.lyra.player.core.player.lifecycle.recoverUsbExclusivePlaybackOnForeground
import com.lyra.player.core.player.lifecycle.updateUsbExclusiveForegroundState
import com.lyra.player.core.player.policy.usb.shouldPromptForUsbExclusiveBackgroundPermission
import com.lyra.player.core.player.service.AudioPlayerService
import com.lyra.player.data.local.media.LocalSongSupport
import com.lyra.player.core.startup.player.PlayerStartupBootstrapper
import com.lyra.player.core.startup.player.PlayerStartupAudioFocusRefresher
import com.lyra.player.core.startup.player.PlayerStartupHistoryRecorder
import com.lyra.player.core.startup.player.PlayerStartupServiceSyncCoordinator
import com.lyra.player.core.startup.theme.StartupThemeResolver
import com.lyra.player.data.model.displayArtist
import com.lyra.player.data.model.displayCoverUrl
import com.lyra.player.data.model.displayName
import com.lyra.player.data.model.sameIdentityAs
import com.lyra.player.data.model.stableKey
import com.lyra.player.data.local.playlist.system.FavoritesPlaylist
import com.lyra.player.data.playlist.usage.UsageEntry
import com.lyra.player.data.profile.UserProfileStore
import com.lyra.player.data.settings.DEFAULT_ENHANCED_ADVANCED_BLUR_RADIUS_DP
import com.lyra.player.data.settings.AdvancedBlurQualityPreference
import com.lyra.player.data.settings.FloatingLyricsPreferences
import com.lyra.player.data.settings.LyricFontScaleTarget
import com.lyra.player.data.settings.LyricFontScales
import com.lyra.player.data.settings.PlaybackPreferenceSnapshot
import com.lyra.player.data.settings.ThemeDefaults
import com.lyra.player.data.settings.ThemeMode
import com.lyra.player.data.settings.ThemePreferenceSnapshot
import com.lyra.player.data.settings.isCurrentBuildDimensity
import com.lyra.player.data.settings.readPlaybackPreferenceSnapshotCached
import com.lyra.player.data.storage.clearExtraStorageCaches
import com.lyra.player.data.traffic.TrafficNetworkType
import com.lyra.player.navigation.Destinations
import com.lyra.player.navigation.LauncherShortcutAction
import com.lyra.player.navigation.LauncherShortcutRequest
import com.lyra.player.navigation.launcherShortcutMainTabRoute
import com.lyra.player.ui.component.playback.resolvePlaybackWaiting
import com.lyra.player.ui.component.common.ThemeRevealOverlay
import com.lyra.player.ui.component.common.blockUnderlyingTouches
import com.lyra.player.ui.effect.glass.AdvancedGlassController
import com.lyra.player.ui.effect.glass.AdvancedGlassHost
import com.lyra.player.ui.effect.glass.AdvancedGlassNavigationHandoff
import com.lyra.player.ui.effect.glass.AdvancedGlassSceneMotion
import com.lyra.player.ui.effect.glass.AdvancedGlassSceneLayer
import com.lyra.player.ui.effect.glass.DRAWER_BACKGROUND_SINK_FRACTION
import com.lyra.player.ui.effect.glass.DRAWER_RECESSED_CONTENT_SCALE
import com.lyra.player.ui.effect.glass.advancedGlassSceneZIndex
import com.lyra.player.ui.effect.glass.animateAdvancedGlassVisibilitySceneMotion
import com.lyra.player.ui.effect.glass.captureAdvancedGlassBackdrop
import com.lyra.player.ui.effect.glass.isAdvancedGlassBackendSupported
import com.lyra.player.ui.effect.glass.rememberAdvancedGlassBackdrop
import com.lyra.player.ui.feedback.AppFeedback
import com.lyra.player.ui.feedback.AppFeedbackHostEffect
import com.lyra.player.ui.feedback.LyraSnackbarHost
import com.lyra.player.ui.feedback.showNeriSnackbar
import com.lyra.player.ui.screen.DownloadManagerScreen
import com.lyra.player.ui.screen.DownloadProgressScreen
import com.lyra.player.ui.screen.NowPlayingScreen
import com.lyra.player.ui.screen.RecentScreen
import com.lyra.player.ui.screen.PlaybackStatsScreen
import com.lyra.player.ui.screen.account.AccountScreen
import com.lyra.player.ui.screen.account.CloudPlaylistsScreen
import com.lyra.player.ui.screen.account.FollowedArtistsScreen
import com.lyra.player.ui.screen.debug.BiliApiProbeScreen
import com.lyra.player.ui.screen.debug.CrashLogListScreen
import com.lyra.player.ui.screen.debug.DebugCrashTestType
import com.lyra.player.ui.screen.debug.DebugHomeScreen
import com.lyra.player.ui.screen.debug.LogListScreen
import com.lyra.player.ui.screen.debug.NeteaseApiProbeScreen
import com.lyra.player.ui.screen.debug.SearchApiProbeScreen
import com.lyra.player.ui.screen.debug.UsbExclusiveDebugScreen
import com.lyra.player.ui.screen.debug.YouTubeApiProbeScreen
import com.lyra.player.ui.screen.artist.BiliUploaderDetailScreen
import com.lyra.player.ui.screen.artist.NeteaseArtistDetailScreen
import com.lyra.player.ui.screen.artist.TencentArtistDetailScreen
import com.lyra.player.ui.screen.artist.YouTubeMusicCreatorNavigationScreen
import com.lyra.player.ui.screen.host.ExploreHostScreen
import com.lyra.player.ui.screen.host.HomeHostScreen
import com.lyra.player.ui.screen.host.LibraryHostScreen
import com.lyra.player.ui.screen.host.SettingsHostScreen
import com.lyra.player.ui.screen.host.rememberHomeHostRuntimeState
import com.lyra.player.ui.screen.tab.shouldShowHomeContinueSection
import com.lyra.player.ui.screen.playlist.BiliPlaylistDetailScreen
import com.lyra.player.ui.screen.playlist.LocalPlaylistDetailScreen
import com.lyra.player.ui.screen.playlist.NeteaseAlbumDetailScreen
import com.lyra.player.ui.screen.playlist.NeteasePlaylistDetailScreen
import com.lyra.player.ui.screen.playlist.YouTubeMusicPlaylistDetailScreen
import com.lyra.player.ui.theme.LyraTheme
import com.lyra.player.ui.theme.rememberActualSystemDarkTheme
import com.lyra.player.ui.util.rememberSongDisplayCoverUrl
import com.lyra.player.ui.view.HyperBackground
import com.lyra.player.ui.viewmodel.debug.LogViewerScreen
import com.lyra.player.data.model.NeteaseArtistSummary
import com.lyra.player.data.playlist.favorite.FAVORITE_SOURCE_TENCENT_ARTIST
import com.lyra.player.data.model.BiliUploaderSummary
import com.lyra.player.ui.viewmodel.playlist.BiliVideoItem
import com.lyra.player.data.model.SongItem
import com.lyra.player.ui.viewmodel.tab.AlbumSummary
import com.lyra.player.ui.viewmodel.tab.BiliPlaylist
import com.lyra.player.ui.viewmodel.tab.PlaylistSummary
import com.lyra.player.ui.viewmodel.tab.YouTubeMusicPlaylist
import com.lyra.player.util.crash.AnrWatchdog
import com.lyra.player.util.media.CoverArtColorCache
import com.lyra.player.core.crash.ExceptionHandler
import com.lyra.player.util.crash.NativeCrashHandler
import com.lyra.player.core.logging.LyraLogger
import com.lyra.player.util.media.adjustedAccentColorArgb
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lyra.player.ui.melox.glass.LocalMeloxBackdrop
import com.lyra.player.ui.melox.glass.meloxDragGlassVeil
import com.lyra.player.ui.haptic.HapticTextButton
import com.lyra.player.ui.haptic.performHapticFeedback
import com.lyra.player.util.platform.openAppBackgroundSettings
import com.lyra.player.util.platform.readBackgroundBehaviorAllowance
import com.lyra.player.util.platform.requestIgnoreBatteryOptimizationsCompat
import com.lyra.player.util.platform.LanguageManager
import com.lyra.player.util.format.formatFileSize
import com.lyra.player.util.media.isRemoteImageSource
import com.lyra.player.ui.network.rememberOfflineModeState
import com.lyra.player.ui.haptic.syncHapticFeedbackSetting
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private val navigationGson: Gson by lazy(LazyThreadSafetyMode.PUBLICATION) { Gson() }
private val EmptyLauncherShortcutRequestFlow =
    MutableStateFlow<LauncherShortcutRequest?>(null)
private const val LAUNCHER_SHORTCUT_PLAYLIST_READY_TIMEOUT_MS = 5000L
private val MAIN_TAB_ROUTES = listOf(
    Destinations.Home.route,
    Destinations.Explore.route,
    Destinations.Library.route,
    Destinations.Settings.route,
    Destinations.Debug.route
)
private val TRANSPARENT_MAIN_TAB_DETAIL_ROUTES = setOf(
    Destinations.PlaylistDetail.route,
    Destinations.NeteaseAlbumDetail.route,
    Destinations.NeteaseArtistDetail.route,
    Destinations.TencentArtistDetail.route,
    Destinations.BiliPlaylistDetail.route,
    Destinations.BiliUploaderDetail.route,
    Destinations.YouTubeMusicCreatorDetail.route,
    Destinations.YouTubeMusicPlaylistDetail.route,
    Destinations.LocalPlaylistDetail.route,
    Destinations.Recent.route,
    Destinations.PlaybackStats.route,
    Destinations.Account.route,
    Destinations.CloudPlaylists.route,
    Destinations.FollowedArtists.route,
    Destinations.DownloadManager.route,
    Destinations.DownloadProgress.route
)
private val DEBUG_NAVIGATION_DEPTH_BY_ROUTE = mapOf(
    Destinations.Debug.route to 0,
    Destinations.DebugUsbExclusive.route to 1,
    Destinations.DebugYouTube.route to 1,
    Destinations.DebugBili.route to 1,
    Destinations.DebugNetease.route to 1,
    Destinations.DebugSearch.route to 1,
    Destinations.DebugLogsList.route to 1,
    Destinations.DebugCrashLogsList.route to 1,
    Destinations.DebugLogViewer.route to 2
)
private val DEBUG_MAIN_TAB_CHILD_ROUTES = DEBUG_NAVIGATION_DEPTH_BY_ROUTE
    .filterValues { depth -> depth > 0 }
    .keys

private fun transparentNavigationDepth(route: String?): Int {
    val debugDepth = DEBUG_NAVIGATION_DEPTH_BY_ROUTE[route]
    if (debugDepth != null) return debugDepth
    return when {
        route == Destinations.NeteaseAlbumDetail.route ||
            route == Destinations.YouTubeMusicPlaylistDetail.route ||
            route == Destinations.DownloadProgress.route -> 2
        route in TRANSPARENT_MAIN_TAB_DETAIL_ROUTES -> 1
        else -> 0
    }
}

internal fun shouldUseInstantBiliUploaderPlaylistTransition(
    initialRoute: String?,
    targetRoute: String?
): Boolean {
    return (initialRoute == Destinations.BiliUploaderDetail.route &&
        targetRoute == Destinations.BiliPlaylistDetail.route) ||
        (initialRoute == Destinations.BiliPlaylistDetail.route &&
            targetRoute == Destinations.BiliUploaderDetail.route)
}

internal const val MAIN_TAB_DETAIL_OPEN_DURATION_MS = 220
internal const val MAIN_TAB_DETAIL_CLOSE_DURATION_MS = 240
internal const val DRAWER_DETAIL_OPEN_DURATION_MS = 300
internal const val DRAWER_DETAIL_CLOSE_DURATION_MS = 280
internal const val MAIN_TAB_LAYER_Z_INDEX = 0f
internal const val NAV_HOST_LAYER_Z_INDEX = 1f
internal const val MINI_PLAYER_OVERLAY_Z_INDEX = 2f
private const val DRAWER_ROOT_RETAIN_ALPHA = 0.999f
internal const val DEBUG_NAVIGATION_OPEN_DURATION_MS = 220
internal const val DEBUG_NAVIGATION_CLOSE_DURATION_MS = 240

internal enum class MainTabDetailHandoff {
    OPEN_DETAIL,
    RETURN_TO_TAB
}

internal enum class MainTabBackgroundMotion {
    NONE,
    COHERENT_EXIT,
    DRAWER_SINK
}

internal data class MainTabBackgroundTransform(
    val translationYFraction: Float,
    val scale: Float,
    val alpha: Float
)

internal fun resolveMainTabTransitionDirection(
    initialRoute: String?,
    targetRoute: String?
): Int? {
    val initialIndex = MAIN_TAB_ROUTES.indexOf(initialRoute).takeIf { it >= 0 } ?: return null
    val targetIndex = MAIN_TAB_ROUTES.indexOf(targetRoute).takeIf { it >= 0 } ?: return null
    if (initialIndex == targetIndex) return null
    return if (targetIndex > initialIndex) 1 else -1
}

internal fun shouldDispatchMainTabNavigation(
    currentRoute: String?,
    pendingRoute: String?,
    targetRoute: String
): Boolean = pendingRoute != targetRoute &&
    (currentRoute != targetRoute || pendingRoute != null)

internal fun shouldAcceptObservedMainTabRoute(
    observedRoute: String?,
    pendingRoute: String?
): Boolean = observedRoute != null &&
    observedRoute in MAIN_TAB_ROUTES &&
    (pendingRoute == null || pendingRoute == observedRoute)

internal fun shouldUseAdvancedGlassNavigationHandoff(
    visibleRoutes: Collection<String?>
): Boolean {
    val routes = visibleRoutes.filterNotNull().toSet()
    return routes.size > 1 && routes.any { it !in MAIN_TAB_ROUTES }
}

internal fun resolveMainTabDetailHandoff(
    initialRoute: String?,
    targetRoute: String?
): MainTabDetailHandoff? {
    if (initialRoute == null || targetRoute == null) return null
    val initialIsMainTab = initialRoute in MAIN_TAB_ROUTES
    val targetIsMainTab = targetRoute in MAIN_TAB_ROUTES
    return when {
        initialIsMainTab && targetRoute in TRANSPARENT_MAIN_TAB_DETAIL_ROUTES ->
            MainTabDetailHandoff.OPEN_DETAIL
        initialRoute in TRANSPARENT_MAIN_TAB_DETAIL_ROUTES && targetIsMainTab ->
            MainTabDetailHandoff.RETURN_TO_TAB
        else -> null
    }
}

internal fun resolveDebugNavigationTransitionDirection(
    initialRoute: String?,
    targetRoute: String?
): Int? {
    val initialDepth = DEBUG_NAVIGATION_DEPTH_BY_ROUTE[initialRoute] ?: return null
    val targetDepth = DEBUG_NAVIGATION_DEPTH_BY_ROUTE[targetRoute] ?: return null
    if (initialDepth == targetDepth) return null
    return if (targetDepth > initialDepth) 1 else -1
}

internal data class BottomBarLayoutInsets(
    val navContentBottomPadding: Dp,
    val screenBottomInset: Dp,
    val miniPlayerBottomPadding: Dp
)

internal fun resolveBottomBarLayoutInsets(
    baseBlurRequested: Boolean,
    bottomBarInset: Dp,
    reservedMiniPlayerHeight: Dp
): BottomBarLayoutInsets = if (baseBlurRequested) {
    BottomBarLayoutInsets(
        navContentBottomPadding = 0.dp,
        screenBottomInset = reservedMiniPlayerHeight + bottomBarInset,
        miniPlayerBottomPadding = bottomBarInset
    )
} else {
    BottomBarLayoutInsets(
        navContentBottomPadding = bottomBarInset,
        screenBottomInset = reservedMiniPlayerHeight,
        miniPlayerBottomPadding = 0.dp
    )
}

internal fun resolveMainTabBackgroundMotion(
    route: String?,
    coherentFeedbackEnabled: Boolean
): MainTabBackgroundMotion = when {
    route in DEBUG_MAIN_TAB_CHILD_ROUTES && coherentFeedbackEnabled ->
        MainTabBackgroundMotion.COHERENT_EXIT
    route in DEBUG_MAIN_TAB_CHILD_ROUTES -> MainTabBackgroundMotion.DRAWER_SINK
    route in TRANSPARENT_MAIN_TAB_DETAIL_ROUTES && coherentFeedbackEnabled ->
        MainTabBackgroundMotion.COHERENT_EXIT
    route in TRANSPARENT_MAIN_TAB_DETAIL_ROUTES -> MainTabBackgroundMotion.DRAWER_SINK
    else -> MainTabBackgroundMotion.NONE
}

internal fun resolveMainTabBackgroundTransform(
    motion: MainTabBackgroundMotion,
    progress: Float
): MainTabBackgroundTransform {
    val normalizedProgress = progress.coerceIn(0f, 1f)
    return when (motion) {
        MainTabBackgroundMotion.NONE -> MainTabBackgroundTransform(
            translationYFraction = 0f,
            scale = 1f,
            alpha = 1f
        )
        MainTabBackgroundMotion.COHERENT_EXIT -> MainTabBackgroundTransform(
            translationYFraction = -normalizedProgress,
            scale = 1f,
            alpha = 1f
        )
        MainTabBackgroundMotion.DRAWER_SINK -> MainTabBackgroundTransform(
            translationYFraction = DRAWER_BACKGROUND_SINK_FRACTION * normalizedProgress,
            scale = 1f - (1f - DRAWER_RECESSED_CONTENT_SCALE) * normalizedProgress,
            alpha = 1f
        )
    }
}

internal fun resolveMainTabBackgroundMotionDurationMillis(
    targetProgress: Float,
    coherentFeedbackEnabled: Boolean,
    debugSceneVisible: Boolean
): Int = when {
    debugSceneVisible && coherentFeedbackEnabled && targetProgress > 0f ->
        DEBUG_NAVIGATION_OPEN_DURATION_MS
    debugSceneVisible && coherentFeedbackEnabled -> DEBUG_NAVIGATION_CLOSE_DURATION_MS
    coherentFeedbackEnabled && targetProgress > 0f -> MAIN_TAB_DETAIL_OPEN_DURATION_MS
    coherentFeedbackEnabled -> MAIN_TAB_DETAIL_CLOSE_DURATION_MS
    targetProgress > 0f -> DRAWER_DETAIL_OPEN_DURATION_MS
    else -> DRAWER_DETAIL_CLOSE_DURATION_MS
}

internal fun mainTabDetailContentOffsetEasing(): Easing = FastOutSlowInEasing

internal fun shouldReleaseStartupGlassGate(
    baseBlurEnabled: Boolean,
    backgroundEffectReady: Boolean,
    contentEffectReady: Boolean
): Boolean {
    return !baseBlurEnabled || backgroundEffectReady || contentEffectReady
}

internal fun shouldShowStartupGlassGate(
    baseBlurEnabled: Boolean,
    gateReleased: Boolean,
    backgroundEffectReady: Boolean,
    contentEffectReady: Boolean
): Boolean {
    return baseBlurEnabled &&
        !gateReleased &&
        !backgroundEffectReady &&
        !contentEffectReady
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabEnterTransition(
    coherentFeedbackEnabled: Boolean = true
): EnterTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    val direction = resolveMainTabTransitionDirection(
        initialRoute = initialRoute,
        targetRoute = targetRoute
    )
    if (direction != null) {
        return EnterTransition.None
    }
    val debugDirection = resolveDebugNavigationTransitionDirection(
        initialRoute = initialRoute,
        targetRoute = targetRoute
    )
    if (debugDirection != null) {
        return if (coherentFeedbackEnabled) {
            debugNavigationEnterTransition(debugDirection)
        } else {
            fadeIn(
                initialAlpha = DRAWER_ROOT_RETAIN_ALPHA,
                animationSpec = tween(
                    durationMillis = if (debugDirection > 0) {
                        DRAWER_DETAIL_OPEN_DURATION_MS
                    } else {
                        DRAWER_DETAIL_CLOSE_DURATION_MS
                    },
                    easing = mainTabDetailContentOffsetEasing()
                )
            )
        }
    }
    return if (
        resolveMainTabDetailHandoff(initialRoute, targetRoute) ==
        MainTabDetailHandoff.RETURN_TO_TAB && coherentFeedbackEnabled
    ) {
        slideInVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_CLOSE_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> -fullHeight }
    } else {
        EnterTransition.None
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabExitTransition(
    coherentFeedbackEnabled: Boolean = true
): ExitTransition {
    val initialRoute = initialState.destination.route
    val targetRoute = targetState.destination.route
    val direction = resolveMainTabTransitionDirection(
        initialRoute = initialRoute,
        targetRoute = targetRoute
    )
    if (direction != null) {
        return ExitTransition.None
    }
    val debugDirection = resolveDebugNavigationTransitionDirection(
        initialRoute = initialRoute,
        targetRoute = targetRoute
    )
    if (debugDirection != null) {
        return if (coherentFeedbackEnabled) {
            debugNavigationExitTransition(debugDirection)
        } else {
            ExitTransition.KeepUntilTransitionsFinished
        }
    }
    return if (
        resolveMainTabDetailHandoff(initialRoute, targetRoute) ==
        MainTabDetailHandoff.OPEN_DETAIL && coherentFeedbackEnabled
    ) {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_OPEN_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> -fullHeight }
    } else {
        ExitTransition.None
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.transparentDetailEnterTransition(
    coherentFeedbackEnabled: Boolean = true
): EnterTransition {
    if (
        shouldUseInstantBiliUploaderPlaylistTransition(
            initialRoute = initialState.destination.route,
            targetRoute = targetState.destination.route
        )
    ) {
        return EnterTransition.None
    }
    val durationMillis = if (coherentFeedbackEnabled) {
        MAIN_TAB_DETAIL_OPEN_DURATION_MS
    } else {
        DRAWER_DETAIL_OPEN_DURATION_MS
    }
    return if (coherentFeedbackEnabled) {
        slideInVertically(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> fullHeight }
    } else {
        fadeIn(
            initialAlpha = DRAWER_ROOT_RETAIN_ALPHA,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = mainTabDetailContentOffsetEasing()
            )
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.transparentDetailExitTransition(
    coherentFeedbackEnabled: Boolean = true
): ExitTransition {
    if (
        shouldUseInstantBiliUploaderPlaylistTransition(
            initialRoute = initialState.destination.route,
            targetRoute = targetState.destination.route
        )
    ) {
        return ExitTransition.None
    }
    val handoff = resolveMainTabDetailHandoff(
        initialRoute = initialState.destination.route,
        targetRoute = targetState.destination.route
    )
    return if (!coherentFeedbackEnabled) {
        ExitTransition.KeepUntilTransitionsFinished
    } else if (handoff == MainTabDetailHandoff.RETURN_TO_TAB) {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_CLOSE_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> fullHeight }
    } else {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_OPEN_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> -fullHeight }
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.transparentDetailPopEnterTransition(
    coherentFeedbackEnabled: Boolean = true
): EnterTransition {
    if (
        shouldUseInstantBiliUploaderPlaylistTransition(
            initialRoute = initialState.destination.route,
            targetRoute = targetState.destination.route
        )
    ) {
        return EnterTransition.None
    }
    return if (coherentFeedbackEnabled) {
        slideInVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_CLOSE_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> -fullHeight }
    } else {
        fadeIn(
            initialAlpha = DRAWER_ROOT_RETAIN_ALPHA,
            animationSpec = tween(
                durationMillis = DRAWER_DETAIL_CLOSE_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.transparentDetailPopExitTransition(
    coherentFeedbackEnabled: Boolean = true
): ExitTransition {
    if (
        shouldUseInstantBiliUploaderPlaylistTransition(
            initialRoute = initialState.destination.route,
            targetRoute = targetState.destination.route
        )
    ) {
        return ExitTransition.None
    }
    return if (coherentFeedbackEnabled) {
        slideOutVertically(
            animationSpec = tween(
                durationMillis = MAIN_TAB_DETAIL_CLOSE_DURATION_MS,
                easing = mainTabDetailContentOffsetEasing()
            )
        ) { fullHeight -> fullHeight }
    } else {
        ExitTransition.KeepUntilTransitionsFinished
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.debugNavigationEnterTransition(
    coherentFeedbackEnabled: Boolean = true
): EnterTransition {
    val direction = resolveDebugNavigationTransitionDirection(
        initialRoute = initialState.destination.route,
        targetRoute = targetState.destination.route
    ) ?: return EnterTransition.None
    return if (coherentFeedbackEnabled) {
        debugNavigationEnterTransition(direction)
    } else {
        fadeIn(
            initialAlpha = DRAWER_ROOT_RETAIN_ALPHA,
            animationSpec = tween(
                durationMillis = if (direction > 0) {
                    DRAWER_DETAIL_OPEN_DURATION_MS
                } else {
                    DRAWER_DETAIL_CLOSE_DURATION_MS
                },
                easing = mainTabDetailContentOffsetEasing()
            )
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.debugNavigationExitTransition(
    coherentFeedbackEnabled: Boolean = true
): ExitTransition {
    val direction = resolveDebugNavigationTransitionDirection(
        initialRoute = initialState.destination.route,
        targetRoute = targetState.destination.route
    ) ?: return ExitTransition.None
    return if (coherentFeedbackEnabled) {
        debugNavigationExitTransition(direction)
    } else {
        ExitTransition.KeepUntilTransitionsFinished
    }
}

private fun debugNavigationEnterTransition(direction: Int): EnterTransition {
    return slideInVertically(
        animationSpec = tween(debugNavigationDurationMs(direction))
    ) { fullHeight -> direction * fullHeight }
}

private fun debugNavigationExitTransition(direction: Int): ExitTransition {
    return slideOutVertically(
        animationSpec = tween(debugNavigationDurationMs(direction))
    ) { fullHeight -> -direction * fullHeight }
}

private fun debugNavigationDurationMs(direction: Int): Int {
    return if (direction > 0) {
        DEBUG_NAVIGATION_OPEN_DURATION_MS
    } else {
        DEBUG_NAVIGATION_CLOSE_DURATION_MS
    }
}

internal fun resolveMainStartDestination(
    preferredRoute: String?,
    showHomeTab: Boolean,
    devModeEnabled: Boolean
): String? {
    return when (preferredRoute ?: return null) {
        Destinations.Home.route -> if (showHomeTab) Destinations.Home.route else Destinations.Explore.route
        Destinations.Explore.route -> Destinations.Explore.route
        Destinations.Library.route -> Destinations.Library.route
        Destinations.Settings.route -> Destinations.Settings.route
        Destinations.Debug.route -> if (devModeEnabled) Destinations.Debug.route else if (showHomeTab) Destinations.Home.route else Destinations.Explore.route
        else -> if (showHomeTab) Destinations.Home.route else Destinations.Explore.route
    }
}

private fun SongItem?.resolveUiCoverSource(context: Context): String? {
    return this?.displayCoverUrl(context)
}

private const val NOW_PLAYING_REMOTE_BLUR_IMAGE_SIZE_PX = 640
private const val NOW_PLAYING_LOCAL_BLUR_IMAGE_SIZE_PX = 384
private const val PLAYBACK_VISUAL_COVER_CLEAR_DELAY_MS = 900L
private const val NOW_PLAYING_BACKGROUND_CROSSFADE_MS = 520

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun resolvedNowPlayingBlurImageSizePx(coverUrl: String?): Int {
    return if (isRemoteImageSource(coverUrl)) {
        NOW_PLAYING_REMOTE_BLUR_IMAGE_SIZE_PX
    } else {
        NOW_PLAYING_LOCAL_BLUR_IMAGE_SIZE_PX
    }
}

private fun resolvedNowPlayingBlurStrength(coverUrl: String?, configuredBlurAmount: Float): Float {
    return if (isRemoteImageSource(coverUrl)) {
        configuredBlurAmount
    } else {
        configuredBlurAmount.coerceAtMost(64f)
    }
}

internal fun resolvePlaybackVisualCoverUrl(
    currentCoverUrl: String?,
    previousVisualCoverUrl: String?,
    hasCurrentSong: Boolean,
    clearDelayElapsed: Boolean,
    preservePreviousVisualCover: Boolean = true
): String? {
    val normalizedCoverUrl = currentCoverUrl?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        normalizedCoverUrl != null -> normalizedCoverUrl
        !hasCurrentSong || clearDelayElapsed -> null
        !preservePreviousVisualCover -> null
        else -> previousVisualCoverUrl
    }
}

@Composable
private fun rememberPlaybackVisualCoverUrl(
    coverUrl: String?,
    currentSongKey: String?
): String? {
    var visualCoverUrl by remember {
        mutableStateOf(
            resolvePlaybackVisualCoverUrl(
                currentCoverUrl = coverUrl,
                previousVisualCoverUrl = null,
                hasCurrentSong = currentSongKey != null,
                clearDelayElapsed = false
            )
        )
    }
    var lastObservedSongKey by remember { mutableStateOf(currentSongKey) }
    val songChangedSinceLastObservation = currentSongKey != lastObservedSongKey

    LaunchedEffect(coverUrl, currentSongKey) {
        val preservePreviousVisualCover = currentSongKey == lastObservedSongKey
        visualCoverUrl = resolvePlaybackVisualCoverUrl(
            currentCoverUrl = coverUrl,
            previousVisualCoverUrl = visualCoverUrl,
            hasCurrentSong = currentSongKey != null,
            clearDelayElapsed = false,
            preservePreviousVisualCover = preservePreviousVisualCover
        )
        lastObservedSongKey = currentSongKey

        if (coverUrl.isNullOrBlank() && currentSongKey != null && visualCoverUrl != null) {
            delay(PLAYBACK_VISUAL_COVER_CLEAR_DELAY_MS)
            visualCoverUrl = resolvePlaybackVisualCoverUrl(
                currentCoverUrl = coverUrl,
                previousVisualCoverUrl = visualCoverUrl,
                hasCurrentSong = true,
                clearDelayElapsed = true
            )
        }
    }

    val normalizedCoverUrl = coverUrl?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        normalizedCoverUrl != null -> normalizedCoverUrl
        currentSongKey == null -> null
        songChangedSinceLastObservation -> null
        else -> visualCoverUrl
    }
}

@Composable
private fun HigherQualityLocalDownloadDialog(
    request: GlobalDownloadManager.HigherQualityLocalDownloadRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_higher_quality_local_title)) },
        text = {
            Text(
                stringResource(
                    R.string.download_higher_quality_local_message,
                    request.localLabel,
                    request.localBitrateKbps
                )
            )
        },
        // 右下角：左「坚持下载」、右「明白」。
        dismissButton = {
            HapticTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.download_higher_quality_local_continue))
            }
        },
        confirmButton = {
            HapticTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.download_higher_quality_local_got_it))
            }
        }
    )
}

@Composable
private fun TrafficRiskDownloadDialog(
    request: GlobalDownloadManager.TrafficRiskDownloadRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val networkLabel = stringResource(
        when (request.networkType) {
            TrafficNetworkType.ROAMING -> R.string.traffic_risk_network_roaming
            TrafficNetworkType.MOBILE -> R.string.traffic_risk_network_mobile
            TrafficNetworkType.WIFI -> R.string.traffic_risk_network_wifi
        }
    )
    val message = if (request.songCount <= 1) {
        stringResource(
            R.string.traffic_risk_download_single_message,
            networkLabel,
            request.songs.firstOrNull()?.displayName().orEmpty()
        )
    } else {
        pluralStringResource(
            R.plurals.traffic_risk_download_batch_message,
            request.songCount,
            networkLabel,
            request.songCount
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.traffic_risk_download_title)) },
        text = { Text(message) },
        confirmButton = {
            HapticTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.traffic_risk_download_confirm))
            }
        },
        dismissButton = {
            HapticTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
internal fun MobileDataDownloadInterruptionDialog(
    request: GlobalDownloadManager.MobileDataDownloadInterruptionRequest,
    onContinue: () -> Unit,
    onWaitWifi: () -> Unit,
    onCancelAll: () -> Unit
) {
    val networkLabel = stringResource(
        when (request.networkType) {
            TrafficNetworkType.ROAMING -> R.string.traffic_risk_network_roaming
            TrafficNetworkType.MOBILE -> R.string.traffic_risk_network_mobile
            TrafficNetworkType.WIFI -> R.string.traffic_risk_network_wifi
        }
    )

    AlertDialog(
        onDismissRequest = onWaitWifi,
        title = { Text(stringResource(R.string.mobile_data_download_interruption_title)) },
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    pluralStringResource(
                        R.plurals.mobile_data_download_interruption_message,
                        request.taskCount,
                        networkLabel,
                        request.taskCount
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    HapticTextButton(onClick = onWaitWifi) {
                        Text(stringResource(R.string.mobile_data_download_wait_wifi))
                    }
                    HapticTextButton(onClick = onContinue) {
                        Text(stringResource(R.string.traffic_risk_download_confirm))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End)
                ) {
                    HapticTextButton(onClick = onCancelAll) {
                        Text(
                            stringResource(R.string.mobile_data_download_cancel_all),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun UsbExclusiveBackgroundPermissionDialog(
    batteryOptimizationAllowed: Boolean,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onNeverShowAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_usb_exclusive_background_permission_title))
        },
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.settings_usb_exclusive_background_permission_desc))
                if (!batteryOptimizationAllowed) {
                    HapticTextButton(
                        onClick = onRequestBatteryOptimization,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_usb_exclusive_background_permission_battery))
                    }
                }
                HapticTextButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_usb_exclusive_background_permission_app_settings))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    HapticTextButton(onClick = onNeverShowAgain) {
                        Text(stringResource(R.string.settings_usb_exclusive_background_permission_never))
                    }
                    HapticTextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.settings_usb_exclusive_background_permission_later))
                    }
                }
            }
        }
    )
}

private const val THEME_REVEAL_SNAPSHOT_MAX_DIMENSION_PX = 1080
private const val THEME_REVEAL_STABLE_DRAW_PASSES = 1
private const val THEME_REVEAL_DURATION_MILLIS = 720
private const val THEME_REVEAL_WATCHDOG_DELAY_MILLIS = 900L
private val THEME_REVEAL_SNAPSHOT_CONFIG = Bitmap.Config.RGB_565

internal data class ThemeRevealSnapshotDimensions(
    val width: Int,
    val height: Int
)

internal fun resolveThemeRevealSnapshotDimensions(
    width: Int,
    height: Int,
    maxDimensionPx: Int = THEME_REVEAL_SNAPSHOT_MAX_DIMENSION_PX
): ThemeRevealSnapshotDimensions {
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val maxDimension = maxOf(safeWidth, safeHeight)
    val downsampleRatio = (maxDimension.toFloat() / maxDimensionPx)
        .coerceAtLeast(1f)
    return ThemeRevealSnapshotDimensions(
        width = (safeWidth / downsampleRatio).roundToInt().coerceAtLeast(1),
        height = (safeHeight / downsampleRatio).roundToInt().coerceAtLeast(1)
    )
}

internal fun shouldBlockThemeModeChange(
    captureInFlight: Boolean,
    writeInFlight: Boolean,
    revealActive: Boolean,
    hasPendingThemePreference: Boolean
): Boolean = captureInFlight || writeInFlight || revealActive || hasPendingThemePreference

internal fun resolveThemeToggleTarget(isDark: Boolean): ThemeMode =
    if (isDark) ThemeMode.LIGHT else ThemeMode.DARK

internal fun localPlaylistIdFromSourceRoute(sourceRoute: String?): Long? {
    return sourceRoute
        ?.takeIf { it.startsWith("local_playlist_detail/") }
        ?.removePrefix("local_playlist_detail/")
        ?.toLongOrNull()
}

private data class HomeUsageSnapshot(
    val entries: List<UsageEntry> = emptyList(),
    val isLoaded: Boolean = false
)

private fun View.drawScaledThemeRevealBitmap(): Bitmap? {
    if (width <= 0 || height <= 0) {
        return null
    }
    val snapshotDimensions = resolveThemeRevealSnapshotDimensions(
        width = width,
        height = height
    )
    return runCatching {
        createBitmap(
            snapshotDimensions.width,
            snapshotDimensions.height,
            THEME_REVEAL_SNAPSHOT_CONFIG
        ).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.scale(
                snapshotDimensions.width.toFloat() / width.toFloat(),
                snapshotDimensions.height.toFloat() / height.toFloat()
            )
            draw(canvas)
        }
    }.getOrNull()
}

private suspend fun captureThemeRevealSnapshot(
    activity: Activity?,
    fallbackView: View
): ImageBitmap? {
    val windowBitmap = activity?.let { currentActivity ->
        suspendCancellableCoroutine { continuation ->
            val decorView = currentActivity.window.decorView
            if (decorView.width <= 0 || decorView.height <= 0) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val snapshotDimensions = resolveThemeRevealSnapshotDimensions(
                width = decorView.width,
                height = decorView.height
            )
            val bitmap = createBitmap(
                snapshotDimensions.width,
                snapshotDimensions.height,
                THEME_REVEAL_SNAPSHOT_CONFIG
            )

            PixelCopy.request(
                currentActivity.window,
                bitmap,
                { result ->
                    continuation.resume(if (result == PixelCopy.SUCCESS) bitmap else null)
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    return windowBitmap?.asImageBitmap() ?: captureThemeRevealFallbackSnapshot(fallbackView)
}

private suspend fun captureThemeRevealFallbackSnapshot(view: View): ImageBitmap? {
    return withContext(Dispatchers.Main.immediate) {
        runCatching {
            if (view.width > 0 && view.height > 0) {
                view.drawScaledThemeRevealBitmap()?.asImageBitmap()
            } else {
                null
            }
        }.getOrNull()
    }
}

private suspend fun awaitNextDraw(view: View) {
    if (!view.isAttachedToWindow || view.width <= 0 || view.height <= 0) {
        return
    }

    withTimeoutOrNull(120L) {
        suspendCancellableCoroutine { continuation ->
            val observer = view.viewTreeObserver
            var handled = false
            val drawListener = object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    if (handled) return
                    handled = true
                    view.post {
                        if (observer.isAlive) {
                            observer.removeOnDrawListener(this)
                        }
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }

            observer.addOnDrawListener(drawListener)
            continuation.invokeOnCancellation {
                if (handled) {
                    return@invokeOnCancellation
                }
                handled = true
                view.post {
                    if (observer.isAlive) {
                        observer.removeOnDrawListener(drawListener)
                    }
                }
            }
            view.invalidate()
        }
    }
}

private suspend fun awaitStableDraw(view: View) {
    repeat(THEME_REVEAL_STABLE_DRAW_PASSES) {
        awaitNextDraw(view)
    }
}

/**
 * 根据封面提取播放界面强调色
 */
@Composable
private fun NowPlayingAccentBackdrop(
    coverUrl: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    refreshKey: Int = 0,
    offlineMode: Boolean = false,
    onAccentChanged: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val fallback = if (isDark) Color(0xFF121212) else Color(0xFFF5F5F5)
    var target by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(coverUrl, isDark, refreshKey, offlineMode) {
        if (coverUrl.isNullOrEmpty()) {
            target = null
            onAccentChanged(null)
            return@LaunchedEffect
        }
        val cached = CoverArtColorCache.peek(coverUrl)
        if (cached != null) {
            target = Color(adjustedAccentColorArgb(cached.baseColorArgb, isDark))
            onAccentChanged(cached.seedHex)
        }
        val sample = CoverArtColorCache.getOrLoad(context, coverUrl, offlineMode)
        if (sample != null) {
            target = Color(adjustedAccentColorArgb(sample.baseColorArgb, isDark))
            onAccentChanged(sample.seedHex)
        } else if (cached == null) {
            target = null
            onAccentChanged(null)
        }
    }

    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = target ?: fallback,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "accent-bg"
    )

    val vignetteAlpha by animateFloatAsState(
        targetValue = if (isDark) 0.12f else 0.25f, // 暗色更强一点，亮色很轻
        animationSpec = tween(300),
        label = "vignette-alpha"
    )

    val whiteMaskAlpha by animateFloatAsState(
        targetValue = if (isDark) 0f else 0.05f,
        animationSpec = tween(300),
        label = "white-mask-alpha"
    )

    Box(
        modifier = modifier
            .background(bgColor)
            .drawWithContent {
                drawContent()
                // 顶部黑色渐隐
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = vignetteAlpha),
                            Color.Transparent
                        )
                    )
                )
                // 亮色模式白色遮罩, 整体柔化
                if (whiteMaskAlpha > 0f) {
                    drawRect(Color.White.copy(alpha = whiteMaskAlpha))
                }
            }
    )
}

@Composable
private fun OfflineModeBottomBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.offline_mode_bottom_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun LyraApp(
    initialThemeSnapshot: ThemePreferenceSnapshot = ThemePreferenceSnapshot(),
    launcherShortcutRequestFlow: StateFlow<LauncherShortcutRequest?> =
        EmptyLauncherShortcutRequestFlow,
    onLauncherShortcutRequestConsumed: (LauncherShortcutRequest) -> Unit = {},
    onIsDarkChanged: (Boolean) -> Unit = {},
    onNowPlayingVisibilityChanged: (Boolean) -> Unit = {},
    onLanguageChanged: (LanguageManager.Language) -> Unit = {}
) {
    var appContentReady by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 先交一个极轻的背景首帧, 下一帧再挂整棵导航和状态订阅树
        withFrameNanos { }
        appContentReady = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedVisibility(
            visible = appContentReady,
            enter = fadeIn(
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ),
            exit = ExitTransition.None,
            modifier = Modifier.fillMaxSize()
        ) {
            LyraAppContent(
                initialThemeSnapshot = initialThemeSnapshot,
                launcherShortcutRequestFlow = launcherShortcutRequestFlow,
                onLauncherShortcutRequestConsumed = onLauncherShortcutRequestConsumed,
                onIsDarkChanged = onIsDarkChanged,
                onNowPlayingVisibilityChanged = onNowPlayingVisibilityChanged,
                onLanguageChanged = onLanguageChanged
            )
        }
    }
}

@Composable
private fun StartupGlassGateOverlay(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isDark) {
        Color(0xFF101010)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val scrimColor = if (isDark) {
        Color.Black.copy(alpha = 0.20f)
    } else {
        MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
    }
    Box(
        modifier = modifier
            .background(baseColor)
            .background(scrimColor)
            .blockUnderlyingTouches()
    )
}

@Composable
private fun LyraAppContent(
    initialThemeSnapshot: ThemePreferenceSnapshot = ThemePreferenceSnapshot(),
    launcherShortcutRequestFlow: StateFlow<LauncherShortcutRequest?> =
        EmptyLauncherShortcutRequestFlow,
    onLauncherShortcutRequestConsumed: (LauncherShortcutRequest) -> Unit = {},
    onIsDarkChanged: (Boolean) -> Unit = {},
    onNowPlayingVisibilityChanged: (Boolean) -> Unit = {},
    onLanguageChanged: (LanguageManager.Language) -> Unit = {}
) {
    val context = LocalContext.current
    val composeResources = LocalResources.current
    val latestOnNowPlayingVisibilityChanged by rememberUpdatedState(
        onNowPlayingVisibilityChanged
    )
    val latestOnLauncherShortcutRequestConsumed by rememberUpdatedState(
        onLauncherShortcutRequestConsumed
    )
    val launcherShortcutRequest by launcherShortcutRequestFlow.collectAsStateWithLifecycle()
    val offlineMode by rememberOfflineModeState()
    val rootView = LocalView.current
    val repo = remember { AppContainer.settingsRepo }
    val systemDark = rememberActualSystemDarkTheme()
    val application = remember(context) { context.applicationContext as Application }
    SideEffect {
        // 播放点击可能早于启动预加载完成, 先绑定上下文避免懒初始化缺入口
        PlayerManager.bindApplication(application)
    }
    val startupPlaybackPreferences = remember(application) {
        readPlaybackPreferenceSnapshotCached(application) ?: PlaybackPreferenceSnapshot()
    }
    val profileStore = remember(context) { UserProfileStore.getInstance(context) }
    val userProfile by profileStore.profileFlow.collectAsStateWithLifecycle()

    val storedFollowSystemDark by repo.followSystemDarkFlow.collectAsStateWithLifecycle(
        initialValue = initialThemeSnapshot.followSystemDark
    )
    val storedForceDark by repo.forceDarkFlow.collectAsStateWithLifecycle(
        initialValue = initialThemeSnapshot.forceDark
    )
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    // 播放页下拉收起期间：底栏与页面层提前归位，收起动画飞向迷你播放器。
    var nowPlayingDismissing by remember { mutableStateOf(false) }
    var miniPlayerCoverBounds by remember { mutableStateOf<Rect?>(null) }
    var showNowPlayingLyrics by rememberSaveable { mutableStateOf(false) }
    var currentPlaybackSourceRoute by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreLyricsAfterAlbumBack by rememberSaveable { mutableStateOf(false) }
    var lyricsAlbumRouteObserved by rememberSaveable { mutableStateOf(false) }
    val devModeEnabled by repo.devModeEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val alwaysRecordLogsEnabled by repo.alwaysRecordLogsEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val themeSeedColor by repo.themeSeedColorFlow.collectAsStateWithLifecycle(initialValue = ThemeDefaults.DEFAULT_SEED_COLOR_HEX)
    val themeColorPalette by repo.themeColorPaletteFlow.collectAsStateWithLifecycle(initialValue = ThemeDefaults.PRESET_COLORS)
    val lyricBlurEnabled by repo.lyricBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val lyricBlurAmount by repo.lyricBlurAmountFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val cloudMusicLyricDefaultOffsetMs by repo.cloudMusicLyricDefaultOffsetMsFlow
        .collectAsStateWithLifecycle(initialValue = startupPlaybackPreferences.cloudMusicLyricDefaultOffsetMs)
    val qqMusicLyricDefaultOffsetMs by repo.qqMusicLyricDefaultOffsetMsFlow
        .collectAsStateWithLifecycle(initialValue = startupPlaybackPreferences.qqMusicLyricDefaultOffsetMs)
    val floatingLyricsPreferences by repo.floatingLyricsPreferencesFlow.collectAsStateWithLifecycle(
        initialValue = FloatingLyricsPreferences()
    )
    val advancedLyricsEnabled by repo.advancedLyricsEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val coherentFeedbackEnabled by repo.coherentFeedbackEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val advancedBlurEnabled by repo.advancedBlurEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val enhancedAdvancedBlurEnabled by repo.enhancedAdvancedBlurEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val enhancedAdvancedBlurRadiusDp by repo.enhancedAdvancedBlurRadiusDpFlow
        .collectAsStateWithLifecycle(
            initialValue = DEFAULT_ENHANCED_ADVANCED_BLUR_RADIUS_DP
        )
    val initialAdvancedBlurQuality = remember {
        AdvancedBlurQualityPreference.defaultForDevice(isCurrentBuildDimensity())
    }
    val advancedBlurQuality by repo.advancedBlurQualityFlow.collectAsStateWithLifecycle(
        initialValue = initialAdvancedBlurQuality
    )
    val advancedBlurAvailable = isAdvancedGlassBackendSupported(Build.VERSION.SDK_INT)
    val effectiveAdvancedBlurEnabled = advancedBlurAvailable && advancedBlurEnabled
    val nowPlayingAudioReactiveEnabled by repo.nowPlayingAudioReactiveEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingDynamicBackgroundEnabled by repo.nowPlayingDynamicBackgroundEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingCoverBlurBackgroundEnabled by repo.nowPlayingCoverBlurBackgroundEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val nowPlayingCoverBlurAmount by repo.nowPlayingCoverBlurAmountFlow.collectAsStateWithLifecycle(initialValue = 1.5f)
    val nowPlayingCoverBlurDarken by repo.nowPlayingCoverBlurDarkenFlow.collectAsStateWithLifecycle(initialValue = 0.2f)
    val lyricFontScales by repo.lyricFontScalesFlow.collectAsStateWithLifecycle(
        initialValue = LyricFontScales(
            coverLyric = 1.0f,
            coverTranslation = 1.0f,
            lyricsPageLyric = 1.0f,
            lyricsPageTranslation = 1.0f
        )
    )
    val uiDensityScale by repo.uiDensityScaleFlow.collectAsStateWithLifecycle(initialValue = 1.0f)
    val bypassProxy by repo.bypassProxyFlow.collectAsStateWithLifecycle(initialValue = true)
    val backgroundImageUri by repo.backgroundImageUriFlow.collectAsStateWithLifecycle(initialValue = null)
    val downloadDirectoryUri by repo.downloadDirectoryUriFlow.collectAsStateWithLifecycle(initialValue = null)
    val downloadFileNameTemplate by repo.downloadFileNameTemplateFlow.collectAsStateWithLifecycle(initialValue = null)
    val backgroundImageBlur by repo.backgroundImageBlurFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val backgroundImageAlpha by repo.backgroundImageAlphaFlow.collectAsStateWithLifecycle(initialValue = 0.3f)
    val hapticFeedbackEnabled by repo.hapticFeedbackEnabledFlow.collectAsStateWithLifecycle(initialValue = true)
    val showCoverSourceBadge by repo.showCoverSourceBadgeFlow.collectAsStateWithLifecycle(initialValue = true)
    val nowPlayingKeepScreenOn by repo.nowPlayingKeepScreenOnFlow.collectAsStateWithLifecycle(initialValue = true)
    val showNowPlayingTitle by repo.nowPlayingShowTitleFlow.collectAsStateWithLifecycle(initialValue = true)
    val showLyricTranslation by repo.showLyricTranslationFlow.collectAsStateWithLifecycle(initialValue = true)
    val defaultStartDestination: String? by repo.defaultStartDestinationFlow
        .collectAsStateWithLifecycle(initialValue = null)
    val showHomeTrendingCard by repo.homeCardTrendingFlow.collectAsStateWithLifecycle(initialValue = true)
    val showHomeRadarCard by repo.homeCardRadarFlow.collectAsStateWithLifecycle(initialValue = true)
    val showHomeRecommendedCard by repo.homeCardRecommendedFlow.collectAsStateWithLifecycle(initialValue = true)
    val playbackFadeIn by repo.playbackFadeInFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.playbackFadeIn
    )
    val playbackCrossfadeNext by repo.playbackCrossfadeNextFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.playbackCrossfadeNext
    )
    val sleepTimerFinishCurrentOnExpiry by repo.sleepTimerFinishCurrentOnExpiryFlow
        .collectAsStateWithLifecycle(
            initialValue = startupPlaybackPreferences.sleepTimerFinishCurrentOnExpiry
        )
    val playbackFadeInDurationMs by repo.playbackFadeInDurationMsFlow.collectAsStateWithLifecycle(initialValue = 500L)
    val playbackFadeOutDurationMs by repo.playbackFadeOutDurationMsFlow.collectAsStateWithLifecycle(initialValue = 500L)
    val playbackCrossfadeInDurationMs by repo.playbackCrossfadeInDurationMsFlow.collectAsStateWithLifecycle(initialValue = 500L)
    val playbackCrossfadeOutDurationMs by repo.playbackCrossfadeOutDurationMsFlow.collectAsStateWithLifecycle(initialValue = 500L)
    val playbackVolumeNormalizationEnabled by repo.playbackVolumeNormalizationEnabledFlow
        .collectAsStateWithLifecycle(
            initialValue = startupPlaybackPreferences.playbackVolumeNormalizationEnabled
        )
    val playbackHighResolutionOutputEnabled by repo.playbackHighResolutionOutputEnabledFlow
        .collectAsStateWithLifecycle(
            initialValue = startupPlaybackPreferences.playbackHighResolutionOutputEnabled
        )
    val playbackVolumeBalance by repo.playbackVolumeBalanceFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.playbackVolumeBalance
    )
    val keepLastPlaybackProgress by repo.keepLastPlaybackProgressFlow.collectAsStateWithLifecycle(initialValue = true)
    val rememberLongFormPlaybackProgress by repo.rememberLongFormPlaybackProgressFlow
        .collectAsStateWithLifecycle(
            initialValue = startupPlaybackPreferences.rememberLongFormPlaybackProgress
        )
    val keepPlaybackModeState by repo.keepPlaybackModeStateFlow.collectAsStateWithLifecycle(initialValue = true)
    val neteaseAutoSourceSwitch by repo.neteaseAutoSourceSwitchFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.neteaseAutoSourceSwitch
    )
    val neteaseLocalSourceFallback by repo.neteaseLocalSourceFallbackFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.neteaseLocalSourceFallback
    )
    val stopOnBluetoothDisconnect by repo.stopOnBluetoothDisconnectFlow.collectAsStateWithLifecycle(initialValue = true)
    val usbExclusivePlayback by repo.usbExclusivePlaybackFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.usbExclusivePlayback
    )
    val usbExclusiveBackgroundPermissionPromptSuppressed by repo
        .usbExclusiveBackgroundPermissionPromptSuppressedFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val allowMixedPlayback by repo.allowMixedPlaybackFlow.collectAsStateWithLifecycle(initialValue = false)
    val preemptAudioFocus by repo.preemptAudioFocusFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.preemptAudioFocus
    )
    val maxCacheSizeBytes by repo.maxCacheSizeBytesFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.maxCacheSizeBytes
    )
    val homeUsageSnapshot by produceState(
        initialValue = HomeUsageSnapshot(),
        key1 = context
    ) {
        val usageFlow = withContext(Dispatchers.IO) {
            AppContainer.playlistUsageRepo.frequentPlaylistsFlow
        }
        usageFlow.collect { entries ->
            value = HomeUsageSnapshot(entries = entries, isLoaded = true)
        }
    }
    val showHomeTab =
        shouldShowHomeContinueSection(
            usageLoaded = homeUsageSnapshot.isLoaded,
            hasUsage = homeUsageSnapshot.entries.isNotEmpty()
        ) ||
            showHomeTrendingCard ||
            showHomeRadarCard ||
            showHomeRecommendedCard
    var pendingFollowSystemDark by remember { mutableStateOf<Boolean?>(null) }
    var pendingForceDark by remember { mutableStateOf<Boolean?>(null) }
    var themeRevealSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var themeRevealOriginWindow by remember { mutableStateOf<Offset?>(null) }
    var themeRevealStartRadiusPx by remember { mutableFloatStateOf(0f) }
    var themeRevealFallbackColorArgb by remember { mutableStateOf<Int?>(null) }
    var themeRevealCaptureInFlight by remember { mutableStateOf(false) }
    var themeRevealCaptureJob by remember { mutableStateOf<Job?>(null) }
    var themeRevealCaptureToken by remember { mutableIntStateOf(0) }
    var themeModeWriteInFlight by remember { mutableStateOf(false) }
    var pendingBackgroundImageAlpha by remember { mutableStateOf<Float?>(null) }
    var coverArtRefreshToken by remember { mutableIntStateOf(0) }
    var showUsbExclusiveBackgroundPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var usbExclusiveBackgroundPermissionPromptHandled by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val startupAudioFocusRefresher = remember(context) {
        PlayerStartupAudioFocusRefresher(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleResumed = when (event) {
                Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> false
                else -> lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        lifecycleResumed,
        usbExclusivePlayback,
        usbExclusiveBackgroundPermissionPromptSuppressed
    ) {
        if (!usbExclusivePlayback) {
            usbExclusiveBackgroundPermissionPromptHandled = false
            showUsbExclusiveBackgroundPermissionDialog = false
            return@LaunchedEffect
        }
        val shouldInspectBackgroundBehavior = lifecycleResumed &&
            !usbExclusiveBackgroundPermissionPromptSuppressed &&
            !usbExclusiveBackgroundPermissionPromptHandled
        val backgroundBehaviorAllowed = if (shouldInspectBackgroundBehavior) {
            context.readBackgroundBehaviorAllowance().fullyAllowed
        } else {
            true
        }
        if (
            shouldPromptForUsbExclusiveBackgroundPermission(
                usbExclusiveEnabled = usbExclusivePlayback,
                appResumed = lifecycleResumed,
                promptSuppressed = usbExclusiveBackgroundPermissionPromptSuppressed,
                backgroundBehaviorAllowed = backgroundBehaviorAllowed,
                promptHandledInCurrentSession = usbExclusiveBackgroundPermissionPromptHandled
            )
        ) {
            showUsbExclusiveBackgroundPermissionDialog = true
        }
        if (shouldInspectBackgroundBehavior) {
            usbExclusiveBackgroundPermissionPromptHandled = true
        }
    }

    val followSystemDark = pendingFollowSystemDark ?: storedFollowSystemDark
    val forceDark = pendingForceDark ?: storedForceDark
    val themeMode = remember(followSystemDark, forceDark) {
        ThemeMode.fromPreferenceFlags(
            forceDark = forceDark,
            followSystemDark = followSystemDark
        )
    }
    val effectiveBackgroundImageAlpha = pendingBackgroundImageAlpha ?: backgroundImageAlpha

    val clearThemeRevealVisualState = {
        themeRevealSnapshot = null
        themeRevealOriginWindow = null
        themeRevealStartRadiusPx = 0f
        themeRevealFallbackColorArgb = null
    }
    val clearPendingThemeModeChange = {
        pendingFollowSystemDark = null
        pendingForceDark = null
    }
    val clearThemeRevealState = {
        themeRevealCaptureToken += 1
        themeRevealCaptureJob?.cancel()
        themeRevealCaptureJob = null
        themeRevealCaptureInFlight = false
        themeModeWriteInFlight = false
        clearPendingThemeModeChange()
        clearThemeRevealVisualState()
    }
    val finishThemeReveal = { captureToken: Int ->
        if (themeRevealCaptureToken == captureToken) {
            clearThemeRevealVisualState()
        }
    }

    // 缓存当前封面的取色结果, 避免开关动态取色时先闪到默认种子色
    val currentSong by PlayerManager.currentSongFlow.collectAsStateWithLifecycle()
    val displayCoverUrl = rememberSongDisplayCoverUrl(currentSong)
    val currentSongKey = remember(currentSong) { currentSong?.stableKey() }
    val playbackVisualCoverUrl = rememberPlaybackVisualCoverUrl(
        coverUrl = displayCoverUrl,
        currentSongKey = currentSongKey
    )
    val scope = rememberCoroutineScope()
    var pendingTrafficRiskDownloadRequest by remember {
        mutableStateOf<GlobalDownloadManager.TrafficRiskDownloadRequest?>(null)
    }
    LaunchedEffect(Unit) {
        GlobalDownloadManager.trafficRiskDownloadRequests.collect { request ->
            pendingTrafficRiskDownloadRequest = request
        }
    }
    var pendingHigherQualityDownloadRequest by remember {
        mutableStateOf<GlobalDownloadManager.HigherQualityLocalDownloadRequest?>(null)
    }
    LaunchedEffect(Unit) {
        GlobalDownloadManager.higherQualityLocalDownloadRequests.collect { request ->
            pendingHigherQualityDownloadRequest = request
        }
    }

    val serviceSyncCoordinator = remember(context) {
        PlayerStartupServiceSyncCoordinator(
            awaitUiFrame = { withFrameNanos { } },
            isServiceReadyForPassiveLocalPlaybackSync = AudioPlayerService::isReadyForPassiveLocalPlaybackSync,
            hasItems = PlayerManager::hasItems,
            hasLocalCurrentSong = {
                PlayerManager.currentSongFlow.value?.let { song ->
                    LocalSongSupport.isLocalSong(song, context)
                } == true
            },
            isUsbExclusivePlaybackActiveForForegroundService =
                PlayerManager::isUsbExclusivePlaybackActiveForForegroundService,
            shouldRunPlaybackServiceInForeground = PlayerManager::shouldRunPlaybackServiceInForeground,
            isServiceInstanceActiveForDiagnostics = AudioPlayerService::isInstanceActiveForDiagnostics,
            isServiceForegroundActiveForDiagnostics = AudioPlayerService::isForegroundActiveForDiagnostics,
            startService = { source, forceForeground ->
                AudioPlayerService.startSyncService(
                    context,
                    source,
                    forceForeground = forceForeground
                )
            },
            playbackCommandFlow = PlayerManager.playbackCommandFlow
        )
    }
    val scheduleAudioServiceStart: (String, Boolean) -> Unit = { source, forceForeground ->
        scope.launch {
            serviceSyncCoordinator.startServiceAfterUiFrame(
                source = source,
                forceForeground = forceForeground
            )
        }
    }
    var playbackBootstrapReady by remember { mutableStateOf(false) }

    fun updateStartupAudioFocus(reason: String) {
        startupAudioFocusRefresher.refreshForeground(
            lifecycleResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
            reason = reason,
            preemptAudioFocus = preemptAudioFocus,
            allowMixedPlayback = allowMixedPlayback,
            usbExclusivePlayback = usbExclusivePlayback
        )
    }

    LaunchedEffect(application) {
        playbackBootstrapReady = false
        PlayerStartupBootstrapper(
            app = application,
            context = context,
            awaitUiFrameBeforePlayerInit = {
                withFrameNanos { }
            }
        ).bootstrap().serviceStart?.let { serviceStart ->
            scheduleAudioServiceStart(serviceStart.source, serviceStart.forceForeground)
        }
        playbackBootstrapReady = true

        launch {
            serviceSyncCoordinator.collectLocalPlaybackCommands()
        }

        val historyRecorder = PlayerStartupHistoryRecorder(
            currentSongFlow = PlayerManager.currentSongFlow,
            recordSong = AppContainer.playHistoryRepo::record,
            startupSongToSkip = PlayerManager.currentSongFlow.value
        )
        launch {
            historyRecorder.run()
        }

    }

    LaunchedEffect(preemptAudioFocus, allowMixedPlayback, usbExclusivePlayback) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            updateStartupAudioFocus("settings_changed")
        } else {
            startupAudioFocusRefresher.releaseForInactiveSettingsChange(
                preemptAudioFocus = preemptAudioFocus,
                usbExclusivePlayback = usbExclusivePlayback,
                allowMixedPlayback = allowMixedPlayback
            )
        }
    }

    LaunchedEffect(storedFollowSystemDark, pendingFollowSystemDark) {
        if (pendingFollowSystemDark != null && pendingFollowSystemDark == storedFollowSystemDark) {
            pendingFollowSystemDark = null
        }
    }
    LaunchedEffect(backgroundImageAlpha, pendingBackgroundImageAlpha) {
        if (
            pendingBackgroundImageAlpha != null &&
            abs((pendingBackgroundImageAlpha ?: backgroundImageAlpha) - backgroundImageAlpha) < 0.001f
        ) {
            pendingBackgroundImageAlpha = null
        }
    }

    LaunchedEffect(storedForceDark, pendingForceDark) {
        if (pendingForceDark != null && pendingForceDark == storedForceDark) {
            pendingForceDark = null
        }
    }

    // 同步触感反馈设置
    LaunchedEffect(hapticFeedbackEnabled) {
        syncHapticFeedbackSetting(hapticFeedbackEnabled)
    }

    val isDark = StartupThemeResolver.resolveModeUseDark(
        mode = themeMode,
        systemDark = systemDark
    )
    val initialMainStartDestination = resolveMainStartDestination(
        preferredRoute = defaultStartDestination,
        showHomeTab = showHomeTab,
        devModeEnabled = devModeEnabled
    ) ?: run {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF101010) else Color(0xFFF4EFE7))
        )
        return
    }
    val currentDefaultStartDestination =
        defaultStartDestination ?: initialMainStartDestination
    val backgroundGlassBackdrop = rememberAdvancedGlassBackdrop()
    val contentGlassBackdrop = rememberAdvancedGlassBackdrop()
    val meloxBackdrop = rememberLayerBackdrop()
    val advancedGlassController = remember(
        advancedBlurEnabled,
        enhancedAdvancedBlurEnabled,
        enhancedAdvancedBlurRadiusDp,
        advancedBlurQuality
    ) {
        AdvancedGlassController(
            sdkInt = Build.VERSION.SDK_INT,
            advancedBlurEnabled = advancedBlurEnabled,
            enhancedAdvancedBlurEnabled = enhancedAdvancedBlurEnabled,
            backendReady = isAdvancedGlassBackendSupported(Build.VERSION.SDK_INT),
            enhancedAdvancedBlurRadiusDp = enhancedAdvancedBlurRadiusDp,
            advancedBlurQuality = advancedBlurQuality
        )
    }
    var startupGlassGateReleased by rememberSaveable {
        mutableStateOf(!advancedGlassController.isBaseBlurEnabled)
    }
    val startupBackgroundGlassReady = backgroundGlassBackdrop.hasActiveBlur
    val startupContentGlassReady = contentGlassBackdrop.hasActiveBlur
    LaunchedEffect(
        advancedGlassController.isBaseBlurEnabled,
        startupBackgroundGlassReady,
        startupContentGlassReady
    ) {
        if (
            shouldReleaseStartupGlassGate(
                baseBlurEnabled = advancedGlassController.isBaseBlurEnabled,
                backgroundEffectReady = startupBackgroundGlassReady,
                contentEffectReady = startupContentGlassReady
            )
        ) {
            startupGlassGateReleased = true
        }
    }
    // Some GPUs never report a ready blur effect; never keep the shell covered
    // behind the startup gate after the first frames.
    LaunchedEffect(advancedGlassController.isBaseBlurEnabled, startupGlassGateReleased) {
        if (!advancedGlassController.isBaseBlurEnabled || startupGlassGateReleased) return@LaunchedEffect
        delay(1200)
        startupGlassGateReleased = true
    }
    val preferredQuality by repo.audioQualityFlow.collectAsStateWithLifecycle(initialValue = "exhigh")
    val youtubePreferredQuality by repo.youtubeAudioQualityFlow.collectAsStateWithLifecycle(initialValue = "high")
    val biliPreferredQuality by repo.biliAudioQualityFlow.collectAsStateWithLifecycle(initialValue = "high")
    val mobileDataFollowDefaultAudioQuality by repo.mobileDataFollowDefaultAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataFollowDefaultAudioQuality
    )
    val mobileDataNeteaseAudioQuality by repo.mobileDataNeteaseAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataNeteaseAudioQuality
    )
    val mobileDataYouTubeAudioQuality by repo.mobileDataYouTubeAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataYouTubeAudioQuality
    )
    val mobileDataBiliAudioQuality by repo.mobileDataBiliAudioQualityFlow.collectAsStateWithLifecycle(
        initialValue = startupPlaybackPreferences.mobileDataBiliAudioQuality
    )
    val currentThemeBackgroundArgb = MaterialTheme.colorScheme.background.toArgb()
    // retained main-tab scenes can keep an earlier callback, so read the current theme state at click time
    val latestThemeMode by rememberUpdatedState(themeMode)
    val latestIsDark by rememberUpdatedState(isDark)
    val latestSystemDark by rememberUpdatedState(systemDark)
    val latestThemeBackgroundArgb by rememberUpdatedState(currentThemeBackgroundArgb)
    val themeRevealActive =
        themeRevealOriginWindow != null &&
            themeRevealFallbackColorArgb != null
    val latestThemeRevealActive by rememberUpdatedState(themeRevealActive)

    LaunchedEffect(isDark, themeRevealActive, themeRevealCaptureInFlight) {
        if (!themeRevealActive && !themeRevealCaptureInFlight) {
            onIsDarkChanged(isDark)
        }
    }

    val activeThemeRevealToken = themeRevealCaptureToken
    LaunchedEffect(themeRevealActive, activeThemeRevealToken) {
        if (!themeRevealActive) {
            return@LaunchedEffect
        }
        delay(THEME_REVEAL_WATCHDOG_DELAY_MILLIS)
        finishThemeReveal(activeThemeRevealToken)
    }

    fun requestThemeModeChange(
        targetMode: ThemeMode,
        originInWindow: Offset,
        startRadiusPx: Float
    ) {
        if (
            shouldBlockThemeModeChange(
                captureInFlight = themeRevealCaptureInFlight,
                writeInFlight = themeModeWriteInFlight,
                revealActive = latestThemeRevealActive,
                hasPendingThemePreference = pendingFollowSystemDark != null ||
                    pendingForceDark != null
            )
        ) {
            return
        }

        if (targetMode == latestThemeMode) {
            return
        }

        val nextFollowSystemDark = targetMode.followSystemDark
        val nextForceDark = targetMode.forceDark
        val nextDark = targetMode.resolveUseDark(latestSystemDark)
        if (nextDark == latestIsDark) {
            themeModeWriteInFlight = true
            scope.launch {
                try {
                    repo.setThemeMode(
                        followSystemDark = nextFollowSystemDark,
                        forceDark = nextForceDark
                    )
                } finally {
                    themeModeWriteInFlight = false
                }
            }
            return
        }

        val activity = context as? Activity
        val captureView = activity?.window?.decorView?.rootView ?: rootView.rootView
        val captureToken = themeRevealCaptureToken + 1
        themeRevealCaptureToken = captureToken
        themeRevealCaptureInFlight = true

        val captureJob = scope.launch {
            var themeWriteStarted = false
            var themeWriteCompleted = false
            try {
                awaitStableDraw(captureView)
                val snapshot = runCatching {
                    captureThemeRevealSnapshot(
                        activity = activity,
                        fallbackView = captureView
                    )
                }.getOrNull()
                val lifecycleActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                val activityValid = activity == null || (!activity.isFinishing && !activity.isDestroyed)
                if (themeRevealCaptureToken != captureToken || !lifecycleActive || !activityValid) {
                    return@launch
                }

                clearThemeRevealVisualState()
                themeRevealSnapshot = snapshot
                themeRevealFallbackColorArgb = latestThemeBackgroundArgb
                themeRevealOriginWindow = originInWindow
                themeRevealStartRadiusPx = startRadiusPx.coerceAtLeast(1f)
                pendingFollowSystemDark = nextFollowSystemDark
                pendingForceDark = nextForceDark
                themeModeWriteInFlight = true
                themeWriteStarted = true
                repo.setThemeMode(
                    followSystemDark = nextFollowSystemDark,
                    forceDark = nextForceDark
                )
                themeWriteCompleted = true
            } finally {
                if (themeRevealCaptureToken == captureToken) {
                    if (themeWriteStarted && !themeWriteCompleted) {
                        clearPendingThemeModeChange()
                        clearThemeRevealVisualState()
                    }
                    themeRevealCaptureJob = null
                    themeRevealCaptureInFlight = false
                    themeModeWriteInFlight = false
                }
            }
        }
        themeRevealCaptureJob = captureJob
    }

    fun requestThemeToggle(originInWindow: Offset, startRadiusPx: Float) {
        val targetMode = resolveThemeToggleTarget(latestIsDark)
        requestThemeModeChange(
            targetMode = targetMode,
            originInWindow = originInWindow,
            startRadiusPx = startRadiusPx
        )
    }

    DisposableEffect(lifecycleOwner, preemptAudioFocus, allowMixedPlayback, usbExclusivePlayback) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    PlayerManager.updateUsbExclusiveForegroundState(
                        foreground = true,
                        reason = "lifecycle_resume"
                    )
                    coverArtRefreshToken += 1
                    if (!PlayerManager.isUsbExclusiveNativePlaybackStable()) {
                        updateStartupAudioFocus("lifecycle_resume")
                    }
                    PlayerManager.recoverUsbExclusivePlaybackOnForeground("lifecycle_resume")
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    PlayerManager.updateUsbExclusiveForegroundState(
                        foreground = false,
                        reason = "lifecycle_${event.name.lowercase()}"
                    )
                    clearThemeRevealState()
                    val keepUsbExclusiveFocus = PlayerManager.isPlayerInitialized() &&
                        PlayerManager.usbExclusivePlaybackEnabled &&
                        PlayerManager.shouldUseUsbExclusiveFocusGuard()
                    if (keepUsbExclusiveFocus) {
                        updateStartupAudioFocus("lifecycle_${event.name.lowercase()}_keep_usb")
                    } else {
                        startupAudioFocusRefresher.release("lifecycle_${event.name.lowercase()}")
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    clearThemeRevealState()
                    startupAudioFocusRefresher.release("lifecycle_${event.name.lowercase()}")
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(rootView, backgroundImageUri) {
        clearThemeRevealState()
    }

    fun playSongsAndOpenNowPlaying(
        songs: List<SongItem>,
        index: Int,
        sourceRoute: String? = null
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        PlayerManager.prefetchYouTubePlayableUrlWindow(
            playlist = songs,
            startIndex = index,
            source = "ui_click_before_play"
        )
        // 播放队列可能包含歌词等大字段, 避免通过 Binder 传整份歌单导致崩溃
        val localPlaylistId = localPlaylistIdFromSourceRoute(sourceRoute)
        if (localPlaylistId == null) {
            PlayerManager.playPlaylist(songs, index)
        } else {
            PlayerManager.playLocalPlaylist(
                playlistId = localPlaylistId,
                songs = songs,
                startIndex = index
            )
        }
        scheduleAudioServiceStart(
            "play_songs_and_open_now_playing",
            true
        )
    }

    fun playSongPreservingQueueAndOpenNowPlaying(song: SongItem) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = null
        PlayerManager.prefetchYouTubePlayableUrlWindow(
            playlist = listOf(song),
            startIndex = 0,
            source = "ui_click_preserve_queue_before_play"
        )
        PlayerManager.replaceCurrentInQueueAndPlay(song)
        scheduleAudioServiceStart(
            "play_search_result_preserve_queue",
            true
        )
    }

    fun addSongToQueueNextFromSearch(song: SongItem) {
        PlayerManager.addToQueueNext(song)
        scheduleAudioServiceStart("search_result_play_next", false)
    }

    fun addSongToQueueEndFromSearch(song: SongItem) {
        PlayerManager.addToQueueEnd(song)
        scheduleAudioServiceStart("search_result_add_to_queue_end", false)
    }

    fun ensureAudioServiceStarted(source: String = "ensure_audio_service_started") {
        LyraLogger.d(
            "LYRA-App",
            "ensureAudioServiceStarted hasItems=${PlayerManager.hasItems()} transportActive=${PlayerManager.isTransportActive()} isPlaying=${PlayerManager.isPlayingFlow.value}"
        )
        scheduleAudioServiceStart(source, false)
    }

    fun playBiliAudioAndOpenNowPlayingWithSource(
        videos: List<BiliVideoItem>,
        index: Int,
        sourceRoute: String?
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        LyraLogger.d("LYRA-App", "Playing audio from Bili video: ${videos[index].title}")
        PlayerManager.playBiliVideoAsAudio(videos, index)
        showNowPlaying = true
        ensureAudioServiceStarted(source = "play_bili_audio_and_open_now_playing")
    }

    fun playBiliPartsAndOpenNowPlayingWithSource(
        videoInfo: BiliClient.VideoBasicInfo,
        index: Int,
        coverUrl: String,
        sourceRoute: String?
    ) {
        restoreLyricsAfterAlbumBack = false
        lyricsAlbumRouteObserved = false
        currentPlaybackSourceRoute = sourceRoute
        LyraLogger.d("LYRA-App", "Playing parts from Bili video: ${videoInfo.title}")
        PlayerManager.playBiliVideoParts(videoInfo, index, coverUrl)
        showNowPlaying = true
        ensureAudioServiceStarted(source = "play_bili_parts_and_open_now_playing")
    }

    fun playBiliPartsAndOpenNowPlaying(
        videoInfo: BiliClient.VideoBasicInfo,
        index: Int,
        coverUrl: String
    ) {
        playBiliPartsAndOpenNowPlayingWithSource(
            videoInfo = videoInfo,
            index = index,
            coverUrl = coverUrl,
            null
        )
    }

    fun neteasePlaylistSourceRoute(playlist: PlaylistSummary): String {
        return "playlist_detail/${Uri.encode(navigationGson.toJson(playlist))}"
    }

    fun neteaseAlbumSourceRoute(album: AlbumSummary): String {
        return "netease_album_detail/${Uri.encode(navigationGson.toJson(album))}"
    }

    fun biliPlaylistSourceRoute(playlist: BiliPlaylist): String {
        return "bili_playlist_detail/${Uri.encode(navigationGson.toJson(playlist))}"
    }

    fun biliUploaderSourceRoute(uploader: BiliUploaderSummary): String {
        return "bili_uploader_detail/${Uri.encode(navigationGson.toJson(uploader))}"
    }

    fun localPlaylistSourceRoute(id: Long): String {
        return "local_playlist_detail/$id"
    }

    LyraTheme(
            followSystemDark = followSystemDark,
            forceDark = forceDark,
            accentColorHex = themeSeedColor,
            systemDark = systemDark
    ) {
            AppUpdateStartupHost()
            // changing NavHost's start destination rebuilds its graph and clears the live stack
            val navHostStartDestination = remember { initialMainStartDestination }
            val navController = rememberNavController()
            val backEntry by navController.currentBackStackEntryAsState()
            // Keep every NavHost entry that is still participating in the transition active
            val visibleNavigationEntries by navController.visibleEntries
                .collectAsStateWithLifecycle()
            val visibleNavigationOwners: Set<Any> = remember(
                visibleNavigationEntries,
                backEntry,
                lifecycleOwner
            ) {
                buildSet {
                    if (visibleNavigationEntries.isNotEmpty()) {
                        addAll(visibleNavigationEntries)
                    } else {
                        backEntry?.let(::add)
                    }
                    add(lifecycleOwner)
                }
            }
            val currentRoute = backEntry?.destination?.route
            val visibleNavigationRoutes = remember(visibleNavigationEntries, currentRoute) {
                buildSet<String?> {
                    visibleNavigationEntries.forEach { entry ->
                        add(entry.destination.route)
                    }
                    add(currentRoute)
                }
            }
            val currentBackgroundMotion = resolveMainTabBackgroundMotion(
                route = currentRoute,
                coherentFeedbackEnabled = coherentFeedbackEnabled
            )
            val mainTabBackgroundMotion = if (
                currentBackgroundMotion != MainTabBackgroundMotion.NONE
            ) {
                currentBackgroundMotion
            } else {
                visibleNavigationRoutes.firstNotNullOfOrNull { route ->
                    resolveMainTabBackgroundMotion(
                        route = route,
                        coherentFeedbackEnabled = coherentFeedbackEnabled
                    ).takeUnless { it == MainTabBackgroundMotion.NONE }
                } ?: MainTabBackgroundMotion.NONE
            }
            var mainTabDetailContentHeightPx by remember {
                mutableIntStateOf(0)
            }
            val mainTabBackgroundTargetProgress = if (
                currentBackgroundMotion == MainTabBackgroundMotion.NONE
            ) {
                0f
            } else {
                1f
            }
            val debugSceneVisible = visibleNavigationRoutes.any { route ->
                route in DEBUG_MAIN_TAB_CHILD_ROUTES
            }
            val mainTabBackgroundProgress by animateFloatAsState(
                targetValue = mainTabBackgroundTargetProgress,
                animationSpec = tween(
                    durationMillis = resolveMainTabBackgroundMotionDurationMillis(
                        targetProgress = mainTabBackgroundTargetProgress,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        debugSceneVisible = debugSceneVisible
                    ),
                    easing = mainTabDetailContentOffsetEasing()
                ),
                label = "main_tab_detail_content_handoff"
            )
            val mainTabBackgroundTransform = resolveMainTabBackgroundTransform(
                motion = mainTabBackgroundMotion,
                progress = mainTabBackgroundProgress
            )
            val mainTabLayerTransform = if (
                mainTabBackgroundMotion == MainTabBackgroundMotion.COHERENT_EXIT
            ) {
                mainTabBackgroundTransform
            } else {
                MainTabBackgroundTransform(
                    translationYFraction = 0f,
                    scale = 1f,
                    alpha = 1f
                )
            }
            val effectiveStartDestination = remember(
                currentDefaultStartDestination,
                showHomeTab,
                devModeEnabled
            ) {
                resolveMainStartDestination(
                    preferredRoute = currentDefaultStartDestination,
                    showHomeTab = showHomeTab,
                    devModeEnabled = devModeEnabled
                ) ?: navHostStartDestination
            }
            var selectedMainTabRoute by rememberSaveable(navHostStartDestination) {
                mutableStateOf(navHostStartDestination)
            }
            var pendingMainTabRoute by remember(navHostStartDestination) {
                mutableStateOf<String?>(null)
            }
            // 搜索页时底栏收起层要显示"最后进入的那个标签"，所以单独记住它。
            var lastContentTabRoute by rememberSaveable(navHostStartDestination) {
                mutableStateOf(navHostStartDestination)
            }
            LaunchedEffect(selectedMainTabRoute) {
                if (selectedMainTabRoute != Destinations.Explore.route) {
                    lastContentTabRoute = selectedMainTabRoute
                }
            }
            val mainTabTransitionState = rememberMainTabLayerTransitionState(
                selectedMainTabRoute
            )
            LaunchedEffect(currentRoute, navHostStartDestination) {
                val observedRoute = currentRoute
                if (
                    shouldAcceptObservedMainTabRoute(
                        observedRoute = observedRoute,
                        pendingRoute = pendingMainTabRoute
                    )
                ) {
                    selectedMainTabRoute = checkNotNull(observedRoute)
                    if (pendingMainTabRoute == observedRoute) {
                        pendingMainTabRoute = null
                    }
                }
            }
            var visibleMainTabGlassOwners by remember(navHostStartDestination) {
                mutableStateOf<Set<MainTabGlassOwner>>(
                    setOf(MainTabGlassOwner(navHostStartDestination))
                )
            }
            val activeAdvancedGlassOwners: Set<Any> = remember(
                visibleNavigationOwners,
                visibleMainTabGlassOwners,
                selectedMainTabRoute
            ) {
                visibleNavigationOwners +
                    visibleMainTabGlassOwners +
                    MainTabGlassOwner(selectedMainTabRoute)
            }
            fun navigateToMainTab(route: String) {
                if (selectedMainTabRoute != route) {
                    selectedMainTabRoute = route
                }
                mainTabTransitionState.request(route)
                if (
                    !shouldDispatchMainTabNavigation(
                        currentRoute = currentRoute,
                        pendingRoute = pendingMainTabRoute,
                        targetRoute = route
                    )
                ) {
                    return
                }
                pendingMainTabRoute = route
                // 下载等非主标签页面可能叠在主标签之上：先弹回目标标签，
                // 弹不到再走常规导航，避免"点首页仍停留在下载页"。
                if (navController.popBackStack(route, inclusive = false)) {
                    return
                }
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            fun showLauncherShortcutToast(messageRes: Int) {
                AppFeedback.showToast(
                    context = context,
                    message = composeResources.getString(messageRes)
                )
            }
            LaunchedEffect(launcherShortcutRequest, playbackBootstrapReady) {
                val request = launcherShortcutRequest ?: return@LaunchedEffect
                if (!playbackBootstrapReady) return@LaunchedEffect

                launcherShortcutMainTabRoute(request.action)?.let { route ->
                    navigateToMainTab(route)
                    latestOnLauncherShortcutRequestConsumed(request)
                    return@LaunchedEffect
                }

                when (request.action) {
                    LauncherShortcutAction.ContinuePlayback -> {
                        if (PlayerManager.hasItems()) {
                            showNowPlaying = true
                            PlayerManager.play()
                            scheduleAudioServiceStart(
                                "launcher_shortcut_continue_playback",
                                true
                            )
                        } else {
                            navigateToMainTab(Destinations.Library.route)
                            showLauncherShortcutToast(
                                R.string.launcher_shortcut_no_resumable_queue
                            )
                        }
                    }
                    LauncherShortcutAction.ShuffleFavorites -> {
                        val playlistsReady = withTimeoutOrNull(
                            LAUNCHER_SHORTCUT_PLAYLIST_READY_TIMEOUT_MS
                        ) {
                            PlayerManager.localPlaylistsReadyFlow.first { ready -> ready }
                        } == true
                        val favoritesSongs = if (playlistsReady) {
                            FavoritesPlaylist
                                .firstOrNull(PlayerManager.playlistsFlow.value, context)
                                ?.songs
                                .orEmpty()
                        } else {
                            emptyList()
                        }
                        if (favoritesSongs.isEmpty()) {
                            navigateToMainTab(Destinations.Library.route)
                            showLauncherShortcutToast(
                                R.string.launcher_shortcut_favorites_empty
                            )
                        } else {
                            PlayerManager.setShuffle(true)
                            playSongsAndOpenNowPlaying(
                                songs = favoritesSongs,
                                index = Random.nextInt(favoritesSongs.size),
                                sourceRoute = localPlaylistSourceRoute(
                                    FavoritesPlaylist.SYSTEM_ID
                                )
                            )
                        }
                    }
                    LauncherShortcutAction.OpenExplore,
                    LauncherShortcutAction.OpenLibrary -> Unit
                }

                latestOnLauncherShortcutRequestConsumed(request)
            }
            suspend fun preloadNeteaseDetailRouteCover(route: String) {
                val coverUrl = runCatching {
                    when {
                        route.startsWith("playlist_detail/") -> {
                            val json = Uri.decode(route.substringAfter("playlist_detail/"))
                            navigationGson.fromJson(json, PlaylistSummary::class.java).picUrl
                        }
                        route.startsWith("netease_album_detail/") -> {
                            val json = Uri.decode(route.substringAfter("netease_album_detail/"))
                            navigationGson.fromJson(json, AlbumSummary::class.java).picUrl
                        }
                        else -> null
                    }
                }.getOrNull()
                CoverArtColorCache.preload(context, coverUrl, offlineMode)
            }
            fun navigateToNeteaseAlbum(
                album: AlbumSummary,
                afterNavigate: () -> Unit = {}
            ) {
                scope.launch {
                    CoverArtColorCache.preload(context, album.picUrl, offlineMode)
                    navController.navigate(neteaseAlbumSourceRoute(album)) {
                        launchSingleTop = true
                    }
                    afterNavigate()
                }
            }
            fun navigateToPlaybackSourceRoute(route: String) {
                scope.launch {
                    preloadNeteaseDetailRouteCover(route)
                    showNowPlayingLyrics = false
                    showNowPlaying = false
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }
            fun navigateToNeteaseArtist(artist: NeteaseArtistSummary) {
                val json = Uri.encode(navigationGson.toJson(artist))
                val currentEntry = navController.currentBackStackEntry
                val currentIsArtist =
                    currentEntry?.destination?.route == Destinations.NeteaseArtistDetail.route
                val currentArtist = currentEntry
                    ?.arguments
                    ?.getString("artistJson")
                    ?.let {
                        runCatching {
                            navigationGson.fromJson(it, NeteaseArtistSummary::class.java)
                        }.getOrNull()
                    }
                if (currentArtist?.id == artist.id) {
                    return
                }
                if (currentIsArtist) {
                    navController.popBackStack()
                }
                navController.navigate("netease_artist_detail/$json") {
                    launchSingleTop = true
                }
            }
            fun navigateToTencentArtist(artist: CustomChannelArtist) {
                if (artist.id.isBlank()) return
                val json = Uri.encode(navigationGson.toJson(artist))
                val currentEntry = navController.currentBackStackEntry
                val currentIsArtist =
                    currentEntry?.destination?.route == Destinations.TencentArtistDetail.route
                val currentArtist = currentEntry
                    ?.takeIf { currentIsArtist }
                    ?.arguments
                    ?.getString("artistJson")
                    ?.let {
                        runCatching {
                            navigationGson.fromJson(it, CustomChannelArtist::class.java)
                        }.getOrNull()
                    }
                if (currentArtist?.id == artist.id) {
                    return
                }
                if (currentIsArtist) {
                    navController.popBackStack()
                }
                navController.navigate("tencent_artist_detail/$json") {
                    launchSingleTop = true
                }
            }
            fun navigateToBiliUploader(uploader: BiliUploaderSummary) {
                if (uploader.mid <= 0L) return
                val currentEntry = navController.currentBackStackEntry
                val currentIsUploader =
                    currentEntry?.destination?.route == Destinations.BiliUploaderDetail.route
                val currentUploader = currentEntry
                    ?.arguments
                    ?.getString("uploaderJson")
                    ?.let {
                        runCatching {
                            navigationGson.fromJson(it, BiliUploaderSummary::class.java)
                        }.getOrNull()
                    }
                if (currentUploader?.mid == uploader.mid) {
                    return
                }
                if (currentIsUploader) {
                    navController.popBackStack()
                }
                navController.navigate(biliUploaderSourceRoute(uploader)) {
                    launchSingleTop = true
                }
            }
            fun navigateToYouTubeMusicCreator(creator: YouTubeMusicCreatorSummary) {
                if (creator.browseId.isBlank()) return
                val currentEntry = navController.currentBackStackEntry
                val currentCreator = currentEntry
                    ?.takeIf {
                        it.destination.route == Destinations.YouTubeMusicCreatorDetail.route
                    }
                    ?.arguments
                    ?.getString("creatorJson")
                    ?.let { creatorJson ->
                        runCatching {
                            navigationGson.fromJson(
                                creatorJson,
                                YouTubeMusicCreatorSummary::class.java
                            )
                        }.getOrNull()
                    }
                if (currentCreator?.browseId == creator.browseId) {
                    return
                }
                val json = Uri.encode(navigationGson.toJson(creator))
                navController.navigate("youtube_music_creator_detail/$json") {
                    launchSingleTop = true
                }
            }
            fun navigateToYouTubeMusicPlaylist(playlist: YouTubeMusicPlaylist) {
                if (playlist.browseId.isBlank()) return
                val currentEntry = navController.currentBackStackEntry
                val currentPlaylist = currentEntry
                    ?.takeIf {
                        it.destination.route == Destinations.YouTubeMusicPlaylistDetail.route
                    }
                    ?.arguments
                    ?.getString("playlistJson")
                    ?.let { playlistJson ->
                        runCatching {
                            navigationGson.fromJson(
                                playlistJson,
                                YouTubeMusicPlaylist::class.java
                            )
                        }.getOrNull()
                    }
                if (currentPlaylist?.browseId == playlist.browseId) {
                    return
                }
                val json = Uri.encode(navigationGson.toJson(playlist))
                navController.navigate("youtube_music_playlist_detail/$json") {
                    launchSingleTop = true
                }
            }
            LaunchedEffect(currentRoute, restoreLyricsAfterAlbumBack) {
                if (!restoreLyricsAfterAlbumBack) {
                    lyricsAlbumRouteObserved = false
                    return@LaunchedEffect
                }
                if (currentRoute == Destinations.NeteaseAlbumDetail.route) {
                    lyricsAlbumRouteObserved = true
                    return@LaunchedEffect
                }
                if (lyricsAlbumRouteObserved) {
                    restoreLyricsAfterAlbumBack = false
                    lyricsAlbumRouteObserved = false
                    showNowPlayingLyrics = true
                    showNowPlaying = true
                }
            }
            val bottomBarItems = remember(showHomeTab, devModeEnabled) {
                buildList {
                    if (showHomeTab) add(Destinations.Home to Icons.Outlined.Home)
                    add(Destinations.Library to Icons.Outlined.LibraryMusic)
                    add(Destinations.Settings to Icons.Outlined.Settings)
                    if (devModeEnabled) add(Destinations.Debug to Icons.Outlined.BugReport)
                }
            }
            val searchDockActive = selectedMainTabRoute == Destinations.Explore.route

            val snackbarHostState = remember { SnackbarHostState() }
            val homeHostRuntimeState = rememberHomeHostRuntimeState()

            @Composable
            fun RenderNavigationScene(
                revealTopFraction: Float = 0f,
                contentTranslationYFraction: Float = 0f,
                contentScale: Float = 1f,
                navigationDepth: Int = 0,
                fixedBackground: Boolean = false,
                content: @Composable () -> Unit
            ) {
                AdvancedGlassSceneLayer(
                    controller = advancedGlassController,
                    modifier = Modifier.advancedGlassSceneZIndex(navigationDepth),
                    motion = AdvancedGlassSceneMotion(
                        revealTopFraction = revealTopFraction,
                        contentTranslationYFraction = contentTranslationYFraction,
                        contentScale = contentScale
                    ),
                    disableStretchOverscroll = backgroundImageUri != null,
                    fixedBackground = fixedBackground,
                    background = {
                        // 场景自绘壁纸背景, 玻璃模糊要采样它
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            CustomBackground(
                                imageUri = backgroundImageUri,
                                blur = backgroundImageBlur,
                                alpha = effectiveBackgroundImageAlpha
                            )
                        }
                    },
                    content = { content() }
                )
            }

            @Composable
            fun AnimatedContentScope.RenderNavHostScene(
                sceneRoute: String?,
                content: @Composable () -> Unit
            ) {
                val sceneDepth = transparentNavigationDepth(sceneRoute)
                val currentDepth = transparentNavigationDepth(currentRoute)
                val enteringFromDeeperScene = sceneRoute == currentRoute &&
                    visibleNavigationRoutes.any { route ->
                        transparentNavigationDepth(route) > sceneDepth
                    }
                val exitingToDeeperScene = sceneRoute != currentRoute &&
                    currentDepth > sceneDepth
                val motion = transition.animateAdvancedGlassVisibilitySceneMotion(
                    coherentFeedbackEnabled = coherentFeedbackEnabled,
                    enteringFromDeeperScene = enteringFromDeeperScene,
                    exitingToDeeperScene = exitingToDeeperScene,
                    label = "nav_scene_${sceneRoute.orEmpty()}"
                )
                RenderNavigationScene(
                    revealTopFraction = motion.revealTopFraction,
                    contentTranslationYFraction = motion.contentTranslationYFraction,
                    contentScale = motion.contentScale,
                    navigationDepth = sceneDepth,
                    fixedBackground = false,
                    content = content
                )
            }

            @Composable
            fun RenderMainTabNavigationScene(
                revealTopFraction: Float,
                contentTranslationYFraction: Float,
                contentScale: Float,
                sceneDepth: Int = 0,
                content: @Composable () -> Unit
            ) {
                val applyExternalDrawerMotion =
                    mainTabBackgroundMotion == MainTabBackgroundMotion.DRAWER_SINK
                RenderNavigationScene(
                    revealTopFraction = revealTopFraction,
                    contentTranslationYFraction = contentTranslationYFraction +
                        if (applyExternalDrawerMotion) {
                            mainTabBackgroundTransform.translationYFraction
                        } else {
                            0f
                        },
                    contentScale = contentScale * if (applyExternalDrawerMotion) {
                        mainTabBackgroundTransform.scale
                    } else {
                        1f
                    },
                    navigationDepth = sceneDepth,
                    // 只有 tab 根列表 (sceneDepth 0) 走固定背景, 横滑切 tab 时壁纸不动
                    // 嵌套详情必须保留不透明自背景, 靠揭示裁剪盖住退出列表, 否则两页内容互透叠印
                    fixedBackground = backgroundImageUri != null && sceneDepth == 0,
                    content = content
                )
            }

            @Composable
            fun RenderMainTabRoute(route: String) {
                when (route) {
                    Destinations.Home.route -> HomeHostScreen(
                        showTrendingCard = showHomeTrendingCard,
                        showRadarCard = showHomeRadarCard,
                        showRecommendedCard = showHomeRecommendedCard,
                        homeUsageEntries = homeUsageSnapshot.entries,
                        homeUsageLoaded = homeUsageSnapshot.isLoaded,
                        offlineMode = offlineMode,
                        runtimeState = homeHostRuntimeState,
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        onPlayBiliAudioWithSourceRoute = ::playBiliAudioAndOpenNowPlayingWithSource,
                        onPlayBiliPartsWithSourceRoute = ::playBiliPartsAndOpenNowPlayingWithSource,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        neteaseAlbumSourceRoute = ::neteaseAlbumSourceRoute,
                        biliPlaylistSourceRoute = ::biliPlaylistSourceRoute,
                        localPlaylistSourceRoute = ::localPlaylistSourceRoute,
                        onOpenSettings = { navigateToMainTab(Destinations.Settings.route) },
                        avatarUri = userProfile.avatarUri,
                        onOpenAccount = { navController.navigate(Destinations.Account.route) },
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )

                    Destinations.Explore.route -> ExploreHostScreen(
                        offlineMode = offlineMode,
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        onSongPlayPreservingQueue =
                            ::playSongPreservingQueueAndOpenNowPlaying,
                        onSongPlayNext = ::addSongToQueueNextFromSearch,
                        onSongAddToQueueEnd = ::addSongToQueueEndFromSearch,
                        onPlayParts = ::playBiliPartsAndOpenNowPlaying,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )

                    Destinations.Library.route -> LibraryHostScreen(
                        onSongClick = ::playSongsAndOpenNowPlaying,
                        onSongClickWithSourceRoute = ::playSongsAndOpenNowPlaying,
                        onPlayBiliAudioWithSourceRoute = ::playBiliAudioAndOpenNowPlayingWithSource,
                        onPlayBiliPartsWithSourceRoute = ::playBiliPartsAndOpenNowPlayingWithSource,
                        neteasePlaylistSourceRoute = ::neteasePlaylistSourceRoute,
                        neteaseAlbumSourceRoute = ::neteaseAlbumSourceRoute,
                        biliPlaylistSourceRoute = ::biliPlaylistSourceRoute,
                        localPlaylistSourceRoute = ::localPlaylistSourceRoute,
                        onOpenRecent = {
                            navController.navigate(Destinations.Recent.route)
                        },
                        onOpenStats = {
                            navController.navigate(Destinations.PlaybackStats.route)
                        },
                        onOpenDownloads = {
                            navController.navigate(Destinations.DownloadManager.route)
                        },
                        offlineMode = offlineMode,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )

                    Destinations.Settings.route -> SettingsHostScreen(
                        isDarkTheme = isDark,
                        themeMode = themeMode,
                        onThemeToggleRequest = ::requestThemeToggle,
                        onThemeModeRequest = ::requestThemeModeChange,
                        preferredQuality = preferredQuality,
                        onQualityChange = { scope.launch { repo.setAudioQuality(it) } },
                        youtubePreferredQuality = youtubePreferredQuality,
                        onYouTubeQualityChange = {
                            scope.launch { repo.setYouTubeAudioQuality(it) }
                        },
                        biliPreferredQuality = biliPreferredQuality,
                        onBiliQualityChange = { scope.launch { repo.setBiliAudioQuality(it) } },
                        mobileDataFollowDefaultAudioQuality =
                            mobileDataFollowDefaultAudioQuality,
                        onMobileDataFollowDefaultAudioQualityChange = { enabled ->
                            scope.launch {
                                repo.setMobileDataFollowDefaultAudioQuality(enabled)
                            }
                        },
                        mobileDataNeteaseAudioQuality = mobileDataNeteaseAudioQuality,
                        onMobileDataNeteaseAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataNeteaseAudioQuality(quality)
                            }
                        },
                        mobileDataYouTubeAudioQuality = mobileDataYouTubeAudioQuality,
                        onMobileDataYouTubeAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataYouTubeAudioQuality(quality)
                            }
                        },
                        mobileDataBiliAudioQuality = mobileDataBiliAudioQuality,
                        onMobileDataBiliAudioQualityChange = { quality ->
                            scope.launch {
                                repo.setMobileDataBiliAudioQuality(quality)
                            }
                        },
                        accentColorHex = themeSeedColor,
                        onAccentColorChange = { hex ->
                            scope.launch { repo.setThemeSeedColor(hex) }
                        },
                        themeColorPalette = themeColorPalette,
                        onAddColorToPalette = { hex ->
                            scope.launch { repo.addThemePaletteColor(hex) }
                        },
                        onRemoveColorFromPalette = { hex ->
                            scope.launch { repo.removeThemePaletteColor(hex) }
                        },
                        devModeEnabled = devModeEnabled,
                        onDevModeChange = { enabled ->
                            scope.launch { repo.setDevModeEnabled(enabled) }
                        },
                        lyricBlurEnabled = lyricBlurEnabled,
                        onLyricBlurEnabledChange = { enabled ->
                            scope.launch { repo.setLyricBlurEnabled(enabled) }
                        },
                        lyricBlurAmount = lyricBlurAmount,
                        onLyricBlurAmountChange = { amount ->
                            scope.launch { repo.setLyricBlurAmount(amount) }
                        },
                        cloudMusicLyricDefaultOffsetMs = cloudMusicLyricDefaultOffsetMs,
                        onCloudMusicLyricDefaultOffsetMsChange = { offsetMs ->
                            scope.launch {
                                val previousOffset = cloudMusicLyricDefaultOffsetMs
                                if (previousOffset == offsetMs) {
                                    return@launch
                                }
                                PlayerManager.rebaseUserLyricOffsetsForSource(
                                    targetSource = MusicPlatform.CLOUD_MUSIC,
                                    previousDefaultOffsetMs = previousOffset,
                                    newDefaultOffsetMs = offsetMs
                                )
                                runCatching {
                                    repo.setCloudMusicLyricDefaultOffsetMs(offsetMs)
                                }.onFailure {
                                    PlayerManager.rebaseUserLyricOffsetsForSource(
                                        targetSource = MusicPlatform.CLOUD_MUSIC,
                                        previousDefaultOffsetMs = offsetMs,
                                        newDefaultOffsetMs = previousOffset
                                    )
                                }.getOrThrow()
                            }
                        },
                        qqMusicLyricDefaultOffsetMs = qqMusicLyricDefaultOffsetMs,
                        onQqMusicLyricDefaultOffsetMsChange = { offsetMs ->
                            scope.launch {
                                val previousOffset = qqMusicLyricDefaultOffsetMs
                                if (previousOffset == offsetMs) {
                                    return@launch
                                }
                                PlayerManager.rebaseUserLyricOffsetsForSource(
                                    targetSource = MusicPlatform.QQ_MUSIC,
                                    previousDefaultOffsetMs = previousOffset,
                                    newDefaultOffsetMs = offsetMs
                                )
                                runCatching {
                                    repo.setQqMusicLyricDefaultOffsetMs(offsetMs)
                                }.onFailure {
                                    PlayerManager.rebaseUserLyricOffsetsForSource(
                                        targetSource = MusicPlatform.QQ_MUSIC,
                                        previousDefaultOffsetMs = offsetMs,
                                        newDefaultOffsetMs = previousOffset
                                    )
                                }.getOrThrow()
                            }
                        },
                        floatingLyricsPreferences = floatingLyricsPreferences,
                        onFloatingLyricsPreferencesChange = { preferences ->
                            scope.launch { repo.setFloatingLyricsPreferences(preferences) }
                        },
                        advancedBlurEnabled = advancedBlurEnabled,
                        onAdvancedBlurEnabledChange = { enabled ->
                            scope.launch { repo.setAdvancedBlurEnabled(enabled) }
                        },
                        enhancedAdvancedBlurEnabled = enhancedAdvancedBlurEnabled,
                        onEnhancedAdvancedBlurEnabledChange = { enabled ->
                            scope.launch {
                                repo.setEnhancedAdvancedBlurEnabled(enabled)
                            }
                        },
                        enhancedAdvancedBlurRadiusDp = enhancedAdvancedBlurRadiusDp,
                        onEnhancedAdvancedBlurRadiusDpChange = { radiusDp ->
                            scope.launch {
                                repo.setEnhancedAdvancedBlurRadiusDp(radiusDp)
                            }
                        },
                        advancedBlurQuality = advancedBlurQuality,
                        onAdvancedBlurQualityChange = { quality ->
                            scope.launch { repo.setAdvancedBlurQuality(quality) }
                        },
                        nowPlayingAudioReactiveEnabled = nowPlayingAudioReactiveEnabled,
                        onNowPlayingAudioReactiveEnabledChange = { enabled ->
                            scope.launch { repo.setNowPlayingAudioReactiveEnabled(enabled) }
                        },
                        nowPlayingDynamicBackgroundEnabled = nowPlayingDynamicBackgroundEnabled,
                        onNowPlayingDynamicBackgroundEnabledChange = { enabled ->
                            scope.launch { repo.setNowPlayingDynamicBackgroundEnabled(enabled) }
                        },
                        nowPlayingCoverBlurBackgroundEnabled =
                            nowPlayingCoverBlurBackgroundEnabled,
                        onNowPlayingCoverBlurBackgroundEnabledChange = { enabled ->
                            scope.launch {
                                repo.setNowPlayingCoverBlurBackgroundEnabled(enabled)
                            }
                        },
                        nowPlayingCoverBlurAmount = nowPlayingCoverBlurAmount,
                        onNowPlayingCoverBlurAmountChange = { amount ->
                            scope.launch { repo.setNowPlayingCoverBlurAmount(amount) }
                        },
                        nowPlayingCoverBlurDarken = nowPlayingCoverBlurDarken,
                        onNowPlayingCoverBlurDarkenChange = { amount ->
                            scope.launch { repo.setNowPlayingCoverBlurDarken(amount) }
                        },
                        lyricFontScales = lyricFontScales,
                        onLyricFontScaleChange = { target: LyricFontScaleTarget, scale ->
                            scope.launch { repo.setLyricFontScale(target, scale) }
                        },
                        uiDensityScale = uiDensityScale,
                        onUiDensityScaleChange = { scale ->
                            scope.launch { repo.setUiDensityScale(scale) }
                        },
                        bypassProxy = bypassProxy,
                        onBypassProxyChange = { enabled ->
                            scope.launch { repo.setBypassProxy(enabled) }
                        },
                        backgroundImageUri = backgroundImageUri,
                        onBackgroundImageChange = { uri ->
                            scope.launch { repo.setBackgroundImageUri(uri?.toString()) }
                        },
                        downloadDirectoryUri = downloadDirectoryUri,
                        downloadFileNameTemplate = downloadFileNameTemplate,
                        onDownloadDirectoryUriChange = { uri, label ->
                            scope.launch {
                                repo.setDownloadDirectory(uri, label)
                                ManagedDownloadStorage.updateConfiguredTreeUri(uri)
                                ManagedDownloadStorage.updateCustomDirectoryLabel(label)
                            }
                        },
                        onDownloadFileNameTemplateChange = { template ->
                            scope.launch { repo.setDownloadFileNameTemplate(template) }
                        },
                        backgroundImageBlur = backgroundImageBlur,
                        onBackgroundImageBlurChange = {},
                        onBackgroundImageBlurChangeFinished = { blur ->
                            scope.launch { repo.setBackgroundImageBlur(blur) }
                        },
                        backgroundImageAlpha = effectiveBackgroundImageAlpha,
                        onBackgroundImageAlphaChange = { alpha ->
                            pendingBackgroundImageAlpha = alpha
                        },
                        onBackgroundImageAlphaChangeFinished = { alpha ->
                            pendingBackgroundImageAlpha = alpha
                            scope.launch { repo.setBackgroundImageAlpha(alpha) }
                        },
                        defaultStartDestination = currentDefaultStartDestination,
                        onDefaultStartDestinationChange = { route ->
                            scope.launch { repo.setDefaultStartDestination(route) }
                        },
                        showHomeTrendingCard = showHomeTrendingCard,
                        onShowHomeTrendingCardChange = { enabled ->
                            scope.launch { repo.setHomeCardTrending(enabled) }
                        },
                        showHomeRadarCard = showHomeRadarCard,
                        onShowHomeRadarCardChange = { enabled ->
                            scope.launch { repo.setHomeCardRadar(enabled) }
                        },
                        showHomeRecommendedCard = showHomeRecommendedCard,
                        onShowHomeRecommendedCardChange = { enabled ->
                            scope.launch { repo.setHomeCardRecommended(enabled) }
                        },
                        homeHasRecentUsage = homeUsageSnapshot.entries.isNotEmpty(),
                        playbackFadeIn = playbackFadeIn,
                        onPlaybackFadeInChange = { enabled ->
                            scope.launch { repo.setPlaybackFadeIn(enabled) }
                        },
                        playbackCrossfadeNext = playbackCrossfadeNext,
                        onPlaybackCrossfadeNextChange = { enabled ->
                            scope.launch { repo.setPlaybackCrossfadeNext(enabled) }
                        },
                        sleepTimerFinishCurrentOnExpiry = sleepTimerFinishCurrentOnExpiry,
                        onSleepTimerFinishCurrentOnExpiryChange = { enabled ->
                            scope.launch {
                                repo.setSleepTimerFinishCurrentOnExpiry(enabled)
                            }
                        },
                        playbackFadeInDurationMs = playbackFadeInDurationMs,
                        onPlaybackFadeInDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackFadeInDurationMs(duration) }
                        },
                        playbackFadeOutDurationMs = playbackFadeOutDurationMs,
                        onPlaybackFadeOutDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackFadeOutDurationMs(duration) }
                        },
                        playbackCrossfadeInDurationMs = playbackCrossfadeInDurationMs,
                        onPlaybackCrossfadeInDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackCrossfadeInDurationMs(duration) }
                        },
                        playbackCrossfadeOutDurationMs = playbackCrossfadeOutDurationMs,
                        onPlaybackCrossfadeOutDurationMsChange = { duration ->
                            scope.launch { repo.setPlaybackCrossfadeOutDurationMs(duration) }
                        },
                        playbackVolumeNormalizationEnabled =
                            playbackVolumeNormalizationEnabled,
                        onPlaybackVolumeNormalizationEnabledChange = { enabled ->
                            PlayerManager.setPlaybackVolumeNormalizationEnabled(enabled)
                        },
                        playbackHighResolutionOutputEnabled =
                            playbackHighResolutionOutputEnabled,
                        onPlaybackHighResolutionOutputEnabledChange = { enabled ->
                            PlayerManager.setPlaybackHighResolutionOutputEnabled(enabled)
                            AppFeedback.show(
                                context = context,
                                message = composeResources.getString(R.string.settings_restart_hint)
                            )
                        },
                        playbackVolumeBalance = playbackVolumeBalance,
                        onPlaybackVolumeBalanceChange = { balance ->
                            PlayerManager.setPlaybackVolumeBalance(balance)
                        },
                        keepLastPlaybackProgress = keepLastPlaybackProgress,
                        onKeepLastPlaybackProgressChange = { enabled ->
                            scope.launch { repo.setKeepLastPlaybackProgress(enabled) }
                        },
                        rememberLongFormPlaybackProgress = rememberLongFormPlaybackProgress,
                        onRememberLongFormPlaybackProgressChange = { enabled ->
                            scope.launch {
                                repo.setRememberLongFormPlaybackProgress(enabled)
                            }
                        },
                        keepPlaybackModeState = keepPlaybackModeState,
                        onKeepPlaybackModeStateChange = { enabled ->
                            scope.launch { repo.setKeepPlaybackModeState(enabled) }
                        },
                        neteaseAutoSourceSwitch = neteaseAutoSourceSwitch,
                        onNeteaseAutoSourceSwitchChange = { enabled ->
                            scope.launch { repo.setNeteaseAutoSourceSwitch(enabled) }
                        },
                        neteaseLocalSourceFallback = neteaseLocalSourceFallback,
                        onNeteaseLocalSourceFallbackChange = { enabled ->
                            scope.launch { repo.setNeteaseLocalSourceFallback(enabled) }
                        },
                        stopOnBluetoothDisconnect = stopOnBluetoothDisconnect,
                        onStopOnBluetoothDisconnectChange = { enabled ->
                            scope.launch { repo.setStopOnBluetoothDisconnect(enabled) }
                        },
                        usbExclusivePlayback = usbExclusivePlayback,
                        onUsbExclusivePlaybackChange = { enabled ->
                            if (PlayerManager.beginUsbExclusiveToggleTransitionFromUi(enabled)) {
                                scope.launch { repo.setUsbExclusivePlayback(enabled) }
                            }
                        },
                        allowMixedPlayback = allowMixedPlayback,
                        onAllowMixedPlaybackChange = { enabled ->
                            scope.launch { repo.setAllowMixedPlayback(enabled) }
                        },
                        preemptAudioFocus = preemptAudioFocus,
                        onPreemptAudioFocusChange = { enabled ->
                            scope.launch { repo.setPreemptAudioFocus(enabled) }
                        },
                        maxCacheSizeBytes = maxCacheSizeBytes,
                        onMaxCacheSizeBytesChange = { size ->
                            scope.launch { repo.setMaxCacheSizeBytes(size) }
                        },
                        onClearCacheClick = { options ->
                            scope.launch {
                                val messages = mutableListOf<String>()
                                if (options.needsPlayerCacheClear) {
                                    val (_, message) = PlayerManager.clearCache(
                                        clearAudio = options.audioCache,
                                        clearImage = options.imageCache
                                    )
                                    messages += message
                                }
                                if (options.needsExtraCacheClear) {
                                    if (options.lyricsCache) {
                                        PlayerLyricsProvider.clearLyricsCaches(
                                            neteaseLyricsCache = PlayerManager.neteaseLyricsCache,
                                            ytMusicLyricsCache = PlayerManager.ytMusicLyricsCache
                                        )
                                        withContext(Dispatchers.IO) {
                                            PlayerLyricsProvider.clearPersistentLyricCache(
                                                AppContainer.applicationContext
                                            )
                                        }
                                    }
                                    val result = clearExtraStorageCaches(context, options)
                                    messages += when {
                                        !result.success -> composeResources.getString(
                                            R.string.storage_extra_cache_clear_partial
                                        )
                                        result.roomBytesMadeReusable > 0L ->
                                            composeResources.getString(
                                                R.string.storage_extra_cache_clear_room_complete,
                                                formatFileSize(result.freedBytes),
                                                formatFileSize(result.roomBytesMadeReusable)
                                            )
                                        else -> composeResources.getString(
                                            R.string.storage_extra_cache_clear_complete,
                                            formatFileSize(result.freedBytes)
                                        )
                                    }
                                }
                                snackbarHostState.showNeriSnackbar(messages.joinToString(" · "))
                            }
                        },
                        onBeforeLanguageRestart = clearThemeRevealState,
                        onLanguageChanged = onLanguageChanged,
                        coherentFeedbackEnabled = coherentFeedbackEnabled,
                        renderScene = { revealTop, translationY, scale, sceneDepth, sceneContent ->
                            RenderMainTabNavigationScene(
                                revealTop,
                                translationY,
                                scale,
                                sceneDepth = sceneDepth,
                                content = sceneContent
                            )
                        }
                    )

                    Destinations.Debug.route -> {
                        val debugHomeScrollState = rememberScrollState()
                        RenderMainTabNavigationScene(
                            revealTopFraction = 0f,
                            contentTranslationYFraction = 0f,
                            contentScale = 1f
                        ) {
                            DebugHomeScreen(
                            scrollState = debugHomeScrollState,
                            alwaysRecordLogsEnabled = alwaysRecordLogsEnabled,
                            onAlwaysRecordLogsChange = { enabled ->
                                scope.launch { repo.setAlwaysRecordLogsEnabled(enabled) }
                            },
                            onOpenUsbExclusiveDebug = {
                                navController.navigate(Destinations.DebugUsbExclusive.route)
                            },
                            onOpenYouTubeDebug = {
                                navController.navigate(Destinations.DebugYouTube.route)
                            },
                            onOpenBiliDebug = {
                                navController.navigate(Destinations.DebugBili.route)
                            },
                            onOpenNeteaseDebug = {
                                navController.navigate(Destinations.DebugNetease.route)
                            },
                            onOpenSearchDebug = {
                                navController.navigate(Destinations.DebugSearch.route)
                            },
                            onOpenLogs = {
                                navController.navigate(Destinations.DebugLogsList.route)
                            },
                            onOpenCrashLogs = {
                                navController.navigate(Destinations.DebugCrashLogsList.route)
                            },
                            onTestExceptionHandler = { crashType ->
                            val crashMessage = composeResources.getString(R.string.test_exception_message)
                            when (crashType) {
                                DebugCrashTestType.JvmHandled -> {
                                    ExceptionHandler.safeExecute("DebugTestHandled") {
                                        throw RuntimeException(crashMessage)
                                    }
                                }

                                DebugCrashTestType.JvmUncaughtMain -> {
                                    Handler(Looper.getMainLooper()).post {
                                        throw RuntimeException(crashMessage)
                                    }
                                }

                                DebugCrashTestType.JvmUncaughtWorker -> {
                                    Thread {
                                        throw RuntimeException(crashMessage)
                                    }.start()
                                }

                                DebugCrashTestType.MainThreadAnr -> {
                                    AnrWatchdog.triggerTestAnr(context)
                                }

                                DebugCrashTestType.NativeSigSegv -> {
                                    Handler(Looper.getMainLooper()).post {
                                        NativeCrashHandler.triggerTestCrash(
                                            context = context,
                                            crashType = NativeCrashHandler.TestCrashType.SigSegv
                                        )
                                    }
                                }

                                DebugCrashTestType.NativeSigAbrt -> {
                                    Handler(Looper.getMainLooper()).post {
                                        NativeCrashHandler.triggerTestCrash(
                                            context = context,
                                            crashType = NativeCrashHandler.TestCrashType.SigAbrt
                                        )
                                    }
                                }
                            }
                            },
                            onHideDebugMode = {
                                scope.launch { repo.setDevModeEnabled(false) }
                                navController.navigate(Destinations.Settings.route) {
                                    popUpTo(Destinations.Debug.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            )
                        }
                    }
                }
            }

            val effectiveDynamicBackgroundEnabled =
                nowPlayingDynamicBackgroundEnabled && !nowPlayingCoverBlurBackgroundEnabled
            val effectiveAudioReactiveEnabled =
                nowPlayingAudioReactiveEnabled && effectiveDynamicBackgroundEnabled

            DisposableEffect(showNowPlaying, effectiveAudioReactiveEnabled, lifecycleResumed) {
                AudioReactive.enabled = showNowPlaying && effectiveAudioReactiveEnabled && lifecycleResumed
                onDispose { AudioReactive.enabled = false }
            }

            DisposableEffect(showNowPlaying, lifecycleResumed) {
                PlayerManager.updateInteractiveNowPlayingVisible(showNowPlaying && lifecycleResumed)
                onDispose { PlayerManager.updateInteractiveNowPlayingVisible(false) }
            }

            val activity = remember(context) { context.findActivity() }
            DisposableEffect(activity, showNowPlaying, nowPlayingKeepScreenOn, lifecycleResumed) {
                val window = activity?.window
                val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                val shouldKeepScreenOn = showNowPlaying && nowPlayingKeepScreenOn && lifecycleResumed
                val wasKeepScreenOn = window?.attributes?.flags?.and(keepScreenOnFlag) == keepScreenOnFlag
                if (shouldKeepScreenOn) {
                    window?.addFlags(keepScreenOnFlag)
                }
                onDispose {
                    if (shouldKeepScreenOn && !wasKeepScreenOn) {
                        window?.clearFlags(keepScreenOnFlag)
                    }
                }
            }

            AdvancedGlassHost(
                controller = advancedGlassController,
                backgroundBackdrop = backgroundGlassBackdrop,
                contentBackdrop = contentGlassBackdrop,
                activeNavigationOwners = activeAdvancedGlassOwners,
                disableStretchOverscroll = backgroundImageUri != null
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .captureAdvancedGlassBackdrop(backgroundGlassBackdrop)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    CustomBackground(
                        imageUri = backgroundImageUri,
                        blur = backgroundImageBlur,
                        alpha = effectiveBackgroundImageAlpha
                    )
                }

                val containerColor = Color.Transparent

                // 迷你播放器常驻组合：下拉手势一开始它的封面/歌名落点就已经量好，
                // 不用等首帧布局再去补建飞行层，松手时也不会突然组一整块玻璃而卡一下。
                val isMiniPlayerVisible = currentSong != null
                // 下拉/上滑联动底栏：内容向下滚动时收起成图标+迷你播放器。
                var chromeCollapsed by rememberSaveable { mutableStateOf(false) }
                val chromeCollapseConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            if (source == NestedScrollSource.UserInput) {
                                if (available.y < -6f && !chromeCollapsed) {
                                    chromeCollapsed = true
                                } else if (available.y > 6f && chromeCollapsed) {
                                    chromeCollapsed = false
                                }
                            }
                            return Offset.Zero
                        }

                        override suspend fun onPostFling(
                            consumed: Velocity,
                            available: Velocity
                        ): Velocity {
                            if (consumed.y < -800f || available.y < -800f) {
                                chromeCollapsed = true
                            } else if (consumed.y > 800f || available.y > 800f) {
                                chromeCollapsed = false
                            }
                            return Velocity.Zero
                        }
                    }
                }
                val isPlaybackControlPlaying by PlayerManager.playbackControlPlayingFlow.collectAsStateWithLifecycle()
                val isAudioRouteMuted by PlayerManager.audioRouteMuteSuppressedFlow
                    .collectAsStateWithLifecycle()
                val isPlaying by PlayerManager.isPlayingFlow.collectAsStateWithLifecycle()
                val usbPlaybackPreparing by PlayerManager.usbExclusivePlaybackPreparingFlow
                    .collectAsStateWithLifecycle()
                val isPlaybackWaiting = resolvePlaybackWaiting(
                    playbackRequested = isPlaybackControlPlaying,
                    isPlaying = isPlaying,
                    usbPlaybackPreparing = usbPlaybackPreparing
                )
                // The mini player lives inside the bottom bar, so the bar's own
                // measured height already reserves the space it occupies.
                val reservedMiniPlayerHeightDp = 0.dp

                // 播放器展开时页面层下沉：0.83 缩放 / 30dp 下移 / 0.24 压暗，
                // 与参考播放器同一口径；只作用于页面层，底部 chrome 不随之缩放。
                val playerRecedeProgress by animateFloatAsState(
                    targetValue = if (showNowPlaying && !nowPlayingDismissing) 1f else 0f,
                    animationSpec = tween(
                        durationMillis = if (showNowPlaying) 350 else 320,
                        easing = FastOutSlowInEasing
                    ),
                    label = "player_recede_progress"
                )
                val playerRecedeShape = remember { RoundedCornerShape(36.dp) }

                LaunchedEffect(currentRoute, showHomeTab, effectiveStartDestination) {
                    if (!showHomeTab && currentRoute == Destinations.Home.route) {
                        navController.navigate(effectiveStartDestination) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                CompositionLocalProvider(
                    LocalMiniPlayerHeight provides reservedMiniPlayerHeightDp,
                    LocalMeloxBackdrop provides if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        meloxBackdrop
                    } else {
                        null
                    }
                ) {
                    AppFeedbackHostEffect(snackbarHostState)
                    Scaffold(
                        containerColor = containerColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        snackbarHost = {
                            val miniH = LocalMiniPlayerHeight.current
                            LyraSnackbarHost(
                                hostState = snackbarHostState,
                                bottomPadding = miniH
                            )
                        },
                        bottomBar = {
                            val bottomBarVisibilityProgress by animateFloatAsState(
                                targetValue = if (showNowPlaying && !nowPlayingDismissing) 0f else 1f,
                                animationSpec = tween(
                                    durationMillis = if (showNowPlaying) 220 else 280,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "bottom_bar_visibility"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clipToBounds()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .graphicsLayer {
                                            // 只淡入淡出、不做位移：迷你播放器是用
                                            // boundsInWindow 量的，底栏一旦位移，量到的
                                            // 落点会跟着漂，播放页收起的封面就落不准。
                                            alpha = bottomBarVisibilityProgress
                                        }
                                ) {
                                    AnimatedVisibility(visible = offlineMode) {
                                        OfflineModeBottomBanner()
                                    }

                                    MeloxBottomChrome(
                                        items = bottomBarItems.map { (dest, icon) ->
                                            MeloxTabItem(
                                                route = dest.route,
                                                label = stringResource(dest.labelResId),
                                                icon = icon,
                                                selectedIcon = when (dest) {
                                                    Destinations.Home -> Icons.Filled.Home
                                                    Destinations.Library -> Icons.Filled.LibraryMusic
                                                    Destinations.Settings -> Icons.Filled.Settings
                                                    Destinations.Debug -> Icons.Filled.BugReport
                                                    else -> null
                                                }
                                            )
                                        },
                                        selectedRoute = if (searchDockActive) {
                                            null
                                        } else {
                                            selectedMainTabRoute
                                        },
                                        searchActive = searchDockActive,
                                        compactRoute = lastContentTabRoute,
                                        collapsed = chromeCollapsed,
                                        onSelect = { route ->
                                            context.performHapticFeedback()
                                            chromeCollapsed = false
                                            navigateToMainTab(route)
                                        },
                                        onSearchClick = {
                                            context.performHapticFeedback()
                                            chromeCollapsed = false
                                            navigateToMainTab(Destinations.Explore.route)
                                        },
                                        miniPlayerVisible = isMiniPlayerVisible,
                                        miniPlayer = { compactProgress ->
                                            val miniQueue by PlayerManager.currentQueueFlow
                                                .collectAsStateWithLifecycle()
                                            val miniQueueRevision by
                                                PlayerManager.currentQueueDisplayRevisionFlow
                                                    .collectAsStateWithLifecycle()
                                            // 用播放器自己的队标位置取邻居；按曲目 id 反查在换源/回填后会落空。
                                            val miniNeighbors = remember(
                                                miniQueue,
                                                miniQueueRevision,
                                                currentSong,
                                            ) {
                                                val snapshot = PlayerManager.currentQueueDisplaySnapshot()
                                                snapshot.items.getOrNull(snapshot.currentDisplayIndex - 1)?.song to
                                                    snapshot.items.getOrNull(snapshot.currentDisplayIndex + 1)?.song
                                            }
                                            val miniPreviousSong = miniNeighbors.first
                                            val miniNextSong = miniNeighbors.second
                                            val miniPreviousCover = rememberSongDisplayCoverUrl(miniPreviousSong)
                                            val miniNextCover = rememberSongDisplayCoverUrl(miniNextSong)
                                            val miniPreviousTrack = remember(miniPreviousSong, miniPreviousCover) {
                                                miniPreviousSong?.let { song ->
                                                    MeloxMiniTrack(
                                                        title = song.displayName(),
                                                        artist = song.displayArtist(),
                                                        coverUrl = miniPreviousCover
                                                    )
                                                }
                                            }
                                            val miniNextTrack = remember(miniNextSong, miniNextCover) {
                                                miniNextSong?.let { song ->
                                                    MeloxMiniTrack(
                                                        title = song.displayName(),
                                                        artist = song.displayArtist(),
                                                        coverUrl = miniNextCover
                                                    )
                                                }
                                            }
                                            AnimatedVisibility(
                                                visible = isMiniPlayerVisible,
                                                enter = if (nowPlayingDismissing) {
                                                    EnterTransition.None
                                                } else {
                                                    slideInVertically(
                                                        animationSpec = tween(
                                                            durationMillis = 220,
                                                            easing = FastOutSlowInEasing
                                                        ),
                                                        initialOffsetY = { it / 2 }
                                                    ) + fadeIn(animationSpec = tween(durationMillis = 180))
                                                },
                                                exit = slideOutVertically(
                                                    animationSpec = tween(
                                                        durationMillis = 180,
                                                        easing = FastOutSlowInEasing
                                                    ),
                                                    targetOffsetY = { it / 2 }
                                                ) + fadeOut(animationSpec = tween(durationMillis = 120))
                                            ) {
                                                MeloxMiniPlayer(
                                                    title = currentSong?.displayName()
                                                        ?: composeResources.getString(
                                                            R.string.nowplaying_no_playback
                                                        ),
                                                    artist = currentSong?.displayArtist() ?: "",
                                                    coverUrl = displayCoverUrl,
                                                    isPlaying = isPlaybackControlPlaying,
                                                    playPauseEnabled = !usbPlaybackPreparing,
                                                    onPlayPause = { PlayerManager.togglePlayPause() },
                                                    onNext = { PlayerManager.next() },
                                                    onPrevious = { PlayerManager.previous() },
                                                    previousTrack = miniPreviousTrack,
                                                    nextTrack = miniNextTrack,
                                                    onExpand = { showNowPlaying = true },
                                                    compactProgress = compactProgress,
                                                    onCoverBoundsChanged = { rect ->
                                                        miniPlayerCoverBounds = rect
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val bottomBarInset = innerPadding.calculateBottomPadding()
                            .coerceAtLeast(0.dp)
                        val bottomBarLayoutInsets = resolveBottomBarLayoutInsets(
                            baseBlurRequested = advancedGlassController.isBaseBlurRequested,
                            bottomBarInset = bottomBarInset,
                            reservedMiniPlayerHeight = reservedMiniPlayerHeightDp
                        )
                        CompositionLocalProvider(
                            LocalMiniPlayerHeight provides bottomBarLayoutInsets.screenBottomInset
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(
                                        bottom = bottomBarLayoutInsets.navContentBottomPadding
                                    )
                                    .clipToBounds()
                                    .then(
                                        if (
                                            Build.VERSION.SDK_INT >=
                                            Build.VERSION_CODES.TIRAMISU
                                        ) {
                                            Modifier.layerBackdrop(meloxBackdrop)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .graphicsLayer {
                                        val progress = playerRecedeProgress
                                        if (progress > 0.0005f) {
                                            scaleX = 1f - 0.17f * progress
                                            scaleY = 1f -
                                                (0.17f * size.width / size.height) * progress
                                            translationY = 30.dp.toPx() * progress
                                            transformOrigin = TransformOrigin.Center
                                            shape = playerRecedeShape
                                            clip = true
                                        } else {
                                            clip = false
                                        }
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        val progress = playerRecedeProgress
                                        if (progress > 0.0005f) {
                                            drawRect(
                                                Color.Black.copy(alpha = 0.24f * progress)
                                            )
                                        }
                                    }
                            ) {
                                // Keep the effect on a stable layer outside NavHost transitions
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(chromeCollapseConnection)
                                        .captureAdvancedGlassBackdrop(contentGlassBackdrop)
                                ) {
                                    MainTabLayerHost(
                                        selectedRoute = selectedMainTabRoute,
                                        transitionState = mainTabTransitionState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onSizeChanged { size ->
                                                if (size.height > 0) {
                                                    mainTabDetailContentHeightPx = size.height
                                                }
                                            }
                                            .offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = (
                                                        mainTabLayerTransform
                                                            .translationYFraction *
                                                            mainTabDetailContentHeightPx
                                                    ).roundToInt()
                                                )
                                            }
                                            .graphicsLayer {
                                                scaleX = mainTabLayerTransform.scale
                                                scaleY = mainTabLayerTransform.scale
                                                alpha = mainTabLayerTransform.alpha
                                                transformOrigin = TransformOrigin.Center
                                            }
                                            .zIndex(MAIN_TAB_LAYER_Z_INDEX),
                                        onVisibleGlassOwnersChanged = {
                                            visibleMainTabGlassOwners = it
                                        },
                                        content = { route ->
                                            RenderMainTabRoute(route)
                                        }
                                    )
                                    AdvancedGlassNavigationHandoff(
                                        enabled = shouldUseAdvancedGlassNavigationHandoff(
                                            visibleNavigationRoutes
                                        )
                                    ) {
                                        NavHost(
                                            navController = navController,
                                            startDestination = navHostStartDestination,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .zIndex(NAV_HOST_LAYER_Z_INDEX)
                                        ) {
                                composable(
                                    Destinations.Home.route,
                                    enterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {}

                                composable(
                                    route = Destinations.PlaylistDetail.route,
                                    arguments = listOf(navArgument("playlistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val playlistJson = backStackEntry.arguments?.getString("playlistJson")
                                    val playlist = navigationGson.fromJson(playlistJson, PlaylistSummary::class.java)
                                    RenderNavHostScene(
                                        Destinations.PlaylistDetail.route
                                    ) {
                                        NeteasePlaylistDetailScreen(
                                            playlist = playlist,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = { songs, index ->
                                                playSongsAndOpenNowPlaying(
                                                    songs = songs,
                                                    index = index,
                                                    sourceRoute = neteasePlaylistSourceRoute(playlist)
                                                )
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.NeteaseAlbumDetail.route,
                                    arguments = listOf(navArgument("playlistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val playlistJson = backStackEntry.arguments?.getString("playlistJson")
                                    val album = navigationGson.fromJson(playlistJson, AlbumSummary::class.java)
                                    RenderNavHostScene(
                                        Destinations.NeteaseAlbumDetail.route
                                    ) {
                                        NeteaseAlbumDetailScreen(
                                            album = album,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = { songs, index ->
                                                playSongsAndOpenNowPlaying(
                                                    songs = songs,
                                                    index = index,
                                                    sourceRoute = neteaseAlbumSourceRoute(album)
                                                )
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.NeteaseArtistDetail.route,
                                    arguments = listOf(navArgument("artistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val artistJson = backStackEntry.arguments?.getString("artistJson")
                                    val artist = navigationGson.fromJson(artistJson, NeteaseArtistSummary::class.java)
                                    RenderNavHostScene(
                                        Destinations.NeteaseArtistDetail.route
                                    ) {
                                        NeteaseArtistDetailScreen(
                                            artist = artist,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            offlineMode = offlineMode,
                                            onAlbumClick = { album ->
                                                navigateToNeteaseAlbum(album)
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.TencentArtistDetail.route,
                                    arguments = listOf(navArgument("artistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val artistJson = backStackEntry.arguments?.getString("artistJson")
                                    val artist = navigationGson.fromJson(
                                        artistJson,
                                        CustomChannelArtist::class.java
                                    )
                                    RenderNavHostScene(
                                        Destinations.TencentArtistDetail.route
                                    ) {
                                        TencentArtistDetailScreen(
                                            artist = artist,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.YouTubeMusicCreatorDetail.route,
                                    arguments = listOf(navArgument("creatorJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val creatorJson = backStackEntry.arguments
                                        ?.getString("creatorJson")
                                    val creator = navigationGson.fromJson(
                                        creatorJson,
                                        YouTubeMusicCreatorSummary::class.java
                                    )
                                    RenderNavHostScene(
                                        Destinations.YouTubeMusicCreatorDetail.route
                                    ) {
                                        YouTubeMusicCreatorNavigationScreen(
                                            creator = creator,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            onPlaylistClick = ::navigateToYouTubeMusicPlaylist,
                                            onCreatorClick = ::navigateToYouTubeMusicCreator,
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.YouTubeMusicPlaylistDetail.route,
                                    arguments = listOf(navArgument("playlistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val playlistJson = backStackEntry.arguments
                                        ?.getString("playlistJson")
                                    val playlist = navigationGson.fromJson(
                                        playlistJson,
                                        YouTubeMusicPlaylist::class.java
                                    )
                                    RenderNavHostScene(
                                        Destinations.YouTubeMusicPlaylistDetail.route
                                    ) {
                                        YouTubeMusicPlaylistDetailScreen(
                                            playlist = playlist,
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.BiliPlaylistDetail.route,
                                    arguments = listOf(navArgument("playlistJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val playlistJson = backStackEntry.arguments?.getString("playlistJson")
                                    val playlist = navigationGson.fromJson(playlistJson, BiliPlaylist::class.java)
                                    val suppressBiliPlaylistVisibilityTransition =
                                        shouldUseInstantBiliUploaderPlaylistTransition(
                                            initialRoute = navController.previousBackStackEntry
                                                ?.destination
                                                ?.route,
                                            targetRoute = Destinations.BiliPlaylistDetail.route
                                        )
                                    RenderNavHostScene(
                                        Destinations.BiliPlaylistDetail.route
                                    ) {
                                        BiliPlaylistDetailScreen(
                                            playlist = playlist,
                                            suppressVisibilityTransition =
                                                suppressBiliPlaylistVisibilityTransition,
                                            onBack = { navController.popBackStack() },
                                            onPlayAudio = { videos, index ->
                                                playBiliAudioAndOpenNowPlayingWithSource(
                                                    videos = videos,
                                                    index = index,
                                                    sourceRoute = biliPlaylistSourceRoute(playlist)
                                                )
                                            },
                                            onPlayParts = { videoInfo, index, coverUrl ->
                                                playBiliPartsAndOpenNowPlayingWithSource(
                                                    videoInfo = videoInfo,
                                                    index = index,
                                                    coverUrl = coverUrl,
                                                    sourceRoute = biliPlaylistSourceRoute(playlist)
                                                )
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.BiliUploaderDetail.route,
                                    arguments = listOf(navArgument("uploaderJson") {
                                        type = NavType.StringType
                                    }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val uploaderJson = backStackEntry.arguments
                                        ?.getString("uploaderJson")
                                    val uploader = navigationGson.fromJson(
                                        uploaderJson,
                                        BiliUploaderSummary::class.java
                                    )
                                    RenderNavHostScene(
                                        Destinations.BiliUploaderDetail.route
                                    ) {
                                        BiliUploaderDetailScreen(
                                            uploader = uploader,
                                            onBack = { navController.popBackStack() },
                                            onPlayAudio = { videos, index ->
                                                playBiliAudioAndOpenNowPlayingWithSource(
                                                    videos = videos,
                                                    index = index,
                                                    sourceRoute = biliUploaderSourceRoute(uploader)
                                                )
                                            },
                                            onPlayParts = { videoInfo, index, coverUrl ->
                                                playBiliPartsAndOpenNowPlayingWithSource(
                                                    videoInfo = videoInfo,
                                                    index = index,
                                                    coverUrl = coverUrl,
                                                    sourceRoute = biliUploaderSourceRoute(uploader)
                                                )
                                            },
                                            onContentClick = { playlist ->
                                                navController.navigate(
                                                    biliPlaylistSourceRoute(playlist)
                                                ) {
                                                    launchSingleTop = true
                                                }
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    Destinations.Explore.route,
                                    enterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {}

                                composable(
                                    Destinations.Library.route,
                                    enterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {}

                                composable(
                                    route = Destinations.LocalPlaylistDetail.route,
                                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val id = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                                    RenderNavHostScene(
                                        Destinations.LocalPlaylistDetail.route
                                    ) {
                                        LocalPlaylistDetailScreen(
                                            playlistId = id,
                                            onBack = { navController.popBackStack() },
                                            onDeleted = { navController.popBackStack() },
                                            onSongClick = { songs, index ->
                                                playSongsAndOpenNowPlaying(
                                                    songs = songs,
                                                    index = index,
                                                    sourceRoute = localPlaylistSourceRoute(id)
                                                )
                                            },
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.Recent.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.Recent.route) {
                                        RecentScreen(
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.Account.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.Account.route) {
                                        AccountScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenHistory = {
                                                navController.navigate(Destinations.Recent.route)
                                            },
                                            onOpenCloudPlaylists = {
                                                navController.navigate(
                                                    Destinations.CloudPlaylists.route
                                                )
                                            },
                                            onOpenFollowedArtists = {
                                                navController.navigate(
                                                    Destinations.FollowedArtists.route
                                                )
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.CloudPlaylists.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.CloudPlaylists.route) {
                                        CloudPlaylistsScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenPlaylist = { playlistId ->
                                                navController.navigate(
                                                    localPlaylistSourceRoute(playlistId)
                                                )
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.FollowedArtists.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.FollowedArtists.route) {
                                        FollowedArtistsScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenArtist = { artist ->
                                                if (artist.source == FAVORITE_SOURCE_TENCENT_ARTIST) {
                                                    navigateToTencentArtist(
                                                        CustomChannelArtist(
                                                            id = artist.browseId.orEmpty(),
                                                            name = artist.name,
                                                            coverUrl = artist.coverUrl
                                                        )
                                                    )
                                                } else {
                                                    navigateToNeteaseArtist(
                                                        NeteaseArtistSummary(
                                                            id = artist.id,
                                                            name = artist.name
                                                        )
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.PlaybackStats.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.PlaybackStats.route) {
                                        PlaybackStatsScreen(
                                            onBack = { navController.popBackStack() },
                                            onSongClick = ::playSongsAndOpenNowPlaying,
                                            offlineMode = offlineMode
                                        )
                                    }
                                }

                                composable(
                                    Destinations.Settings.route,
                                    enterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {}

                                composable(
                                    route = Destinations.DownloadManager.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    val downloadManagerListState = rememberSaveable(
                                        saver = LazyListState.Saver
                                    ) { LazyListState() }
                                    RenderNavHostScene(Destinations.DownloadManager.route) {
                                        DownloadManagerScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenDownloadProgress = {
                                                navController.navigate(
                                                    Destinations.DownloadProgress.route
                                                )
                                            },
                                            listState = downloadManagerListState,
                                            offlineMode = offlineMode,
                                            asTabRoot = true
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.DownloadProgress.route,
                                    enterTransition = {
                                        transparentDetailEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        transparentDetailExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        transparentDetailPopEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        transparentDetailPopExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    val downloadProgressListState = rememberSaveable(
                                        saver = LazyListState.Saver
                                    ) { LazyListState() }
                                    RenderNavHostScene(Destinations.DownloadProgress.route) {
                                        DownloadProgressScreen(
                                            onBack = { navController.popBackStack() },
                                            listState = downloadProgressListState
                                        )
                                    }
                                }

                                composable(
                                    Destinations.Debug.route,
                                    enterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        mainTabEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        mainTabExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {}
                                composable(
                                    route = Destinations.DebugUsbExclusive.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugUsbExclusive.route) {
                                        UsbExclusiveDebugScreen()
                                    }
                                }
                                composable(
                                    route = Destinations.DebugYouTube.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugYouTube.route) {
                                        YouTubeApiProbeScreen()
                                    }
                                }
                                composable(
                                    route = Destinations.DebugBili.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugBili.route) {
                                        BiliApiProbeScreen()
                                    }
                                }
                                composable(
                                    route = Destinations.DebugNetease.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugNetease.route) {
                                        NeteaseApiProbeScreen()
                                    }
                                }
                                composable(
                                    route = Destinations.DebugSearch.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugSearch.route) {
                                        SearchApiProbeScreen()
                                    }
                                }
                                composable(
                                    route = Destinations.DebugLogsList.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugLogsList.route) {
                                        LogListScreen(
                                            onBack = { navController.popBackStack() },
                                            onLogFileClick = { filePath ->
                                                navController.navigate(
                                                    Destinations.DebugLogViewer.createRoute(filePath)
                                                )
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.DebugCrashLogsList.route,
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) {
                                    RenderNavHostScene(Destinations.DebugCrashLogsList.route) {
                                        CrashLogListScreen(
                                            onBack = { navController.popBackStack() },
                                            onLogFileClick = { filePath ->
                                                navController.navigate(
                                                    Destinations.DebugLogViewer.createRoute(filePath)
                                                )
                                            }
                                        )
                                    }
                                }

                                composable(
                                    route = Destinations.DebugLogViewer.route,
                                    arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
                                    enterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    exitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    },
                                    popEnterTransition = {
                                        debugNavigationEnterTransition(coherentFeedbackEnabled)
                                    },
                                    popExitTransition = {
                                        debugNavigationExitTransition(coherentFeedbackEnabled)
                                    }
                                ) { backStackEntry ->
                                    val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
                                    RenderNavHostScene(Destinations.DebugLogViewer.route) {
                                        LogViewerScreen(
                                            filePath = filePath,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showNowPlaying,
                    enter = slideInVertically(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullHeight -> fullHeight }
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                    exit = slideOutVertically(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        targetOffsetY = { fullHeight -> fullHeight }
                    ) + fadeOut(animationSpec = tween(durationMillis = 180))
                ) {
                    DisposableEffect(Unit) {
                        latestOnNowPlayingVisibilityChanged(true)
                        onDispose {
                            latestOnNowPlayingVisibilityChanged(false)
                        }
                    }
                    val currentCoverUrl = playbackVisualCoverUrl

                    LyraTheme(
                        followSystemDark = false,
                        forceDark = true,
                        accentColorHex = themeSeedColor
                    ) {
                        BackHandler { showNowPlaying = false }

                        val nowPlayingQueue by PlayerManager.currentQueueFlow.collectAsStateWithLifecycle()
                        val nowPlayingCoverUrl = currentCoverUrl
                        // 播放页整层（模糊底 + 内容）随下拉一起位移，露出的上方区域
                        // 才是底下的首页/曲库页面；收起时整层淡出。
                        val nowPlayingSheetOffsetPx = remember { mutableFloatStateOf(0f) }
                        val nowPlayingSheetAlpha = remember { mutableFloatStateOf(1f) }
                        // 收起结束时这份状态是 0；再次打开页面前先复位，
                        // 否则退出动画还没跑完就点开迷你播放器会得到一张全透明的播放页。
                        LaunchedEffect(showNowPlaying) {
                            if (showNowPlaying) {
                                nowPlayingSheetOffsetPx.floatValue = 0f
                                nowPlayingSheetAlpha.floatValue = 1f
                            }
                        }

                        // 下拉收起时播放页背景换成玻璃：刚下拉是磨砂的，越往下越透亮。
                        // 进度只在绘制阶段读取，拖动过程不触发重组。
                        val glassPresenceRampPx = with(LocalDensity.current) { 32.dp.toPx() }
                        val glassClearStartPx = with(LocalDensity.current) { 8.dp.toPx() }
                        val glassClearRampPx = with(LocalDensity.current) { 240.dp.toPx() }
                        val nowPlayingGlassPresence = remember(glassPresenceRampPx) {
                            {
                                (nowPlayingSheetOffsetPx.floatValue / glassPresenceRampPx)
                                    .coerceIn(0f, 1f)
                            }
                        }
                        val nowPlayingGlassClarity = remember(
                            glassClearStartPx,
                            glassClearRampPx
                        ) {
                            {
                                (
                                    (nowPlayingSheetOffsetPx.floatValue - glassClearStartPx) /
                                        glassClearRampPx
                                    ).coerceIn(0f, 1f)
                            }
                        }
                        val glassBackdrop = if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            LocalView.current.isHardwareAccelerated
                        ) {
                            meloxBackdrop
                        } else {
                            null
                        }
                        val dragGlassModifier = remember(
                            glassBackdrop,
                            nowPlayingGlassPresence,
                            nowPlayingGlassClarity
                        ) {
                            Modifier
                                .fillMaxSize()
                                .meloxDragGlassVeil(
                                    backdrop = glassBackdrop,
                                    presence = nowPlayingGlassPresence,
                                    clarity = nowPlayingGlassClarity
                                )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blockUnderlyingTouches()
                        ) {
                            val coverBlurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            val hasCoverBlur =
                                coverBlurAvailable &&
                                    nowPlayingCoverBlurBackgroundEnabled &&
                                    !nowPlayingCoverUrl.isNullOrBlank()
                            val blurStrength = nowPlayingCoverBlurAmount.coerceIn(0f, 500f)
                            val effectiveBlurStrength = remember(nowPlayingCoverUrl, blurStrength) {
                                resolvedNowPlayingBlurStrength(
                                    coverUrl = nowPlayingCoverUrl,
                                    configuredBlurAmount = blurStrength
                                )
                            }
                            val blurImageSizePx = remember(nowPlayingCoverUrl) {
                                resolvedNowPlayingBlurImageSizePx(nowPlayingCoverUrl)
                            }
                            val shouldPreloadCoverBlurNeighbors = remember(nowPlayingCoverUrl) {
                                isRemoteImageSource(nowPlayingCoverUrl)
                            }
                            val imageLoader = remember(context) { Coil.imageLoader(context) }
                            var stableCoverUrl by remember { mutableStateOf<String?>(null) }
                            var stableBlurStrength by remember { mutableStateOf<Float?>(null) }
                            var coverBlurLoadFailed by remember { mutableStateOf(false) }
                            val coverBlurRequestKey = remember(nowPlayingCoverUrl, effectiveBlurStrength) {
                                if (nowPlayingCoverUrl.isNullOrBlank()) {
                                    null
                                } else {
                                    "nowplaying-blur:$nowPlayingCoverUrl:$effectiveBlurStrength"
                                }
                            }
                            val latestCoverBlurRequestKey by rememberUpdatedState(coverBlurRequestKey)
                            val currentQueueIndex = remember(nowPlayingQueue, currentSong) {
                                val current = currentSong ?: return@remember -1
                                nowPlayingQueue.indexOfFirst { it.sameIdentityAs(current) }
                            }
                            val preloadCoverUrls = remember(
                                nowPlayingQueue,
                                currentQueueIndex,
                                shouldPreloadCoverBlurNeighbors
                            ) {
                                if (currentQueueIndex == -1 || !shouldPreloadCoverBlurNeighbors) {
                                    emptyList()
                                } else {
                                    listOfNotNull(
                                        nowPlayingQueue.getOrNull(currentQueueIndex - 1)
                                            .resolveUiCoverSource(context),
                                        nowPlayingQueue.getOrNull(currentQueueIndex + 1)
                                            .resolveUiCoverSource(context)
                                    ).distinct()
                                }
                            }

                            LaunchedEffect(
                                hasCoverBlur,
                                effectiveBlurStrength,
                                blurImageSizePx,
                                preloadCoverUrls,
                                offlineMode
                            ) {
                                if (!hasCoverBlur || preloadCoverUrls.isEmpty()) return@LaunchedEffect
                                preloadCoverUrls.forEach { url ->
                                    imageLoader.enqueue(
                                        ImageRequest.Builder(context)
                                            .data(url)
                                            .allowHardware(false)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .size(blurImageSizePx)
                                            .precision(Precision.INEXACT)
                                            .memoryCacheKey("nowplaying-blur:$url:$effectiveBlurStrength")
                                            .diskCacheKey("nowplaying-blur:$url:$effectiveBlurStrength")
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .networkCachePolicy(
                                                if (offlineMode && isRemoteImageSource(url)) {
                                                    CachePolicy.DISABLED
                                                } else {
                                                    CachePolicy.ENABLED
                                                }
                                            )
                                            .transformations(
                                                if (effectiveBlurStrength > 0f) {
                                                    listOf(BlurTransformation(context, effectiveBlurStrength))
                                                } else {
                                                    emptyList()
                                                }
                                            )
                                            .build()
                                    )
                                }
                            }

                            LaunchedEffect(hasCoverBlur, nowPlayingCoverUrl, effectiveBlurStrength) {
                                if (!hasCoverBlur) {
                                    stableCoverUrl = null
                                    stableBlurStrength = null
                                    coverBlurLoadFailed = false
                                } else {
                                    coverBlurLoadFailed = false
                                }
                            }

                            val blurBackdropCoverUrl = stableCoverUrl ?: nowPlayingCoverUrl
                            val useCoverBlurBackground = hasCoverBlur && !coverBlurLoadFailed

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = nowPlayingSheetAlpha.floatValue }
                            ) {
                            // 背景层单独跟随下拉位移：播放页内容在 NowPlayingScreen 里
                            // 用同一个位移量自己走，封面飞行层留在位移之外。
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = nowPlayingSheetOffsetPx.floatValue
                                    }
                            ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = 1f - nowPlayingGlassPresence()
                                    }
                            ) {
                                if (!useCoverBlurBackground) {
                                    // 背景固定按暗色逻辑渲染
                                    NowPlayingAccentBackdrop(
                                        coverUrl = nowPlayingCoverUrl,
                                        isDark = true,
                                        refreshKey = coverArtRefreshToken,
                                        modifier = Modifier.fillMaxSize(),
                                        offlineMode = offlineMode
                                    )
                                }

                                if (useCoverBlurBackground) {
                                    // 先铺一层强调色背景, 避免首次加载和旋转重建时黑底闪烁
                                    NowPlayingAccentBackdrop(
                                        coverUrl = blurBackdropCoverUrl,
                                        isDark = true,
                                        refreshKey = coverArtRefreshToken,
                                        modifier = Modifier.fillMaxSize(),
                                        offlineMode = offlineMode
                                    )
                                    val shouldShowStable =
                                        stableCoverUrl != null &&
                                            (
                                                stableCoverUrl != nowPlayingCoverUrl ||
                                                    stableBlurStrength != effectiveBlurStrength
                                                )
                                    if (shouldShowStable) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(stableCoverUrl)
                                                .allowHardware(false)
                                                .bitmapConfig(Bitmap.Config.RGB_565)
                                                .size(blurImageSizePx)
                                                .precision(Precision.INEXACT)
                                                .memoryCacheKey("nowplaying-blur:$stableCoverUrl:$stableBlurStrength")
                                                .diskCacheKey("nowplaying-blur:$stableCoverUrl:$stableBlurStrength")
                                                .networkCachePolicy(
                                                    if (offlineMode && isRemoteImageSource(stableCoverUrl)) {
                                                        CachePolicy.DISABLED
                                                    } else {
                                                        CachePolicy.ENABLED
                                                    }
                                                )
                                                .transformations(
                                                    if ((stableBlurStrength ?: 0f) > 0f) {
                                                        listOf(BlurTransformation(context, stableBlurStrength ?: 0f))
                                                    } else {
                                                        emptyList()
                                                    }
                                                )
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(nowPlayingCoverUrl)
                                            .crossfade(NOW_PLAYING_BACKGROUND_CROSSFADE_MS)
                                            .allowHardware(false)
                                            .bitmapConfig(Bitmap.Config.RGB_565)
                                            .size(blurImageSizePx)
                                            .precision(Precision.INEXACT)
                                            .memoryCacheKey("nowplaying-blur:$nowPlayingCoverUrl:$effectiveBlurStrength")
                                            .diskCacheKey("nowplaying-blur:$nowPlayingCoverUrl:$effectiveBlurStrength")
                                            .networkCachePolicy(
                                                if (offlineMode && isRemoteImageSource(nowPlayingCoverUrl)) {
                                                    CachePolicy.DISABLED
                                                } else {
                                                    CachePolicy.ENABLED
                                                }
                                            )
                                            .transformations(
                                                if (effectiveBlurStrength > 0f) {
                                                    listOf(BlurTransformation(context, effectiveBlurStrength))
                                                } else {
                                                    emptyList()
                                                }
                                            )
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        onSuccess = {
                                            if (latestCoverBlurRequestKey == coverBlurRequestKey) {
                                                stableCoverUrl = nowPlayingCoverUrl
                                                stableBlurStrength = effectiveBlurStrength
                                                coverBlurLoadFailed = false
                                            }
                                        },
                                        onError = {
                                            if (latestCoverBlurRequestKey == coverBlurRequestKey) {
                                                coverBlurLoadFailed = stableCoverUrl.isNullOrBlank()
                                            }
                                        }
                                    )
                                    if (nowPlayingCoverBlurDarken > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = nowPlayingCoverBlurDarken.coerceIn(0f, 0.8f)))
                                        )
                                    }
                                } else if (effectiveDynamicBackgroundEnabled) {
                                    HyperBackground(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { alpha = 0.80f },
                                        isDark = true,
                                        coverUrl = nowPlayingCoverUrl,
                                        refreshKey = coverArtRefreshToken,
                                        offlineMode = offlineMode
                                    )
                                }
                            }

                            // 玻璃底随下拉一起位移：刚下拉磨砂遮住底页，越往下越透亮。
                            Box(modifier = dragGlassModifier)
                            }
                            }

                            CompositionLocalProvider(LocalMiniPlayerHeight provides 0.dp) {
                                val currentSourceRoute = currentPlaybackSourceRoute
                                NowPlayingScreen(
                                    onNavigateUp = {
                                        // 不要在这里把 sheetAlpha 复位成 1f：
                                        // 退出动画还在用这份状态渲染，复位会让播放页整屏闪现一下。
                                        showNowPlaying = false
                                        nowPlayingDismissing = false
                                    },
                                    onDismissStarted = { nowPlayingDismissing = true },
                                    onDismissCanceled = { nowPlayingDismissing = false },
                                    miniPlayerCoverBounds = miniPlayerCoverBounds,
                                    sheetOffsetPx = nowPlayingSheetOffsetPx,
                                    sheetAlpha = nowPlayingSheetAlpha,
                                    onOpenCurrentPlaybackSource = currentSourceRoute?.let { route ->
                                        {
                                            navigateToPlaybackSourceRoute(route)
                                        }
                                    },
                                    showLyricsScreen = showNowPlayingLyrics,
                                    onShowLyricsScreenChange = { showNowPlayingLyrics = it },
                                    onEnterAlbum = { album ->
                                        val shouldRestoreLyrics = showNowPlayingLyrics
                                        navigateToNeteaseAlbum(album) {
                                            if (shouldRestoreLyrics) {
                                                restoreLyricsAfterAlbumBack = true
                                            }
                                        }
                                    },
                                    onEnterArtist = ::navigateToNeteaseArtist,
                                    onEnterBiliUploader = ::navigateToBiliUploader,
                                    onEnterYouTubeCreator = ::navigateToYouTubeMusicCreator,
                                    onEnterTencentArtist = ::navigateToTencentArtist,
                                    lyricBlurEnabled = lyricBlurEnabled,
                                    lyricBlurAmount = lyricBlurAmount,
                                    lyricFontScales = lyricFontScales,
                                    onLyricFontScaleChange = { target, scale ->
                                        scope.launch { repo.setLyricFontScale(target, scale) }
                                    },
                                    advancedLyricsEnabled = advancedLyricsEnabled,
                                    showCoverSourceBadge = showCoverSourceBadge,
                                    showLyricTranslation = showLyricTranslation,
                                    showNowPlayingTitle = showNowPlayingTitle,
                                    offlineMode = offlineMode,
                                    resolvedCoverUrl = displayCoverUrl,
                                    visualCoverUrl = playbackVisualCoverUrl,
                                    playbackSongKey = currentSongKey
                                )
                            }
                        }
                    }
                }

                val revealOrigin = themeRevealOriginWindow
                val revealFallbackColor = themeRevealFallbackColorArgb?.let(::Color)
                if (revealOrigin != null && revealFallbackColor != null) {
                    val revealCaptureToken = themeRevealCaptureToken
                    ThemeRevealOverlay(
                        snapshot = themeRevealSnapshot,
                        fallbackColor = revealFallbackColor,
                        originInWindow = revealOrigin,
                        modifier = Modifier.fillMaxSize(),
                        startRadiusPx = themeRevealStartRadiusPx,
                        legacySnapshotDim = true,
                        durationMillis = THEME_REVEAL_DURATION_MILLIS,
                        onFinished = { finishThemeReveal(revealCaptureToken) }
                    )
                }

                pendingTrafficRiskDownloadRequest?.let { request ->
                    TrafficRiskDownloadDialog(
                        request = request,
                        onConfirm = {
                            pendingTrafficRiskDownloadRequest = null
                            GlobalDownloadManager.confirmTrafficRiskDownload(context, request)
                        },
                        onDismiss = {
                            pendingTrafficRiskDownloadRequest = null
                        }
                    )
                }

                pendingHigherQualityDownloadRequest?.let { request ->
                    HigherQualityLocalDownloadDialog(
                        request = request,
                        onConfirm = {
                            pendingHigherQualityDownloadRequest = null
                            GlobalDownloadManager.confirmHigherQualityLocalDownload(context, request)
                        },
                        onDismiss = {
                            pendingHigherQualityDownloadRequest = null
                        }
                    )
                }

                if (showUsbExclusiveBackgroundPermissionDialog) {
                    UsbExclusiveBackgroundPermissionDialog(
                        batteryOptimizationAllowed = context
                            .readBackgroundBehaviorAllowance()
                            .ignoringBatteryOptimizations,
                        onRequestBatteryOptimization = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            context.requestIgnoreBatteryOptimizationsCompat()
                        },
                        onOpenAppSettings = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            context.openAppBackgroundSettings()
                        },
                        onNeverShowAgain = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                            scope.launch {
                                repo.setUsbExclusiveBackgroundPermissionPromptSuppressed(true)
                            }
                        },
                        onDismiss = {
                            showUsbExclusiveBackgroundPermissionDialog = false
                        }
                    )
                }

                if (
                    shouldShowStartupGlassGate(
                        baseBlurEnabled = advancedGlassController.isBaseBlurEnabled,
                        gateReleased = startupGlassGateReleased,
                        backgroundEffectReady = startupBackgroundGlassReady,
                        contentEffectReady = startupContentGlassReady
                    )
                ) {
                    StartupGlassGateOverlay(
                        isDark = isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
