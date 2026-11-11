package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.util.AudioPlayerManager
import com.example.util.FirebaseStorageAudioUploader
import com.example.util.VoiceRecorderManager
import com.example.ui.components.FullscreenMediaViewer
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.BlinkAiService
import com.example.data.ai.ChatSummaryResult
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.User
import com.example.data.repository.BlinkRepository
import com.example.ui.components.AvatarBadge
import com.example.ui.components.camera.CameraXPreviewCapture
import com.example.ui.theme.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
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
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("ChatScreen", "Failed to get Firestore instance with databaseId: ${e.message}")
            null
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
    var isCameraSnapOpen by remember { mutableStateOf(false) }
    var selectedMediaForFullscreen by remember { mutableStateOf<ChatMessage?>(null) }
    var selectedMessageForReaction by remember { mutableStateOf<ChatMessage?>(null) }

    // Voice audio recording & playback managers
    val voiceRecorderManager = remember { VoiceRecorderManager(context) }
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    var isVoiceRecording by remember { mutableStateOf(false) }
    var voiceRecordingTicks by remember { mutableIntStateOf(0) }
    var voiceAmplitude by remember { mutableIntStateOf(0) }

    // Audio playback state
    var playingAudioMessageId by remember { mutableStateOf<String?>(null) }
    var audioPlaybackProgress by remember { mutableFloatStateOf(0f) }
    var audioCurrentPositionMs by remember { mutableIntStateOf(0) }

    // Clean up audio hardware on dispose
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorderManager.cancelRecording()
            audioPlayerManager.stop()
        }
    }

    // Timer & amplitude meter loop
    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            voiceRecordingTicks = 0
            while (isVoiceRecording) {
                delay(200)
                if (isVoiceRecording) {
                    voiceAmplitude = voiceRecorderManager.getMaxAmplitude()
                    voiceRecordingTicks++
                }
            }
        } else {
            voiceRecordingTicks = 0
            voiceAmplitude = 0
        }
    }

    // Audio playback polling
    LaunchedEffect(playingAudioMessageId) {
        if (playingAudioMessageId != null) {
            // Wait for player to prepare if needed
            var waitCounter = 0
            while (playingAudioMessageId != null && audioPlayerManager.isPreparing && waitCounter < 30) {
                delay(80)
                waitCounter++
            }
            while (playingAudioMessageId != null && (audioPlayerManager.isPlaying || audioPlayerManager.isPreparing || audioPlayerManager.currentPosition > 0)) {
                val cur = audioPlayerManager.currentPosition
                val dur = audioPlayerManager.duration
                audioCurrentPositionMs = cur
                if (dur > 0) {
                    audioPlaybackProgress = (cur.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                }
                delay(100)
            }
            if (!audioPlayerManager.isPlaying && !audioPlayerManager.isPreparing) {
                playingAudioMessageId = null
                audioPlaybackProgress = 0f
                audioCurrentPositionMs = 0
            }
        }
    }

    // Real-time typing indicators synced with Cloud Firestore
    var typingUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    val isOtherParticipantTyping = typingUsers.isNotEmpty()
    val typingDisplayName = remember(typingUsers, recipientUsername, conversationTitle) {
        when {
            typingUsers.isEmpty() -> recipientUsername
            typingUsers.size == 1 -> typingUsers.first()
            typingUsers.size == 2 -> "${typingUsers[0]} & ${typingUsers[1]}"
            else -> "${typingUsers.first()} & ${typingUsers.size - 1} others"
        }
    }

    // Sync local user typing status to Firestore
    LaunchedEffect(inputText, conversationId) {
        val curUserId = currentUser?.id ?: "user_me"
        val curUserName = currentUser?.displayName ?: "Alex Rivers"
        val indicatorDocId = "${conversationId}_${curUserId}"

        if (firestore != null) {
            val isTypingNow = inputText.trim().isNotEmpty()
            try {
                firestore.collection("typing_indicators").document(indicatorDocId).set(
                    hashMapOf(
                        "conversationId" to conversationId,
                        "userId" to curUserId,
                        "userName" to curUserName,
                        "isTyping" to isTypingNow,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to update typing status in Firestore: ${e.message}")
            }

            if (isTypingNow) {
                // Auto-clear typing status after 3.5 seconds of user idle
                delay(3500)
                try {
                    firestore.collection("typing_indicators").document(indicatorDocId).set(
                        hashMapOf(
                            "conversationId" to conversationId,
                            "userId" to curUserId,
                            "userName" to curUserName,
                            "isTyping" to false,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Visual media picker from device
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val msgId = "m_" + UUID.randomUUID().toString().take(8)
            val curUserId = currentUser?.id ?: "user_me"
            val curUserName = currentUser?.displayName ?: "Alex Rivers"
            val now = System.currentTimeMillis()
            val formattedTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))

            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video") || uri.toString().contains("video") || uri.toString().endsWith(".mp4")
            val mediaType = if (isVideo) "video" else "image"
            val defaultText = if (isVideo) {
                if (isBlinkMode) "⚡ Disappearing Video Snap" else "🎬 Shared Video"
            } else {
                if (isBlinkMode) "⚡ Disappearing Snap Photo" else "📸 Shared Photo"
            }

            repository?.sendMessage(
                conversationId = conversationId,
                text = defaultText,
                isBlink = isBlinkMode,
                mediaType = mediaType,
                mediaUri = uri.toString()
            )

            try {
                if (firestore != null) {
                    val messagePayload = hashMapOf(
                        "id" to msgId,
                        "conversationId" to conversationId,
                        "senderId" to curUserId,
                        "senderName" to curUserName,
                        "text" to defaultText,
                        "mediaUri" to uri.toString(),
                        "mediaType" to mediaType,
                        "isBlink" to isBlinkMode,
                        "blinkSecondsRemaining" to (if (isBlinkMode) (if (isVideo) 15 else 10) else 0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "timestamp" to now,
                        "timeFormatted" to formattedTimeStr,
                        "readAt" to null,
                        "isRead" to false
                    )
                    firestore.collection("messages").document(msgId).set(messagePayload)
                }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to send gallery media to Firestore: ${e.message}")
            }
        }
    }

    // AI summary state
    var showAiSummarySheet by remember { mutableStateOf(false) }
    var isSummarizing by remember { mutableStateOf(false) }
    var chatSummary by remember { mutableStateOf<ChatSummaryResult?>(null) }

    // Fullscreen CameraX Preview & Media Capture (Photo & Video) overlay for Chat
    if (isCameraSnapOpen) {
        CameraXPreviewCapture(
            onMediaCaptured = { uri, mediaType ->
                isCameraSnapOpen = false
                val msgId = "m_" + UUID.randomUUID().toString().take(8)
                val curUserId = currentUser?.id ?: "user_me"
                val curUserName = currentUser?.displayName ?: "Alex Rivers"
                val now = System.currentTimeMillis()
                val formattedTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))

                val isVideo = (mediaType == "video")
                val defaultText = if (isVideo) {
                    if (isBlinkMode) "⚡ Disappearing Video Snap" else "🎬 Video Snap"
                } else {
                    if (isBlinkMode) "⚡ Disappearing Snap Photo" else "📸 Photo Snap"
                }

                repository?.sendMessage(
                    conversationId = conversationId,
                    text = defaultText,
                    isBlink = isBlinkMode,
                    mediaType = mediaType,
                    mediaUri = uri.toString()
                )

                try {
                    if (firestore != null) {
                        val messagePayload = hashMapOf(
                            "id" to msgId,
                            "conversationId" to conversationId,
                            "senderId" to curUserId,
                            "senderName" to curUserName,
                            "text" to defaultText,
                            "mediaUri" to uri.toString(),
                            "mediaType" to mediaType,
                            "isBlink" to isBlinkMode,
                            "blinkSecondsRemaining" to (if (isBlinkMode) (if (isVideo) 15 else 10) else 0),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "timestamp" to now,
                            "timeFormatted" to formattedTimeStr,
                            "readAt" to null,
                            "isRead" to false
                        )
                        firestore.collection("messages").document(msgId).set(messagePayload)
                            .addOnSuccessListener {
                                isFirestoreConnected = true
                            }
                            .addOnFailureListener { e ->
                                Log.w("ChatScreen", "Firestore media write failed: ${e.message}")
                            }
                    }
                } catch (e: Exception) {
                    Log.w("ChatScreen", "Failed to send media to Firestore: ${e.message}")
                }
            },
            onClose = { isCameraSnapOpen = false },
            title = "CameraX Snap (Photo/Video)",
            showReviewStep = true
        )
        return
    }

    // Real-time Firestore Snapshot Listeners (Messages & Typing Indicators)
    DisposableEffect(conversationId, firestore) {
        var listenerRegistration: ListenerRegistration? = null
        var typingListenerRegistration: ListenerRegistration? = null
        val curUserId = currentUser?.id ?: "user_me"
        val indicatorDocId = "${conversationId}_${curUserId}"

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
                                val reactionStr = doc.getString("reaction")
                                val reactionsRaw = doc.get("reactions") as? Map<*, *>
                                val parsedReactions: Map<String, List<String>> = if (reactionsRaw != null) {
                                    reactionsRaw.mapNotNull { (k, v) ->
                                        val emojiKey = k as? String ?: return@mapNotNull null
                                        val usersList = (v as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                                        if (usersList.isNotEmpty()) emojiKey to usersList else null
                                    }.toMap()
                                } else if (!reactionStr.isNullOrBlank()) {
                                    mapOf(reactionStr to listOf(senderId))
                                } else {
                                    emptyMap()
                                }

                                val readAtTs = doc.getTimestamp("readAt")?.toDate()?.time
                                    ?: doc.getLong("readAt")
                                val isRead = (readAtTs != null) || (doc.getBoolean("isRead") ?: false)

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
                                    isFromMe = (senderId == curUserId),
                                    isRead = isRead,
                                    readAt = readAtTs,
                                    reaction = reactionStr ?: parsedReactions.keys.firstOrNull(),
                                    reactions = parsedReactions
                                )
                            }.sortedBy { it.timestamp }

                            // Update live messages list
                            firestoreMessages = items
                            isFirestoreConnected = true
                            firestoreError = null

                            // When recipient opens the chat or receives messages while viewing,
                            // automatically update 'readAt' timestamp in Firestore
                            val unreadIncomingDocs = snapshot.documents.filter { doc ->
                                val sId = doc.getString("senderId") ?: ""
                                val rAt = doc.get("readAt")
                                sId.isNotBlank() && sId != curUserId && rAt == null
                            }
                            if (unreadIncomingDocs.isNotEmpty()) {
                                try {
                                    val batch = firestore.batch()
                                    for (doc in unreadIncomingDocs) {
                                        batch.update(doc.reference, mapOf(
                                            "readAt" to FieldValue.serverTimestamp(),
                                            "isRead" to true
                                        ))
                                    }
                                    batch.commit().addOnFailureListener { e ->
                                        Log.w("ChatScreen", "Failed to mark incoming messages as read: ${e.message}")
                                    }
                                } catch (e: Exception) {
                                    Log.w("ChatScreen", "Error updating read status: ${e.message}")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to attach snapshot listener: ${e.message}")
            }

            // Real-time typing indicators listener
            try {
                typingListenerRegistration = firestore.collection("typing_indicators")
                    .whereEqualTo("conversationId", conversationId)
                    .whereEqualTo("isTyping", true)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w("ChatScreen", "Typing listener error: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val now = System.currentTimeMillis()
                            val typers = snapshot.documents.mapNotNull { doc ->
                                val uId = doc.getString("userId") ?: ""
                                val isTyping = doc.getBoolean("isTyping") ?: false
                                val updatedAt = doc.getLong("updatedAt") ?: 0L
                                val name = doc.getString("userName") ?: recipientUsername
                                // Only other participants active in the last 6 seconds
                                if (uId != curUserId && isTyping && (now - updatedAt < 6000)) {
                                    name
                                } else {
                                    null
                                }
                            }
                            typingUsers = typers
                        }
                    }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to attach typing listener: ${e.message}")
            }
        }

        onDispose {
            listenerRegistration?.remove()
            typingListenerRegistration?.remove()
            // Mark self as not typing on exit
            if (firestore != null) {
                try {
                    firestore.collection("typing_indicators").document(indicatorDocId).set(
                        hashMapOf(
                            "conversationId" to conversationId,
                            "userId" to curUserId,
                            "userName" to (currentUser?.displayName ?: "User"),
                            "isTyping" to false,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    // Unified message stream: merges Firestore documents with local state to ensure instant feedback
    val displayMessages = remember(firestoreMessages, repoMessages) {
        val map = linkedMapOf<String, ChatMessage>()
        repoMessages.forEach { map[it.id] = it }
        firestoreMessages.forEach { map[it.id] = it }
        map.values.sortedBy { it.timestamp }
    }

    // Auto-scroll to latest message or active typing indicator
    LaunchedEffect(displayMessages.size, isOtherParticipantTyping) {
        val targetIndex = displayMessages.size + (if (isOtherParticipantTyping) 1 else 0) - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
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
                        "timeFormatted" to formattedTimeStr,
                        "readAt" to null,
                        "isRead" to false
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

            // Clear self typing indicator in Firestore immediately upon sending
            try {
                val indicatorDocId = "${conversationId}_${curUserId}"
                firestore?.collection("typing_indicators")?.document(indicatorDocId)?.set(
                    hashMapOf(
                        "conversationId" to conversationId,
                        "userId" to curUserId,
                        "userName" to curUserName,
                        "isTyping" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Ignore
            }

            coroutineScope.launch {
                if (displayMessages.isNotEmpty()) {
                    listState.animateScrollToItem(displayMessages.size - 1)
                }
            }
        }
    }

    val onToggleReaction: (ChatMessage, String) -> Unit = { targetMsg, emoji ->
        val curUserId = currentUser?.id ?: "user_me"
        val currentUsers = targetMsg.reactions[emoji] ?: emptyList()
        val newUsers = if (currentUsers.contains(curUserId)) {
            currentUsers - curUserId
        } else {
            currentUsers + curUserId
        }
        val newReactions = if (newUsers.isEmpty()) {
            targetMsg.reactions - emoji
        } else {
            targetMsg.reactions + (emoji to newUsers)
        }
        val newPrimaryReaction = newReactions.keys.firstOrNull()

        // 1. Optimistic UI update on firestoreMessages
        firestoreMessages = firestoreMessages.map { msg ->
            if (msg.id == targetMsg.id) {
                msg.copy(
                    reactions = newReactions,
                    reaction = newPrimaryReaction
                )
            } else msg
        }

        // 2. Local repository update
        repository?.toggleMessageReaction(conversationId, targetMsg.id, emoji, curUserId)

        // 3. Real-time Cloud Firestore write
        if (firestore != null) {
            coroutineScope.launch {
                try {
                    firestore.collection("messages").document(targetMsg.id).set(
                        mapOf(
                            "reactions" to newReactions,
                            "reaction" to newPrimaryReaction
                        ),
                        SetOptions.merge()
                    ).addOnFailureListener { e ->
                        Log.w("ChatScreen", "Failed to update reaction in Firestore: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.w("ChatScreen", "Firestore reaction error: ${e.message}")
                }
            }
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val audioDir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
            val audioFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            val started = voiceRecorderManager.startRecording(audioFile)
            if (started) {
                isVoiceRecording = true
            }
        } else {
            Log.w("ChatScreen", "Record audio permission denied")
        }
    }

    val startVoiceRecordingAction: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val audioDir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
            val audioFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            val started = voiceRecorderManager.startRecording(audioFile)
            if (started) {
                isVoiceRecording = true
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val cancelVoiceRecordingAction: () -> Unit = {
        voiceRecorderManager.cancelRecording()
        isVoiceRecording = false
    }

    val sendVoiceRecordingAction: () -> Unit = {
        val (recordedFile, durationMs) = voiceRecorderManager.stopRecording()
        isVoiceRecording = false

        if (recordedFile != null && durationMs > 400) {
            val msgId = "m_" + UUID.randomUUID().toString().take(8)
            val curUserId = currentUser?.id ?: "user_me"
            val curUserName = currentUser?.displayName ?: "Alex Rivers"
            val now = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(now))
            val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
            val localUri = Uri.fromFile(recordedFile).toString()
            val textCaption = "🎙️ Voice note (${durationSec}s)"

            // 1. Optimistic UI update in chat
            val optimisticVoiceMsg = ChatMessage(
                id = msgId,
                conversationId = conversationId,
                senderId = curUserId,
                senderName = curUserName,
                text = textCaption,
                mediaUri = localUri,
                mediaType = "audio",
                timestamp = now,
                timeFormatted = formattedTime,
                isBlink = isBlinkMode,
                blinkSecondsRemaining = if (isBlinkMode) 15 else 0,
                isFromMe = true,
                isRead = false,
                readAt = null
            )
            firestoreMessages = (firestoreMessages + optimisticVoiceMsg).sortedBy { it.timestamp }

            // 2. Repository fallback update
            repository?.sendMessage(
                conversationId = conversationId,
                text = textCaption,
                isBlink = isBlinkMode,
                mediaType = "audio",
                mediaUri = localUri
            )

            // 3. Upload to Firebase Storage and update Firestore message
            FirebaseStorageAudioUploader.uploadVoiceClip(
                audioFile = recordedFile,
                conversationId = conversationId,
                messageId = msgId,
                onSuccess = { downloadUrl ->
                    try {
                        val payload = hashMapOf(
                            "id" to msgId,
                            "conversationId" to conversationId,
                            "senderId" to curUserId,
                            "senderName" to curUserName,
                            "text" to textCaption,
                            "mediaUri" to downloadUrl,
                            "mediaType" to "audio",
                            "isBlink" to isBlinkMode,
                            "blinkSecondsRemaining" to (if (isBlinkMode) 15 else 0),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "timestamp" to now,
                            "timeFormatted" to formattedTime,
                            "readAt" to null,
                            "isRead" to false
                        )
                        firestore?.collection("messages")?.document(msgId)?.set(payload, SetOptions.merge())
                    } catch (e: Exception) {
                        Log.w("ChatScreen", "Failed to update voice message in Firestore: ${e.message}")
                    }
                },
                onFailure = { e ->
                    Log.w("ChatScreen", "Firebase Storage voice note upload failed: ${e.message}")
                }
            )

            coroutineScope.launch {
                delay(100)
                if (firestoreMessages.isNotEmpty()) {
                    listState.animateScrollToItem(firestoreMessages.size - 1)
                }
            }
        }
    }

    val onToggleAudioPlayback: (ChatMessage) -> Unit = { message ->
        val uri = message.mediaUri
        if (!uri.isNullOrBlank()) {
            if (playingAudioMessageId == message.id) {
                if (audioPlayerManager.isPlaying) {
                    audioPlayerManager.pause()
                } else {
                    audioPlayerManager.resume()
                }
            } else {
                playingAudioMessageId = message.id
                audioPlaybackProgress = 0f
                audioCurrentPositionMs = 0
                audioPlayerManager.play(
                    uriString = uri,
                    onCompletion = {
                        playingAudioMessageId = null
                        audioPlaybackProgress = 0f
                        audioCurrentPositionMs = 0
                    },
                    onError = { err ->
                        Log.w("ChatScreen", "Playback failed: $err")
                        playingAudioMessageId = null
                        audioPlaybackProgress = 0f
                    }
                )
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
                            if (isOtherParticipantTyping) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.testTag("chat_header_typing_indicator")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(BlinkCyanLight)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$typingDisplayName is typing...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BlinkCyanLight
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isRecipientOnline) "@$recipientUsername • Active now" else "@$recipientUsername • Offline",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                                        if (prompt.contains("Send a snap")) {
                                            isCameraSnapOpen = true
                                        } else {
                                            inputText = prompt
                                        }
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

                // Real-time typing notification banner right above input field
                AnimatedVisibility(
                    visible = isOtherParticipantTyping,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                            .testTag("chat_bottom_typing_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(BlinkCyanLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$typingDisplayName is typing...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BlinkCyanLight
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (isVoiceRecording) {
                    // Voice Recording Active Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("chat_voice_recording_active_bar"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel / Trash button
                        IconButton(
                            onClick = cancelVoiceRecordingAction,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                .testTag("chat_voice_cancel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Cancel recording",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Waveform and recording duration indicator
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Pulsing red dot & timer
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val pulseAlpha by rememberInfiniteTransition(label = "rec_pulse").animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "rec_pulse_alpha"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .alpha(pulseAlpha)
                                            .background(Color.Red)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val totalSeconds = (voiceRecordingTicks * 200) / 1000
                                    Text(
                                        text = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Undulating mini waveform
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    val normalizedAmp = (voiceAmplitude / 1200).coerceIn(1, 8)
                                    for (i in 0 until 12) {
                                        val barHeight = ((i % 4 + 2) * normalizedAmp).coerceIn(4, 18)
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(barHeight.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(
                                                    if (i % 2 == 0) BlinkCyanLight else BlinkPrimaryLight
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Send Voice Clip button
                        IconButton(
                            onClick = sendVoiceRecordingAction,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan)))
                                .testTag("chat_voice_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Voice Clip",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
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

                        Spacer(modifier = Modifier.width(6.dp))

                        // Camera Snap button (CameraX Preview & Snap)
                        IconButton(
                            onClick = { isCameraSnapOpen = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("chat_camera_snap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Snap Photo or Video",
                                tint = BlinkCyanLight,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Gallery Picker button (Pick Photo or Video)
                        IconButton(
                            onClick = {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("chat_gallery_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Pick Media from Gallery",
                                tint = BlinkPrimaryLight,
                                modifier = Modifier.size(19.dp)
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

                        // Send or Mic Record Button
                        val canSend = inputText.trim().isNotBlank()
                        if (canSend) {
                            val sendScale by animateFloatAsState(
                                targetValue = 1.05f,
                                label = "send_scale"
                            )
                            IconButton(
                                onClick = onSendAction,
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(sendScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isBlinkMode) Brush.linearGradient(listOf(BlinkPink, BlinkAmber))
                                        else Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                                    )
                                    .testTag("chat_send_button")
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = startVoiceRecordingAction,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                                    )
                                    .testTag("chat_voice_record_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
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

                        val curUserId = currentUser?.id ?: "user_me"
                        ChatMessageBubble(
                            message = message,
                            recipientAvatarColor = recipientAvatarColorHex,
                            recipientName = conversationTitle,
                            currentUserId = curUserId,
                            currentlyPlayingId = playingAudioMessageId,
                            isAudioPlaying = audioPlayerManager.isPlaying,
                            playbackProgress = audioPlaybackProgress,
                            currentPositionMs = audioCurrentPositionMs,
                            onMediaClick = { clickedMsg ->
                                selectedMediaForFullscreen = clickedMsg
                            },
                            onToggleAudioPlayback = onToggleAudioPlayback,
                            onReactionClick = { emoji ->
                                onToggleReaction(message, emoji)
                            },
                            onLongClick = {
                                selectedMessageForReaction = message
                            }
                        )
                    }

                    // Real-Time Animated Typing Indicator Bubble inside message stream
                    if (isOtherParticipantTyping) {
                        item(key = "realtime_typing_indicator_bubble") {
                            TypingIndicatorBubble(
                                recipientName = typingDisplayName,
                                recipientAvatarColor = recipientAvatarColorHex
                            )
                        }
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

    // Fullscreen Media Viewer Dialog (Photos and Videos from Firestore / CameraX)
    if (selectedMediaForFullscreen != null) {
        FullscreenMediaViewer(
            message = selectedMediaForFullscreen!!,
            onDismiss = { selectedMediaForFullscreen = null }
        )
    }

    // Real-Time Message Reaction Picker Dialog
    if (selectedMessageForReaction != null) {
        val curUserId = currentUser?.id ?: "user_me"
        MessageReactionPickerDialog(
            message = selectedMessageForReaction!!,
            currentUserId = curUserId,
            onDismiss = { selectedMessageForReaction = null },
            onReactionSelected = { emoji ->
                onToggleReaction(selectedMessageForReaction!!, emoji)
                selectedMessageForReaction = null
            }
        )
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
 * Message bubble item rendering distinct styles for outgoing (from me) vs incoming messages,
 * supporting text, photos, and video attachments stored in Cloud Firestore.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    recipientAvatarColor: String,
    recipientName: String,
    currentUserId: String = "user_me",
    currentlyPlayingId: String? = null,
    isAudioPlaying: Boolean = false,
    playbackProgress: Float = 0f,
    currentPositionMs: Int = 0,
    onMediaClick: ((ChatMessage) -> Unit)? = null,
    onToggleAudioPlayback: ((ChatMessage) -> Unit)? = null,
    onReactionClick: ((String) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isFromMe = message.isFromMe
    val haptics = LocalHapticFeedback.current

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

            Column(
                horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
            ) {
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
                        .combinedClickable(
                            onClick = {
                                if (!message.mediaUri.isNullOrBlank()) {
                                    onMediaClick?.invoke(message)
                                }
                            },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick?.invoke()
                            }
                        )
                ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (!message.mediaUri.isNullOrBlank()) 6.dp else 14.dp,
                        vertical = if (!message.mediaUri.isNullOrBlank()) 6.dp else 9.dp
                    )
                ) {
                    // Ephemeral Blink Header
                    if (message.isBlink) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = if (isFromMe) Color.White else BlinkPink,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Blink Disappearing Media",
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
                            color = BlinkCyanLight,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Media Attachment from CameraX / Firestore / Voice Notes
                    if (!message.mediaUri.isNullOrBlank()) {
                        val isAudio = message.mediaType.equals("audio", ignoreCase = true)
                        val isVideo = message.mediaType.equals("video", ignoreCase = true)

                        if (isAudio) {
                            // Voice Note Audio Player
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isFromMe) Color.White.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFromMe) Color.White.copy(alpha = 0.3f)
                                    else BlinkCyanLight.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("voice_note_bubble_${message.id}")
                                    .testTag("audio_message_bubble_${message.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isThisPlaying = (currentlyPlayingId == message.id && isAudioPlaying)
                                    IconButton(
                                        onClick = { onToggleAudioPlayback?.invoke(message) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isThisPlaying) BlinkCyan
                                                else if (isFromMe) Color.White
                                                else BlinkPrimary
                                            )
                                            .testTag("voice_play_pause_${message.id}")
                                            .testTag("audio_playback_button_${message.id}")
                                            .testTag("audio_playback_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isThisPlaying) "Pause audio message" else "Play audio message",
                                            tint = if (isThisPlaying) Color.Black else if (isFromMe) BlinkPrimary else Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        // Visual Waveform Bars
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            val barHeights = listOf(
                                                8, 14, 20, 10, 16, 22, 12, 18, 14, 20, 10, 22, 16, 12, 18, 14, 20
                                            )
                                            val progress = if (currentlyPlayingId == message.id) playbackProgress else 0f
                                            barHeights.forEachIndexed { index, heightDp ->
                                                val barFraction = index.toFloat() / barHeights.size.toFloat()
                                                val isPlayed = barFraction <= progress
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(heightDp.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(
                                                            if (isPlayed) (if (isFromMe) Color.White else BlinkCyanLight)
                                                            else (if (isFromMe) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (currentlyPlayingId == message.id && currentPositionMs > 0) {
                                                    val sec = (currentPositionMs / 1000) % 60
                                                    val min = (currentPositionMs / 1000) / 60
                                                    String.format("%d:%02d", min, sec)
                                                } else {
                                                    "Voice Note"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isFromMe) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = if (isFromMe) Color.White.copy(alpha = 0.7f) else BlinkCyanLight,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Audio",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFromMe) Color.White.copy(alpha = 0.7f) else BlinkCyanLight
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Photo or Video Media Attachment from CameraX / Firestore
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .combinedClickable(
                                        onClick = { onMediaClick?.invoke(message) },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongClick?.invoke()
                                        }
                                    )
                                    .padding(bottom = 4.dp)
                            ) {
                                AsyncImage(
                                    model = message.mediaUri,
                                    contentDescription = if (isVideo) "Video clip" else "Photo snap",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .testTag(if (isVideo) "chat_message_video_${message.id}" else "chat_message_photo_${message.id}")
                                )

                                // Video Overlay: Play button in center
                                if (isVideo) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(48.dp),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.65f),
                                            border = BorderStroke(1.5.dp, BlinkCyanLight)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Video",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Video tag in top-left
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BlinkPink.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "🎬 VIDEO",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    // Photo tag in top-left
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BlinkPrimary.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "📸 PHOTO",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Fullscreen icon in bottom-right
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "View Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Message text caption body
                    if (message.text.isNotBlank() && (!message.mediaType.equals("audio", ignoreCase = true) || !message.text.startsWith("🎙️ Voice note"))) {
                        Text(
                            text = message.text,
                            color = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(horizontal = if (!message.mediaUri.isNullOrBlank()) 6.dp else 0.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

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
                            .padding(horizontal = if (!message.mediaUri.isNullOrBlank()) 6.dp else 0.dp)
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
                            val isMessageRead = (message.readAt != null) || message.isRead
                            val readTimeFormatted = remember(message.readAt) {
                                message.readAt?.let {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .testTag("chat_message_read_status_${message.id}")
                                    .testTag("read_status_${message.id}")
                            ) {
                                if (isMessageRead) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read at ${readTimeFormatted ?: displayTime}",
                                        tint = BlinkCyanLight,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (readTimeFormatted != null) "Read $readTimeFormatted" else "Read",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BlinkCyanLight,
                                        modifier = Modifier.testTag("read_status_text_${message.id}")
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Sent",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Sent",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.testTag("read_status_text_${message.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time reactions row attached to this message
            val activeReactions = message.reactions.filterValues { it.isNotEmpty() }
            if (activeReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .padding(
                            start = if (isFromMe) 0.dp else 4.dp,
                            end = if (isFromMe) 4.dp else 0.dp
                        )
                        .testTag("chat_message_reactions_${message.id}"),
                    horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeReactions.forEach { (emoji, reactors) ->
                        val hasUserReacted = reactors.contains(currentUserId)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasUserReacted) BlinkPrimary.copy(alpha = 0.28f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (hasUserReacted) BlinkCyanLight.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onReactionClick?.invoke(emoji) }
                                .testTag("reaction_chip_${message.id}_${emoji}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 12.sp
                                )
                                if (reactors.size > 1) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${reactors.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasUserReacted) BlinkCyanLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Small "+" add reaction pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable { onLongClick?.invoke() }
                            .testTag("add_reaction_chip_${message.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add reaction",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Fullscreen viewer dialog for photos and videos shared in Chat,
 * supporting interactive pinch-to-zoom, pan, double-tap, and playback.
 */
@Composable
fun FullscreenChatMediaDialog(
    message: ChatMessage,
    onDismiss: () -> Unit
) {
    FullscreenMediaViewer(
        message = message,
        onDismiss = onDismiss
    )
}

/**
 * Polished animated 3-dot typing indicator bubble showing when another participant is writing,
 * synchronized in real-time through Cloud Firestore.
 */
@Composable
fun TypingIndicatorBubble(
    recipientName: String,
    recipientAvatarColor: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots_transition")

    // Staggered vertical bouncing animations for the three dots
    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1_offset"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2_offset"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, delayMillis = 280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3_offset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("chat_typing_indicator"),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Start
    ) {
        AvatarBadge(
            name = recipientName,
            colorHex = recipientAvatarColor,
            size = 28.dp,
            modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
        )

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 4.dp,
                bottomEnd = 18.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, BlinkCyanLight.copy(alpha = 0.25f)),
            modifier = Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Three animated bouncing dots
                Box(
                    modifier = Modifier
                        .offset(y = dot1Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BlinkCyanLight)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot2Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BlinkPrimaryLight)
                )
                Box(
                    modifier = Modifier
                        .offset(y = dot3Offset.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BlinkPinkLight)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "$recipientName is typing...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Modern floating reaction picker dialog allowing users to pick an emoji
 * or toggle an existing reaction in real time via Cloud Firestore.
 */
@Composable
fun MessageReactionPickerDialog(
    message: ChatMessage,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    var showAllEmojis by remember { mutableStateOf(false) }
    val quickEmojis = listOf("❤️", "🔥", "😂", "👍", "😮", "😢", "⚡", "🎉")
    val extendedEmojis = listOf(
        "✨", "🙌", "💯", "🚀", "👀", "👏", "😎", "💡",
        "💔", "🥳", "🙏", "🤩", "🎯", "💀", "🫡", "🤝"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clickable(enabled = false) {}
                    .testTag("reaction_picker_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, BlinkCyanLight.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEmotions,
                                contentDescription = null,
                                tint = BlinkCyanLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "React with Emoji",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Message snippet preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        val previewText = when {
                            message.text.isNotBlank() -> message.text
                            message.mediaType == "video" -> "🎬 Video clip"
                            message.mediaType == "photo" -> "📸 Photo snap"
                            else -> "Message"
                        }
                        Text(
                            text = previewText,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Quick Emojis Grid/Row
                    Text(
                        text = "Quick Reactions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickEmojis.forEach { emoji ->
                            val isReactedByMe = message.reactions[emoji]?.contains(currentUserId) == true
                            Surface(
                                shape = CircleShape,
                                color = if (isReactedByMe) BlinkPrimary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isReactedByMe) BorderStroke(1.5.dp, BlinkCyanLight) else null,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onReactionSelected(emoji) }
                                    .testTag("reaction_picker_emoji_$emoji")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Expandable Extra Emojis
                    AnimatedVisibility(visible = showAllEmojis) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "More Emojis",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // 8 columns grid of extended emojis
                            val chunked = extendedEmojis.chunked(8)
                            chunked.forEach { rowEmojis ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowEmojis.forEach { emoji ->
                                        val isReactedByMe = message.reactions[emoji]?.contains(currentUserId) == true
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isReactedByMe) BlinkPrimary.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            border = if (isReactedByMe) BorderStroke(1.5.dp, BlinkCyanLight) else null,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable { onReactionSelected(emoji) }
                                                .testTag("reaction_picker_emoji_$emoji")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = emoji,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Toggle More Button
                    TextButton(
                        onClick = { showAllEmojis = !showAllEmojis },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("reaction_picker_more_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showAllEmojis) "Show Less" else "+ More Emojis",
                                color = BlinkPrimaryLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
