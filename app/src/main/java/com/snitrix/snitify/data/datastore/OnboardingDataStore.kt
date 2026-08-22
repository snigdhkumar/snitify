package com.snitrix.snitify.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "snitify_preferences")

class OnboardingDataStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_SELECTED_LANGUAGES = stringSetPreferencesKey("selected_languages")
        private val KEY_SELECTED_ARTISTS = stringSetPreferencesKey("selected_artists")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val selectedLanguages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_LANGUAGES] ?: emptySet()
    }

    val selectedArtists: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_ARTISTS] ?: emptySet()
    }

    val artistImagesMap: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val map = mutableMapOf<String, String>()
        val artists = preferences[KEY_SELECTED_ARTISTS] ?: emptySet()
        artists.forEach { name ->
            val key = stringPreferencesKey("artist_img_${name.lowercase().trim()}")
            val path = preferences[key]
            if (!path.isNullOrEmpty()) {
                map[name] = path
            }
        }
        map
    }

    val languageImagesMap: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val map = mutableMapOf<String, String>()
        val languages = preferences[KEY_SELECTED_LANGUAGES] ?: emptySet()
        languages.forEach { name ->
            val key = stringPreferencesKey("lang_img_${name.lowercase().trim()}")
            val path = preferences[key]
            if (!path.isNullOrEmpty()) {
                map[name] = path
            }
        }
        map
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun savePreferences(languages: Set<String>, artists: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LANGUAGES] = languages
            preferences[KEY_SELECTED_ARTISTS] = artists
            preferences[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateLanguages(languages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LANGUAGES] = languages
        }
    }

    suspend fun updateArtists(artists: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_ARTISTS] = artists
        }
    }

    suspend fun saveArtistImagePath(name: String, imagePath: String?) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("artist_img_${name.lowercase().trim()}")
            if (imagePath != null) {
                preferences[key] = imagePath
            } else {
                preferences.remove(key)
            }
        }
    }

    suspend fun saveLanguageImagePath(name: String, imagePath: String?) {
        context.dataStore.edit { preferences ->
            val key = stringPreferencesKey("lang_img_${name.lowercase().trim()}")
            if (imagePath != null) {
                preferences[key] = imagePath
            } else {
                preferences.remove(key)
            }
        }
    }

    suspend fun removeArtist(name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SELECTED_ARTISTS]?.toMutableSet() ?: mutableSetOf()
            current.remove(name)
            preferences[KEY_SELECTED_ARTISTS] = current
            preferences.remove(stringPreferencesKey("artist_img_${name.lowercase().trim()}"))
        }
    }

    suspend fun removeLanguage(name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_SELECTED_LANGUAGES]?.toMutableSet() ?: mutableSetOf()
            current.remove(name)
            preferences[KEY_SELECTED_LANGUAGES] = current
            preferences.remove(stringPreferencesKey("lang_img_${name.lowercase().trim()}"))
        }
    }
}
