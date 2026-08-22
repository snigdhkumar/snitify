package com.snitrix.snitify.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snitrix.snitify.R
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.ui.component.ImportDestinationDialog
import com.snitrix.snitify.ui.component.ImportTargetType
import com.snitrix.snitify.ui.screens.settings.components.AppearancePreviewCard
import com.snitrix.snitify.ui.screens.settings.components.SettingsItem
import com.snitrix.snitify.ui.screens.settings.components.SettingsSection
import com.snitrix.snitify.ui.theme.ThemeManager
import com.snitrix.snitify.utils.BackupRestoreManager
import com.snitrix.snitify.utils.ImportParser

@Composable
fun SettingsMainStep(
    onNavigateToAppearance: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToUpdate: () -> Unit = {},
    onBackClick: () -> Unit,
    viewModel: com.snitrix.snitify.ui.viewmodel.MusicViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appColors = com.snitrix.snitify.ui.theme.LocalAppThemeColors.current
    val wavySliderEnabled by ThemeManager.wavySliderEnabled.collectAsState()

    var pendingParsedSongs by remember { mutableStateOf<List<Song>?>(null) }
    var pendingSuggestedName by remember { mutableStateOf("") }

    // Backup & Restore Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = BackupRestoreManager.exportBackupToUri(context, uri)
            if (success) {
                Toast.makeText(context, "App data exported successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to export app data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // First check if it's a native Snitify backup JSON
            val isNativeBackup = BackupRestoreManager.importBackupFromUri(context, uri)
            if (isNativeBackup) {
                viewModel?.refreshAllData()
                Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show()
            } else {
                // Parse external CSV / JSON / TXT file
                val fileName = getFileNameFromUri(context, uri)
                val (suggestedName, songs) = ImportParser.parseUri(context, uri, fileName)
                if (songs.isNotEmpty()) {
                    pendingSuggestedName = suggestedName
                    pendingParsedSongs = songs
                } else {
                    Toast.makeText(context, "No valid songs found in file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (pendingParsedSongs != null) {
        ImportDestinationDialog(
            suggestedName = pendingSuggestedName,
            songCount = pendingParsedSongs?.size ?: 0,
            onDismiss = { pendingParsedSongs = null },
            onConfirmImport = { targetType, playlistName ->
                pendingParsedSongs?.let { songs ->
                    if (targetType == ImportTargetType.LIKED_SONGS) {
                        viewModel?.startImportToFavorites(songs) { added, skipped ->
                            val msg = if (skipped > 0) {
                                "Imported $added new song(s) into Favorites ($skipped duplicate(s) skipped)"
                            } else {
                                "Imported $added song(s) into Favorites!"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        viewModel?.startImportToPlaylist(playlistName, songs) { res ->
                            val actionText = if (res.isMerged) "Merged" else "Imported"
                            val msg = if (res.skippedCount > 0) {
                                "$actionText ${res.addedCount} new song(s) into '${res.playlistName}' (${res.skippedCount} duplicate(s) skipped)"
                            } else {
                                "$actionText ${res.addedCount} song(s) into '${res.playlistName}'"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                pendingParsedSongs = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .statusBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Appearance Section
            item {
                SettingsSection(title = "APPEARANCE") {
                    SettingsItem(
                        title = "App Color Theme",
                        description = "Choose your custom color accent",
                        iconRes = R.drawable.palette,
                        onClick = onNavigateToAppearance
                    )
                }
            }

            // 2. Customization Section
            item {
                SettingsSection(title = "CUSTOMIZATION") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsItem(
                            title = "Wavy Seekbar Slider",
                            description = "Enable animated wavy progress slider",
                            iconRes = R.drawable.sliders,
                            trailingContent = {
                                Switch(
                                    checked = wavySliderEnabled,
                                    onCheckedChange = { enabled ->
                                        ThemeManager.setWavySliderEnabled(enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = appColors.primaryAccent,
                                        uncheckedThumbColor = Color(0xFFA7A7A7),
                                        uncheckedTrackColor = Color(0xFF282828)
                                    )
                                )
                            }
                        )

                        AppearancePreviewCard(
                            isWavyEnabled = wavySliderEnabled,
                            label = "Wavy Seekbar Preview"
                        )
                    }
                }
            }

            // 3. Data Section
            item {
                SettingsSection(title = "DATA") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsItem(
                            title = "Export App Data",
                            description = "Backup your playlists and favorites to a JSON file",
                            iconRes = R.drawable.backup,
                            onClick = { exportLauncher.launch("snitify_backup.json") }
                        )

                        SettingsItem(
                            title = "Import External Backup",
                            description = "Import Spotify CSV, JSON, or TXT playlists & favorites",
                            iconRes = R.drawable.restore,
                            onClick = { importLauncher.launch("*/*") }
                        )
                    }
                }
            }

            // 4. Updates Section
            item {
                SettingsSection(title = "UPDATES") {
                    SettingsItem(
                        title = "App Updates",
                        description = "Check for new releases, background downloading & install",
                        iconRes = R.drawable.ic_download,
                        onClick = onNavigateToUpdate
                    )
                }
            }

            // 5. About Section
            item {
                SettingsSection(title = "ABOUT") {
                    SettingsItem(
                        title = "App & Developer Details",
                        description = "Version ${com.snitrix.snitify.BuildConfig.VERSION_NAME}, licenses, and developer information",
                        iconRes = R.drawable.info,
                        onClick = onNavigateToAbout
                    )
                }
            }

            // 5. Bottom Brand & Version Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logosnitify),
                        contentDescription = "Snitify Logo",
                        modifier = Modifier.height(36.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Version 6",
                        color = Color(0xFF727272),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var name = "imported_file"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return name
}
