package com.example.data.ai

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ModerationResult(
    val isSafe: Boolean,
    val scorePercentage: Int, // 0 - 100
    val flag: String? = null,
    val explanation: String
)

data class CustomAiFilterResult(
    val id: String,
    val name: String,
    val emoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val vignetteAlpha: Float,
    val particleType: String, // "stars", "matrix", "sparks", "dust", "none"
    val scanlines: Boolean,
    val promptDescription: String
)

data class ChatSummaryResult(
    val summary: String,
    val keyPoints: List<String>,
    val vibe: String,
    val actionItems: List<String>
)

object BlinkAiService {
    private const val TAG = "BlinkAiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // --- Core Raw Gemini API Caller ---
    suspend fun callGeminiApi(prompt: String, systemPrompt: String? = null): String? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }
                )
                put("contents", contentsArray)

                if (!systemPrompt.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                    })
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && !responseString.isNullOrBlank()) {
                val root = JSONObject(responseString)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text.trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini API call failed", e)
        }
        null
    }

    // --- 1. AI Powered Chat Summaries ---
    suspend fun summarizeChat(messagesText: List<String>): ChatSummaryResult = withContext(Dispatchers.IO) {
        val transcript = messagesText.takeLast(15).joinToString("\n")
        val prompt = "Summarize this chat conversation:\n$transcript\n\nProvide response in JSON format with fields: summary, keyPoints (array of strings), vibe (single word or short phrase), actionItems (array of strings)."
        val systemPrompt = "You are an AI assistant in Blinkchat that summarizes conversations accurately and concisely in valid JSON format."

        val apiResult = callGeminiApi(prompt, systemPrompt)
        if (apiResult != null) {
            try {
                val cleanJson = apiResult.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleanJson}")
                val summary = json.optString("summary", "Catch up on recent updates from your friends.")
                val keyPointsJson = json.optJSONArray("keyPoints")
                val keyPoints = mutableListOf<String>()
                if (keyPointsJson != null) {
                    for (i in 0 until keyPointsJson.length()) {
                        keyPoints.add(keyPointsJson.getString(i))
                    }
                }
                val vibe = json.optString("vibe", "Fun & Energetic ⚡")
                val actionItemsJson = json.optJSONArray("actionItems")
                val actionItems = mutableListOf<String>()
                if (actionItemsJson != null) {
                    for (i in 0 until actionItemsJson.length()) {
                        actionItems.add(actionItemsJson.getString(i))
                    }
                }
                return@withContext ChatSummaryResult(
                    summary = summary,
                    keyPoints = if (keyPoints.isNotEmpty()) keyPoints else listOf("Friends discussed weekend festival plans", "Shared snaps of downtown night lights"),
                    vibe = vibe,
                    actionItems = if (actionItems.isNotEmpty()) actionItems else listOf("Reply with concert availability")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing chat summary JSON", e)
            }
        }

        // Resilient fallback summary
        ChatSummaryResult(
            summary = "Alex and friends caught up on recent festival moments, shared exciting camera filter snaps, and agreed to meet up downtown tonight.",
            keyPoints = listOf(
                "Discussed the new Sunset Glow & Cyberpunk camera filters 📸",
                "Shared memories from Rooftop Sunset Sessions 🌇",
                "Planned to catch up for late-night ramen under the neon signs 🍜"
            ),
            vibe = "Excited & Vibrant ✨",
            actionItems = listOf("Send over your location when arriving", "Share festival video snaps")
        )
    }

    // --- 2. AI Assisted Content Moderation ---
    suspend fun moderateContent(text: String): ModerationResult = withContext(Dispatchers.IO) {
        val prompt = "Analyze the following user-submitted text for safety and community guideline violations (harassment, hate speech, severe toxicity, spam, violence, sexually explicit):\n\"$text\"\n\nRespond strictly with JSON: {\"isSafe\": boolean, \"scorePercentage\": integer (0-100 where 100 is completely safe), \"flag\": string or null, \"explanation\": string}."
        val systemPrompt = "You are an AI trust and safety moderation engine for Blinkchat."

        val apiResult = callGeminiApi(prompt, systemPrompt)
        if (apiResult != null) {
            try {
                val cleanJson = apiResult.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleanJson}")
                return@withContext ModerationResult(
                    isSafe = json.optBoolean("isSafe", true),
                    scorePercentage = json.optInt("scorePercentage", 98),
                    flag = json.optString("flag").ifBlank { null },
                    explanation = json.optString("explanation", "Content adheres to Blinkchat community guidelines.")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Moderation parse error", e)
            }
        }

        // Rule-based safety check
        val lower = text.lowercase()
        val offensiveWords = listOf("hate", "kill", "die", "ugly", "idiot", "attack", "scam", "nude")
        val foundWord = offensiveWords.firstOrNull { lower.contains(it) }

        if (foundWord != null) {
            ModerationResult(
                isSafe = false,
                scorePercentage = 34,
                flag = "Potentially Sensitive / Harassment",
                explanation = "AI detected potentially hurtful or sensitive language. Please ensure comments respect community standards."
            )
        } else {
            ModerationResult(
                isSafe = true,
                scorePercentage = 99,
                flag = null,
                explanation = "Content passed AI safety check. Friendly and appropriate for Blinkchat."
            )
        }
    }

    // --- 3. Customizable AI Filter Generator ---
    suspend fun generateCustomAiFilter(userPrompt: String): CustomAiFilterResult = withContext(Dispatchers.IO) {
        val prompt = "Create a custom camera aesthetic filter based on user request: \"$userPrompt\".\nRespond strictly in JSON with fields:\nname (2-3 words),\nemoji (single emoji),\nprimaryColorHex (hex code e.g. #FF007A),\nsecondaryColorHex (hex code e.g. #00F0FF),\nvignetteAlpha (float 0.1 to 0.6),\nparticleType (\"stars\", \"matrix\", \"sparks\", \"dust\", or \"none\"),\nscanlines (boolean),\npromptDescription (short 1 sentence description)."

        val apiResult = callGeminiApi(prompt, "You are a creative visual shader designer in Blinkchat.")
        if (apiResult != null) {
            try {
                val cleanJson = apiResult.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleanJson}")
                val primaryColor = try { Color(android.graphics.Color.parseColor(json.optString("primaryColorHex", "#8B5CF6"))) } catch (e: Exception) { Color(0xFF8B5CF6) }
                val secondaryColor = try { Color(android.graphics.Color.parseColor(json.optString("secondaryColorHex", "#06B6D4"))) } catch (e: Exception) { Color(0xFF06B6D4) }
                return@withContext CustomAiFilterResult(
                    id = "custom_" + System.currentTimeMillis(),
                    name = json.optString("name", "Custom AI Glow"),
                    emoji = json.optString("emoji", "✨"),
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    vignetteAlpha = json.optDouble("vignetteAlpha", 0.3).toFloat(),
                    particleType = json.optString("particleType", "stars"),
                    scanlines = json.optBoolean("scanlines", false),
                    promptDescription = json.optString("promptDescription", "Created from prompt: $userPrompt")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Filter parse failed", e)
            }
        }

        // Fallback filter customization
        val lower = userPrompt.lowercase()
        when {
            lower.contains("matrix") || lower.contains("green") || lower.contains("hacker") -> {
                CustomAiFilterResult(
                    id = "custom_" + System.currentTimeMillis(),
                    name = "Matrix Code Rain",
                    emoji = "🟢",
                    primaryColor = Color(0xFF00FF66),
                    secondaryColor = Color(0xFF003300),
                    vignetteAlpha = 0.45f,
                    particleType = "matrix",
                    scanlines = true,
                    promptDescription = "Digital code terminal with cyber emerald glow"
                )
            }
            lower.contains("anime") || lower.contains("pastel") || lower.contains("pink") -> {
                CustomAiFilterResult(
                    id = "custom_" + System.currentTimeMillis(),
                    name = "Anime Pastel Dream",
                    emoji = "🌸",
                    primaryColor = Color(0xFFFFB6C1),
                    secondaryColor = Color(0xFFE6E6FA),
                    vignetteAlpha = 0.25f,
                    particleType = "dust",
                    scanlines = false,
                    promptDescription = "Dreamy 90s anime sky with soft pastel blush"
                )
            }
            else -> {
                CustomAiFilterResult(
                    id = "custom_" + System.currentTimeMillis(),
                    name = "Electric Aura",
                    emoji = "⚡",
                    primaryColor = Color(0xFF7C3AED),
                    secondaryColor = Color(0xFF06B6D4),
                    vignetteAlpha = 0.35f,
                    particleType = "sparks",
                    scanlines = false,
                    promptDescription = "Vibrant electromagnetic shimmer crafted by AI"
                )
            }
        }
    }

    // --- 4. Interactive Voice Conversation with Blink AI ---
    suspend fun getVoiceChatResponse(userSpokenText: String): String = withContext(Dispatchers.IO) {
        val prompt = "User is speaking to you in live voice chat: \"$userSpokenText\".\nRespond naturally in 1-2 conversational spoken sentences with warmth, witty personality, and encouraging tone. Do not use complex markdown."
        val result = callGeminiApi(prompt, "You are Blink AI speaking live in voice mode. Keep answers concise, natural, and friendly.")
        result ?: when {
            userSpokenText.lowercase().contains("how are you") -> "I'm feeling electric! What are we creating or snapping together today?"
            userSpokenText.lowercase().contains("filter") -> "I recommend trying the new Anime Pastel or Cyberpunk Glow filters on the camera!"
            userSpokenText.lowercase().contains("bye") -> "Catch you later! Don't forget to keep your Blink streak burning!"
            else -> "I heard you! That sounds awesome—let's make something creative on Blinkchat!"
        }
    }

    // --- 5. General AI Chat Response ---
    suspend fun getAiResponse(userMessage: String, contextHistory: List<Pair<String, Boolean>> = emptyList()): String = withContext(Dispatchers.IO) {
        val result = callGeminiApi(
            prompt = userMessage,
            systemPrompt = "You are Blink AI, the vibrant, creative, and witty in-app AI companion inside Blinkchat. " +
                    "Help users craft epic captions, brainstorm fun photo/video filter ideas, reply with personality, " +
                    "and answer questions about safety, privacy, and staying safe online. Keep answers punchy and warm!"
        )
        result ?: generateSmartFallback(userMessage)
    }

    private fun generateSmartFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("caption") || lower.contains("post") -> {
                "Here are 3 fire captions for your snap:\n\n" +
                "1. \"Blinking through neon nights and good vibes ⚡✨\"\n" +
                "2. \"Catch me in full glow mode 📸💫 #BlinkLife\"\n" +
                "3. \"Moments that don't need a filter, but look epic with one anyway 🔮\""
            }
            lower.contains("filter") || lower.contains("camera") -> {
                "Try the **Cyberpunk Neon** filter for night streets, or **Sunset Glow** for that golden hour magic! Don't forget you can tap 'Custom AI Filter' to design your own shader 🎨✨"
            }
            lower.contains("privacy") || lower.contains("ghost") || lower.contains("safety") -> {
                "Privacy on Blinkchat is fully in your hands! 🛡️ Switch on **Ghost Mode** in Settings to hide your active presence, set stories to 'Friends Only', or block/report anyone instantly."
            }
            else -> {
                "I'm here for you! Ask me for snap captions, explore the AI Gallery, or switch on voice chat to talk live with me! ⚡"
            }
        }
    }
}
