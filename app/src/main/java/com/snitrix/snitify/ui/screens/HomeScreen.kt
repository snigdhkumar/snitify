package com.snitrix.snitify.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.snitrix.snitify.playback.DownloadStatus
import com.snitrix.snitify.ui.component.DownloadProgressWidget
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassSurface
import coil.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToTrackList: (title: String, tracks: List<Song>) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val recommendedSongs by viewModel.recommendedSongs.collectAsState()
    val isRefreshing by viewModel.isRefreshingHome.collectAsState()
    val context = LocalContext.current

    val appColors = LocalAppThemeColors.current
    val downloadStatusMap by viewModel.downloadStatusFlow.collectAsState()

    val quickPicksList = remember(recentlyPlayed) {
        recentlyPlayed.filter { song ->
            !song.id.startsWith("content://") &&
                    !song.id.startsWith("/") &&
                    song.id.toLongOrNull() == null &&
                    !(song.coverUrl.startsWith("/") && !song.coverUrl.endsWith(".jpg", ignoreCase = true) && !song.coverUrl.endsWith(".png", ignoreCase = true))
        }
    }

    var isRecentlyPlayedExpanded by remember { mutableStateOf(false) }
    val displayedQuickPicksCount = if (isRecentlyPlayedExpanded) 20 else 5

    val coroutineScope = rememberCoroutineScope()

    val onboardingDataStore = remember(context) { com.snitrix.snitify.data.datastore.OnboardingDataStore(context) }
    val selectedArtistsSet by onboardingDataStore.selectedArtists.collectAsState(initial = null)
    val selectedLanguagesSet by onboardingDataStore.selectedLanguages.collectAsState(initial = null)

    val userSelectedArtists = remember(selectedArtistsSet) {
        selectedArtistsSet?.toList() ?: listOf("Arijit Singh", "Taylor Swift", "KK", "Shreya Ghoshal")
    }

    val userSelectedLanguages = remember(selectedLanguagesSet) {
        selectedLanguagesSet?.toList() ?: listOf("Hindi", "English")
    }

    val artistImagesMap by onboardingDataStore.artistImagesMap.collectAsState(initial = emptyMap())
    val languageImagesMap by onboardingDataStore.languageImagesMap.collectAsState(initial = emptyMap())

    // Dialog & Options state for editing/adding artist & language
    var showAddArtistDialog by remember { mutableStateOf(false) }
    var showAddLanguageDialog by remember { mutableStateOf(false) }
    var artistForOptions by remember { mutableStateOf<String?>(null) }
    var languageForOptions by remember { mutableStateOf<String?>(null) }
    var artistToEditName by remember { mutableStateOf<String?>(null) }
    var languageToEditName by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header with Title "Home" and Search & Settings icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Home",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onOpenSearch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_user),
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshHomeFeed() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp)
                ) {
                    // Section 1: Quick Picks
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recently Played",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (quickPicksList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "No music played recently",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickPicksList.take(displayedQuickPicksCount).forEach { song ->
                                    val dlProgress = downloadStatusMap[song.id]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.playSongFromList(song, quickPicksList, isFromRecentlyPlayed = true) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = song.finalCover,
                                                contentDescription = song.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (dlProgress != null && (dlProgress.status == DownloadStatus.DOWNLOADING || dlProgress.status == DownloadStatus.PENDING)) {
                                                com.snitrix.snitify.ui.component.DownloadThumbnailOverlay(progressPercent = dlProgress.percent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = song.title,
                                                    color = if (currentTrack?.id == song.id) appColors.primaryAccent else TextPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (currentTrack?.id == song.id && (dlProgress == null || (dlProgress.status != DownloadStatus.DOWNLOADING && dlProgress.status != DownloadStatus.PENDING))) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    com.snitrix.snitify.ui.component.RowVisualizer(
                                                        isPlaying = isPlaying,
                                                        color = appColors.primaryAccent
                                                    )
                                                }
                                            }
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
                                    }
                                }

                                if (quickPicksList.size > 5) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { isRecentlyPlayedExpanded = !isRecentlyPlayedExpanded }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isRecentlyPlayedExpanded) "View less" else "View all",
                                            color = appColors.primaryAccent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isRecentlyPlayedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isRecentlyPlayedExpanded) "View less" else "View all",
                                            tint = appColors.primaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: From Your Artists
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "From Your Artists",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (userSelectedArtists.isEmpty()) {
                            // "Add your favourite artists now" empty state prompt
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable { showAddArtistDialog = true }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_plus),
                                        contentDescription = null,
                                        tint = appColors.primaryAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Add your favourite artists now",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(userSelectedArtists, key = { "artist_$it" }) { artistName ->
                                    val artistRes = remember(artistName) { getArtistDrawableRes(artistName) }
                                    val customArtistImg = artistImagesMap[artistName]

                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    onNavigateToTrackList(artistName, emptyList())
                                                },
                                                onLongClick = {
                                                    artistForOptions = artistName
                                                }
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF181818))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!customArtistImg.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = java.io.File(customArtistImg),
                                                    contentDescription = artistName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (artistRes != null) {
                                                AsyncImage(
                                                    model = artistRes,
                                                    contentDescription = artistName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_user),
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = artistName,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Artist",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // "+ Add New" Artist item at the end of the scroll list
                                item {
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showAddArtistDialog = true },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF181818))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_plus),
                                                contentDescription = "Add New",
                                                tint = appColors.primaryAccent,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Add New",
                                            color = appColors.primaryAccent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: In Your Languages
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "In Your Languages",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (userSelectedLanguages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable { showAddLanguageDialog = true }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ Add Languages",
                                    color = appColors.primaryAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(userSelectedLanguages, key = { "lang_$it" }) { languageName ->
                                    val feedTitle = "$languageName Hits"
                                    val langImgRes = when (languageName.lowercase()) {
                                        "hindi" -> R.drawable.hindi
                                        "english" -> R.drawable.english
                                        else -> null
                                    }
                                    val customLangImg = languageImagesMap[languageName]

                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    onNavigateToTrackList(feedTitle, emptyList())
                                                },
                                                onLongClick = {
                                                    languageForOptions = languageName
                                                }
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF181818))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!customLangImg.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = java.io.File(customLangImg),
                                                    contentDescription = languageName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else if (langImgRes != null) {
                                                Image(
                                                    painter = painterResource(id = langImgRes),
                                                    contentDescription = languageName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = languageName,
                                                    color = TextPrimary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = languageName,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Language",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                item {
                                    Column(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showAddLanguageDialog = true },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(140.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF181818))
                                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_plus),
                                                contentDescription = "Add Language",
                                                tint = appColors.primaryAccent,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Add Language",
                                            color = appColors.primaryAccent,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    // ── Add Artist Dialog ────────────────────────────────────────────────
    if (showAddArtistDialog) {
        com.snitrix.snitify.ui.component.CreateArtistBottomSheet(
            onSave = { name, imagePath ->
                if (name.isNotBlank()) {
                    coroutineScope.launch {
                        val updated = userSelectedArtists.toMutableList()
                        if (!updated.contains(name)) {
                            updated.add(name)
                            onboardingDataStore.updateArtists(updated.toSet())
                        }
                        if (imagePath != null) {
                            onboardingDataStore.saveArtistImagePath(name, imagePath)
                        }
                        showAddArtistDialog = false
                    }
                }
            },
            onDismiss = { showAddArtistDialog = false }
        )
    }

    // ── Add Language Dialog ────────────────────────────────────────────────
    if (showAddLanguageDialog) {
        com.snitrix.snitify.ui.component.CreateLanguageBottomSheet(
            onSave = { name, imagePath ->
                if (name.isNotBlank()) {
                    coroutineScope.launch {
                        val updated = userSelectedLanguages.toMutableList()
                        if (!updated.contains(name)) {
                            updated.add(name)
                            onboardingDataStore.updateLanguages(updated.toSet())
                        }
                        if (imagePath != null) {
                            onboardingDataStore.saveLanguageImagePath(name, imagePath)
                        }
                        showAddLanguageDialog = false
                    }
                }
            },
            onDismiss = { showAddLanguageDialog = false }
        )
    }

    // ── Artist Options BottomSheet ─────────────────────────────────────────
    if (artistForOptions != null) {
        val targetArtist = artistForOptions!!
        ModalBottomSheet(
            onDismissRequest = { artistForOptions = null },
            containerColor = BackgroundBlack,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = targetArtist,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            artistToEditName = targetArtist
                            artistForOptions = null
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Edit artist",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text("Edit Artist", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                onboardingDataStore.removeArtist(targetArtist)
                                artistForOptions = null
                                Toast.makeText(context, "Removed $targetArtist", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Remove artist",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text("Remove Artist", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (artistToEditName != null) {
        val targetArtist = artistToEditName!!
        com.snitrix.snitify.ui.component.CreateArtistBottomSheet(
            initialName = targetArtist,
            initialImagePath = artistImagesMap[targetArtist],
            onSave = { newName, newImagePath ->
                coroutineScope.launch {
                    if (newName != targetArtist) {
                        onboardingDataStore.removeArtist(targetArtist)
                        val updated = userSelectedArtists.toMutableList()
                        updated.remove(targetArtist)
                        if (!updated.contains(newName)) updated.add(newName)
                        onboardingDataStore.updateArtists(updated.toSet())
                    }
                    onboardingDataStore.saveArtistImagePath(newName, newImagePath)
                    artistToEditName = null
                    Toast.makeText(context, "Updated $newName", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { artistToEditName = null }
        )
    }

    // ── Language Options BottomSheet ───────────────────────────────────────
    if (languageForOptions != null) {
        val targetLang = languageForOptions!!
        ModalBottomSheet(
            onDismissRequest = { languageForOptions = null },
            containerColor = BackgroundBlack,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = targetLang,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            languageToEditName = targetLang
                            languageForOptions = null
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                        contentDescription = "Edit language",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text("Edit Language", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                onboardingDataStore.removeLanguage(targetLang)
                                languageForOptions = null
                                Toast.makeText(context, "Removed $targetLang", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Remove language",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text("Remove Language", color = Color(0xFFFF5252), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (languageToEditName != null) {
        val targetLang = languageToEditName!!
        com.snitrix.snitify.ui.component.CreateLanguageBottomSheet(
            initialName = targetLang,
            initialImagePath = languageImagesMap[targetLang],
            onSave = { newName, newImagePath ->
                coroutineScope.launch {
                    if (newName != targetLang) {
                        onboardingDataStore.removeLanguage(targetLang)
                        val updated = userSelectedLanguages.toMutableList()
                        updated.remove(targetLang)
                        if (!updated.contains(newName)) updated.add(newName)
                        onboardingDataStore.updateLanguages(updated.toSet())
                    }
                    onboardingDataStore.saveLanguageImagePath(newName, newImagePath)
                    languageToEditName = null
                    Toast.makeText(context, "Updated $newName", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { languageToEditName = null }
        )
    }
}

private fun getArtistDrawableRes(artistName: String): Int? {
    return when (artistName.lowercase().trim()) {
        "arijit singh" -> R.drawable.artist_arijit_singh
        "shreya ghoshal" -> R.drawable.artist_shreya_ghoshal
        "sonu nigam" -> R.drawable.artist_sonu_nigam
        "kk" -> R.drawable.artist_kk
        "jubin nautiyal" -> R.drawable.artist_jubin_nautiyal
        "sunidhi chauhan" -> R.drawable.artist_sunidhi_chauhan
        "pritam" -> R.drawable.artist_pritam
        "a. r. rahman", "ar rahman", "a r rahman" -> R.drawable.artist_ar_rahman
        "badshah" -> R.drawable.artist_badshah
        "neha kakkar" -> R.drawable.artist_neha_kakkar
        "taylor swift" -> R.drawable.artist_taylor_swift
        "ed sheeran" -> R.drawable.artist_ed_sheeran
        "coldplay" -> R.drawable.artist_coldplay
        "imagine dragons" -> R.drawable.artist_imagine_dragons
        "bruno mars" -> R.drawable.artist_bruno_mars
        "the weeknd" -> R.drawable.artist_the_weeknd
        "billie eilish" -> R.drawable.artist_billie_eilish
        "dua lipa" -> R.drawable.artist_dua_lipa
        "adele" -> R.drawable.artist_adele
        "post malone" -> R.drawable.artist_post_malone
        else -> null
    }
}
