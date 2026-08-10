package com.visiontv.app.ui.component

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.Text
import com.visiontv.app.data.model.Episode
import com.visiontv.app.data.model.Series

private val OverlayBg = Color(0xFF0D0D0D)
private val SelectionBg = Color(0xFF1C1C1E)
private val AccentWhite = Color.White
private val TextMuted = Color(0xFF8E8E93)

@Composable
fun EpisodePicker(
    series: Series,
    onEpisodeSelected: (Episode) -> Unit,
    onClose: () -> Unit
) {
    var selectedSeason by remember { mutableStateOf(series.seasons.keys.firstOrNull() ?: 1) }
    val episodes = series.seasons[selectedSeason] ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxSize(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .background(OverlayBg)
                .clickable(enabled = false) { } // prevent closing when clicking inside
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = series.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentWhite
            )

            // Season Selection
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Seasons:", color = TextMuted, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(series.seasons.keys.toList().sorted()) { season ->
                        val isActive = season == selectedSeason
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isActive) Color.White else SelectionBg)
                                .clickable { selectedSeason = season }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Season $season",
                                color = if (isActive) Color.Black else AccentWhite,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Episode List
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes) { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SelectionBg)
                            .clickable { onEpisodeSelected(episode) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "E${episode.episodeNumber}",
                            color = TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = episode.title,
                            color = AccentWhite,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(50))
                    .background(SelectionBg)
                    .clickable { onClose() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Close", color = AccentWhite)
            }
        }
    }
}
