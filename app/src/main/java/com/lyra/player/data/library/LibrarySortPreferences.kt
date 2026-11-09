package com.lyra.player.data.library

import android.content.Context
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class LibrarySortMode {
    ADDED_TIME,
    ALPHABETICAL
}

/**
 * 曲库「本地」「艺人」列表排序方式。设置页与曲库页共享同一实例，
 * 设置变化后列表无需重建即可跟随刷新。
 */
class LibrarySortPreferences private constructor(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _sortMode = MutableStateFlow(resolveSortMode(prefs.getString(KEY_SORT_MODE, null)))

    val sortMode: StateFlow<LibrarySortMode> = _sortMode.asStateFlow()

    fun setSortMode(mode: LibrarySortMode) {
        if (_sortMode.value == mode) return
        prefs.edit().putString(KEY_SORT_MODE, mode.name).apply()
        _sortMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "library_ui_preferences"
        private const val KEY_SORT_MODE = "library_sort_mode"

        @Volatile
        private var instance: LibrarySortPreferences? = null

        fun getInstance(context: Context): LibrarySortPreferences =
            instance ?: synchronized(this) {
                instance ?: LibrarySortPreferences(context.applicationContext).also { instance = it }
            }

        internal fun resolveSortMode(storageValue: String?): LibrarySortMode =
            LibrarySortMode.entries.firstOrNull { it.name == storageValue }
                ?: LibrarySortMode.ADDED_TIME
    }
}

internal const val LIBRARY_SORT_OTHER_GROUP = '#'

internal data class LibrarySortGroup<T>(
    val letter: Char,
    val items: List<T>
)

/** 取排序首字母：中文转拼音，A-Z 之外的归到 # */
internal fun librarySortLetter(name: String): Char {
    val first = name.trimStart().firstOrNull() ?: return LIBRARY_SORT_OTHER_GROUP
    val letter = if (Pinyin.isChinese(first)) {
        Pinyin.toPinyin(first).firstOrNull()
    } else {
        first
    } ?: return LIBRARY_SORT_OTHER_GROUP
    val upper = letter.uppercaseChar()
    return if (upper in 'A'..'Z') upper else LIBRARY_SORT_OTHER_GROUP
}

/** 组内排序键：逐字转拼音后转小写，保证中文按拼音、英文按字母排序 */
internal fun librarySortKey(name: String): String {
    val builder = StringBuilder(name.length)
    name.forEach { char ->
        if (Pinyin.isChinese(char)) {
            builder.append(Pinyin.toPinyin(char))
        } else {
            builder.append(char)
        }
    }
    return builder.toString().lowercase(Locale.ROOT)
}

internal fun <T> groupByLibrarySortLetter(
    items: List<T>,
    name: (T) -> String
): List<LibrarySortGroup<T>> {
    return items
        .groupBy { item -> librarySortLetter(name(item)) }
        .entries
        .sortedWith(compareBy({ it.key == LIBRARY_SORT_OTHER_GROUP }, { it.key }))
        .map { (letter, groupItems) ->
            LibrarySortGroup(
                letter = letter,
                items = groupItems.sortedWith(
                    compareBy<T> { item -> librarySortKey(name(item)) }
                        .thenBy { item -> name(item) }
                )
            )
        }
}
