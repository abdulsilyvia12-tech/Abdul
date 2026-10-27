package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.BlinkRepository
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    callState: BlinkRepository.CallState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onFlipCamera: () -> Unit
) {
    var callSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(callState.status) {
        if (callState.status == "Connected") {
            while (true) {
                delay(1000)
                callSeconds++
            }
        }
    }

    val formattedDuration = remember(callSeconds) {
        val minutes = callSeconds / 60
        val secs = callSeconds % 60
        String.format("%02d:%02d", minutes, secs)
    }

    val targetUser = callState.targetUser

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlinkDarkBg)
            .testTag("call_screen")
    ) {
        // --- Video Stream Background (if video call) ---
        if (callState.isVideo) {
            Image(
                painter = painterResource(id = R.drawable.feed_sunset_snap_1792586114190),
                contentDescription = "Video stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Picture-in-Picture Self Camera View
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .width(100.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .background(Color.DarkGray)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.feed_cyber_snap_1792586101459),
                    contentDescription = "My camera",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // --- Caller Information Header ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!callState.isVideo) {
                AvatarBadge(
                    name = targetUser?.displayName ?: "Friend",
                    colorHex = targetUser?.avatarColorHex ?: "#8B5CF6",
                    size = 110.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = targetUser?.displayName ?: "Friend",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (callState.status == "Connected") formattedDuration else callState.status,
                fontSize = 14.sp,
                color = if (callState.status == "Connected") BlinkGreen else BlinkCyanLight,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (callState.isVideo) "📹 HD Video Call • End-to-End Encrypted" else "🎙️ HD Voice Call • End-to-End Encrypted",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // --- Bottom Call Action Controls ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute Mic Button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        .testTag("call_mute_button")
                ) {
                    Icon(
                        imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (callState.isMuted) Color.Black else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Speaker Button
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (callState.isSpeakerOn) BlinkPrimary else Color.White.copy(alpha = 0.2f))
                        .testTag("call_speaker_button")
                ) {
                    Icon(
                        imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        contentDescription = "Speaker",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Flip Camera (if video call)
                if (callState.isVideo) {
                    IconButton(
                        onClick = onFlipCamera,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .testTag("call_flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // End Call Button (Red)
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BlinkPink)
                        .testTag("call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
