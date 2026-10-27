package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.BlinkAiService
import com.example.data.ai.ChatSummaryResult
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.User
import com.example.data.repository.BlinkRepository
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatScreen component built with Jetpack Compose.
 * Displays a real-time list of messages synchronized with Cloud Firestore
 * and provides a rich text input field for composing and sending new messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String = "conv_general",
    conversationTitle: String = "General Chat",
    recipientUsername: String = "community",
    recipientAvatarColorHex: String = "#8B5CF6",
    isRecipientOnline: Boolean = true,
    currentUser: User? = null,
    onBack: (() -> Unit)? = null,
    repository: BlinkRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Firestore instance resolution
    val firestore = remember {
        repository?.firestore ?: try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId)
            else FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e2: Exception) {
                null
            }
        }
    }

    // State for Firestore live messages
    var firestoreMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isFirestoreConnected by remember { mutableStateOf(false) }
    var firestoreError by remember { mutableStateOf<String?>(null) }

    // State for local repository fallback messages
    val repoMessagesMap by repository?.messages?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyMap()) }
    val repoMessages = repoMessagesMap[conversationId] ?: emptyList()

    // Input state
    var inputText by remember { mutableStateOf("") }
    var isBlinkMode by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    // AI summary state
    var showAiSummarySheet by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var chatSummary by remember { mutableStateOf<ChatSummaryResult?>(null) }

    // Real-time Firestore Snapshot Listener
    DisposableEffect(conversationId, firestore) {
        var listenerRegistration: ListenerRegistration? = null
        if (firestore != null) {
            try {
                listenerRegistration = firestore.collection("messages")
                    .whereEqualTo("conversationId", conversationId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("ChatScreen", "Firestore listen error: ${error.message}")
                            firestoreError = error.message
                            isFirestoreConnected = false
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val curUserId = currentUser?.id ?: "user_me"
                            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val items = snapshot.documents.mapNotNull { doc ->
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: ""
                                val text = doc.getString("text") ?: ""
                                val mediaUri = doc.getString("mediaUri")
                                val mediaType = doc.getString("mediaType") ?: "none"
                                val isBlink = doc.getBoolean("isBlink") ?: false
                                val blinkSeconds = doc.getLong("blinkSecondsRemaining")?.toInt() ?: 10
                                val ts = doc.getTimestamp("createdAt")?.toDate()?.time
                                    ?: doc.getLong("timestamp")
                                    ?: System.currentTimeMillis()
                                ChatMessage(
                                    id = id,
                                    conversationId = conversationId,
                                    senderId = senderId,
                                    text = text,
                                    mediaUri = mediaUri,
                                    mediaType = mediaType,
                                    timestamp = ts,
                                    timeFormatted = timeFormat.format(Date(ts)),
                                    isBlink = isBlink,
                                    blinkSecondsRemaining = blinkSeconds,
                                    isFromMe = (senderId == curUserId)
                                )
                            }.sortedBy { it.timestamp }

                            firestoreMessages = items
                            isFirestoreConnected = true
                            firestoreError = null
                        }
                    }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to attach snapshot listener: ${e.message}")
            }
        }

        onDispose {
            listenerRegistration?.remove()
        }
    }

    // Unified message stream: merges Firestore documents with local state to ensure instant feedback
    val displayMessages = remember(firestoreMessages, repoMessages) {
        val map = linkedMapOf<String, ChatMessage>()
        repoMessages.forEach { map[it.id] = it }
        firestoreMessages.forEach { map[it.id] = it }
        map.values.sortedBy { it.timestamp }
    }

    // Auto-scroll to latest message
    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    // Send message logic
    val onSendAction: () -> Unit = {
        val trimmed = inputText.trim()
        if (trimmed.isNotBlank()) {
            isSending = true
            val msgId = "m_" + UUID.randomUUID().toString().take(8)
            val curUserId = currentUser?.id ?: "user_me"
            val curUserName = currentUser?.displayName ?: "Alex Rivers"
            val now = System.currentTimeMillis()
            val formattedTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))

            // 1. Send via repository (updates local reactive flows, conversation last message & triggers AI reply)
            repository?.sendMessage(
                conversationId = conversationId,
                text = trimmed,
                isBlink = isBlinkMode,
                mediaType = "none"
            )

            // 2. Direct Firestore write
            try {
                if (firestore != null) {
                    val messagePayload = hashMapOf(
                        "id" to msgId,
                        "conversationId" to conversationId,
                        "senderId" to curUserId,
                        "senderName" to curUserName,
                        "text" to trimmed,
                        "mediaUri" to null,
                        "mediaType" to "none",
                        "isBlink" to isBlinkMode,
                        "blinkSecondsRemaining" to (if (isBlinkMode) 10 else 0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "timestamp" to now,
                        "timeFormatted" to formattedTimeStr
                    )
                    firestore.collection("messages").document(msgId).set(messagePayload)
                        .addOnSuccessListener {
                            isFirestoreConnected = true
                        }
                        .addOnFailureListener { e ->
                            Log.w("ChatScreen", "Firestore send failure: ${e.message}")
                        }
                }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Exception writing message to Firestore: ${e.message}")
            }

            inputText = ""
            isSending = false
            coroutineScope.launch {
                if (displayMessages.isNotEmpty()) {
                    listState.animateScrollToItem(displayMessages.size - 1)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("chat_header_info")
                    ) {
                        AvatarBadge(
                            name = conversationTitle,
                            colorHex = recipientAvatarColorHex,
                            size = 38.dp,
                            showOnline = true,
                            isOnline = isRecipientOnline
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversationTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Live Firestore Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isFirestoreConnected) BlinkGreen.copy(alpha = 0.18f)
                                            else BlinkAmber.copy(alpha = 0.18f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isFirestoreConnected) BlinkGreen.copy(alpha = 0.5f)
                                            else BlinkAmber.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .testTag("firestore_status_badge")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isFirestoreConnected) BlinkGreen else BlinkAmber)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isFirestoreConnected) "Firestore Live" else "Firestore Sync",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFirestoreConnected) BlinkGreen else BlinkAmber
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isRecipientOnline) "@$recipientUsername • Active now" else "@$recipientUsername • Offline",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    // Voice Call Button
                    IconButton(
                        onClick = {
                            val target = repository?.allUsers?.value?.find { it.username == recipientUsername }
                                ?: User(
                                    id = "user_$recipientUsername",
                                    username = recipientUsername,
                                    displayName = conversationTitle,
                                    email = "$recipientUsername@blinkchat.com",
                                    avatarColorHex = recipientAvatarColorHex,
                                    isOnline = isRecipientOnline
                                )
                            repository?.startCall(target, isVideo = false)
                        },
                        modifier = Modifier.testTag("chat_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = BlinkCyanLight
                        )
                    }

                    // Video Call Button
                    IconButton(
                        onClick = {
                            val target = repository?.allUsers?.value?.find { it.username == recipientUsername }
                                ?: User(
                                    id = "user_$recipientUsername",
                                    username = recipientUsername,
                                    displayName = conversationTitle,
                                    email = "$recipientUsername@blinkchat.com",
                                    avatarColorHex = recipientAvatarColorHex,
                                    isOnline = isRecipientOnline
                                )
                            repository?.startCall(target, isVideo = true)
                        },
                        modifier = Modifier.testTag("chat_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = BlinkPrimaryLight
                        )
                    }

                    // AI Chat Summary action
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSummarizing = true
                                showAiSummarySheet = true
                                chatSummary = BlinkAiService.summarizeChat(displayMessages.map { it.text })
                                isSummarizing = false
                            }
                        },
                        modifier = Modifier.testTag("chat_ai_summary_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = "AI Summary",
                            tint = BlinkPrimaryLight
                        )
                    }

                    // Ephemeral Blink Mode Toggle
                    IconButton(
                        onClick = { isBlinkMode = !isBlinkMode },
                        modifier = Modifier.testTag("chat_blink_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isBlinkMode) Icons.Default.HourglassTop else Icons.Outlined.HourglassEmpty,
                            contentDescription = "Disappearing Blink Mode",
                            tint = if (isBlinkMode) BlinkPink else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                // Active Blink Mode Banner
                AnimatedVisibility(visible = isBlinkMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BlinkPink.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = BlinkPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blink Mode active • Messages expire in 10s after opening",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlinkPinkLight
                        )
                    }
                }

                // Quick message suggestions bar when chat is empty or starting
                if (displayMessages.size <= 2) {
                    val quickPrompts = listOf(
                        "👋 Hello!",
                        "⚡ Blinkchat is awesome!",
                        "🔥 Loved your post!",
                        "📸 Send a snap"
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPrompts) { prompt ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        inputText = prompt
                                    },
                                color = BlinkPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 11.sp,
                                    color = BlinkPrimaryLight,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Input Field Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Blink Toggle inside input bar
                    IconButton(
                        onClick = { isBlinkMode = !isBlinkMode },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBlinkMode) BlinkPink.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("chat_quick_blink_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Toggle Blink",
                            tint = if (isBlinkMode) BlinkPink else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Main Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                            .testTag("chat_text_input"),
                        placeholder = {
                            Text(
                                text = if (isBlinkMode) "Send disappearing Blink..." else "Type a message...",
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendAction() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isBlinkMode) BlinkPink else BlinkPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    val canSend = inputText.trim().isNotBlank()
                    val sendScale by animateFloatAsState(
                        targetValue = if (canSend) 1.05f else 1.0f,
                        label = "send_scale"
                    )

                    IconButton(
                        onClick = onSendAction,
                        enabled = canSend,
                        modifier = Modifier
                            .size(44.dp)
                            .scale(sendScale)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    if (isBlinkMode) Brush.linearGradient(listOf(BlinkPink, BlinkAmber))
                                    else Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                            .testTag("chat_send_button")
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (displayMessages.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BlinkPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = BlinkPrimaryLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Say hello to $conversationTitle!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Messages are synchronized in real-time with Cloud Firestore. Type a message below to start the conversation! ⚡",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // List of Messages from Firestore
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .testTag("chat_messages_list")
                        .testTag("messages_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Header indicating Firestore live channel
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "💬 Firestore Live Chat • ${displayMessages.size} messages",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    itemsIndexed(displayMessages, key = { _, message -> message.id }) { index, message ->
                        val showDateSeparator = if (index == 0) {
                            true
                        } else {
                            val prevMessage = displayMessages[index - 1]
                            val calPrev = Calendar.getInstance().apply { timeInMillis = prevMessage.timestamp }
                            val calCur = Calendar.getInstance().apply { timeInMillis = message.timestamp }
                            calPrev.get(Calendar.DAY_OF_YEAR) != calCur.get(Calendar.DAY_OF_YEAR) ||
                            calPrev.get(Calendar.YEAR) != calCur.get(Calendar.YEAR)
                        }

                        if (showDateSeparator) {
                            val dateHeader = remember(message.timestamp) {
                                val msgCal = Calendar.getInstance().apply { timeInMillis = message.timestamp }
                                val nowCal = Calendar.getInstance()
                                when {
                                    nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                                    nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> "Today"
                                    nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                                    nowCal.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
                                    else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(message.timestamp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = dateHeader,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        ChatMessageBubble(
                            message = message,
                            recipientAvatarColor = recipientAvatarColorHex,
                            recipientName = conversationTitle
                        )
                    }
                }
            }
        }
    }

    // AI Summary Bottom Sheet
    if (showAiSummarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSummarySheet = false },
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BlinkPrimaryLight
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Chat Summary",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showAiSummarySheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                if (isSummarizing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BlinkPrimary)
                    }
                } else if (chatSummary != null) {
                    val summary = chatSummary!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Key Discussion & Takeaways:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlinkPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summary.summary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            if (summary.keyPoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                summary.keyPoints.forEach { point ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text("• ", color = BlinkCyanLight, fontWeight = FontWeight.Bold)
                                        Text(point, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAiSummarySheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

/**
 * Convenience overload for ChatScreen taking a Conversation object.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversation: Conversation,
    currentUser: User?,
    onBack: () -> Unit,
    repository: BlinkRepository,
    modifier: Modifier = Modifier
) {
    ChatScreen(
        conversationId = conversation.id,
        conversationTitle = conversation.name,
        recipientUsername = conversation.username,
        recipientAvatarColorHex = conversation.avatarColorHex,
        isRecipientOnline = conversation.isOnline,
        currentUser = currentUser,
        onBack = onBack,
        repository = repository,
        modifier = modifier
    )
}

/**
 * Message bubble item rendering distinct styles for outgoing (from me) vs incoming messages.
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    recipientAvatarColor: String,
    recipientName: String,
    modifier: Modifier = Modifier
) {
    val isFromMe = message.isFromMe

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chat_message_item_${message.id}"),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isFromMe) 0.85f else 0.88f),
            horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            // Incoming message avatar
            if (!isFromMe) {
                AvatarBadge(
                    name = recipientName,
                    colorHex = recipientAvatarColor,
                    size = 28.dp,
                    modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
                )
            }

            // Message Bubble Surface
            Surface(
                shape = if (isFromMe) {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 4.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 18.dp
                    )
                },
                color = if (isFromMe) {
                    if (message.isBlink) BlinkPink else BlinkPrimary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 2.dp,
                modifier = Modifier
                    .clip(
                        if (isFromMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                        else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                    )
                    .then(
                        if (isFromMe) {
                            Modifier.background(
                                if (message.isBlink) Brush.horizontalGradient(listOf(BlinkPink, BlinkAmber))
                                else Brush.horizontalGradient(listOf(BlinkPrimary, BlinkCyan))
                            )
                        } else Modifier
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    // Ephemeral Blink Header
                    if (message.isBlink) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isFromMe) Color.White else BlinkPink,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blink (10s)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFromMe) Color.White.copy(alpha = 0.9f) else BlinkPink
                            )
                        }
                    }

                    // Sender name for group chats
                    if (!isFromMe && message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BlinkCyanLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Message text body
                    Text(
                        text = message.text,
                        color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Explicit time in message
                    val displayTime = remember(message.timestamp, message.timeFormatted) {
                        if (message.timeFormatted.isNotBlank() && message.timeFormatted != "Just now") {
                            message.timeFormatted
                        } else if (message.timestamp > 0) {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
                        } else {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                        }
                    }

                    // Timestamp and delivery checkmark
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("chat_message_time_container_${message.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Message timestamp",
                            tint = if (isFromMe) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = displayTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isFromMe) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("chat_message_time_${message.id}")
                        )
                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
