package com.example.data.model

import androidx.compose.ui.graphics.Color

data class PrivacySettings(
    val ghostMode: Boolean = false,
    val storyAudience: String = "Everyone", // Everyone, Friends, Close Friends
    val whoCanMessage: String = "Everyone", // Everyone, Friends Only
    val whoCanCall: String = "Everyone", // Everyone, Friends Only
    val readReceipts: Boolean = true,
    val activeStatus: Boolean = true
)

enum class ReportReason(val displayName: String, val description: String) {
    HARASSMENT("Harassment or Bullying", "Threats, insulting behavior, or stalking"),
    SPAM("Spam or Scams", "Unsolicited promotional content or deceptive links"),
    HATE_SPEECH("Hate Speech or Symbols", "Attack on identity, race, religion, or orientation"),
    INAPPROPRIATE("Inappropriate Content", "Explicit imagery, gore, or graphic media"),
    IMPERSONATION("Impersonation", "Pretending to be another person or brand"),
    OTHER("Other Violation", "Any other Blinkchat community guidelines breach")
}

data class SafetyReport(
    val id: String,
    val reporterId: String,
    val targetUserId: String?,
    val targetPostId: String?,
    val reason: ReportReason,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BlockedUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarColorHex: String = "#8B5CF6",
    val blockedAt: Long = System.currentTimeMillis()
)

enum class FilterCategory(val label: String, val icon: String) {
    ALL("All", "⚡"),
    LENSES("Snap Lenses", "✨"),
    BEAUTY("Beauty & Face", "💄"),
    SHADERS("GPU Shaders", "🌈"),
    FUNNY_FACES("Funny Faces", "🤪"),
    GAMES("Funny Games", "🎮"),
    AESTHETIC("Aesthetic", "🔮"),
    CYBER("Cyber & Retro", "🤖")
}

data class CreativeFilter(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val tintColor: Color,
    val vignetteAlpha: Float = 0.2f,
    val saturationMultiplier: Float = 1.0f,
    val contrastMultiplier: Float = 1.0f,
    val isGlitch: Boolean = false,
    val hasParticles: Boolean = false,
    val scanlines: Boolean = false,
    val category: FilterCategory = FilterCategory.ALL,
    val funnyFaceType: String = "",
    val gameType: String = "",
    val overlayTitle: String = "",
    val shaderType: com.example.ui.camera.filters.CameraShaderType = com.example.ui.camera.filters.CameraShaderType.NONE,
    val defaultIntensity: Float = 0.85f,
    val arLensType: String = "",
    val beautySmoothing: Float = 0f,
    val blushIntensity: Float = 0f,
    val hasSparkles: Boolean = false,
    val isLense: Boolean = false
)
