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
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.lyra.player.core.di.AppContainer
import com.lyra.player.core.logging.LyraLogger
import okhttp3.Request
import org.json.JSONObject

private const val TAG = "CustomSourceManager"
private const val LATENCY_PROBE_TIMEOUT_MS = 6_000L

internal data class CustomSourceState(
    val id: String,
    val name: String,
    val version: String,
    val enabled: Boolean,
    val bundled: Boolean,
    val order: Int,
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
 * 单个自定义源的探测延迟。
 */
internal sealed interface CustomSourceLatency {
    data object Checking : CustomSourceLatency
    data class Responded(val millis: Long) : CustomSourceLatency
    data object Timeout : CustomSourceLatency
}

/**
 * 自定义源（音源脚本）管理器：负责脚本加载、状态维护和直链解析。
 */
internal object CustomSourceManager {
    private val runtimes = ConcurrentHashMap<String, CustomSourceRuntime>()
    private val loadLock = Mutex()

    private val _states = MutableStateFlow<List<CustomSourceState>>(emptyList())
    val states: StateFlow<List<CustomSourceState>> = _states.asStateFlow()

    private val _latencies = MutableStateFlow<Map<String, CustomSourceLatency>>(emptyMap())
    val latencies: StateFlow<Map<String, CustomSourceLatency>> = _latencies.asStateFlow()

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
        _latencies.value = _latencies.value - id
        ensureLoaded(context, forceReload = false)
        if (enabled) {
            _states.value.firstOrNull { it.id == id }?.let { state ->
                probeLatencyInternal(state)
            }
        }
    }

    /**
     * 保存启用音源的检索顺序：越靠前越先尝试解析。
     */
    suspend fun reorder(context: Context, orderedIds: List<String>) {
        CustomSourceStore.reorder(context, orderedIds)
        publish(CustomSourceStore.list(context))
        LyraLogger.i(TAG, "自定义源顺序已更新: " + orderedIds.joinToString(","))
    }

    /**
     * 重新探测所有已启用音源的响应延迟。
     */
    suspend fun refreshLatencies(context: Context) {
        ensureLoaded(context)
        val targets = _states.value.filter { it.enabled && it.loaded }
        _latencies.value = targets.associate { it.id to CustomSourceLatency.Checking }
        coroutineScope {
            targets.forEach { state ->
                launch { probeLatencyInternal(state) }
            }
        }
    }

    private suspend fun probeLatencyInternal(state: CustomSourceState) {
        val sourceId = state.declarations.firstOrNull()?.sourceId
        val url = sourceId?.let(::latencyProbeUrl)
        if (url == null) {
            _latencies.value = _latencies.value - state.id
            return
        }
        _latencies.value = _latencies.value + (state.id to CustomSourceLatency.Checking)
        val startedAt = SystemClock.elapsedRealtime()
        val reachable = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).get().build()
                AppContainer.sharedOkHttpClient.newBuilder()
                    .callTimeout(LATENCY_PROBE_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .build()
                    .newCall(request)
                    .execute()
                    .use { it.close() }
                true
            }.getOrDefault(false)
        }
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val latency = if (reachable) {
            CustomSourceLatency.Responded(elapsed)
        } else {
            CustomSourceLatency.Timeout
        }
        _latencies.value = _latencies.value + (state.id to latency)
        LyraLogger.i(TAG, "自定义源延迟探测 ${state.name}: $latency (${sourceId})")
    }

    /**
     * 用对应平台的一个轻量地址测网络往返，避免为探测而去解析真实歌曲。
     */
    private fun latencyProbeUrl(sourceId: String): String? = when (sourceId) {
        "wy" -> "https://music.163.com/favicon.ico"
        "tx" -> "https://u.y.qq.com/favicon.ico"
        "kw", "kg" -> "https://www.kugou.com/favicon.ico"
        "mg" -> "https://music.migu.cn/favicon.ico"
        else -> null
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
        _latencies.value = _latencies.value - id
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
        _latencies.value = emptyMap()
        ensureLoaded(context, forceReload = true)
        refreshLatencies(context)
    }

    /**
     * 让自定义源解析歌曲直链。
     *
     * 所有「已启用脚本 × 待尝试音质」的组合同时并发，谁先返回可用直链就用谁；
     * 串行逐个等待会让最慢的脚本把播放硬生生拖住十几秒。
     */
    suspend fun resolveMusicUrl(
        context: Context,
        sourceId: String,
        qualities: List<String>,
        musicInfo: JSONObject
    ): String? {
        if (sourceId.isBlank() || qualities.isEmpty()) return null
        ensureLoaded(context)
        val orderById = _states.value.associate { it.id to it.order }
        val candidates = runtimes.entries
            .filter { (_, runtime) ->
                runtime.isReady && sourceId in runtime.sourceIds
            }
            .sortedBy { (id, _) -> orderById[id] ?: Int.MAX_VALUE }
        if (candidates.isEmpty()) {
            LyraLogger.w(TAG, "没有可用的音源脚本运行环境: source=$sourceId")
            return null
        }
        return coroutineScope {
            val tasks = candidates.flatMap { (id, runtime) ->
                qualities.map { quality ->
                    async {
                        val startedAt = SystemClock.elapsedRealtime()
                        val url = runCatching {
                            runtime.resolveMusicUrl(sourceId, quality, musicInfo)
                        }.getOrElse { error ->
                            if (error is CancellationException) throw error
                            null
                        }
                        val elapsed = SystemClock.elapsedRealtime() - startedAt
                        if (url.isNullOrBlank()) {
                            LyraLogger.i(
                                TAG,
                                "自定义源 $id 未解析出直链: source=$sourceId quality=$quality, " +
                                    "耗时=${elapsed}ms"
                            )
                        } else {
                            LyraLogger.d(
                                TAG,
                                "自定义源 $id 解析成功: source=$sourceId quality=$quality, " +
                                    "耗时=${elapsed}ms"
                            )
                        }
                        url
                    }
                }
            }
            val pending = tasks.toMutableList()
            try {
                while (pending.isNotEmpty()) {
                    val (completed, url) = select {
                        pending.forEach { task -> task.onAwait { task to it } }
                    }
                    pending.remove(completed)
                    if (!url.isNullOrBlank()) return@coroutineScope url
                }
                null
            } finally {
                tasks.forEach { it.cancel() }
            }
        }
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
                order = script.order,
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
