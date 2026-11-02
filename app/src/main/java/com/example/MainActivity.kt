package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.data.model.MatchState
import com.example.data.model.Story
import com.example.data.model.User
import com.example.data.repository.BlinkRepository
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    private val repository by lazy { BlinkRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                BlinkApp(repository = repository)
            }
        }
    }
}

@Composable
fun BlinkApp(repository: BlinkRepository) {
    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val allUsers by repository.allUsers.collectAsStateWithLifecycle()
    val posts by repository.posts.collectAsStateWithLifecycle()
    val stories by repository.stories.collectAsStateWithLifecycle()
    val commentsMap by repository.comments.collectAsStateWithLifecycle()
    val conversations by repository.conversations.collectAsStateWithLifecycle()
    val messagesMap by repository.messages.collectAsStateWithLifecycle()
    val privacySettings by repository.privacySettings.collectAsStateWithLifecycle()
    val blockedUsers by repository.blockedUsers.collectAsStateWithLifecycle()
    val callState by repository.currentCall.collectAsStateWithLifecycle()
    val videos by repository.videos.collectAsStateWithLifecycle()
    val friendRequests by repository.friendRequests.collectAsStateWithLifecycle()
    val notifications by repository.notifications.collectAsStateWithLifecycle()

    // Real-Time Anonymous Random Matching & Chat State (Firestore Powered)
    val randomMatchState by repository.randomMatchState.collectAsStateWithLifecycle()
    val activeRandomChat by repository.activeRandomChat.collectAsStateWithLifecycle()
    val randomChatMessages by repository.randomChatMessages.collectAsStateWithLifecycle()

    // CRM & Offline State
    val customerAccounts by repository.customerAccounts.collectAsStateWithLifecycle()
    val payments by repository.payments.collectAsStateWithLifecycle()
    val isOffline by repository.isOffline.collectAsStateWithLifecycle()

    // Navigation sub-states
    var currentTab by remember { mutableStateOf(BlinkTab.FEED) }
    var activeStory by remember { mutableStateOf<Story?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var isPrivacySettingsOpen by remember { mutableStateOf(false) }
    var isVoiceChatOpen by remember { mutableStateOf(false) }
    var isCrmOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }

    // --- Active Real-Time Anonymous Random Chat Overlay (Zero Profile Needed) ---
    if (activeRandomChat != null) {
        AnonymousRandomChatScreen(
            chat = activeRandomChat!!,
            messages = randomChatMessages,
            myAnonymousId = repository.anonymousUserId,
            myAnonymousName = repository.anonymousUserName,
            onSendMessage = { repository.sendRandomChatMessage(it) },
            onSkipNext = { repository.skipRandomChat() },
            onReport = { reason, details -> repository.reportRandomChat(reason, details) },
            onEndChat = { repository.cancelRandomMatch() }
        )
        return
    }

    // --- Radar Pulse Matching Dialog for Anonymous Blink Search ---
    if (randomMatchState == MatchState.SEARCHING) {
        RandomBlinkSearchingDialog(
            anonymousName = repository.anonymousUserName,
            anonymousAvatarColor = repository.anonymousAvatarColor,
            onCancel = { repository.cancelRandomMatch() }
        )
    }

    // --- CRM Dashboard Screen Overlay (Protected by Password: Snail1961) ---
    if (isCrmOpen) {
        BackHandler {
            isCrmOpen = false
        }
        CrmDashboardScreen(
            customers = customerAccounts,
            payments = payments,
            metrics = repository.getCrmMetrics(),
            onRecordPayment = { custId, name, email, plan, amount, status, method ->
                repository.recordPayment(custId, name, email, plan, amount, status, method)
            },
            onUpdateCustomerTier = { userId, tier ->
                repository.updateCustomerTier(userId, tier)
            },
            onToggleCustomerActive = { userId ->
                repository.toggleCustomerActive(userId)
            },
            onClose = { isCrmOpen = false }
        )
        return
    }

    // Auto-restore authenticated Firebase user session on app launch
    LaunchedEffect(Unit) {
        val fbUser = Firebase.auth.currentUser
        if (fbUser != null && currentUser == null) {
            repository.loginWithFirebase(fbUser)
        }
    }

    // --- Firebase Authentication & Customer Portal ---
    if (currentUser == null) {
        AuthScreen(
            onLoginSuccess = { identifier ->
                repository.login(identifier)
            },
            onFirebaseLoginSuccess = { firebaseUser ->
                repository.loginWithFirebase(firebaseUser)
            },
            onCreateAccountSuccess = { username, displayName, email, bio ->
                repository.createAccount(username, displayName, email, bio)
            },
            onOpenCrm = {
                isCrmOpen = true
            },
            onContinueOffline = {
                repository.toggleOfflineMode()
                repository.login("alex")
            },
            onStartAnonymousBlink = {
                repository.startRandomMatch()
            },
            demoUsers = allUsers
        )
        return
    }

    // --- Active Audio / Video Call Screen Overlay ---
    if (callState.isActive) {
        BackHandler {
            repository.endCall()
        }
        CallScreen(
            callState = callState,
            onEndCall = { repository.endCall() },
            onToggleMute = { repository.toggleMuteCall() },
            onToggleSpeaker = { repository.toggleSpeakerCall() },
            onFlipCamera = { repository.flipCameraCall() }
        )
        return
    }

    // --- Interactive Voice Conversation Overlay ---
    if (isVoiceChatOpen) {
        BackHandler {
            isVoiceChatOpen = false
        }
        VoiceConversationScreen(
            onClose = { isVoiceChatOpen = false }
        )
        return
    }

    // --- Fullscreen Story Viewer Overlay ---
    if (activeStory != null) {
        BackHandler {
            activeStory = null
        }
        StoryViewerScreen(
            story = activeStory!!,
            onClose = { activeStory = null },
            onSendReply = { replyText ->
                val conv = conversations.find { it.participantId == activeStory!!.userId }
                if (conv != null) {
                    repository.sendMessage(conv.id, "Replied: $replyText")
                }
            }
        )
        return
    }

    // --- Fullscreen Chat Screen Overlay (Live Firestore Synchronized) ---
    if (activeConversation != null) {
        BackHandler {
            activeConversation = null
        }
        val conv = activeConversation!!
        ChatScreen(
            conversation = conv,
            currentUser = currentUser,
            onBack = { activeConversation = null },
            repository = repository
        )
        return
    }

    // --- Privacy & Safety Settings Screen Overlay ---
    if (isPrivacySettingsOpen) {
        BackHandler {
            isPrivacySettingsOpen = false
        }
        PrivacySettingsScreen(
            settings = privacySettings,
            blockedUsers = blockedUsers,
            onBack = { isPrivacySettingsOpen = false },
            onUpdateSettings = { newSettings ->
                repository.updatePrivacySettings(newSettings)
            },
            onUnblockUser = { userId ->
                repository.unblockUser(userId)
            }
        )
        return
    }

    // Handle back button on secondary tabs -> return to Home (Feed)
    if (currentTab != BlinkTab.FEED) {
        BackHandler {
            currentTab = BlinkTab.FEED
        }
    }

    // --- Main Tab Navigation Shell ---
    Scaffold(
        topBar = {
            if (currentTab != BlinkTab.CAMERA && currentTab != BlinkTab.VIDEOS) {
                BlinkTopBar(
                    currentUser = currentUser,
                    isOffline = isOffline,
                    unreadNotificationsCount = notifications.count { !it.isRead },
                    onLocationClick = { currentTab = BlinkTab.LOCATION },
                    onNotificationsClick = { isNotificationsOpen = true },
                    onCrmClick = { isCrmOpen = true },
                    onSafetyCenterClick = { isPrivacySettingsOpen = true },
                    onCameraClick = { currentTab = BlinkTab.CAMERA },
                    onProfileClick = { currentTab = BlinkTab.PROFILE }
                )
            }
        },
        bottomBar = {
            if (currentTab != BlinkTab.CAMERA) {
                val unreadTotal = conversations.sumOf { it.unreadCount }
                BlinkBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    unreadMessagesCount = unreadTotal
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (currentTab != BlinkTab.CAMERA) innerPadding.calculateBottomPadding() else 0.dp,
                    top = if (currentTab != BlinkTab.CAMERA && currentTab != BlinkTab.VIDEOS) innerPadding.calculateTopPadding() else 0.dp
                )
        ) {

            when (currentTab) {
                // Location Feature (Beside Home on the left side)
                BlinkTab.LOCATION -> {
                    LocationScreen(
                        currentUser = currentUser,
                        friends = allUsers.filter { it.id != currentUser?.id },
                        onUpdateLocation = { locName, coords ->
                            repository.updateUserLocation(locName, coords)
                        },
                        onToggleGhostMode = { isGhost ->
                            repository.updateProfile(
                                displayName = currentUser?.displayName ?: "",
                                bio = currentUser?.bio ?: "",
                                statusText = currentUser?.statusText ?: "",
                                isGhostMode = isGhost
                            )
                        },
                        onStartChat = { friend ->
                            val existing = conversations.find { it.participantId == friend.id }
                            if (existing != null) {
                                activeConversation = existing
                            } else {
                                val newConv = Conversation(
                                    id = "conv_" + friend.id,
                                    participantId = friend.id,
                                    name = friend.displayName,
                                    username = friend.username,
                                    avatarColorHex = friend.avatarColorHex,
                                    lastMessage = "Started location chat 📍",
                                    lastMessageTime = "Just now",
                                    unreadCount = 0,
                                    isOnline = friend.isOnline
                                )
                                activeConversation = newConv
                            }
                        }
                    )
                }

                // Home Tab (Feed & Stories combined with Social Media Hub)
                BlinkTab.FEED -> {
                    FeedScreen(
                        currentUser = currentUser,
                        stories = stories,
                        posts = posts,
                        commentsMap = commentsMap,
                        conversations = conversations,
                        onLikePost = { postId -> repository.toggleLikePost(postId) },
                        onReactionClick = { postId, emoji -> repository.togglePostReaction(postId, emoji) },
                        onSavePost = { postId -> repository.toggleSavePost(postId) },
                        onAddComment = { postId, text -> repository.addComment(postId, text) },
                        onSharePostToConversation = { postId, convId -> repository.sharePostToConversation(postId, convId) },
                        onFollowCreator = { creatorUsername -> repository.followCreator(creatorUsername) },
                        onOpenStory = { story -> activeStory = story },
                        onAddStoryClick = { currentTab = BlinkTab.CAMERA },
                        onCreatePostClick = { currentTab = BlinkTab.CAMERA },
                        onBlockUser = { userToBlock ->
                            repository.blockUser(userToBlock)
                        },
                        onReportPost = { postId, authorName ->
                            repository.reportContent(
                                targetUserId = null,
                                targetPostId = postId,
                                reason = com.example.data.model.ReportReason.INAPPROPRIATE,
                                details = "Reported post by $authorName"
                            )
                        },
                        onUseTrendingSound = { soundTitle ->
                            currentTab = BlinkTab.CAMERA
                        }
                    )
                }

                BlinkTab.CAMERA -> {
                    CameraScreen(
                        onPostCreated = { caption, filterTag, _, imageUri ->
                            repository.createPost(caption, filterTag, imageUri = imageUri)
                            repository.createStory(caption, filterTag, imageUri = imageUri)
                            currentTab = BlinkTab.FEED
                        },
                        onCreateVideo = { title, caption, tags, isBlink, filter, music ->
                            repository.postNewVideo(
                                title = title,
                                caption = caption,
                                hashtags = tags,
                                isBlink = isBlink,
                                filterName = filter,
                                musicTrack = music
                            )
                            currentTab = BlinkTab.VIDEOS
                        },
                        onCloseCamera = {
                            currentTab = BlinkTab.FEED
                        }
                    )
                }

                BlinkTab.CHATS -> {
                    ChatListScreen(
                        conversations = conversations,
                        allUsers = allUsers,
                        currentUser = currentUser,
                        onSelectConversation = { conv ->
                            activeConversation = conv
                        },
                        onStartNewChat = { targetUser ->
                            val existing = conversations.find { it.participantId == targetUser.id }
                            if (existing != null) {
                                activeConversation = existing
                            } else {
                                val newConv = Conversation(
                                    id = "conv_" + targetUser.id,
                                    participantId = targetUser.id,
                                    name = targetUser.displayName,
                                    username = targetUser.username,
                                    avatarColorHex = targetUser.avatarColorHex,
                                    lastMessage = "Started chat ⚡",
                                    lastMessageTime = "Just now",
                                    unreadCount = 0,
                                    isOnline = targetUser.isOnline
                                )
                                activeConversation = newConv
                            }
                        },
                        onCreateGroupChat = { groupTitle, memberIds ->
                            val createdGroup = repository.createGroupChat(groupTitle, memberIds)
                            activeConversation = createdGroup
                        },
                        onAcceptRequest = { user ->
                            val existing = conversations.find { it.participantId == user.id }
                            if (existing != null) {
                                activeConversation = existing
                            } else {
                                val newConv = Conversation(
                                    id = "conv_" + user.id,
                                    participantId = user.id,
                                    name = user.displayName,
                                    username = user.username,
                                    avatarColorHex = user.avatarColorHex,
                                    lastMessage = "Connected as friends ⚡",
                                    lastMessageTime = "Just now",
                                    unreadCount = 0,
                                    isOnline = user.isOnline
                                )
                                activeConversation = newConv
                            }
                        },
                        onBlockUser = { user ->
                            repository.blockUser(user)
                        },
                        friendRequests = friendRequests,
                        onSearchUsers = { query -> repository.searchUsers(query) },
                        onSendFriendRequest = { targetUser -> repository.sendFriendRequest(targetUser) },
                        onAcceptFriendRequest = { reqId -> repository.acceptFriendRequest(reqId) },
                        onDeclineFriendRequest = { reqId -> repository.declineFriendRequest(reqId) }
                    )
                }

                BlinkTab.VIDEOS -> {
                    BlinkVideosScreen(
                        videos = videos,
                        currentUser = currentUser,
                        conversations = conversations,
                        onLikeVideo = { videoId -> repository.toggleLikeVideo(videoId) },
                        onSaveVideo = { videoId -> repository.toggleSaveVideo(videoId) },
                        onAddComment = { videoId, text -> repository.addVideoComment(videoId, text) },
                        onShareVideo = { videoId, convId -> repository.shareVideoToConversation(videoId, convId) },
                        onCreateVideo = { title, caption, tags, isBlink, filter, music ->
                            repository.postNewVideo(
                                title = title,
                                caption = caption,
                                hashtags = tags,
                                isBlink = isBlink,
                                filterName = filter,
                                musicTrack = music
                            )
                        }
                    )
                }

                BlinkTab.PROFILE -> {
                    currentUser?.let { user ->
                        ProfileScreen(
                            user = user,
                            userPosts = posts,
                            onOpenPrivacySettings = { isPrivacySettingsOpen = true },
                            onOpenCrm = { isCrmOpen = true },
                            onUpdateProfile = { name, bio, status, isGhost ->
                                repository.updateProfile(name, bio, status, isGhost)
                            },
                            onUpdateLocation = { locName, coords ->
                                repository.updateUserLocation(locName, coords)
                            },
                            onUpdateDressing = { outfitName, outfitEmoji, headwear, accessory, auraGlow, bannerTheme, vipBadge, avatarColorHex, newLevel, newXp ->
                                repository.updateDressingAndUpgrade(outfitName, outfitEmoji, headwear, accessory, auraGlow, bannerTheme, vipBadge, avatarColorHex, newLevel, newXp)
                            },
                            friendRequests = friendRequests,
                            onSearchUsers = { query -> repository.searchUsers(query) },
                            onSendFriendRequest = { targetUser -> repository.sendFriendRequest(targetUser) },
                            onAcceptFriendRequest = { reqId -> repository.acceptFriendRequest(reqId) },
                            onDeclineFriendRequest = { reqId -> repository.declineFriendRequest(reqId) },
                            onLogout = {
                                repository.logout()
                            }
                        )
                    }
                }
            }

            // ⚡ One-Click Floating "Blink" Match Button (Always Accessible at Bottom)
            if (currentTab != BlinkTab.CAMERA && currentTab != BlinkTab.VIDEOS) {
                RandomBlinkMatchFloatingButton(
                    onClick = { repository.startRandomMatch() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }

        // --- Social Notifications & Activity Center Bottom Sheet ---
        if (isNotificationsOpen) {
            NotificationsSheet(
                notifications = notifications,
                onDismiss = { isNotificationsOpen = false },
                onMarkAsRead = { notifId -> repository.markNotificationAsRead(notifId) },
                onMarkAllAsRead = { repository.markAllNotificationsAsRead() },
                onToggleFollowBack = { notifId -> repository.toggleFollowBackNotification(notifId) },
                onDeleteNotification = { notifId -> repository.deleteNotification(notifId) },
                onSendBlinkStreak = {
                    isNotificationsOpen = false
                    currentTab = BlinkTab.CAMERA
                }
            )
        }
    }
}

