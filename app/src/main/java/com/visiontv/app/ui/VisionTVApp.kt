package com.visiontv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.visiontv.app.ui.component.SideBar
import com.visiontv.app.ui.screen.FavoritesScreen
import com.visiontv.app.ui.screen.LogsScreen
import com.visiontv.app.ui.screen.MoviesScreen
import com.visiontv.app.ui.screen.SeriesScreen
import com.visiontv.app.ui.screen.SettingsScreen
import com.visiontv.app.ui.screen.TvScreen
import com.visiontv.app.ui.screen.PlayerScreen
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.ui.theme.VisionTVTheme

@Composable
fun VisionTVApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    var activePlayerItem by remember { mutableStateOf<PlaybackItem?>(null) }

    VisionTVTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                // Sidebar fijo a la izquierda (estilo TV)
                Box(modifier = Modifier.width(80.dp).fillMaxHeight()) {
                    SideBar(
                        currentRoute = currentRoute ?: "tv",
                    ) { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }

                // Contenido principal
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = "tv",
                    ) {
                        composable("tv") { TvScreen(onPlay = { item: PlaybackItem -> activePlayerItem = item }) }
                        composable("movies") { MoviesScreen(onPlay = { item: PlaybackItem -> activePlayerItem = item }) }
                        composable("series") { SeriesScreen(onPlay = { item: PlaybackItem -> activePlayerItem = item }) }
                        composable("favorites") { FavoritesScreen(onPlay = { item: PlaybackItem -> activePlayerItem = item }) }
                        composable("logs") { LogsScreen() }
                        composable("settings") { SettingsScreen() }
                    }
                }
            }

            // Global Fullscreen Player
            if (activePlayerItem != null) {
                PlayerScreen(
                    item = activePlayerItem!!,
                    onClose = { activePlayerItem = null }
                )
            }
        }
    }
}
