package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStudioManager
import com.example.audio.AudioStudioPlayer
import com.example.audio.PlaybackState
import com.example.audio.RecordingState
import com.example.data.Bookmark
import com.example.data.Recording
import com.example.data.RecordingRepository
import com.example.data.StartRecordDatabase
import com.example.ui.theme.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StartRecordDatabase.getDatabase(application)
    private val repository = RecordingRepository(database.recordingDao(), database.bookmarkDao())

    private val recorderManager = AudioStudioManager(application)
    private val playerManager = AudioStudioPlayer()

    // Main Theme State
    private val _currentTheme = MutableStateFlow(AppTheme.MIDNIGHT_BLUE)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    // Recording States
    val recordingState: StateFlow<RecordingState> = recorderManager.recordingState
    val currentAmplitude: StateFlow<Float> = recorderManager.amplitudeFlow
    val elapsedTimeMs: StateFlow<Long> = recorderManager.elapsedTimeMs

    // Playback States
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val playbackPositionMs: StateFlow<Long> = playerManager.currentPositionMs
    val playbackTotalDurationMs: StateFlow<Long> = playerManager.totalDurationMs
    val currentPlayingFilePath: StateFlow<String?> = playerManager.currentFilePath

    // Active Recording Context
    private val _activeFormat = MutableStateFlow("M4A")
    val activeFormat: StateFlow<String> = _activeFormat.asStateFlow()

    private val _activeQuality = MutableStateFlow("High")
    val activeQuality: StateFlow<String> = _activeQuality.asStateFlow()

    private val _noiseReduction = MutableStateFlow(false)
    val noiseReduction: StateFlow<Boolean> = _noiseReduction.asStateFlow()

    private val _audioEnhancement = MutableStateFlow(false)
    val audioEnhancement: StateFlow<Boolean> = _audioEnhancement.asStateFlow()

    private val _activeCategory = MutableStateFlow("General")
    val activeCategory: StateFlow<String> = _activeCategory.asStateFlow()

    private val _activeTitle = MutableStateFlow("")
    val activeTitle: StateFlow<String> = _activeTitle.asStateFlow()

    private val _recordingContactName = MutableStateFlow<String?>(null)
    val recordingContactName: StateFlow<String?> = _recordingContactName.asStateFlow()

    private val _isCallRecording = MutableStateFlow(false)
    val isCallRecording: StateFlow<Boolean> = _isCallRecording.asStateFlow()

    // Temporary storage of recorded file prior to save dialog
    private var lastRecordedFile: File? = null

    // Library Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _sortBy = MutableStateFlow("date") // date, duration, size
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _gridMode = MutableStateFlow(false)
    val gridMode: StateFlow<Boolean> = _gridMode.asStateFlow()

    private val _filterFavoritesOnly = MutableStateFlow(false)
    val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly.asStateFlow()

    private val _filterArchivedOnly = MutableStateFlow(false)
    val filterArchivedOnly: StateFlow<Boolean> = _filterArchivedOnly.asStateFlow()

    // App Lock States
    private val _appLocked = MutableStateFlow(false)
    val appLocked: StateFlow<Boolean> = _appLocked.asStateFlow()

    private val _biometricsEnabled = MutableStateFlow(false)
    val biometricsEnabled: StateFlow<Boolean> = _biometricsEnabled.asStateFlow()

    private val _masterPin = MutableStateFlow("1234")
    val masterPin: StateFlow<String> = _masterPin.asStateFlow()

    // Temporary active recording bookmarks
    private val _activeRecordingBookmarks = MutableStateFlow<List<Long>>(emptyList())
    val activeRecordingBookmarks: StateFlow<List<Long>> = _activeRecordingBookmarks.asStateFlow()

    data class FilterState(
        val query: String,
        val categoryFilter: String?,
        val sort: String,
        val favsOnly: Boolean,
        val archOnly: Boolean
    )

    private val filterStateFlow: Flow<FilterState> = combine(
        _searchQuery,
        _selectedCategoryFilter,
        _sortBy,
        _filterFavoritesOnly,
        _filterArchivedOnly
    ) { query, categoryFilter, sort, favsOnly, archOnly ->
        FilterState(query, categoryFilter, sort, favsOnly, archOnly)
    }

    // Unified List of Recordings
    val recordings: StateFlow<List<Recording>> = combine(
        repository.allRecordings,
        repository.favoriteRecordings,
        repository.archivedRecordings,
        filterStateFlow
    ) { all, favs, archived, filters ->
        var list = when {
            filters.archOnly -> archived
            filters.favsOnly -> favs
            else -> all
        }

        // Apply Search
        if (filters.query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(filters.query, ignoreCase = true) ||
                        (it.notes?.contains(filters.query, ignoreCase = true) ?: false) ||
                        it.tags.contains(filters.query, ignoreCase = true) ||
                        (it.contactName?.contains(filters.query, ignoreCase = true) ?: false)
            }
        }

        // Apply Category Filter
        if (filters.categoryFilter != null) {
            list = list.filter { it.category == filters.categoryFilter }
        }

        // Apply Sorting
        when (filters.sort) {
            "duration" -> list.sortedByDescending { it.durationMs }
            "size" -> list.sortedByDescending { it.sizeBytes }
            else -> list.sortedByDescending { it.timestamp } // Date by default
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Pre-seed some default elements to make app beautiful right on first launch
        viewModelScope.launch {
            repository.allRecordings.firstOrNull()?.let { existing ->
                if (existing.isEmpty()) {
                    seedDefaultRecordings()
                }
            }
        }
    }

    private suspend fun seedDefaultRecordings() {
        val seedItems = listOf(
            Recording(
                title = "Board Pitch Meeting",
                filePath = "/seed/board_pitch.m4a",
                durationMs = 1245000L, // ~20 mins
                sizeBytes = 24500000L,
                category = "Meeting",
                tags = "Important,Pitch,Workspace",
                notes = "Discussion regarding Q3 metrics & scaling options. Team raised concern with Server budget.",
                isFavorite = true
            ),
            Recording(
                title = "Dev Sprint Sync",
                filePath = "/seed/sprint_sync.m4a",
                durationMs = 600000L, // 10 mins
                sizeBytes = 11200000L,
                category = "Meeting",
                tags = "Sprint,Tech",
                notes = "Agreed to proceed with jetpack compose and modern Room database pre-seed structures."
            ),
            Recording(
                title = "Interview - Senior Android Lead",
                filePath = "/seed/interview.m4a",
                durationMs = 2715000L, // ~45 mins
                sizeBytes = 55400000L,
                category = "Interview",
                tags = "Hiring,HR,Tech",
                notes = "Candidate Rahul Shah demonstrated exceptional architecture and premium interface design values."
            ),
            Recording(
                title = "Advanced Mobile Systems Lecture",
                filePath = "/seed/lecture_complex.m4a",
                durationMs = 3580000L, // ~60 mins
                sizeBytes = 72100000L,
                category = "Lecture",
                tags = "University,DSP",
                notes = "Explaining Fourier transformations on digital signals, noise suppression, and gain control vectors."
            ),
            Recording(
                title = "Follow-up Call with Client",
                filePath = "/seed/client_call.m4a",
                durationMs = 325000L, // ~5 mins
                sizeBytes = 6300000L,
                category = "Call",
                isCall = true,
                contactName = "Alex Mercer",
                tags = "Project-Delta,Call-Log",
                notes = "Secure recording of conversation agreeing to specifications."
            ),
            Recording(
                title = "Sunset Melody Concept Voice Note",
                filePath = "/seed/voice_note.m4a",
                durationMs = 45000L, // 45s
                sizeBytes = 9450000L,
                category = "Voice Note",
                tags = "Creative,Acoustic",
                notes = "Quick acoustic guitar progression in E major."
            )
        )

        for (item in seedItems) {
            val id = repository.insertRecording(item).toInt()
            // Seed a booklet bookmark
            if (item.title == "Board Pitch Meeting") {
                repository.insertBookmark(Bookmark(recordingId = id, label = "Q3 Objectives", timestampMs = 120000L))
                repository.insertBookmark(Bookmark(recordingId = id, label = "Server Cost Debate", timestampMs = 540000L))
            }
        }
    }

    // Config Setters
    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }

    fun setFormat(format: String) {
        _activeFormat.value = format
    }

    fun setQuality(quality: String) {
        _activeQuality.value = quality
    }

    fun setNoiseReduction(enabled: Boolean) {
        _noiseReduction.value = enabled
    }

    fun setAudioEnhancement(enabled: Boolean) {
        _audioEnhancement.value = enabled
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    fun setTitle(title: String) {
        _activeTitle.value = title
    }

    fun setContactInfo(isCall: Boolean, contactName: String?) {
        _isCallRecording.value = isCall
        _recordingContactName.value = contactName
    }

    // Recording Operations
    fun startRecording() {
        _activeRecordingBookmarks.value = emptyList()
        val finalTitle = _activeTitle.value.ifBlank { "Recording_${System.currentTimeMillis()}" }
        val file = recorderManager.startRecording(
            format = _activeFormat.value,
            quality = _activeQuality.value,
            noiseReduction = _noiseReduction.value,
            audioEnhancement = _audioEnhancement.value
        )
        lastRecordedFile = file
    }

    fun pauseRecording() {
        recorderManager.pauseRecording()
    }

    fun resumeRecording() {
        recorderManager.resumeRecording()
    }

    fun addActiveRecordingBookmark(label: String = "Marker") {
        val currentStamp = elapsedTimeMs.value
        _activeRecordingBookmarks.value = _activeRecordingBookmarks.value + currentStamp
    }

    fun stopRecording(customNotes: String = "", customTags: String = "") {
        val duration = recorderManager.stopRecording()
        val file = lastRecordedFile

        if (file != null && file.exists() && duration > 500) {
            viewModelScope.launch {
                val finalTitle = _activeTitle.value.ifBlank { "Recording_${System.currentTimeMillis()}" }
                val newRecording = Recording(
                    title = finalTitle,
                    filePath = file.absolutePath,
                    durationMs = duration,
                    sizeBytes = file.length(),
                    category = _activeCategory.value,
                    notes = customNotes.ifBlank { "Saved via start-record studio panel." },
                    tags = customTags,
                    isCall = _isCallRecording.value,
                    contactName = _recordingContactName.value,
                    audioFormat = _activeFormat.value,
                    quality = _activeQuality.value,
                    noiseReduction = _noiseReduction.value,
                    audioEnhancement = _audioEnhancement.value
                )
                val idInstalled = repository.insertRecording(newRecording).toInt()

                // Save bookmarks if there were any during active tape
                _activeRecordingBookmarks.value.forEachIndexed { idx, stamp ->
                    repository.insertBookmark(
                        Bookmark(
                            recordingId = idInstalled,
                            label = "Marker ${idx + 1}",
                            timestampMs = stamp
                        )
                    )
                }
                
                // Clear state
                _activeTitle.value = ""
                _recordingContactName.value = null
                _isCallRecording.value = false
                lastRecordedFile = null
            }
        } else {
            Log.w("AudioViewModel", "Recording stopped but file was either missing or duration too short")
        }
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        _activeTitle.value = ""
        lastRecordedFile = null
    }

    // Playback Operations
    fun playRecording(recording: Recording, speed: Float = 1.0f) {
        // Safe check for mock file playback so the application does not lock
        if (recording.filePath.startsWith("/seed/")) {
            // Emulate playback instantly since seed file is symbolic
            viewModelScope.launch {
                playerManager.stopPlayback()
                // Directly mock playing on standard managers
                val appFilesDir = getApplication<Application>().filesDir
                val tempFile = File(appFilesDir, "dummy_play.mp3")
                if (!tempFile.exists()) {
                    tempFile.writeText("dummy content for audio tracking")
                }
                playerManager.playFile(tempFile.absolutePath, speed)
            }
        } else {
            playerManager.playFile(recording.filePath, speed)
        }
    }

    fun pausePlayback() {
        playerManager.pausePlayback()
    }

    fun resumePlayback() {
        playerManager.resumePlayback()
    }

    fun stopPlayback() {
        playerManager.stopPlayback()
    }

    fun seekPlayback(progressMs: Long) {
        playerManager.seekTo(progressMs)
    }

    fun skipPlaybackForward() {
        playerManager.skipForward(10)
    }

    fun skipPlaybackBackward() {
        playerManager.skipBackward(10)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setSpeed(speed)
    }

    // Database Actions
    fun toggleFavorite(recording: Recording) {
        viewModelScope.launch {
            repository.updateRecording(recording.copy(isFavorite = !recording.isFavorite))
        }
    }

    fun archiveRecording(recording: Recording) {
        viewModelScope.launch {
            repository.updateRecording(recording.copy(isArchived = !recording.isArchived))
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
            try {
                val file = File(recording.filePath)
                if (file.exists() && !recording.filePath.startsWith("/seed/")) {
                    file.delete()
                }
            } catch (e: Exception) {
                // File-system safe
            }
        }
    }

    fun renameRecording(recording: Recording, newTitle: String) {
        viewModelScope.launch {
            repository.updateRecording(recording.copy(title = newTitle))
        }
    }

    fun updateNotesAndTags(recording: Recording, newNotes: String, newTags: String) {
        viewModelScope.launch {
            repository.updateRecording(recording.copy(notes = newNotes, tags = newTags))
        }
    }

    // Bookmark Actions
    fun getBookmarksForRecording(recordingId: Int): Flow<List<Bookmark>> {
        return repository.getBookmarksForRecording(recordingId)
    }

    fun addBookmarkAtCurrentPlayback(recordingId: Int, label: String) {
        viewModelScope.launch {
            repository.insertBookmark(
                Bookmark(
                    recordingId = recordingId,
                    label = label,
                    timestampMs = playbackPositionMs.value
                )
            )
        }
    }

    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
        }
    }

    // Filtering Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun setGridMode(grid: Boolean) {
        _gridMode.value = grid
    }

    fun toggleFavoritesFilter() {
        _filterFavoritesOnly.value = !_filterFavoritesOnly.value
    }

    fun toggleArchivedFilter() {
        _filterArchivedOnly.value = !_filterArchivedOnly.value
    }

    // App Lock Actions
    fun setAppLocked(locked: Boolean) {
        _appLocked.value = locked
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        _biometricsEnabled.value = enabled
    }

    fun updatePin(pin: String) {
        if (pin.length == 4) {
            _masterPin.value = pin
        }
    }

    // Analytics Generator
    fun getStorageSizeFormatted(bytes: Long): String {
        val kb = bytes / 1024f
        val mb = kb / 1024f
        return if (mb > 1) {
            String.format("%.1f MB", mb)
        } else {
            String.format("%.1f KB", kb)
        }
    }

    fun getDurationFormatted(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
