package moe.ouom.neriplayer.core.customsource

/*
 * NeriPlayer - A unified Android player for streaming music and videos from multiple online platforms.
 * Copyright (C) 2025-2025 NeriPlayer developers
 * https://github.com/cwuom/NeriPlayer
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * File: moe.ouom.neriplayer.core.customsource/CustomSourceStore
 * Created: 2026/9/24
 */

import android.content.Context
import android.net.Uri
import java.io.File
import moe.ouom.neriplayer.core.logging.NPLogger
import org.json.JSONObject

private const val TAG = "CustomSourceStore"
private const val SOURCE_DIR_NAME = "custom_sources"
private const val META_FILE_NAME = "meta.json"
private const val BUNDLED_ASSET_DIR = "customsource/sources"

internal data class CustomSourceScript(
    val id: String,
    val name: String,
    val version: String,
    val enabled: Boolean,
    val bundled: Boolean,
    val file: File
)

/**
 * 自定义源脚本的存放与元数据管理。
 *
 * 脚本放在 filesDir 下的 custom_sources 目录，是否启用、来源（内置/导入）记录在同目录的 meta.json。
 */
internal object CustomSourceStore {
    private fun sourceDir(context: Context): File =
        File(context.filesDir, SOURCE_DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun metaFile(context: Context): File = File(sourceDir(context), META_FILE_NAME)

    private fun readMeta(context: Context): JSONObject {
        val file = metaFile(context)
        if (!file.isFile) return JSONObject()
        return runCatching { JSONObject(file.readText()) }.getOrElse { JSONObject() }
    }

    private fun writeMeta(context: Context, meta: JSONObject) {
        runCatching { metaFile(context).writeText(meta.toString()) }
            .onFailure { NPLogger.w(TAG, "写入自定义源元数据失败: ${it.message}") }
    }

    fun list(context: Context): List<CustomSourceScript> {
        val meta = readMeta(context).optJSONObject("scripts") ?: JSONObject()
        val files = sourceDir(context).listFiles { file -> file.isFile && file.name.endsWith(".js") }
            ?.sortedBy { it.name }
            .orEmpty()
        return files.map { file ->
            val id = file.nameWithoutExtension
            val entry = meta.optJSONObject(id)
            CustomSourceScript(
                id = id,
                name = entry?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: parseHeaderValue(file.readText(), "name")
                    ?: id,
                version = entry?.optString("version")?.takeIf { it.isNotBlank() }
                    ?: parseHeaderValue(file.readText(), "version").orEmpty(),
                enabled = entry?.optBoolean("enabled", true) ?: true,
                bundled = entry?.optBoolean("bundled", false) ?: false,
                file = file
            )
        }
    }

    fun readScriptText(context: Context, id: String): String? {
        val file = File(sourceDir(context), "$id.js")
        if (!file.isFile) return null
        return runCatching { file.readText() }.getOrNull()
    }

    fun setEnabled(context: Context, id: String, enabled: Boolean) {
        val meta = readMeta(context)
        val scripts = meta.optJSONObject("scripts") ?: JSONObject()
        val entry = scripts.optJSONObject(id) ?: JSONObject()
        entry.put("enabled", enabled)
        scripts.put(id, entry)
        meta.put("scripts", scripts)
        writeMeta(context, meta)
    }

    fun delete(context: Context, id: String): Boolean {
        val file = File(sourceDir(context), "$id.js")
        val removed = file.delete()
        val meta = readMeta(context)
        val scripts = meta.optJSONObject("scripts") ?: JSONObject()
        scripts.remove(id)
        meta.put("scripts", scripts)
        writeMeta(context, meta)
        return removed
    }

    /**
     * 首次使用时把 `assets/customsource/sources` 下的内置音源脚本复制到应用私有目录并默认启用。
     *
     * 内置脚本随应用更新自动同步；用户导入的脚本不受影响。
     */
    fun installBundledSources(context: Context) {
        val assets = runCatching { context.assets.list(BUNDLED_ASSET_DIR) }.getOrNull().orEmpty()
        NPLogger.i(TAG, "内置音源资源: " + assets.joinToString(","))
        if (assets.isEmpty()) return
        val meta = readMeta(context)
        val scripts = meta.optJSONObject("scripts") ?: JSONObject()
        var changed = false
        assets.filter { it.endsWith(".js") }.forEach { fileName ->
            val id = fileName.removeSuffix(".js")
            val target = File(sourceDir(context), fileName)
            val text = runCatching {
                context.assets.open("$BUNDLED_ASSET_DIR/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
            }.getOrNull() ?: return@forEach
            // 内置音源随应用更新，每次都同步最新内容（用户自己导入的脚本不受影响）
            if (!target.isFile || target.readText() != text) {
                runCatching { target.writeText(text) }
                    .onSuccess { NPLogger.i(TAG, "已同步内置音源: ${target.name}") }
                    .onFailure { NPLogger.w(TAG, "写入内置音源失败: ${it.message}") }
            }
            val entry = scripts.optJSONObject(id) ?: JSONObject()
            if (entry.optString("name").isBlank()) {
                entry.put("name", parseHeaderValue(text, "name") ?: id)
            }
            if (entry.optString("version").isBlank()) {
                entry.put("version", parseHeaderValue(text, "version").orEmpty())
            }
            if (!entry.has("enabled")) {
                entry.put("enabled", true)
            }
            entry.put("bundled", true)
            scripts.put(id, entry)
            changed = true
        }
        if (changed) {
            meta.put("scripts", scripts)
            writeMeta(context, meta)
        }
        pruneRemovedBundledSources(context, assets.map { it.removeSuffix(".js") }.toSet())
    }

    /**
     * 清理已经从应用里移除的内置音源（用户自己导入的脚本不动）。
     */
    private fun pruneRemovedBundledSources(context: Context, availableBundleIds: Set<String>) {
        val meta = readMeta(context)
        val scripts = meta.optJSONObject("scripts") ?: return
        val staleIds = scripts.keys().asSequence()
            .filter { id -> scripts.optJSONObject(id)?.optBoolean("bundled", false) == true }
            .filterNot { id -> id in availableBundleIds }
            .toList()
        if (staleIds.isEmpty()) {
            return
        }
        staleIds.forEach { id ->
            File(sourceDir(context), "$id.js").delete()
            scripts.remove(id)
            NPLogger.i(TAG, "已移除内置音源: $id")
        }
        meta.put("scripts", scripts)
        writeMeta(context, meta)
    }

    /**
     * 从文件选择器导入一个 .js 音源脚本。
     */
    fun importScript(context: Context, uri: Uri, displayName: String?): String? {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return null
        if (text.isBlank()) return null
        val baseId = (displayName ?: uri.lastPathSegment ?: "custom_source")
            .substringAfterLast('/')
            .removeSuffix(".js")
            .replace(Regex("[^0-9A-Za-z_\\u4e00-\\u9fff-]"), "_")
            .ifBlank { "custom_source" }
        var id = baseId
        var index = 1
        while (File(sourceDir(context), "$id.js").exists()) {
            id = "${baseId}_$index"
            index++
        }
        val target = File(sourceDir(context), "$id.js")
        runCatching { target.writeText(text) }.getOrElse {
            NPLogger.w(TAG, "写入导入音源失败: ${it.message}")
            return null
        }
        val meta = readMeta(context)
        val scripts = meta.optJSONObject("scripts") ?: JSONObject()
        scripts.put(
            id,
            JSONObject()
                .put("name", parseHeaderValue(text, "name") ?: id)
                .put("version", parseHeaderValue(text, "version").orEmpty())
                .put("enabled", true)
                .put("bundled", false)
        )
        meta.put("scripts", scripts)
        writeMeta(context, meta)
        return id
    }

    /**
     * 读取脚本头部的 @name / @version 等信息。
     */
    internal fun parseHeaderValue(scriptText: String, key: String): String? {
        val regex = Regex("@$key\\s+([^\\r\\n*]+)")
        return regex.find(scriptText)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
