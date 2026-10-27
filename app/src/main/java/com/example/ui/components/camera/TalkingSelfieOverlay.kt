package com.example.ui.components.camera

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.BlinkMusicPlayer
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun TalkingSelfieFaceBubble(
    isActive: Boolean,
    onClose: () -> Unit,
    voiceMode: String,
    onVoiceModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "talking_selfie")
    val pulseRing by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ring"
    )

    // Animated talking mouth opening & closing
    val mouthHeight by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_height"
    )

    var currentPhraseIndex by remember { mutableStateOf(0) }
    val phrases = listOf(
        "Say anything, I repeat it! 🎙️",
        "Blinkchat talking lens on point! ✨",
        "Testing 1 2 3... Drop the beat! 🎶",
        "Looking stunning today! 🤩",
        "Tag your crew in this snap! 🚀"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 110.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speech Bubble pinned above face lens
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, BlinkCyan),
                modifier = Modifier.testTag("talking_selfie_speech_bubble")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🗣️", fontSize = 14.sp)
                    Text(
                        text = phrases[currentPhraseIndex % phrases.size],
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Lens", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // The Round Talking Face Lens
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseRing)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF3B0764), Color(0xFF0F172A), Color.Black)
                        )
                    )
                    .border(
                        3.dp,
                        Brush.linearGradient(listOf(BlinkPink, BlinkCyan, BlinkPrimary)),
                        CircleShape
                    )
                    .clickable {
                        currentPhraseIndex++
                        BlinkMusicPlayer.playSoundEffect("squeak")
                    }
                    .testTag("talking_selfie_face_bubble"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Cartoon Eyes
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Talking animated mouth
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(mouthHeight.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF43F5E))
                            .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                    )
                }
            }

            // Voice Pitch Mode selector pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "normal" to "Natural 🗣️",
                    "chipmunk" to "Chipmunk 🐿️",
                    "robot" to "Robot 🤖",
                    "echo" to "Echo 🏔️"
                ).forEach { (mode, label) ->
                    val isSelected = voiceMode == mode
                    Surface(
                        onClick = {
                            onVoiceModeChange(mode)
                            BlinkMusicPlayer.playSoundEffect("laugh")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BlinkPink else Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
