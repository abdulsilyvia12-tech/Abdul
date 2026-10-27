package com.example.ui.components.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.BlinkMusicPlayer
import com.example.data.audio.MusicTrackInfo
import com.example.ui.theme.*

/**
 * Top-mounted Song Search Bar for the Camera Tab.
 * Allows creators to search for any song (Bongo Flava, Dramatic, Comedy, EDM, Afrobeats, Lofi, etc.)
 * both BEFORE recording/capturing in the live camera viewfinder,
 * or AFTER recording in the post-production review studio.
 */
@Composable
fun CameraSongSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTrack: String,
    onSelectTrack: (String) -> Unit,
    onClearTrack: () -> Unit,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isPlaying by BlinkMusicPlayer.isPlaying.collectAsState()
    val activeTrack by BlinkMusicPlayer.currentTrack.collectAsState()

    var activeGenreFilter by remember { mutableStateOf<String?>(null) }

    // Filter tracks based on query and optional genre chip
    val searchResults = remember(searchQuery, activeGenreFilter) {
        val q = searchQuery.trim().lowercase()
        val all = BlinkMusicPlayer.availableTracks
        if (q.isBlank() && activeGenreFilter == null) {
            all
        } else {
            all.filter { track ->
                val matchesQuery = if (q.isBlank()) true else {
                    track.title.lowercase().contains(q) ||
                    track.artist.lowercase().contains(q) ||
                    track.genre.lowercase().contains(q) ||
                    track.description.lowercase().contains(q)
                }
                val matchesGenre = if (activeGenreFilter == null) true else {
                    track.genre.equals(activeGenreFilter, ignoreCase = true) ||
                    (activeGenreFilter == "Bongo" && (track.genre.contains("Bongo", true) || track.title.contains("Bongo", true))) ||
                    (activeGenreFilter == "Dramatic" && track.genre.contains("Dramatic", true)) ||
                    (activeGenreFilter == "Funny" && track.genre.contains("Funny", true)) ||
                    (activeGenreFilter == "EDM" && track.genre.contains("Electronic", true)) ||
                    (activeGenreFilter == "Lofi" && track.genre.contains("Lofi", true))
                }
                matchesQuery && matchesGenre
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("camera_song_search_container")
    ) {
        // --- Main Top Search Bar Surface ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(
                    BorderStroke(
                        width = if (isExpanded || searchQuery.isNotBlank()) 1.5.dp else 1.dp,
                        color = if (isExpanded) BlinkCyan else Color.White.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .testTag("camera_song_search_bar"),
            shape = RoundedCornerShape(22.dp),
            color = Color.Black.copy(alpha = 0.75f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Search / Music Icon
                Icon(
                    imageVector = if (selectedTrack.isNotBlank() && isPlaying) Icons.Filled.GraphicEq else Icons.Default.Search,
                    contentDescription = "Search Song",
                    tint = if (selectedTrack.isNotBlank()) BlinkCyanLight else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleExpanded(!isExpanded) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchQueryChange(it)
                        if (!isExpanded) onToggleExpanded(true)
                    },
                    placeholder = {
                        Text(
                            text = if (selectedTrack.isNotBlank()) "🎵 $selectedTrack" else "Search songs (Bongo, Dramatic, Funny, EDM)...",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("camera_song_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = BlinkCyanLight
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                    })
                )

                // Trailing Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                activeGenreFilter = null
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("camera_song_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Expand / Collapse Chevron Toggle
                    IconButton(
                        onClick = { onToggleExpanded(!isExpanded) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("camera_song_search_expand_toggle")
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse results" else "Expand songs",
                            tint = if (isExpanded) BlinkCyanLight else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Active Selected Song Pill Banner (When Not Searching) ---
        if (!isExpanded && selectedTrack.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleExpanded(true) }
                    .testTag("camera_active_song_pill"),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🎵", fontSize = 12.sp)
                    Text(
                        text = selectedTrack,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                    Text(
                        text = if (isPlaying) "• Playing ⚡" else "• Paused",
                        color = if (isPlaying) BlinkCyanLight else BlinkTextSecondary,
                        fontSize = 10.sp
                    )
                    IconButton(
                        onClick = onClearTrack,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Track",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // --- Expandable Song Search Results Dropdown Panel ---
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
                    .testTag("camera_song_search_dropdown"),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Quick Genre Pills Row
                    val genreChips = listOf("All", "Bongo", "Dramatic", "Funny", "EDM", "Lofi")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(genreChips) { chip ->
                            val isSel = (chip == "All" && activeGenreFilter == null) || (activeGenreFilter == chip)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) BlinkPrimary else Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (isSel) BlinkCyan else Color.Transparent),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        activeGenreFilter = if (chip == "All") null else chip
                                    }
                                    .testTag("genre_chip_${chip.lowercase()}")
                            ) {
                                Text(
                                    text = when (chip) {
                                        "Bongo" -> "🌴 Bongo Flava"
                                        "Dramatic" -> "🎭 Dramatic"
                                        "Funny" -> "🎪 Funny"
                                        "EDM" -> "⚡ EDM"
                                        "Lofi" -> "🌌 Lofi"
                                        else -> "⚡ All Tracks"
                                    },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Result count label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${searchResults.size} Songs Found • Tap to preview & use",
                            color = BlinkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Done",
                            color = BlinkCyanLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    onToggleExpanded(false)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Search Results List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(searchResults) { track ->
                            val isCurrentTrack = selectedTrack == track.title
                            val isThisTrackPlaying = isPlaying && activeTrack == track.title

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentTrack) BlinkPrimaryDark.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCurrentTrack) BlinkCyan else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectTrack(track.title)
                                        BlinkMusicPlayer.playTrack(track.title)
                                        onToggleExpanded(false)
                                        focusManager.clearFocus()
                                    }
                                    .testTag("song_item_${track.title.take(15).replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Emoji Icon
                                        Text(text = track.iconEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                color = Color.White,
                                                fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = track.artist,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = BlinkCyan.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = track.genre,
                                                        color = BlinkCyanLight,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Play / Preview Button
                                    IconButton(
                                        onClick = {
                                            if (isThisTrackPlaying) {
                                                BlinkMusicPlayer.pausePlayback()
                                            } else {
                                                BlinkMusicPlayer.playTrack(track.title)
                                                onSelectTrack(track.title)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isThisTrackPlaying) BlinkPink else BlinkPrimary)
                                            .testTag("preview_track_${track.title.take(10).replace(" ", "_")}")
                                    ) {
                                        Icon(
                                            imageVector = if (isThisTrackPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isThisTrackPlaying) "Pause" else "Play preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
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
}
