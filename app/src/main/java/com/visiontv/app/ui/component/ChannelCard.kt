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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import com.visiontv.app.data.model.Channel

private val CardBg = Color(0xFF1C1C1E)
private val CardBorderFocused = Color.White
private val CardBorderUnfocused = Color(0xFF2C2C2E)
private val AccentWhite = Color.White
private val TextMuted = Color(0xFF8E8E93)
private val FavColor = Color(0xFFFF3B30)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onClick: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((Channel) -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(value = false) }
    val scale by animateFloatAsState(if (isFocused) 1.05f else 1.0f, label = "scale")
    val metadata = channel.metadata
    val hasMetadata = metadata?.posterUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (hasMetadata) 280.dp else 180.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) CardBorderFocused else CardBorderUnfocused,
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(
                onClick = { onClick(channel) },
                onLongClick = { onToggleFavorite?.invoke(channel) },
            ),
    ) {
        if ((metadata != null) && hasMetadata) {
            // Movie/Series Poster Style
            AsyncImage(
                model = metadata.posterUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Bottom Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 400f,
                        ),
                    ),
            )
        } else {
            // Live TV Logo Style
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }

        // Shared Overlay Elements (Name & Favorite Status)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (onToggleFavorite != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) FavColor else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isFocused && (onToggleFavorite != null)) {
                    Text(
                        text = "Hold to Favorite",
                        fontSize = 10.sp,
                        color = FavColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = channel.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!hasMetadata && channel.category.isNotBlank()) {
                    Text(
                        text = channel.category.split(":").first().trim(),
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Play Indicator (only when focused)
        if (isFocused) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
