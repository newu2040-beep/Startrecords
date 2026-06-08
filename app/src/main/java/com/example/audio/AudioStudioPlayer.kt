package com.example.audio

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class PlaybackState {
    IDLE, PLAYING, PAUSED
}

class AudioStudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var playerThread: Thread? = null

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath

    private var currentSpeed = 1.0f

    fun playFile(filePath: String, speed: Float = 1.0f) {
        stopPlayback()
        currentSpeed = speed

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioStudioPlayer", "Audio file does not exist: $filePath")
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                _totalDurationMs.value = duration.toLong()
                
                setPlaybackSpeed(speed)
                
                start()
                _currentFilePath.value = filePath
                _playbackState.value = PlaybackState.PLAYING
            } catch (e: Exception) {
                Log.e("AudioStudioPlayer", "Failed to start audio playback", e)
                return
            }
        }

        mediaPlayer?.setOnCompletionListener {
            stopPlayback()
        }

        playerThread = Thread {
            try {
                while (_playbackState.value == PlaybackState.PLAYING || _playbackState.value == PlaybackState.PAUSED) {
                    if (_playbackState.value == PlaybackState.PLAYING) {
                        _currentPositionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    }
                    Thread.sleep(100)
                }
            } catch (e: InterruptedException) {
                // Thread naturally exits
            }
        }
        playerThread?.start()
    }

    fun pausePlayback() {
        if (_playbackState.value != PlaybackState.PLAYING) return
        mediaPlayer?.pause()
        _playbackState.value = PlaybackState.PAUSED
    }

    fun resumePlayback() {
        if (_playbackState.value != PlaybackState.PAUSED) return
        mediaPlayer?.start()
        _playbackState.value = PlaybackState.PLAYING
    }

    fun stopPlayback() {
        _playbackState.value = PlaybackState.IDLE
        _currentPositionMs.value = 0L
        _totalDurationMs.value = 0L
        _currentFilePath.value = null

        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // Graceful non-crash
        } finally {
            mediaPlayer?.release()
            mediaPlayer = null
        }

        playerThread?.interrupt()
        playerThread = null
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun skipForward(seconds: Int = 10) {
        mediaPlayer?.let {
            val target = (it.currentPosition + seconds * 1000).coerceAtMost(it.duration)
            seekTo(target.toLong())
        }
    }

    fun skipBackward(seconds: Int = 10) {
        mediaPlayer?.let {
            val target = (it.currentPosition - seconds * 1000).coerceAtLeast(0)
            seekTo(target.toLong())
        }
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed
        setPlaybackSpeed(speed)
    }

    private fun setPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { mp ->
                try {
                    val params = PlaybackParams().apply { this.speed = speed }
                    mp.playbackParams = params
                } catch (e: Exception) {
                    Log.e("AudioStudioPlayer", "Failed to set playback speed", e)
                }
            }
        }
    }
}
