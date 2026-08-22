package com.snitrix.snitify.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.snitrix.snitify.R
import com.snitrix.snitify.data.db.Playlist
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.DownloadProgressWidget
import com.snitrix.snitify.ui.component.PlaylistCollageCover
import com.snitrix.snitify.ui.component.RowVisualizer
import com.snitrix.snitify.ui.component.TrackListSkeleton
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenSearch: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentPlaylist by remember(playlist) { mutableStateOf(playlist) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isInitialLoad by remember { mutableStateOf(true) }
    var isResolvingCovers by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showDeletePlaylistConfirm by remember { mutableStateOf(false) }
    var showPlaylistOptionsBottomSheet by remember { mutableStateOf(false) }
    var songToDeleteDownload by remember { mutableStateOf<Song?>(null) }
    var songToRemoveFromPlaylist by remember { mutableStateOf<Song?>(null) }
    var selectedSongForBottomSheet by remember { mutableStateOf<Song?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var showSearchModal by remember { mutableStateOf(false) }
    var showGlobalOnlineSearch by remember { mutableStateOf(false) }
    var showAddSourcePicker by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }
    var showFavoritesPicker by remember { mutableStateOf(false) }

    val deviceSongs by viewModel.deviceSongs.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()

    val prefs = remember(context) { context.getSharedPreferences("snitify_sort_prefs", android.content.Context.MODE_PRIVATE) }
    var sortOption by remember(currentPlaylist.id) {
        mutableStateOf(prefs.getString("sort_playlist_${currentPlaylist.id}", "adding_time_new_old") ?: "adding_time_new_old")
    }

    val filteredSongs = remember(songs, sortOption) {
        when (sortOption) {
            "adding_time_new_old" -> songs
            "adding_time_old_new" -> songs.reversed()
            "title_az" -> {
                songs.sortedWith { s1, s2 ->
                    val t1 = s1.title.dropWhile { !it.isLetter() }
                    val t2 = s2.title.dropWhile { !it.isLetter() }
                    t1.compareTo(t2, ignoreCase = true)
                }
            }
            "title_za" -> {
                songs.sortedWith { s1, s2 ->
                    val t1 = s1.title.dropWhile { !it.isLetter() }
                    val t2 = s2.title.dropWhile { !it.isLetter() }
                    t2.compareTo(t1, ignoreCase = true)
                }
            }
            else -> songs
        }
    }

    val searchFilteredSongs = remember(filteredSongs, searchQuery) {
        if (searchQuery.isBlank()) filteredSongs
        else filteredSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val playlistUpdateTrigger by viewModel.playlistUpdateTrigger.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val downloadStatusMap by viewModel.downloadStatusFlow.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val likedTrackIds by viewModel.likedTrackIds.collectAsState()

    val isPlaylistPlaying = remember(songs, currentTrack, isPlaying) {
        songs.any { it.id == currentTrack?.id } && isPlaying
    }

    val playlistsList by viewModel.playlists.collectAsState()
    LaunchedEffect(playlistsList) {
        playlistsList.find { it.id == currentPlaylist.id }?.let {
            currentPlaylist = it
        }
    }


    // Load songs on first open, then kick off background cover resolution
    LaunchedEffect(currentPlaylist.id) {
        isInitialLoad = true
        songs = viewModel.getPlaylistSongs(currentPlaylist.id)
        isInitialLoad = false
        val hasUnresolved = songs.any {
            it.coverUrl.isBlank() || it.coverUrl == "defaultthumbnail" ||
            it.coverUrl == "null" || it.id.startsWith("import") || it.id.contains("spotify")
        }
        if (hasUnresolved) {
            isResolvingCovers = true
            viewModel.resolveMissingPlaylistCovers(currentPlaylist.id)
        }
    }

    // Reload songs from DB whenever a cover gets resolved (per-song live update)
    LaunchedEffect(playlistUpdateTrigger) {
        if (playlistUpdateTrigger > 0) {
            songs = viewModel.getPlaylistSongs(currentPlaylist.id)
            val stillUnresolved = songs.any {
                it.coverUrl.isBlank() || it.coverUrl == "defaultthumbnail" ||
                it.coverUrl == "null" || it.id.startsWith("import") || it.id.contains("spotify")
            }
            if (!stillUnresolved) isResolvingCovers = false
        }
    }

    val appColors = LocalAppThemeColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "playlistShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, appColors.background),
        startY = 0f,
        endY = 450f
    )

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 440.dp.toPx() }

    val rawScrollFraction by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (lazyListState.firstVisibleItemScrollOffset.toFloat() / collapseThresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    val collapseFraction = FastOutSlowInEasing.transform(rawScrollFraction)

    val animatedArtworkSize = lerp(220.dp, 52.dp, collapseFraction)
    val animatedCornerRadius = lerp(16.dp, 8.dp, collapseFraction)
    val animatedOpacity = lerp(1.0f, 0.8f, collapseFraction)
    val animatedTranslateY = lerp(0f, -150f, collapseFraction)
    val toolbarAlpha = collapseFraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(topGradient)
        )

        // Sticky Top App Bar Toolbar — hidden when any search overlay is active
        if (!showGlobalOnlineSearch && !showSearchModal) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f)
                    .background(BackgroundBlack.copy(alpha = toolbarAlpha))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (collapseFraction > 0.4f) {
                        Text(
                            text = currentPlaylist.name,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    } else if (songs.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(CircleShape)
                                .background(BackgroundBlack.copy(alpha = 0.5f))
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable {
                                    showGlobalOnlineSearch = false
                                    showSearchModal = true
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Search songs",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(52.dp))
                    }
                }
            }
        }

        // Main Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                // Item 0: Hero Artwork & Playlist Title & Action Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 4-Song Collage Cover Artwork
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = animatedTranslateY
                                    alpha = animatedOpacity
                                }
                                .size(animatedArtworkSize)
                                .clip(RoundedCornerShape(animatedCornerRadius))
                                .border(1.dp, GlassBorder, RoundedCornerShape(animatedCornerRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlaylistCollageCover(
                                logoPath = currentPlaylist.logoPath,
                                songs = songs,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentPlaylist.name,
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = animatedTranslateY * 0.5f
                                    alpha = lerp(1.0f, 0.0f, collapseFraction * 1.2f).coerceIn(0f, 1f)
                                }
                                .padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Bar: Share | Options 3-Dots | Shuffle | Floating Green Play/Pause
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                Toast.makeText(context, "Share link copied", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(onClick = { showPlaylistOptionsBottomSheet = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vertical),
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(onClick = {
                                if (songs.isNotEmpty()) {
                                    val shuffled = songs.shuffled()
                                    viewModel.playSongFromList(shuffled.first(), shuffled)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_shuffle),
                                    contentDescription = "Shuffle",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(appColors.primaryAccent)
                                    .clickable {
                                        val isCurrentTrackInPlaylist = songs.any { it.id == currentTrack?.id }
                                        if (isCurrentTrackInPlaylist) {
                                            viewModel.togglePlay()
                                        } else if (filteredSongs.isNotEmpty()) {
                                            viewModel.playSongFromList(filteredSongs.first(), filteredSongs)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isPlaylistPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = if (isPlaylistPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .padding(start = if (isPlaylistPlaying) 0.dp else 3.dp)
                                )
                            }
                        }

                    }
                }

                // Song List Items — show skeleton while initial load or resolving all covers, then real content
                if (isInitialLoad || (isResolvingCovers && songs.all { it.coverUrl.isBlank() || it.coverUrl == "defaultthumbnail" || it.id.startsWith("import") || it.id.contains("spotify") })) {
                    item {
                        TrackListSkeleton(
                            itemCount = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (songs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = {
                                    showSearchModal = false
                                    showGlobalOnlineSearch = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_plus),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Songs to Playlist", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (searchFilteredSongs.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No matching songs found in this playlist",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching for a different song title or artist name",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(searchFilteredSongs, key = { "pl_song_${it.id}" }) { song ->
                        val isCurrentTrack = currentTrack?.id == song.id
                        val dlProgress = downloadStatusMap[song.id]

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.playSongFromList(song, searchFilteredSongs)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF262626))
                                        .clickable {
                                            if (isCurrentTrack) {
                                                viewModel.togglePlay()
                                            } else {
                                                viewModel.playSongFromList(song, searchFilteredSongs)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isSongUnresolved = song.coverUrl.isBlank() ||
                                            song.coverUrl == "defaultthumbnail" ||
                                            song.coverUrl == "null" ||
                                            song.id.startsWith("import") ||
                                            song.id.contains("spotify")

                                    if (isSongUnresolved) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                        )
                                    } else {
                                        SubcomposeAsyncImage(
                                            model = song.finalCover,
                                            contentDescription = song.title,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            loading = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                                )
                                            },
                                            success = {
                                                SubcomposeAsyncImageContent()
                                            },
                                            error = {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                                )
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                        com.snitrix.snitify.ui.component.DownloadThumbnailOverlay(progressPercent = dlProgress.percent)
                                    } else if (isCurrentTrack) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (isPlaying) appColors.primaryAccent.copy(alpha = 0.55f)
                                                    else Color.Black.copy(alpha = 0.45f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                                ),
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrentTrack) appColors.primaryAccent else TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                        Text(
                                            text = "Downloading... ${dlProgress.percent}%",
                                            color = appColors.primaryAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Text(
                                            text = song.artist,
                                            color = TextSecondary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isCurrentTrack && (dlProgress == null || (dlProgress.status != DownloadStatus.DOWNLOADING && dlProgress.status != DownloadStatus.PENDING))) {
                                    RowVisualizer(isPlaying = isPlaying, color = appColors.primaryAccent)
                                }

                                IconButton(onClick = { selectedSongForBottomSheet = song }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_more_vertical),
                                        contentDescription = "Song Options",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Spotify 3-Dot Options Bottom Sheet Menu
        if (showPlaylistOptionsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylistOptionsBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF1E1E1E),
                scrimColor = Color.Black.copy(alpha = 0.5f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF555555))
                    )
                }
            ) {
                com.snitrix.snitify.ui.component.TransparentSystemBarsForDialog()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlaylistCollageCover(
                                logoPath = currentPlaylist.logoPath,
                                songs = songs,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentPlaylist.name,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "by Snitify • Private playlist",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistOptionsBottomSheet = false
                                Toast.makeText(context, "Feature coming soon", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Share", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistOptionsBottomSheet = false
                                showSearchModal = false
                                showGlobalOnlineSearch = true
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Add to this playlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistOptionsBottomSheet = false
                                showEditDialog = true
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Edit playlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistOptionsBottomSheet = false
                                showSortDialog = true
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Sort playlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }



                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPlaylistOptionsBottomSheet = false
                                showDeletePlaylistConfirm = true
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Delete playlist", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Dedicated Song 3-Dot Options Bottom Sheet Menu
        if (selectedSongForBottomSheet != null) {
            val targetSong = selectedSongForBottomSheet!!
            ModalBottomSheet(
                onDismissRequest = { selectedSongForBottomSheet = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF1E1E1E),
                scrimColor = Color.Black.copy(alpha = 0.5f),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF555555))
                    )
                }
            ) {
                com.snitrix.snitify.ui.component.TransparentSystemBarsForDialog()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Header Row: Song Thumbnail + Title + Artist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF262626)),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = targetSong.finalCover,
                                contentDescription = targetSong.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                    )
                                },
                                success = { SubcomposeAsyncImageContent() },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetSong.title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = targetSong.artist,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Button 1: Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSongForBottomSheet = null
                                Toast.makeText(context, "Feature coming soon", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Share", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    // Button 1.5: Add to / Remove from favorites
                    val isLiked = likedTrackIds.contains(targetSong.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val songToFav = targetSong
                                selectedSongForBottomSheet = null
                                viewModel.toggleLikeSong(songToFav)
                                Toast.makeText(
                                    context,
                                    if (isLiked) "Removed from Favorites" else "Added to Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLiked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_heart_broken),
                                contentDescription = "Remove from favorites",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Add to favorites",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = if (isLiked) "Remove from favorites" else "Add to favorites",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Button 2: Remove from Playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val s = targetSong
                                selectedSongForBottomSheet = null
                                songToRemoveFromPlaylist = s
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_remove_from_playlist),
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text("Remove from playlist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    // Button 3: Dynamic Download / Cancel / Delete Action
                    val targetDlProgress = downloadStatusMap[targetSong.id]
                    val isTargetDownloading = targetDlProgress?.status == DownloadStatus.DOWNLOADING || targetDlProgress?.status == DownloadStatus.PENDING
                    val isTargetDownloaded = downloadedTracks.any { it.id == targetSong.id } || targetDlProgress?.status == DownloadStatus.COMPLETED

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSongForBottomSheet = null
                                when {
                                    isTargetDownloading -> {
                                        viewModel.cancelDownload(targetSong.id)
                                        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                    isTargetDownloaded -> {
                                        songToDeleteDownload = targetSong
                                    }
                                    else -> {
                                        viewModel.downloadTrack(targetSong)
                                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = when {
                                    isTargetDownloading || isTargetDownloaded -> R.drawable.ic_delete
                                    else -> R.drawable.ic_download
                                }
                            ),
                            contentDescription = when {
                                isTargetDownloading -> "Cancel Download"
                                isTargetDownloaded -> "Delete Download"
                                else -> "Download"
                            },
                            tint = when {
                                isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5252)
                                else -> appColors.primaryAccent
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = when {
                                isTargetDownloading -> "Cancel Download"
                                isTargetDownloaded -> "Delete Download"
                                else -> "Download"
                            },
                            color = when {
                                isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5252)
                                else -> TextPrimary
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Dialog 0: Delete Download Confirmation
        if (songToDeleteDownload != null) {
            val targetSong = songToDeleteDownload!!
            AlertDialog(
                onDismissRequest = { songToDeleteDownload = null },
                containerColor = BackgroundBlack,
                title = {
                    Text(
                        text = "Delete Download",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${targetSong.title}' from your downloads?",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteDownloadedTrack(targetSong.id)
                            songToDeleteDownload = null
                            Toast.makeText(context, "Download deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { songToDeleteDownload = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 1: Delete Song Confirmation
        if (songToRemoveFromPlaylist != null) {
            val songToDelete = songToRemoveFromPlaylist!!
            AlertDialog(
                onDismissRequest = { songToRemoveFromPlaylist = null },
                containerColor = BackgroundBlack,
                title = { Text("Remove Song", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove '${songToDelete.title}' from this playlist?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeSongFromPlaylist(currentPlaylist.id, songToDelete.id)
                            songs = viewModel.getPlaylistSongs(currentPlaylist.id)
                            songToRemoveFromPlaylist = null
                            Toast.makeText(context, "Song removed", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Remove", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { songToRemoveFromPlaylist = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 2: Edit Playlist Info Sheet (Matching Screenshot 1)
        if (showEditDialog) {
            com.snitrix.snitify.ui.component.EditPlaylistBottomSheet(
                playlist = currentPlaylist,
                onSave = { newName, newLogoPath ->
                    if (newName.isNotBlank()) {
                        com.snitrix.snitify.data.db.DatabaseManager.updatePlaylist(currentPlaylist.id, newName, newLogoPath)
                        currentPlaylist = currentPlaylist.copy(name = newName, logoPath = newLogoPath)
                        viewModel.refreshPlaylists()
                        Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeletePlaylist = {
                    showDeletePlaylistConfirm = true
                },
                onDismiss = { showEditDialog = false }
            )
        }

        // Dialog 3: Delete Playlist Confirm
        if (showDeletePlaylistConfirm) {
            AlertDialog(
                onDismissRequest = { showDeletePlaylistConfirm = false },
                containerColor = BackgroundBlack,
                title = { Text("Delete Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete '${currentPlaylist.name}'?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            com.snitrix.snitify.data.db.DatabaseManager.deletePlaylist(currentPlaylist.id)
                            viewModel.refreshPlaylists()
                            showDeletePlaylistConfirm = false
                            onBack()
                            Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeletePlaylistConfirm = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 4: Sort Playlist Sheet (Matching Screenshot 2)
        if (showSortDialog) {
            com.snitrix.snitify.ui.component.SortByBottomSheet(
                selectedKey = sortOption,
                onOptionSelected = {
                    sortOption = it
                    prefs.edit().putString("sort_playlist_${currentPlaylist.id}", it).apply()
                },
                onDismiss = { showSortDialog = false }
            )
        }

        // Dialog 5: Multi-Source Add Songs Picker
        if (showAddSourcePicker) {
            AlertDialog(
                onDismissRequest = { showAddSourcePicker = false },
                containerColor = BackgroundBlack,
                title = { Text("Add Songs to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select where to add songs from:", color = TextSecondary, fontSize = 13.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Source 1: Device Media
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    showAddSourcePicker = false
                                    showDevicePicker = true
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_library),
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Device Media", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Add local audio files stored on device", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // Source 2: My Favorites
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    showAddSourcePicker = false
                                    showFavoritesPicker = true
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_heart_filled),
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("My Favorites", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Add songs from your Liked collection", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        // Source 3: Online Search
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    showAddSourcePicker = false
                                    onOpenSearch(currentPlaylist.id)
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = appColors.primaryAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Online Search", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Search YouTube Music catalog online", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    Button(
                        onClick = { showAddSourcePicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 6: Picker for Device Local Songs
        if (showDevicePicker) {
            AlertDialog(
                onDismissRequest = { showDevicePicker = false },
                containerColor = BackgroundBlack,
                title = { Text("Add Device Songs", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        if (deviceSongs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No local device audio files found.", color = TextSecondary, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(deviceSongs, key = { track -> "dev_${track.id}" }) { track ->
                                    val isAlreadyAdded = songs.any { it.id == track.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = isAlreadyAdded == false) {
                                                viewModel.addSongToPlaylist(currentPlaylist.id, track)
                                                songs = viewModel.getPlaylistSongs(currentPlaylist.id)
                                                viewModel.triggerPlaylistUpdate()
                                                Toast.makeText(context, "Added '${track.title}'", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                color = if (isAlreadyAdded) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = track.artist,
                                                color = TextSecondary.copy(alpha = if (isAlreadyAdded) 0.4f else 0.8f),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isAlreadyAdded) {
                                            Text("✓ Added", color = appColors.primaryAccent.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(painter = painterResource(id = R.drawable.ic_plus), contentDescription = "Add", tint = appColors.primaryAccent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDevicePicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }

        // Dialog 7: Picker for Favorites Songs
        if (showFavoritesPicker) {
            AlertDialog(
                onDismissRequest = { showFavoritesPicker = false },
                containerColor = BackgroundBlack,
                title = { Text("Add Favorites Songs", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        if (favorites.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No songs in Favorites yet.", color = TextSecondary, fontSize = 14.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(favorites, key = { track -> "fav_${track.id}" }) { track ->
                                    val isAlreadyAdded = songs.any { it.id == track.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = isAlreadyAdded == false) {
                                                viewModel.addSongToPlaylist(currentPlaylist.id, track)
                                                songs = viewModel.getPlaylistSongs(currentPlaylist.id)
                                                viewModel.triggerPlaylistUpdate()
                                                Toast.makeText(context, "Added '${track.title}'", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 10.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                color = if (isAlreadyAdded) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = track.artist,
                                                color = TextSecondary.copy(alpha = if (isAlreadyAdded) 0.4f else 0.8f),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isAlreadyAdded) {
                                            Text("✓ Added", color = appColors.primaryAccent.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(painter = painterResource(id = R.drawable.ic_plus), contentDescription = "Add", tint = appColors.primaryAccent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFavoritesPicker = false },
                        colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            )
        }

        // Dialog 8: Dedicated In-Playlist Search Modal
        if (!showGlobalOnlineSearch) {
            com.snitrix.snitify.ui.component.InPlaylistSearchModal(
                visible = showSearchModal,
                playlistSongs = songs,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onDismiss = { showSearchModal = false },
                onTrackSelect = { song, list ->
                    viewModel.playSongFromList(song, list)
                },
                playlistId = currentPlaylist.id,
                viewModel = viewModel
            )
        }

        // Dialog 9: Global Online Search Screen for adding songs to playlist
        if (showGlobalOnlineSearch) {
            com.snitrix.snitify.ui.screens.SearchScreen(
                viewModel = viewModel,
                mode = com.snitrix.snitify.ui.screens.SearchScreenMode.ADD_TO_PLAYLIST,
                playlistId = currentPlaylist.id,
                onDismiss = {
                    showGlobalOnlineSearch = false
                    songs = viewModel.getPlaylistSongs(currentPlaylist.id)
                    viewModel.triggerPlaylistUpdate()
                }
            )
        }
    }
}
