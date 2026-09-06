package com.visiontv.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.visiontv.app.data.model.PlaylistSource
import com.visiontv.app.data.model.PlaylistSourceType
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.playlistsFlow.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingPlaylist = null) }
    }

    fun showEditDialog(playlist: PlaylistSource) {
        _uiState.update { 
            it.copy(
                showAddDialog = true, 
                editingPlaylist = playlist,
                newPlaylistName = playlist.name,
                newPlaylistUrl = playlist.url,
                newPlaylistType = playlist.type
            ) 
        }
    }

    fun hideAddDialog() {
        _uiState.update { 
            it.copy(
                showAddDialog = false, 
                errorMessage = null, 
                newPlaylistName = "", 
                newPlaylistUrl = "",
                editingPlaylist = null
            ) 
        }
    }

    fun updateNewName(name: String) {
        _uiState.update { it.copy(newPlaylistName = name) }
    }

    fun updateNewUrl(url: String) {
        _uiState.update { it.copy(newPlaylistUrl = url) }
    }

    fun updateNewType(type: PlaylistSourceType) {
        _uiState.update { it.copy(newPlaylistType = type) }
    }

    fun addPlaylist() {
        val state = _uiState.value
        if (state.newPlaylistName.isBlank() || state.newPlaylistUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please complete all fields") }
            return
        }

        viewModelScope.launch {
            val current = state.playlists.toMutableList()
            val newPlaylist = PlaylistSource(state.newPlaylistName, state.newPlaylistUrl, state.newPlaylistType)
            
            if (state.editingPlaylist != null) {
                // Update existing
                val index = current.indexOfFirst { it.url == state.editingPlaylist.url }
                if (index != -1) {
                    current[index] = newPlaylist
                }
            } else {
                // Add new
                if (current.any { it.url == newPlaylist.url }) {
                    _uiState.update { it.copy(errorMessage = "URL already exists") }
                    return@launch
                }
                current.add(newPlaylist)
            }

            preferences.savePlaylists(current)
            _uiState.update { it.copy(
                successMessage = if (state.editingPlaylist != null) "Playlist updated" else "Playlist added",
                showAddDialog = false,
                newPlaylistName = "",
                newPlaylistUrl = "",
                editingPlaylist = null
            ) }
        }
    }

    fun removePlaylist(playlist: PlaylistSource) {
        viewModelScope.launch {
            val current = _uiState.value.playlists
            preferences.savePlaylists(current.filter { it.url != playlist.url })
            _uiState.update { it.copy(successMessage = "Playlist removed") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
