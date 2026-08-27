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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.data.model.PlaybackType
import com.visiontv.app.ui.component.EpisodePicker
import com.visiontv.app.ui.component.FeaturedHero
import com.visiontv.app.ui.component.SeriesCard
import com.visiontv.app.viewmodel.SeriesViewModel

private val ScreenBg = Color(0xFF000000)
private val HeaderBg = Color(0xFF0D0D0D)
private val SearchBg = Color(0xFF1C1C1E)
private val AccentWhite = Color.White
private val TextMuted = Color(0xFF8E8E93)
private val PillActive = Color.White
private val PillInactive = Color(0xFF1C1C1E)
private val BorderColor = Color(0xFF2C2C2E)

@Composable
fun SeriesScreen(
    onPlay: (PlaybackItem) -> Unit,
    viewModel: SeriesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(HeaderBg)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                        contentDescription = stringResource(R.string.search_series),
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
                                    stringResource(R.string.search_series),
                                    color = TextMuted,
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AccentWhite)
                        .clickable { viewModel.refreshSeries() }
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

            // ── Categories ────────────────────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderBg)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories) { cat ->
                    val isActive = cat == uiState.activeCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) PillActive else PillInactive)
                            .border(
                                1.dp,
                                if (isActive) Color.Transparent else BorderColor,
                                RoundedCornerShape(50)
                            )
                            .clickable { viewModel.updateCategory(cat) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (isActive) Color.Black else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
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
                                stringResource(R.string.loading_series),
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
                                    .clickable { viewModel.refreshSeries() }
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

                    uiState.filteredSeries.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.no_series_found),
                                color = TextMuted,
                                fontSize = 16.sp
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            contentPadding = PaddingValues(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Hero Section
                            val featured = uiState.series.find { it.backdropUrl != null }
                            if ((featured != null) && uiState.searchQuery.isBlank() && (uiState.activeCategory == "All")) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    FeaturedHero(
                                        title = featured.title,
                                        backdropUrl = featured.backdropUrl,
                                        rating = featured.rating,
                                        releaseDate = null,
                                        overview = featured.overview,
                                    ) { viewModel.selectSeries(featured) }
                                }
                            }

                            items(uiState.filteredSeries, key = { it.id }) { series ->
                                SeriesCard(
                                    series = series,
                                    isFavorite = uiState.favorites.contains(series.id),
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onClick = { viewModel.selectSeries(it) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Episode Picker Overlay ────────────────────────────────────────
        if (uiState.selectedSeries != null) {
            EpisodePicker(
                series = uiState.selectedSeries!!,
                onEpisodeSelected = { episode ->
                    onPlay(
                        PlaybackItem(
                            url = episode.streamUrl,
                            title = "${uiState.selectedSeries!!.title} - S${episode.seasonNumber}E${episode.episodeNumber}",
                            type = PlaybackType.SERIES,
                            itemId = uiState.selectedSeries!!.id,
                            headers = episode.headers
                        )
                    )
                    viewModel.clearSelectedSeries()
                },
                onClose = viewModel::clearSelectedSeries,
            )
        }
    }
}
