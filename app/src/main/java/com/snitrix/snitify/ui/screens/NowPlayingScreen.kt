package com.snitrix.snitify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.ui.theme.AccentBlueProgress
import com.snitrix.snitify.ui.theme.AccentLavender
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.GradientPurpleMid
import com.snitrix.snitify.ui.theme.GradientPurpleTop
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.viewmodel.MusicViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onDismiss()
    }

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progressMs by viewModel.progressMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatEnabled by viewModel.repeatEnabled.collectAsState()
    val transitionType by viewModel.transitionType.collectAsState()
    val likedTrackIds by viewModel.likedTrackIds.collectAsState()
    val isTrackLoading by viewModel.isTrackLoading.collectAsState()
    
    val context = LocalContext.current
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val downloadProgress by viewModel.downloadStatusFlow.collectAsState()
    
    var showMenuDialog by remember { mutableStateOf(false) }
    var showPlaylistSelectDialog by remember { mutableStateOf(false) }

    val track = currentTrack ?: return

    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeight.toPx() }

    val appColors = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current
    val wavySliderEnabled by com.snitrix.snitify.ui.theme.ThemeManager.wavySliderEnabled.collectAsState()
    val thumbnailRotationEnabled by com.snitrix.snitify.ui.theme.ThemeManager.thumbnailRotationEnabled.collectAsState()
    val playerBgStyle by com.snitrix.snitify.ui.theme.ThemeManager.playerBgStyle.collectAsState()

    // Top Radial Gradient Overlay
    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, Color.Transparent),
        startY = 0f,
        endY = screenHeightPx * 0.55f
    )

    // Slow rotation animation for playing vinyl (stays frozen on stop/pause or when rotation is disabled by user)
    var rotationAngle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying, thumbnailRotationEnabled) {
        if (isPlaying && thumbnailRotationEnabled) {
            val startTime = System.currentTimeMillis()
            val startAngle = rotationAngle
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                rotationAngle = (startAngle + (elapsed / 1000f) * 24f) % 360f
                kotlinx.coroutines.delay(16)
            }
        }
    }
    val currentRotation = rotationAngle

    // Format progress and duration
    val progressSec = progressMs / 1000
    val durationSec = durationMs / 1000
    val progressStr = String.format("%d:%02d", progressSec / 60, progressSec % 60)
    val durationStr = String.format("%d:%02d", durationSec / 60, durationSec % 60)

    val progressFraction = if (durationMs > 0) progressMs.toFloat() / durationMs else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
            .hazeChild(state = hazeState, tint = Color(0xD00A0508))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Consumes touch events to prevent click bleed-through to underlying lists */ }
            )
    ) {
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(topGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = onDismiss,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_down),
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Decorative drag handle
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextPrimary.copy(alpha = 0.3f))
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = { showMenuDialog = true },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vertical),
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Track Info Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        color = TextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val isLiked = likedTrackIds.contains(track.id)
                val heartIcon = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                val heartTint = if (isLiked) appColors.primaryAccent else TextPrimary

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = { viewModel.toggleLike(track.id) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = heartIcon),
                        contentDescription = "Like",
                        tint = heartTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Circular Album Art Player with Conditional Transition (Slide ONLY for next/prev, Fade for list clicks)
            androidx.compose.animation.AnimatedContent(
                targetState = track,
                transitionSpec = {
                    when (transitionType) {
                        com.snitrix.snitify.ui.viewmodel.TrackTransitionType.SLIDE_PREV -> {
                            (androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { -it } + androidx.compose.animation.fadeIn()) togetherWith
                                    (androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { it } + androidx.compose.animation.fadeOut())
                        }
                        com.snitrix.snitify.ui.viewmodel.TrackTransitionType.SLIDE_NEXT -> {
                            (androidx.compose.animation.slideInHorizontally(animationSpec = tween(300)) { it } + androidx.compose.animation.fadeIn()) togetherWith
                                    (androidx.compose.animation.slideOutHorizontally(animationSpec = tween(300)) { -it } + androidx.compose.animation.fadeOut())
                        }
                        else -> {
                            androidx.compose.animation.fadeIn(animationSpec = tween(200)) togetherWith
                                    androidx.compose.animation.fadeOut(animationSpec = tween(200))
                        }
                    }
                },
                label = "trackAlbumArtTransition"
            ) { targetTrack ->
                Box(
                    modifier = Modifier.size(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Layer 1: Peeking blurred background discs
                    AsyncImage(
                        model = targetTrack.finalCover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = (-40).dp)
                            .size(240.dp)
                            .clip(CircleShape)
                            .blur(20.dp)
                            .background(Color.White.copy(alpha = 0.1f)),
                        alpha = 0.35f
                    )

                    AsyncImage(
                        model = targetTrack.finalCover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = 40.dp)
                            .size(240.dp)
                            .clip(CircleShape)
                            .blur(20.dp)
                            .background(Color.White.copy(alpha = 0.1f)),
                        alpha = 0.35f
                    )

                    // Layer 2: Outer glass ring
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(GlassSurface, CircleShape)
                            .border(1.dp, GlassBorder, CircleShape)
                    )

                    // Layer 3: Progress arc
                    Canvas(
                        modifier = Modifier.size(298.dp)
                    ) {
                        drawCircle(
                            color = GlassBorder.copy(alpha = 0.2f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = AccentBlueProgress,
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Layer 4: Central rotating album art
                    AsyncImage(
                        model = targetTrack.finalCover,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(currentRotation)
                            .clip(CircleShape)
                            .border(2.dp, GlassBorder, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    com.snitrix.snitify.ui.theme.ThemeManager.setThumbnailRotationEnabled(!thumbnailRotationEnabled)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Draggable seek slider
            var sliderPosition by remember { mutableStateOf<Float?>(null) }
            val currentSliderValue = sliderPosition ?: progressFraction

            if (wavySliderEnabled) {
                com.snitrix.snitify.ui.component.WavySlider(
                    value = currentSliderValue,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let { viewModel.seekTo(it) }
                        sliderPosition = null
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = appColors.primaryAccent,
                        inactiveTrackColor = GlassBorder.copy(alpha = 0.3f),
                        thumbColor = TextPrimary
                    ),
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            } else {
                Slider(
                    value = currentSliderValue,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let { viewModel.seekTo(it) }
                        sliderPosition = null
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = appColors.primaryAccent,
                        inactiveTrackColor = GlassBorder.copy(alpha = 0.3f),
                        thumbColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }

            // Progress and Duration labels
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progressStr,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = durationStr,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = {
                                val enabled = viewModel.toggleShuffle()
                                Toast.makeText(context, if (enabled) "Shuffle on" else "Shuffle off", Toast.LENGTH_SHORT).show()
                            },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) appColors.primaryAccent else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Skip Previous
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = { viewModel.playPrevious(forcePrev = true) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(TextPrimary)
                        .clickable { viewModel.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isTrackLoading || sliderPosition != null) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                            ),
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Skip Next
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = { viewModel.playNext(forceNext = true) },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable(
                            onClick = {
                                val enabled = viewModel.toggleRepeat()
                                Toast.makeText(context, if (enabled) "Repeat on" else "Repeat off", Toast.LENGTH_SHORT).show()
                            },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_repeat),
                        contentDescription = "Repeat",
                        tint = if (repeatEnabled) appColors.primaryAccent else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showMenuDialog) {
        val trackProgress = downloadProgress[track.id]
        val isDownloaded = downloadedTracks.any { it.id == track.id }
        val currentStatus = trackProgress?.status ?: com.snitrix.snitify.playback.DownloadStatus.IDLE
        val isDownloading = currentStatus == com.snitrix.snitify.playback.DownloadStatus.DOWNLOADING
            || currentStatus == com.snitrix.snitify.playback.DownloadStatus.PENDING
        val pct = trackProgress?.percent ?: 0

        AlertDialog(
            onDismissRequest = { showMenuDialog = false },
            title = { Text(text = track.title, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = track.artist, color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Download Action Row (Hidden if already downloaded or local file)
                    val isLocalFile = track.id.startsWith("content://") || track.id.startsWith("/")
                    if (!isDownloaded && !isLocalFile) {
                        if (isDownloading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Downloading... $pct%",
                                        color = appColors.primaryAccent,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Cancel",
                                        color = Color(0xFFFF5252),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            viewModel.cancelDownload(track.id)
                                            showMenuDialog = false
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { pct / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = appColors.primaryAccent,
                                    trackColor = Color(0xFF333333)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.downloadTrack(track)
                                        showMenuDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_download),
                                    contentDescription = null,
                                    tint = appColors.primaryAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Download Track",
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Divider(color = DividerColor, thickness = 0.5.dp)
                    }

                    // Add to Playlist Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMenuDialog = false
                                showPlaylistSelectDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_library),
                            contentDescription = null,
                            tint = appColors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Add to Playlist",
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showMenuDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                ) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = BackgroundBlack,
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
        )
    }

    if (showPlaylistSelectDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistSelectDialog = false },
            title = { Text("Add to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    if (playlists.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No playlists found.\nCreate a playlist in My Library.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(playlists, key = { it.id }) { playlist ->
                                val plSongs = remember(playlist.id) { viewModel.getPlaylistSongs(playlist.id) }
                                val isAlreadyIn = plSongs.any { it.id == track.id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isAlreadyIn == false) {
                                            viewModel.addSongToPlaylist(playlist.id, track)
                                            showPlaylistSelectDialog = false
                                            Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_library),
                                        contentDescription = null,
                                        tint = if (isAlreadyIn) TextSecondary.copy(alpha = 0.4f) else TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = playlist.name,
                                        color = if (isAlreadyIn) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAlreadyIn) {
                                        Text(
                                            text = "✓ Already Added",
                                            color = appColors.primaryAccent.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Divider(color = DividerColor, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPlaylistSelectDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = BackgroundBlack,
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
        )
    }
}
