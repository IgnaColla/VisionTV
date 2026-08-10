package com.visiontv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.visiontv.app.ui.component.SideBar
import com.visiontv.app.ui.screen.MoviesScreen
import com.visiontv.app.ui.screen.SeriesScreen
import com.visiontv.app.ui.screen.SettingsScreen
import com.visiontv.app.ui.screen.TvScreen
import com.visiontv.app.ui.theme.VisionTVTheme

@Composable
fun VisionTVApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    VisionTVTheme {
        Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Sidebar fijo a la izquierda (estilo TV)
            Box(modifier = Modifier.width(80.dp).fillMaxHeight()) {
                SideBar(
                    currentRoute = currentRoute ?: "tv",
                    onNavigate = { route ->
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
                )
            }

            // Contenido principal
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = "tv"
                ) {
                    composable("tv") { TvScreen() }
                    composable("movies") { MoviesScreen() }
                    composable("series") { SeriesScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}
