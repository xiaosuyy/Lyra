package com.lyra.player.core.customsource

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
 * File: com.lyra.player.core.customsource/CustomSourceManager
 * Created: 2026/9/24
 */

import android.content.Context
import android.net.Uri
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "CustomSourceManager"

internal data class CustomSourceState(
    val id: String,
    val name: String,
    val version: String,
    val enabled: Boolean,
    val bundled: Boolean,
    val loaded: Boolean,
    val loading: Boolean,
    val declarations: List<CustomSourceDeclaration>,
    val error: String?
) {
    val summary: String
        get() = when {
            !enabled -> "已停用"
            loading && !loaded -> "正在加载…"
            loaded -> declarations.joinToString("、") { declaration ->
                declaration.sourceId + " " + declaration.qualitys.firstOrNull().orEmpty()
            }.ifBlank { "已加载，但未声明音源" }
            error.isNullOrBlank() -> "未加载"
            else -> error
        }
}

/**
 * 自定义源（音源脚本）管理器：负责脚本加载、状态维护和直链解析。
 */
internal object CustomSourceManager {
    private val runtimes = ConcurrentHashMap<String, CustomSourceRuntime>()
    private val loadLock = Mutex()

    private val _states = MutableStateFlow<List<CustomSourceState>>(emptyList())
    val states: StateFlow<List<CustomSourceState>> = _states.asStateFlow()

    private val _declaredSourceIds = MutableStateFlow<Set<String>>(emptySet())
    val declaredSourceIds: StateFlow<Set<String>> = _declaredSourceIds.asStateFlow()

    /**
     * 安装内置音源并按需加载所有已启用的脚本。
     */
    suspend fun ensureLoaded(context: Context, forceReload: Boolean = false): List<CustomSourceState> {
        CustomSourceStore.installBundledSources(context)
        return loadLock.withLock {
            val scripts = CustomSourceStore.list(context)
            LyraLogger.i(TAG, "自定义源脚本数量: ${scripts.size}")
            publish(scripts)
            val enabledIds = scripts.filter { it.enabled }.mapTo(HashSet()) { it.id }
            if (forceReload) {
                runtimes.keys.filterNot { it in enabledIds }.forEach { id ->
                    runtimes.remove(id)?.release()
                }
            }
            var errors = emptyMap<String, String>()
            scripts.filter { it.enabled }.forEach { script ->
                val existing = runtimes[script.id]
                if (forceReload || existing == null || !existing.isReady) {
                    existing?.release()
                    runtimes.remove(script.id)
                    val text = CustomSourceStore.readScriptText(context, script.id)
                    if (text.isNullOrBlank()) {
                        LyraLogger.w(TAG, "音源脚本为空: ${script.id}")
                        errors = errors + (script.id to "脚本文件为空")
                        return@forEach
                    }
                    val runtime = CustomSourceRuntime(
                        context = context.applicationContext,
                        httpClient = AppContainer.sharedOkHttpClient,
                        scriptInfoJson = buildScriptInfoJson(script, text)
                    )
                    val state = withContext(Dispatchers.Main) { runtime.load(text) }
                    LyraLogger.i(
                        TAG,
                        "加载音源脚本 ${script.name}: loaded=${state.loaded}, " +
                            "sources=${state.declarations.joinToString(",") { it.sourceId }}, " +
                            "error=${state.error}"
                    )
                    if (!state.loaded) {
                        errors = errors + (script.id to (state.error ?: "加载失败"))
                    }
                    runtimes[script.id] = runtime
                    publish(scripts, errors)
                }
            }
            runtimes.keys.filterNot { it in enabledIds }.forEach { id ->
                runtimes.remove(id)?.release()
            }
            publish(scripts, errors)
        }
    }

    suspend fun setEnabled(context: Context, id: String, enabled: Boolean) {
        CustomSourceStore.setEnabled(context, id, enabled)
        if (!enabled) {
            runtimes.remove(id)?.release()
        }
        ensureLoaded(context, forceReload = false)
    }

    suspend fun importScript(context: Context, uri: Uri, displayName: String?): String? {
        val id = CustomSourceStore.importScript(context, uri, displayName)
        if (id != null) {
            ensureLoaded(context, forceReload = false)
        }
        return id
    }

    suspend fun importScriptText(
        context: Context,
        text: String,
        displayName: String?
    ): String? {
        val id = CustomSourceStore.importScriptText(context, text, displayName)
        if (id != null) {
            ensureLoaded(context, forceReload = false)
        }
        return id
    }

    /**
     * 从网络地址下载并导入音源脚本。
     */
    suspend fun importScriptFromUrl(context: Context, url: String): String? {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return null
        }
        val text = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(trimmedUrl).build()
                AppContainer.sharedOkHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body.string() else null
                }
            }.getOrNull()
        } ?: return null
        if (text.isBlank()) return null
        val fileName = trimmedUrl
            .substringAfterLast('/')
            .substringBefore('?')
            .substringBefore('#')
            .ifBlank { "custom_source" }
        return importScriptText(context = context, text = text, displayName = fileName)
    }

    suspend fun delete(context: Context, id: String) {
        runtimes.remove(id)?.release()
        CustomSourceStore.delete(context, id)
        ensureLoaded(context, forceReload = false)
    }

    suspend fun reload(context: Context, id: String) {
        runtimes.remove(id)?.release()
        CustomSourceStore.installBundledSources(context)
        ensureLoaded(context, forceReload = false)
    }

    /**
     * 重新加载全部已安装且启用的脚本。
     *
     * 不依赖任何硬编码的音源 id：先释放当前所有运行时，再由 [ensureLoaded] 按
     * [CustomSourceStore] 中实际存在的脚本重建，因此内置脚本改名或增删后依然可用。
     */
    suspend fun reloadAll(context: Context) {
        runtimes.values.forEach { it.release() }
        runtimes.clear()
        _declaredSourceIds.value = emptySet()
        ensureLoaded(context, forceReload = true)
    }

    /**
     * 让自定义源解析歌曲直链，优先使用声明了该音源的脚本。
     */
    suspend fun resolveMusicUrl(
        context: Context,
        sourceId: String,
        quality: String,
        musicInfo: JSONObject
    ): String? {
        if (sourceId.isBlank()) return null
        ensureLoaded(context)
        val candidates = runtimes.entries.filter { (_, runtime) ->
            runtime.isReady && sourceId in runtime.sourceIds
        }
        for ((id, runtime) in candidates) {
            val url = runtime.resolveMusicUrl(sourceId, quality, musicInfo)
            if (!url.isNullOrBlank()) {
                LyraLogger.d(TAG, "自定义源 $id 解析成功: source=$sourceId quality=$quality")
                return url
            }
        }
        return null
    }

    /**
     * 该音源可用的最高音质（按 无损 > 320k > 128k 取），没有可用音源时返回 null。
     */
    suspend fun preferredQuality(context: Context, sourceId: String): String? {
        ensureLoaded(context)
        val qualities = runtimes.values
            .filter { it.isReady && sourceId in it.sourceIds }
            .flatMap { runtime ->
                runtime.declaredSources
                    .filter { it.sourceId == sourceId }
                    .flatMap { it.qualitys }
            }
        if (qualities.isEmpty()) return null
        return preferredCustomSourceQuality(qualities.toSet())
    }

    internal fun preferredCustomSourceQuality(available: Set<String>): String? {
        if (available.isEmpty()) return null
        return customSourceQualityPreference.firstOrNull { it in available }
            ?: available.first()
    }

    /**
     * 该音源可用音质，按"优先尝试"的顺序返回（最可能播成功的排在前面）。
     */
    suspend fun availableQualities(context: Context, sourceId: String): List<String> {
        ensureLoaded(context)
        val qualities = runtimes.values
            .filter { it.isReady && sourceId in it.sourceIds }
            .flatMap { runtime ->
                runtime.declaredSources
                    .filter { it.sourceId == sourceId }
                    .flatMap { it.qualitys }
            }
            .toSet()
        if (qualities.isEmpty()) {
            return emptyList()
        }
        val ordered = customSourceQualityPreference.filter { it in qualities } +
            qualities.filterNot { it in customSourceQualityPreference }
        return ordered
    }

    fun releaseAll() {
        runtimes.values.forEach { it.release() }
        runtimes.clear()
        _declaredSourceIds.value = emptySet()
    }

    private fun publish(
        scripts: List<CustomSourceScript>,
        errors: Map<String, String> = emptyMap()
    ): List<CustomSourceState> {
        val nextStates = scripts.map { script ->
            val runtime = runtimes[script.id]
            CustomSourceState(
                id = script.id,
                name = script.name,
                version = script.version,
                enabled = script.enabled,
                bundled = script.bundled,
                loaded = runtime?.isReady == true,
                loading = script.enabled && runtime == null,
                declarations = runtime?.declaredSources.orEmpty(),
                error = errors[script.id]
            )
        }
        _states.value = nextStates
        _declaredSourceIds.value = nextStates
            .filter { it.enabled && it.loaded }
            .flatMap { state -> state.declarations.map { it.sourceId } }
            .toSet()
        return nextStates
    }

    private fun buildScriptInfoJson(script: CustomSourceScript, rawScript: String): String {
        return JSONObject()
            .put("name", script.name)
            .put("description", script.name)
            .put("version", script.version)
            .put("author", "")
            .put("homepage", "")
            .put("rawScript", rawScript.take(4096))
            .toString()
    }
}
