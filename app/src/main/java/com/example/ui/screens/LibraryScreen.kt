package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.Recording
import com.example.ui.viewmodel.AudioViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: AudioViewModel,
    initialSearchQuery: String?,
    onNavigateBack: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val favsOnly by viewModel.filterFavoritesOnly.collectAsState()
    val archsOnly by viewModel.filterArchivedOnly.collectAsState()

    // Playback state bindings
    val playbackState by viewModel.playbackState.collectAsState()
    val playbackPosition by viewModel.playbackPositionMs.collectAsState()
    val playbackDuration by viewModel.playbackTotalDurationMs.collectAsState()
    val currentPlayingFilePath by viewModel.currentPlayingFilePath.collectAsState()

    var selectedRecordingForPlaying by remember { mutableStateOf<Recording?>(null) }
    var showEditDetailsDialog by remember { mutableStateOf<Recording?>(null) }
    var showRenameDialog by remember { mutableStateOf<Recording?>(null) }
    
    // Playback control state variables
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }

    // Synchronize initial query if passed e.g. from Dashboard
    LaunchedEffect(initialSearchQuery) {
        if (initialSearchQuery != null) {
            viewModel.setSearchQuery(initialSearchQuery)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RECORDING LIBRARY", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // List / Grid toggle
                    IconButton(onClick = { viewModel.setGridMode(!gridMode) }) {
                        Icon(
                            imageVector = if (gridMode) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View Mode"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instant Search bar + quick clear
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                label = { Text("Search records, notes, tags or calls...") },
                modifier = Modifier.fillMaxWidth().testTag("library_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ),
                maxLines = 1
            )

            // Filtering options: favorites only, archived only, sorting picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Favorites Pill Toggle
                    FilterQuickPill(
                        selected = favsOnly,
                        label = "Favorites",
                        icon = Icons.Default.Favorite,
                        onClick = { viewModel.toggleFavoritesFilter() }
                    )

                    // Archives Pill Toggle
                    FilterQuickPill(
                        selected = archsOnly,
                        label = "Archived",
                        icon = Icons.Default.Archive,
                        onClick = { viewModel.toggleArchivedFilter() }
                    )
                }

                // Sorting dropdown trigger
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val nextSort = when (sortBy) {
                                "date" -> "duration"
                                "duration" -> "size"
                                else -> "date"
                            }
                            viewModel.setSortBy(nextSort)
                        }
                        .padding(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sortBy.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Horizontally scrolling list of category channels
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val allSelected = categoryFilter == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                            .clickable { viewModel.setCategoryFilter(null) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("All Categories", fontSize = 11.sp, color = if (allSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }

                val listCats = listOf("General", "Meeting", "Lecture", "Interview", "Voice Note", "Personal", "Call")
                items(listCats.size) { index ->
                    val cat = listCats[index]
                    val selected = cat == categoryFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                            .clickable { viewModel.setCategoryFilter(cat) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(cat, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Central grid/list layout representation
            Box(modifier = Modifier.weight(1f)) {
                if (recordings.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Empty Library", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No studio recordings matched your search.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                } else if (gridMode) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recordings) { rec ->
                            GridCardItem(
                                recording = rec,
                                isPlayingState = selectedRecordingForPlaying?.id == rec.id,
                                onClick = {
                                    selectedRecordingForPlaying = rec
                                    viewModel.playRecording(rec, currentSpeed)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(rec) },
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recordings) { rec ->
                            ListCardItem(
                                recording = rec,
                                isPlayingState = selectedRecordingForPlaying?.id == rec.id,
                                onClick = {
                                    selectedRecordingForPlaying = rec
                                    viewModel.playRecording(rec, currentSpeed)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(rec) },
                                onMenuClick = { showEditDetailsDialog = rec },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Active Interactive Bottom Playback Deck
            AnimatedVisibility(
                visible = selectedRecordingForPlaying != null,
                enter = slideInVertically(animationSpec = tween(300)) { it } + fadeIn(),
                exit = slideOutVertically(animationSpec = tween(300)) { it } + fadeOut()
            ) {
                selectedRecordingForPlaying?.let { rec ->
                    val isPlayingNow = playbackState == PlaybackState.PLAYING
                    
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("active_playback_deck")
                    ) {
                        Column {
                            // Player Title metadata and close button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (rec.isCall) Icons.Default.Call else Icons.Default.VolumeUp,
                                    contentDescription = "Deck play icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Speed: ${currentSpeed}x • Format: ${rec.audioFormat}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                
                                // Rename Trigger
                                IconButton(onClick = { showRenameDialog = rec }) {
                                    Icon(imageVector = Icons.Default.EditNote, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                                }

                                // Terminate Player Deck Close Button
                                IconButton(onClick = {
                                    viewModel.stopPlayback()
                                    selectedRecordingForPlaying = null
                                }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close player")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Playback Progress seek and times
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.getDurationFormatted(playbackPosition),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Slider(
                                    value = if (playbackDuration > 0) playbackPosition.toFloat() / playbackDuration.toFloat() else 0f,
                                    onValueChange = { seekPercent ->
                                        viewModel.seekPlayback((seekPercent * playbackDuration).toLong())
                                    },
                                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).height(24.dp)
                                )
                                Text(
                                    text = viewModel.getDurationFormatted(playbackDuration),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Playing Controller Rows: speed, skip backwards, play/pause, skip forwards, sharing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play Speed Toggle Button
                                TextButton(onClick = {
                                    currentSpeed = when (currentSpeed) {
                                        1.0f -> 1.5f
                                        1.5f -> 2.0f
                                        2.0f -> 0.5f
                                        else -> 1.0f
                                    }
                                    viewModel.setPlaybackSpeed(currentSpeed)
                                }) {
                                    Text("${currentSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }

                                // Skip Backward 10s
                                IconButton(onClick = { viewModel.skipPlaybackBackward() }) {
                                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "Skip back")
                                }

                                // Play / Pause core toggle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = {
                                        if (isPlayingNow) {
                                            viewModel.pausePlayback()
                                        } else {
                                            viewModel.resumePlayback()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isPlayingNow) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "play-pause-deck",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                // Skip Forward 10s
                                IconButton(onClick = { viewModel.skipPlaybackForward() }) {
                                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "Skip forward")
                                }

                                // Native Audio Sharing Control
                                val context = LocalContext.current
                                IconButton(onClick = {
                                    try {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "StartRecord Track: ${rec.title}\nFormat: ${rec.audioFormat}\nNotes: ${rec.notes}")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export metadata to standard tools"))
                                    } catch (e: Exception) {
                                        // Safe sandbox sharing trigger
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet representation to edit notes, tags or toggle Archive/Delete
    if (showEditDetailsDialog != null) {
        val rec = showEditDetailsDialog!!
        var notesVal by remember { mutableStateOf(rec.notes ?: "") }
        var tagsVal by remember { mutableStateOf(rec.tags) }

        AlertDialog(
            onDismissRequest = { showEditDetailsDialog = null },
            title = { Text("Track Administration Panel", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notesVal,
                        onValueChange = { notesVal = it },
                        label = { Text("Session Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tagsVal,
                        onValueChange = { tagsVal = it },
                        label = { Text("Structural Tags (comma split)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick toggles in the editor modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.archiveRecording(rec)
                                showEditDetailsDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (rec.isArchived) "Unarchive" else "Archive Column")
                        }

                        Button(
                            onClick = {
                                viewModel.deleteRecording(rec)
                                if (selectedRecordingForPlaying?.id == rec.id) {
                                    viewModel.stopPlayback()
                                    selectedRecordingForPlaying = null
                                }
                                showEditDetailsDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete Track")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateNotesAndTags(rec, notesVal, tagsVal)
                    showEditDetailsDialog = null
                }) {
                    Text("Decompress Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDetailsDialog = null }) { Text("Dismiss") }
            }
        )
    }

    // Secondary rename dialog
    if (showRenameDialog != null) {
        val rec = showRenameDialog!!
        var renameInput by remember { mutableStateOf(rec.title) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Studio Master") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Asset Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameRecording(rec, renameInput)
                    selectedRecordingForPlaying = rec.copy(title = renameInput)
                    showRenameDialog = null
                }) {
                    Text("Rename Track")
                }
            }
        )
    }
}

@Composable
fun FilterQuickPill(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ListCardItem(
    recording: Recording,
    isPlayingState: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onMenuClick: () -> Unit,
    viewModel: AudioViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isPlayingState) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingState) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Status Visual Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPlayingState) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlayingState) Icons.Default.GraphicEq
                    else if (recording.isCall) Icons.Default.Call
                    else Icons.Default.Mic,
                    contentDescription = "Left icon indication",
                    tint = if (isPlayingState) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Titles Metadata Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${recording.category} • ${viewModel.getDurationFormatted(recording.durationMs)} • ${viewModel.getStorageSizeFormatted(recording.sizeBytes)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                if (recording.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recording.tags.split(",").take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Actions Right Pane
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (recording.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Item admin menu",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GridCardItem(
    recording: Recording,
    isPlayingState: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    viewModel: AudioViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(
                width = 1.dp,
                color = if (isPlayingState) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayingState) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPlayingState) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Default.GraphicEq
                        else if (recording.isCall) Icons.Default.Call
                        else Icons.Default.Mic,
                        contentDescription = "visual status",
                        tint = if (isPlayingState) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (recording.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if (recording.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = recording.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${recording.category} • ${viewModel.getDurationFormatted(recording.durationMs)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Text(
                    text = viewModel.getStorageSizeFormatted(recording.sizeBytes),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
