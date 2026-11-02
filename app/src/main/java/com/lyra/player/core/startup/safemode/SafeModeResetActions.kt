package com.lyra.player.core.startup.safemode

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lyra.player.data.auth.bili.BiliCookieRepository
import com.lyra.player.data.auth.netease.NeteaseCookieRepository
import com.lyra.player.data.auth.web.clearAllWebViewLoginState
import com.lyra.player.data.auth.youtube.YouTubeAuthRepository
import com.lyra.player.data.settings.BootstrapSettingsSnapshot
import com.lyra.player.data.settings.PlaybackPreferenceSnapshot
import com.lyra.player.data.settings.ThemePreferenceSnapshot
import com.lyra.player.data.settings.dataStore
import com.lyra.player.data.settings.persistBootstrapSettingsSnapshot
import com.lyra.player.data.settings.persistPlaybackPreferenceSnapshot
import com.lyra.player.data.settings.persistThemePreferenceSnapshot

internal class SafeModeResetActions(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun clearAllCookiesAndLoginOptions() {
        withContext(Dispatchers.IO) {
            NeteaseCookieRepository(appContext).clear()
            BiliCookieRepository(appContext).clear()
            YouTubeAuthRepository(appContext).clear()
        }
        clearAllWebViewLoginState(appContext)
    }

    suspend fun resetAppSettings() {
        withContext(Dispatchers.IO) {
            appContext.dataStore.edit { prefs ->
                prefs.clear()
            }
            persistThemePreferenceSnapshot(appContext, ThemePreferenceSnapshot())
            persistBootstrapSettingsSnapshot(appContext, BootstrapSettingsSnapshot())
            persistPlaybackPreferenceSnapshot(appContext, PlaybackPreferenceSnapshot())
        }
    }
}
