package com.snitrix.snitify.data.repository

import android.content.Context
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.snitrix.snitify.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class MusicRepository {

    suspend fun getRecommendedSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val homePage = YouTube.home().getOrNull()
            val songs = homePage?.sections?.flatMap { section ->
                section.items.filterIsInstance<SongItem>().map { songItem ->
                    Song(
                        id = songItem.id,
                        title = songItem.title,
                        artist = songItem.artists.joinToString(", ") { it.name },
                        album = songItem.album?.name ?: "Unknown Album",
                        duration = songItem.duration ?: 0,
                        coverUrl = songItem.thumbnail ?: ""
                    )
                }
            } ?: emptyList()

            if (songs.isNotEmpty()) {
                Timber.d("Fetched ${songs.size} real songs from YouTube Music home page")
                songs.distinctBy { it.id }
            } else {
                // If YouTube home returns nothing (e.g. at initial sync/auth delay),
                // search for a generic music tag to get real active songs
                searchSongs("acoustic pop").getOrNull() ?: emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load dynamic home songs")
            emptyList()
        }
    }

    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
            result.items.filterIsInstance<SongItem>().map { songItem ->
                Song(
                    id = songItem.id,
                    title = songItem.title,
                    artist = songItem.artists.joinToString(", ") { it.name },
                    album = songItem.album?.name ?: "Unknown Album",
                    duration = songItem.duration ?: 0,
                    coverUrl = songItem.thumbnail ?: ""
                )
            }
        }
    }

    suspend fun getSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val result = YouTube.searchSuggestions(query).getOrThrow()
            result.queries
        }
    }

    fun getDeviceMediaSongs(context: Context): List<Song> = try {
        val songs = mutableListOf<Song>()
        val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            android.provider.MediaStore.Audio.Media._ID,
            android.provider.MediaStore.Audio.Media.TITLE,
            android.provider.MediaStore.Audio.Media.DISPLAY_NAME,
            android.provider.MediaStore.Audio.Media.ARTIST,
            android.provider.MediaStore.Audio.Media.ALBUM,
            android.provider.MediaStore.Audio.Media.ALBUM_ID,
            android.provider.MediaStore.Audio.Media.DURATION,
            android.provider.MediaStore.Audio.Media.DATA,
            android.provider.MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = context.contentResolver.query(uri, projection, selection, null, "${android.provider.MediaStore.Audio.Media.DATE_ADDED} DESC")

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
            val displayNameCol = c.getColumnIndex(android.provider.MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
            val dateCol = c.getColumnIndex(android.provider.MediaStore.Audio.Media.DATE_ADDED)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val rawTitle = c.getString(titleCol) ?: "Unknown Title"
                val displayName = if (displayNameCol >= 0) c.getString(displayNameCol) else null
                val fileNameTitle = displayName?.substringBeforeLast(".")?.ifEmpty { null }
                val title = fileNameTitle ?: (if (rawTitle != "<unknown>" && rawTitle.isNotEmpty()) rawTitle else "Unknown Title")
                val rawArtist = c.getString(artistCol) ?: "Unknown Artist"
                val artist = if (rawArtist == "<unknown>") "Unknown Artist" else rawArtist
                val album = c.getString(albumCol) ?: "Unknown Album"
                val duration = (c.getLong(durationCol) / 1000).toInt()
                val dataPath = c.getString(dataCol) ?: ""
                val albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else -1L
                val dateAdded = if (dateCol >= 0) c.getLong(dateCol) else 0L

                val contentUri = android.content.ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                ).toString()

                val albumArtUri = if (albumId > 0) {
                    android.content.ContentUris.withAppendedId(
                        android.net.Uri.parse("content://media/external/audio/albumart"), albumId
                    ).toString()
                } else {
                    "defaultthumbnail"
                }

                val file = try { java.io.File(dataPath) } catch (e: Exception) { null }
                val actualFileName = if (file != null && file.nameWithoutExtension.isNotBlank()) {
                    file.nameWithoutExtension
                } else {
                    rawTitle
                }

                val folderName = file?.parentFile?.name ?: "Storage"

                songs.add(
                    Song(
                        id = contentUri,
                        title = actualFileName,
                        artist = "$artist - $folderName",
                        album = album,
                        duration = if (dateAdded > 0) dateAdded.toInt() else duration,
                        coverUrl = albumArtUri
                    )
                )
            }
        }
        songs
    } catch (e: Exception) {
        Timber.e(e, "Error scanning MediaStore audio")
        emptyList()
    }
}
