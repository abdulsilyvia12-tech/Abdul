package com.example.ui.components.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.audio.BlinkMusicPlayer
import com.example.data.audio.MusicTrackInfo
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkCyanLight
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary
import kotlinx.coroutines.delay

@Composable
fun CameraMusicDialog(
    visible: Boolean,
    selectedTrack: String,
    onSelectTrack: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenreFilter by remember { mutableStateOf("All") }
    val isPlaying by BlinkMusicPlayer.isPlaying.collectAsState()
    val activeTrack by BlinkMusicPlayer.currentTrack.collectAsState()

    var onlineTracks by remember { mutableStateOf<List<MusicTrackInfo>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }

    val genreTabs = listOf("All", "Phonk & Drift", "Pop Hits", "Ballads & Acoustic", "R&B & Soul", "Rock & Indie", "Hip-Hop & Rap")

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            isSearchingOnline = true
            delay(400)
            val results = BlinkMusicPlayer.searchOnlineItunes(q)
            onlineTracks = results
            isSearchingOnline = false
        } else {
            onlineTracks = emptyList()
            isSearchingOnline = false
        }
    }

    val combinedTracks = remember(selectedGenreFilter, searchQuery, onlineTracks) {
        val q = searchQuery.trim().lowercase()
        val localFiltered = if (selectedGenreFilter == "All") {
            BlinkMusicPlayer.availableTracks
        } else {
            BlinkMusicPlayer.availableTracks.filter { it.genre.contains(selectedGenreFilter, ignoreCase = true) }
        }

        val matchingLocal = if (q.isBlank()) localFiltered else {
            localFiltered.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.genre.lowercase().contains(q) ||
                it.description.lowercase().contains(q)
            }
        }

        val matchingOnline = if (selectedGenreFilter == "All") {
            onlineTracks
        } else {
            onlineTracks.filter { it.genre.contains(selectedGenreFilter, ignoreCase = true) }
        }

        val result = mutableListOf<MusicTrackInfo>()
        val seen = mutableSetOf<String>()
        matchingOnline.forEach {
            if (seen.add(it.title.lowercase())) result.add(it)
        }
        matchingLocal.forEach {
            if (seen.add(it.title.lowercase())) result.add(it)
        }
        result
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, BlinkCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("camera_music_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🎵", fontSize = 18.sp)
                            Text("Music & Sound Library", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("Search any singer, phonk, or artist (Kordhell, DVRST, Taylor, Adele...)", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Search Bar Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, phonk, artists (e.g. Kordhell, DVRST, Taylor)...", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = BlinkCyanLight, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (isSearchingOnline) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BlinkCyanLight, strokeWidth = 2.dp)
                        } else if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_music_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BlinkCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    )
                )

                // Quick Custom Track Pill if user typed something
                if (searchQuery.trim().length >= 2) {
                    Surface(
                        onClick = {
                            val custom = searchQuery.trim()
                            onSelectTrack(custom)
                            BlinkMusicPlayer.playTrack(custom)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = BlinkCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BlinkCyanLight.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "✨ Use \"${searchQuery.trim()}\"",
                                color = BlinkCyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "+ Select",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Genre Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreTabs) { genre ->
                        val isSelected = selectedGenreFilter == genre
                        Surface(
                            onClick = { selectedGenreFilter = genre },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) BlinkPrimary else Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, if (isSelected) BlinkCyan else Color.Transparent)
                        ) {
                            Text(
                                text = when (genre) {
                                    "Pop Hits" -> "🎤 Pop Hits"
                                    "Ballads & Acoustic" -> "🎹 Ballads"
                                    "R&B & Soul" -> "💖 R&B"
                                    "Rock & Indie" -> "🎸 Rock"
                                    "Hip-Hop & Rap" -> "🎧 Hip-Hop"
                                    else -> "🌟 All Hits"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Track List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(combinedTracks, key = { it.title }) { track ->
                        val isCurrentSelected = selectedTrack == track.title
                        val isCurrentlyAuditioning = isPlaying && activeTrack == track.title

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrentSelected) BlinkPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, if (isCurrentSelected) BlinkCyan else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Play Preview Button
                                IconButton(
                                    onClick = {
                                        if (isCurrentlyAuditioning) {
                                            BlinkMusicPlayer.pausePlayback()
                                        } else {
                                            BlinkMusicPlayer.playTrack(track.title, track.previewUrl, track.artworkUrl)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentlyAuditioning) BlinkPink else BlinkCyan.copy(alpha = 0.2f))
                                        .testTag("play_music_preview_${track.title.take(10)}")
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentlyAuditioning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Artwork / Icon
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.genre}",
                                        color = BlinkCyanLight,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = track.description,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        onSelectTrack(track.title)
                                        BlinkMusicPlayer.playTrack(track.title, track.previewUrl, track.artworkUrl)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrentSelected) BlinkPink else BlinkPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("select_music_track_${track.title.take(10)}")
                                ) {
                                    Text(
                                        text = if (isCurrentSelected) "Selected" else "Use Song",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
