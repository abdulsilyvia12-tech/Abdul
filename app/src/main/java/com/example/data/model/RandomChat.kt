package com.example.data.model

data class RandomMatchQueueEntry(
    val id: String = "",
    val anonymousId: String = "",
    val anonymousName: String = "",
    val anonymousAvatarColor: String = "#8B5CF6",
    val status: String = "waiting", // waiting, matched, cancelled
    val matchedChatId: String? = null,
    val peerAnonymousName: String? = null,
    val peerAvatarColor: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
)

data class ActiveRandomChat(
    val id: String = "",
    val participantA: String = "",
    val participantB: String = "",
    val participantAName: String = "Anonymous Blink",
    val participantBName: String = "Cyber Partner",
    val participantAAvatar: String = "#EC4899",
    val participantBAvatar: String = "#06B6D4",
    val status: String = "active", // active, skipped, ended, reported
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 120_000L, // 2 minutes (120 seconds)
    val lastMessage: String = ""
)

data class RandomChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderAnonymousName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false
)

enum class MatchState {
    IDLE,
    SEARCHING,
    MATCHED
}
