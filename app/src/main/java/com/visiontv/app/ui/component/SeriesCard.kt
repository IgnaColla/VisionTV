package com.visiontv.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.data.model.Series

import java.util.Locale

private val CardBg = Color(0xFF1C1C1E)
private val CardBorderFocused = Color.White
private val CardBorderUnfocused = Color(0xFF2C2C2E)
private val AccentWhite = Color.White
private val FavColor = Color(0xFFFF3B30)
private val RatingGold = Color(0xFFFFD700)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCard(
    series: Series,
    isFavorite: Boolean,
    onClick: (Series) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((Series) -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(value = false) }
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1.0f, label = "scale")

    Box(
        modifier = modifier
            .width(160.dp)
            .height(240.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(CardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CardBorderFocused else CardBorderUnfocused,
                shape = RoundedCornerShape(8.dp),
            )
            .combinedClickable(
                onClick = { onClick(series) },
                onLongClick = { onToggleFavorite?.invoke(series) },
            ),
    ) {
        AsyncImage(
            model = series.posterUrl,
            contentDescription = series.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f,
                    ),
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (series.sourceType == PlaylistSourceType.PUBLIC_DOMAIN) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Public Domain",
                            fontSize = 8.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (onToggleFavorite != null) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) FavColor else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isFocused && (onToggleFavorite != null)) {
                    Text(
                        text = "Hold to Favorite",
                        fontSize = 9.sp,
                        color = FavColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = series.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (isFocused) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if ((series.rating != null) && (series.rating > 0)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = RatingGold,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", series.rating),
                                    fontSize = 10.sp,
                                    color = AccentWhite
                                )
                            }
                        }
                        
                        val seasonsCount = series.seasons.size
                        if (seasonsCount > 0) {
                            Text(
                                text = if (seasonsCount == 1) "1 Season" else "$seasonsCount Seasons",
                                fontSize = 10.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        }
    }
}
