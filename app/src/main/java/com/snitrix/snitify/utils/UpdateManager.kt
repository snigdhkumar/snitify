package com.snitrix.snitify.utils

import android.content.Context
import com.snitrix.snitify.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkSize: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val releaseNotes: List<String>
)

object UpdateManager {

    private const val UPDATE_JSON_URL = "https://snitify.snitrix.in/checkupdate.json"
    private const val PREFS_NAME = "snitify_update_prefs"
    private const val KEY_LAST_DISMISSED = "last_dismissed_timestamp"
    private const val COOLDOWN_24H_MS = 24 * 60 * 60 * 1000L

    /**
     * Asynchronously fetch update JSON from server
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(UPDATE_JSON_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)

                val latestCode = root.optInt("versionCode", 0)
                val latestName = root.optString("versionName", "")
                val apkSize = root.optString("apkSize", "18.7 MB")
                val downloadUrl = root.optString("downloadUrl", "https://snitify.snitrix.in")
                val forceUpdate = root.optBoolean("forceUpdate", false)

                val notesList = mutableListOf<String>()
                if (root.has("releaseNotes")) {
                    val array = root.getJSONArray("releaseNotes")
                    for (i in 0 until array.length()) {
                        notesList.add(array.getString(i))
                    }
                }

                // Check if server version code is higher than installed version code
                if (latestCode > BuildConfig.VERSION_CODE) {
                    return@withContext UpdateInfo(
                        versionCode = latestCode,
                        versionName = latestName,
                        apkSize = apkSize.ifBlank { "18.7 MB" },
                        downloadUrl = downloadUrl.ifBlank { "https://snitify.snitrix.in" },
                        forceUpdate = forceUpdate,
                        releaseNotes = notesList
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Update check failed")
        }
        return@withContext null
    }

    /**
     * Determines whether to present update dialog based on 24h cooldown and forceUpdate override
     */
    fun shouldShowUpdateDialog(context: Context, updateInfo: UpdateInfo): Boolean {
        if (updateInfo.forceUpdate) return true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDismissed = prefs.getLong(KEY_LAST_DISMISSED, 0L)
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastDismissed) >= COOLDOWN_24H_MS
    }

    /**
     * Save current timestamp when user taps 'Later'
     */
    fun saveLaterDismissalTimestamp(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_DISMISSED, System.currentTimeMillis()).apply()
    }
}
