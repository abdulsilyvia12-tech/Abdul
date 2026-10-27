package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.BlinkAiService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VoiceConversationScreen(
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isListening by remember { mutableStateOf(false) }
    var isAiSpeaking by remember { mutableStateOf(false) }
    var userTranscript by remember { mutableStateOf("Tap the microphone to speak...") }
    var aiTranscript by remember { mutableStateOf("Hey! I'm Blink AI in live voice mode. Talk to me!") }

    // Pulsing visualizer animation for voice orb
    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening || isAiSpeaking) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening || isAiSpeaking) 900 else 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlinkDarkBg)
            .testTag("voice_conversation_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose, modifier = Modifier.testTag("voice_close_button")) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isListening) BlinkCyan.copy(alpha = 0.2f) else if (isAiSpeaking) BlinkPink.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isListening) BlinkCyan else if (isAiSpeaking) BlinkPink else BlinkGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "Listening to you..." else if (isAiSpeaking) "Blink AI speaking..." else "Voice Live Active",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = { /* Settings */ }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = Color.White)
            }
        }

        // Center Voice Visualizer Orb
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Ripple Ring
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isListening) BlinkCyan.copy(alpha = 0.4f) else if (isAiSpeaking) BlinkPink.copy(alpha = 0.4f) else BlinkPrimary.copy(alpha = 0.25f),
                            CircleShape
                        )
                )

                // Glowing Central Orb
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(orbScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = when {
                                    isListening -> listOf(BlinkCyanLight, BlinkCyan, Color.Transparent)
                                    isAiSpeaking -> listOf(BlinkPinkLight, BlinkPrimary, Color.Transparent)
                                    else -> listOf(BlinkPrimaryLight, BlinkPrimaryDark, Color.Transparent)
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else if (isAiSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Realtime Transcripts Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "You said:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlinkCyanLight
                    )
                    Text(
                        text = userTranscript,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text(
                        text = "Blink AI:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlinkPinkLight,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = aiTranscript,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Bottom Controls: Microphone trigger
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simulated speech sample prompt chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "\"How's my streak?\"",
                    "\"Suggest a night filter\"",
                    "\"Is my post safe?\""
                ).forEach { sample ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch {
                                    val cleaned = sample.replace("\"", "")
                                    userTranscript = cleaned
                                    isListening = true
                                    delay(800)
                                    isListening = false
                                    isAiSpeaking = true
                                    val reply = BlinkAiService.getVoiceChatResponse(cleaned)
                                    aiTranscript = reply
                                    delay(2000)
                                    isAiSpeaking = false
                                }
                            },
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = sample,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Interactive Push-to-Talk Mic
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isListening) BlinkCyan else BlinkPrimary)
                    .clickable {
                        coroutineScope.launch {
                            if (!isListening && !isAiSpeaking) {
                                isListening = true
                                userTranscript = "Hey Blink AI, tell me what filter to use for a night snap!"
                                delay(1200)
                                isListening = false
                                isAiSpeaking = true
                                val response = BlinkAiService.getVoiceChatResponse(userTranscript)
                                aiTranscript = response
                                delay(2200)
                                isAiSpeaking = false
                            }
                        }
                    }
                    .testTag("push_to_talk_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Speak",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isListening) "Listening... speak now" else "Tap microphone to speak",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
