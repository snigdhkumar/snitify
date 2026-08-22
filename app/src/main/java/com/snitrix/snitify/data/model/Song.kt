package com.snitrix.snitify.data.model

import androidx.compose.runtime.Immutable
import com.snitrix.snitify.R

@Immutable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int, // in seconds
    val coverUrl: String
) {
    /** Returns a Coil-loadable model: local file, content URI, HTTP URL, or defaultthumbnail fallback */
    val finalCover: Any
        get() = when {
            coverUrl.isNotBlank() && coverUrl != "null" && coverUrl != "defaultthumbnail" -> {
                when {
                    coverUrl.startsWith("http://") || coverUrl.startsWith("https://") -> coverUrl
                    coverUrl.startsWith("content://") || coverUrl.startsWith("file://") -> android.net.Uri.parse(coverUrl)
                    coverUrl.startsWith("/") || coverUrl.contains(":") -> {
                        val file = java.io.File(coverUrl)
                        if (file.exists() && file.length() > 0) {
                            file
                        } else if (id.length == 11 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
                            "https://img.youtube.com/vi/$id/hqdefault.jpg"
                        } else {
                            R.drawable.defaultthumbnail
                        }
                    }
                    else -> coverUrl
                }
            }
            // Only generate YouTube thumbnail for real 11-char YouTube video IDs (A-Za-z0-9_-)
            // Explicitly exclude import_... and spotify:... IDs
            id.length == 11 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' } -> {
                "https://img.youtube.com/vi/$id/hqdefault.jpg"
            }
            else -> R.drawable.defaultthumbnail
        }
}
