package com.visiontv.app.ui.screen

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import com.visiontv.app.R
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.player.PlayerViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text as TvText

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    item: PlaybackItem,
    onClose: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val player = viewModel.getPlayer()
    val focusRequester = remember { FocusRequester() }
    val errorMessage = uiState.errorMessage

    var showResolutionDialog by remember { mutableStateOf(value = false) }

    val iconClose: ImageVector = Icons.Filled.Close
    val iconFastRewind: ImageVector = Icons.Filled.FastRewind
    val iconPause: ImageVector = Icons.Filled.Pause
    val iconPlayArrow: ImageVector = Icons.Filled.PlayArrow
    val iconFastForward: ImageVector = Icons.Filled.FastForward
    val iconVolume: ImageVector = Icons.AutoMirrored.Filled.VolumeUp
    val iconResolution: ImageVector = Icons.Filled.HighQuality
    val iconFavorite: ImageVector = if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
    val favColor = if (uiState.isFavorite) Color(0xFFFF3B30) else Color.White

    LaunchedEffect(item) { viewModel.prepare(item) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { player.pause() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter,
                        Key.Enter -> {
                            viewModel.togglePlayPause()
                            true
                        }
                        Key.DirectionRight,
                        Key.MediaFastForward -> {
                            viewModel.seekForward()
                            true
                        }
                        Key.DirectionLeft,
                        Key.MediaRewind -> {
                            viewModel.seekBackward()
                            true
                        }
                        Key.DirectionUp -> {
                            viewModel.setVolume(uiState.volume + 0.1f)
                            true
                        }
                        Key.DirectionDown -> {
                            viewModel.setVolume(uiState.volume - 0.1f)
                            true
                        }
                        Key.Back,
                        Key.Escape -> {
                            onClose()
                            true
                        }
                        else -> {
                            viewModel.showControlsTemporarily()
                            false
                        }
                    }
                } else false
            }
    ) {
        // ── Video ─────────────────────────────────────────────────────────
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Buffering ─────────────────────────────────────────────────────
        if (uiState.isBuffering && (errorMessage == null)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // ── Error ─────────────────────────────────────────────────────────
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = errorMessage, color = Color.White, fontSize = 18.sp)
                    Button(onClick = { viewModel.retry() }) { Text(stringResource(R.string.retry)) }
                    Button(onClick = onClose) { Text(stringResource(R.string.player_back)) }
                }
            }
        }

        // ── Controls Overlay ──────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showControls && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize()) {

                // Top Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent))
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            text = uiState.title,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        if (uiState.selectedResolution != null) {
                            Text(
                                text = uiState.selectedResolution!!.label,
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Row(modifier = Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier.background(Color(0x44FFFFFF), CircleShape)
                        ) {
                            Icon(imageVector = iconFavorite, contentDescription = "Favorite", tint = favColor)
                        }
                        IconButton(
                            onClick = { showResolutionDialog = true },
                            modifier = Modifier.background(Color(0x44FFFFFF), CircleShape)
                        ) {
                            Icon(imageVector = iconResolution, contentDescription = "Resolution", tint = Color.White)
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.background(Color(0x44FFFFFF), CircleShape)
                        ) {
                            Icon(imageVector = iconClose, contentDescription = stringResource(R.string.player_close), tint = Color.White)
                        }
                    }
                }

                // Volume Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(40.dp, 200.dp)
                        .background(Color(0x66000000), RoundedCornerShape(20.dp))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = iconVolume, contentDescription = "Volume", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = uiState.volume,
                            onValueChange = { viewModel.setVolume(it) },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    rotationZ = 270f
                                    transformOrigin = TransformOrigin.Center
                                }
                        )
                    }
                }

                // Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                        )
                        .padding(vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.seekBackward() }) {
                            Icon(
                                imageVector = iconFastRewind,
                                contentDescription = stringResource(R.string.player_rewind),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) iconPause else iconPlayArrow,
                                contentDescription = if (uiState.isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = { viewModel.seekForward() }) {
                            Icon(
                                imageVector = iconFastForward,
                                contentDescription = stringResource(R.string.player_forward),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }

        // Resolution Selection Dialog
        if (showResolutionDialog) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)),
                onClick = { showResolutionDialog = false }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier
                            .size(300.dp, 400.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        color = Color(0xFF1C1C1E)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TvText(
                                text = "Select Resolution",
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LazyColumn {
                                item {
                                    ListItem(
                                        selected = uiState.selectedResolution == null,
                                        onClick = {
                                            viewModel.setResolution(null)
                                            showResolutionDialog = false
                                        },
                                        headlineContent = { TvText("Auto", color = Color.White) }
                                    )
                                }
                                items(uiState.availableResolutions) { resolution ->
                                    ListItem(
                                        selected = uiState.selectedResolution == resolution,
                                        onClick = {
                                            viewModel.setResolution(resolution)
                                            showResolutionDialog = false
                                        },
                                        headlineContent = { TvText(resolution.label, color = Color.White) },
                                        supportingContent = { TvText("${resolution.width}x${resolution.height} • ${resolution.bitrate / 1000}kbps", color = Color.LightGray) }
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
