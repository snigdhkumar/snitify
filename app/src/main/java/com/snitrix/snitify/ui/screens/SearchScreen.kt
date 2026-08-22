package com.snitrix.snitify.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.DownloadProgress
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.DownloadProgressWidget
import com.snitrix.snitify.ui.component.RowVisualizer
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel

enum class SearchScreenMode {
    GLOBAL,
    ADD_TO_PLAYLIST
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    mode: SearchScreenMode,
    playlistId: Long?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isPlayerOpen: Boolean = false
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val userPlaylists by viewModel.playlists.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var selectedSongForBottomSheet by remember { mutableStateOf<Song?>(null) }
    var songToDeleteDownload by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistModalForSong by remember { mutableStateOf<Song?>(null) }
    val downloadStatusMap by viewModel.downloadStatusFlow.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length)))
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length))
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BackHandler(enabled = !isPlayerOpen, onBack = onDismiss)

    val appColors = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current

    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, Color.Transparent),
        startY = 0f,
        endY = 300f
    )

    val onSearchTrigger = remember {
        { q: String ->
            if (q.isNotBlank()) {
                viewModel.performSearch(q)
                focusManager.clearFocus()
            }
        }
    }

    // State for long-press remove dialog
    var historyItemToRemove by remember { mutableStateOf<String?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(topGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── YouTube-style Search Header Bar ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back/Close Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262626))
                        .border(1.dp, GlassBorder, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Pill search box
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        if (newValue.text != searchQuery) {
                            viewModel.setSearchQuery(newValue.text)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    cursorBrush = SolidColor(appColors.primaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchTrigger(textFieldValue.text) }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp)),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = if (mode == SearchScreenMode.ADD_TO_PLAYLIST)
                                            "Search Snitify to add..."
                                        else
                                            "Search songs, artists...",
                                        color = TextSecondary,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.setSearchQuery("") }
                                )
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Body content ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    isSearching -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = appColors.primaryAccent)
                        }
                    }

                    searchResults.isNotEmpty() -> {
                        // Render search results matching Artist Screen UI
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults, key = { it.id }) { song ->
                                val dlProgress = downloadStatusMap[song.id]
                                val isCurrentTrack = currentTrack?.id == song.id

                                SearchResultRow(
                                    song = song,
                                    isCurrentTrack = isCurrentTrack,
                                    isPlaying = isPlaying,
                                    mode = mode,
                                    playlistId = playlistId,
                                    dlProgress = dlProgress,
                                    appColors = appColors,
                                    onPlay = {
                                        if (isCurrentTrack) {
                                            viewModel.togglePlay()
                                        } else {
                                            viewModel.playSongFromList(song, searchResults)
                                        }
                                    },
                                    onCancelDownload = { viewModel.cancelDownload(song.id) },
                                    onAdd = {
                                        if (playlistId != null) {
                                            viewModel.addSongToPlaylist(playlistId, song)
                                            Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOptionsClick = {
                                        selectedSongForBottomSheet = song
                                    }
                                )
                            }
                        }
                    }

                    searchSuggestions.isNotEmpty() -> {
                        // Render suggestions list
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(searchSuggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.performSearch(suggestion)
                                            focusManager.clearFocus()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_search),
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = suggestion,
                                        color = TextPrimary,
                                        fontSize = 15.sp
                                    )
                                }
                                Divider(color = DividerColor, thickness = 0.5.dp)
                            }
                        }
                    }

                    searchQuery.isEmpty() && searchHistory.isNotEmpty() -> {
                        // Render recent search history
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            item {
                                Text(
                                    text = "Recent Searches",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                )
                            }
                            items(searchHistory) { query ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.performSearch(query)
                                                focusManager.clearFocus()
                                            },
                                            onLongClick = {
                                                historyItemToRemove = query
                                            }
                                        )
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.history),
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = query,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Divider(color = DividerColor, thickness = 0.5.dp)
                            }
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Search Snitify",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Song 3-Dot Options ModalBottomSheet (Identical to Artist Screen)
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
                            .background(GlassBorder)
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
                                viewModel.playSongFromList(targetSong, searchResults)
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

                    // Action 2: Share
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
                    val likedIds by viewModel.likedTrackIds.collectAsState()
                    val isLiked = likedIds.contains(targetSong.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
                                imageVector = Icons.Default.FavoriteBorder,
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
                                items(userPlaylists, key = { it.id }) { pl ->
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
                                                viewModel.refreshPlaylists()
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

    // ── Long-press Remove Bottom Sheet ──────────────────────────────────────
    historyItemToRemove?.let { query ->
        ModalBottomSheet(
            onDismissRequest = { historyItemToRemove = null },
            sheetState = bottomSheetState,
            containerColor = Color(0xFF1A1A1A),
            scrimColor = Color.Black.copy(alpha = 0.5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            tonalElevation = 0.dp
        ) {
            com.snitrix.snitify.ui.component.TransparentSystemBarsForDialog()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = query,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Remove from search history",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { historyItemToRemove = null }) {
                        Text("Cancel", color = TextSecondary, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.removeSearchHistoryItem(query)
                            historyItemToRemove = null
                        }
                    ) {
                        Text("Remove", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Search Result Row Matching Artist Screen UI ──────────────────────────────────────
@Composable
private fun SearchResultRow(
    song: Song,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    mode: SearchScreenMode,
    playlistId: Long?,
    dlProgress: DownloadProgress?,
    appColors: com.snitrix.snitify.ui.theme.AppThemeColors,
    onPlay: () -> Unit,
    onCancelDownload: () -> Unit,
    onAdd: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (mode == SearchScreenMode.GLOBAL) onPlay()
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Song Thumbnail (52dp x 52dp) with Interactive Play / Pause Badge Overlay
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF222222))
                .clickable { onPlay() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.finalCover,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Interactive Play / Pause Badge Overlay or Download Thumbnail Overlay
            if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                com.snitrix.snitify.ui.component.DownloadThumbnailOverlay(progressPercent = dlProgress.percent)
            } else {
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
        IconButton(onClick = onOptionsClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_more_vertical),
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
