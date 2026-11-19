package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central Firebase Realtime Database configuration & schema manager for Blinkchat.
 * Connects to https://blinkchat-8d1a7-default-rtdb.europe-west1.firebasedatabase.app
 * using a dedicated FirebaseApp instance with disk persistence enabled so all
 * messages, chats, users, blinks, and reports persist and sync in real time across devices.
 */
object FirebaseRtdbManager {
    private const val TAG = "FirebaseRtdbManager"
    private const val RTDB_APP_NAME = "blinkchat_rtdb"

    // User-provided Firebase Realtime Database configuration
    const val API_KEY = "AIzaSyAPGewG6btum9kZWVB7KIBFa50nVg3PLaI"
    const val AUTH_DOMAIN = "blinkchat-8d1a7.firebaseapp.com"
    const val DATABASE_URL = "https://blinkchat-8d1a7-default-rtdb.europe-west1.firebasedatabase.app"
    const val PROJECT_ID = "blinkchat-8d1a7"
    const val STORAGE_BUCKET = "blinkchat-8d1a7.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "196179475252"
    const val APP_ID = "1:196179475252:web:3f90f8850097e26b8f8361"

    // Requested top-level Realtime Database nodes
    const val NODE_USERS = "users"
    const val NODE_CHATS = "chats"
    const val NODE_MESSAGES = "messages"
    const val NODE_BLINKS = "blinks"
    const val NODE_REPORTS = "reports"

    @Volatile
    private var cachedDatabase: FirebaseDatabase? = null

    @Volatile
    private var persistenceInitialized = false

    fun getDatabase(context: Context?): FirebaseDatabase? {
        cachedDatabase?.let { return it }
        if (context == null) return null

        return synchronized(this) {
            cachedDatabase?.let { return@synchronized it }
            try {
                val appContext = context.applicationContext ?: context
                val existingApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == RTDB_APP_NAME }
                val rtdbApp = existingApp ?: run {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(API_KEY)
                        .setApplicationId(APP_ID)
                        .setDatabaseUrl(DATABASE_URL)
                        .setProjectId(PROJECT_ID)
                        .setStorageBucket(STORAGE_BUCKET)
                        .setGcmSenderId(MESSAGING_SENDER_ID)
                        .build()
                    FirebaseApp.initializeApp(appContext, options, RTDB_APP_NAME)
                }

                val db = FirebaseDatabase.getInstance(rtdbApp, DATABASE_URL)
                if (!persistenceInitialized) {
                    try {
                        db.setPersistenceEnabled(true)
                        persistenceInitialized = true
                    } catch (e: Exception) {
                        // Safe guard: ignore if persistence was already set or unsupported in test runner
                        Log.w(TAG, "setPersistenceEnabled notice: ${e.message}")
                    }
                }
                // Keep core top-level paths synced locally for offline durability & fast two-phone updates
                try {
                    db.getReference(NODE_CHATS).keepSynced(true)
                    db.getReference(NODE_MESSAGES).keepSynced(true)
                    db.getReference(NODE_USERS).keepSynced(true)
                } catch (_: Exception) {}

                cachedDatabase = db
                db
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize Firebase Realtime Database: ${e.message}")
                null
            }
        }
    }

    fun usersRef(db: FirebaseDatabase): DatabaseReference = db.getReference(NODE_USERS)
    fun chatsRef(db: FirebaseDatabase): DatabaseReference = db.getReference(NODE_CHATS)
    fun messagesRef(db: FirebaseDatabase): DatabaseReference = db.getReference(NODE_MESSAGES)
    fun chatMessagesRef(db: FirebaseDatabase, chatId: String): DatabaseReference =
        db.getReference(NODE_MESSAGES).child(sanitizeKey(chatId))
    fun blinksRef(db: FirebaseDatabase): DatabaseReference = db.getReference(NODE_BLINKS)
    fun reportsRef(db: FirebaseDatabase): DatabaseReference = db.getReference(NODE_REPORTS)

    fun sanitizeKey(rawKey: String): String {
        val cleaned = rawKey.trim().replace(Regex("[.#$\\[\\]/\\s]"), "_")
        return cleaned.ifBlank { "default_key" }
    }

    /**
     * Generates a deterministic, canonical conversation ID for a 1:1 direct chat
     * so two separate phones always read and write to the exact same RTDB chat & messages path.
     */
    fun canonicalDirectChatId(userIdA: String, userIdB: String): String {
        val a = sanitizeKey(userIdA)
        val b = sanitizeKey(userIdB)
        return if (a <= b) "dm_${a}_$b" else "dm_${b}_$a"
    }

    fun userToMap(user: User): Map<String, Any?> {
        return mapOf(
            "id" to user.id,
            "username" to user.username,
            "displayName" to user.displayName,
            "email" to user.email,
            "avatarColorHex" to user.avatarColorHex,
            "bio" to user.bio,
            "statusText" to user.statusText,
            "location" to user.location,
            "coordinates" to user.coordinates,
            "isOnline" to user.isOnline,
            "isGhostMode" to user.isGhostMode,
            "streaksCount" to user.streaksCount,
            "postsCount" to user.postsCount,
            "friendsCount" to user.friendsCount,
            "followersCount" to user.followersCount,
            "followingCount" to user.followingCount,
            "lastSeen" to ServerValue.TIMESTAMP
        )
    }

    fun parseUserSnapshot(snapshot: DataSnapshot): User? {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return null
        val username = snapshot.child("username").getValue(String::class.java) ?: return null
        val displayName = snapshot.child("displayName").getValue(String::class.java) ?: username
        return User(
            id = id,
            username = username,
            displayName = displayName,
            email = snapshot.child("email").getValue(String::class.java) ?: "$username@blinkchat.com",
            avatarColorHex = snapshot.child("avatarColorHex").getValue(String::class.java) ?: "#8B5CF6",
            bio = snapshot.child("bio").getValue(String::class.java) ?: "Living in the blink of an eye ✨",
            streaksCount = (snapshot.child("streaksCount").value as? Number)?.toInt() ?: 0,
            postsCount = (snapshot.child("postsCount").value as? Number)?.toInt() ?: 0,
            friendsCount = (snapshot.child("friendsCount").value as? Number)?.toInt() ?: 0,
            followersCount = (snapshot.child("followersCount").value as? Number)?.toInt() ?: 0,
            followingCount = (snapshot.child("followingCount").value as? Number)?.toInt() ?: 0,
            statusText = snapshot.child("statusText").getValue(String::class.java) ?: "Available ⚡",
            location = snapshot.child("location").getValue(String::class.java) ?: "Worldwide",
            coordinates = snapshot.child("coordinates").getValue(String::class.java) ?: "",
            isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: true,
            isGhostMode = snapshot.child("isGhostMode").getValue(Boolean::class.java) ?: false
        )
    }

    fun chatToMap(
        conversation: Conversation,
        participantIds: List<String> = emptyList(),
        participantInfo: Map<String, Map<String, Any?>> = emptyMap(),
        lastSenderId: String? = null
    ): Map<String, Any?> {
        val participantsMap = linkedMapOf<String, Boolean>()
        participantIds.filter { it.isNotBlank() }.forEach { participantsMap[sanitizeKey(it)] = true }
        if (conversation.participantId.isNotBlank()) {
            participantsMap[sanitizeKey(conversation.participantId)] = true
        }
        return mapOf(
            "id" to conversation.id,
            "participantId" to conversation.participantId,
            "name" to conversation.name,
            "username" to conversation.username,
            "avatarColorHex" to conversation.avatarColorHex,
            "lastMessage" to conversation.lastMessage,
            "lastMessageTime" to conversation.lastMessageTime,
            "lastSenderId" to (lastSenderId ?: ""),
            "isGroup" to conversation.isGroup,
            "groupTitle" to conversation.groupTitle,
            "groupMembers" to conversation.groupMembers,
            "memberNames" to conversation.memberNames,
            "participants" to participantsMap,
            "participantProfiles" to participantInfo,
            "isOnline" to conversation.isOnline,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    fun messageToMap(
        msgId: String,
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        mediaUri: String?,
        mediaType: String,
        isBlink: Boolean,
        blinkSecondsRemaining: Int,
        timestamp: Long,
        timeFormatted: String,
        isRead: Boolean = false,
        readAt: Long? = null,
        reactions: Map<String, List<String>> = emptyMap(),
        reaction: String? = null
    ): Map<String, Any?> {
        return hashMapOf(
            "id" to msgId,
            "conversationId" to conversationId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "mediaUri" to mediaUri,
            "mediaType" to mediaType,
            "isBlink" to isBlink,
            "blinkSecondsRemaining" to blinkSecondsRemaining,
            "timestamp" to timestamp,
            "createdAt" to ServerValue.TIMESTAMP,
            "timeFormatted" to timeFormatted,
            "readAt" to readAt,
            "isRead" to isRead,
            "reactions" to reactions,
            "reaction" to reaction
        )
    }

    fun parseMessageSnapshot(
        snapshot: DataSnapshot,
        conversationId: String,
        currentUserId: String
    ): ChatMessage? {
        val id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: return null
        val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""
        val senderName = snapshot.child("senderName").getValue(String::class.java) ?: ""
        val text = snapshot.child("text").getValue(String::class.java) ?: ""
        val mediaUri = snapshot.child("mediaUri").getValue(String::class.java)?.takeIf { it.isNotBlank() }
        val mediaType = snapshot.child("mediaType").getValue(String::class.java) ?: "none"
        val isBlink = snapshot.child("isBlink").getValue(Boolean::class.java) ?: false
        val blinkSeconds = (snapshot.child("blinkSecondsRemaining").value as? Number)?.toInt() ?: if (isBlink) 10 else 0
        val ts = (snapshot.child("timestamp").value as? Number)?.toLong()
            ?: (snapshot.child("createdAt").value as? Number)?.toLong()
            ?: System.currentTimeMillis()

        val reactionStr = snapshot.child("reaction").getValue(String::class.java)
        val parsedReactions = mutableMapOf<String, List<String>>()
        val reactionsSnap = snapshot.child("reactions")
        if (reactionsSnap.exists()) {
            for (emojiChild in reactionsSnap.children) {
                val emojiKey = emojiChild.key ?: continue
                val users = emojiChild.children.mapNotNull { it.getValue(String::class.java) }
                if (users.isNotEmpty()) {
                    parsedReactions[emojiKey] = users
                }
            }
        } else if (!reactionStr.isNullOrBlank() && senderId.isNotBlank()) {
            parsedReactions[reactionStr] = listOf(senderId)
        }

        val readAtTs = (snapshot.child("readAt").value as? Number)?.toLong()
        val isRead = (readAtTs != null) || (snapshot.child("isRead").getValue(Boolean::class.java) ?: false)
        val timeFmt = snapshot.child("timeFormatted").getValue(String::class.java)
            ?: SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))

        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = ts,
            timeFormatted = timeFmt,
            isBlink = isBlink,
            blinkSecondsRemaining = blinkSeconds,
            isFromMe = (senderId == currentUserId),
            isRead = isRead,
            readAt = readAtTs,
            reaction = reactionStr ?: parsedReactions.keys.firstOrNull(),
            reactions = parsedReactions
        )
    }
}
