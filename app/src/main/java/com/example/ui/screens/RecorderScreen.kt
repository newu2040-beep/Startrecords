package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RecordingState
import com.example.ui.viewmodel.AudioViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    viewModel: AudioViewModel,
    onNavigateBack: () -> Unit
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val amplitude by viewModel.currentAmplitude.collectAsState()
    val elapsedTime by viewModel.elapsedTimeMs.collectAsState()

    // Config Values
    val title by viewModel.activeTitle.collectAsState()
    val format by viewModel.activeFormat.collectAsState()
    val quality by viewModel.activeQuality.collectAsState()
    val noiseReduction by viewModel.noiseReduction.collectAsState()
    val audioEnhancement by viewModel.audioEnhancement.collectAsState()
    val category by viewModel.activeCategory.collectAsState()
    val isCall by viewModel.isCallRecording.collectAsState()
    val contactName by viewModel.recordingContactName.collectAsState()
    val activeBookmarks by viewModel.activeRecordingBookmarks.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var notesInput by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("RECORDING STUDIO", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (recordingState != RecordingState.INACTIVE) {
                            viewModel.cancelRecording()
                        }
                        onNavigateBack()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recordingState != RecordingState.INACTIVE) {
                        IconButton(onClick = {
                            viewModel.cancelRecording()
                            onNavigateBack()
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Cancel Tape", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dynamic Oscillating Waveform Visualization
            item {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        OscillatingWaveform(
                            amplitude = amplitude,
                            isRecording = recordingState == RecordingState.RECORDING,
                            modifier = Modifier.fillMaxSize(),
                            neonColor = MaterialTheme.colorScheme.primary
                        )
                        
                        // Small state overlays
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (recordingState == RecordingState.RECORDING) Color.Red.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (recordingState == RecordingState.RECORDING) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = recordingState.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recordingState == RecordingState.RECORDING) Color.Red else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Big Timer and Bookmarks count
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.getDurationFormatted(elapsedTime),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    if (activeBookmarks.isNotEmpty()) {
                        Text(
                            text = "📌 Bookmarks added: ${activeBookmarks.size}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Primary control buttons row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bookmark Action Button
                    IconButton(
                        onClick = {
                            if (recordingState == RecordingState.RECORDING) {
                                viewModel.addActiveRecordingBookmark()
                            }
                        },
                        enabled = recordingState == RecordingState.RECORDING,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (recordingState == RecordingState.RECORDING) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Bookmark Marker",
                            tint = if (recordingState == RecordingState.RECORDING) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // MAIN Glow Record Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                            .clickable {
                                when (recordingState) {
                                    RecordingState.INACTIVE -> viewModel.startRecording()
                                    RecordingState.RECORDING -> viewModel.pauseRecording()
                                    RecordingState.PAUSED -> viewModel.resumeRecording()
                                }
                            }
                            .testTag("action_record_button")
                    ) {
                        val iconToUse = when (recordingState) {
                            RecordingState.INACTIVE -> Icons.Default.FiberManualRecord
                            RecordingState.RECORDING -> Icons.Default.Pause
                            RecordingState.PAUSED -> Icons.Default.PlayArrow
                        }
                        val iconColor = when (recordingState) {
                            RecordingState.INACTIVE -> Color.Red
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Icon(
                            imageVector = iconToUse,
                            contentDescription = "Control icon",
                            tint = iconColor,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Stop / Save button
                    IconButton(
                        onClick = {
                            if (recordingState != RecordingState.INACTIVE) {
                                notesInput = ""
                                tagsInput = ""
                                showSaveDialog = true
                            }
                        },
                        enabled = recordingState != RecordingState.INACTIVE,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (recordingState != RecordingState.INACTIVE) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                            )
                            .testTag("action_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Active Recording",
                            tint = if (recordingState != RecordingState.INACTIVE) Color.Red else Color.Gray
                        )
                    }
                }
            }

            // Recorder Configuration Inputs
            item {
                Text(
                    text = "RECORDING DETAILS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // Text Input for Title
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.setTitle(it) },
                    label = { Text("Recording Title") },
                    placeholder = { Text("e.g., Marketing Strategy Sync") },
                    modifier = Modifier.fillMaxWidth().testTag("title_textfield"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    maxLines = 1,
                    enabled = recordingState == RecordingState.INACTIVE
                )
            }

            // Call Mode & Contact Setup (Automatic detection emulation)
            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Call Recording Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Emulates Android call capture detection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isCall,
                            onCheckedChange = { viewModel.setContactInfo(it, if (it) "John Doe" else null) },
                            enabled = recordingState == RecordingState.INACTIVE
                        )
                    }

                    AnimatedVisibility(visible = isCall) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = contactName ?: "",
                                onValueChange = { viewModel.setContactInfo(isCall = true, contactName = it) },
                                label = { Text("Contact Name") },
                                placeholder = { Text("Recipient Name, or 'Unknown'") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Audio Categories Flow
            item {
                Text(
                    text = "Category Channel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("General", "Meeting", "Lecture", "Interview", "Voice Note", "Personal", "Call")
                    categories.forEach { cat ->
                        val isSelected = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = recordingState == RecordingState.INACTIVE) {
                                    viewModel.setCategory(cat)
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quality & Formats Choices
            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Digital Format & Bitrate", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Standard Format", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("M4A", "WAV", "AAC", "MP3").forEach { f ->
                                val selected = f == format
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                        .clickable(enabled = recordingState == RecordingState.INACTIVE) { viewModel.setFormat(f) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(f, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recording Quality", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("High", "Medium", "Low").forEach { q ->
                                val selected = q == quality
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                                        .clickable(enabled = recordingState == RecordingState.INACTIVE) { viewModel.setQuality(q) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(q, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // DSP Audio Processors Settings
            item {
                GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Audio Studio Processors (DSP)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Noise Reduction Options", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Filters high-freq static background hum", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = noiseReduction,
                            onCheckedChange = { viewModel.setNoiseReduction(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Audio Enhancement Tools", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Multi-band gain compressor booster", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = audioEnhancement,
                            onCheckedChange = { viewModel.setAudioEnhancement(it) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Save File Custom Dialog Panel
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Finalize Studio Record", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The master track has been captured successfully. You can attach structural notes or organizational tags before locking it down.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Recording Session Notes") },
                        placeholder = { Text("Add key summaries or reminders...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        label = { Text("Structural Tags") },
                        placeholder = { Text("e.g. Work,Important,Pitch") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.stopRecording(notesInput, tagsInput)
                        showSaveDialog = false
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("dialog_save_button")
                ) {
                    Text("Commence Auto-Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Back to Tape")
                }
            }
        )
    }
}
