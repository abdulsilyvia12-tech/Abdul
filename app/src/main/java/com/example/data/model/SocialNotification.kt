package com.example.data.model

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    MENTION,
    STREAK_ALERT
}

data class SocialNotification(
    val id: String,
    val type: NotificationType,
    val actorName: String,
    val actorUsername: String,
    val actorAvatarColor: String = "#8B5CF6",
    val title: String,
    val description: String,
    val timeAgo: String,
    val isRead: Boolean = false,
    val targetPostId: String? = null,
    val targetVideoId: String? = null,
    val isFollowedBack: Boolean = false
)
