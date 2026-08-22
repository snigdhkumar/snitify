package com.snitrix.snitify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun isValidImageFormat(context: Context, uri: Uri): Boolean {
        val type = context.contentResolver.getType(uri) ?: ""
        return type.startsWith("image/") && (
            type.contains("jpeg") || 
            type.contains("jpg") || 
            type.contains("png") || 
            type.contains("webp")
        )
    }

    fun loadUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun cropToSquare(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    fun compressBitmapToBytes(bitmap: Bitmap, maxBytes: Int = 102400): ByteArray {
        var quality = 100
        var stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        while (stream.toByteArray().size > maxBytes && quality > 10) {
            quality -= 8
            stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        return stream.toByteArray()
    }

    fun savePlaylistLogo(context: Context, uri: Uri): String? {
        val bitmap = loadUriToBitmap(context, uri) ?: return null
        val squareBitmap = cropToSquare(bitmap)
        val compressedBytes = compressBitmapToBytes(squareBitmap)

        return try {
            val logosDir = File(context.filesDir, "playlist_logos")
            if (!logosDir.exists()) logosDir.mkdirs()

            val logoFile = File(logosDir, "logo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(logoFile).use { out ->
                out.write(compressedBytes)
            }
            logoFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
