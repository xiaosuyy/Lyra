package com.lyra.player.ui.component.song

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyra.player.core.di.AppContainer

@Composable
internal fun rememberShowSongArtist(): Boolean {
    val repo = remember { AppContainer.settingsRepo }
    val showArtist by repo.showSongArtistFlow.collectAsStateWithLifecycle(initialValue = true)
    return showArtist
}
