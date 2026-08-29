package com.visiontv.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visiontv.app.R
import com.visiontv.app.data.model.Channel
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.data.model.PlaybackType
import com.visiontv.app.ui.component.ChannelCard
import com.visiontv.app.viewmodel.TvViewModel

private val ScreenBg = Color(0xFF000000)
private val HeaderBg = Color(0xFF0D0D0D)
private val SearchBg = Color(0xFF1C1C1E)
private val AccentWhite = Color.White
private val TextMuted = Color(0xFF8E8E93)
private val BorderColor = Color(0xFF2C2C2E)

@Composable
fun TvScreen(
    onPlay: (PlaybackItem) -> Unit,
    viewModel: TvViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.selectedChannel) {
        uiState.selectedChannel?.let { channel ->
            onPlay(
                PlaybackItem(
                    url = channel.url,
                    title = channel.name,
                    type = PlaybackType.LIVE_TV,
                    itemId = channel.url,
                    headers = channel.headers,
                ),
            )
            viewModel.clearSelectedChannel()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(HeaderBg)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SearchBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search_channels),
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    singleLine = true,
                    textStyle = TextStyle(color = AccentWhite, fontSize = 15.sp),
                    cursorBrush = SolidColor(Color(0xFF6366F1)),
                    decorationBox = { inner ->
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                stringResource(R.string.search_channels),
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Cleanup button
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (uiState.isValidating) Color.Gray else Color(0xFFEF9A9A))
                    .clickable(enabled = !uiState.isValidating) { viewModel.startCleanup() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isValidating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoDelete,
                            contentDescription = "Cleanup",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (uiState.isValidating) "Validating..." else "Cleanup",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Show Working Toggle
            if (uiState.deadChannels.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (uiState.showOnlyWorking) Color(0xFF4CAF50) else Color(0xFF1C1C1E))
                        .clickable { viewModel.toggleShowOnlyWorking() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.showOnlyWorking) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Filter",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (uiState.showOnlyWorking) "Working Only" else "Show All",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Refresh button
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AccentWhite)
                    .clickable { viewModel.refreshChannels() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.refresh),
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── Main Content ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(ScreenBg)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = AccentWhite)
                        Text(
                            stringResource(R.string.loading_channels),
                            color = TextMuted,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            uiState.errorMessage!!,
                            color = Color(0xFFFF3B30),
                            fontSize = 15.sp
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SearchBg)
                                .clickable { viewModel.refreshChannels() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                stringResource(R.string.retry),
                                color = AccentWhite,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                uiState.channels.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            stringResource(R.string.no_channels_found),
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                    }
                }

                uiState.searchQuery.isNotBlank() -> {
                    // Search Results View
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredChannels, key = { it.url }) { channel ->
                            ChannelCard(
                                channel = channel,
                                isFavorite = uiState.favorites.contains(channel.url),
                                onClick = { viewModel.selectChannel(it) },
                            ) { viewModel.toggleFavorite(it) }
                        }
                    }
                }

                else -> {
                    // Home View with Rows
                    val baseList = if (uiState.showOnlyWorking) {
                        uiState.channels.filter { !uiState.deadChannels.contains(it.url) }
                    } else {
                        uiState.channels
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Row 1: Argentina
                        val argentinaChannels = baseList.filter { channel ->
                            (channel.country?.uppercase() == "AR") || 
                            channel.name.contains("Argentina", ignoreCase = true) ||
                            channel.category.contains("Argentina", ignoreCase = true)
                        }
                        if (argentinaChannels.isNotEmpty()) {
                            item {
                                ChannelRow(
                                    title = "Argentina",
                                    channels = argentinaChannels,
                                    favorites = uiState.favorites,
                                    onChannelClick = viewModel::selectChannel,
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                            }
                        }

                        // Row 2: Favorites & Recents
                        val favoriteChannels = baseList.filter { uiState.favorites.contains(it.url) }
                        if (favoriteChannels.isNotEmpty() || uiState.recentChannels.isNotEmpty()) {
                            item {
                                // Filter recents as well
                                val recentFiltered = uiState.recentChannels.filter { !uiState.showOnlyWorking || !uiState.deadChannels.contains(it.url) }
                                val combined = (recentFiltered + favoriteChannels).distinctBy { it.url }
                                if (combined.isNotEmpty()) {
                                    ChannelRow(
                                        title = "Favorites & Recent",
                                        channels = combined,
                                        favorites = uiState.favorites,
                                        onChannelClick = viewModel::selectChannel,
                                        onToggleFavorite = viewModel::toggleFavorite
                                    )
                                }
                            }
                        }

                        // Row 3: Others
                        val others = baseList.filter { channel ->
                            !argentinaChannels.any { it.url == channel.url } &&
                            !favoriteChannels.any { it.url == channel.url }
                        }
                        if (others.isNotEmpty()) {
                            item {
                                ChannelRow(
                                    title = "Other Channels",
                                    channels = others,
                                    favorites = uiState.favorites,
                                    onChannelClick = viewModel::selectChannel,
                                    onToggleFavorite = viewModel::toggleFavorite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    title: String,
    channels: List<Channel>,
    favorites: Set<String>,
    onChannelClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(channels, key = { it.url }) { channel ->
                ChannelCard(
                    channel = channel,
                    isFavorite = favorites.contains(channel.url),
                    onClick = onChannelClick,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.width(200.dp)
                )
            }
        }
    }
}
