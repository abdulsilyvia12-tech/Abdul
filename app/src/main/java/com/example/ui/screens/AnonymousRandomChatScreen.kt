package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveRandomChat
import com.example.data.model.RandomChatMessage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnonymousRandomChatScreen(
    chat: ActiveRandomChat,
    messages: List<RandomChatMessage>,
    myAnonymousId: String,
    myAnonymousName: String,
    onSendMessage: (String) -> Unit,
    onSkipNext: () -> Unit,
    onReport: (reason: String, details: String) -> Unit,
    onEndChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onEndChat()
    }

    val peerName = remember(chat, myAnonymousId) {
        if (chat.participantA == myAnonymousId) chat.participantBName else chat.participantAName
    }

    val peerAvatarColor = remember(chat, myAnonymousId) {
        val hex = if (chat.participantA == myAnonymousId) chat.participantBAvatar else chat.participantAAvatar
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            Color(0xFF06B6D4)
        }
    }

    // 2-minute timer logic
    var remainingSeconds by remember(chat.id) {
        val diff = (chat.expiresAt - System.currentTimeMillis()) / 1000
        mutableStateOf(diff.coerceIn(0, 120))
    }
    var isTimeExpired by remember { mutableStateOf(false) }

    LaunchedEffect(chat.id, chat.expiresAt) {
        while (remainingSeconds > 0) {
            delay(1000)
            val diff = (chat.expiresAt - System.currentTimeMillis()) / 1000
            remainingSeconds = diff.coerceAtLeast(0)
            if (remainingSeconds <= 0) {
                isTimeExpired = true
                break
            }
        }
    }

    var messageInput by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("Inappropriate Content") }
    var reportDetails by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickStarters = remember {
        listOf(
            "Hey! Where are you blinking from? 🌍",
            "What music are you listening to? 🎧",
            "Blink or treat! Having a good day? ✨",
            "Rate my anonymous avatar 🎨",
            "Only 2 mins, quick: favorite movie? 🎬"
        )
    }

    val timerColor = when {
        remainingSeconds > 60 -> BlinkCyanLight
        remainingSeconds > 20 -> BlinkAmber
        else -> Color(0xFFEF4444)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onEndChat,
                            modifier = Modifier.testTag("end_random_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit chat"
                            )
                        }

                        // Peer Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(peerAvatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = peerName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Peer Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = peerName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "⚡",
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Anonymous • Connected via Firestore",
                                fontSize = 11.sp,
                                color = BlinkCyanLight
                            )
                        }

                        // 2-Minute Countdown Pill
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = timerColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, timerColor.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("chat_timer_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = timerColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = timerColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Safety Report Button
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("report_partner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = "Report",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Skip / Next Button
                        Button(
                            onClick = onSkipNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlinkPink
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("skip_next_button")
                        ) {
                            Text(
                                text = "Next ⏩",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Linear Timer Progress Bar
                    val progress = (remainingSeconds / 120f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = timerColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    // Quick Conversation Starter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickStarters) { starter ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.clickable {
                                    onSendMessage(starter)
                                }
                            ) {
                                Text(
                                    text = starter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Type anonymous Blink message...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("anonymous_message_input"),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3,
                            singleLine = false
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    onSendMessage(messageInput)
                                    messageInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = BlinkPrimary
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_anonymous_message_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("anonymous_chat_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    if (msg.senderId == "system") {
                        // System info banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BlinkPrimary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        val isMe = msg.isFromMe || (msg.senderId == myAnonymousId)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isMe) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(peerAvatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = peerName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    ),
                                    color = if (isMe) BlinkPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 1.dp
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }

                                Text(
                                    text = formatChatTime(msg.createdAt),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Time Expired Alert Overlay
            if (isTimeExpired) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, BlinkPink),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .testTag("time_expired_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = BlinkPink,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "2 Minutes Reached! ⏳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The anonymous Blink timer has ended. You can skip to pair with a new random partner right now!",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEndChat,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Exit")
                            }
                            Button(
                                onClick = {
                                    isTimeExpired = false
                                    onSkipNext()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                                modifier = Modifier.weight(1f).testTag("time_expired_next_btn")
                            ) {
                                Text("Next Blink ⚡")
                            }
                        }
                    }
                }
            }
        }
    }

    // Safety Report Dialog
    if (showReportDialog) {
        val reportReasons = listOf(
            "Inappropriate Content / Nudity",
            "Harassment or Hate Speech",
            "Spam or Commercial Links",
            "Underage Safety Concern",
            "Other Policy Violation"
        )

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = null,
                        tint = BlinkPink
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Report Anonymous User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Report $peerName for safety review. The chat will be ended immediately:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    reportReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReportReason == reason,
                                onClick = { selectedReportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = reason, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        placeholder = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReport(selectedReportReason, reportDetails)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPink),
                    modifier = Modifier.testTag("submit_report_button")
                ) {
                    Text("Submit Report & Next", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatChatTime(millis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(millis))
}
