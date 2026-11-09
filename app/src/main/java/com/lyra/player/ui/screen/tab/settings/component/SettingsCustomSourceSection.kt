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
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.lyra.player.R
import com.lyra.player.core.customsource.CustomSourceLatency
import com.lyra.player.core.customsource.CustomSourceManager
import com.lyra.player.core.di.AppContainer
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsDialog
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsOutlinedButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsSwitch
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextButton
import com.lyra.player.ui.screen.tab.settings.miuix.MiuixSettingsTextField
import com.lyra.player.ui.screen.tab.settings.page.MiuixSettingsSectionIntro

/**
 * 自定义源（音源脚本）管理：开关、状态、导入和删除。
 */
@Composable
internal fun SettingsCustomSourceSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val states by CustomSourceManager.states.collectAsStateWithLifecycle()
    val latencies by CustomSourceManager.latencies.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var settlingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val rowHeights = remember { mutableStateMapOf<String, Int>() }
    val localOrder = remember { mutableStateListOf<String>() }

    val enabledStates = remember(states) {
        states.filter { it.enabled }.sortedBy { it.order }
    }
    val disabledStates = remember(states) {
        states.filterNot { it.enabled }.sortedBy { it.order }
    }
    val orderedEnabled = if (localOrder.isEmpty()) {
        enabledStates
    } else {
        localOrder.mapNotNull { id -> enabledStates.firstOrNull { it.id == id } }
    }

    LaunchedEffect(states) {
        if (draggingId != null) return@LaunchedEffect
        val ids = states.filter { it.enabled }.sortedBy { it.order }.map { it.id }
        if (localOrder.toList() != ids) {
            localOrder.clear()
            localOrder.addAll(ids)
        }
    }

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
        CustomSourceManager.refreshLatencies(context)
    }

    if (showImportDialog) {
        val isValidUrl = importUrl.trim().let {
            it.startsWith("http://") || it.startsWith("https://")
        }
        MiuixSettingsDialog(
            onDismissRequest = {
                if (!isImporting) showImportDialog = false
            },
            title = { Text(stringResource(R.string.settings_custom_source_import)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    MiuixSettingsOutlinedButton(
                        onClick = {
                            if (!isImporting) {
                                showImportDialog = false
                                message = null
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_custom_source_import_from_local))
                    }
                    Text(
                        text = stringResource(R.string.settings_custom_source_import_or),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MiuixSettingsTextField(
                        value = importUrl,
                        onValueChange = {
                            importUrl = it
                            importError = null
                        },
                        label = { Text(stringResource(R.string.settings_custom_source_import_url_label)) },
                        placeholder = {
                            Text(stringResource(R.string.settings_custom_source_import_url_placeholder))
                        },
                        singleLine = true,
                        enabled = !isImporting,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.settings_custom_source_import_url_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    importError?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                MiuixSettingsButton(
                    onClick = {
                        if (!isValidUrl) {
                            importError = context.getString(
                                R.string.settings_custom_source_import_invalid_url
                            )
                        } else {
                            isImporting = true
                            importError = null
                            scope.launch {
                                val importedId = CustomSourceManager.importScriptFromUrl(
                                    context = context,
                                    url = importUrl
                                )
                                isImporting = false
                                if (importedId != null) {
                                    message = context.getString(R.string.settings_custom_source_imported)
                                    showImportDialog = false
                                    importUrl = ""
                                } else {
                                    importError = context.getString(
                                        R.string.settings_custom_source_import_failed
                                    )
                                }
                            }
                        }
                    },
                    enabled = importUrl.isNotBlank() && !isImporting
                ) {
                    Text(
                        if (isImporting) {
                            stringResource(R.string.settings_custom_source_importing)
                        } else {
                            stringResource(R.string.settings_custom_source_import_confirm)
                        }
                    )
                }
            },
            dismissButton = {
                MiuixSettingsTextButton(
                    onClick = {
                        if (!isImporting) showImportDialog = false
                    },
                    enabled = !isImporting
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val localFallbackEnabled by AppContainer.settingsRepo
        .neteaseLocalSourceFallbackFlow
        .collectAsStateWithLifecycle(initialValue = false)

    ListItem(
        headlineContent = {
            Text(stringResource(R.string.settings_netease_local_source_fallback))
        },
        trailingContent = {
            MiuixSettingsSwitch(
                checked = localFallbackEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        AppContainer.settingsRepo.setNeteaseLocalSourceFallback(checked)
                    }
                }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    val gdApiEnabled by AppContainer.settingsRepo
        .gdApiEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)

    ListItem(
        headlineContent = {
            Text(stringResource(R.string.settings_gd_api_enabled))
        },
        trailingContent = {
            MiuixSettingsSwitch(
                checked = gdApiEnabled,
                onCheckedChange = { checked ->
                    scope.launch {
                        AppContainer.settingsRepo.setGdApiEnabled(checked)
                        if (checked) {
                            // GD API 与自定义音源脚本互斥：开启后自动关闭全部脚本。
                            CustomSourceManager.ensureLoaded(context)
                            val enabledIds = CustomSourceManager.states.value
                                .filter { it.enabled }
                                .map { it.id }
                            enabledIds.forEach { id ->
                                CustomSourceManager.setEnabled(context, id, false)
                            }
                        }
                    }
                }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    MiuixSettingsSectionIntro(
        title = stringResource(R.string.settings_custom_source_section),
    )

    if (states.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_custom_source_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (states.isNotEmpty()) {
        Text(
            text = stringResource(R.string.settings_custom_source_order_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }

    LookaheadScope {
        Column(Modifier.fillMaxWidth()) {
            (orderedEnabled + disabledStates).forEach { state ->
                key(state.id) {
                    val canDrag = state.enabled && orderedEnabled.size > 1
                    val isOffsetRow = draggingId == state.id || settlingId == state.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isOffsetRow) 1f else 0f)
                            .then(
                                if (isOffsetRow) {
                                    Modifier.offset { IntOffset(0, dragOffsetPx.roundToInt()) }
                                } else {
                                    Modifier.animateBounds(this@LookaheadScope)
                                }
                            )
                            .onGloballyPositioned { coordinates ->
                                rowHeights[state.id] = coordinates.size.height
                            }
                            .then(
                                if (canDrag) {
                                    Modifier.pointerInput(state.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                settlingId = null
                                                draggingId = state.id
                                                dragOffsetPx = 0f
                                            },
                                            onDragCancel = {
                                                draggingId = null
                                                dragOffsetPx = 0f
                                                localOrder.clear()
                                                localOrder.addAll(
                                                    CustomSourceManager.states.value
                                                        .filter { it.enabled }
                                                        .sortedBy { it.order }
                                                        .map { it.id }
                                                )
                                            },
                                            onDragEnd = {
                                                val activeId = draggingId ?: state.id
                                                val residual = dragOffsetPx
                                                draggingId = null
                                                scope.launch {
                                                    CustomSourceManager.reorder(
                                                        context,
                                                        localOrder.toList()
                                                    )
                                                }
                                                if (abs(residual) > 0.5f) {
                                                    settlingId = activeId
                                                    scope.launch {
                                                        animate(
                                                            initialValue = residual,
                                                            targetValue = 0f,
                                                            animationSpec = tween(
                                                                durationMillis = 180,
                                                                easing = FastOutSlowInEasing
                                                            )
                                                        ) { value, _ ->
                                                            if (settlingId == activeId &&
                                                                draggingId == null
                                                            ) {
                                                                dragOffsetPx = value
                                                            }
                                                        }
                                                        if (settlingId == activeId) {
                                                            dragOffsetPx = 0f
                                                            settlingId = null
                                                        }
                                                    }
                                                } else {
                                                    dragOffsetPx = 0f
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val activeId = draggingId ?: return@detectDragGesturesAfterLongPress
                                                dragOffsetPx += dragAmount.y
                                                var index = localOrder.indexOf(activeId)
                                                while (index >= 0) {
                                                    val ownHeight =
                                                        rowHeights[activeId]?.toFloat() ?: break
                                                    if (dragOffsetPx > 0f &&
                                                        index < localOrder.lastIndex
                                                    ) {
                                                        val otherId = localOrder[index + 1]
                                                        val otherHeight =
                                                            (rowHeights[otherId]
                                                                ?: ownHeight.toInt()).toFloat()
                                                        if (dragOffsetPx <=
                                                            (ownHeight + otherHeight) / 2f
                                                        ) {
                                                            break
                                                        }
                                                        localOrder[index] = otherId
                                                        localOrder[index + 1] = activeId
                                                        dragOffsetPx -= otherHeight
                                                        index++
                                                    } else if (dragOffsetPx < 0f && index > 0) {
                                                        val otherId = localOrder[index - 1]
                                                        val otherHeight =
                                                            (rowHeights[otherId]
                                                                ?: ownHeight.toInt()).toFloat()
                                                        if (-dragOffsetPx <=
                                                            (ownHeight + otherHeight) / 2f
                                                        ) {
                                                            break
                                                        }
                                                        localOrder[index] = otherId
                                                        localOrder[index - 1] = activeId
                                                        dragOffsetPx += otherHeight
                                                        index--
                                                    } else {
                                                        break
                                                    }
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val latency = latencies[state.id]
                                    if (state.enabled && latency != null) {
                                        Text(
                                            text = when (latency) {
                                                CustomSourceLatency.Checking -> stringResource(
                                                    R.string.settings_custom_source_latency_checking
                                                )
                                                is CustomSourceLatency.Responded -> stringResource(
                                                    R.string.settings_custom_source_latency_ms,
                                                    latency.millis
                                                )
                                                CustomSourceLatency.Timeout -> stringResource(
                                                    R.string.settings_custom_source_latency_timeout
                                                )
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (latency) {
                                                CustomSourceLatency.Checking ->
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                is CustomSourceLatency.Responded ->
                                                    if (latency.millis > 60L) {
                                                        MaterialTheme.colorScheme.error
                                                    } else {
                                                        LatencyGoodColor
                                                    }
                                                CustomSourceLatency.Timeout ->
                                                    MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                CustomSourceManager.delete(context, state.id)
                                            }
                                        }
                                    ) {
                                        Text(stringResource(R.string.settings_custom_source_delete))
                                    }
                                    MiuixSettingsSwitch(
                                        checked = state.enabled,
                                        onCheckedChange = { checked ->
                                            scope.launch {
                                                if (checked && gdApiEnabled) {
                                                    // 反向互斥：重新启用脚本时关闭 GD API。
                                                    AppContainer.settingsRepo
                                                        .setGdApiEnabled(false)
                                                }
                                                CustomSourceManager.setEnabled(
                                                    context,
                                                    state.id,
                                                    checked
                                                )
                                            }
                                        }
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        TextButton(
            onClick = {
                message = null
                importError = null
                showImportDialog = true
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

private val LatencyGoodColor = Color(0xFF34C759)
