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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.DownloadProgressWidget
import com.snitrix.snitify.ui.component.InPlaylistSearchModal
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

enum class CategoryType {
    FAVORITES,
    DOWNLOADS
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryType: CategoryType,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenSearch: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val favorites by viewModel.favoriteTracks.collectAsState()
    val downloads by viewModel.downloadedTracks.collectAsState()

    val rawSongs = remember(categoryType, favorites, downloads) {
        when (categoryType) {
            CategoryType.FAVORITES -> favorites
            CategoryType.DOWNLOADS -> downloads
        }
    }

    val deviceSongs by viewModel.deviceSongs.collectAsState()

    val title = when (categoryType) {
        CategoryType.FAVORITES -> "Favorites"
        CategoryType.DOWNLOADS -> "Downloads"
    }

    val subtitle = when (categoryType) {
        CategoryType.FAVORITES -> "Auto Playlist"
        CategoryType.DOWNLOADS -> "Offline Media"
    }

    var showSortDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showCategoryOptionsBottomSheet by remember { mutableStateOf(false) }

    var showAddSourcePicker by remember { mutableStateOf(false) }
    var showDevicePicker by remember { mutableStateOf(false) }

    var songToRemove by remember { mutableStateOf<Song?>(null) }
    var selectedSongForBottomSheet by remember { mutableStateOf<Song?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var showSearchModal by remember { mutableStateOf(false) }
    var showGlobalOnlineSearch by remember { mutableStateOf(false) }

    val prefs = remember(context) { context.getSharedPreferences("snitify_sort_prefs", android.content.Context.MODE_PRIVATE) }
    var sortOption by remember(categoryType) {
        mutableStateOf(prefs.getString("sort_category_${categoryType.name}", "adding_time_new_old") ?: "adding_time_new_old")
    }

    val filteredSongs = remember(rawSongs, sortOption) {
        when (sortOption) {
            "adding_time_new_old" -> rawSongs
            "adding_time_old_new" -> rawSongs.reversed()
            "title_az" -> rawSongs.sortedBy { it.title.lowercase() }
            "title_za" -> rawSongs.sortedByDescending { it.title.lowercase() }
            else -> rawSongs
        }
    }

    val searchFilteredSongs = remember(filteredSongs, searchQuery) {
        if (searchQuery.isBlank()) filteredSongs
        else filteredSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val downloadStatusMap by viewModel.downloadStatusFlow.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val likedTrackIds by viewModel.likedTrackIds.collectAsState()

    val isCategoryPlaying = remember(rawSongs, currentTrack, isPlaying) {
        rawSongs.any { it.id == currentTrack?.id } && isPlaying
    }

    val appColors = LocalAppThemeColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "categoryShimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    LaunchedEffect(categoryType, rawSongs.size) {
        if (categoryType == CategoryType.FAVORITES) {
            val hasUnresolved = rawSongs.any {
                it.coverUrl.isBlank() || it.coverUrl == "defaultthumbnail" ||
                it.coverUrl == "null" || it.id.startsWith("import") || it.id.contains("spotify")
            }
            if (hasUnresolved) {
                viewModel.resolveMissingFavoritesCovers()
            }
        }
    }

    val topGradient = Brush.verticalGradient(
        colors = listOf(appColors.gradientTop, appColors.gradientMid, appColors.background),
        startY = 0f,
        endY = 450f
    )

    val lazyListState = rememberLazyListState()

    // Scroll collapse ratio calculation
    val density = LocalDensity.current
    val collapseFraction by remember {
        derivedStateOf {
            val firstItemOffset = lazyListState.firstVisibleItemScrollOffset
            val firstItemIndex = lazyListState.firstVisibleItemIndex
            if (firstItemIndex > 0) 1.0f
            else {
                val maxOffsetPx = with(density) { 180.dp.toPx() }
                (firstItemOffset / maxOffsetPx).coerceIn(0f, 1f)
            }
        }
    }

    val animatedArtworkSize by remember {
        derivedStateOf { lerp(220.dp.value, 110.dp.value, collapseFraction).dp }
    }
    val animatedCornerRadius by remember {
        derivedStateOf { lerp(16.dp.value, 26.dp.value, collapseFraction).dp }
    }
    val animatedOpacity by remember {
        derivedStateOf { lerp(1.0f, 0.0f, collapseFraction * 1.5f).coerceIn(0f, 1f) }
    }
    val animatedTranslateYPx by remember {
        derivedStateOf { with(density) { lerp(0.dp.toPx(), (-40).dp.toPx(), collapseFraction) } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        // Gradient Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(topGradient)
        )

        // Collapsible Top App Bar — hidden when any search overlay is active
        if (!showGlobalOnlineSearch && !showSearchModal) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .zIndex(10f)
                    .background(
                        if (collapseFraction > 0.6f) appColors.background.copy(alpha = (collapseFraction - 0.6f) * 2.5f)
                        else Color.Transparent
                    )
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
                            text = title,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                    } else if (rawSongs.isNotEmpty()) {
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
                // Item 0: Hero Artwork & Title & Action Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artwork Box: 4-Song Collage Cover or Heart/Download Icon
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = animatedTranslateYPx
                                    alpha = animatedOpacity
                                }
                                .size(animatedArtworkSize)
                                .clip(RoundedCornerShape(animatedCornerRadius))
                                .border(1.dp, GlassBorder, RoundedCornerShape(animatedCornerRadius)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rawSongs.size >= 4) {
                                PlaylistCollageCover(
                                    songs = rawSongs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GlassSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (categoryType == CategoryType.FAVORITES) R.drawable.ic_heart_filled else R.drawable.ic_download
                                        ),
                                        contentDescription = title,
                                        tint = if (categoryType == CategoryType.FAVORITES) Color(0xFFFF5252) else appColors.primaryAccent,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = animatedTranslateYPx * 0.5f
                                    alpha = lerp(1.0f, 0.0f, collapseFraction * 1.2f).coerceIn(0f, 1f)
                                }
                                .padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val totalSeconds = rawSongs.sumOf { it.duration }
                        val hours = totalSeconds / 3600
                        val minutes = (totalSeconds % 3600) / 60
                        val seconds = totalSeconds % 60
                        val durationStr = if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds) else String.format("%d:%02d", minutes, seconds)

                        Text(
                            text = "${rawSongs.size} songs",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Bar: Share | 3-Dots | Shuffle | Floating Green Play/Pause
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

                            IconButton(onClick = { showCategoryOptionsBottomSheet = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vertical),
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            IconButton(onClick = {
                                if (rawSongs.isNotEmpty()) {
                                    val shuffled = rawSongs.shuffled()
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
                                        val isCurrentTrackInCategory = rawSongs.any { it.id == currentTrack?.id }
                                        if (isCurrentTrackInCategory) {
                                            viewModel.togglePlay()
                                        } else if (filteredSongs.isNotEmpty()) {
                                            viewModel.playSongFromList(filteredSongs.first(), filteredSongs)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isCategoryPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = if (isCategoryPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .padding(start = if (isCategoryPlaying) 0.dp else 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Song List Items
                if (rawSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs in $title yet.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
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
                                text = "No matching songs found in $title",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    items(searchFilteredSongs, key = { "cat_song_${it.id}" }) { song ->
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
                                            contentScale = ContentScale.Crop,
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

        // Category 3-Dot Options Bottom Sheet
        if (showCategoryOptionsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCategoryOptionsBottomSheet = false },
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(GlassSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rawSongs.size >= 4) {
                                PlaylistCollageCover(
                                    songs = rawSongs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        id = if (categoryType == CategoryType.FAVORITES) R.drawable.ic_heart_filled else R.drawable.ic_download
                                    ),
                                    contentDescription = title,
                                    tint = if (categoryType == CategoryType.FAVORITES) Color(0xFFFF5252) else appColors.primaryAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (categoryType == CategoryType.FAVORITES) "${rawSongs.size} songs" else "$subtitle • ${rawSongs.size} songs",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 1: Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCategoryOptionsBottomSheet = false
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

                    // Option 2: Sort songs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showCategoryOptionsBottomSheet = false
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
                        Text("Sort songs", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    if (categoryType == CategoryType.FAVORITES) {
                        // Option 2: Add to Favorites
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCategoryOptionsBottomSheet = false
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
                            Text("Add to Favorites", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }



                        // Option 4: Clear All Favorites
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCategoryOptionsBottomSheet = false
                                    showClearConfirmDialog = true
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
                            Text("Clear all Favorites", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        // Option 3: Delete All Downloads
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCategoryOptionsBottomSheet = false
                                    showClearConfirmDialog = true
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
                            Text("Delete all downloads", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
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
                            val isSongUnresolved = targetSong.coverUrl.isBlank() ||
                                    targetSong.coverUrl == "defaultthumbnail" ||
                                    targetSong.coverUrl == "null" ||
                                    targetSong.id.startsWith("import") ||
                                    targetSong.id.contains("spotify")

                            if (isSongUnresolved) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF333333).copy(alpha = shimmerAlpha))
                                )
                            } else {
                                SubcomposeAsyncImage(
                                    model = targetSong.finalCover,
                                    contentDescription = targetSong.title,
                                    contentScale = ContentScale.Crop,
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

                    // Add to / Remove from favorites for all categories
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
                                imageVector = androidx.compose.material.icons.Icons.Default.FavoriteBorder,
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

                    // Dynamic Download / Cancel / Delete Action for all category types
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
                                        songToRemove = targetSong
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
                                else -> TextPrimary
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

                    // Save to Device Option (if already downloaded)
                    if (isTargetDownloaded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val s = targetSong
                                    selectedSongForBottomSheet = null
                                    val success = viewModel.saveToPublicDownloads(s)
                                    if (success) {
                                        Toast.makeText(context, "Saved '${s.title}' to Device Downloads", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to save file to device", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                            Text("Save to device", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Dialog 1: Item Removal Confirmation Dialog
        if (songToRemove != null) {
            val songToDelete = songToRemove!!
            AlertDialog(
                onDismissRequest = { songToRemove = null },
                containerColor = BackgroundBlack,
                title = {
                    Text(
                        text = if (categoryType == CategoryType.FAVORITES) "Remove Favorite" else "Delete Download",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (categoryType == CategoryType.FAVORITES)
                            "Are you sure you want to remove '${songToDelete.title}' from Favorites?"
                        else "Are you sure you want to delete '${songToDelete.title}' from downloaded tracks?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (categoryType == CategoryType.FAVORITES) {
                                viewModel.toggleLike(songToDelete.id)
                                Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.deleteDownloadedTrack(songToDelete.id)
                                Toast.makeText(context, "Download deleted", Toast.LENGTH_SHORT).show()
                            }
                            songToRemove = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text(if (categoryType == CategoryType.FAVORITES) "Remove" else "Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { songToRemove = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 2: Clear All Category Confirmation Dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = BackgroundBlack,
                title = { Text(if (categoryType == CategoryType.FAVORITES) "Clear Favorites" else "Delete All Downloads", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text(if (categoryType == CategoryType.FAVORITES) "Are you sure you want to clear all Favorites? This action cannot be undone." else "Are you sure you want to delete all downloaded tracks?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirmDialog = false
                            if (categoryType == CategoryType.FAVORITES) {
                                rawSongs.forEach { viewModel.toggleLike(it.id) }
                                Toast.makeText(context, "Cleared Favorites", Toast.LENGTH_SHORT).show()
                            } else {
                                rawSongs.forEach { viewModel.deleteDownloadedTrack(it.id) }
                                Toast.makeText(context, "Deleted all downloads", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text(if (categoryType == CategoryType.FAVORITES) "Clear All" else "Delete All", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showClearConfirmDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // Dialog 3: Add Source Picker Dialog for Favorites
        if (showAddSourcePicker) {
            AlertDialog(
                onDismissRequest = { showAddSourcePicker = false },
                containerColor = BackgroundBlack,
                title = { Text("Add Songs to Favorites", color = TextPrimary, fontWeight = FontWeight.Bold) },
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

                        // Source 2: Online Search
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    showAddSourcePicker = false
                                    onOpenSearch?.invoke(0L)
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

        // Dialog 4: Device Songs Picker for Favorites
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
                                items(deviceSongs, key = { track -> "dev_fav_${track.id}" }) { track ->
                                    val isAlreadyAdded = rawSongs.any { it.id == track.id }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isAlreadyAdded) {
                                                viewModel.toggleLike(track.id)
                                                Toast.makeText(context, "Added '${track.title}' to Favorites", Toast.LENGTH_SHORT).show()
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
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_plus),
                                                contentDescription = "Add",
                                                tint = appColors.primaryAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
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

        // In-Category Search Slide-up Modal Component
        if (!showGlobalOnlineSearch) {
            InPlaylistSearchModal(
                visible = showSearchModal,
                playlistSongs = rawSongs,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onDismiss = { showSearchModal = false },
                onTrackSelect = { song, trackList ->
                    viewModel.playSongFromList(song, trackList)
                },
                viewModel = viewModel
            )
        }

        // Global Online Search Screen for adding songs to favorites/category
        if (showGlobalOnlineSearch) {
            com.snitrix.snitify.ui.screens.SearchScreen(
                viewModel = viewModel,
                mode = com.snitrix.snitify.ui.screens.SearchScreenMode.GLOBAL,
                playlistId = null,
                onDismiss = {
                    showGlobalOnlineSearch = false
                }
            )
        }

        // Sort by ModalBottomSheet
        if (showSortDialog) {
            com.snitrix.snitify.ui.component.SortByBottomSheet(
                selectedKey = sortOption,
                onOptionSelected = {
                    sortOption = it
                    prefs.edit().putString("sort_category_${categoryType.name}", it).apply()
                },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}
