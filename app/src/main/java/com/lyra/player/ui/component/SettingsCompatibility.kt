package com.lyra.player.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HsvPicker(
    onColorChanged: (String) -> Unit,
    initialHex: String = "0061A4"
) {
    com.lyra.player.ui.component.settings.HsvPicker(
        onColorChanged = onColorChanged,
        initialHex = initialHex
    )
}

@Composable
fun LanguageSettingItem(
    modifier: Modifier = Modifier,
    onLanguageChanged: (com.lyra.player.util.platform.LanguageManager.Language) -> Unit = {}
) {
    com.lyra.player.ui.component.settings.LanguageSettingItem(
        modifier = modifier,
        onLanguageChanged = onLanguageChanged
    )
}
