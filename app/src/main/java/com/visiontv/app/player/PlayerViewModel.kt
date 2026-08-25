package com.visiontv.app.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.visiontv.app.data.model.PlaybackItem
import com.visiontv.app.data.model.PlaybackType
import com.visiontv.app.util.PreferencesManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerInfo = ExoPlayerFactory.create(application)
    private val exoPlayer: ExoPlayer = playerInfo.first
    private val headerFactory: HeaderDataSourceFactory = playerInfo.second
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentItem: PlaybackItem? = null
    private var hideControlsJob: Job? = null

    private val listener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) scheduleHideControls()
        }

        override fun onPlaybackStateChanged(state: Int) {
            _uiState.update { it.copy(
                isBuffering = state == Player.STATE_BUFFERING,
                isEnded = state == Player.STATE_ENDED,
            ) }
            if (state == Player.STATE_READY) {
                updateResolutions()
            }
        }

        @OptIn(UnstableApi::class)
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            updateResolutions()
        }

        override fun onPlayerError(error: PlaybackException) {
            val errorDetails = "Code: ${error.errorCode} (${error.errorCodeName})"
            com.visiontv.app.util.AppLogger.error("Playback error: $errorDetails", listOf("ExoPlayer"), error)
            _uiState.update { it.copy(
                isBuffering = false,
                errorMessage = "Playback error ($errorDetails). Please check your connection.",
            ) }
        }
    }

    init {
        exoPlayer.addListener(listener)
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            preferences.favoritesFlow.collect { favorites ->
                currentItem?.let { item ->
                    _uiState.update { it.copy(isFavorite = favorites.contains(item.itemId)) }
                }
            }
        }
    }

    fun getPlayer(): ExoPlayer = exoPlayer

    @OptIn(UnstableApi::class)
    fun prepare(item: PlaybackItem) {
        currentItem = item
        _uiState.update { PlayerUiState(title = item.title) }

        // Check favorite status immediately
        viewModelScope.launch {
            val favorites = preferences.favoritesFlow.first()
            _uiState.update { it.copy(isFavorite = favorites.contains(item.itemId)) }
        }

        // Apply headers to the factory so segments also get them
        headerFactory.setHeaders(item.headers)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(item.url)
            .apply {
                // Explicitly set MIME type for HLS if URL contains .m3u8 or it's a Live TV stream
                if (item.url.contains(".m3u8", ignoreCase = true) ||
                    item.type == PlaybackType.LIVE_TV) {
                    setMimeType(MimeTypes.APPLICATION_M3U8)
                }
            }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun toggleFavorite() {
        val item = currentItem ?: return
        viewModelScope.launch {
            val favorites = preferences.favoritesFlow.first()
            val updated = if (favorites.contains(item.itemId)) favorites - item.itemId
            else favorites + item.itemId
            preferences.saveFavorites(updated)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        showControlsTemporarily()
    }

    fun seekForward() {
        if (exoPlayer.isCurrentMediaItemSeekable) {
            exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) // +10s
        }
        showControlsTemporarily()
    }

    fun seekBackward() {
        if (exoPlayer.isCurrentMediaItemSeekable) {
            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) // -10s
        }
        showControlsTemporarily()
    }

    fun setVolume(volume: Float) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(volume = exoPlayer.volume) }
    }

    @OptIn(UnstableApi::class)
    fun setResolution(resolution: VideoResolution?) {
        val parametersBuilder = exoPlayer.trackSelectionParameters.buildUpon()
        if (resolution == null) {
            // Auto
            parametersBuilder.setMaxVideoSizeSd()
            parametersBuilder.clearVideoSizeConstraints()
        } else {
            parametersBuilder.setMaxVideoSize(resolution.width, resolution.height)
        }
        exoPlayer.trackSelectionParameters = parametersBuilder.build()
        _uiState.update { it.copy(selectedResolution = resolution) }
    }

    @OptIn(UnstableApi::class)
    private fun updateResolutions() {
        val tracks = exoPlayer.currentTracks
        val videoResolutions = mutableListOf<VideoResolution>()
        
        tracks.groups.forEach { group ->
            if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    if (format.width > 0 && format.height > 0) {
                        videoResolutions.add(
                            VideoResolution(
                                id = "${format.width}x${format.height}",
                                label = "${format.height}p",
                                width = format.width,
                                height = format.height,
                                bitrate = format.bitrate
                            )
                        )
                    }
                }
            }
        }
        
        _uiState.update { it.copy(
            availableResolutions = videoResolutions.distinctBy { res -> res.id }.sortedByDescending { res -> res.height }
        ) }
    }

    fun retry() {
        currentItem?.let { prepare(it) }
    }

    fun showControlsTemporarily() {
        _uiState.update { it.copy(showControls = true) }
        scheduleHideControls()
    }

    private fun scheduleHideControls() {
        hideControlsJob?.cancel()
        hideControlsJob = viewModelScope.launch {
            delay(4.seconds) // hide controls after 4s of inactivity
            _uiState.update { it.copy(showControls = false) }
        }
    }

    override fun onCleared() {
        hideControlsJob?.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
        super.onCleared()
    }
}
