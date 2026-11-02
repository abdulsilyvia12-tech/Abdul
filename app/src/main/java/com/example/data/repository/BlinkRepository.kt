package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.ai.BlinkAiService
import com.example.data.cache.BlinkOfflineCache
import com.example.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BlinkRepository(private val context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.Main)

    val firestore: FirebaseFirestore?
        get() = firestoreDb

    private val firestoreDb: FirebaseFirestore? by lazy {
        try {
            val dbId = context?.getString(R.string.firestore_database_id)
            if (!dbId.isNullOrBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore custom db initialization: ${e.message}")
            try {
                FirebaseFirestore.getInstance()
            } catch (e2: Exception) {
                null
            }
        }
    }

    // Current logged in user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Registered users
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    // Feed Posts
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // Stories
    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    // Comments map: postId -> list of Comments
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments.asStateFlow()

    // Scrolling Videos (Blinks & User Posts)
    private val _videos = MutableStateFlow<List<BlinkVideo>>(emptyList())
    val videos: StateFlow<List<BlinkVideo>> = _videos.asStateFlow()

    // Conversations
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    // Messages map: conversationId -> list of ChatMessages
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    // Privacy & Safety
    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _reports = MutableStateFlow<List<SafetyReport>>(emptyList())
    val reports: StateFlow<List<SafetyReport>> = _reports.asStateFlow()

    // Friend Requests (Firestore Synchronized)
    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    // Social Notifications Activity Feed
    private val _notifications = MutableStateFlow<List<SocialNotification>>(emptyList())
    val notifications: StateFlow<List<SocialNotification>> = _notifications.asStateFlow()

    // Active Call State
    data class CallState(
        val isActive: Boolean = false,
        val targetUser: User? = null,
        val isVideo: Boolean = false,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = true,
        val isCameraFront: Boolean = true,
        val callDurationSeconds: Int = 0,
        val status: String = "Connecting..." // "Ringing...", "Connected", "Ended"
    )
    private val _currentCall = MutableStateFlow(CallState())
    val currentCall: StateFlow<CallState> = _currentCall.asStateFlow()

    // Offline Cache Engine
    val offlineCache: BlinkOfflineCache? by lazy { context?.let { BlinkOfflineCache(it) } }
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    // CRM Customer Accounts & Payments
    private val _customerAccounts = MutableStateFlow<List<CustomerAccount>>(emptyList())
    val customerAccounts: StateFlow<List<CustomerAccount>> = _customerAccounts.asStateFlow()

    private val _payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    val payments: StateFlow<List<PaymentRecord>> = _payments.asStateFlow()

    init {
        seedInitialData()
    }

    private fun seedInitialData() {
        val userMe = User(
            id = "user_me",
            username = "alex",
            displayName = "Alex Rivers",
            email = "alex@blinkchat.com",
            avatarColorHex = "#8B5CF6",
            bio = "Visual storyteller 📸 Neon enthusiast ⚡",
            streaksCount = 28,
            postsCount = 6,
            friendsCount = 189,
            statusText = "Exploring Tokyo nights ✨",
            location = "Tokyo, Japan",
            coordinates = "35.6762° N, 139.6503° E",
            isOnline = true,
            isGhostMode = false
        )

        val userMaya = User(
            id = "user_maya",
            username = "maya_lin",
            displayName = "Maya Lin",
            email = "maya@blinkchat.com",
            avatarColorHex = "#EC4899",
            bio = "Live concerts & festival lover 🎶 Sunset chaser 🌅",
            streaksCount = 42,
            postsCount = 14,
            friendsCount = 312,
            statusText = "At Sunset Sessions 🎧",
            location = "San Francisco, USA",
            coordinates = "37.7749° N, 122.4194° W",
            isOnline = true
        )

        val userKai = User(
            id = "user_kai",
            username = "kai_storm",
            displayName = "Kai Storm",
            email = "kai@blinkchat.com",
            avatarColorHex = "#06B6D4",
            bio = "Cyberpunk aesthetics & digital film 🎬",
            streaksCount = 15,
            postsCount = 9,
            friendsCount = 98,
            statusText = "Editing VHS Blinks 📼",
            location = "Berlin, Germany",
            coordinates = "52.5200° N, 13.4050° E",
            isOnline = false
        )

        val userElena = User(
            id = "user_elena",
            username = "elena_v",
            displayName = "Elena Vance",
            email = "elena@blinkchat.com",
            avatarColorHex = "#10B981",
            bio = "Coffee, creative filters, and spontaneous trips ☕✈️",
            streaksCount = 8,
            postsCount = 4,
            friendsCount = 210,
            statusText = "Available for calls 💬",
            location = "London, UK",
            coordinates = "51.5074° N, 0.1278° W",
            isOnline = true
        )

        val userLucas = User(
            id = "user_lucas",
            username = "lucas_silva",
            displayName = "Lucas Silva",
            email = "lucas@blinkchat.com",
            avatarColorHex = "#F59E0B",
            bio = "Carnival rhythms & beach drone photography 🏖️🥁",
            streaksCount = 19,
            postsCount = 12,
            friendsCount = 175,
            statusText = "Ipanema sunset vibes 🌊",
            location = "Rio de Janeiro, Brazil",
            coordinates = "22.9068° S, 43.1729° W",
            isOnline = true
        )

        val userAisha = User(
            id = "user_aisha",
            username = "aisha_n",
            displayName = "Aisha Noor",
            email = "aisha@blinkchat.com",
            avatarColorHex = "#8B5CF6",
            bio = "Tech creator & safari wanderer 🦁💻",
            streaksCount = 24,
            postsCount = 15,
            friendsCount = 240,
            statusText = "Blinking from Nairobi Tech Hub ⚡",
            location = "Nairobi, Kenya",
            coordinates = "1.2921° S, 36.8219° E",
            isOnline = true
        )

        val userJin = User(
            id = "user_jin",
            username = "jin_park",
            displayName = "Jin Park",
            email = "jin@blinkchat.com",
            avatarColorHex = "#3B82F6",
            bio = "K-pop producer & midnight street food explorer 🍜✨",
            streaksCount = 31,
            postsCount = 22,
            friendsCount = 380,
            statusText = "Hongdae midnight studio 🎹",
            location = "Seoul, South Korea",
            coordinates = "37.5665° N, 126.9780° E",
            isOnline = true
        )

        val userChloe = User(
            id = "user_chloe",
            username = "chloe_dupont",
            displayName = "Chloé Dupont",
            email = "chloe@blinkchat.com",
            avatarColorHex = "#EC4899",
            bio = "Haute couture & rooftop cafe sketches 🥐🎨",
            streaksCount = 11,
            postsCount = 7,
            friendsCount = 195,
            statusText = "Montmartre golden hour ☕",
            location = "Paris, France",
            coordinates = "48.8566° N, 2.3522° E",
            isOnline = false
        )

        val userZane = User(
            id = "user_zane",
            username = "zane_flux",
            displayName = "Zane Flux",
            email = "zane@blinkchat.com",
            avatarColorHex = "#06B6D4",
            bio = "Cyber DJ & audio reactive visualizer coder 🎧⚡",
            streaksCount = 42,
            postsCount = 38,
            friendsCount = 512,
            statusText = "Berlin underground sets 🎛️",
            location = "Berlin, Germany",
            coordinates = "52.5200° N, 13.4050° E",
            isOnline = true
        )

        val userLiam = User(
            id = "user_liam",
            username = "liam_beats",
            displayName = "Liam Vance",
            email = "liam@blinkchat.com",
            avatarColorHex = "#F59E0B",
            bio = "35mm analog street photographer & skater 🛹📸",
            streaksCount = 19,
            postsCount = 15,
            friendsCount = 280,
            statusText = "Hunting golden hour light ☀️",
            location = "London, UK",
            coordinates = "51.5074° N, 0.1278° W",
            isOnline = false
        )

        val userSophia = User(
            id = "user_sophia",
            username = "sophia_art",
            displayName = "Sophia Chen",
            email = "sophia@blinkchat.com",
            avatarColorHex = "#EC4899",
            bio = "Generative 3D artist & motion graphics designer 🎨✨",
            streaksCount = 27,
            postsCount = 29,
            friendsCount = 410,
            statusText = "Blender shader experiments 🔮",
            location = "Toronto, Canada",
            coordinates = "43.6532° N, 79.3832° W",
            isOnline = true
        )

        _allUsers.value = listOf(userMe, userMaya, userKai, userElena, userLucas, userAisha, userJin, userChloe, userZane, userLiam, userSophia)
        _currentUser.value = userMe

        // Initial Stories
        _stories.value = listOf(
            Story(
                id = "story_maya",
                userId = userMaya.id,
                username = userMaya.username,
                userDisplayName = userMaya.displayName,
                userAvatarColor = userMaya.avatarColorHex,
                imageDrawableRes = R.drawable.feed_sunset_snap_1792586114190,
                caption = "Golden hour festival vibes with the crew! 🌅🎉",
                filterTag = "Sunset Glow",
                isViewed = false
            ),
            Story(
                id = "story_kai",
                userId = userKai.id,
                username = userKai.username,
                userDisplayName = userKai.displayName,
                userAvatarColor = userKai.avatarColorHex,
                imageDrawableRes = R.drawable.feed_cyber_snap_1792586101459,
                caption = "Late night ramen run under neon signs 🍜✨",
                filterTag = "Cyberpunk",
                isViewed = false
            )
        )

        // Initial Posts with Rich Social Reactions & Verified Creator Tags
        _posts.value = listOf(
            Post(
                id = "post_1",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                imageDrawableRes = R.drawable.feed_cyber_snap_1792586101459,
                caption = "Caught in the electric frequency of downtown streets. Cyberpunk filter on point tonight! ⚡🌃 #cyberpunk #tokyo",
                filterTag = "Cyberpunk Neon",
                timeAgo = "18m ago",
                likesCount = 84,
                commentsCount = 12,
                sharesCount = 18,
                isLiked = true,
                isVerified = true,
                reactions = mapOf("❤️" to 42, "🔥" to 28, "⚡" to 14),
                userReaction = "⚡"
            ),
            Post(
                id = "post_2",
                authorId = userMaya.id,
                authorName = userMaya.displayName,
                authorUsername = userMaya.username,
                authorAvatarColor = userMaya.avatarColorHex,
                imageDrawableRes = R.drawable.feed_sunset_snap_1792586114190,
                caption = "Unfiltered laughter and golden hour sounds at Rooftop Sunset Sessions 🌇✨ Tag your concert buddies! #sunset #vibes #music",
                filterTag = "Sunset Glow",
                timeAgo = "2h ago",
                likesCount = 142,
                commentsCount = 26,
                sharesCount = 34,
                isLiked = false,
                isVerified = true,
                reactions = mapOf("❤️" to 95, "😍" to 32, "🌅" to 15)
            ),
            Post(
                id = "post_3",
                authorId = userZane.id,
                authorName = userZane.displayName,
                authorUsername = userZane.username,
                authorAvatarColor = userZane.avatarColorHex,
                imageDrawableRes = R.drawable.feed_cyber_snap_1792586101459,
                caption = "Berlin underground warehouse set went completely crazy tonight! Audio-reactive visuals dialed in 🎛️⚡ #phonk #techno #berlin",
                filterTag = "Emerald Matrix",
                timeAgo = "3h ago",
                likesCount = 215,
                commentsCount = 38,
                sharesCount = 52,
                isLiked = true,
                isVerified = true,
                reactions = mapOf("🔥" to 110, "⚡" to 75, "🎧" to 30),
                userReaction = "🔥"
            ),
            Post(
                id = "post_4",
                authorId = userLiam.id,
                authorName = userLiam.displayName,
                authorUsername = userLiam.username,
                authorAvatarColor = userLiam.avatarColorHex,
                imageDrawableRes = R.drawable.feed_sunset_snap_1792586114190,
                caption = "Golden hour street skate line through Southbank. 35mm film tones hitting just right 🛹☀️ #skate #london #goldenhour",
                filterTag = "Vintage Kodachrome",
                timeAgo = "5h ago",
                likesCount = 178,
                commentsCount = 22,
                sharesCount = 29,
                isLiked = false,
                isVerified = false,
                reactions = mapOf("🛹" to 82, "🔥" to 51, "❤️" to 45)
            )
        )

        // Social Notifications Activity Feed
        _notifications.value = listOf(
            SocialNotification(
                id = "notif_1",
                type = NotificationType.LIKE,
                actorName = userMaya.displayName,
                actorUsername = userMaya.username,
                actorAvatarColor = userMaya.avatarColorHex,
                title = "liked your video",
                description = "Blinding Lights City Night ✨",
                timeAgo = "12m ago",
                targetVideoId = "vid_pop_2"
            ),
            SocialNotification(
                id = "notif_2",
                type = NotificationType.FOLLOW,
                actorName = userKai.displayName,
                actorUsername = userKai.username,
                actorAvatarColor = userKai.avatarColorHex,
                title = "started following you",
                description = "Tap to view profile and follow back",
                timeAgo = "28m ago",
                isFollowedBack = false
            ),
            SocialNotification(
                id = "notif_3",
                type = NotificationType.COMMENT,
                actorName = userElena.displayName,
                actorUsername = userElena.username,
                actorAvatarColor = userElena.avatarColorHex,
                title = "commented on your post",
                description = "\"The reflection on the street is pure cinema 🔥\"",
                timeAgo = "1h ago",
                targetPostId = "post_1"
            ),
            SocialNotification(
                id = "notif_4",
                type = NotificationType.STREAK_ALERT,
                actorName = userMaya.displayName,
                actorUsername = userMaya.username,
                actorAvatarColor = userMaya.avatarColorHex,
                title = "🔥 42-Day Streak is expiring soon!",
                description = "Send a Blink to Maya Lin within 4 hours to keep your streak alive ⚡",
                timeAgo = "2h ago"
            ),
            SocialNotification(
                id = "notif_5",
                type = NotificationType.LIKE,
                actorName = userZane.displayName,
                actorUsername = userZane.username,
                actorAvatarColor = userZane.avatarColorHex,
                title = "liked your post",
                description = "Electric frequency in downtown streets ⚡",
                timeAgo = "3h ago",
                targetPostId = "post_1"
            ),
            SocialNotification(
                id = "notif_6",
                type = NotificationType.MENTION,
                actorName = userSophia.displayName,
                actorUsername = userSophia.username,
                actorAvatarColor = userSophia.avatarColorHex,
                title = "mentioned you in a video",
                description = "\"Check out these new shader experiments with @alex 🎨✨\"",
                timeAgo = "5h ago"
            ),
            SocialNotification(
                id = "notif_7",
                type = NotificationType.FOLLOW,
                actorName = userLiam.displayName,
                actorUsername = userLiam.username,
                actorAvatarColor = userLiam.avatarColorHex,
                title = "started following you",
                description = "Tap to view profile",
                timeAgo = "1d ago",
                isFollowedBack = true
            )
        )

        // Initial Comments
        _comments.value = mapOf(
            "post_1" to listOf(
                Comment(
                    id = "c1",
                    postId = "post_1",
                    authorId = userMaya.id,
                    authorName = userMaya.displayName,
                    authorUsername = userMaya.username,
                    authorAvatarColor = userMaya.avatarColorHex,
                    text = "This lighting is insane! Did you snap this on Blinkchat camera?",
                    timeAgo = "12m ago",
                    likesCount = 5,
                    isLiked = true
                ),
                Comment(
                    id = "c2",
                    postId = "post_1",
                    authorId = userElena.id,
                    authorName = userElena.displayName,
                    authorUsername = userElena.username,
                    authorAvatarColor = userElena.avatarColorHex,
                    text = "The reflection on the street is pure cinema 🔥",
                    timeAgo = "8m ago",
                    likesCount = 3,
                    isLiked = false
                )
            ),
            "post_2" to listOf(
                Comment(
                    id = "c3",
                    postId = "post_2",
                    authorId = userMe.id,
                    authorName = userMe.displayName,
                    authorUsername = userMe.username,
                    authorAvatarColor = userMe.avatarColorHex,
                    text = "Looked like an unforgettable sunset set! 🎶🔥",
                    timeAgo = "1h ago",
                    likesCount = 9,
                    isLiked = true
                )
            )
        )

        // Initial Conversations
        val convAi = Conversation(
            id = "conv_ai",
            participantId = "user_ai",
            name = "Blink AI Assistant",
            username = "blink_ai",
            avatarColorHex = "#7C3AED",
            lastMessage = "Hey Alex! Need catchy caption ideas or safety tips? ⚡",
            lastMessageTime = "Just now",
            unreadCount = 1,
            isAiBot = true,
            streak = 100,
            isOnline = true
        )

        val convMaya = Conversation(
            id = "conv_maya",
            participantId = userMaya.id,
            name = userMaya.displayName,
            username = userMaya.username,
            avatarColorHex = userMaya.avatarColorHex,
            lastMessage = "Check out the festival Blinks I just posted! 🌅",
            lastMessageTime = "15m",
            unreadCount = 0,
            isAiBot = false,
            streak = 42,
            isOnline = true,
            isBlinkActive = true
        )

        val convKai = Conversation(
            id = "conv_kai",
            participantId = userKai.id,
            name = userKai.displayName,
            username = userKai.username,
            avatarColorHex = userKai.avatarColorHex,
            lastMessage = "Sent a disappearing Blink photo ⏳",
            lastMessageTime = "1h",
            unreadCount = 2,
            isAiBot = false,
            streak = 15,
            isOnline = false
        )

        val convGroupTokyo = Conversation(
            id = "group_tokyo_nights",
            participantId = "group_tokyo_nights",
            name = "⚡ Tokyo Night Explorers",
            username = "tokyo_group",
            avatarColorHex = "#06B6D4",
            lastMessage = "Maya: Meeting at Shibuya crossing tonight! 📸",
            lastMessageTime = "5m",
            unreadCount = 3,
            isAiBot = false,
            streak = 28,
            isOnline = true,
            isGroup = true,
            groupTitle = "⚡ Tokyo Night Explorers",
            groupMembers = listOf("user_me", userMaya.id, userKai.id, userElena.id),
            memberNames = listOf("Alex", "Maya", "Kai", "Elena")
        )

        _conversations.value = listOf(convGroupTokyo, convAi, convMaya, convKai)

        // Initial Messages
        _messages.value = mapOf(
            "group_tokyo_nights" to listOf(
                ChatMessage(
                    id = "m_g_1",
                    conversationId = "group_tokyo_nights",
                    senderId = userKai.id,
                    senderName = "Kai Storm",
                    text = "Welcome to the Tokyo Night Blinks crew! ⚡🏙️",
                    timeFormatted = "9:30 PM",
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m_g_2",
                    conversationId = "group_tokyo_nights",
                    senderId = userElena.id,
                    senderName = "Elena Vance",
                    text = "Just landed at Haneda! The neon aesthetic here is unreal ✨",
                    timeFormatted = "9:45 PM",
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m_g_3",
                    conversationId = "group_tokyo_nights",
                    senderId = userMaya.id,
                    senderName = "Maya Lin",
                    text = "Maya: Meeting at Shibuya crossing tonight! 📸",
                    timeFormatted = "10:00 PM",
                    isFromMe = false
                )
            ),
            "conv_ai" to listOf(
                ChatMessage(
                    id = "m_ai_1",
                    conversationId = "conv_ai",
                    senderId = "user_ai",
                    text = "Welcome to Blinkchat! ⚡ I'm your creative AI copilot. Ask me to rewrite captions, pick the best filters for your Blinks, or explain safety settings!",
                    timeFormatted = "10:00 AM",
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m_ai_2",
                    conversationId = "conv_ai",
                    senderId = "user_ai",
                    text = "Hey Alex! Need catchy caption ideas or safety tips? ⚡",
                    timeFormatted = "10:01 AM",
                    isFromMe = false
                )
            ),
            "conv_maya" to listOf(
                ChatMessage(
                    id = "m_m_1",
                    conversationId = "conv_maya",
                    senderId = userMaya.id,
                    text = "Hey! Did you see the new Sunset Glow filter on the camera? 🌇",
                    timeFormatted = "11:42 AM",
                    isFromMe = false
                ),
                ChatMessage(
                    id = "m_m_2",
                    conversationId = "conv_maya",
                    senderId = userMe.id,
                    text = "Yeah it looks gorgeous! Just checked your festival story.",
                    timeFormatted = "11:45 AM",
                    isFromMe = true
                ),
                ChatMessage(
                    id = "m_m_3",
                    conversationId = "conv_maya",
                    senderId = userMaya.id,
                    text = "Check out the festival Blinks I just posted! 🌅",
                    timeFormatted = "11:48 AM",
                    isFromMe = false,
                    isBlink = true,
                    blinkSecondsRemaining = 10
                )
            )
        )

        // Seeded Scrolling Videos (Blink Videos & Community User Posts)
        _videos.value = listOf(
            // 🏎️ Phonk & Drift (High-Octane 808 & Cowbell Bass Drops)
            BlinkVideo(
                id = "vid_phonk_1",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "Midnight Tokyo Highway Drift 🏎️💨",
                caption = "Full throttle around the C1 loop under rainy neon signs! That 808 cowbell drop hits different ⚡🏁",
                hashtags = listOf("#phonk", "#drift", "#tokyodrift", "#kordhell", "#jdm", "#c1loop"),
                musicTrack = "Murder In My Mind - KORDHELL",
                category = VideoCategory.PHONK,
                isBlink = true,
                durationSeconds = 15,
                likesCount = 8420,
                commentsCount = 620,
                sharesCount = 2100,
                isLiked = true,
                isSaved = true,
                filterName = "Neon Edge",
                thumbnailRes = R.drawable.feed_cyber_snap_1792586101459,
                videoGradientColors = listOf(0xFF0F172A, 0xFF4338CA, 0xFF06B6D4),
                comments = listOf(
                    VideoComment("vc_p1", "Zane Flux", "zane_flux", "#06B6D4", "That transition on the hairpin turn was illegal 🔥💨", "10m ago", 82, true),
                    VideoComment("vc_p2", "Maya Lin", "maya_lin", "#EC4899", "KORDHELL never misses!! Turn this up 🔊", "4m ago", 45, false)
                ),
                createdAtFormatted = "18m ago"
            ),
            BlinkVideo(
                id = "vid_phonk_2",
                authorId = userZane.id,
                authorName = userZane.displayName,
                authorUsername = userZane.username,
                authorAvatarColor = userZane.avatarColorHex,
                title = "Night Supercar Cruise in the Rain 🌌",
                caption = "Atmospheric drift phonk through city reflections. Pure cinematic night drive vibes 🌧️✨",
                hashtags = listOf("#dvrst", "#closeeyes", "#driftphonk", "#nightdrive", "#supercar"),
                musicTrack = "Close Eyes - DVRST",
                category = VideoCategory.PHONK,
                isBlink = false,
                durationSeconds = 18,
                likesCount = 7120,
                commentsCount = 480,
                sharesCount = 1650,
                isLiked = true,
                isSaved = true,
                filterName = "Cyber Glitch",
                videoGradientColors = listOf(0xFF020617, 0xFF1E1B4B, 0xFF818CF8),
                comments = listOf(
                    VideoComment("vc_p3", "Liam Vance", "liam_beats", "#F59E0B", "DVRST's synth line is unmatched for late night drives 🌃", "30m ago", 37, true)
                ),
                createdAtFormatted = "45m ago"
            ),
            BlinkVideo(
                id = "vid_phonk_3",
                authorId = userLucas.id,
                authorName = userLucas.displayName,
                authorUsername = userLucas.username,
                authorAvatarColor = userLucas.avatarColorHex,
                title = "Heavy Deadlift PR: 500 lbs Lockout 🦾🔥",
                caption = "226kg / 500lbs moving like butter! Zero hesitation, pure gym phonk adrenaline ⚡😤",
                hashtags = listOf("#gymphonk", "#deadlift", "#pr", "#interworld", "#fitness", "#gains"),
                musicTrack = "METAMORPHOSIS - INTERWORLD",
                category = VideoCategory.PHONK,
                isBlink = true,
                durationSeconds = 14,
                likesCount = 9340,
                commentsCount = 810,
                sharesCount = 2450,
                isLiked = true,
                isSaved = true,
                filterName = "Thermal FLIR",
                videoGradientColors = listOf(0xFF18181B, 0xFF7F1D1D, 0xFFEF4444),
                comments = listOf(
                    VideoComment("vc_p4", "Kai Storm", "kai_storm", "#06B6D4", "LIGHT WEIGHT BABY!! INTERWORLD in the headphones = +50 lbs 💪", "15m ago", 112, true)
                ),
                createdAtFormatted = "1h ago"
            ),
            BlinkVideo(
                id = "vid_phonk_4",
                authorId = userSophia.id,
                authorName = userSophia.displayName,
                authorUsername = userSophia.username,
                authorAvatarColor = userSophia.avatarColorHex,
                title = "Brazilian Funk Phonk Dance Speed 🇧🇷⚡",
                caption = "Insane footwork speed challenge with Brazilian drift rhythm! Can you keep up with this tempo? 💃🔥",
                hashtags = listOf("#brazilianphonk", "#montagem", "#dancechallenge", "#funk", "#viral"),
                musicTrack = "Montagem Coral - Dj Holanda & Mc Gw",
                category = VideoCategory.PHONK,
                isBlink = true,
                durationSeconds = 16,
                likesCount = 11200,
                commentsCount = 940,
                sharesCount = 3400,
                isLiked = true,
                isSaved = true,
                filterName = "Chromatic RGB",
                videoGradientColors = listOf(0xFF064E3B, 0xFF10B981, 0xFFFACC15),
                comments = listOf(
                    VideoComment("vc_p5", "Elena Vance", "elena_v", "#10B981", "The foot speed is actually humanly impossible 😭🔥", "25m ago", 64, true)
                ),
                createdAtFormatted = "2h ago"
            ),
            BlinkVideo(
                id = "vid_phonk_5",
                authorId = userLiam.id,
                authorName = userLiam.displayName,
                authorUsername = userLiam.username,
                authorAvatarColor = userLiam.avatarColorHex,
                title = "Shibuya Underpass Skate Line 🛹📼",
                caption = "Kickflip down the 6-stair into a backside powerslide. 90s cassette grit and pure flow 📼🛹",
                hashtags = listOf("#skate", "#memphisphonk", "#pharmacist", "#street", "#vhs"),
                musicTrack = "North Memphis - Pharmacist",
                category = VideoCategory.PHONK,
                isBlink = false,
                durationSeconds = 15,
                likesCount = 6310,
                commentsCount = 390,
                sharesCount = 1120,
                isLiked = false,
                isSaved = true,
                filterName = "VHS 90s CRT",
                videoGradientColors = listOf(0xFF1C1917, 0xFF78350F, 0xFFF59E0B),
                createdAtFormatted = "3h ago"
            ),
            BlinkVideo(
                id = "vid_phonk_6",
                authorId = userJin.id,
                authorName = userJin.displayName,
                authorUsername = userJin.username,
                authorAvatarColor = userJin.avatarColorHex,
                title = "The Ultimate Chad Workout Montage 🗿👑",
                caption = "Average Blinkchat enjoyer hitting the gym at 5 AM! Unstoppable discipline & heavy bass 🗿💪",
                hashtags = listOf("#gigachad", "#gym", "#motivation", "#phonk", "#chad", "#discipline"),
                musicTrack = "GigaChad Theme (Phonk Remix) - g3ox_em",
                category = VideoCategory.PHONK,
                isBlink = true,
                durationSeconds = 15,
                likesCount = 12500,
                commentsCount = 1150,
                sharesCount = 4200,
                isLiked = true,
                isSaved = true,
                filterName = "Pop Halftone",
                videoGradientColors = listOf(0xFF18181B, 0xFF374151, 0xFF9CA3AF),
                comments = listOf(
                    VideoComment("vc_p6", "Lucas Gray", "lucas_g", "#F59E0B", "Can you hear the silence? Can you see the dark? 🗿👑", "40m ago", 204, true)
                ),
                createdAtFormatted = "3h ago"
            ),
            BlinkVideo(
                id = "vid_phonk_7",
                authorId = userMaya.id,
                authorName = userMaya.displayName,
                authorUsername = userMaya.username,
                authorAvatarColor = userMaya.avatarColorHex,
                title = "Cyber Drift Horizon Sunset 🌅⚡",
                caption = "Hypnotic dark wave phonk blasting as the sun dips below mountain peaks 🏔️🚗",
                hashtags = listOf("#hensonn", "#sahara", "#drift", "#darkwave", "#sunsetdrive"),
                musicTrack = "Sahara - Hensonn",
                category = VideoCategory.PHONK,
                isBlink = false,
                durationSeconds = 20,
                likesCount = 7890,
                commentsCount = 510,
                sharesCount = 1820,
                isLiked = true,
                isSaved = false,
                filterName = "Teal & Orange",
                videoGradientColors = listOf(0xFF31102A, 0xFF9D174D, 0xFFF97316),
                createdAtFormatted = "4h ago"
            ),

            // 🎤 Pop Hits & World Artists
            BlinkVideo(
                id = "vid_pop_1",
                authorId = userMaya.id,
                authorName = userMaya.displayName,
                authorUsername = userMaya.username,
                authorAvatarColor = userMaya.avatarColorHex,
                title = "Cruel Summer Singalong 💖",
                caption = "Nothing beats singing Taylor Swift with the windows down! Cruel summer vibes 🎤✨",
                hashtags = listOf("#taylorswift", "#cruelsummer", "#popmusic", "#singalong", "#erastour"),
                musicTrack = "Cruel Summer - Taylor Swift",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 22,
                likesCount = 1420,
                commentsCount = 118,
                sharesCount = 280,
                isLiked = true,
                isSaved = true,
                filterName = "Golden Sunset",
                thumbnailRes = R.drawable.feed_sunset_snap_1792586114190,
                videoGradientColors = listOf(0xFF831843, 0xFFBE185D, 0xFFF472B6),
                comments = listOf(
                    VideoComment("vc_b1", "Kai Storm", "kai_storm", "#06B6D4", "Taylor's bridge is unmatched!! 💖🔥", "12m ago", 24, true),
                    VideoComment("vc_b2", "Elena Vance", "elena_v", "#10B981", "The vocal melody is pure bliss ✨", "5m ago", 9, false)
                ),
                createdAtFormatted = "1h ago"
            ),
            BlinkVideo(
                id = "vid_pop_2",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "Blinding Lights City Night ✨",
                caption = "Late night drive with The Weeknd on repeat! Neon vibes all night 🕶️✨",
                hashtags = listOf("#theweeknd", "#blindinglights", "#synthpop", "#nightdrive", "#neon"),
                musicTrack = "Blinding Lights - The Weeknd",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 18,
                likesCount = 2150,
                commentsCount = 192,
                sharesCount = 410,
                isLiked = false,
                isSaved = true,
                filterName = "Neon Heat",
                videoGradientColors = listOf(0xFF1E1B4B, 0xFF4338CA, 0xFF818CF8),
                comments = listOf(
                    VideoComment("vc_b3", "Maya Lin", "maya_lin", "#EC4899", "Best song of the decade hands down 🔥", "30m ago", 45, true)
                ),
                createdAtFormatted = "2h ago"
            ),
            BlinkVideo(
                id = "vid_pop_3",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "As It Was Golden Hour 🌟",
                caption = "Harry Styles acoustic harmonies hitting right in the golden hour feelings 🌅",
                hashtags = listOf("#harrystyles", "#asitwas", "#pop", "#indiepop", "#vibes"),
                musicTrack = "As It Was - Harry Styles",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 25,
                likesCount = 980,
                commentsCount = 76,
                sharesCount = 160,
                isLiked = false,
                isSaved = false,
                videoGradientColors = listOf(0xFF1E1B4B, 0xFF4338CA, 0xFFF43F5E),
                createdAtFormatted = "4h ago"
            ),
            BlinkVideo(
                id = "vid_pop_4",
                authorId = userChloe.id,
                authorName = userChloe.displayName,
                authorUsername = userChloe.username,
                authorAvatarColor = userChloe.avatarColorHex,
                title = "Flowers Rooftop Golden Dance 🌸",
                caption = "I can buy myself flowers, write my name in the sand! Self-love rooftop party with the girls 🌇💐",
                hashtags = listOf("#mileycyrus", "#flowers", "#selflove", "#dance", "#rooftop"),
                musicTrack = "Flowers - Miley Cyrus",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 22,
                likesCount = 6890,
                commentsCount = 430,
                sharesCount = 1540,
                isLiked = true,
                isSaved = true,
                filterName = "Golden Hour",
                videoGradientColors = listOf(0xFF831843, 0xFFDB2777, 0xFFFDE047),
                createdAtFormatted = "3h ago"
            ),
            BlinkVideo(
                id = "vid_pop_5",
                authorId = userAisha.id,
                authorName = userAisha.displayName,
                authorUsername = userAisha.username,
                authorAvatarColor = userAisha.avatarColorHex,
                title = "Roller Skates Nu-Disco Bounce 🪩",
                caption = "Glide, spin, and vibe! Sunset roller skate choreo by the beach promenade 🌴🛼",
                hashtags = listOf("#dualipa", "#levitating", "#rollerskates", "#disco", "#beachvibes"),
                musicTrack = "Levitating - Dua Lipa",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 19,
                likesCount = 8430,
                commentsCount = 560,
                sharesCount = 2190,
                isLiked = true,
                isSaved = true,
                filterName = "Prism Sparkle",
                videoGradientColors = listOf(0xFF4C1D95, 0xFF7C3AED, 0xFFEC4899),
                createdAtFormatted = "4h ago"
            ),
            BlinkVideo(
                id = "vid_pop_6",
                authorId = userLucas.id,
                authorName = userLucas.displayName,
                authorUsername = userLucas.username,
                authorAvatarColor = userLucas.avatarColorHex,
                title = "Acoustic Loop Station Cover 🎸",
                caption = "Building Ed Sheeran's iconic marimba riff using just my acoustic guitar body and a loop pedal! 🎶🎸",
                hashtags = listOf("#edsheeran", "#shapeofyou", "#loopstation", "#guitarcover", "#fingerstyle"),
                musicTrack = "Shape of You - Ed Sheeran",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 26,
                likesCount = 5940,
                commentsCount = 390,
                sharesCount = 1240,
                isLiked = false,
                isSaved = true,
                filterName = "Vintage Kodachrome",
                videoGradientColors = listOf(0xFF78350F, 0xFFD97706, 0xFFFCD34D),
                createdAtFormatted = "5h ago"
            ),
            BlinkVideo(
                id = "vid_pop_7",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "That Vampire Bridge Hits So Hard 🩸",
                caption = "Full volume screams in the car at midnight!! Olivia Rodrigo wrote this from the soul 🚗😭",
                hashtags = listOf("#oliviarodrigo", "#vampire", "#singalong", "#carride", "#poprock"),
                musicTrack = "Vampire - Olivia Rodrigo",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 21,
                likesCount = 7720,
                commentsCount = 620,
                sharesCount = 1980,
                isLiked = true,
                isSaved = true,
                filterName = "Noir 35mm",
                videoGradientColors = listOf(0xFF18181B, 0xFF450A0A, 0xFFDC2626),
                createdAtFormatted = "6h ago"
            ),
            BlinkVideo(
                id = "vid_pop_8",
                authorId = userMe.id,
                authorName = userMe.displayName,
                authorUsername = userMe.username,
                authorAvatarColor = userMe.avatarColorHex,
                title = "Synthwave Night Lights 🕶️",
                caption = "Raindrops on the windshield dancing to The Weeknd's unforgettable 80s synth riff 🌧️🎹",
                hashtags = listOf("#theweeknd", "#saveyourtears", "#nightdrive", "#synthwave", "#chill"),
                musicTrack = "Save Your Tears - The Weeknd",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 23,
                likesCount = 8650,
                commentsCount = 640,
                sharesCount = 2310,
                isLiked = true,
                isSaved = true,
                filterName = "Teal & Orange",
                videoGradientColors = listOf(0xFF0F172A, 0xFF312E81, 0xFF38BDF8),
                createdAtFormatted = "6h ago"
            ),
            BlinkVideo(
                id = "vid_pop_9",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "Festival Mainstage Thunder Drop ⚡🔥",
                caption = "When the drums hit and 50,000 people jumped at the exact same second!! UNREAL energy 🏟️🎆",
                hashtags = listOf("#imaginedragons", "#radioactive", "#festival", "#rock", "#crowd"),
                musicTrack = "Radioactive - Imagine Dragons",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 18,
                likesCount = 10450,
                commentsCount = 890,
                sharesCount = 3670,
                isLiked = true,
                isSaved = true,
                filterName = "Neon Edge",
                videoGradientColors = listOf(0xFF020617, 0xFF1E3A8A, 0xFFF59E0B),
                createdAtFormatted = "7h ago"
            ),
            BlinkVideo(
                id = "vid_pop_10",
                authorId = userLiam.id,
                authorName = userLiam.displayName,
                authorUsername = userLiam.username,
                authorAvatarColor = userLiam.avatarColorHex,
                title = "Golden Hour Skatepark Sunset 🌻",
                caption = "Catching the last rays of sunshine doing tre-flips with friends! Peak summer memories 🛹☀️",
                hashtags = listOf("#postmalone", "#sunflower", "#skatepark", "#goldenhour", "#summer"),
                musicTrack = "Sunflower - Post Malone & Swae Lee",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 20,
                likesCount = 8780,
                commentsCount = 630,
                sharesCount = 2410,
                isLiked = true,
                isSaved = true,
                filterName = "Golden Sunset",
                videoGradientColors = listOf(0xFF78350F, 0xFFD97706, 0xFFF59E0B),
                createdAtFormatted = "8h ago"
            ),
            BlinkVideo(
                id = "vid_pop_11",
                authorId = userSophia.id,
                authorName = userSophia.displayName,
                authorUsername = userSophia.username,
                authorAvatarColor = userSophia.avatarColorHex,
                title = "Vintage Motown Silk Sonic Groove 🚪🎙️",
                caption = "Velvety vocal harmonies and 70s soul warmth on a vintage microphone 🎶💖",
                hashtags = listOf("#brunomars", "#silksonic", "#motown", "#soul", "#vocalcover"),
                musicTrack = "Leave The Door Open - Bruno Mars",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 24,
                likesCount = 6920,
                commentsCount = 490,
                sharesCount = 1680,
                isLiked = true,
                isSaved = false,
                filterName = "Vintage Kodachrome",
                videoGradientColors = listOf(0xFF451A03, 0xFF92400E, 0xFFFBBF24),
                createdAtFormatted = "8h ago"
            ),
            BlinkVideo(
                id = "vid_pop_12",
                authorId = userAisha.id,
                authorName = userAisha.displayName,
                authorUsername = userAisha.username,
                authorAvatarColor = userAisha.avatarColorHex,
                title = "Midnight Shadow Neo-Soul Jam 🗡️",
                caption = "Smooth bassline and guitar chords under moody crimson streetlamps 🌹🌙",
                hashtags = listOf("#sza", "#killbill", "#neosoul", "#vibes", "#nightvibes"),
                musicTrack = "Kill Bill - SZA",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 21,
                likesCount = 8120,
                commentsCount = 590,
                sharesCount = 2240,
                isLiked = true,
                isSaved = true,
                filterName = "Teal & Orange",
                videoGradientColors = listOf(0xFF1E1B4B, 0xFF4C1D95, 0xFFF43F5E),
                createdAtFormatted = "9h ago"
            ),

            // 🎭 Dramatic Videos
            BlinkVideo(
                id = "vid_drama_1",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "Midnight Storm: The Final Choice 🎭",
                caption = "When thunder struck the rooftop, everything changed. Captured in dramatic 4K slow motion ⚡🌧️",
                hashtags = listOf("#dramatic", "#cinematic", "#thriller", "#storm", "#slowmo"),
                musicTrack = "🎭 Dramatic Requiem - Dark Epic Orchestra",
                category = VideoCategory.DRAMATIC,
                isBlink = true,
                durationSeconds = 20,
                likesCount = 3420,
                commentsCount = 312,
                sharesCount = 590,
                isLiked = true,
                isSaved = true,
                filterName = "Dark Cinema",
                videoGradientColors = listOf(0xFF020617, 0xFF1E293B, 0xFF3B82F6),
                comments = listOf(
                    VideoComment("vc_d1", "Alex Rivers", "alex", "#8B5CF6", "The tension build up gave me actual chills 😱", "15m ago", 38, true),
                    VideoComment("vc_d2", "Maya Lin", "maya_lin", "#EC4899", "This looks like a multi-million dollar movie trailer!! 🎬", "8m ago", 52, false)
                ),
                createdAtFormatted = "30m ago"
            ),
            BlinkVideo(
                id = "vid_drama_2",
                authorId = userMe.id,
                authorName = userMe.displayName,
                authorUsername = userMe.username,
                authorAvatarColor = userMe.avatarColorHex,
                title = "Seconds Before Disaster: The Vault ⚡",
                caption = "Ticking clock, heart racing, zero room for error. The ultimate escape challenge ⏳😱",
                hashtags = listOf("#suspense", "#epic", "#hanszimmer", "#escape", "#dramatic"),
                musicTrack = "⚡ Hans Zimmer Suspense - Time Ticking",
                category = VideoCategory.DRAMATIC,
                isBlink = false,
                durationSeconds = 28,
                likesCount = 1890,
                commentsCount = 145,
                sharesCount = 320,
                isLiked = false,
                isSaved = true,
                filterName = "Noir Shadows",
                videoGradientColors = listOf(0xFF18181B, 0xFF27272A, 0xFFDC2626),
                createdAtFormatted = "2h ago"
            ),
            BlinkVideo(
                id = "vid_drama_3",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "Against All Odds: Championship Apex 🏆",
                caption = "Down by 2 points with 3 seconds remaining on the clock. The most dramatic buzzer beater ever! 🏀🔥",
                hashtags = listOf("#drama", "#apex", "#clutch", "#sports", "#champion"),
                musicTrack = "🌩️ Thunderstorm Fury - Cinema Apex",
                category = VideoCategory.DRAMATIC,
                isBlink = true,
                durationSeconds = 16,
                likesCount = 4210,
                commentsCount = 420,
                sharesCount = 890,
                isLiked = true,
                isSaved = false,
                videoGradientColors = listOf(0xFF111827, 0xFF7F1D1D, 0xFFEF4444),
                createdAtFormatted = "4h ago"
            ),
            BlinkVideo(
                id = "vid_drama_4",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "The Dark Alley Encounter 🌑",
                caption = "Someone left a glowing cyber briefcase in the neon rain. Should I open it?! 📦⚡",
                hashtags = listOf("#mystery", "#noir", "#drama", "#cyberpunk", "#cliffhanger"),
                musicTrack = "🌑 Midnight Betrayal - Moody Noir Thriller",
                category = VideoCategory.DRAMATIC,
                isBlink = true,
                durationSeconds = 15,
                likesCount = 2670,
                commentsCount = 380,
                sharesCount = 410,
                isLiked = false,
                isSaved = true,
                thumbnailRes = R.drawable.feed_cyber_snap_1792586101459,
                videoGradientColors = listOf(0xFF0F172A, 0xFF312E81, 0xFF06B6D4),
                createdAtFormatted = "5h ago"
            ),

            // 😂 Funny & Comedy Videos
            BlinkVideo(
                id = "vid_funny_1",
                authorId = userMe.id,
                authorName = userMe.displayName,
                authorUsername = userMe.username,
                authorAvatarColor = userMe.avatarColorHex,
                title = "When the WiFi Cuts Out During Boss Fight 😂",
                caption = "I have never screamed so loud in my entire gaming career 😭💀 Look at my face with the Googly Eyes filter!!",
                hashtags = listOf("#funny", "#gaming", "#comedy", "#fails", "#wifi", "#relatable"),
                musicTrack = "🎪 Circus Ragtime Bounce - Comedy Beats",
                category = VideoCategory.FUNNY,
                isBlink = true,
                durationSeconds = 14,
                likesCount = 5890,
                commentsCount = 640,
                sharesCount = 1200,
                isLiked = true,
                isSaved = true,
                filterName = "Googly Eyes",
                videoGradientColors = listOf(0xFF3F3F46, 0xFFF59E0B, 0xFFEF4444),
                comments = listOf(
                    VideoComment("vc_f1", "Kai Storm", "kai_storm", "#06B6D4", "BRO I AM CRYING LAUGHING AT YOUR EXPRESSION 😭😭", "10m ago", 95, true),
                    VideoComment("vc_f2", "Maya Lin", "maya_lin", "#EC4899", "The music timing when the screen went black is comedy genius!!", "4m ago", 48, false)
                ),
                createdAtFormatted = "1h ago"
            ),
            BlinkVideo(
                id = "vid_funny_2",
                authorId = userMaya.id,
                authorName = userMaya.displayName,
                authorUsername = userMaya.username,
                authorAvatarColor = userMaya.avatarColorHex,
                title = "Trying to Cook Like a 5-Star Chef (Catastrophe) 🍌",
                caption = "Expectation: Gordon Ramsay. Reality: entire kitchen covered in pancake batter 😂🥞",
                hashtags = listOf("#comedy", "#cookingfail", "#funny", "#slapstick", "#pancakes"),
                musicTrack = "🍌 Banana Slip Polka - Cartoon Whistle",
                category = VideoCategory.FUNNY,
                isBlink = false,
                durationSeconds = 22,
                likesCount = 3780,
                commentsCount = 295,
                sharesCount = 740,
                isLiked = true,
                isSaved = false,
                filterName = "Clown Nose",
                videoGradientColors = listOf(0xFF78350F, 0xFFD97706, 0xFFFDE047),
                comments = listOf(
                    VideoComment("vc_f3", "Elena Vance", "elena_v", "#10B981", "HOW did flour get on the CEILING?! 🤣", "20m ago", 60, false)
                ),
                createdAtFormatted = "2h ago"
            ),
            BlinkVideo(
                id = "vid_funny_3",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "My Dog Discovered the Mirror 🐶🪞",
                caption = "He is 100% convinced there is an undercover spy pup living inside the glass! 🤣🐶",
                hashtags = listOf("#dogsoftiktok", "#funnyanimals", "#puppy", "#cute", "#mirror"),
                musicTrack = "🤹 Squeaky Toy March - Funny Animals",
                category = VideoCategory.FUNNY,
                isBlink = true,
                durationSeconds = 16,
                likesCount = 6420,
                commentsCount = 512,
                sharesCount = 1450,
                isLiked = true,
                isSaved = true,
                filterName = "Puppy Ears",
                videoGradientColors = listOf(0xFF1E293B, 0xFF0D9488, 0xFF2DD4BF),
                createdAtFormatted = "3h ago"
            ),
            BlinkVideo(
                id = "vid_funny_4",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "That One Friend Who Can't Whisper 🎺",
                caption = "Bro just announced our private secret to the entire 300-person lecture hall 💀💀",
                hashtags = listOf("#comedy", "#skit", "#funny", "#relatable", "#schoolmemes"),
                musicTrack = "🎺 Kazoo Party Disaster - Comedy Gold",
                category = VideoCategory.FUNNY,
                isBlink = true,
                durationSeconds = 12,
                likesCount = 4890,
                commentsCount = 370,
                sharesCount = 890,
                isLiked = false,
                isSaved = false,
                videoGradientColors = listOf(0xFF31102A, 0xFFBE185D, 0xFFF43F5E),
                createdAtFormatted = "5h ago"
            ),
            BlinkVideo(
                id = "vid_funny_5",
                authorId = userMe.id,
                authorName = userMe.displayName,
                authorUsername = userMe.username,
                authorAvatarColor = userMe.avatarColorHex,
                title = "Goose Chase in Ultra Slow-Motion 🦆🏃",
                caption = "I tried to take an aesthetic photo of the lake and ended up sprinting for my life 😂",
                hashtags = listOf("#goose", "#funny", "#run", "#chase", "#comedy"),
                musicTrack = "🦆 The Quack Attack - Silly Goose Shuffle",
                category = VideoCategory.FUNNY,
                isBlink = false,
                durationSeconds = 18,
                likesCount = 5120,
                commentsCount = 440,
                sharesCount = 1050,
                isLiked = true,
                isSaved = true,
                videoGradientColors = listOf(0xFF064E3B, 0xFF059669, 0xFF34D399),
                createdAtFormatted = "6h ago"
            ),

            // 🎵 Music & Beat Drops
            BlinkVideo(
                id = "vid_music_1",
                authorId = userKai.id,
                authorName = userKai.displayName,
                authorUsername = userKai.username,
                authorAvatarColor = userKai.avatarColorHex,
                title = "Tokyo Neon Speedrun ⚡",
                caption = "Night cruising under Shinjuku neon signs! 130BPM Synthwave pulse on full blast 🚀",
                hashtags = listOf("#blink", "#tokyonights", "#cyberpunk", "#neon", "#speedrun"),
                musicTrack = "⚡ Tokyo Drift Synthwave - Kai Storm",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 14,
                likesCount = 3840,
                commentsCount = 290,
                sharesCount = 640,
                isLiked = true,
                isSaved = true,
                filterName = "Cyber Neon",
                isAiGenerated = true,
                thumbnailRes = R.drawable.feed_cyber_snap_1792586101459,
                videoGradientColors = listOf(0xFF0F172A, 0xFF4338CA, 0xFF06B6D4),
                comments = listOf(
                    VideoComment("vc_1", "Maya Lin", "maya_lin", "#EC4899", "The chromatic aberration transition is unreal! 🔥", "10m ago", 12, true)
                ),
                createdAtFormatted = "1h ago"
            ),
            BlinkVideo(
                id = "vid_music_2",
                authorId = userMaya.id,
                authorName = userMaya.displayName,
                authorUsername = userMaya.username,
                authorAvatarColor = userMaya.avatarColorHex,
                title = "Hyperpop Sparkle Explosion ✨💖",
                caption = "160BPM glittering beat drop with pastel dreamscape visuals! Turn the bass UP 🔊✨",
                hashtags = listOf("#hyperpop", "#sparkle", "#electronic", "#edm", "#kawaii"),
                musicTrack = "💖 Hyperpop Sparkle - Kawaii Glitch",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 15,
                likesCount = 2890,
                commentsCount = 210,
                sharesCount = 520,
                isLiked = true,
                isSaved = true,
                filterName = "Prism Sparkle",
                videoGradientColors = listOf(0xFF831843, 0xFFDB2777, 0xFFF472B6),
                createdAtFormatted = "3h ago"
            ),
            BlinkVideo(
                id = "vid_music_3",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "Acoustic Sunset Fingerstyle Jam 🎸",
                caption = "Live acoustic session on the pier as the golden sun sinks into the ocean 🌅🎶",
                hashtags = listOf("#acoustic", "#fingerstyle", "#guitar", "#chillvibes", "#sunset"),
                musicTrack = "🎸 Golden Coast Sunset - Acoustic Jam",
                category = VideoCategory.MUSIC,
                isBlink = false,
                durationSeconds = 24,
                likesCount = 3120,
                commentsCount = 180,
                sharesCount = 420,
                isLiked = false,
                isSaved = true,
                thumbnailRes = R.drawable.feed_sunset_snap_1792586114190,
                videoGradientColors = listOf(0xFF31102A, 0xFFBE185D, 0xFFF97316),
                createdAtFormatted = "5h ago"
            ),
            BlinkVideo(
                id = "vid_music_4",
                authorId = userElena.id,
                authorName = userElena.displayName,
                authorUsername = userElena.username,
                authorAvatarColor = userElena.avatarColorHex,
                title = "Holographic Aurora Morph 🌌",
                caption = "Ambient chillhop synthesizer pulsing to celestial northern lights 🪐✨",
                hashtags = listOf("#aiart", "#aurora", "#text2video", "#ambient", "#blinkstudio"),
                musicTrack = "🌌 Cosmic Pulsar - Lofi Starlight",
                category = VideoCategory.MUSIC,
                isBlink = true,
                durationSeconds = 16,
                likesCount = 2521,
                commentsCount = 141,
                sharesCount = 278,
                isLiked = false,
                isSaved = true,
                filterName = "Cosmic Glow",
                videoGradientColors = listOf(0xFF030712, 0xFF3B0764, 0xFF14B8A6),
                createdAtFormatted = "6h ago"
            )
        )

        // Seed CRM Customer Accounts
        val cAlex = CustomerAccount(
            userId = userMe.id,
            username = userMe.username,
            displayName = userMe.displayName,
            email = userMe.email,
            tier = CustomerTier.PRO,
            isActive = true,
            isOnline = true,
            location = "Tokyo, Japan",
            totalSpent = 29.97,
            joinedDate = "Sep 2026",
            lastActive = "Active now",
            avatarColorHex = userMe.avatarColorHex
        )

        val cMaya = CustomerAccount(
            userId = userMaya.id,
            username = userMaya.username,
            displayName = userMaya.displayName,
            email = userMaya.email,
            tier = CustomerTier.VIP,
            isActive = true,
            isOnline = true,
            location = "San Francisco, USA",
            totalSpent = 149.97,
            joinedDate = "Jul 2026",
            lastActive = "Active now",
            avatarColorHex = userMaya.avatarColorHex
        )

        val cKai = CustomerAccount(
            userId = userKai.id,
            username = userKai.username,
            displayName = userKai.displayName,
            email = userKai.email,
            tier = CustomerTier.CREATOR,
            isActive = true,
            isOnline = false,
            location = "Berlin, Germany",
            totalSpent = 59.97,
            joinedDate = "Aug 2026",
            lastActive = "2h ago",
            avatarColorHex = userKai.avatarColorHex
        )

        val cElena = CustomerAccount(
            userId = userElena.id,
            username = userElena.username,
            displayName = userElena.displayName,
            email = userElena.email,
            tier = CustomerTier.ENTERPRISE,
            isActive = true,
            isOnline = true,
            location = "London, UK",
            totalSpent = 399.98,
            joinedDate = "May 2026",
            lastActive = "Active now",
            avatarColorHex = userElena.avatarColorHex
        )

        val cLucas = CustomerAccount(
            userId = userLucas.id,
            username = userLucas.username,
            displayName = userLucas.displayName,
            email = userLucas.email,
            tier = CustomerTier.PRO,
            isActive = true,
            isOnline = true,
            location = "Rio de Janeiro, Brazil",
            totalSpent = 19.98,
            joinedDate = "Aug 2026",
            lastActive = "Active now",
            avatarColorHex = userLucas.avatarColorHex
        )

        val cDavid = CustomerAccount(
            userId = "user_david",
            username = "david_nyc",
            displayName = "David Vance",
            email = "david@nycapitol.com",
            tier = CustomerTier.ENTERPRISE,
            isActive = true,
            isOnline = false,
            location = "New York, USA",
            totalSpent = 799.96,
            joinedDate = "Apr 2026",
            lastActive = "Yesterday",
            avatarColorHex = "#06B6D4"
        )

        _customerAccounts.value = listOf(cAlex, cMaya, cKai, cElena, cLucas, cDavid)

        // Seed CRM Payments & Transactions
        _payments.value = listOf(
            PaymentRecord(
                id = "pay_982141",
                customerId = cDavid.userId,
                customerName = cDavid.displayName,
                customerEmail = cDavid.email,
                planName = "Blink Enterprise Annual License",
                amount = 799.96,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 22, 2026",
                paymentMethod = "Wire Transfer •••• 9812"
            ),
            PaymentRecord(
                id = "pay_982142",
                customerId = cElena.userId,
                customerName = cElena.displayName,
                customerEmail = cElena.email,
                planName = "Enterprise Team Pass",
                amount = 199.99,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 20, 2026",
                paymentMethod = "Visa •••• 5521"
            ),
            PaymentRecord(
                id = "pay_982143",
                customerId = cMaya.userId,
                customerName = cMaya.displayName,
                customerEmail = cMaya.email,
                planName = "VIP Gold Annual Pass",
                amount = 149.97,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 18, 2026",
                paymentMethod = "Apple Pay"
            ),
            PaymentRecord(
                id = "pay_982144",
                customerId = cKai.userId,
                customerName = cKai.displayName,
                customerEmail = cKai.email,
                planName = "Creator Filter Pack Upgrade",
                amount = 19.99,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 12, 2026",
                paymentMethod = "Mastercard •••• 1042"
            ),
            PaymentRecord(
                id = "pay_982145",
                customerId = cAlex.userId,
                customerName = cAlex.displayName,
                customerEmail = cAlex.email,
                planName = "Blink Pro Monthly Subscription",
                amount = 9.99,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 10, 2026",
                paymentMethod = "Visa •••• 4242"
            ),
            PaymentRecord(
                id = "pay_982146",
                customerId = cLucas.userId,
                customerName = cLucas.displayName,
                customerEmail = cLucas.email,
                planName = "Blink Pro Monthly Subscription",
                amount = 9.99,
                currency = "USD",
                status = PaymentStatus.COMPLETED,
                dateFormatted = "Oct 08, 2026",
                paymentMethod = "PayPal"
            )
        )

        // Restore offline cache if available on device
        offlineCache?.let { cache ->
            cache.loadCachedPosts()?.let { if (it.isNotEmpty()) _posts.value = it }
            cache.loadCachedCustomers()?.let { if (it.isNotEmpty()) _customerAccounts.value = it }
            cache.loadCachedPayments()?.let { if (it.isNotEmpty()) _payments.value = it }
        }

        // Seed initial incoming friend requests
        val reqChloe = FriendRequest(
            id = "freq_chloe",
            senderId = userChloe.id,
            senderUsername = userChloe.username,
            senderDisplayName = userChloe.displayName,
            senderAvatarColorHex = userChloe.avatarColorHex,
            receiverId = userMe.id,
            receiverUsername = userMe.username,
            status = FriendRequest.STATUS_PENDING,
            timestamp = System.currentTimeMillis() - 3600000L
        )
        val reqJin = FriendRequest(
            id = "freq_jin",
            senderId = userJin.id,
            senderUsername = userJin.username,
            senderDisplayName = userJin.displayName,
            senderAvatarColorHex = userJin.avatarColorHex,
            receiverId = userMe.id,
            receiverUsername = userMe.username,
            status = FriendRequest.STATUS_PENDING,
            timestamp = System.currentTimeMillis() - 7200000L
        )
        _friendRequests.value = listOf(reqChloe, reqJin)

        listenToFirestoreFriendRequests()
    }

    // --- CRM Dashboard & Offline Caching Methods ---
    fun toggleOfflineMode() {
        _isOffline.value = !_isOffline.value
    }

    fun syncOfflineCache() {
        try {
            offlineCache?.saveOfflineState(
                currentUser = _currentUser.value,
                posts = _posts.value,
                stories = _stories.value,
                conversations = _conversations.value,
                messages = _messages.value,
                customers = _customerAccounts.value,
                payments = _payments.value
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "syncOfflineCache error: ${e.message}")
        }
    }

    fun recordPayment(
        customerId: String,
        customerName: String,
        customerEmail: String,
        planName: String,
        amount: Double,
        status: PaymentStatus,
        method: String
    ) {
        val newRecord = PaymentRecord(
            id = "pay_" + UUID.randomUUID().toString().take(8),
            customerId = customerId,
            customerName = customerName,
            customerEmail = customerEmail,
            planName = planName,
            amount = amount,
            currency = "USD",
            status = status,
            dateFormatted = "Just now",
            paymentMethod = method,
            timestamp = System.currentTimeMillis()
        )
        _payments.update { listOf(newRecord) + it }

        // Update customer total spend
        _customerAccounts.update { list ->
            list.map {
                if (it.userId == customerId || it.email.equals(customerEmail, ignoreCase = true)) {
                    it.copy(totalSpent = it.totalSpent + amount)
                } else it
            }
        }
        syncOfflineCache()
    }

    fun updateCustomerTier(userId: String, newTier: CustomerTier) {
        _customerAccounts.update { list ->
            list.map { if (it.userId == userId) it.copy(tier = newTier) else it }
        }
        syncOfflineCache()
    }

    fun toggleCustomerActive(userId: String) {
        _customerAccounts.update { list ->
            list.map { if (it.userId == userId) it.copy(isActive = !it.isActive) else it }
        }
        syncOfflineCache()
    }

    fun getCrmMetrics(): CrmMetrics {
        val customersList = _customerAccounts.value
        val paymentsList = _payments.value
        val totalRevenue = paymentsList.filter { it.status == PaymentStatus.COMPLETED }.sumOf { it.amount }
        val mrr = customersList.sumOf { it.tier.priceMonthly }
        val activeOnline = customersList.count { it.isOnline && it.isActive }
        val arpu = if (customersList.isNotEmpty()) totalRevenue / customersList.size else 0.0

        return CrmMetrics(
            totalCustomers = customersList.size,
            activeUsersCount = activeOnline,
            newTodayCount = 18,
            totalRevenue = totalRevenue,
            monthlyRecurringRevenue = mrr,
            averageRevenuePerUser = arpu,
            paidTransactionsCount = paymentsList.count { it.status == PaymentStatus.COMPLETED }
        )
    }

    // --- Authentication & Account Creation ---
    fun createAccount(username: String, displayName: String, email: String, bio: String = ""): Boolean {
        if (username.isBlank() || email.isBlank()) return false
        val cleanUser = username.trim().removePrefix("@")
        val newUser = User(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = cleanUser,
            displayName = displayName.ifBlank { cleanUser },
            email = email.trim(),
            avatarColorHex = listOf("#8B5CF6", "#06B6D4", "#EC4899", "#10B981", "#F59E0B").random(),
            bio = bio.ifBlank { "New to Blinkchat! ⚡" },
            streaksCount = 1,
            postsCount = 0,
            friendsCount = 1,
            statusText = "Ready to blink ✨",
            isOnline = true
        )
        _allUsers.update { it + newUser }
        _currentUser.value = newUser
        return true
    }

    fun login(identifier: String): Boolean {
        val clean = identifier.trim().lowercase().removePrefix("@")
        val found = _allUsers.value.find { 
            it.username.lowercase() == clean || it.email.lowercase() == clean 
        }
        if (found != null) {
            _currentUser.value = found
            return true
        }
        // If not found, create a demo profile for frictionless sign-in
        val newUser = User(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = clean,
            displayName = clean.replaceFirstChar { it.uppercase() },
            email = "$clean@blinkchat.com",
            avatarColorHex = "#8B5CF6",
            bio = "Hello Blinkchat! 📸✨",
            streaksCount = 1,
            postsCount = 0,
            friendsCount = 3,
            isOnline = true
        )
        _allUsers.update { it + newUser }
        _currentUser.value = newUser
        return true
    }

    fun switchUser(userId: String) {
        val target = _allUsers.value.find { it.id == userId }
        if (target != null) {
            _currentUser.value = target
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // --- Profile Management ---
    fun updateProfile(displayName: String, bio: String, statusText: String, isGhostMode: Boolean) {
        val cur = _currentUser.value ?: return
        val updated = cur.copy(
            displayName = displayName,
            bio = bio,
            statusText = statusText,
            isGhostMode = isGhostMode
        )
        _currentUser.value = updated
        _allUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        _privacySettings.update { it.copy(ghostMode = isGhostMode) }
        syncOfflineCache()
    }

    fun updateDressingAndUpgrade(
        outfitName: String,
        outfitEmoji: String,
        headwear: String,
        accessory: String,
        auraGlow: String,
        bannerTheme: String,
        vipBadge: String,
        avatarColorHex: String,
        newLevel: Int = 5,
        newXp: Int = 850
    ) {
        val cur = _currentUser.value ?: return
        val updated = cur.copy(
            outfitName = outfitName,
            outfitEmoji = outfitEmoji,
            headwear = headwear,
            accessory = accessory,
            auraGlow = auraGlow,
            bannerTheme = bannerTheme,
            vipBadge = vipBadge,
            avatarColorHex = avatarColorHex,
            profileLevel = newLevel,
            profileXp = newXp
        )
        _currentUser.value = updated
        _allUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        syncOfflineCache()
    }

    // --- Friend Request System (Firestore & In-App Sync) ---
    fun searchUsers(query: String): List<User> {
        val q = query.trim().removePrefix("@").lowercase()
        val curId = _currentUser.value?.id ?: ""
        if (q.isEmpty()) return _allUsers.value.filter { it.id != curId }
        return _allUsers.value.filter { user ->
            user.id != curId && (
                user.username.lowercase().contains(q) ||
                user.displayName.lowercase().contains(q)
            )
        }
    }

    fun sendFriendRequest(targetUser: User): Boolean {
        val cur = _currentUser.value ?: return false
        if (targetUser.id == cur.id) return false

        // Check if request already pending or accepted
        val existing = _friendRequests.value.find {
            (it.senderId == cur.id && it.receiverId == targetUser.id) ||
            (it.senderId == targetUser.id && it.receiverId == cur.id)
        }
        if (existing != null && existing.status == FriendRequest.STATUS_PENDING) {
            return false
        }

        val newReq = FriendRequest(
            id = "freq_" + UUID.randomUUID().toString().take(8),
            senderId = cur.id,
            senderUsername = cur.username,
            senderDisplayName = cur.displayName,
            senderAvatarColorHex = cur.avatarColorHex,
            receiverId = targetUser.id,
            receiverUsername = targetUser.username,
            status = FriendRequest.STATUS_PENDING,
            timestamp = System.currentTimeMillis()
        )

        _friendRequests.update { listOf(newReq) + it.filter { r -> r.id != newReq.id } }

        try {
            firestoreDb?.collection("friend_requests")?.document(newReq.id)?.set(newReq.toMap())
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore friend request write: ${e.message}")
        }
        return true
    }

    fun acceptFriendRequest(requestId: String): Boolean {
        val req = _friendRequests.value.find { it.id == requestId } ?: return false
        val cur = _currentUser.value ?: return false

        _friendRequests.update { list ->
            list.map { if (it.id == requestId) it.copy(status = FriendRequest.STATUS_ACCEPTED) else it }
        }

        // Increment friends count for current user
        _currentUser.update { it?.copy(friendsCount = it.friendsCount + 1) }

        // Increment friends count for sender if in _allUsers
        _allUsers.update { list ->
            list.map {
                when (it.id) {
                    req.senderId -> it.copy(friendsCount = it.friendsCount + 1)
                    cur.id -> it.copy(friendsCount = it.friendsCount + 1)
                    else -> it
                }
            }
        }

        // Automatically start or ensure a conversation exists
        val senderUser = _allUsers.value.find { it.id == req.senderId }
        if (senderUser != null) {
            val existingConv = _conversations.value.find { it.participantId == senderUser.id }
            if (existingConv == null) {
                val newConv = Conversation(
                    id = "conv_" + senderUser.id,
                    participantId = senderUser.id,
                    name = senderUser.displayName,
                    username = senderUser.username,
                    avatarColorHex = senderUser.avatarColorHex,
                    lastMessage = "You are now connected! Say hi 👋",
                    lastMessageTime = "Just now",
                    unreadCount = 0,
                    isOnline = senderUser.isOnline
                )
                _conversations.update { listOf(newConv) + it }
            }
        }

        try {
            firestoreDb?.collection("friend_requests")?.document(requestId)?.update(
                mapOf("status" to FriendRequest.STATUS_ACCEPTED)
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore friend request accept: ${e.message}")
        }
        return true
    }

    fun declineFriendRequest(requestId: String): Boolean {
        _friendRequests.update { list ->
            list.map { if (it.id == requestId) it.copy(status = FriendRequest.STATUS_DECLINED) else it }
        }

        try {
            firestoreDb?.collection("friend_requests")?.document(requestId)?.update(
                mapOf("status" to FriendRequest.STATUS_DECLINED)
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore friend request decline: ${e.message}")
        }
        return true
    }

    fun listenToFirestoreFriendRequests() {
        try {
            val db = firestoreDb ?: return
            db.collection("friend_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("BlinkRepo", "Friend requests listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val requests = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: ""
                                val senderUsername = doc.getString("senderUsername") ?: ""
                                val senderDisplayName = doc.getString("senderDisplayName") ?: senderUsername
                                val senderAvatar = doc.getString("senderAvatarColorHex") ?: "#8B5CF6"
                                val receiverId = doc.getString("receiverId") ?: ""
                                val receiverUsername = doc.getString("receiverUsername") ?: ""
                                val status = doc.getString("status") ?: FriendRequest.STATUS_PENDING
                                val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                FriendRequest(
                                    id = id,
                                    senderId = senderId,
                                    senderUsername = senderUsername,
                                    senderDisplayName = senderDisplayName,
                                    senderAvatarColorHex = senderAvatar,
                                    receiverId = receiverId,
                                    receiverUsername = receiverUsername,
                                    status = status,
                                    timestamp = ts
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (requests.isNotEmpty()) {
                            _friendRequests.update { current ->
                                val map = (current + requests).associateBy { it.id }
                                map.values.toList().sortedByDescending { it.timestamp }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("BlinkRepo", "listenToFirestoreFriendRequests init error: ${e.message}")
        }
    }

    // --- Posts & Feed ---
    fun toggleLikePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val newLiked = !post.isLiked
                    val newCount = if (newLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
                    post.copy(isLiked = newLiked, likesCount = newCount)
                } else post
            }
        }
    }

    fun toggleSavePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(isSaved = !post.isSaved) else post
            }
        }
    }

    fun togglePostReaction(postId: String, emoji: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) {
                    val currentReactions = post.reactions.toMutableMap()
                    val prevReaction = post.userReaction
                    val newReaction: String?

                    if (prevReaction == emoji) {
                        // User un-reacts with same emoji
                        val count = (currentReactions[emoji] ?: 1) - 1
                        if (count <= 0) currentReactions.remove(emoji) else currentReactions[emoji] = count
                        newReaction = null
                    } else {
                        // If they had a previous reaction, remove it
                        if (prevReaction != null) {
                            val oldCount = (currentReactions[prevReaction] ?: 1) - 1
                            if (oldCount <= 0) currentReactions.remove(prevReaction) else currentReactions[prevReaction] = oldCount
                        }
                        // Add new reaction
                        currentReactions[emoji] = (currentReactions[emoji] ?: 0) + 1
                        newReaction = emoji
                    }

                    // Also sync isLiked if emoji is heart
                    val newLiked = if (newReaction == "❤️") true else if (prevReaction == "❤️" && newReaction == null) false else post.isLiked
                    val newLikesCount = if (newReaction == "❤️" && !post.isLiked) post.likesCount + 1 
                                        else if (prevReaction == "❤️" && newReaction != "❤️" && post.isLiked) (post.likesCount - 1).coerceAtLeast(0) 
                                        else post.likesCount

                    post.copy(
                        reactions = currentReactions,
                        userReaction = newReaction,
                        isLiked = newLiked,
                        likesCount = newLikesCount
                    )
                } else post
            }
        }
    }

    fun sharePost(postId: String) {
        _posts.update { list ->
            list.map { post ->
                if (post.id == postId) post.copy(sharesCount = post.sharesCount + 1) else post
            }
        }
    }

    fun sharePostToConversation(postId: String, conversationId: String) {
        val post = _posts.value.find { it.id == postId } ?: return
        sharePost(postId)
        sendMessage(
            conversationId = conversationId,
            text = "Shared a Blink by @${post.authorUsername}: \"${post.caption.take(60)}...\" 🔗✨"
        )
    }

    // --- Notifications Management ---
    fun markNotificationAsRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.update { list ->
            list.map { it.copy(isRead = true) }
        }
    }

    fun toggleFollowBackNotification(notificationId: String) {
        _notifications.update { list ->
            list.map { notif ->
                if (notif.id == notificationId) {
                    val newFollowedBack = !notif.isFollowedBack
                    // Update user following count
                    _currentUser.update { user ->
                        user?.let {
                            val change = if (newFollowedBack) 1 else -1
                            it.copy(followingCount = (it.followingCount + change).coerceAtLeast(0))
                        }
                    }
                    notif.copy(isFollowedBack = newFollowedBack)
                } else notif
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        _notifications.update { list ->
            list.filterNot { it.id == notificationId }
        }
    }

    fun followCreator(creatorUsername: String) {
        _currentUser.update { user ->
            user?.copy(followingCount = user.followingCount + 1)
        }
    }


    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        val user = _currentUser.value ?: return
        val newComment = Comment(
            id = "c_" + UUID.randomUUID().toString().take(8),
            postId = postId,
            authorId = user.id,
            authorName = user.displayName,
            authorUsername = user.username,
            authorAvatarColor = user.avatarColorHex,
            text = text.trim(),
            timeAgo = "Just now",
            likesCount = 0,
            isLiked = false
        )
        _comments.update { map ->
            val existing = map[postId] ?: emptyList()
            map + (postId to (existing + newComment))
        }
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        try {
            firestoreDb?.collection("comments")?.document(newComment.id)?.set(
                mapOf(
                    "id" to newComment.id,
                    "postId" to postId,
                    "authorId" to user.id,
                    "authorName" to user.displayName,
                    "authorUsername" to user.username,
                    "text" to text.trim(),
                    "likesCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore comment write: ${e.message}")
        }
    }

    fun createPost(caption: String, filterTag: String, imageUri: String? = null, imageDrawableRes: Int? = null) {
        val user = _currentUser.value ?: return
        val newPost = Post(
            id = "post_" + UUID.randomUUID().toString().take(8),
            authorId = user.id,
            authorName = user.displayName,
            authorUsername = user.username,
            authorAvatarColor = user.avatarColorHex,
            imageDrawableRes = if (imageUri != null) null else (imageDrawableRes ?: R.drawable.feed_cyber_snap_1792586101459),
            imageUri = imageUri,
            caption = caption,
            filterTag = filterTag,
            timeAgo = "Just now",
            likesCount = 1,
            commentsCount = 0,
            isLiked = true,
            isSaved = false
        )
        _posts.update { listOf(newPost) + it }
        _currentUser.update { it?.copy(postsCount = (it.postsCount) + 1) }

        try {
            firestoreDb?.collection("posts")?.document(newPost.id)?.set(
                mapOf(
                    "id" to newPost.id,
                    "authorId" to user.id,
                    "authorName" to user.displayName,
                    "authorUsername" to user.username,
                    "caption" to caption,
                    "filterTag" to filterTag,
                    "imageUri" to (imageUri ?: ""),
                    "likesCount" to 1,
                    "commentsCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore post write: ${e.message}")
        }
    }

    fun createStory(caption: String, filterTag: String, imageUri: String? = null, imageDrawableRes: Int? = null) {
        val user = _currentUser.value ?: return
        val newStory = Story(
            id = "story_" + UUID.randomUUID().toString().take(8),
            userId = user.id,
            username = user.username,
            userDisplayName = user.displayName,
            userAvatarColor = user.avatarColorHex,
            imageDrawableRes = if (imageUri != null) null else (imageDrawableRes ?: R.drawable.feed_sunset_snap_1792586114190),
            imageUri = imageUri,
            caption = caption,
            filterTag = filterTag,
            isViewed = false
        )
        _stories.update { listOf(newStory) + it }
    }

    // --- Group Chat Creation (Firestore backed) ---
    fun createGroupChat(groupTitle: String, participantIds: List<String>): Conversation {
        val curUser = _currentUser.value ?: return _conversations.value.first()
        val allParticipantIds = (participantIds + curUser.id).distinct()
        val members = _allUsers.value.filter { it.id in allParticipantIds }
        val memberDisplayNames = members.map { it.displayName }
        val groupId = "group_" + UUID.randomUUID().toString().take(8)
        val cleanTitle = groupTitle.ifBlank { "⚡ Blink Group Chat" }
        val groupConv = Conversation(
            id = groupId,
            participantId = groupId,
            name = cleanTitle,
            username = "group_" + cleanTitle.lowercase().replace(" ", "_").take(15),
            avatarColorHex = listOf("#06B6D4", "#8B5CF6", "#EC4899", "#10B981", "#F59E0B").random(),
            lastMessage = "${curUser.displayName} created group \"$cleanTitle\" ⚡",
            lastMessageTime = "Just now",
            unreadCount = 0,
            isAiBot = false,
            streak = 1,
            isOnline = true,
            isGroup = true,
            groupTitle = cleanTitle,
            groupMembers = allParticipantIds,
            memberNames = memberDisplayNames
        )
        _conversations.update { listOf(groupConv) + it }

        // Initial welcome message
        val initMsgId = "m_g_" + UUID.randomUUID().toString().take(8)
        val initMsg = ChatMessage(
            id = initMsgId,
            conversationId = groupId,
            senderId = curUser.id,
            senderName = curUser.displayName,
            text = "⚡ Welcome everyone to $cleanTitle! Share your live Blinks and photos here.",
            timeFormatted = "Just now",
            isFromMe = true
        )
        _messages.update { map ->
            map + (groupId to listOf(initMsg))
        }

        // Sync group to Firestore
        try {
            firestoreDb?.collection("groups")?.document(groupId)?.set(
                mapOf(
                    "id" to groupId,
                    "title" to cleanTitle,
                    "createdBy" to curUser.id,
                    "participantIds" to allParticipantIds,
                    "memberNames" to memberDisplayNames,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            firestoreDb?.collection("messages")?.document(initMsgId)?.set(
                mapOf(
                    "id" to initMsgId,
                    "conversationId" to groupId,
                    "senderId" to curUser.id,
                    "senderName" to curUser.displayName,
                    "text" to initMsg.text,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore createGroup error: ${e.message}")
        }
        return groupConv
    }

    // --- Worldwide Location Management ---
    fun updateUserLocation(newLocation: String, newCoordinates: String = "") {
        val cur = _currentUser.value ?: return
        val updated = cur.copy(location = newLocation, coordinates = newCoordinates)
        _currentUser.value = updated
        _allUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    // --- Chat & Messages ---
    fun sendMessage(
        conversationId: String,
        text: String,
        isBlink: Boolean = false,
        mediaType: String = "none",
        mediaUri: String? = null
    ) {
        val user = _currentUser.value ?: return
        val msgId = "m_" + UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val newMsg = ChatMessage(
            id = msgId,
            conversationId = conversationId,
            senderId = user.id,
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = now,
            timeFormatted = timeFormatted,
            isBlink = isBlink,
            blinkSecondsRemaining = if (isBlink) 10 else 0,
            isFromMe = true
        )

        _messages.update { map ->
            val list = map[conversationId] ?: emptyList()
            map + (conversationId to (list + newMsg))
        }

        // Update conversation summary
        val previewText = when {
            isBlink -> "⏳ Sent a Blink disappearing message"
            mediaType == "photo" -> "📸 Sent a photo snap"
            mediaType == "video" -> "🎥 Sent a video snap"
            mediaType == "audio" -> "🎙️ Sent an audio note"
            else -> text
        }
        _conversations.update { convs ->
            convs.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        lastMessage = previewText,
                        lastMessageTime = timeFormatted
                    )
                } else conv
            }
        }

        // Sync with Firestore collection "messages"
        try {
            val db = firestoreDb
            if (db != null) {
                val messageMap = hashMapOf(
                    "id" to msgId,
                    "conversationId" to conversationId,
                    "senderId" to user.id,
                    "senderName" to user.displayName,
                    "text" to text,
                    "mediaUri" to mediaUri,
                    "mediaType" to mediaType,
                    "isBlink" to isBlink,
                    "blinkSecondsRemaining" to (if (isBlink) 10 else 0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "timestamp" to now,
                    "timeFormatted" to timeFormatted
                )
                db.collection("messages").document(msgId).set(messageMap)
                    .addOnFailureListener { e ->
                        Log.w("BlinkRepo", "Firestore sendMessage failed: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore sendMessage exception: ${e.message}")
        }

        // If message is to Blink AI, generate smart response!
        val conv = _conversations.value.find { it.id == conversationId }
        if (conv?.isAiBot == true) {
            scope.launch {
                val currentHistory = (_messages.value[conversationId] ?: emptyList()).map { 
                    it.text to it.isFromMe 
                }
                val aiReply = BlinkAiService.getAiResponse(text, currentHistory)
                val nowAi = System.currentTimeMillis()
                val aiTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(nowAi))
                val aiMsg = ChatMessage(
                    id = "m_ai_" + UUID.randomUUID().toString().take(8),
                    conversationId = conversationId,
                    senderId = "user_ai",
                    text = aiReply,
                    timestamp = nowAi,
                    timeFormatted = aiTime,
                    isFromMe = false
                )
                _messages.update { map ->
                    val list = map[conversationId] ?: emptyList()
                    map + (conversationId to (list + aiMsg))
                }
                _conversations.update { convs ->
                    convs.map { c ->
                        if (c.id == conversationId) {
                            c.copy(lastMessage = aiReply.take(40) + "...", lastMessageTime = aiTime)
                        } else c
                    }
                }
            }
        }
    }

    /**
     * Attaches a real-time Firestore listener for messages in the specified conversation.
     * Merges incoming Firestore messages into the repository state flow.
     */
    fun observeFirestoreMessages(
        conversationId: String,
        onMessagesUpdated: ((List<ChatMessage>) -> Unit)? = null
    ): com.google.firebase.firestore.ListenerRegistration? {
        val db = firestoreDb ?: return null
        return try {
            db.collection("messages")
                .whereEqualTo("conversationId", conversationId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("BlinkRepo", "Firestore messages listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val curUserId = _currentUser.value?.id ?: "user_me"
                        val firestoreMsgs = snapshot.documents.mapNotNull { doc ->
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
                            ChatMessage(
                                id = id,
                                conversationId = conversationId,
                                senderId = senderId,
                                text = text,
                                mediaUri = mediaUri,
                                mediaType = mediaType,
                                timestamp = ts,
                                timeFormatted = formatTime(ts),
                                isBlink = isBlink,
                                blinkSecondsRemaining = blinkSeconds,
                                isFromMe = (senderId == curUserId)
                            )
                        }.sortedBy { it.timestamp }

                        _messages.update { map ->
                            val existing = map[conversationId] ?: emptyList()
                            val firestoreIds = firestoreMsgs.map { it.id }.toSet()
                            val localOnly = existing.filter { it.id !in firestoreIds }
                            (localOnly + firestoreMsgs).sortedBy { it.timestamp }.let { merged ->
                                map + (conversationId to merged)
                            }
                        }
                        onMessagesUpdated?.invoke(firestoreMsgs)
                    }
                }
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Failed to register observeFirestoreMessages: ${e.message}")
            null
        }
    }

    fun startCall(targetUser: User, isVideo: Boolean) {
        _currentCall.value = CallState(
            isActive = true,
            targetUser = targetUser,
            isVideo = isVideo,
            status = "Ringing..."
        )
        // Transition to connected after a quick simulated interval
        scope.launch {
            kotlinx.coroutines.delay(2000)
            if (_currentCall.value.isActive) {
                _currentCall.update { it.copy(status = "Connected") }
            }
        }
    }

    fun toggleMuteCall() {
        _currentCall.update { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleSpeakerCall() {
        _currentCall.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun flipCameraCall() {
        _currentCall.update { it.copy(isCameraFront = !it.isCameraFront) }
    }

    fun endCall() {
        _currentCall.value = CallState(isActive = false, status = "Ended")
    }

    // --- Safety: Block & Report ---
    fun blockUser(user: User) {
        val blocked = BlockedUser(
            userId = user.id,
            username = user.username,
            displayName = user.displayName,
            avatarColorHex = user.avatarColorHex
        )
        _blockedUsers.update { it + blocked }
        // Filter out posts and conversations from this user
        _posts.update { list -> list.filter { it.authorId != user.id } }
        _conversations.update { list -> list.filter { it.participantId != user.id } }
        _stories.update { list -> list.filter { it.userId != user.id } }
    }

    fun unblockUser(userId: String) {
        _blockedUsers.update { it.filter { b -> b.userId != userId } }
    }

    fun reportContent(
        targetUserId: String?,
        targetPostId: String?,
        reason: ReportReason,
        details: String
    ) {
        val user = _currentUser.value ?: return
        val report = SafetyReport(
            id = "rep_" + UUID.randomUUID().toString().take(8),
            reporterId = user.id,
            targetUserId = targetUserId,
            targetPostId = targetPostId,
            reason = reason,
            details = details
        )
        _reports.update { it + report }

        try {
            firestoreDb?.collection("reports")?.document(report.id)?.set(
                mapOf(
                    "id" to report.id,
                    "reporterId" to user.id,
                    "targetUserId" to (targetUserId ?: ""),
                    "targetPostId" to (targetPostId ?: ""),
                    "reason" to reason.name,
                    "details" to details,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore report write: ${e.message}")
        }
    }

    // --- Videos (Blinks & User Posts) ---
    fun toggleLikeVideo(videoId: String) {
        _videos.update { list ->
            list.map { vid ->
                if (vid.id == videoId) {
                    val newLiked = !vid.isLiked
                    val newCount = if (newLiked) vid.likesCount + 1 else maxOf(0, vid.likesCount - 1)
                    vid.copy(isLiked = newLiked, likesCount = newCount)
                } else vid
            }
        }
    }

    fun toggleSaveVideo(videoId: String) {
        _videos.update { list ->
            list.map { vid ->
                if (vid.id == videoId) {
                    vid.copy(isSaved = !vid.isSaved)
                } else vid
            }
        }
    }

    fun addVideoComment(videoId: String, text: String) {
        if (text.isBlank()) return
        val user = _currentUser.value ?: return
        val newComment = VideoComment(
            id = "vc_" + UUID.randomUUID().toString().take(8),
            authorName = user.displayName,
            authorUsername = user.username,
            authorAvatarColor = user.avatarColorHex,
            text = text.trim(),
            timestampFormatted = "Just now",
            likesCount = 0,
            isLiked = false
        )

        _videos.update { list ->
            list.map { vid ->
                if (vid.id == videoId) {
                    vid.copy(
                        comments = listOf(newComment) + vid.comments,
                        commentsCount = vid.commentsCount + 1
                    )
                } else vid
            }
        }
    }

    fun postNewVideo(
        title: String,
        caption: String,
        hashtags: List<String> = emptyList(),
        isBlink: Boolean = true,
        filterName: String? = null,
        musicTrack: String = "⚡ Blink Energy - Original Sound",
        aiPrompt: String? = null,
        thumbnailRes: Int? = null
    ): BlinkVideo? {
        val user = _currentUser.value ?: return null
        val newVideo = BlinkVideo(
            id = "vid_" + UUID.randomUUID().toString().take(8),
            authorId = user.id,
            authorName = user.displayName,
            authorUsername = user.username,
            authorAvatarColor = user.avatarColorHex,
            title = title.ifBlank { "New Blink Video ⚡" },
            caption = caption,
            hashtags = hashtags.ifEmpty { listOf("#blink", "#newvideo") },
            musicTrack = musicTrack,
            isBlink = isBlink,
            durationSeconds = if (isBlink) 12 else 30,
            likesCount = 1,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = true,
            isSaved = false,
            filterName = filterName ?: "Neon Glow",
            isAiGenerated = aiPrompt != null,
            aiPrompt = aiPrompt,
            thumbnailRes = thumbnailRes ?: R.drawable.feed_cyber_snap_1792586101459,
            videoGradientColors = listOf(0xFF1E1B4B, 0xFF6366F1, 0xFF06B6D4),
            comments = emptyList(),
            createdAtFormatted = "Just now"
        )

        _videos.update { listOf(newVideo) + it }
        _currentUser.update { it?.copy(postsCount = (it.postsCount) + 1) }

        try {
            firestoreDb?.collection("videos")?.document(newVideo.id)?.set(
                mapOf(
                    "id" to newVideo.id,
                    "authorId" to user.id,
                    "authorName" to user.displayName,
                    "authorUsername" to user.username,
                    "title" to newVideo.title,
                    "caption" to caption,
                    "isBlink" to isBlink,
                    "likesCount" to 1,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore video write: ${e.message}")
        }

        return newVideo
    }

    fun shareVideoToConversation(videoId: String, conversationId: String) {
        val vid = _videos.value.find { it.id == videoId } ?: return
        sendMessage(
            conversationId = conversationId,
            text = "Shared a video: \"${vid.title}\" 🎬🍿\n${vid.caption}",
            isBlink = vid.isBlink,
            mediaType = "video"
        )
        _videos.update { list ->
            list.map { if (it.id == videoId) it.copy(sharesCount = it.sharesCount + 1) else it }
        }
    }

    // --- Privacy Settings ---
    fun updatePrivacySettings(newSettings: PrivacySettings) {
        _privacySettings.value = newSettings
        _currentUser.update { it?.copy(isGhostMode = newSettings.ghostMode) }
    }

    // --- Anonymous Real-Time Random Blink Chat ---
    val anonymousUserId: String = "anon_" + UUID.randomUUID().toString().take(8)
    val anonymousUserName: String = getRandomAnonymousName()
    val anonymousAvatarColor: String = getRandomAvatarColor()

    private val _randomMatchState = MutableStateFlow(MatchState.IDLE)
    val randomMatchState: StateFlow<MatchState> = _randomMatchState.asStateFlow()

    private val _activeRandomChat = MutableStateFlow<ActiveRandomChat?>(null)
    val activeRandomChat: StateFlow<ActiveRandomChat?> = _activeRandomChat.asStateFlow()

    private val _randomChatMessages = MutableStateFlow<List<RandomChatMessage>>(emptyList())
    val randomChatMessages: StateFlow<List<RandomChatMessage>> = _randomChatMessages.asStateFlow()

    private var myQueueEntryId: String? = null
    private var queueListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var chatDocListener: ListenerRegistration? = null
    private var matchingFallbackJob: Job? = null
    private var simulatedChatJob: Job? = null

    fun startRandomMatch() {
        cleanUpRandomMatch()
        _randomMatchState.value = MatchState.SEARCHING
        _randomChatMessages.value = emptyList()

        val myEntryId = "q_" + UUID.randomUUID().toString().take(8)
        myQueueEntryId = myEntryId

        val db = firestoreDb
        if (db != null) {
            try {
                // Look for an existing waiting peer in match_queue
                db.collection("match_queue")
                    .whereEqualTo("status", "waiting")
                    .limit(5)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val peerDoc = snapshot.documents.firstOrNull { doc ->
                            doc.getString("anonymousId") != anonymousUserId &&
                            doc.getString("status") == "waiting"
                        }

                        if (peerDoc != null) {
                            // Match with waiting peer!
                            val peerId = peerDoc.getString("anonymousId") ?: "peer_anon"
                            val peerName = peerDoc.getString("anonymousName") ?: "Anonymous Blink"
                            val peerAvatar = peerDoc.getString("anonymousAvatarColor") ?: "#06B6D4"
                            val chatId = "chat_" + UUID.randomUUID().toString().take(10)

                            val newChat = ActiveRandomChat(
                                id = chatId,
                                participantA = peerId,
                                participantB = anonymousUserId,
                                participantAName = peerName,
                                participantBName = anonymousUserName,
                                participantAAvatar = peerAvatar,
                                participantBAvatar = anonymousAvatarColor,
                                status = "active",
                                createdAt = System.currentTimeMillis(),
                                expiresAt = System.currentTimeMillis() + 120_000L
                            )

                            // 1. Create active_chat doc
                            db.collection("active_chats").document(chatId).set(
                                mapOf(
                                    "id" to chatId,
                                    "participantA" to peerId,
                                    "participantB" to anonymousUserId,
                                    "participantAName" to peerName,
                                    "participantBName" to anonymousUserName,
                                    "participantAAvatar" to peerAvatar,
                                    "participantBAvatar" to anonymousAvatarColor,
                                    "status" to "active",
                                    "createdAt" to newChat.createdAt,
                                    "expiresAt" to newChat.expiresAt
                                )
                            )

                            // 2. Notify peer
                            peerDoc.reference.update(
                                mapOf(
                                    "status" to "matched",
                                    "matchedChatId" to chatId,
                                    "peerAnonymousName" to anonymousUserName,
                                    "peerAvatarColor" to anonymousAvatarColor
                                )
                            )

                            // 3. Join chat directly
                            joinActiveRandomChat(newChat)
                        } else {
                            // Put myself in queue
                            val myData = mapOf(
                                "id" to myEntryId,
                                "anonymousId" to anonymousUserId,
                                "anonymousName" to anonymousUserName,
                                "anonymousAvatarColor" to anonymousAvatarColor,
                                "status" to "waiting",
                                "joinedAt" to System.currentTimeMillis()
                            )
                            db.collection("match_queue").document(myEntryId).set(myData)

                            // Listen for match confirmation on my queue entry
                            queueListener = db.collection("match_queue").document(myEntryId)
                                .addSnapshotListener { docSnap, error ->
                                    if (error != null || docSnap == null || !docSnap.exists()) return@addSnapshotListener
                                    val status = docSnap.getString("status")
                                    val matchedChatId = docSnap.getString("matchedChatId")
                                    if (status == "matched" && !matchedChatId.isNullOrBlank()) {
                                        val peerName = docSnap.getString("peerAnonymousName") ?: "Anonymous Partner"
                                        val peerAvatar = docSnap.getString("peerAvatarColor") ?: "#06B6D4"
                                        val matchedChat = ActiveRandomChat(
                                            id = matchedChatId,
                                            participantA = anonymousUserId,
                                            participantB = "peer",
                                            participantAName = anonymousUserName,
                                            participantBName = peerName,
                                            participantAAvatar = anonymousAvatarColor,
                                            participantBAvatar = peerAvatar,
                                            status = "active",
                                            createdAt = System.currentTimeMillis(),
                                            expiresAt = System.currentTimeMillis() + 120_000L
                                        )
                                        joinActiveRandomChat(matchedChat)
                                    }
                                }

                            // Responsive fallback for single-user live emulator preview
                            startFallbackPairingTimer()
                        }
                    }
                    .addOnFailureListener {
                        startFallbackPairingTimer()
                    }
            } catch (e: Exception) {
                Log.w("BlinkRepo", "startRandomMatch error: ${e.message}")
                startFallbackPairingTimer()
            }
        } else {
            startFallbackPairingTimer()
        }
    }

    private fun startFallbackPairingTimer() {
        matchingFallbackJob?.cancel()
        matchingFallbackJob = scope.launch {
            delay(2800) // Realistic 2.8s radar search
            if (_randomMatchState.value == MatchState.SEARCHING) {
                val partnerName = getRandomAnonymousName(exclude = anonymousUserName)
                val partnerAvatar = getRandomAvatarColor()
                val chatId = "chat_sim_" + UUID.randomUUID().toString().take(8)

                val simulatedChat = ActiveRandomChat(
                    id = chatId,
                    participantA = anonymousUserId,
                    participantB = "sim_peer_" + UUID.randomUUID().toString().take(4),
                    participantAName = anonymousUserName,
                    participantBName = partnerName,
                    participantAAvatar = anonymousAvatarColor,
                    participantBAvatar = partnerAvatar,
                    status = "active",
                    createdAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + 120_000L
                )

                // Also persist in Firestore active_chats if online
                try {
                    firestoreDb?.collection("active_chats")?.document(chatId)?.set(
                        mapOf(
                            "id" to chatId,
                            "participantA" to simulatedChat.participantA,
                            "participantB" to simulatedChat.participantB,
                            "participantAName" to simulatedChat.participantAName,
                            "participantBName" to simulatedChat.participantBName,
                            "status" to "active",
                            "createdAt" to simulatedChat.createdAt,
                            "expiresAt" to simulatedChat.expiresAt
                        )
                    )
                } catch (_: Exception) {}

                joinActiveRandomChat(simulatedChat)
            }
        }
    }

    private fun joinActiveRandomChat(chat: ActiveRandomChat) {
        matchingFallbackJob?.cancel()
        queueListener?.remove()
        queueListener = null

        // Remove from match queue
        myQueueEntryId?.let { entryId ->
            try {
                firestoreDb?.collection("match_queue")?.document(entryId)?.delete()
            } catch (_: Exception) {}
        }

        _activeRandomChat.value = chat
        _randomMatchState.value = MatchState.MATCHED

        // Welcome system message
        val welcomeMsg = RandomChatMessage(
            id = "sys_welcome",
            chatId = chat.id,
            senderId = "system",
            senderAnonymousName = "Blink System",
            text = "⚡ Anonymous match established! You have 2:00 to chat. Say hello or tap Next to skip.",
            createdAt = System.currentTimeMillis(),
            isFromMe = false
        )
        _randomChatMessages.value = listOf(welcomeMsg)

        // Real-time Firestore snapshot listener on /active_chats/{chatId}/messages
        val db = firestoreDb
        if (db != null) {
            try {
                messagesListener = db.collection("active_chats")
                    .document(chat.id)
                    .collection("messages")
                    .orderBy("createdAt")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener
                        val remoteMessages = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val text = doc.getString("text") ?: return@mapNotNull null
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderAnonymousName") ?: "Anonymous"
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            RandomChatMessage(
                                id = id,
                                chatId = chat.id,
                                senderId = senderId,
                                senderAnonymousName = senderName,
                                text = text,
                                createdAt = createdAt,
                                isFromMe = (senderId == anonymousUserId)
                            )
                        }
                        if (remoteMessages.isNotEmpty()) {
                            _randomChatMessages.update { current ->
                                val existingIds = current.map { it.id }.toSet()
                                val newItems = remoteMessages.filterNot { it.id in existingIds }
                                current + newItems
                            }
                        }
                    }

                // Listen to chat doc for skip/ended
                chatDocListener = db.collection("active_chats").document(chat.id)
                    .addSnapshotListener { doc, error ->
                        if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                        val status = doc.getString("status")
                        if (status == "skipped" && _activeRandomChat.value?.status == "active") {
                            _activeRandomChat.update { it?.copy(status = "skipped") }
                        }
                    }
            } catch (e: Exception) {
                Log.w("BlinkRepo", "joinActiveRandomChat listener error: ${e.message}")
            }
        }

        // If simulated partner, initiate greetings
        if (chat.participantB.startsWith("sim_peer_")) {
            scheduleSimulatedPartnerGreeting(chat)
        }
    }

    private fun scheduleSimulatedPartnerGreeting(chat: ActiveRandomChat) {
        simulatedChatJob?.cancel()
        simulatedChatJob = scope.launch {
            delay(1200)
            if (_activeRandomChat.value?.id == chat.id) {
                val greetings = listOf(
                    "Hey there! Where are you blinking from? ⚡",
                    "Yo! How's your day going? ✨",
                    "Hey! Love the anonymous Blink match feature 🔥",
                    "Hello! What music or videos are you into today? 🎧"
                )
                val reply = greetings.random()
                val peerMsg = RandomChatMessage(
                    id = "msg_" + UUID.randomUUID().toString().take(8),
                    chatId = chat.id,
                    senderId = chat.participantB,
                    senderAnonymousName = chat.participantBName,
                    text = reply,
                    createdAt = System.currentTimeMillis(),
                    isFromMe = false
                )
                _randomChatMessages.update { it + peerMsg }
            }
        }
    }

    fun sendRandomChatMessage(text: String) {
        if (text.isBlank()) return
        val chat = _activeRandomChat.value ?: return

        val newMsg = RandomChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            chatId = chat.id,
            senderId = anonymousUserId,
            senderAnonymousName = anonymousUserName,
            text = text.trim(),
            createdAt = System.currentTimeMillis(),
            isFromMe = true
        )

        _randomChatMessages.update { it + newMsg }

        // Write to Firestore /active_chats/{chatId}/messages
        try {
            firestoreDb?.collection("active_chats")
                ?.document(chat.id)
                ?.collection("messages")
                ?.document(newMsg.id)
                ?.set(
                    mapOf(
                        "id" to newMsg.id,
                        "chatId" to chat.id,
                        "senderId" to anonymousUserId,
                        "senderAnonymousName" to anonymousUserName,
                        "text" to newMsg.text,
                        "createdAt" to newMsg.createdAt
                    )
                )
        } catch (e: Exception) {
            Log.w("BlinkRepo", "Firestore random message write: ${e.message}")
        }

        // If simulated partner, schedule conversational response
        if (chat.participantB.startsWith("sim_peer_")) {
            scope.launch {
                delay(1500 + (text.length * 20L).coerceAtMost(2000L))
                if (_activeRandomChat.value?.id == chat.id) {
                    val responses = listOf(
                        "Haha that's awesome! ⚡",
                        "Nice! I'm listening to some phonk and editing blinks right now 🎧",
                        "Only 2 minutes on this timer, goes fast! ⏳",
                        "Same here! That's super cool 🔥",
                        "Tokyo neon vibes forever ✨ Have you tried the camera shaders?",
                        "Totally agree! Glad we got matched 🙌"
                    )
                    val partnerReply = RandomChatMessage(
                        id = "msg_" + UUID.randomUUID().toString().take(8),
                        chatId = chat.id,
                        senderId = chat.participantB,
                        senderAnonymousName = chat.participantBName,
                        text = responses.random(),
                        createdAt = System.currentTimeMillis(),
                        isFromMe = false
                    )
                    _randomChatMessages.update { it + partnerReply }
                }
            }
        }
    }

    fun skipRandomChat() {
        val currentChat = _activeRandomChat.value
        if (currentChat != null) {
            try {
                firestoreDb?.collection("active_chats")
                    ?.document(currentChat.id)
                    ?.update("status", "skipped")
            } catch (_: Exception) {}
        }
        cleanUpRandomMatch()
        // Immediately search for next random partner!
        startRandomMatch()
    }

    fun cancelRandomMatch() {
        val currentChat = _activeRandomChat.value
        if (currentChat != null) {
            try {
                firestoreDb?.collection("active_chats")
                    ?.document(currentChat.id)
                    ?.update("status", "ended")
            } catch (_: Exception) {}
        }
        cleanUpRandomMatch()
        _randomMatchState.value = MatchState.IDLE
        _activeRandomChat.value = null
        _randomChatMessages.value = emptyList()
    }

    fun reportRandomChat(reason: String, details: String) {
        val currentChat = _activeRandomChat.value
        val peerName = if (currentChat?.participantA == anonymousUserId) currentChat.participantBName else (currentChat?.participantAName ?: "Anonymous")
        
        try {
            val reportId = "rep_" + UUID.randomUUID().toString().take(8)
            firestoreDb?.collection("reports")?.document(reportId)?.set(
                mapOf(
                    "id" to reportId,
                    "targetType" to "random_chat",
                    "chatId" to (currentChat?.id ?: ""),
                    "reportedAnonymousUser" to peerName,
                    "reporterAnonymousId" to anonymousUserId,
                    "reason" to reason,
                    "details" to details,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            currentChat?.id?.let { cid ->
                firestoreDb?.collection("active_chats")?.document(cid)?.update("status", "reported")
            }
        } catch (_: Exception) {}

        cleanUpRandomMatch()
        _randomMatchState.value = MatchState.IDLE
        _activeRandomChat.value = null
        _randomChatMessages.value = emptyList()
    }

    private fun cleanUpRandomMatch() {
        matchingFallbackJob?.cancel()
        matchingFallbackJob = null
        simulatedChatJob?.cancel()
        simulatedChatJob = null
        queueListener?.remove()
        queueListener = null
        messagesListener?.remove()
        messagesListener = null
        chatDocListener?.remove()
        chatDocListener = null

        myQueueEntryId?.let { entryId ->
            try {
                firestoreDb?.collection("match_queue")?.document(entryId)?.delete()
            } catch (_: Exception) {}
        }
        myQueueEntryId = null
    }

    private fun getRandomAnonymousName(exclude: String = ""): String {
        val adjectives = listOf("Neon", "Cyber", "Echo", "Cosmo", "Velvet", "Solar", "Hyper", "Quantum", "Shadow", "Aura", "Stellar", "Pixel")
        val nouns = listOf("Blink", "Nomad", "Surfer", "Phantom", "Pulse", "Flare", "Rider", "Voyager", "Viper", "Echo", "Drifter", "Spark")
        val number = (10..99).random()
        var name = "${adjectives.random()}${nouns.random()} #$number"
        if (name == exclude) {
            name = "${adjectives.random()}${nouns.random()} #${(100..999).random()}"
        }
        return name
    }

    private fun getRandomAvatarColor(): String {
        val colors = listOf("#EC4899", "#06B6D4", "#8B5CF6", "#10B981", "#F59E0B", "#3B82F6", "#EF4444", "#14B8A6")
        return colors.random()
    }

    private fun formatTime(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }
}


