package com.lyra.player.data.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

/** 头像体积较大，同步前统一压成小尺寸 JPEG，避免同步文件膨胀。 */
object ProfileSyncCodec {
    private const val MAX_AVATAR_EDGE = 256
    private const val JPEG_QUALITY = 85

    fun encodeAvatar(path: String?): String {
        val file = path?.takeIf { it.isNotBlank() }?.let(::File) ?: return ""
        if (!file.isFile) return ""
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return ""

            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= MAX_AVATAR_EDGE ||
                bounds.outHeight / (sampleSize * 2) >= MAX_AVATAR_EDGE
            ) {
                sampleSize *= 2
            }
            val decoded = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            ) ?: return ""
            val scaled = scaleDown(decoded)
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            Base64.getEncoder().encodeToString(output.toByteArray())
        }.getOrDefault("")
    }

    fun decodeToFile(encoded: String, target: File): Boolean {
        if (encoded.isBlank()) return false
        return runCatching {
            val bytes = Base64.getDecoder().decode(encoded)
            target.parentFile?.mkdirs()
            target.outputStream().use { it.write(bytes) }
            true
        }.getOrDefault(false)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (longestEdge <= MAX_AVATAR_EDGE) return bitmap
        val scale = MAX_AVATAR_EDGE.toFloat() / longestEdge
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }
}
