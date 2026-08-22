package com.snitrix.snitify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.snitrix.snitify.data.db.Playlist
import com.snitrix.snitify.ui.theme.BackgroundBlack
import com.snitrix.snitify.ui.theme.GlassBorder
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistBottomSheet(
    playlist: Playlist = Playlist(id = 0, name = "", logoPath = null),
    isCreateMode: Boolean = false,
    onSave: (newName: String, logoPath: String?) -> Unit,
    onDeletePlaylist: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppThemeColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameText by remember { mutableStateOf(playlist.name) }
    var selectedLogoPath by remember { mutableStateOf<String?>(playlist.logoPath) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = copyUriToInternalStorage(context, it)
            if (savedPath != null) {
                selectedLogoPath = savedPath
            }
        }
    }

    val isSaveEnabled = nameText.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header Bar: Cancel | Title | Save/Create
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = if (isCreateMode) "Create playlist" else "Name & details",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        val trimmedName = nameText.trim()
                        if (isSaveEnabled && trimmedName.isNotBlank()) {
                            val allPlaylists = com.snitrix.snitify.data.db.DatabaseManager.getAllPlaylists()
                            val isDuplicate = allPlaylists.any { pl ->
                                pl.id != playlist.id && pl.name.equals(trimmedName, ignoreCase = true)
                            }
                            if (isDuplicate) {
                                android.widget.Toast.makeText(context, "A playlist named '$trimmedName' already exists!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onSave(trimmedName, selectedLogoPath)
                                onDismiss()
                            }
                        }
                    },
                    enabled = isSaveEnabled
                ) {
                    Text(
                        text = if (isCreateMode) "Create" else "Save",
                        color = if (isSaveEnabled) appColors.primaryAccent else appColors.primaryAccent.copy(alpha = 0.4f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Main Details Row: Artwork Box on left, Name on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Artwork Box with Edit Badge
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF242424))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val songs = remember(playlist.id) {
                        if (playlist.id > 0) com.snitrix.snitify.data.db.DatabaseManager.getPlaylistSongs(playlist.id)
                        else emptyList()
                    }
                    PlaylistCollageCover(
                        songs = songs,
                        logoPath = selectedLogoPath,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    )

                    // Edit Pencil Badge Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Right side: Single Dark Text Field for Playlist Name
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = { Text("Playlist name", color = TextSecondary, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF282828),
                            unfocusedContainerColor = Color(0xFF242424),
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
                }
            }

            if (!isCreateMode && onDeletePlaylist != null) {
                Spacer(modifier = Modifier.height(28.dp))

                // Action: Delete Playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onDismiss()
                            onDeletePlaylist()
                        }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "Delete playlist",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "playlist_cover_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
