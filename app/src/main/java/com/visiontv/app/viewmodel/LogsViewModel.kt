package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.repository.IptvRepository
import com.visiontv.app.util.AppLogger
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IptvRepository()
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            AppLogger.logs.collect { logs ->
                _uiState.update { it.copy(logs = logs.reversed()) } // Newest first
            }
        }
    }

    fun clearLogs() {
        AppLogger.clearLogs()
    }

    fun refreshPlaylists() {
        viewModelScope.launch {
            val playlists = preferences.playlistsFlow.first()
            AppLogger.info("Manual refresh triggered from logs...", listOf("logs", "refresh"))
            repository.fetchAllPlaylists(playlists)
        }
    }
}
