package com.snitrix.snitify.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.DownloadProgressWidget
import com.snitrix.snitify.ui.component.InPlaylistSearchModal
import com.snitrix.snitify.ui.component.RowVisualizer
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackListScreen(
    title: String,
    tracks: List<Song>,
    currentTrack: Song?,
    onTrackSelect: (Song, List<Song>) -> Unit,
    onBack: () -> Unit,
    onDeleteTrack: ((Song) -> Unit)? = null,
    viewModel: MusicViewModel? = null,
    onNavigateToDownloads: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppThemeColors.current
    val context = LocalContext.current

    val isPlaying by viewModel?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val downloadStatusMap by viewModel?.downloadStatusFlow?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }
    val downloadedTracks by viewModel?.downloadedTracks?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val userPlaylists by viewModel?.playlists?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val isLocalCategory = title.equals("Favorites", ignoreCase = true) ||
            title.equals("Downloads", ignoreCase = true) ||
            title.equals("Downloaded Songs", ignoreCase = true) ||
            title.equals("Liked Songs", ignoreCase = true)

    var displayTracks by remember(tracks) { mutableStateOf(tracks) }
    var isLoadingFeed by remember { mutableStateOf(tracks.isEmpty() && viewModel != null && !isLocalCategory) }

    LaunchedEffect(title, tracks) {
        if (tracks.isNotEmpty()) {
            displayTracks = tracks
            isLoadingFeed = false
        } else if (viewModel != null && !isLocalCategory) {
            isLoadingFeed = true
            displayTracks = viewModel.fetchFeedForQuery(title)
            isLoadingFeed = false
        } else {
            isLoadingFeed = false
        }
    }

    var isSearchActive by remember { mutableStateOf(false) }

    var selectedSongForBottomSheet by remember { mutableStateOf<Song?>(null) }
    var songToDeleteDownload by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistModalForSong by remember { mutableStateOf<Song?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar with Back, Title, and Search button at top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
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

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Search Button at top right
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (isLoadingFeed) {
                TrackListSkeleton()
            } else if (displayTracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in this list.",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(displayTracks, key = { index, song -> "tl_${title}_${song.id}_$index" }) { _, song ->
                        val isCurrentTrack = currentTrack?.id == song.id
                        val dlProgress = downloadStatusMap[song.id]
                        val isResolvingCover = song.coverUrl.isBlank() || song.id.startsWith("spotify") || song.id.startsWith("import")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (isCurrentTrack && viewModel != null) {
                                        viewModel.togglePlay()
                                    } else {
                                        onTrackSelect(song, displayTracks)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Song Thumbnail with Interactive Play / Pause / Spinner Overlay Button
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF222222))
                                    .clickable {
                                        if (isCurrentTrack) {
                                            viewModel?.togglePlay()
                                        } else {
                                            onTrackSelect(song, displayTracks)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isResolvingCover && song.coverUrl.isBlank()) {
                                    val transition = rememberInfiniteTransition(label = "cover_shimmer")
                                    val alpha by transition.animateFloat(
                                        initialValue = 0.25f,
                                        targetValue = 0.65f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF333333).copy(alpha = alpha))
                                    )
                                } else {
                                    AsyncImage(
                                        model = song.finalCover,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                    com.snitrix.snitify.ui.component.DownloadThumbnailOverlay(progressPercent = dlProgress.percent)
                                } else {
                                    // Interactive Play / Pause Badge Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                if (isCurrentTrack && isPlaying) appColors.primaryAccent.copy(alpha = 0.55f)
                                                else Color.Black.copy(alpha = 0.35f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (isCurrentTrack && isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                            ),
                                            contentDescription = if (isCurrentTrack && isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Song Details Title & Artist
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

                            // 3-Dot Options Button
                            IconButton(onClick = { selectedSongForBottomSheet = song }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vertical),
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Black & White Search Modal (Identical to Playlist Search)
        if (isSearchActive) {
            InPlaylistSearchModal(
                visible = isSearchActive,
                playlistSongs = displayTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onDismiss = { isSearchActive = false },
                onTrackSelect = { song, list ->
                    onTrackSelect(song, list)
                }
            )
        }

        // Song 3-Dot Options ModalBottomSheet (Play, Share, Add to Playlist, Download)
        if (selectedSongForBottomSheet != null) {
            val targetSong = selectedSongForBottomSheet!!
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { selectedSongForBottomSheet = null },
                sheetState = sheetState,
                containerColor = BackgroundBlack,
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF222222)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = targetSong.finalCover,
                                contentDescription = targetSong.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = targetSong.title,
                                color = TextPrimary,
                                fontSize = 17.sp,
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
                    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 1: Play
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSongForBottomSheet = null
                                onTrackSelect(targetSong, displayTracks)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play),
                            contentDescription = "Play",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Play",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action 2: Share (Correct Vector Share Icon & Feature Coming Soon Toast)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSongForBottomSheet = null
                                Toast.makeText(context, "Feature coming soon", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Share",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action 3: Add to playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val songToAddToPl = targetSong
                                selectedSongForBottomSheet = null
                                showAddToPlaylistModalForSong = songToAddToPl
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_plus),
                            contentDescription = "Add to playlist",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Add to playlist",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action 3.5: Add to / Remove from favorites
                    val likedIds by viewModel?.likedTrackIds?.collectAsState() ?: remember { mutableStateOf(emptySet()) }
                    val isLiked = likedIds.contains(targetSong.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val songToFav = targetSong
                                selectedSongForBottomSheet = null
                                viewModel?.toggleLikeSong(songToFav)
                                Toast.makeText(
                                    context,
                                    if (isLiked) "Removed from Favorites" else "Added to Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
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
                                imageVector = androidx.compose.material.icons.Icons.Default.FavoriteBorder,
                                contentDescription = "Add to favorites",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (isLiked) "Remove from favorites" else "Add to favorites",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Action 4: Dynamic Download / Cancel / Delete Action
                    val targetDlProgress = downloadStatusMap[targetSong.id]
                    val isTargetDownloading = targetDlProgress?.status == DownloadStatus.DOWNLOADING || targetDlProgress?.status == DownloadStatus.PENDING
                    val isTargetDownloaded = downloadedTracks.any { it.id == targetSong.id } || targetDlProgress?.status == DownloadStatus.COMPLETED

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedSongForBottomSheet = null
                                when {
                                    isTargetDownloading -> {
                                        viewModel?.cancelDownload(targetSong.id)
                                        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                    isTargetDownloaded -> {
                                        songToDeleteDownload = targetSong
                                    }
                                    else -> {
                                        viewModel?.downloadTrack(targetSong)
                                        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
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
                                isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5555)
                                else -> appColors.primaryAccent
                            },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = when {
                                isTargetDownloading -> "Cancel Download"
                                isTargetDownloaded -> "Delete Download"
                                else -> "Download"
                            },
                            color = when {
                                isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5555)
                                else -> TextPrimary
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Reduced bottom spacer to eliminate extra space below Download button
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Delete Download Confirmation Dialog
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
                            viewModel?.deleteDownloadedTrack(targetSong.id)
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

        // Add to Playlist Picker Dialog Modal
        if (showAddToPlaylistModalForSong != null) {
            val targetSong = showAddToPlaylistModalForSong!!

            AlertDialog(
                onDismissRequest = { showAddToPlaylistModalForSong = null },
                containerColor = BackgroundBlack,
                title = {
                    Text(
                        text = "Add to Playlist",
                        color = TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        if (userPlaylists.isEmpty()) {
                            Text(
                                text = "No playlists found. Create a playlist first!",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(userPlaylists, key = { pl -> pl.id }) { pl ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                com.snitrix.snitify.data.db.DatabaseManager.addSongToPlaylist(
                                                    pl.id,
                                                    targetSong.id,
                                                    targetSong.title,
                                                    targetSong.artist,
                                                    targetSong.album ?: "",
                                                    targetSong.duration,
                                                    targetSong.coverUrl ?: ""
                                                )
                                                viewModel?.refreshPlaylists()
                                                showAddToPlaylistModalForSong = null
                                                Toast.makeText(context, "Added '${targetSong.title}' to '${pl.name}'", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_library),
                                            contentDescription = null,
                                            tint = appColors.primaryAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = pl.name,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddToPlaylistModalForSong = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun TrackListSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 160.dp, top = 8.dp)
    ) {
        items(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222222).copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222222).copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF1A1A1A).copy(alpha = alpha))
                    )
                }
            }
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}
