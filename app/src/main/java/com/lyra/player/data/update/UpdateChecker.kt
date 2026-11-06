package com.lyra.player.data.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

interface UpdateChecker {
    fun isConfigured(): Boolean

    suspend fun findLatestUpdate(): Result<AppUpdateInfo?>

    suspend fun downloadApk(
        update: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File>
}

internal fun installedAtMillis(context: Context): Long {
    val packageManager = context.packageManager
    return runCatching {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.lastUpdateTime
    }.getOrDefault(0L)
}
