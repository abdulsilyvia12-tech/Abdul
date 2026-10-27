package com.example.data.model

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val mediaUri: String? = null,
    val mediaType: String = "none", // none, photo, video, audio
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "12:00 PM",
    val isBlink: Boolean = false, // disappearing message
    val blinkSecondsRemaining: Int = 10,
    val isRead: Boolean = true,
    val isExpired: Boolean = false,
    val isFromMe: Boolean = false,
    val senderName: String = "",
    val reaction: String? = null
)

data class Conversation(
    val id: String,
    val participantId: String,
    val name: String,
    val username: String,
    val avatarColorHex: String = "#8B5CF6",
    val lastMessage: String = "",
    val lastMessageTime: String = "Just now",
    val unreadCount: Int = 0,
    val isAiBot: Boolean = false,
    val streak: Int = 0,
    val isOnline: Boolean = false,
    val isBlinkActive: Boolean = false,
    val isGroup: Boolean = false,
    val groupTitle: String? = null,
    val groupMembers: List<String> = emptyList(),
    val memberNames: List<String> = emptyList()
)
