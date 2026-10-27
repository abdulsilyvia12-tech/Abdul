package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.BlinkAiService
import com.example.data.ai.ChatSummaryResult
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onSendMessage: (text: String, isBlink: Boolean, mediaType: String) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onStartVoiceChat: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isBlinkMode by remember { mutableStateOf(conversation.isBlinkActive) }
    var showMenu by remember { mutableStateOf(false) }

    // AI Summary State
    var showSummarySheet by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var chatSummary by remember { mutableStateOf<ChatSummaryResult?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarBadge(
                            name = conversation.name,
                            colorHex = conversation.avatarColorHex,
                            size = 38.dp,
                            showOnline = true,
                            isOnline = conversation.isOnline
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversation.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (conversation.streak > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "🔥 ${conversation.streak}", fontSize = 11.sp, color = BlinkAmber)
                                }
                            }
                            Text(
                                text = if (conversation.isAiBot) "AI Assistant • 24/7 Active" else if (conversation.isOnline) "Active now" else "Offline",
                                fontSize = 11.sp,
                                color = if (conversation.isOnline || conversation.isAiBot) BlinkGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // AI Summary Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSummarizing = true
                                showSummarySheet = true
                                chatSummary = BlinkAiService.summarizeChat(messages.map { it.text })
                                isSummarizing = false
                            }
                        },
                        modifier = Modifier.testTag("chat_ai_summary_button")
                    ) {
                        Icon(Icons.Default.Summarize, contentDescription = "AI Summary", tint = BlinkPrimaryLight)
                    }

                    // Live Voice Chat Button
                    IconButton(
                        onClick = onStartVoiceChat,
                        modifier = Modifier.testTag("chat_voice_orb_button")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Live Voice Chat", tint = BlinkCyanLight)
                    }

                    if (!conversation.isAiBot) {
                        IconButton(
                            onClick = { onStartCall(false) },
                            modifier = Modifier.testTag("start_audio_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = BlinkCyanLight)
                        }
                        IconButton(
                            onClick = { onStartCall(true) },
                            modifier = Modifier.testTag("start_video_call_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = BlinkPrimaryLight)
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isBlinkMode) "Turn Off Blink Mode" else "Turn On Blink Mode") },
                                leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null) },
                                onClick = {
                                    isBlinkMode = !isBlinkMode
                                    showMenu = false
                                }
                            )
                            if (!conversation.isAiBot) {
                                DropdownMenuItem(
                                    text = { Text("Report User", color = BlinkPink) },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = BlinkPink) },
                                    onClick = {
                                        showMenu = false
                                        onReportUser()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block @${conversation.username}", color = BlinkPink) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = BlinkPink) },
                                    onClick = {
                                        showMenu = false
                                        onBlockUser()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Blink AI quick prompt suggestions
                if (conversation.isAiBot) {
                    val aiPrompts = listOf(
                        "✨ Suggest a snap caption",
                        "📸 Which filter is best?",
                        "🛡️ How do I stay safe?",
                        "👻 Explain Ghost Mode"
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(aiPrompts) { prompt ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSendMessage(prompt, false, "none")
                                    },
                                color = BlinkPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 11.sp,
                                    color = BlinkPrimaryLight,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Disappearing Blink indicator bar
                AnimatedVisibility(visible = isBlinkMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BlinkPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = BlinkPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blink Mode Active • Messages disappear in 10s after view",
                            fontSize = 11.sp,
                            color = BlinkPrimaryLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Input Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blink toggle icon button
                    IconButton(
                        onClick = { isBlinkMode = !isBlinkMode },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isBlinkMode) BlinkPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("toggle_blink_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = "Blink mode",
                            tint = if (isBlinkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Camera quick snap button
                    IconButton(
                        onClick = {
                            onSendMessage("Sent a photo snap 📸", isBlinkMode, "photo")
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("chat_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Send Photo",
                            tint = BlinkCyanLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Audio note simulation button
                    IconButton(
                        onClick = {
                            onSendMessage("Voice note (0:08) 🎙️", isBlinkMode, "audio")
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Send Audio",
                            tint = BlinkPinkLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(if (isBlinkMode) "Blink message (10s)..." else "Send a message...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(22.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isBlinkMode) BlinkPink else BlinkPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText, isBlinkMode, "none")
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) BlinkPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    isAi = conversation.isAiBot && !msg.isFromMe
                )
            }
        }
    }

    // --- AI Powered Chat Summary Bottom Sheet ---
    if (showSummarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showSummarySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BlinkPrimaryLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Conversation Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (chatSummary != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BlinkCyan.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = chatSummary!!.vibe,
                                color = BlinkCyanLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isSummarizing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BlinkPrimaryLight)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Blink AI is analyzing conversation...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (chatSummary != null) {
                    Text(
                        text = chatSummary!!.summary,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Key Takeaways:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    chatSummary!!.keyPoints.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = "•", color = BlinkPrimaryLight, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = point, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Suggested Actions:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    chatSummary!!.actionItems.forEach { action ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(text = "👉 $action", fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { showSummarySheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isAi: Boolean = false
) {
    val isMe = message.isFromMe
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isMe && isAi) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BlinkPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        )
                    ),
                color = when {
                    message.isBlink -> if (isMe) BlinkPrimaryDark else BlinkPink.copy(alpha = 0.25f)
                    isMe -> BlinkPrimary
                    isAi -> BlinkPrimaryDark.copy(alpha = 0.25f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.isBlink) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isMe) Color.White else BlinkPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blink Snap (${message.blinkSecondsRemaining}s)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.White else BlinkPink
                            )
                        }
                    }

                    if (message.mediaType == "photo") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.feed_cyber_snap_1792586101459),
                                contentDescription = "Shared photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (message.mediaType == "audio") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isMe) Color.White.copy(alpha = 0.3f) else BlinkPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(12, 24, 18, 30, 16, 28, 20, 14, 22, 10).forEach { barHeight ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isMe) Color.White else BlinkPrimaryLight)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "0:08",
                                fontSize = 10.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.timeFormatted,
                            fontSize = 10.sp,
                            color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = if (message.isRead) BlinkCyanLight else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

