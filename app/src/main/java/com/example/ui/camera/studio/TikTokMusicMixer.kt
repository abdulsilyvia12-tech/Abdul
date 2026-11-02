package com.example.ui.camera.studio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.BlinkMusicPlayer
import com.example.data.audio.MusicTrackInfo
import com.example.ui.theme.*

/**
 * TikTok-style Music Mixer & Trending Sounds Studio.
 * Features:
 * 1. 6 Stored Trending TikTok Sounds with instant audio preview on tap.
 * 2. Visual Audio Waveform animation synced with video playback.
 * 3. Dual Volume Sliders: Original Mic Audio vs Added Music Soundtrack.
 */
@Composable
fun TikTokMusicMixer(
    selectedTrack: String,
    originalVolume: Float,
    musicVolume: Float,
    isVideoPlaying: Boolean,
    onSelectSound: (MusicTrackInfo) -> Unit,
    onOriginalVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingSounds = remember { BlinkMusicPlayer.trendingTikTokSounds }

    // Waveform jump animation for audio visualizer
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(250, easing = LinearEasing), RepeatMode.Reverse), label = "w1"
    )
    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse), label = "w2"
    )
    val waveHeight3 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse), label = "w3"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, BlinkPink.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("tiktok_music_mixer"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header: Active Sound with Animated Waveform
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = selectedTrack.ifBlank { "Choose Trending Sound" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // Synced Audio Waveform Animation
            if (isVideoPlaying && selectedTrack.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(24.dp)
                ) {
                    Box(modifier = Modifier.width(3.dp).height(waveHeight1.dp).clip(RoundedCornerShape(2.dp)).background(BlinkCyanLight))
                    Box(modifier = Modifier.width(3.dp).height(waveHeight2.dp).clip(RoundedCornerShape(2.dp)).background(BlinkPink))
                    Box(modifier = Modifier.width(3.dp).height(waveHeight3.dp).clip(RoundedCornerShape(2.dp)).background(BlinkPrimary))
                    Box(modifier = Modifier.width(3.dp).height(waveHeight1.dp * 0.8f).clip(RoundedCornerShape(2.dp)).background(BlinkCyanLight))
                }
            }
        }

        // --- 1. Stored Trending TikTok Sounds (6 Curated MP3s) ---
        Text(
            text = "🔥 6 Trending TikTok Sounds (Tap to Play & Sync):",
            color = BlinkCyanLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(trendingSounds) { sound ->
                val isSelected = selectedTrack.contains(sound.title.take(10))
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectSound(sound)
                            BlinkMusicPlayer.playTrack(sound.title)
                        }
                        .testTag("sound_item_${sound.title.take(6)}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) BlinkPrimary else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) BlinkCyan else Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(sound.iconEmoji, fontSize = 16.sp)
                        Column {
                            Text(
                                text = sound.title,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = sound.artist,
                                color = BlinkTextSecondary,
                                fontSize = 9.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.8.dp)

        // --- 2. Dual Volume Sliders (Original Video Audio vs Added Music) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Slider 1: Original Audio Volume
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🗣️ Original Mic", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Text("${(originalVolume * 100).toInt()}%", color = BlinkCyanLight, fontSize = 10.5.sp)
                }
                Slider(
                    value = originalVolume,
                    onValueChange = onOriginalVolumeChange,
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = BlinkCyan,
                        activeTrackColor = BlinkCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }

            // Slider 2: Music Soundtrack Volume
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎵 Added Music", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Text("${(musicVolume * 100).toInt()}%", color = BlinkPink, fontSize = 10.5.sp)
                }
                Slider(
                    value = musicVolume,
                    onValueChange = onMusicVolumeChange,
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = BlinkPink,
                        activeTrackColor = BlinkPink,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}
