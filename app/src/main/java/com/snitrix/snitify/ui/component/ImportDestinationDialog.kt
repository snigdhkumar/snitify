package com.snitrix.snitify.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.ui.theme.LocalAppThemeColors
import com.snitrix.snitify.ui.theme.TextPrimary
import com.snitrix.snitify.ui.theme.TextSecondary

enum class ImportTargetType {
    LIKED_SONGS,
    PLAYLIST
}

@Composable
fun ImportDestinationDialog(
    suggestedName: String,
    songCount: Int,
    onDismiss: () -> Unit,
    onConfirmImport: (target: ImportTargetType, playlistName: String) -> Unit
) {
    val appColors = LocalAppThemeColors.current

    val initialTarget = if (suggestedName.contains("liked", ignoreCase = true) || suggestedName.contains("favorite", ignoreCase = true)) {
        ImportTargetType.LIKED_SONGS
    } else {
        ImportTargetType.PLAYLIST
    }

    var selectedTarget by remember { mutableStateOf(initialTarget) }
    var playlistName by remember { mutableStateOf(suggestedName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = {
            Column {
                Text(
                    text = "Import Music File",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Found $songCount song(s) in file",
                    color = appColors.primaryAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Import Target:",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Liked Songs (Favorites)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTarget == ImportTargetType.LIKED_SONGS) Color(0xFF2A2A2A) else Color.Transparent)
                        .clickable { selectedTarget = ImportTargetType.LIKED_SONGS }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTarget == ImportTargetType.LIKED_SONGS,
                        onClick = { selectedTarget = ImportTargetType.LIKED_SONGS },
                        colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liked Songs (Favorites)",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Option 2: Playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTarget == ImportTargetType.PLAYLIST) Color(0xFF2A2A2A) else Color.Transparent)
                        .clickable { selectedTarget = ImportTargetType.PLAYLIST }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedTarget == ImportTargetType.PLAYLIST,
                        onClick = { selectedTarget = ImportTargetType.PLAYLIST },
                        colors = RadioButtonDefaults.colors(selectedColor = appColors.primaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Playlist",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (selectedTarget == ImportTargetType.PLAYLIST) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Playlist Name:",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        singleLine = true,
                        placeholder = { Text("Enter playlist name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = appColors.primaryAccent,
                            unfocusedBorderColor = Color(0xFF444444),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (selectedTarget == ImportTargetType.PLAYLIST) playlistName.trim().ifEmpty { "Imported Playlist" } else ""
                    onConfirmImport(selectedTarget, finalName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = appColors.primaryAccent)
            ) {
                Text(text = "Import", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}
