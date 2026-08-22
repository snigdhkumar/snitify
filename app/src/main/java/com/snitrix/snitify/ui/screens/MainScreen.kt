package com.snitrix.snitify.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.PlaybackManager
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild


@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val activeLibraryCategory by viewModel.activeLibraryCategory.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isTrackLoading by viewModel.isTrackLoading.collectAsState()
    val appColors = LocalAppThemeColors.current
    val haptic = LocalHapticFeedback.current

    var showNowPlaying by remember { mutableStateOf(false) }

    var showSearchScreen by remember { mutableStateOf(false) }
    var searchScreenMode by remember { mutableStateOf(SearchScreenMode.GLOBAL) }
    var activeSearchPlaylistId by remember { mutableStateOf<Long?>(null) }

    // Internal sub-navigation for list screens
    var activeListTitle by remember { mutableStateOf<String?>(null) }
    var activeListTracks by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Natively handle Android hardware back press/swipe gestures
    BackHandler(enabled = showNowPlaying || showSearchScreen || activeListTitle != null || activeLibraryCategory != null || activeTab != "home") {
        if (showNowPlaying) {
            showNowPlaying = false
        } else if (showSearchScreen) {
            showSearchScreen = false
        } else if (activeTab != "home") {
            activeListTitle = null
            viewModel.selectLibraryCategory(null)
            viewModel.selectPlaylist(null)
            viewModel.selectTab("home")
        } else if (activeListTitle != null) {
            activeListTitle = null
        } else if (activeLibraryCategory != null) {
            viewModel.selectLibraryCategory(null)
        }
    }

    val hazeState = remember { HazeState() }
    val downloadBannerSong by viewModel.downloadBannerEvent.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var availableUpdateInfo by remember { mutableStateOf<com.snitrix.snitify.utils.UpdateInfo?>(null) }

    // Background OTA Version Update Checker
    LaunchedEffect(Unit) {
        val info = com.snitrix.snitify.utils.UpdateManager.checkForUpdates()
        if (info != null && com.snitrix.snitify.utils.UpdateManager.shouldShowUpdateDialog(context, info)) {
            availableUpdateInfo = info
        }
    }

    LaunchedEffect(downloadBannerSong) {
        if (downloadBannerSong != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.dismissDownloadBanner()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Download Toast Banner Overlay
        AnimatedVisibility(
            visible = downloadBannerSong != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            downloadBannerSong?.let { song ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E1E1E),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Download",
                            tint = appColors.primaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloading Track...",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = song.title,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = appColors.primaryAccent,
                            modifier = Modifier.clickable {
                                viewModel.dismissDownloadBanner()
                                showSearchScreen = false
                                activeListTitle = null
                                viewModel.selectTab("library")
                                viewModel.selectLibraryCategory("downloads")
                            }
                        ) {
                            Text(
                                text = "VIEW",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        // Active Tab Screen Content (wrapped in haze receiver with dark background to prevent white flashes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appColors.background)
                .haze(state = hazeState)
        ) {
            AnimatedContent(
                targetState = Pair(activeTab, activeListTitle),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(180)) + slideInVertically { height -> height / 20 }).togetherWith(
                        fadeOut(animationSpec = tween(140)) + slideOutVertically { height -> -height / 20 }
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(appColors.background),
                label = "ScreenTransition"
            ) { (tab, listTitle) ->
                when (tab) {
                    "home" -> {
                        if (listTitle != null) {
                            TrackListScreen(
                                title = listTitle,
                                tracks = activeListTracks,
                                currentTrack = currentTrack,
                                onTrackSelect = { song, trackList -> viewModel.playSongFromList(song, trackList) },
                                onBack = { activeListTitle = null },
                                viewModel = viewModel,
                                onNavigateToDownloads = {
                                    activeListTitle = null
                                    viewModel.selectTab("library")
                                    viewModel.selectLibraryCategory("downloads")
                                }
                            )
                        } else {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToTrackList = { title, tracks ->
                                    activeListTitle = title
                                    activeListTracks = tracks
                                },
                                onOpenSearch = {
                                    activeSearchPlaylistId = null
                                    searchScreenMode = SearchScreenMode.GLOBAL
                                    showSearchScreen = true
                                },
                                onOpenSettings = {
                                    viewModel.selectTab("settings")
                                }
                            )
                        }
                    }
                    "search" -> SearchScreen(
                        viewModel = viewModel,
                        mode = SearchScreenMode.GLOBAL,
                        playlistId = null,
                        isPlayerOpen = showNowPlaying,
                        onDismiss = {
                            viewModel.selectTab("home")
                        }
                    )
                    "library" -> LibraryScreen(
                        viewModel = viewModel,
                        isSearchActive = showSearchScreen || showNowPlaying,
                        onOpenSearch = { playlistId ->
                            activeSearchPlaylistId = playlistId
                            searchScreenMode = SearchScreenMode.ADD_TO_PLAYLIST
                            showSearchScreen = true
                        }
                    )
                    "settings" -> SettingsScreen(onBackToHome = { viewModel.selectTab("home") }, viewModel = viewModel, isPlayerOpen = showNowPlaying)
                }
            }

            // Full Screen Search Screen Overlay
            AnimatedVisibility(
                visible = showSearchScreen,
                enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(180)) { height -> height / 20 },
                exit = fadeOut(animationSpec = tween(140)) + slideOutVertically(animationSpec = tween(140)) { height -> height / 20 }
            ) {
                SearchScreen(
                    viewModel = viewModel,
                    mode = searchScreenMode,
                    playlistId = activeSearchPlaylistId,
                    isPlayerOpen = showNowPlaying,
                    onDismiss = {
                        showSearchScreen = false
                        viewModel.setSearchQuery("")
                    }
                )
            }
        }

        // Mini Player & Bottom Navigation overlay block
        AnimatedVisibility(
            visible = !showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Floating Mini-Player Pill
                // ─── Fix: hold last non-null track so exit animation shows content ───
                var lastTrack by remember { mutableStateOf<Song?>(null) }
                var isFadingOut by remember { mutableStateOf(false) }
                LaunchedEffect(currentTrack) {
                    if (currentTrack != null) {
                        lastTrack = currentTrack
                        isFadingOut = false
                    }
                }
                val displayTrack by remember { derivedStateOf { currentTrack ?: lastTrack } }
                val transitionType by viewModel.transitionType.collectAsState()

                AnimatedVisibility(
                    visible = currentTrack != null && !isFadingOut,
                    enter = androidx.compose.animation.fadeIn(tween(250, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250, easing = FastOutSlowInEasing)),
                    exit = ExitTransition.None
                ) {
                    displayTrack?.let { track ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .hazeChild(state = hazeState, shape = RoundedCornerShape(20.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                .background(GlassSurface, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ── Tap, Long-press & Swipe region (Artwork + Title + Artist) ──
                                var skipDirection by remember { mutableStateOf(1) } // 1: Next (Right-to-Left), -1: Prev (Left-to-Right)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clipToBounds()
                                        .clip(RoundedCornerShape(12.dp))
                                        .pointerInput(track.id) {
                                            detectTapGestures(
                                                onTap = { showNowPlaying = true },
                                                onLongPress = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    isFadingOut = true
                                                    PlaybackManager.fadeAndStop {
                                                        viewModel.stopPlaybackAndClear()
                                                        isFadingOut = false
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(track.id) {
                                            var totalX = 0f
                                            var totalY = 0f
                                            detectDragGestures(
                                                onDragStart = {
                                                    totalX = 0f
                                                    totalY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    totalX += dragAmount.x
                                                    totalY += dragAmount.y
                                                },
                                                onDragEnd = {
                                                    val absX = kotlin.math.abs(totalX)
                                                    val absY = kotlin.math.abs(totalY)
                                                    when {
                                                        totalY > 60f && absY > absX -> {
                                                            // Swipe Down → instant stop music
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            isFadingOut = true
                                                            PlaybackManager.fadeAndStop {
                                                                viewModel.stopPlaybackAndClear()
                                                                isFadingOut = false
                                                            }
                                                        }
                                                        totalX < -50f && absX > absY -> {
                                                            // Swipe Right to Left (<-) → Next Song
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            skipDirection = 1
                                                            viewModel.playNext(forceNext = true)
                                                        }
                                                        totalX > 50f && absX > absY -> {
                                                            // Swipe Left to Right (->) → Previous Song
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            skipDirection = -1
                                                            viewModel.playPrevious(forcePrev = true)
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    AnimatedContent(
                                        targetState = track,
                                        transitionSpec = {
                                            when (transitionType) {
                                                com.snitrix.snitify.ui.viewmodel.TrackTransitionType.SLIDE_NEXT -> {
                                                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> width }
                                                        .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { width -> -width })
                                                }
                                                com.snitrix.snitify.ui.viewmodel.TrackTransitionType.SLIDE_PREV -> {
                                                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { width -> -width }
                                                        .togetherWith(slideOutHorizontally(tween(250, easing = FastOutSlowInEasing)) { width -> width })
                                                }
                                                else -> {
                                                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                                                }
                                            }
                                        },
                                        label = "trackContent"
                                    ) { t ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = t.finalCover,
                                                contentDescription = "Cover",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = t.title,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = t.artist,
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Skip Backward
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            skipDirection = -1
                                            viewModel.playPrevious(forcePrev = true)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_skip_previous),
                                        contentDescription = "Previous",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Play/Pause icon button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { viewModel.togglePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTrackLoading) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = appColors.primaryAccent,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(
                                                id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                            ),
                                            contentDescription = "Play/Pause",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Skip Next (Forward)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            skipDirection = 1
                                            viewModel.playNext(forceNext = true)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_skip_next),
                                        contentDescription = "Next",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))

                // Floating Bottom Navigation Bar Pill
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .hazeChild(state = hazeState, shape = RoundedCornerShape(32.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
                            .padding(bottom = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Tab 1: Home
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    onClick = {
                                        activeListTitle = null
                                        viewModel.selectLibraryCategory(null)
                                        viewModel.selectPlaylist(null)
                                        viewModel.selectTab("home")
                                        showSearchScreen = false
                                    },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_home),
                                contentDescription = "Home",
                                tint = if (activeTab == "home") Color.White else TextSecondary.copy(alpha = 0.65f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Persistent Circular Thumbnail (deep thick white border ring, fixed across app theme colors)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    onClick = { if (currentTrack != null) showNowPlaying = true },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer deep thick white ring — fixed style, independent of theme color
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .border(
                                        width = 3.dp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack != null) {
                                    // Filled with album art
                                    AsyncImage(
                                        model = currentTrack!!.finalCover,
                                        contentDescription = "Now Playing",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    // Empty state — play icon placeholder
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_play),
                                        contentDescription = "No Music",
                                        tint = Color.White.copy(alpha = 0.60f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Tab 2: Library
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable(
                                    onClick = {
                                        showSearchScreen = false
                                        activeListTitle = null
                                        viewModel.selectTab("library")
                                    },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_library),
                                contentDescription = "Library",
                                tint = if (activeTab == "library") Color.White else TextSecondary.copy(alpha = 0.65f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Full Screen Now Playing Sheet Overlay
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                hazeState = hazeState,
                onDismiss = { showNowPlaying = false }
            )
        }

        // OTA Version Update Dialog Overlay
        availableUpdateInfo?.let { updateInfo ->
            com.snitrix.snitify.ui.component.UpdateDialog(
                updateInfo = updateInfo,
                onDismissLater = {
                    com.snitrix.snitify.utils.UpdateManager.saveLaterDismissalTimestamp(context)
                    availableUpdateInfo = null
                }
            )
        }

        // Modal Import Progress Overlay
        val importState by viewModel.importProgressState.collectAsState()
        importState?.let { state ->
            com.snitrix.snitify.ui.component.ImportProgressDialog(
                state = state,
                onCancel = { viewModel.cancelImport() }
            )
        }
    }
}
