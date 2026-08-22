package com.snitrix.snitify.ui.component

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.DownloadProgress
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InPlaylistSearchModal(
    visible: Boolean,
    playlistSongs: List<Song>,
    currentTrack: Song?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onTrackSelect: (Song, List<Song>) -> Unit,
    playlistId: Long? = null,
    viewModel: MusicViewModel? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = visible) {
        keyboardController?.hide()
        onDismiss()
    }

    var searchQuery by remember { mutableStateOf("") }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length)))
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var selectedSongForBottomSheet by remember { mutableStateOf<Song?>(null) }
    var songToDeleteDownload by remember { mutableStateOf<Song?>(null) }
    val downloadStatusMap by viewModel?.downloadStatusFlow?.collectAsState() ?: remember { mutableStateOf(emptyMap()) }
    val downloadedTracks by viewModel?.downloadedTracks?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "modalShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val appColors = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current

    val searchResults = remember(searchQuery, playlistSongs) {
        if (searchQuery.isBlank()) {
            playlistSongs
        } else {
            val q = searchQuery.trim().lowercase()
            playlistSongs.filter { song ->
                song.title.lowercase().contains(q) || song.artist.lowercase().contains(q)
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + scaleIn(
            initialScale = 0.85f,
            transformOrigin = TransformOrigin(0.5f, 0.05f),
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(animationSpec = tween(180)) + scaleOut(
            targetScale = 0.85f,
            transformOrigin = TransformOrigin(0.5f, 0.05f),
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with Left Back Arrow & Centered Search Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(Color(0xFF262626))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_search),
                                contentDescription = null,
                                tint = if (isFocused) appColors.primaryAccent else Color(0xFF8A8A8A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search songs",
                                        color = Color(0xFF8A8A8A),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        textFieldValue = newValue
                                        searchQuery = newValue.text
                                    },
                                    singleLine = true,
                                    cursorBrush = SolidColor(Color.White),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { isFocused = it.isFocused }
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        textFieldValue = TextFieldValue("", TextRange(0))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_close),
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF8A8A8A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                // Results list
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching songs in playlist",
                            color = TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        items(searchResults, key = { "in_pl_${it.id}" }) { song ->
                            val isCurrentTrack = currentTrack?.id == song.id
                            val dlProgress = downloadStatusMap[song.id]

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        keyboardController?.hide()
                                        onTrackSelect(song, playlistSongs)
                                        onDismiss()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail Box with Animated Shimmer Skeleton Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF262626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = song.finalCover,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
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
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_library),
                                                contentDescription = null,
                                                tint = Color(0xFF666666),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                        DownloadThumbnailOverlay(progressPercent = dlProgress.percent)
                                    } else if (isCurrentTrack) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (isPlaying) appColors.primaryAccent.copy(alpha = 0.55f)
                                                    else Color.Black.copy(alpha = 0.35f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                                ),
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    HighlightedText(
                                        text = song.title,
                                        query = searchQuery,
                                        baseColor = if (isCurrentTrack) appColors.primaryAccent else TextPrimary,
                                        highlightColor = appColors.primaryAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                        Text(
                                            text = "Downloading... ${dlProgress.percent}%",
                                            color = appColors.primaryAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        HighlightedText(
                                            text = song.artist,
                                            query = searchQuery,
                                            baseColor = TextSecondary,
                                            highlightColor = appColors.primaryAccent,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal
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
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Song 3-Dot Options ModalBottomSheet
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
                    TransparentSystemBarsForDialog()
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
                                    onTrackSelect(targetSong, playlistSongs)
                                    onDismiss()
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

                        // Action 3: Remove from playlist
                        if (playlistId != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val songToRemove = targetSong
                                        selectedSongForBottomSheet = null
                                        viewModel?.removeSongFromPlaylist(playlistId, songToRemove.id)
                                        Toast.makeText(context, "Removed '${songToRemove.title}' from playlist", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Remove from playlist",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Remove from playlist",
                                    color = Color(0xFFFF5252),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Action 4: Add to / Remove from favorites
                        val likedIds = viewModel?.likedTrackIds?.collectAsState()?.value ?: emptySet()
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

                        // Action 5: Dynamic Download / Cancel / Delete Action
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
                                    isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5252)
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
                                    isTargetDownloading || isTargetDownloaded -> Color(0xFFFF5252)
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
            androidx.compose.material3.AlertDialog(
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
                    androidx.compose.material3.Button(
                        onClick = {
                            viewModel?.deleteDownloadedTrack(targetSong.id)
                            songToDeleteDownload = null
                            Toast.makeText(context, "Download deleted", Toast.LENGTH_SHORT).show()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { songToDeleteDownload = null }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    baseColor: Color,
    highlightColor: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val annotatedString = remember(text, query) {
        if (query.isBlank()) {
            buildAnnotatedString {
                withStyle(style = SpanStyle(color = baseColor, fontSize = fontSize, fontWeight = fontWeight)) {
                    append(text)
                }
            }
        } else {
            val q = query.trim()
            val startIndex = text.indexOf(q, ignoreCase = true)
            if (startIndex == -1) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(color = baseColor, fontSize = fontSize, fontWeight = fontWeight)) {
                        append(text)
                    }
                }
            } else {
                val endIndex = startIndex + q.length
                buildAnnotatedString {
                    if (startIndex > 0) {
                        withStyle(style = SpanStyle(color = baseColor, fontSize = fontSize, fontWeight = fontWeight)) {
                            append(text.substring(0, startIndex))
                        }
                    }
                    withStyle(style = SpanStyle(color = highlightColor, fontSize = fontSize, fontWeight = FontWeight.Bold)) {
                        append(text.substring(startIndex, endIndex))
                    }
                    if (endIndex < text.length) {
                        withStyle(style = SpanStyle(color = baseColor, fontSize = fontSize, fontWeight = fontWeight)) {
                            append(text.substring(endIndex))
                        }
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        maxLines = maxLines,
        overflow = overflow
    )
}
