package com.example.ui.components.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.audio.BlinkMusicPlayer
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkCyanLight
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun CameraVideoEditDialog(
    visible: Boolean,
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    currentTimerSec: Int,
    onTimerChange: (Int) -> Unit,
    isGridEnabled: Boolean,
    onToggleGrid: () -> Unit,
    isFlashOn: Boolean,
    onToggleFlash: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, BlinkCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("camera_video_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✂️", fontSize = 18.sp)
                        Text("Video Studio Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // 1. Video Recording Speed
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recording Speed:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0.5f to "0.5x 🐢",
                            1.0f to "1.0x Standard",
                            2.0f to "2.0x 🐇",
                            3.0f to "3.0x 🚀"
                        ).forEach { (speed, label) ->
                            val isSelected = currentSpeed == speed
                            Surface(
                                onClick = { onSpeedChange(speed) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BlinkPrimary else Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, if (isSelected) BlinkCyan else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Hands-Free Countdown Timer
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hands-Free Countdown Timer:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "Off",
                            3 to "3 Seconds ⏱️",
                            10 to "10 Seconds ⏳"
                        ).forEach { (sec, label) ->
                            val isSelected = currentTimerSec == sec
                            Surface(
                                onClick = { onTimerChange(sec) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) BlinkPink else Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, if (isSelected) Color.White else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Grid Guide & Flash Torch Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        onClick = onToggleGrid,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isGridEnabled) BlinkCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (isGridEnabled) BlinkCyan else Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📐", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isGridEnabled) "Grid Guide ON" else "3x3 Grid Guide",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        onClick = onToggleFlash,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFlashOn) Color(0xFFFACC15).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (isFlashOn) Color(0xFFFACC15) else Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("⚡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isFlashOn) "Torch ON" else "Torch Flash",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Live Soundboard FX (Dramatic, Comedy, Squeak, Airhorn)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Live Soundboard FX (Tap to trigger sound!):", color = BlinkCyanLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "dramatic" to "😱 Dun Dun!",
                            "laugh" to "😂 Laugh",
                            "squeak" to "🦆 Squeak",
                            "airhorn" to "📢 Airhorn"
                        ).forEach { (effect, label) ->
                            Surface(
                                onClick = { BlinkMusicPlayer.playSoundEffect(effect) },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
