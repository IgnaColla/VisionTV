package com.visiontv.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.data.model.PlaybackType
import com.visiontv.app.ui.component.ChannelCard
import com.visiontv.app.ui.component.EpisodePicker
import com.visiontv.app.ui.component.MovieCard
import com.visiontv.app.ui.component.SeriesCard
import com.visiontv.app.viewmodel.FavoritesViewModel

private val ScreenBg = Color(0xFF000000)
private val HeaderBg = Color(0xFF0D0D0D)
private val TextMuted = Color(0xFF8E8E93)

@Composable
fun FavoritesScreen(
    onPlay: (PlaybackItem) -> Unit,
    viewModel: FavoritesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val selectedSeries by viewModel.selectedSeries.collectAsStateWithLifecycle()

    LaunchedEffect(selectedChannel) {
        selectedChannel?.let { channel ->
            onPlay(
                PlaybackItem(
                    url = channel.url,
                    title = channel.name,
                    type = PlaybackType.LIVE_TV,
                    itemId = channel.url,
                    headers = channel.headers,
                ),
            )
            viewModel.clearChannelSelection()
        }
    }

    LaunchedEffect(selectedMovie) {
        selectedMovie?.let { movie ->
            onPlay(
                PlaybackItem(
                    url = movie.streamUrl,
                    title = movie.title,
                    type = PlaybackType.MOVIE,
                    itemId = movie.streamUrl,
                    headers = movie.headers,
                )
            )
            viewModel.clearMovieSelection()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Favorites",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (uiState.favoriteChannels.isEmpty() && uiState.favoriteMovies.isEmpty() && uiState.favoriteSeries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No favorites added yet", color = TextMuted, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    if (uiState.favoriteChannels.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "TV Channels",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.favoriteChannels) { channel ->
                                        ChannelCard(
                                            channel = channel,
                                            isFavorite = true,
                                            onClick = { viewModel.selectChannel(it) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it.url) },
                                            modifier = Modifier.width(200.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.favoriteMovies.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "Movies",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.favoriteMovies) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            isFavorite = true,
                                            onClick = { viewModel.selectMovie(it) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it.streamUrl) },
                                            modifier = Modifier.width(160.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.favoriteSeries.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "Series",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(uiState.favoriteSeries) { series ->
                                        SeriesCard(
                                            series = series,
                                            isFavorite = true,
                                            onClick = { viewModel.selectSeries(it) },
                                            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                                            modifier = Modifier.width(160.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedSeries != null) {
            EpisodePicker(
                series = selectedSeries!!,
                onEpisodeSelected = { episode ->
                    onPlay(
                        PlaybackItem(
                            url = episode.streamUrl,
                            title = "${selectedSeries!!.title} - S${episode.seasonNumber}E${episode.episodeNumber}",
                            type = PlaybackType.SERIES,
                            itemId = selectedSeries!!.id,
                            headers = episode.headers
                        )
                    )
                    viewModel.clearSeriesSelection()
                },
                onClose = viewModel::clearSeriesSelection
            )
        }
    }
}
