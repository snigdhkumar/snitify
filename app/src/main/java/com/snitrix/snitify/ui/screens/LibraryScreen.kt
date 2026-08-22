package com.snitrix.snitify.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import androidx.compose.runtime.Composable
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
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snitrix.snitify.R
import com.snitrix.snitify.data.db.Playlist
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.DividerColor
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.GlassSurface
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import com.snitrix.snitify.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onOpenSearch: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false
) {
    val activeCategory by viewModel.activeLibraryCategory.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()
    val downloads by viewModel.downloadedTracks.collectAsState()
    val deviceSongs by viewModel.deviceSongs.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()

    BackHandler(enabled = (selectedPlaylist != null || activeCategory != null) && !isSearchActive) {
        if (selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
            viewModel.refreshPlaylists()
        } else {
            viewModel.selectLibraryCategory(null)
        }
    }

    androidx.compose.animation.AnimatedContent(
        targetState = Pair(activeCategory, selectedPlaylist),
        transitionSpec = {
            val isTargetDeeper = (targetState.second != null && initialState.second == null) ||
                    (targetState.first != null && initialState.first == null)
            if (isTargetDeeper) {
                (androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it / 3 }) + androidx.compose.animation.fadeOut())
            } else {
                (androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it / 3 }) + androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut())
            }
        },
        label = "LibraryScreenTransition"
    ) { (cat, playlist) ->
        if (playlist != null) {
            PlaylistDetailsScreen(
                playlist = playlist,
                viewModel = viewModel,
                onBack = {
                    viewModel.selectPlaylist(null)
                    viewModel.refreshPlaylists()
                },
                onOpenSearch = onOpenSearch
            )
        } else if (cat == "favorites") {
            CategoryDetailsScreen(
                categoryType = CategoryType.FAVORITES,
                viewModel = viewModel,
                onBack = { viewModel.selectLibraryCategory(null) },
                onOpenSearch = onOpenSearch
            )
        } else if (cat == "downloads") {
            CategoryDetailsScreen(
                categoryType = CategoryType.DOWNLOADS,
                viewModel = viewModel,
                onBack = { viewModel.selectLibraryCategory(null) },
                onOpenSearch = onOpenSearch
            )
        } else if (cat == "playlists") {
            PlaylistsCategoryScreen(
                viewModel = viewModel,
                onPlaylistClick = { viewModel.selectPlaylist(it) },
                onBack = { viewModel.selectLibraryCategory(null) }
            )
        } else if (cat == "device_media") {
            DeviceMediaScreen(
                viewModel = viewModel,
                deviceSongs = deviceSongs,
                onBack = { viewModel.selectLibraryCategory(null) }
            )
        } else {
            MainMetrolistMinimalLibraryView(
                viewModel = viewModel,
                activeCategory = activeCategory,
                onCategoryChange = { viewModel.selectLibraryCategory(it) },
                onPlaylistClick = { viewModel.selectPlaylist(it) },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMetrolistMinimalLibraryView(
    viewModel: MusicViewModel,
    activeCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favoriteTracks.collectAsState()
    val downloads by viewModel.downloadedTracks.collectAsState()
    val deviceSongs by viewModel.deviceSongs.collectAsState()
    val mContext = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppThemeColors.current

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
            // Minimal Top Header with Title "Library" only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Grid Items: Liked, Downloaded, Playlists, Device Media
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 160.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Liked (Favorites)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryChange("favorites") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.favourites),
                                contentDescription = "Favorites",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Favorites",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${favorites.size} songs",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Card 2: Downloaded
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryChange("downloads") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.downloads),
                                contentDescription = "Downloads",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Downloads",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${downloads.size} songs",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Card 3: Dedicated Playlists Button (Opens PlaylistsCategoryScreen)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryChange("playlists") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.playlist),
                                contentDescription = "Playlists",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Playlists",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${playlists.size} playlists",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Card 4: Device Media (Opens DeviceMediaScreen matching screenshots)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryChange("device_media") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.device_media),
                                contentDescription = "Device Media",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Device Media",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${deviceSongs.size} track(s)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// Dedicated Playlists Category Screen with "+" FAB Button
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsCategoryScreen(
    viewModel: MusicViewModel,
    onPlaylistClick: (Playlist) -> Unit,
    onBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val appColors = LocalAppThemeColors.current
    var showCreateDialog by remember { mutableStateOf(false) }

    var selectedPlaylistForMenu by remember { mutableStateOf<Playlist?>(null) }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
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
                    text = "Playlists",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No playlists created yet. Tap '+' to create one!",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 160.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists, key = { "pl_cat_" + it.id }) { pl ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { onPlaylistClick(pl) },
                                    onLongClick = { selectedPlaylistForMenu = pl }
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val plSongs = remember(pl.id) { com.snitrix.snitify.data.db.DatabaseManager.getPlaylistSongs(pl.id) }
                                com.snitrix.snitify.ui.component.PlaylistCollageCover(
                                    logoPath = pl.logoPath,
                                    songs = plSongs,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pl.name,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        val currentTrack by viewModel.currentTrack.collectAsState()

        // Floating Action Button anchored on PlaylistsScreen
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = if (currentTrack != null) 160.dp else 96.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = appColors.primaryAccent,
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = "Create Playlist",
                modifier = Modifier.size(24.dp)
            )
        }

        // Selection Menu BottomSheet on Long Press
        if (selectedPlaylistForMenu != null) {
            val currentPl = selectedPlaylistForMenu!!
            val sheetSongs = remember(currentPl.id) {
                com.snitrix.snitify.data.db.DatabaseManager.getPlaylistSongs(currentPl.id)
            }
            ModalBottomSheet(
                onDismissRequest = { selectedPlaylistForMenu = null },
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 36.dp)
                ) {
                    // Header Row: Playlist collage cover + Name & Song count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.snitrix.snitify.ui.component.PlaylistCollageCover(
                                songs = sheetSongs,
                                logoPath = currentPl.logoPath,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentPl.name,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${sheetSongs.size} songs",
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

                    // Button 1: Edit playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playlistToEdit = currentPl
                                selectedPlaylistForMenu = null
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit playlist",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = "Edit playlist",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Button 2: Delete playlist
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playlistToDelete = currentPl
                                selectedPlaylistForMenu = null
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete playlist",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Text(
                            text = "Delete playlist",
                            color = Color(0xFFFF5252),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Edit Playlist Info Sheet (Matching Screenshot 1)
        if (playlistToEdit != null) {
            val targetPl = playlistToEdit!!
            com.snitrix.snitify.ui.component.EditPlaylistBottomSheet(
                playlist = targetPl,
                onSave = { newName, newLogoPath ->
                    if (newName.isNotBlank()) {
                        com.snitrix.snitify.data.db.DatabaseManager.updatePlaylist(targetPl.id, newName, newLogoPath)
                        viewModel.refreshPlaylists()
                        Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                    }
                },
                onDeletePlaylist = {
                    playlistToDelete = targetPl
                },
                onDismiss = { playlistToEdit = null }
            )
        }

        // Delete Confirmation Dialog
        if (playlistToDelete != null) {
            val pl = playlistToDelete!!
            AlertDialog(
                onDismissRequest = { playlistToDelete = null },
                containerColor = BackgroundBlack,
                title = { Text("Delete Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete '${pl.name}'? This action cannot be undone.", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePlaylist(pl.id)
                            playlistToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { playlistToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showCreateDialog) {
            com.snitrix.snitify.ui.component.EditPlaylistBottomSheet(
                isCreateMode = true,
                onSave = { name, logoPath ->
                    val success = viewModel.createPlaylist(name, logoPath)
                    if (success) {
                        showCreateDialog = false
                    } else {
                        Toast.makeText(context, "A playlist named '$name' already exists!", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showCreateDialog = false }
            )
        }
    }
}

// Dedicated Device Media Screen with Sorting (Latest First, Oldest First, A-Z, Z-A) and Thumbnails
@Composable
fun DeviceMediaScreen(
    viewModel: MusicViewModel,
    deviceSongs: List<Song>,
    onBack: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val appColors = LocalAppThemeColors.current
    val context = LocalContext.current

    val permissionToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var isPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permissionToRequest
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
        if (granted) {
            viewModel.scanDeviceSongs()
        } else {
            Toast.makeText(context, "Permission required to access local device music", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!isPermissionGranted) {
            permissionLauncher.launch(permissionToRequest)
        } else {
            viewModel.scanDeviceSongs()
        }
    }

    val prefs = remember(context) { context.getSharedPreferences("snitify_sort_prefs", android.content.Context.MODE_PRIVATE) }
    var sortOption by remember {
        mutableStateOf(prefs.getString("sort_library_device", "adding_time_new_old") ?: "adding_time_new_old")
    }
    var showSortDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showFoldersDialog by remember { mutableStateOf(false) }
    var newFolderPathText by remember { mutableStateOf("") }
    val deviceMediaFolders by viewModel.deviceMediaFolders.collectAsState()

    val filteredSongs = remember(deviceSongs, sortOption) {
        when (sortOption) {
            "adding_time_new_old", "latest_first" -> deviceSongs.sortedByDescending { it.id.toLongOrNull() ?: 0L }
            "adding_time_old_new", "oldest_first" -> deviceSongs.sortedBy { it.id.toLongOrNull() ?: 0L }
            "title_az", "a_z" -> deviceSongs.sortedBy { it.title.lowercase() }
            "title_za", "z_a" -> deviceSongs.sortedByDescending { it.title.lowercase() }
            else -> deviceSongs
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${deviceSongs.size} track(s)",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = "Menu Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    androidx.compose.material3.DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier
                            .background(BackgroundBlack)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Sort by", color = TextPrimary) },
                            onClick = {
                                showOptionsMenu = false
                                showSortDialog = true
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Refresh list", color = TextPrimary) },
                            onClick = {
                                showOptionsMenu = false
                                viewModel.scanDeviceSongs()
                                Toast.makeText(context, "Refreshing device media...", Toast.LENGTH_SHORT).show()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Media folders", color = TextPrimary) },
                            onClick = {
                                showOptionsMenu = false
                                showFoldersDialog = true
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                if (!isPermissionGranted) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Permission required to access local music on device",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(permissionToRequest) },
                                    colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                                ) {
                                    Text("Grant Permission", color = Color.White)
                                }
                            }
                        }
                    }
                } else if (filteredSongs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No local audio files found in Music or Downloads folders",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredSongs, key = { "dev_page_" + it.id }) { song ->
                        val isCurrentTrack = currentTrack?.id == song.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playSongFromList(song, filteredSongs) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail / Artwork of the music file
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GlassSurface)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = song.finalCover,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (currentTrack?.id == song.id) appColors.primaryAccent else TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = song.artist,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isCurrentTrack) {
                                com.snitrix.snitify.ui.component.RowVisualizer(isPlaying = isPlaying, color = appColors.primaryAccent)
                            }
                        }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Sort Order Sheet (Matching Screenshot 2)
        if (showSortDialog) {
            com.snitrix.snitify.ui.component.SortByBottomSheet(
                selectedKey = sortOption,
                onOptionSelected = { newSort ->
                    sortOption = newSort
                    prefs.edit().putString("sort_library_device", newSort).apply()
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }

        // Media Folders Management Dialog
        if (showFoldersDialog) {
            AlertDialog(
                onDismissRequest = { showFoldersDialog = false },
                title = { Text("Scanned Media Folders", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Only audio files inside these folder names will be scanned:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        deviceMediaFolders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• /$folder/", color = TextPrimary, fontSize = 14.sp)
                                if (deviceMediaFolders.size > 1) {
                                    IconButton(onClick = { viewModel.removeDeviceMediaFolder(folder) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Remove Folder",
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.OutlinedTextField(
                                value = newFolderPathText,
                                onValueChange = { newFolderPathText = it },
                                placeholder = { Text("Folder name...", color = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = appColors.primaryAccent,
                                    unfocusedBorderColor = GlassBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newFolderPathText.isNotBlank()) {
                                        viewModel.addDeviceMediaFolder(newFolderPathText)
                                        newFolderPathText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
                            ) {
                                Text("Add", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFoldersDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Done", color = appColors.primaryAccent)
                    }
                },
                containerColor = BackgroundBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            )
        }
    }
}
