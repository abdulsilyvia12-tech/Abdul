package com.example.data.model

import androidx.annotation.DrawableRes

enum class VideoCategory {
    ALL,
    PHONK, // Drift phonk, Brazilian phonk, bass-boosted gym edits
    DRAMATIC, // Heavy suspense, cinematic drama, intense soundtracks
    FUNNY, // Comedy sketches, laugh-out-loud pranks, funny faces
    MUSIC, // High-energy beat drops, dance, acoustic jams
    BONGO, // Bongo Flava, Afrobeats & tropical rhythms
    BLINKS, // Short, energetic, AI-animated disappearing blinks
    COMMUNITY // User uploaded/recorded creator videos
}

data class VideoComment(
    val id: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarColor: String,
    val text: String,
    val timestampFormatted: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

data class BlinkVideo(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarColor: String = "#8B5CF6",
    val title: String,
    val caption: String,
    val hashtags: List<String> = emptyList(),
    val musicTrack: String = "Original Audio - Blinkchat",
    val category: VideoCategory = VideoCategory.COMMUNITY,
    val isBlink: Boolean = true, // Whether this is an ephemeral/AI blink video
    val durationSeconds: Int = 15,
    val likesCount: Int = 120,
    val commentsCount: Int = 18,
    val sharesCount: Int = 24,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val filterName: String? = null,
    val isAiGenerated: Boolean = false,
    val aiPrompt: String? = null,
    @DrawableRes val thumbnailRes: Int? = null,
    val videoGradientColors: List<Long> = listOf(0xFF1E1B4B, 0xFF4338CA, 0xFF06B6D4),
    val comments: List<VideoComment> = emptyList(),
    val createdAtFormatted: String = "Just now"
)
