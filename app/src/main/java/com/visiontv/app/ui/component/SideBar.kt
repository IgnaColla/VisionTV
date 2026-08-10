package com.visiontv.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp

@Composable
fun SideBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SideBarItem(
            icon = Icons.Default.Tv,
            label = "TV",
            isSelected = currentRoute == "tv",
            onClick = { onNavigate("tv") }
        )
        SideBarItem(
            icon = Icons.Default.Movie,
            label = "Movies",
            isSelected = currentRoute == "movies",
            onClick = { onNavigate("movies") }
        )
        SideBarItem(
            icon = Icons.Default.VideoLibrary,
            label = "Series",
            isSelected = currentRoute == "series",
            onClick = { onNavigate("series") }
        )
        
        Box(modifier = Modifier.weight(1f)) // Spacer
        
        SideBarItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            isSelected = currentRoute == "settings",
            onClick = { onNavigate("settings") }
        )
    }
}

@Composable
private fun SideBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFocused -> Color.White.copy(alpha = 0.2f)
                    isSelected -> Color.White.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected || isFocused) Color.White else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
    }
}
