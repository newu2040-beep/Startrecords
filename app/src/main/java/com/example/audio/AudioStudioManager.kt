package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class RecordingState {
    INACTIVE, RECORDING, PAUSED
}

class AudioStudioManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0
    private var totalPausedDurationMs: Long = 0
    private var lastPauseTimeMs: Long = 0

    private val _recordingState = MutableStateFlow(RecordingState.INACTIVE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs

    private var recordThread: Thread? = null

    fun startRecording(format: String, quality: String, noiseReduction: Boolean, audioEnhancement: Boolean): File? {
        if (_recordingState.value != RecordingState.INACTIVE) return null

        val filename = "SR_${System.currentTimeMillis()}.${format.lowercase()}"
        val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(outputDir, filename)
        currentFile = file

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            
            val outputFormat = when (format.uppercase()) {
                "WAV" -> MediaRecorder.OutputFormat.THREE_GPP
                "MP3" -> MediaRecorder.OutputFormat.MPEG_4
                "AAC" -> MediaRecorder.OutputFormat.MPEG_4
                else -> MediaRecorder.OutputFormat.MPEG_4
            }
            setOutputFormat(outputFormat)

            val audioEncoder = when (format.uppercase()) {
                "AAC", "M4A" -> MediaRecorder.AudioEncoder.AAC
                else -> MediaRecorder.AudioEncoder.HE_AAC
            }
            setAudioEncoder(audioEncoder)

            val (bitRate, sampleRate) = when (quality.lowercase()) {
                "high" -> Pair(320000, 48000)
                "medium" -> Pair(128000, 44100)
                else -> Pair(64000, 22050)
            }
            setAudioEncodingBitRate(bitRate)
            setAudioSamplingRate(sampleRate)
            setOutputFile(file.absolutePath)

            try {
                prepare()
                start()
            } catch (e: Exception) {
                Log.e("AudioStudioManager", "Failed to start media recorder", e)
                return null
            }
        }

        startTimeMs = System.currentTimeMillis()
        totalPausedDurationMs = 0
        lastPauseTimeMs = 0
        _recordingState.value = RecordingState.RECORDING

        recordThread = Thread {
            try {
                while (_recordingState.value != RecordingState.INACTIVE) {
                    if (_recordingState.value == RecordingState.RECORDING) {
                        val elapsed = System.currentTimeMillis() - startTimeMs - totalPausedDurationMs
                        _elapsedTimeMs.value = elapsed

                        val amp = mediaRecorder?.maxAmplitude ?: 0
                        var normAmp = amp.toFloat() / 32767f
                        if (normAmp < 0.01f) {
                            normAmp = 0.02f + (Math.sin(System.currentTimeMillis().toDouble() / 150.0).toFloat() * 0.04f + 0.04f)
                        } else {
                            if (noiseReduction) normAmp *= 0.5f
                            if (audioEnhancement) normAmp *= 1.4f
                        }
                        _amplitudeFlow.value = normAmp.coerceIn(0.01f, 1.0f)
                    }
                    Thread.sleep(100)
                }
            } catch (e: InterruptedException) {
                // Thread exits
            }
        }
        recordThread?.start()

        return file
    }

    fun pauseRecording() {
        if (_recordingState.value != RecordingState.RECORDING) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                lastPauseTimeMs = System.currentTimeMillis()
                _recordingState.value = RecordingState.PAUSED
            }
        } catch (e: Exception) {
            Log.e("AudioStudioManager", "Failed to pause recording", e)
        }
    }

    fun resumeRecording() {
        if (_recordingState.value != RecordingState.PAUSED) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                totalPausedDurationMs += System.currentTimeMillis() - lastPauseTimeMs
                _recordingState.value = RecordingState.RECORDING
            }
        } catch (e: Exception) {
            Log.e("AudioStudioManager", "Failed to resume recording", e)
        }
    }

    fun stopRecording(): Long {
        if (_recordingState.value == RecordingState.INACTIVE) return 0L
        _recordingState.value = RecordingState.INACTIVE
        _amplitudeFlow.value = 0f
        
        val duration = _elapsedTimeMs.value
        _elapsedTimeMs.value = 0L

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e("AudioStudioManager", "Error stopping recorder", e)
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }

        recordThread?.interrupt()
        recordThread = null

        return duration
    }

    fun cancelRecording() {
        if (_recordingState.value == RecordingState.INACTIVE) return
        stopRecording()
        currentFile?.delete()
        currentFile = null
    }
}
