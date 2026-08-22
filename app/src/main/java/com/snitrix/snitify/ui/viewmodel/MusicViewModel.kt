package com.snitrix.snitify.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.data.repository.MusicRepository
import com.snitrix.snitify.playback.PlaybackManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import com.snitrix.snitify.data.db.DatabaseManager
import com.snitrix.snitify.data.db.Playlist
import com.snitrix.snitify.playback.DownloadManager
import com.snitrix.snitify.playback.DownloadProgress
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.ImportProgressState
import com.snitrix.snitify.utils.BackupRestoreManager
import kotlinx.coroutines.Job
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.HomePage

enum class TrackTransitionType {
    NONE,
    SLIDE_NEXT,
    SLIDE_PREV
}

class MusicViewModel(
    private val repository: MusicRepository,
    private val context: Context
) : ViewModel() {

    // Tab State
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    suspend fun fetchFeedForQuery(query: String): List<Song> {
        return repository.searchSongs(query).getOrDefault(emptyList())
    }

    private val _playlistUpdateTrigger = MutableStateFlow(0)
    val playlistUpdateTrigger: StateFlow<Int> = _playlistUpdateTrigger.asStateFlow()

    fun triggerPlaylistUpdate() {
        _playlistUpdateTrigger.value += 1
    }

    // YouTube Home Feed State
    private val _homePage = MutableStateFlow<HomePage?>(null)
    val homePage: StateFlow<HomePage?> = _homePage.asStateFlow()

    private val _isRefreshingHome = MutableStateFlow(false)
    val isRefreshingHome: StateFlow<Boolean> = _isRefreshingHome.asStateFlow()

    fun refreshHomeFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingHome.value = true
            YouTube.home().onSuccess { page ->
                _homePage.value = page
            }.onFailure {
                Timber.e(it, "Failed to load YouTube home page")
            }
            _isRefreshingHome.value = false
        }
    }

    // Playlist/Recommended Songs
    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    // Import State
    private val _importProgressState = MutableStateFlow<ImportProgressState?>(null)
    val importProgressState: StateFlow<ImportProgressState?> = _importProgressState.asStateFlow()
    private var importJob: Job? = null

    // Playback State
    private val _currentTrack = MutableStateFlow<Song?>(null)
    val currentTrack: StateFlow<Song?> = _currentTrack.asStateFlow()

    private val _maximizePlayerSignal = MutableStateFlow(0)
    val maximizePlayerSignal: StateFlow<Int> = _maximizePlayerSignal.asStateFlow()

    fun triggerMaximizePlayer() {
        _maximizePlayerSignal.value += 1
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressMs = MutableStateFlow(0L)
    val progressMs: StateFlow<Long> = _progressMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isTrackLoading = MutableStateFlow(false)
    val isTrackLoading: StateFlow<Boolean> = _isTrackLoading.asStateFlow()

    // Queue & Shuffle/Repeat State
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatEnabled = MutableStateFlow(false)
    val repeatEnabled: StateFlow<Boolean> = _repeatEnabled.asStateFlow()

    private val _transitionType = MutableStateFlow(TrackTransitionType.NONE)
    val transitionType: StateFlow<TrackTransitionType> = _transitionType.asStateFlow()

    // Liked Songs State
    private val _likedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val likedTrackIds: StateFlow<Set<String>> = _likedTrackIds.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<Song>>(emptyList())
    val downloadedTracks: StateFlow<List<Song>> = _downloadedTracks.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<List<Song>>(emptyList())
    val favoriteTracks: StateFlow<List<Song>> = _favoriteTracks.asStateFlow()

    private val _deviceSongs = MutableStateFlow<List<Song>>(emptyList())
    val deviceSongs: StateFlow<List<Song>> = _deviceSongs.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _activeLibraryCategory = MutableStateFlow<String?>(null)
    val activeLibraryCategory: StateFlow<String?> = _activeLibraryCategory.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    /** Live map of songId → DownloadProgress (status + percent) */
    val downloadStatusFlow: StateFlow<Map<String, DownloadProgress>> = DownloadManager.downloadStatus

    private val _downloadBannerEvent = MutableStateFlow<Song?>(null)
    val downloadBannerEvent: StateFlow<Song?> = _downloadBannerEvent.asStateFlow()

    fun dismissDownloadBanner() {
        _downloadBannerEvent.value = null
    }

    init {
        DatabaseManager.init(context)
        refreshDatabaseState()
        DownloadManager.resumeInterruptedDownloads(context)

        // Load initial YouTube home feed
        refreshHomeFeed()
        loadDeviceSongs()
        loadSearchHistory()

        // Load initial recommended songs
        viewModelScope.launch {
            val recs = repository.getRecommendedSongs()
            _recommendedSongs.value = recs
            if (_queue.value.isEmpty()) {
                _queue.value = recs
            }
        }

        // Refresh downloaded tracks whenever download status changes (started, downloading, or completed)
        viewModelScope.launch {
            downloadStatusFlow.collect {
                refreshDownloadedTracks()
            }
        }

        // Configure playback manager listener
        PlaybackManager.setOnStateChangeListener { playing, progress, duration, loading ->
            _isPlaying.value = playing
            _progressMs.value = progress
            _durationMs.value = duration
            _isTrackLoading.value = loading
        }

        // Configure auto-advance when a track ends
        PlaybackManager.setOnTrackEndedListener {
            playNext()
        }

        // Wire notification button callbacks to ViewModel actions
        PlaybackManager.setOnNextRequestedListener { playNext() }
        PlaybackManager.setOnPrevRequestedListener { playPrevious() }

        // Setup debounced search suggestion flow
        setupSearchDebounce()
    }

    private var activeSearchJob: Job? = null
    private var lastSearchedQuery: String = ""

    @OptIn(FlowPreview::class)
    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collect { query ->
                    val q = query.trim()
                    if (q.length >= 2) {
                        repository.getSuggestions(q).onSuccess { suggestions ->
                            _searchSuggestions.value = suggestions
                        }.onFailure {
                            Timber.e(it, "Failed to get suggestions")
                        }

                        if (lastSearchedQuery != q) {
                            activeSearchJob?.cancel()
                            activeSearchJob = viewModelScope.launch {
                                _isSearching.value = true
                                lastSearchedQuery = q
                                repository.searchSongs(q).onSuccess { songs ->
                                    _searchResults.value = songs
                                }.onFailure {
                                    Timber.e(it, "Search failed for: $q")
                                }
                                _isSearching.value = false
                            }
                        }
                    } else {
                        _searchSuggestions.value = emptyList()
                        _searchResults.value = emptyList()
                        lastSearchedQuery = ""
                    }
                }
        }
    }

    fun selectTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectLibraryCategory(category: String?) {
        _activeLibraryCategory.value = category
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            _searchSuggestions.value = emptyList()
            lastSearchedQuery = ""
        }
    }

    fun performSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        if (lastSearchedQuery == q && _searchResults.value.isNotEmpty()) return

        _searchQuery.value = q
        _searchSuggestions.value = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            DatabaseManager.addSearchHistory(q)
            _searchHistory.value = DatabaseManager.getSearchHistory()
        }

        activeSearchJob?.cancel()
        activeSearchJob = viewModelScope.launch {
            _isSearching.value = true
            lastSearchedQuery = q
            repository.searchSongs(q).onSuccess { songs ->
                _searchResults.value = songs
            }.onFailure {
                Timber.e(it, "Search failed for: $q")
            }
            _isSearching.value = false
        }
    }

    fun resolveMissingPlaylistCovers(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = DatabaseManager.getPlaylistSongs(playlistId)
            val songsToResolve = songs.filter { song ->
                song.coverUrl.isBlank() ||
                song.coverUrl == "defaultthumbnail" ||
                song.coverUrl == "null" ||
                song.id.startsWith("import") ||
                song.id.contains("spotify")
            }
            if (songsToResolve.isEmpty()) return@launch

            // 3-Worker Bounded Chunking: process 3 songs at a time to prevent socket/memory exhaustion & UI thrashing
            songsToResolve.chunked(3).forEach { chunk ->
                var chunkUpdated = false
                coroutineScope {
                    chunk.map { song ->
                        async {
                            val searchQuery = "${song.title} ${song.artist}".trim()
                            if (searchQuery.isBlank()) return@async
                            try {
                                val searchRes = com.metrolist.innertube.YouTube.search(
                                    searchQuery,
                                    com.metrolist.innertube.YouTube.SearchFilter.FILTER_SONG
                                ).getOrNull()
                                val matchedSong = searchRes?.items
                                    ?.filterIsInstance<com.metrolist.innertube.models.SongItem>()
                                    ?.firstOrNull()
                                if (matchedSong != null) {
                                    val realCover = matchedSong.thumbnail ?: ""
                                    if (realCover.isNotEmpty()) {
                                        DatabaseManager.updateImportedSongIdAndCover(
                                            song.id, matchedSong.id, realCover
                                        )
                                        chunkUpdated = true
                                    }
                                }
                            } catch (e: Throwable) {
                                Timber.e(e, "Error resolving cover for: ${song.title}")
                            }
                        }
                    }.forEach { it.await() }
                }
                if (chunkUpdated) {
                    triggerPlaylistUpdate()
                }
            }
        }
    }

    fun resolveMissingFavoritesCovers() {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = DatabaseManager.getAllFavorites()
            val songsToResolve = songs.filter { song ->
                song.coverUrl.isBlank() ||
                song.coverUrl == "defaultthumbnail" ||
                song.coverUrl == "null" ||
                song.id.startsWith("import") ||
                song.id.contains("spotify")
            }
            if (songsToResolve.isEmpty()) return@launch

            songsToResolve.chunked(3).forEach { chunk ->
                var chunkUpdated = false
                coroutineScope {
                    chunk.map { song ->
                        async {
                            val searchQuery = "${song.title} ${song.artist}".trim()
                            if (searchQuery.isBlank()) return@async
                            try {
                                val searchRes = com.metrolist.innertube.YouTube.search(
                                    searchQuery,
                                    com.metrolist.innertube.YouTube.SearchFilter.FILTER_SONG
                                ).getOrNull()
                                val matchedSong = searchRes?.items
                                    ?.filterIsInstance<com.metrolist.innertube.models.SongItem>()
                                    ?.firstOrNull()
                                if (matchedSong != null) {
                                    val realCover = matchedSong.thumbnail ?: ""
                                    if (realCover.isNotEmpty()) {
                                        DatabaseManager.updateFavoriteCover(
                                            song.id, matchedSong.id, realCover
                                        )
                                        chunkUpdated = true
                                    }
                                }
                            } catch (e: Throwable) {
                                Timber.e(e, "Error resolving favorite cover for: ${song.title}")
                            }
                        }
                    }.forEach { it.await() }
                }
                if (chunkUpdated) {
                    refreshFavorites()
                }
            }
        }
    }

    fun startImportToFavorites(songs: List<Song>, onComplete: ((added: Int, skipped: Int) -> Unit)? = null) {
        importJob?.cancel()
        importJob = viewModelScope.launch(Dispatchers.IO) {
            val total = songs.size
            var isCancelledFlag = false

            _importProgressState.value = ImportProgressState(
                destinationName = "Favorites",
                totalSongs = total,
                currentCount = 0,
                currentSongTitle = "",
                currentSongArtist = ""
            )

            val (added, skipped) = BackupRestoreManager.importParsedSongsToFavorites(
                songs = songs,
                onProgress = { current, totalCount, song ->
                    _importProgressState.value = ImportProgressState(
                        destinationName = "Favorites",
                        totalSongs = totalCount,
                        currentCount = current,
                        currentSongTitle = song.title,
                        currentSongArtist = song.artist
                    )
                },
                isCancelled = { isCancelledFlag }
            )

            _importProgressState.value = null
            refreshFavorites()
            if (onComplete != null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete.invoke(added, skipped)
                }
            }
        }
    }

    fun startImportToPlaylist(playlistName: String, songs: List<Song>, onComplete: ((result: BackupRestoreManager.PlaylistImportResult) -> Unit)? = null) {
        importJob?.cancel()
        importJob = viewModelScope.launch(Dispatchers.IO) {
            val total = songs.size
            var isCancelledFlag = false

            _importProgressState.value = ImportProgressState(
                destinationName = playlistName.ifBlank { "Imported Playlist" },
                totalSongs = total,
                currentCount = 0,
                currentSongTitle = "",
                currentSongArtist = ""
            )

            val res = BackupRestoreManager.importParsedSongsToPlaylist(
                playlistName = playlistName,
                songs = songs,
                onProgress = { current, totalCount, song ->
                    _importProgressState.value = ImportProgressState(
                        destinationName = playlistName.ifBlank { "Imported Playlist" },
                        totalSongs = totalCount,
                        currentCount = current,
                        currentSongTitle = song.title,
                        currentSongArtist = song.artist
                    )
                },
                isCancelled = { isCancelledFlag }
            )

            _importProgressState.value = null
            refreshPlaylists()
            if (onComplete != null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete.invoke(res)
                }
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _importProgressState.value = null
        refreshFavorites()
        refreshPlaylists()
    }

    fun loadSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _searchHistory.value = DatabaseManager.getSearchHistory()
        }
    }

    fun removeSearchHistoryItem(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseManager.removeSearchHistory(query)
            _searchHistory.value = DatabaseManager.getSearchHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseManager.clearSearchHistory()
            _searchHistory.value = emptyList()
        }
    }

    private fun playTrackHelper(song: Song) {
        val currentQueue = _queue.value
        val currentIndex = currentQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        if (currentQueue.isNotEmpty()) {
            PlaybackManager.updateQueue(context, currentQueue, currentIndex)
        }

        val mediaStoreContentUri = song.id.toLongOrNull()?.let {
            android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it).toString()
        }

        val localPath = when {
            song.id.startsWith("content://") || song.id.startsWith("/") -> song.id
            (try { java.io.File(song.id).exists() } catch (e: Exception) { false }) -> song.id
            mediaStoreContentUri != null -> mediaStoreContentUri
            else -> null
        }
        val isOffline = localPath != null

        PlaybackManager.play(context, song.id, localPath, song.title, song.artist, song.coverUrl)

        // Pre-fetch stream URL for the next song in the queue for smooth auto-play
        val nextIdx = currentQueue.indexOfFirst { it.id == song.id }
        if (nextIdx != -1 && nextIdx + 1 < currentQueue.size) {
            val nextTrack = currentQueue[nextIdx + 1]
            PlaybackManager.prefetchNextStreamUrl(context, nextTrack.id)
        }
    }

    private val _isFromRecentlyPlayedQueue = MutableStateFlow(false)

    fun playSong(song: Song, isFromRecentlyPlayed: Boolean = false) {
        _isFromRecentlyPlayedQueue.value = isFromRecentlyPlayed
        _transitionType.value = TrackTransitionType.NONE
        if (_currentTrack.value?.id == song.id) {
            triggerMaximizePlayer()
            return
        }
        _currentTrack.value = song
        if (!_queue.value.contains(song)) {
            _queue.value = _queue.value + song
        }
        playTrackHelper(song)
        triggerMaximizePlayer()
        // Update MediaStyle notification with track info
        PlaybackManager.updateNotificationMetadata(
            title = song.title,
            artist = song.artist,
            artUrl = song.coverUrl
        )
        // Persist to SQLite history and update Flow if not played from Recently Played screen
        if (!isFromRecentlyPlayed) {
            viewModelScope.launch(Dispatchers.IO) {
                DatabaseManager.addRecentlyPlayed(song)
                refreshRecentlyPlayed()
            }
        }
    }

    fun playSongFromList(song: Song, list: List<Song>, isFromRecentlyPlayed: Boolean = false) {
        _isFromRecentlyPlayedQueue.value = isFromRecentlyPlayed
        _transitionType.value = TrackTransitionType.NONE
        if (list.isNotEmpty()) {
            _queue.value = list
        }
        if (_currentTrack.value?.id == song.id) {
            triggerMaximizePlayer()
            return
        }
        _currentTrack.value = song
        playTrackHelper(song)
        triggerMaximizePlayer()
        // Update MediaStyle notification with track info
        PlaybackManager.updateNotificationMetadata(
            title = song.title,
            artist = song.artist,
            artUrl = song.coverUrl
        )
        
        if (!isFromRecentlyPlayed) {
            viewModelScope.launch(Dispatchers.IO) {
                DatabaseManager.addRecentlyPlayed(song)
                refreshRecentlyPlayed()
            }
        }
    }

    fun togglePlay() {
        val song = _currentTrack.value ?: return
        if (_isPlaying.value) {
            PlaybackManager.pause()
        } else {
            // Check if player is initialized / has a source, otherwise trigger play
            if (PlaybackManager.getCurrentPosition() > 0) {
                PlaybackManager.resume()
            } else {
                playTrackHelper(song)
            }
        }
    }

    fun stopPlaybackAndClear() {
        PlaybackManager.stopPlaybackAndService()
        _currentTrack.value = null
        _isPlaying.value = false
    }

    fun playNext(forceNext: Boolean = false) {
        _transitionType.value = TrackTransitionType.SLIDE_NEXT
        val current = _currentTrack.value
        
        var currentQueue = _queue.value

        // If queue has 1 or 0 songs, auto-populate queue from recommended/recent/device songs
        if (currentQueue.size <= 1) {
            val fallbackPool = (_recommendedSongs.value + _recentlyPlayed.value + _deviceSongs.value)
                .distinctBy { it.id }
                .filter { it.id != current?.id }
            if (fallbackPool.isNotEmpty()) {
                currentQueue = if (current != null) listOf(current) + fallbackPool else fallbackPool
                _queue.value = currentQueue
            }
        }

        if (currentQueue.isEmpty()) return

        val currentIndex = if (current != null) currentQueue.indexOfFirst { it.id == current.id } else -1

        // Repeat mode active -> only loop automatically on track end, but skip to next when manually invoked (forceNext)
        if (_repeatEnabled.value && current != null && !forceNext) {
            playTrackHelper(current)
            return
        }

        val nextIndex = when {
            _shuffleEnabled.value -> {
                val candidates = currentQueue.indices.filter { it != currentIndex }
                if (candidates.isNotEmpty()) candidates.random() else 0
            }
            currentIndex != -1 -> (currentIndex + 1) % currentQueue.size
            else -> 0
        }

        val nextSong = currentQueue[nextIndex]
        _currentTrack.value = nextSong
        playTrackHelper(nextSong)
        if (!_isFromRecentlyPlayedQueue.value) {
            viewModelScope.launch(Dispatchers.IO) {
                DatabaseManager.addRecentlyPlayed(nextSong)
                refreshRecentlyPlayed()
            }
        }
    }

    fun playPrevious(forcePrev: Boolean = false) {
        _transitionType.value = TrackTransitionType.SLIDE_PREV
        val current = _currentTrack.value
        var currentQueue = _queue.value
        if (currentQueue.size <= 1) {
            val fallbackPool = (_recommendedSongs.value + _recentlyPlayed.value + _deviceSongs.value)
                .distinctBy { it.id }
                .filter { it.id != current?.id }
            if (fallbackPool.isNotEmpty()) {
                currentQueue = if (current != null) listOf(current) + fallbackPool else fallbackPool
                _queue.value = currentQueue
            }
        }
        if (currentQueue.isEmpty()) return

        // If progress is > 3 seconds, restart the song instead of skipping back unless forced
        if (_progressMs.value > 3000 && !forcePrev) {
            PlaybackManager.seekTo(0)
            return
        }

        val currentIndex = if (current != null) currentQueue.indexOfFirst { it.id == current.id } else -1

        if (_repeatEnabled.value && current != null && !forcePrev) {
            playTrackHelper(current)
            return
        }

        val prevIndex = when {
            _shuffleEnabled.value -> {
                val candidates = currentQueue.indices.filter { it != currentIndex }
                if (candidates.isNotEmpty()) candidates.random() else 0
            }
            currentIndex != -1 -> if (currentIndex - 1 < 0) currentQueue.size - 1 else currentIndex - 1
            else -> 0
        }

        val prevSong = currentQueue[prevIndex]
        _currentTrack.value = prevSong
        playTrackHelper(prevSong)
        if (!_isFromRecentlyPlayedQueue.value) {
            viewModelScope.launch(Dispatchers.IO) {
                DatabaseManager.addRecentlyPlayed(prevSong)
                refreshRecentlyPlayed()
            }
        }
    }

    /** Returns the track that would play on the next playNext() call, without modifying state. */
    fun peekNextTrack(): Song? {
        val current = _currentTrack.value
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return null
        val currentIndex = if (current != null) currentQueue.indexOfFirst { it.id == current.id } else -1
        if (_shuffleEnabled.value) return null // Can't predict shuffle
        val nextIndex = if (currentIndex != -1) (currentIndex + 1) % currentQueue.size else 0
        return currentQueue[nextIndex]
    }

    /** Returns the track that would play on the next playPrevious() call, without modifying state. */
    fun peekPreviousTrack(): Song? {
        val current = _currentTrack.value
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return null
        val currentIndex = if (current != null) currentQueue.indexOfFirst { it.id == current.id } else -1
        if (_shuffleEnabled.value) return null // Can't predict shuffle
        val prevIndex = if (currentIndex != -1) {
            if (currentIndex - 1 < 0) currentQueue.size - 1 else currentIndex - 1
        } else 0
        return currentQueue[prevIndex]
    }

    fun seekTo(progress: Float) {
        val duration = _durationMs.value
        if (duration > 0) {
            val seekPos = (progress * duration).toLong()
            PlaybackManager.seekTo(seekPos)
        }
    }

    fun toggleShuffle(): Boolean {
        _shuffleEnabled.value = !_shuffleEnabled.value
        return _shuffleEnabled.value
    }

    fun toggleRepeat(): Boolean {
        _repeatEnabled.value = !_repeatEnabled.value
        return _repeatEnabled.value
    }

    fun toggleLikeSong(song: Song) {
        val currentLiked = _likedTrackIds.value.toMutableSet()
        val isLiked = currentLiked.contains(song.id)

        if (isLiked) {
            currentLiked.remove(song.id)
            DatabaseManager.removeFavorite(song.id)
        } else {
            currentLiked.add(song.id)
            DatabaseManager.addFavorite(
                song.id,
                song.title,
                song.artist,
                song.album ?: "",
                song.duration,
                song.coverUrl ?: ""
            )
        }
        _likedTrackIds.value = currentLiked
        refreshFavorites()
    }

    fun toggleLike(songId: String) {
        val currentLiked = _likedTrackIds.value.toMutableSet()
        val isLiked = currentLiked.contains(songId)
        
        if (isLiked) {
            currentLiked.remove(songId)
            DatabaseManager.removeFavorite(songId)
        } else {
            currentLiked.add(songId)
            val song = _recommendedSongs.value.find { it.id == songId }
                ?: _queue.value.find { it.id == songId }
                ?: _searchResults.value.find { it.id == songId }
                ?: _currentTrack.value?.takeIf { it.id == songId }
            
            if (song != null) {
                DatabaseManager.addFavorite(song.id, song.title, song.artist, song.album, song.duration, song.coverUrl)
            }
        }
        _likedTrackIds.value = currentLiked
        refreshFavorites()
    }


    fun refreshDatabaseState() {
        refreshPlaylists()
        refreshDownloadedTracks()
        refreshFavorites()
        refreshRecentlyPlayed()
    }

    fun refreshAllData() {
        refreshPlaylists()
        refreshDownloadedTracks()
        refreshFavorites()
        refreshRecentlyPlayed()
    }

    fun refreshPlaylists() {
        _playlists.value = DatabaseManager.getAllPlaylists()
    }

    fun refreshDownloadedTracks() {
        _downloadedTracks.value = DatabaseManager.getAllDownloads()
    }

    fun refreshFavorites() {
        val favorites = DatabaseManager.getAllFavorites()
        _favoriteTracks.value = favorites
        _likedTrackIds.value = favorites.map { it.id }.toSet()
    }

    fun refreshRecentlyPlayed() {
        _recentlyPlayed.value = DatabaseManager.getRecentlyPlayed()
    }

    fun loadDeviceSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceMedia = repository.getDeviceMediaSongs(context)
            _deviceSongs.value = deviceMedia
        }
    }

    fun playSongsList(songsList: List<Song>) {
        if (songsList.isEmpty()) return
        playSongFromList(songsList.first(), songsList)
    }

    fun addSongsToQueue(songsList: List<Song>) {
        val currentQueue = _queue.value.toMutableList()
        songsList.forEach { song ->
            if (!currentQueue.contains(song)) {
                currentQueue.add(song)
            }
        }
        _queue.value = currentQueue
    }

    fun createPlaylist(name: String, logoPath: String?): Boolean {
        val trimmed = name.trim()
        val exists = _playlists.value.any { it.name.trim().equals(trimmed, ignoreCase = true) }
        if (exists) {
            return false
        }
        DatabaseManager.createPlaylist(trimmed, logoPath)
        refreshPlaylists()
        return true
    }

    fun deletePlaylist(playlistId: Long) {
        DatabaseManager.deletePlaylist(playlistId)
        refreshPlaylists()
    }

    fun updatePlaylist(playlistId: Long, name: String, logoPath: String?) {
        DatabaseManager.updatePlaylist(playlistId, name, logoPath)
        refreshPlaylists()
    }

    fun savePlaylistCoverImage(uri: android.net.Uri): String? {
        return try {
            val dir = java.io.File(context.filesDir, "playlist_covers").also { it.mkdirs() }
            val file = java.io.File(dir, "cover_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save playlist cover image")
            null
        }
    }

    fun playlistExists(name: String, excludeId: Long? = null): Boolean {
        return DatabaseManager.playlistExists(name, excludeId)
    }

    fun saveToPublicDownloads(song: Song): Boolean {
        return DownloadManager.saveToPublicDownloads(context, song)
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        DatabaseManager.addSongToPlaylist(playlistId, song.id, song.title, song.artist, song.album, song.duration, song.coverUrl)
        triggerPlaylistUpdate()
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        DatabaseManager.removeSongFromPlaylist(playlistId, songId)
        triggerPlaylistUpdate()
    }

    fun getPlaylistSongs(playlistId: Long): List<Song> {
        return DatabaseManager.getPlaylistSongs(playlistId)
    }

    fun downloadTrack(song: Song) {
        DownloadManager.downloadTrack(context, song)
        _downloadBannerEvent.value = song
    }

    fun cancelDownload(songId: String) {
        DownloadManager.cancelDownload(context, songId)
        refreshDownloadedTracks()
    }

    fun deleteDownloadedTrack(songId: String) {
        DownloadManager.deleteDownloadedTrack(context, songId)
        refreshDownloadedTracks()
    }    private val prefs = context.getSharedPreferences("metrolist_prefs", android.content.Context.MODE_PRIVATE)

    private val _deviceMediaFolders = MutableStateFlow<Set<String>>(
        prefs.getStringSet("device_media_folders", setOf("Music", "Download", "Downloads")) ?: setOf("Music", "Download", "Downloads")
    )
    val deviceMediaFolders: StateFlow<Set<String>> = _deviceMediaFolders.asStateFlow()

    fun addDeviceMediaFolder(folderName: String) {
        val cleaned = folderName.trim().trim('/')
        if (cleaned.isNotEmpty()) {
            val updated = _deviceMediaFolders.value + cleaned
            _deviceMediaFolders.value = updated
            prefs.edit().putStringSet("device_media_folders", updated).apply()
            scanDeviceSongs()
        }
    }

    fun removeDeviceMediaFolder(folderName: String) {
        val updated = _deviceMediaFolders.value - folderName
        _deviceMediaFolders.value = updated
        prefs.edit().putStringSet("device_media_folders", updated).apply()
        scanDeviceSongs()
    }

    fun shouldShowDeviceMediaNotice(): Boolean {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) return true

        val shown = prefs.getBoolean("device_media_notice_shown", false)
        if (!shown) {
            prefs.edit().putBoolean("device_media_notice_shown", true).apply()
            return true
        }
        return false
    }

    fun scanDeviceSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<Song>()
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ARTIST,
                android.provider.MediaStore.Audio.Media.ALBUM,
                android.provider.MediaStore.Audio.Media.DURATION,
                android.provider.MediaStore.Audio.Media.DATA
            )
            val selection = "${android.provider.MediaStore.Audio.Media.IS_MUSIC} != 0"
            try {
                val allowedFolders = _deviceMediaFolders.value.map { it.lowercase() }
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.ALBUM)
                    val durCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.DATA)
                    
                    while (cursor.moveToNext()) {
                        val idVal = cursor.getLong(idCol).toString()
                        val titleVal = cursor.getString(titleCol) ?: "Unknown Track"
                        val artistVal = cursor.getString(artistCol) ?: "Unknown Artist"
                        val durationVal = (cursor.getLong(durCol) / 1000).toInt()
                        val pathVal = cursor.getString(dataCol) ?: ""
                        
                        val pathLower = pathVal.lowercase()
                        val matchesFolder = allowedFolders.any { folder ->
                            pathLower.contains("/${folder}/") || pathLower.endsWith("/${folder}") || pathLower.contains("/${folder}")
                        }

                        if (matchesFolder) {
                            val parentFolder = if (pathVal.isNotEmpty()) {
                                java.io.File(pathVal).parentFile?.name ?: "Music"
                            } else {
                                "Music"
                            }

                            list.add(
                                Song(
                                    id = idVal,
                                    title = titleVal,
                                    artist = artistVal,
                                    album = parentFolder,
                                    duration = durationVal,
                                    coverUrl = "defaultthumbnail"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "scanDeviceSongs failed")
            }
            _deviceSongs.value = list
        }
    }
    override fun onCleared() {
        super.onCleared()
        PlaybackManager.release()
    }
}
