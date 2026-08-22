package com.snitrix.snitify.utils

import android.content.Context
import android.net.Uri
import com.snitrix.snitify.data.db.DatabaseManager
import com.snitrix.snitify.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupRestoreManager {

    fun createBackupJson(context: Context): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Playlists
        val playlists = DatabaseManager.getAllPlaylists()
        val playlistsArray = JSONArray()
        for (pl in playlists) {
            val plObj = JSONObject()
            plObj.put("id", pl.id)
            plObj.put("name", pl.name)
            plObj.put("logoPath", pl.logoPath ?: "")
            
            // Songs inside playlist
            val songs = DatabaseManager.getPlaylistSongs(pl.id)
            val songsArray = JSONArray()
            for (s in songs) {
                songsArray.put(songToJson(s))
            }
            plObj.put("songs", songsArray)
            playlistsArray.put(plObj)
        }
        root.put("playlists", playlistsArray)

        // Favorites
        val favorites = DatabaseManager.getAllFavorites()
        val favsArray = JSONArray()
        for (song in favorites) {
            favsArray.put(songToJson(song))
        }
        root.put("favorites", favsArray)

        // Recently Played
        val history = DatabaseManager.getRecentlyPlayed()
        val histArray = JSONArray()
        for (song in history) {
            histArray.put(songToJson(song))
        }
        root.put("history", histArray)

        return root.toString(2)
    }

    fun exportBackupToUri(context: Context, uri: Uri): Boolean {
        return try {
            val json = createBackupJson(context)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val contentStr = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        contentStr.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val root = JSONObject(contentStr.toString())
            
            // Restore Favorites
            if (root.has("favorites")) {
                val favsArray = root.getJSONArray("favorites")
                for (i in 0 until favsArray.length()) {
                    val song = jsonToSong(favsArray.getJSONObject(i))
                    DatabaseManager.addFavorite(song.id, song.title, song.artist, song.album, song.duration, song.coverUrl)
                }
            }

            // Restore Recently Played History
            if (root.has("history")) {
                val histArray = root.getJSONArray("history")
                for (i in 0 until histArray.length()) {
                    val song = jsonToSong(histArray.getJSONObject(i))
                    DatabaseManager.addRecentlyPlayed(song)
                }
            }

            // Restore Playlists
            if (root.has("playlists")) {
                val playlistsArray = root.getJSONArray("playlists")
                for (i in 0 until playlistsArray.length()) {
                    val plObj = playlistsArray.getJSONObject(i)
                    val name = plObj.getString("name")
                    val rawLogo = plObj.optString("logoPath", "")
                    val logoPath = if (rawLogo.isNotEmpty()) rawLogo else null
                    
                    if (!DatabaseManager.playlistExists(name)) {
                        val plId = DatabaseManager.createPlaylist(name, logoPath)
                        if (plId > 0 && plObj.has("songs")) {
                            val songsArray = plObj.getJSONArray("songs")
                            for (j in 0 until songsArray.length()) {
                                val s = jsonToSong(songsArray.getJSONObject(j))
                                DatabaseManager.addSongToPlaylist(plId, s.id, s.title, s.artist, s.album, s.duration, s.coverUrl)
                            }
                        }
                    }
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class PlaylistImportResult(val addedCount: Int, val skippedCount: Int, val isMerged: Boolean, val playlistName: String)

    fun importParsedSongsToFavorites(
        songs: List<Song>,
        onProgress: ((current: Int, total: Int, currentSong: Song) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null
    ): Pair<Int, Int> {
        val existingFavorites = DatabaseManager.getAllFavorites().toMutableList()
        val bucketMap = DuplicateDetector.buildBucketMap(existingFavorites).mapValues { it.value.toMutableList() }.toMutableMap()

        val songsToAdd = mutableListOf<Song>()
        var addedCount = 0
        var skippedCount = 0
        val total = songs.size

        for ((index, song) in songs.withIndex()) {
            if (isCancelled?.invoke() == true) break
            onProgress?.invoke(index + 1, total, song)

            if (DuplicateDetector.isDuplicateAgainstList(song, existingFavorites, bucketMap)) {
                skippedCount++
            } else {
                songsToAdd.add(song)
                addedCount++
                existingFavorites.add(song)
                
                val normTitle = DuplicateDetector.normalizeString(song.title)
                val bucketKey = if (normTitle.length >= 2) normTitle.substring(0, 2) else normTitle
                bucketMap.getOrPut(bucketKey) { mutableListOf() }.add(song)
            }
        }

        if (songsToAdd.isNotEmpty()) {
            DatabaseManager.addFavoritesBatch(songsToAdd)
        }
        return Pair(addedCount, skippedCount)
    }

    fun importParsedSongsToPlaylist(
        playlistName: String,
        songs: List<Song>,
        onProgress: ((current: Int, total: Int, currentSong: Song) -> Unit)? = null,
        isCancelled: (() -> Boolean)? = null
    ): PlaylistImportResult {
        val cleanName = playlistName.ifBlank { "Imported Playlist" }
        val existingPlaylist = DatabaseManager.getAllPlaylists().find { it.name.equals(cleanName, ignoreCase = true) }
        val isMerged = existingPlaylist != null

        val playlistId = existingPlaylist?.id ?: DatabaseManager.createPlaylist(cleanName, null)
        if (playlistId <= 0L) return PlaylistImportResult(0, songs.size, false, cleanName)

        val existingSongs = DatabaseManager.getPlaylistSongs(playlistId).toMutableList()
        val bucketMap = DuplicateDetector.buildBucketMap(existingSongs).mapValues { it.value.toMutableList() }.toMutableMap()

        val songsToAdd = mutableListOf<Song>()
        var addedCount = 0
        var skippedCount = 0
        val total = songs.size

        for ((index, song) in songs.withIndex()) {
            if (isCancelled?.invoke() == true) break
            onProgress?.invoke(index + 1, total, song)

            if (DuplicateDetector.isDuplicateAgainstList(song, existingSongs, bucketMap)) {
                skippedCount++
            } else {
                songsToAdd.add(song)
                addedCount++
                existingSongs.add(song)

                val normTitle = DuplicateDetector.normalizeString(song.title)
                val bucketKey = if (normTitle.length >= 2) normTitle.substring(0, 2) else normTitle
                bucketMap.getOrPut(bucketKey) { mutableListOf() }.add(song)
            }
        }

        if (songsToAdd.isNotEmpty()) {
            DatabaseManager.addPlaylistSongsBatch(playlistId, songsToAdd)
        }
        return PlaylistImportResult(addedCount, skippedCount, isMerged, cleanName)
    }

    private fun songToJson(song: Song): JSONObject {
        val obj = JSONObject()
        obj.put("id", song.id)
        obj.put("title", song.title)
        obj.put("artist", song.artist)
        obj.put("album", song.album)
        obj.put("duration", song.duration)
        obj.put("coverUrl", song.coverUrl)
        return obj
    }

    private fun jsonToSong(obj: JSONObject): Song {
        return Song(
            id = obj.getString("id"),
            title = obj.getString("title"),
            artist = obj.getString("artist"),
            album = obj.optString("album", ""),
            duration = obj.optInt("duration", 0),
            coverUrl = obj.optString("coverUrl", "")
        )
    }
}
