package com.example.data.model

data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarColor: String = "#8B5CF6",
    val imageDrawableRes: Int? = null,
    val imageUri: String? = null,
    val caption: String,
    val filterTag: String = "Normal",
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val audience: String = "Everyone", // Everyone, Friends, Close Friends
    val reactions: Map<String, Int> = emptyMap(),
    val userReaction: String? = null,
    val sharesCount: Int = 0,
    val isVerified: Boolean = false
)

data class Story(
    val id: String,
    val userId: String,
    val username: String,
    val userDisplayName: String,
    val userAvatarColor: String = "#8B5CF6",
    val imageDrawableRes: Int? = null,
    val imageUri: String? = null,
    val caption: String = "",
    val filterTag: String = "Normal",
    val timestamp: Long = System.currentTimeMillis(),
    val isViewed: Boolean = false
)

data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarColor: String = "#8B5CF6",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "2m",
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)
