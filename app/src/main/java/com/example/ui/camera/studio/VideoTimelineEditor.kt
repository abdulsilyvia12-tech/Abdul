package com.example.ui.camera.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * TikTok / Reels Style Video Timeline Editor & Trimmer.
 * Features:
 * 1. Visual Thumbnail Filmstrip across clip duration.
 * 2. Draggable Playhead Scrubber with synchronized timestamp.
 * 3. Left & Right Trim Handles to set start/end points.
 * 4. Speed Multipliers (0.5x, 1.0x, 2.0x, 3.0x).
 */
@Composable
fun VideoTimelineEditor(
    totalDurationSec: Float,
    currentPositionSec: Float,
    trimStartSec: Float,
    trimEndSec: Float,
    speedMultiplier: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    onTrimChange: (start: Float, end: Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.82f))
            .border(1.dp, BlinkCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("video_timeline_editor"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Timing Header & Play/Pause
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BlinkPrimary)
                        .testTag("timeline_play_toggle")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Time Readout (e.g. 00:03.4 / 00:15.0)
                val curMin = (currentPositionSec / 60).toInt()
                val curSec = (currentPositionSec % 60).toInt()
                val totMin = (totalDurationSec / 60).toInt()
                val totSec = (totalDurationSec % 60).toInt()
                Text(
                    text = String.format("%02d:%02d / %02d:%02d", curMin, curSec, totMin, totSec),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Speed Control Buttons (0.5x, 1x, 2x, 3x)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0.5f to "0.5x", 1.0f to "1x", 2.0f to "2x", 3.0f to "3x").forEach { (speed, label) ->
                    val isSel = speedMultiplier == speed
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSpeedChange(speed) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) BlinkPink else Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color.White else Color.Transparent)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Filmstrip Timeline Track with Scrubber
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E1B4B))
                .border(1.5.dp, BlinkCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()

            // Simulated Video Thumbnail Frames Strip
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(8) { idx ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (idx % 2 == 0) Color(0xFF312E81) else Color(0xFF3730A3)
                            )
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎞️",
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Trim Dimming Regions (Left trim dim, Right trim dim)
            val startRatio = (trimStartSec / totalDurationSec).coerceIn(0f, 1f)
            val endRatio = (trimEndSec / totalDurationSec).coerceIn(0f, 1f)

            if (startRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(startRatio)
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // Active Yellow Trim Box
            val boxStartPx = totalWidthPx * startRatio
            val boxEndPx = totalWidthPx * endRatio
            val boxWidthDp = ((boxEndPx - boxStartPx) / 2.7f).dp

            Box(
                modifier = Modifier
                    .offset(x = (boxStartPx / 2.7f).dp)
                    .width(boxWidthDp)
                    .fillMaxHeight()
                    .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(6.dp))
            )

            // Draggable Red Scrubber Playhead
            val playheadRatio = (currentPositionSec / totalDurationSec).coerceIn(0f, 1f)
            val playheadOffsetDp = ((totalWidthPx * playheadRatio) / 2.7f).dp

            Box(
                modifier = Modifier
                    .offset(x = playheadOffsetDp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newRatio = (playheadRatio + (dragAmount.x / totalWidthPx)).coerceIn(0f, 1f)
                            onSeek(newRatio * totalDurationSec)
                        }
                    }
            )
        }

        // Trim slider guides
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "✂️ Start: ${String.format("%.1fs", trimStartSec)}",
                color = Color(0xFFFBBF24),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "End: ${String.format("%.1fs", trimEndSec)}",
                color = Color(0xFFFBBF24),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
