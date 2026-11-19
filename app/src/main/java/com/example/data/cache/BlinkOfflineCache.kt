package com.example.data.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Offline storage and caching engine for Blinkchat.
 * Persists posts, stories, messages, conversations, user profile, customer accounts,
 * and payment records to disk so the application can be used completely offline.
 */
class BlinkOfflineCache(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("blink_offline_prefs", Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "blink_offline_data.json")

    fun saveOfflineState(
        currentUser: User?,
        posts: List<Post>,
        stories: List<Story>,
        conversations: List<Conversation>,
        messages: Map<String, List<ChatMessage>>,
        customers: List<CustomerAccount>,
        payments: List<PaymentRecord>
    ) {
        try {
            val root = JSONObject()

            // Save user
            if (currentUser != null) {
                val uObj = JSONObject().apply {
                    put("id", currentUser.id)
                    put("username", currentUser.username)
                    put("displayName", currentUser.displayName)
                    put("email", currentUser.email)
                    put("avatarColorHex", currentUser.avatarColorHex)
                    put("bio", currentUser.bio)
                    put("streaksCount", currentUser.streaksCount)
                    put("postsCount", currentUser.postsCount)
                    put("friendsCount", currentUser.friendsCount)
                    put("followersCount", currentUser.followersCount)
                    put("followingCount", currentUser.followingCount)
                    put("statusText", currentUser.statusText)
                    put("location", currentUser.location)
                    put("coordinates", currentUser.coordinates)
                    put("isOnline", currentUser.isOnline)
                    put("isGhostMode", currentUser.isGhostMode)
                    put("outfitName", currentUser.outfitName)
                    put("outfitEmoji", currentUser.outfitEmoji)
                    put("headwear", currentUser.headwear)
                    put("accessory", currentUser.accessory)
                    put("auraGlow", currentUser.auraGlow)
                    put("bannerTheme", currentUser.bannerTheme)
                    put("vipBadge", currentUser.vipBadge)
                    put("profileLevel", currentUser.profileLevel)
                    put("profileXp", currentUser.profileXp)
                }
                root.put("currentUser", uObj)
            }

            // Save posts
            val postsArray = JSONArray()
            for (p in posts.take(50)) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("authorId", p.authorId)
                    put("authorName", p.authorName)
                    put("authorUsername", p.authorUsername)
                    put("authorAvatarColor", p.authorAvatarColor)
                    put("imageUri", p.imageUri ?: "")
                    put("caption", p.caption)
                    put("filterTag", p.filterTag)
                    put("timeAgo", p.timeAgo)
                    put("likesCount", p.likesCount)
                    put("commentsCount", p.commentsCount)
                    put("isLiked", p.isLiked)
                    put("isSaved", p.isSaved)
                }
                postsArray.put(pObj)
            }
            root.put("posts", postsArray)

            // Save customers
            val customersArray = JSONArray()
            for (c in customers) {
                val cObj = JSONObject().apply {
                    put("userId", c.userId)
                    put("username", c.username)
                    put("displayName", c.displayName)
                    put("email", c.email)
                    put("tier", c.tier.name)
                    put("isActive", c.isActive)
                    put("isOnline", c.isOnline)
                    put("location", c.location)
                    put("totalSpent", c.totalSpent)
                    put("joinedDate", c.joinedDate)
                    put("lastActive", c.lastActive)
                    put("avatarColorHex", c.avatarColorHex)
                }
                customersArray.put(cObj)
            }
            root.put("customers", customersArray)

            // Save payments
            val paymentsArray = JSONArray()
            for (pm in payments) {
                val pmObj = JSONObject().apply {
                    put("id", pm.id)
                    put("customerId", pm.customerId)
                    put("customerName", pm.customerName)
                    put("customerEmail", pm.customerEmail)
                    put("planName", pm.planName)
                    put("amount", pm.amount)
                    put("currency", pm.currency)
                    put("status", pm.status.name)
                    put("dateFormatted", pm.dateFormatted)
                    put("paymentMethod", pm.paymentMethod)
                    put("timestamp", pm.timestamp)
                }
                paymentsArray.put(pmObj)
            }
            root.put("payments", paymentsArray)

            // Save conversations
            val convsArray = JSONArray()
            for (conv in conversations) {
                val cObj = JSONObject().apply {
                    put("id", conv.id)
                    put("participantId", conv.participantId)
                    put("name", conv.name)
                    put("username", conv.username)
                    put("avatarColorHex", conv.avatarColorHex)
                    put("lastMessage", conv.lastMessage)
                    put("lastMessageTime", conv.lastMessageTime)
                    put("unreadCount", conv.unreadCount)
                    put("isAiBot", conv.isAiBot)
                    put("streak", conv.streak)
                    put("isOnline", conv.isOnline)
                    put("isBlinkActive", conv.isBlinkActive)
                    put("isGroup", conv.isGroup)
                    put("groupTitle", conv.groupTitle ?: "")
                    put("groupMembers", JSONArray(conv.groupMembers))
                    put("memberNames", JSONArray(conv.memberNames))
                }
                convsArray.put(cObj)
            }
            root.put("conversations", convsArray)

            // Save messages grouped by conversationId
            val messagesObj = JSONObject()
            for ((convId, msgList) in messages) {
                val msgArr = JSONArray()
                for (m in msgList.takeLast(200)) {
                    val mObj = JSONObject().apply {
                        put("id", m.id)
                        put("conversationId", m.conversationId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("text", m.text)
                        put("mediaUri", m.mediaUri ?: "")
                        put("mediaType", m.mediaType)
                        put("timestamp", m.timestamp)
                        put("timeFormatted", m.timeFormatted)
                        put("isBlink", m.isBlink)
                        put("blinkSecondsRemaining", m.blinkSecondsRemaining)
                        put("isRead", m.isRead)
                        put("isExpired", m.isExpired)
                        put("isFromMe", m.isFromMe)
                        put("reaction", m.reaction ?: "")
                        if (m.readAt != null) put("readAt", m.readAt)
                        val reactJson = JSONObject()
                        m.reactions.forEach { (emoji, users) ->
                            reactJson.put(emoji, JSONArray(users))
                        }
                        put("reactions", reactJson)
                    }
                    msgArr.put(mObj)
                }
                messagesObj.put(convId, msgArr)
            }
            root.put("messages", messagesObj)

            // Write to local cache file
            cacheFile.writeText(root.toString())
            prefs.edit().putLong("last_cached_timestamp", System.currentTimeMillis()).apply()
            Log.d("BlinkOfflineCache", "Successfully saved offline cache to ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Log.w("BlinkOfflineCache", "Failed to write offline cache: ${e.message}")
        }
    }

    fun loadCachedUser(): User? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("currentUser")) return null
            val u = root.getJSONObject("currentUser")
            User(
                id = u.getString("id"),
                username = u.getString("username"),
                displayName = u.getString("displayName"),
                email = u.getString("email"),
                avatarColorHex = u.optString("avatarColorHex", "#8B5CF6"),
                bio = u.optString("bio", ""),
                streaksCount = u.optInt("streaksCount", 0),
                postsCount = u.optInt("postsCount", 0),
                friendsCount = u.optInt("friendsCount", 0),
                followersCount = u.optInt("followersCount", 0),
                followingCount = u.optInt("followingCount", 0),
                statusText = u.optString("statusText", "Available ⚡"),
                location = u.optString("location", "Tokyo, Japan"),
                coordinates = u.optString("coordinates", "35.6762° N, 139.6503° E"),
                isOnline = u.optBoolean("isOnline", true),
                isGhostMode = u.optBoolean("isGhostMode", false),
                outfitName = u.optString("outfitName", "Cyber Neon Trench"),
                outfitEmoji = u.optString("outfitEmoji", "🧥"),
                headwear = u.optString("headwear", "Hologram Halo 🪖"),
                accessory = u.optString("accessory", "Matrix Glitch Shades 🕶️"),
                auraGlow = u.optString("auraGlow", "#8B5CF6"),
                bannerTheme = u.optString("bannerTheme", "Synthwave Tokyo Night 🌆"),
                vipBadge = u.optString("vipBadge", "⚡ VIP Creator"),
                profileLevel = u.optInt("profileLevel", 5),
                profileXp = u.optInt("profileXp", 850)
            )
        } catch (e: Exception) {
            Log.w("BlinkOfflineCache", "Failed to load cached user: ${e.message}")
            null
        }
    }

    fun loadCachedPosts(): List<Post>? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("posts")) return null
            val arr = root.getJSONArray("posts")
            val list = mutableListOf<Post>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val uriStr = p.optString("imageUri", "")
                list.add(
                    Post(
                        id = p.getString("id"),
                        authorId = p.getString("authorId"),
                        authorName = p.getString("authorName"),
                        authorUsername = p.getString("authorUsername"),
                        authorAvatarColor = p.optString("authorAvatarColor", "#8B5CF6"),
                        imageUri = if (uriStr.isNotBlank()) uriStr else null,
                        caption = p.getString("caption"),
                        filterTag = p.optString("filterTag", "Normal"),
                        timeAgo = p.optString("timeAgo", "Recently"),
                        likesCount = p.optInt("likesCount", 0),
                        commentsCount = p.optInt("commentsCount", 0),
                        isLiked = p.optBoolean("isLiked", false),
                        isSaved = p.optBoolean("isSaved", false)
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.w("BlinkOfflineCache", "Failed to load cached posts: ${e.message}")
            null
        }
    }

    fun loadCachedCustomers(): List<CustomerAccount>? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("customers")) return null
            val arr = root.getJSONArray("customers")
            val list = mutableListOf<CustomerAccount>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val tierName = c.optString("tier", CustomerTier.PRO.name)
                val tier = try { CustomerTier.valueOf(tierName) } catch (e: Exception) { CustomerTier.PRO }
                list.add(
                    CustomerAccount(
                        userId = c.getString("userId"),
                        username = c.getString("username"),
                        displayName = c.getString("displayName"),
                        email = c.getString("email"),
                        tier = tier,
                        isActive = c.optBoolean("isActive", true),
                        isOnline = c.optBoolean("isOnline", true),
                        location = c.optString("location", "Tokyo, Japan"),
                        totalSpent = c.optDouble("totalSpent", 0.0),
                        joinedDate = c.optString("joinedDate", "Oct 2026"),
                        lastActive = c.optString("lastActive", "Active recently"),
                        avatarColorHex = c.optString("avatarColorHex", "#8B5CF6")
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            null
        }
    }

    fun loadCachedPayments(): List<PaymentRecord>? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("payments")) return null
            val arr = root.getJSONArray("payments")
            val list = mutableListOf<PaymentRecord>()
            for (i in 0 until arr.length()) {
                val pm = arr.getJSONObject(i)
                val statusName = pm.optString("status", PaymentStatus.COMPLETED.name)
                val status = try { PaymentStatus.valueOf(statusName) } catch (e: Exception) { PaymentStatus.COMPLETED }
                list.add(
                    PaymentRecord(
                        id = pm.getString("id"),
                        customerId = pm.getString("customerId"),
                        customerName = pm.getString("customerName"),
                        customerEmail = pm.getString("customerEmail"),
                        planName = pm.getString("planName"),
                        amount = pm.getDouble("amount"),
                        currency = pm.optString("currency", "USD"),
                        status = status,
                        dateFormatted = pm.optString("dateFormatted", "Oct 22, 2026"),
                        paymentMethod = pm.optString("paymentMethod", "Visa •••• 4242"),
                        timestamp = pm.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            null
        }
    }

    fun loadCachedConversations(): List<Conversation>? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("conversations")) return null
            val arr = root.getJSONArray("conversations")
            val list = mutableListOf<Conversation>()
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val membersArr = c.optJSONArray("groupMembers")
                val members = mutableListOf<String>()
                if (membersArr != null) {
                    for (j in 0 until membersArr.length()) members.add(membersArr.getString(j))
                }
                val namesArr = c.optJSONArray("memberNames")
                val names = mutableListOf<String>()
                if (namesArr != null) {
                    for (j in 0 until namesArr.length()) names.add(namesArr.getString(j))
                }
                val groupTitleRaw = c.optString("groupTitle", "")
                list.add(
                    Conversation(
                        id = c.getString("id"),
                        participantId = c.optString("participantId", c.getString("id")),
                        name = c.getString("name"),
                        username = c.optString("username", ""),
                        avatarColorHex = c.optString("avatarColorHex", "#8B5CF6"),
                        lastMessage = c.optString("lastMessage", ""),
                        lastMessageTime = c.optString("lastMessageTime", "Just now"),
                        unreadCount = c.optInt("unreadCount", 0),
                        isAiBot = c.optBoolean("isAiBot", false),
                        streak = c.optInt("streak", 0),
                        isOnline = c.optBoolean("isOnline", false),
                        isBlinkActive = c.optBoolean("isBlinkActive", false),
                        isGroup = c.optBoolean("isGroup", false),
                        groupTitle = groupTitleRaw.ifBlank { null },
                        groupMembers = members,
                        memberNames = names
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            null
        }
    }

    fun loadCachedMessages(): Map<String, List<ChatMessage>>? {
        if (!cacheFile.exists()) return null
        return try {
            val root = JSONObject(cacheFile.readText())
            if (!root.has("messages")) return null
            val obj = root.getJSONObject("messages")
            val result = mutableMapOf<String, List<ChatMessage>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val convId = keys.next()
                val arr = obj.getJSONArray(convId)
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) {
                    val m = arr.getJSONObject(i)
                    val mediaUriRaw = m.optString("mediaUri", "")
                    val reactionRaw = m.optString("reaction", "")
                    val reactJson = m.optJSONObject("reactions")
                    val reactionsMap = mutableMapOf<String, List<String>>()
                    if (reactJson != null) {
                        val rKeys = reactJson.keys()
                        while (rKeys.hasNext()) {
                            val emoji = rKeys.next()
                            val uArr = reactJson.optJSONArray(emoji)
                            val users = mutableListOf<String>()
                            if (uArr != null) {
                                for (k in 0 until uArr.length()) users.add(uArr.getString(k))
                            }
                            if (users.isNotEmpty()) reactionsMap[emoji] = users
                        }
                    }
                    list.add(
                        ChatMessage(
                            id = m.getString("id"),
                            conversationId = m.optString("conversationId", convId),
                            senderId = m.optString("senderId", ""),
                            senderName = m.optString("senderName", ""),
                            text = m.optString("text", ""),
                            mediaUri = mediaUriRaw.ifBlank { null },
                            mediaType = m.optString("mediaType", "none"),
                            timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                            timeFormatted = m.optString("timeFormatted", "Just now"),
                            isBlink = m.optBoolean("isBlink", false),
                            blinkSecondsRemaining = m.optInt("blinkSecondsRemaining", 10),
                            isRead = m.optBoolean("isRead", false),
                            isExpired = m.optBoolean("isExpired", false),
                            isFromMe = m.optBoolean("isFromMe", false),
                            reaction = reactionRaw.ifBlank { null },
                            reactions = reactionsMap,
                            readAt = if (m.has("readAt")) m.optLong("readAt") else null
                        )
                    )
                }
                result[convId] = list
            }
            if (result.isNotEmpty()) result else null
        } catch (e: Exception) {
            null
        }
    }

    fun isOfflineCacheAvailable(): Boolean = cacheFile.exists() && cacheFile.length() > 0

    fun getLastCachedTimeFormatted(): String {
        val ts = prefs.getLong("last_cached_timestamp", 0L)
        if (ts == 0L) return "Not cached yet"
        val diffMins = (System.currentTimeMillis() - ts) / 60000
        return if (diffMins < 1) "Just now" else "${diffMins}m ago"
    }
}
