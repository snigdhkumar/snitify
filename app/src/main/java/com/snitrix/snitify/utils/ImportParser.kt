package com.snitrix.snitify.utils

import android.content.Context
import android.net.Uri
import com.snitrix.snitify.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

object ImportParser {

    /**
     * Entry point to parse an imported file URI (CSV, JSON, or TXT).
     * Returns a Pair containing:
     * - Suggested default name (cleaned filename)
     * - List of parsed [Song] objects
     */
    fun parseUri(context: Context, uri: Uri, fileName: String): Pair<String, List<Song>> {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val cleanName = fileName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val content = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Failed to read content from URI: $uri")
            ""
        }

        if (content.isBlank()) return Pair(cleanName, emptyList())

        val songs = when {
            extension == "csv" || content.startsWith("Track URI") || content.contains(',') -> parseCsv(content)
            extension == "json" || content.trimStart().startsWith('{') || content.trimStart().startsWith('[') -> parseJson(content)
            else -> parseTxt(content)
        }

        return Pair(cleanName, songs)
    }

    /**
     * Parses CSV text with dynamic column header detection and handles double quotes & comma delimiters.
     */
    fun parseCsv(csvText: String): List<Song> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headerRow = parseCsvLine(lines.first())
        val titleIdx = findHeaderIndex(headerRow, listOf("track name", "title", "track", "name", "song title"))
        val artistIdx = findHeaderIndex(headerRow, listOf("artist name(s)", "artist name", "artist", "artists"))
        val albumIdx = findHeaderIndex(headerRow, listOf("album name", "album"))
        val durationIdx = findHeaderIndex(headerRow, listOf("duration (ms)", "duration", "length"))
        val uriIdx = findHeaderIndex(headerRow, listOf("track uri", "uri", "id"))

        if (titleIdx == -1 && artistIdx == -1) {
            // Fallback: simple CSV without headers
            return lines.mapNotNull { parseFallbackLine(it) }
        }

        val songs = mutableListOf<Song>()
        for (i in 1 until lines.size) {
            val columns = parseCsvLine(lines[i])
            val title = if (titleIdx != -1 && titleIdx < columns.size) columns[titleIdx].trim() else ""
            val rawArtist = if (artistIdx != -1 && artistIdx < columns.size) columns[artistIdx].trim() else "Unknown Artist"
            val album = if (albumIdx != -1 && albumIdx < columns.size) columns[albumIdx].trim() else ""
            val rawDuration = if (durationIdx != -1 && durationIdx < columns.size) columns[durationIdx].trim() else "0"
            val rawUri = if (uriIdx != -1 && uriIdx < columns.size) columns[uriIdx].trim() else ""

            if (title.isBlank()) continue

            val artist = sanitizeArtist(rawArtist)
            val durationMs = rawDuration.toLongOrNull()?.toInt() ?: 0
            val songId = if (rawUri.isNotBlank()) rawUri else "import_${title.hashCode()}_${artist.hashCode()}"

            songs.add(
                Song(
                    id = songId,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = durationMs,
                    coverUrl = ""
                )
            )
        }

        return songs
    }

    /**
     * Parses JSON content (supports Spotify export JSON arrays or custom JSON format).
     */
    fun parseJson(jsonText: String): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val trimmed = jsonText.trim()
            if (trimmed.startsWith('[')) {
                val jsonArray = JSONArray(trimmed)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    parseJsonObjectToSong(obj)?.let { songs.add(it) }
                }
            } else if (trimmed.startsWith('{')) {
                val obj = JSONObject(trimmed)
                if (obj.has("playlists")) {
                    // Extract songs across playlists
                    val playlists = obj.getJSONArray("playlists")
                    for (i in 0 until playlists.length()) {
                        val pl = playlists.getJSONObject(i)
                        val sArray = pl.optJSONArray("songs") ?: continue
                        for (j in 0 until sArray.length()) {
                            parseJsonObjectToSong(sArray.getJSONObject(j))?.let { songs.add(it) }
                        }
                    }
                } else if (obj.has("favorites")) {
                    val favs = obj.getJSONArray("favorites")
                    for (i in 0 until favs.length()) {
                        parseJsonObjectToSong(favs.getJSONObject(i))?.let { songs.add(it) }
                    }
                } else if (obj.has("items")) {
                    val items = obj.getJSONArray("items")
                    for (i in 0 until items.length()) {
                        parseJsonObjectToSong(items.getJSONObject(i))?.let { songs.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing JSON import")
        }
        return songs
    }

    /**
     * Parses TXT lists line-by-line (e.g. "Track Name - Artist Name" or "Artist Name - Track Name").
     */
    fun parseTxt(txtContent: String): List<Song> {
        val songs = mutableListOf<Song>()
        txtContent.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                parseFallbackLine(trimmed)?.let { songs.add(it) }
            }
        }
        return songs
    }

    // ── Private Helper Functions ──────────────────────────────────────────────

    private fun findHeaderIndex(headers: List<String>, candidates: List<String>): Int {
        for (candidate in candidates) {
            val idx = headers.indexOfFirst { it.lowercase().trim() == candidate }
            if (idx != -1) return idx
        }
        return -1
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun parseFallbackLine(line: String): Song? {
        val parts = line.split('-', '–', '—', '|').map { it.trim() }
        if (parts.size >= 2) {
            val title = parts[0]
            val artist = sanitizeArtist(parts[1])
            if (title.isNotBlank()) {
                return Song(
                    id = "import_${title.hashCode()}_${artist.hashCode()}",
                    title = title,
                    artist = artist,
                    album = "",
                    duration = 0,
                    coverUrl = ""
                )
            }
        } else if (line.isNotBlank()) {
            return Song(
                id = "import_${line.hashCode()}",
                title = line,
                artist = "Unknown Artist",
                album = "",
                duration = 0,
                coverUrl = ""
            )
        }
        return null
    }

    private fun parseJsonObjectToSong(obj: JSONObject): Song? {
        val title = obj.optString("trackName").ifEmpty { obj.optString("title").ifEmpty { obj.optString("name") } }
        if (title.isBlank()) return null

        val artist = obj.optString("artistName").ifEmpty { obj.optString("artist").ifEmpty { "Unknown Artist" } }
        val album = obj.optString("albumName").ifEmpty { obj.optString("album") }
        val duration = obj.optInt("duration", obj.optInt("duration_ms", 0))
        val coverUrl = obj.optString("coverUrl", "")
        val songId = obj.optString("id").ifEmpty { obj.optString("uri").ifEmpty { "import_${title.hashCode()}_${artist.hashCode()}" } }

        return Song(
            id = songId,
            title = title,
            artist = sanitizeArtist(artist),
            album = album,
            duration = duration,
            coverUrl = coverUrl
        )
    }

    private fun sanitizeArtist(rawArtist: String): String {
        if (rawArtist.isBlank()) return "Unknown Artist"
        // Replace Spotify ';' delimiters with ', '
        return rawArtist.split(';').map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
    }
}
