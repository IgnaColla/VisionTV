package com.visiontv.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.visiontv.app.R
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = uiState.errorMessage
    val successMessage = uiState.successMessage

    // Auto-clear messages after 3 seconds
    LaunchedEffect(successMessage, errorMessage) {
        if ((successMessage != null) || (errorMessage != null)) {
            delay(3.seconds)
            viewModel.clearMessages()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Title ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.settings_title), fontSize = 28.sp, color = Color.White)

                FocusableIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_playlist),
                    onClick = { viewModel.showAddDialog() }
                )
            }

            // ── Success/Error Message ──────────────────────────────────────────
            if (successMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B5E20), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = successMessage,
                        color = Color.White,
                    )
                }
            }

            // ── Playlists Section ─────────────────────────────────────────
            Text(stringResource(R.string.active_playlists), fontSize = 18.sp, color = Color.Gray)

            if (uiState.playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists), color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.playlists, key = { it.url }) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onRemove = { viewModel.removePlaylist(playlist) },
                        )
                    }
                }
            }
        }

        // ── Add Playlist Dialog ───────────────────────────────────────
        if (uiState.showAddDialog) {
            AddPlaylistDialog(
                name = uiState.newPlaylistName,
                url = uiState.newPlaylistUrl,
                type = uiState.newPlaylistType,
                errorMessage = errorMessage,
                onNameChange = viewModel::updateNewName,
                onUrlChange = viewModel::updateNewUrl,
                onTypeChange = viewModel::updateNewType,
                onConfirm = { viewModel.addPlaylist() },
                onDismiss = { viewModel.hideAddDialog() },
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: PlaylistSource,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 16.sp,
                )
                androidx.compose.material3.Text(
                    text = playlist.url,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
                androidx.compose.material3.Text(
                    text = when (playlist.type) {
                        PlaylistSourceType.LIVE_TV -> stringResource(R.string.type_live_tv)
                        PlaylistSourceType.MOVIES -> stringResource(R.string.type_movies)
                        PlaylistSourceType.SERIES -> stringResource(R.string.type_series)
                        PlaylistSourceType.PUBLIC_DOMAIN -> "⚖️ Public Domain"
                    },
                    color = Color(0xFF90CAF9),
                    fontSize = 12.sp,
                )
            }

            FocusableIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(R.string.playlist_removed),
                tint = Color(0xFFEF9A9A),
                onClick = onRemove
            )
        }
    }
}

@Composable
private fun FocusableIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(CircleShape)
            .background(if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) Color.White else tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AddPlaylistDialog(
    name: String,
    url: String,
    type: PlaylistSourceType,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onTypeChange: (PlaylistSourceType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(value = false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            androidx.compose.material3.Text(stringResource(R.string.add_playlist), color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.DarkGray,
                    cursorColor = Color.White,
                    focusedContainerColor = Color(0xFF252525),
                    unfocusedContainerColor = Color(0xFF1E1E1E)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { androidx.compose.material3.Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { androidx.compose.material3.Text(stringResource(R.string.playlist_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Type Selector
                Box {
                    OutlinedTextField(
                        value = when (type) {
                            PlaylistSourceType.LIVE_TV -> stringResource(R.string.nav_live_tv)
                            PlaylistSourceType.MOVIES -> stringResource(R.string.nav_movies)
                            PlaylistSourceType.SERIES -> stringResource(R.string.nav_series)
                            PlaylistSourceType.PUBLIC_DOMAIN -> "Public Domain"
                        },
                        onValueChange = {},
                        label = { androidx.compose.material3.Text(stringResource(R.string.playlist_type)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                androidx.compose.material3.Text("▼", color = Color.White)
                            }
                        },
                        colors = textFieldColors
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text(stringResource(R.string.type_live_tv)) },
                            onClick = {
                                onTypeChange(PlaylistSourceType.LIVE_TV)
                                dropdownExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text(stringResource(R.string.type_movies)) },
                            onClick = {
                                onTypeChange(PlaylistSourceType.MOVIES)
                                dropdownExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text(stringResource(R.string.type_series)) },
                            onClick = {
                                onTypeChange(PlaylistSourceType.SERIES)
                                dropdownExpanded = false
                            },
                        )
                    }
                }

                // Error
                errorMessage?.let {
                    androidx.compose.material3.Text(
                        text = it,
                        color = Color(0xFFEF9A9A),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(stringResource(R.string.cancel), color = Color.LightGray)
            }
        },
    )
}
