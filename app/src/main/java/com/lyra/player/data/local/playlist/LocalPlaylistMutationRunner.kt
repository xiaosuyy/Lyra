package com.lyra.player.data.local.playlist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.lyra.player.core.logging.LyraLogger

suspend fun <T> runLocalPlaylistMutationSafely(
    operation: String,
    mutation: suspend () -> T
): Result<T> {
    return try {
        Result.success(mutation())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        LyraLogger.e("LocalPlaylistMutation", "$operation failed", error)
        Result.failure(error)
    }
}

fun <T> CoroutineScope.launchLocalPlaylistMutation(
    operation: String,
    onResult: (Result<T>) -> Unit = {},
    mutation: suspend () -> T
): Job {
    return launch {
        onResult(runLocalPlaylistMutationSafely(operation, mutation))
    }
}
