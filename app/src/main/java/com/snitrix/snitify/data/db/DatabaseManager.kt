package com.snitrix.snitify.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.snitrix.snitify.data.model.Song

data class DownloadedTrack(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val localPath: String,
    val coverLocalPath: String
)

data class Playlist(
    val id: Long,
    val name: String,
    val logoPath: String?
)

object DatabaseManager {
    private const val DATABASE_NAME = "metrolist.db"
    private const val DATABASE_VERSION = 5

    private var dbHelper: DatabaseHelper? = null

    fun init(context: Context) {
        if (dbHelper == null) {
            dbHelper = DatabaseHelper(context.applicationContext)
        }
    }

    private val db: SQLiteDatabase
        get() = dbHelper?.writableDatabase ?: throw IllegalStateException("DatabaseManager not initialized")

    // ─── Recently Played ──────────────────────────────────────────────────────

    fun addRecentlyPlayed(song: Song) {
        if (dbHelper == null) return
        val values = ContentValues().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("duration", song.duration)
            put("cover_url", song.coverUrl)
            put("played_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("recently_played", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        // Prune to top 20
        db.execSQL("DELETE FROM recently_played WHERE id NOT IN (SELECT id FROM recently_played ORDER BY played_at DESC LIMIT 20)")
    }

    fun getRecentlyPlayed(): List<Song> {
        val list = mutableListOf<Song>()
        if (dbHelper == null) return list
        val cursor = db.query("recently_played", null, null, null, null, null, "played_at DESC")
        if (cursor.moveToFirst()) {
            do {
                val id       = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val title    = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val artist   = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album    = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url"))
                val offlineCover = getLocalCoverPath(id)
                list.add(Song(id, title, artist, album, duration, offlineCover ?: coverUrl))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // ─── Downloads ────────────────────────────────────────────────────────────

    fun insertDownload(
        id: String, title: String, artist: String, album: String,
        duration: Int, coverUrl: String, localPath: String, coverLocalPath: String
    ) {
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("duration", duration)
            put("cover_url", coverUrl)
            put("local_path", localPath)
            put("cover_local_path", coverLocalPath)
        }
        db.insertWithOnConflict("downloads", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun isDownloaded(id: String): Boolean {
        if (dbHelper == null) return false
        val cursor = db.query("downloads", arrayOf("id"), "id = ?", arrayOf(id), null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getDownloadPath(id: String): String? {
        if (dbHelper == null) return null
        val cursor = db.query("downloads", arrayOf("local_path"), "id = ?", arrayOf(id), null, null, null)
        var path: String? = null
        if (cursor.moveToFirst()) path = cursor.getString(0)
        cursor.close()
        return path
    }

    fun getLocalCoverPath(id: String): String? {
        if (dbHelper == null) return null
        val cursor = db.query("downloads", arrayOf("cover_local_path"), "id = ?", arrayOf(id), null, null, null)
        var path: String? = null
        if (cursor.moveToFirst()) path = cursor.getString(0)
        cursor.close()
        return path
    }

    fun getAllDownloads(): List<Song> {
        val list = mutableListOf<Song>()
        val cursor = db.query("downloads", null, null, null, null, null, "title ASC")
        if (cursor.moveToFirst()) {
            do {
                val id       = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val title    = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val artist   = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album    = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url"))
                val coverLocal = cursor.getString(cursor.getColumnIndexOrThrow("cover_local_path"))
                val finalCoverStr = when {
                    coverLocal.isNotBlank() && java.io.File(coverLocal).exists() && java.io.File(coverLocal).length() > 0 -> coverLocal
                    coverUrl.isNotBlank() && coverUrl != "null" && coverUrl != "defaultthumbnail" -> coverUrl
                    id.length == 11 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' } -> "https://img.youtube.com/vi/$id/hqdefault.jpg"
                    else -> coverLocal.ifEmpty { coverUrl }
                }
                list.add(Song(id, title, artist, album, duration, finalCoverStr))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteDownload(id: String) {
        db.delete("downloads", "id = ?", arrayOf(id))
    }

    // ─── Playlists ────────────────────────────────────────────────────────────

    fun createPlaylist(name: String, logoPath: String?): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("logo_path", logoPath)
        }
        return db.insert("playlists", null, values)
    }

    fun getAllPlaylists(): List<Playlist> {
        val list = mutableListOf<Playlist>()
        val cursor = db.query("playlists", null, null, null, null, null, "id DESC")
        if (cursor.moveToFirst()) {
            do {
                val id       = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val name     = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val logoPath = cursor.getString(cursor.getColumnIndexOrThrow("logo_path"))
                list.add(Playlist(id, name, logoPath))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deletePlaylist(playlistId: Long) {
        db.delete("playlists", "id = ?", arrayOf(playlistId.toString()))
        db.delete("playlist_songs", "playlist_id = ?", arrayOf(playlistId.toString()))
    }

    fun updatePlaylist(id: Long, name: String, logoPath: String?) {
        val values = ContentValues().apply {
            put("name", name)
            put("logo_path", logoPath)
        }
        db.update("playlists", values, "id = ?", arrayOf(id.toString()))
    }

    fun playlistExists(name: String, excludeId: Long? = null): Boolean {
        val selection = if (excludeId != null) "name = ? COLLATE NOCASE AND id != ?" else "name = ? COLLATE NOCASE"
        val selectionArgs = if (excludeId != null) arrayOf(name, excludeId.toString()) else arrayOf(name)
        val cursor = db.query("playlists", arrayOf("id"), selection, selectionArgs, null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun addSongToPlaylist(
        playlistId: Long, songId: String, title: String, artist: String,
        album: String, duration: Int, coverUrl: String
    ) {
        val values = ContentValues().apply {
            put("playlist_id", playlistId)
            put("song_id", songId)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("duration", duration)
            put("cover_url", coverUrl)
        }
        db.insertWithOnConflict("playlist_songs", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        db.delete("playlist_songs", "playlist_id = ? AND song_id = ?",
            arrayOf(playlistId.toString(), songId))
    }

    fun getPlaylistSongs(playlistId: Long): List<Song> {
        val list = mutableListOf<Song>()
        val cursor = db.query("playlist_songs", null, "playlist_id = ?",
            arrayOf(playlistId.toString()), null, null, "rowid ASC")
        if (cursor.moveToFirst()) {
            do {
                val songId   = cursor.getString(cursor.getColumnIndexOrThrow("song_id"))
                val title    = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val artist   = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album    = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url"))
                val offlineCoverPath = getLocalCoverPath(songId)
                val validOfflineCover = offlineCoverPath?.takeIf { it.isNotBlank() && java.io.File(it).exists() }
                list.add(Song(songId, title, artist, album, duration, validOfflineCover ?: coverUrl))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // ─── Favorites ────────────────────────────────────────────────────────────

    fun addFavorite(id: String, title: String, artist: String, album: String, duration: Int, coverUrl: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("duration", duration)
            put("cover_url", coverUrl)
        }
        db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateFavoriteCover(oldId: String, newId: String, newCoverUrl: String) {
        if (dbHelper == null) return
        val values = ContentValues().apply {
            put("id", newId)
            put("cover_url", newCoverUrl)
        }
        db.update("favorites", values, "id = ?", arrayOf(oldId))
    }

    fun addFavoritesBatch(songs: List<Song>) {
        if (dbHelper == null || songs.isEmpty()) return
        db.beginTransaction()
        try {
            for (song in songs) {
                val values = ContentValues().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album ?: "")
                    put("duration", song.duration)
                    put("cover_url", song.coverUrl ?: "")
                }
                db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addPlaylistSongsBatch(playlistId: Long, songs: List<Song>) {
        if (dbHelper == null || songs.isEmpty()) return
        db.beginTransaction()
        try {
            for (song in songs) {
                val values = ContentValues().apply {
                    put("playlist_id", playlistId)
                    put("song_id", song.id)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album ?: "")
                    put("duration", song.duration)
                    put("cover_url", song.coverUrl ?: "")
                }
                db.insertWithOnConflict("playlist_songs", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun removeFavorite(id: String) {
        db.delete("favorites", "id = ?", arrayOf(id))
    }

    fun isFavorite(id: String): Boolean {
        if (dbHelper == null) return false
        val cursor = db.query("favorites", arrayOf("id"), "id = ?", arrayOf(id), null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getAllFavorites(): List<Song> {
        val list = mutableListOf<Song>()
        val cursor = db.query("favorites", null, null, null, null, null, "rowid DESC")
        if (cursor.moveToFirst()) {
            do {
                val id       = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val title    = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val artist   = cursor.getString(cursor.getColumnIndexOrThrow("artist"))
                val album    = cursor.getString(cursor.getColumnIndexOrThrow("album"))
                val duration = cursor.getInt(cursor.getColumnIndexOrThrow("duration"))
                val coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url"))
                val offlineCover = getLocalCoverPath(id)
                list.add(Song(id, title, artist, album, duration, offlineCover ?: coverUrl))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateImportedSongIdAndCover(oldId: String, newId: String, newCoverUrl: String) {
        if (dbHelper == null) return
        val favValues = ContentValues().apply {
            put("id", newId)
            if (newCoverUrl.isNotBlank()) put("cover_url", newCoverUrl)
        }
        db.update("favorites", favValues, "id = ?", arrayOf(oldId))

        val plValues = ContentValues().apply {
            put("song_id", newId)
            if (newCoverUrl.isNotBlank()) put("cover_url", newCoverUrl)
        }
        db.update("playlist_songs", plValues, "song_id = ?", arrayOf(oldId))
    }

    // ─── Search History ───────────────────────────────────────────────────────

    fun addSearchHistory(query: String) {
        if (dbHelper == null) return
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val values = ContentValues().apply {
            put("query", trimmed)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("search_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        // Prune to top 50 entries
        db.execSQL("""
            DELETE FROM search_history WHERE query NOT IN
            (SELECT query FROM search_history ORDER BY timestamp DESC LIMIT 50)
        """.trimIndent())
    }

    fun getSearchHistory(): List<String> {
        val list = mutableListOf<String>()
        if (dbHelper == null) return list
        val cursor = db.query("search_history", arrayOf("query"), null, null, null, null, "timestamp DESC", "50")
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun removeSearchHistory(query: String) {
        if (dbHelper == null) return
        db.delete("search_history", "query = ?", arrayOf(query.trim()))
    }

    fun clearSearchHistory() {
        if (dbHelper == null) return
        db.delete("search_history", null, null)
    }

    // ─── Schema ───────────────────────────────────────────────────────────────

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE downloads (
                    id               TEXT PRIMARY KEY,
                    title            TEXT NOT NULL,
                    artist           TEXT NOT NULL,
                    album            TEXT NOT NULL DEFAULT '',
                    duration         INTEGER NOT NULL DEFAULT 0,
                    cover_url        TEXT NOT NULL,
                    local_path       TEXT NOT NULL,
                    cover_local_path TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE playlists (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    name      TEXT NOT NULL,
                    logo_path TEXT
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE playlist_songs (
                    playlist_id INTEGER NOT NULL,
                    song_id     TEXT NOT NULL,
                    title       TEXT NOT NULL,
                    artist      TEXT NOT NULL,
                    album       TEXT NOT NULL DEFAULT '',
                    duration    INTEGER NOT NULL DEFAULT 0,
                    cover_url   TEXT NOT NULL,
                    PRIMARY KEY (playlist_id, song_id)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE favorites (
                    id       TEXT PRIMARY KEY,
                    title    TEXT NOT NULL,
                    artist   TEXT NOT NULL,
                    album    TEXT NOT NULL DEFAULT '',
                    duration INTEGER NOT NULL DEFAULT 0,
                    cover_url TEXT NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE recently_played (
                    id       TEXT PRIMARY KEY,
                    title    TEXT NOT NULL,
                    artist   TEXT NOT NULL,
                    album    TEXT NOT NULL DEFAULT '',
                    duration INTEGER NOT NULL DEFAULT 0,
                    cover_url TEXT NOT NULL,
                    played_at INTEGER NOT NULL
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE search_history (
                    query     TEXT PRIMARY KEY,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Migrate existing databases without dropping user data
            if (oldVersion < 5) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS search_history (
                        query     TEXT PRIMARY KEY,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
