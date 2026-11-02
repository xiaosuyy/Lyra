package com.lyra.player.ui.screen.tab.settings.component

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
 * File: com.lyra.player.ui.screen.tab.settings.component/SettingsCustomSourceSection
 * Created: 2026/9/24
 */

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.lyra.player.R
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsSwitch
import com.lyra.player.ui.screen.tab.settings.page.MiuixSettingsSectionIntro

/**
 * 自定义源（音源脚本）管理：开关、状态、导入和删除。
 */
@Composable
internal fun SettingsCustomSourceSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val states by CustomSourceManager.states.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val displayName = runCatching {
                context.contentResolver
                    .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
            }.getOrNull()
            val importedId = CustomSourceManager.importScript(context, uri, displayName)
            message = if (importedId != null) {
                context.getString(R.string.settings_custom_source_imported)
            } else {
                context.getString(R.string.settings_custom_source_import_failed)
            }
        }
    }

    LaunchedEffect(Unit) {
        CustomSourceManager.ensureLoaded(context)
    }

    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_custom_source_section),
        description = stringResource(R.string.settings_custom_source_section_desc)
    )

    if (states.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_custom_source_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    states.forEach { state ->
        ListItem(
            headlineContent = {
                Text(
                    text = if (state.version.isNotBlank()) {
                        "${state.name}  v${state.version}"
                    } else {
                        state.name
                    }
                )
            },
            supportingContent = {
                Text(
                    text = state.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Row {
                    TextButton(
                        onClick = {
                            scope.launch { CustomSourceManager.delete(context, state.id) }
                        }
                    ) {
                        Text(stringResource(R.string.settings_custom_source_delete))
                    }
                    MiuixSettingsSwitch(
                        checked = state.enabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                CustomSourceManager.setEnabled(context, state.id, checked)
                            }
                        }
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }

    Column(Modifier.fillMaxWidth()) {
        TextButton(
            onClick = {
                message = null
                importLauncher.launch(arrayOf("*/*"))
            }
        ) {
            Text(stringResource(R.string.settings_custom_source_import))
        }
        TextButton(
            onClick = {
                message = null
                scope.launch {
                    CustomSourceManager.reloadAll(context)
                    message = context.getString(R.string.settings_custom_source_reloaded)
                }
            }
        ) {
            Text(stringResource(R.string.settings_custom_source_reload))
        }
        message?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
