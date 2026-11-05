package com.lyra.player.data.profile

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 与 WebDAV 账号绑定的本地展示资料：昵称与头像。
 * 以账号名为 key，切换 WebDAV 账号时各自保留自己的资料。
 */
class UserProfileStore private constructor(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _profileFlow = MutableStateFlow(load())
    val profileFlow: StateFlow<Profile> = _profileFlow.asStateFlow()

    fun profileFor(username: String): Profile = load(username)

    fun setName(username: String, name: String) {
        prefs.edit { putString(key(username, KEY_NAME), name) }
        _profileFlow.value = load(username)
    }

    fun setAvatarUri(username: String, uri: String?) {
        prefs.edit {
            if (uri.isNullOrBlank()) remove(key(username, KEY_AVATAR)) else
                putString(key(username, KEY_AVATAR), uri)
        }
        _profileFlow.value = load(username)
    }

    private fun load(username: String? = null): Profile {
        val key = username ?: prefs.getString(KEY_LAST_USERNAME, null).orEmpty()
        return Profile(
            username = key,
            displayName = prefs.getString(this.key(key, KEY_NAME), null).orEmpty(),
            avatarUri = prefs.getString(this.key(key, KEY_AVATAR), null),
        )
    }

    fun rememberActiveAccount(username: String) {
        prefs.edit { putString(KEY_LAST_USERNAME, username) }
        _profileFlow.value = load(username)
    }

    fun clearActiveAccount() {
        prefs.edit { remove(KEY_LAST_USERNAME) }
        _profileFlow.value = Profile()
    }

    private fun key(username: String, field: String): String = "$field:$username"

    data class Profile(
        val username: String = "",
        val displayName: String = "",
        val avatarUri: String? = null,
    )

    companion object {
        private const val PREFS_NAME = "user_profile"
        private const val KEY_NAME = "display_name"
        private const val KEY_AVATAR = "avatar_uri"
        private const val KEY_LAST_USERNAME = "last_username"

        @Volatile
        private var instance: UserProfileStore? = null

        fun getInstance(context: Context): UserProfileStore {
            return instance ?: synchronized(this) {
                instance ?: UserProfileStore(context).also { instance = it }
            }
        }
    }
}
