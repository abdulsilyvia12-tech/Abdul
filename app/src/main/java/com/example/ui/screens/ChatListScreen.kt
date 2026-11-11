package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.BlinkMusicPlayer
import com.example.data.model.Conversation
import com.example.data.model.User
import com.example.ui.components.AvatarBadge
import com.example.ui.components.FriendRequestsDialog
import com.example.ui.theme.*

enum class ChatSectionTab {
    CHATS,
    FIND_PEOPLE
}

enum class PeopleSubTab {
    REQUESTS,
    WORLDWIDE_PEOPLE,
    MY_FRIENDS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    conversations: List<Conversation>,
    allUsers: List<User>,
    currentUser: User?,
    onSelectConversation: (Conversation) -> Unit,
    onStartNewChat: (User) -> Unit,
    onCreateGroupChat: (groupTitle: String, participantIds: List<String>) -> Unit,
    onAcceptRequest: (User) -> Unit,
    onBlockUser: (User) -> Unit,
    friendRequests: List<com.example.data.model.FriendRequest> = emptyList(),
    onSearchUsers: ((String) -> List<User>)? = null,
    onSendFriendRequest: ((User) -> Boolean)? = null,
    onAcceptFriendRequest: ((String) -> Boolean)? = null,
    onDeclineFriendRequest: ((String) -> Boolean)? = null
) {
    var activeSection by remember { mutableStateOf(ChatSectionTab.CHATS) }
    var activePeopleSubTab by remember { mutableStateOf(PeopleSubTab.REQUESTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showFriendNetworkDialog by remember { mutableStateOf(false) }

    // Dialog states
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var userToBlockConfirm by remember { mutableStateOf<User?>(null) }
    var groupChatTitle by remember { mutableStateOf("") }
    val selectedGroupMembers = remember { mutableStateListOf<String>() }

    // Track accepted friends & requests state dynamically
    val acceptedUserIds = remember { mutableStateListOf<String>() }
    val pendingRequestUserIds = remember {
        mutableStateListOf("user_lucas", "user_aisha")
    }

    // Audio status while chatting
    val isMusicPlaying by BlinkMusicPlayer.isPlaying.collectAsState()
    val isMusicMuted by BlinkMusicPlayer.isMuted.collectAsState()
    val currentMusicTrack by BlinkMusicPlayer.currentTrack.collectAsState()

    // Filter conversations
    val filteredConversations = conversations.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.lastMessage.contains(searchQuery, ignoreCase = true)
    }

    // Filter people for Find Friends
    val candidateUsers = allUsers.filter { it.id != currentUser?.id }
    val filteredPeople = candidateUsers.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
        it.username.contains(searchQuery, ignoreCase = true) ||
        it.location.contains(searchQuery, ignoreCase = true) ||
        it.bio.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize().testTag("chat_list_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            // --- Background Music Playing Indicator Pill ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .border(1.dp, BlinkCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).clickable { BlinkMusicPlayer.togglePlayPause() }
                    ) {
                        Text(text = if (isMusicPlaying) "🎵" else "🔇", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isMusicPlaying) "Music Playing Live ⚡" else "Music In Background",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMusicPlaying) BlinkCyanLight else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentMusicTrack,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { BlinkMusicPlayer.togglePlayPause() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMusicPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = BlinkPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { BlinkMusicPlayer.toggleMute() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMusicMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // --- Section Switcher: Chats vs Find Friends & Requests ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Chats tab
                val isChats = activeSection == ChatSectionTab.CHATS
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { activeSection = ChatSectionTab.CHATS }
                        .testTag("tab_chats"),
                    color = if (isChats) BlinkPrimary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = if (isChats) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chats (${conversations.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isChats) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Find Friends & Requests tab
                val isPeople = activeSection == ChatSectionTab.FIND_PEOPLE
                Surface(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { activeSection = ChatSectionTab.FIND_PEOPLE }
                        .testTag("tab_find_friends"),
                    color = if (isPeople) BlinkPrimary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = if (isPeople) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Find Friends & Requests",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isPeople) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        if (pendingRequestUserIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(BlinkPink),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${pendingRequestUserIds.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar (searches conversations or users by name/username)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (activeSection == ChatSectionTab.CHATS) "Search chats & messages..."
                        else "Search by name or @username worldwide...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = BlinkPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("chat_search_bar"),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BlinkPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // --- VIEW A: CHATS & GROUPS LIST ---
            if (activeSection == ChatSectionTab.CHATS) {
                val matchingFriends = remember(candidateUsers, searchQuery) {
                    if (searchQuery.isBlank()) emptyList()
                    else candidateUsers.filter {
                        it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.username.contains(searchQuery, ignoreCase = true) ||
                        it.location.contains(searchQuery, ignoreCase = true) ||
                        it.bio.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("conversations_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    // When not searching, show "Friends & Contacts" quick bar
                    if (searchQuery.isBlank()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "👥 Friends & Contacts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BlinkCyan.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "${candidateUsers.size}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BlinkCyanLight,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = { showNewChatDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp).testTag("see_all_friends_button")
                                    ) {
                                        Text("Find more", fontSize = 11.sp, color = BlinkPrimaryLight, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = BlinkPrimaryLight)
                                    }
                                }

                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("friends_quick_contacts_row"),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    // Start New Chat Action Chip
                                    item {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(62.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { showNewChatDialog = true }
                                                .testTag("quick_new_chat_button")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(BlinkPrimary.copy(alpha = 0.15f))
                                                    .border(1.5.dp, BlinkPrimary, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "New Chat",
                                                    tint = BlinkPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "New Chat",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    items(candidateUsers.take(15), key = { it.id }) { user ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(62.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onStartNewChat(user) }
                                                .testTag("quick_friend_${user.username}")
                                        ) {
                                            AvatarBadge(
                                                name = user.displayName,
                                                colorHex = user.avatarColorHex,
                                                size = 46.dp,
                                                showOnline = true,
                                                isOnline = user.isOnline
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = user.displayName.split(" ").firstOrNull() ?: user.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Blink AI Featured Pinned Card
                        val aiConv = conversations.find { it.isAiBot }
                        if (aiConv != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onSelectConversation(aiConv) }
                                        .testTag("blink_ai_chat_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Brush.linearGradient(listOf(BlinkPrimaryDark.copy(alpha = 0.35f), BlinkCyan.copy(alpha = 0.15f))))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Brush.radialGradient(listOf(BlinkPrimary, BlinkCyan))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White, modifier = Modifier.size(26.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "Blink AI Copilot", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(8.dp), color = BlinkPrimary) {
                                                    Text(text = "AI ⚡", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = aiConv.lastMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = BlinkPrimaryLight)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }

                    // SEARCH RESULTS: Matching Active Conversations
                    if (filteredConversations.isNotEmpty()) {
                        if (searchQuery.isNotBlank()) {
                            item {
                                Text(
                                    text = "💬 Chats (${filteredConversations.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(filteredConversations.filter { !it.isAiBot }, key = { it.id }) { conv ->
                            ConversationItem(
                                conversation = conv,
                                onClick = { onSelectConversation(conv) }
                            )
                        }
                    }

                    // SEARCH RESULTS: Matching Friends & Contacts (to start a new conversation!)
                    if (searchQuery.isNotBlank() && matchingFriends.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "👥 Friends & Contacts (${matchingFriends.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BlinkPrimaryLight,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }

                        items(matchingFriends, key = { "search_friend_${it.id}" }) { friend ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onStartNewChat(friend) }
                                    .testTag("search_friend_item_${friend.username}"),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarBadge(
                                        name = friend.displayName,
                                        colorHex = friend.avatarColorHex,
                                        size = 46.dp,
                                        showOnline = true,
                                        isOnline = friend.isOnline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friend.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "@${friend.username} • 📍 ${friend.location}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (friend.bio.isNotBlank()) {
                                            Text(
                                                text = friend.bio,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onStartNewChat(friend) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("start_chat_friend_${friend.username}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Empty Search State
                    if (searchQuery.isNotBlank() && filteredConversations.isEmpty() && matchingFriends.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No chats or friends matching \"$searchQuery\"",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.testTag("chat_search_empty_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- VIEW B: FIND FRIENDS, REQUESTS, ACCEPT & BLOCK ---
            if (activeSection == ChatSectionTab.FIND_PEOPLE) {
                // Open Dedicated Friend Network Studio Dialog Button
                val liveIncomingCount = friendRequests.count { it.receiverId == currentUser?.id && it.status == com.example.data.model.FriendRequest.STATUS_PENDING }
                Button(
                    onClick = { showFriendNetworkDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("open_friend_network_studio_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search @username & Manage Requests", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (liveIncomingCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BlinkPink
                        ) {
                            Text(
                                text = "$liveIncomingCount pending",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Sub-filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeopleSubTab.values().forEach { subTab ->
                        val isSel = activePeopleSubTab == subTab
                        val label = when (subTab) {
                            PeopleSubTab.REQUESTS -> "Requests (${pendingRequestUserIds.size})"
                            PeopleSubTab.WORLDWIDE_PEOPLE -> "🌍 Worldwide"
                            PeopleSubTab.MY_FRIENDS -> "Friends"
                        }
                        FilterChip(
                            selected = isSel,
                            onClick = { activePeopleSubTab = subTab },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlinkPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("people_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 6.dp)
                ) {
                    val displayList = when (activePeopleSubTab) {
                        PeopleSubTab.REQUESTS -> filteredPeople.filter { it.id in pendingRequestUserIds }
                        PeopleSubTab.MY_FRIENDS -> filteredPeople.filter { it.id in acceptedUserIds }
                        PeopleSubTab.WORLDWIDE_PEOPLE -> filteredPeople.filter { it.id !in pendingRequestUserIds }
                    }

                    if (displayList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No users matching \"$searchQuery\""
                                           else "No pending requests right now ✨",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    items(displayList, key = { it.id }) { user ->
                        val isPending = user.id in pendingRequestUserIds
                        val isAlreadyFriend = user.id in acceptedUserIds

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_card_${user.username}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarBadge(
                                        name = user.displayName,
                                        colorHex = user.avatarColorHex,
                                        size = 48.dp,
                                        showOnline = true,
                                        isOnline = user.isOnline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "@${user.username}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // Worldwide location badge
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = "📍 ${user.location}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BlinkCyanLight
                                            )
                                        }
                                    }
                                }

                                if (user.bio.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = user.bio,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons: Accept / Add Friend / Block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPending) {
                                        // Accept button
                                        Button(
                                            onClick = {
                                                pendingRequestUserIds.remove(user.id)
                                                acceptedUserIds.add(user.id)
                                                onAcceptRequest(user)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = BlinkGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("accept_request_${user.username}")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Block button
                                        OutlinedButton(
                                            onClick = { userToBlockConfirm = user },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlinkPink),
                                            border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(38.dp).testTag("block_request_${user.username}")
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Block", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Standard Worldwide User Card
                                        Button(
                                            onClick = {
                                                if (!isAlreadyFriend) {
                                                    acceptedUserIds.add(user.id)
                                                }
                                                onStartNewChat(user)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAlreadyFriend) BlinkPrimary else BlinkCyan
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1.2f).height(38.dp).testTag("chat_user_${user.username}")
                                        ) {
                                            Icon(
                                                imageVector = if (isAlreadyFriend) Icons.Default.Chat else Icons.Default.PersonAdd,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isAlreadyFriend) "Message" else "Add Friend",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Block button
                                        OutlinedButton(
                                            onClick = { userToBlockConfirm = user },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BlinkPink),
                                            border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(0.8f).height(38.dp).testTag("block_user_${user.username}")
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Block", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Bottom FAB: Start Chat & Create Group Chat ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // "Create Group Chat" Button
            FloatingActionButton(
                onClick = {
                    groupChatTitle = ""
                    selectedGroupMembers.clear()
                    showCreateGroupDialog = true
                },
                containerColor = BlinkCyan,
                contentColor = Color.White,
                modifier = Modifier.size(48.dp).testTag("create_group_chat_fab")
            ) {
                Icon(imageVector = Icons.Default.GroupAdd, contentDescription = "New Group Chat", modifier = Modifier.size(22.dp))
            }

            // New 1-on-1 Chat FAB
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = BlinkPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(imageVector = Icons.Default.AddComment, contentDescription = "New Chat")
            }
        }

        // --- Create Group Chat Modal (Firestore Backed) ---
        if (showCreateGroupDialog) {
            AlertDialog(
                onDismissRequest = { showCreateGroupDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = BlinkCyanLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Named Group Chat", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Name your group room and pick friends to join in real-time Firestore conversation:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = groupChatTitle,
                            onValueChange = { groupChatTitle = it },
                            placeholder = { Text("e.g. Tokyo Night Blinks ⚡") },
                            modifier = Modifier.fillMaxWidth().testTag("group_title_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Text(
                            text = "Select Friends (${selectedGroupMembers.size} selected):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(candidateUsers) { user ->
                                val isChecked = user.id in selectedGroupMembers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (isChecked) selectedGroupMembers.remove(user.id)
                                            else selectedGroupMembers.add(user.id)
                                        }
                                        .background(if (isChecked) BlinkPrimary.copy(alpha = 0.12f) else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedGroupMembers.add(user.id)
                                            else selectedGroupMembers.remove(user.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AvatarBadge(name = user.displayName, colorHex = user.avatarColorHex, size = 32.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = user.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(text = "📍 ${user.location}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (groupChatTitle.isNotBlank()) {
                                onCreateGroupChat(groupChatTitle, selectedGroupMembers.toList())
                                showCreateGroupDialog = false
                            }
                        },
                        enabled = groupChatTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                        modifier = Modifier.testTag("submit_create_group_button")
                    ) {
                        Text("Create Room ⚡")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateGroupDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Start 1-on-1 Chat Searchable Friend Picker Modal ---
        if (showNewChatDialog) {
            SearchableFriendPickerDialog(
                candidateUsers = candidateUsers,
                onDismiss = { showNewChatDialog = false },
                onSelectUser = { user ->
                    showNewChatDialog = false
                    onStartNewChat(user)
                }
            )
        }

        // --- Block User Confirmation Dialog ---
        if (userToBlockConfirm != null) {
            val u = userToBlockConfirm!!
            AlertDialog(
                onDismissRequest = { userToBlockConfirm = null },
                title = { Text("Block @${u.username}?", fontWeight = FontWeight.Bold, color = BlinkPink) },
                text = {
                    Text(
                        "Are you sure you want to block ${u.displayName}? They won't be able to send you messages, Blinks, or see your posts.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingRequestUserIds.remove(u.id)
                            acceptedUserIds.remove(u.id)
                            onBlockUser(u)
                            userToBlockConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPink)
                    ) {
                        Text("Block User")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToBlockConfirm = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- Friend Network & Requests Dialog ---
        if (showFriendNetworkDialog && currentUser != null && onSearchUsers != null) {
            FriendRequestsDialog(
                currentUser = currentUser,
                friendRequests = friendRequests,
                onDismiss = { showFriendNetworkDialog = false },
                onSearchUsers = onSearchUsers,
                onSendFriendRequest = onSendFriendRequest ?: { false },
                onAcceptFriendRequest = onAcceptFriendRequest ?: { false },
                onDeclineFriendRequest = onDeclineFriendRequest ?: { false }
            )
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("conversation_item_${conversation.username}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AvatarBadge(
                    name = conversation.name,
                    colorHex = conversation.avatarColorHex,
                    size = 50.dp,
                    showOnline = !conversation.isGroup,
                    isOnline = conversation.isOnline
                )
                if (conversation.isGroup) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(BlinkCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = "Group", tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = conversation.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (conversation.isGroup) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BlinkCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "👥 Group",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlinkCyanLight,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (conversation.streak > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🔥 ${conversation.streak}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlinkAmber
                            )
                        }
                    }

                    Text(
                        text = conversation.lastMessageTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 13.sp,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(BlinkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conversation.unreadCount}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Searchable friend picker dialog allowing users to search by name, @username, location,
 * filter by online status, and immediately start conversations with specific contacts.
 */
@Composable
fun SearchableFriendPickerDialog(
    candidateUsers: List<User>,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showOnlyOnline by remember { mutableStateOf(false) }

    val filteredFriends = remember(candidateUsers, query, showOnlyOnline) {
        candidateUsers.filter { user ->
            val matchesQuery = query.isBlank() ||
                user.displayName.contains(query, ignoreCase = true) ||
                user.username.contains(query, ignoreCase = true) ||
                user.location.contains(query, ignoreCase = true) ||
                user.bio.contains(query, ignoreCase = true)
            val matchesOnline = !showOnlyOnline || user.isOnline
            matchesQuery && matchesOnline
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("searchable_friends_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = BlinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start a Conversation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BlinkPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${candidateUsers.size} friends",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlinkPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Search input field
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search friends by name, @username...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search friends",
                            tint = BlinkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_friends_input")
                        .testTag("friend_search_bar"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlinkPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showOnlyOnline,
                        onClick = { showOnlyOnline = false },
                        label = { Text("All (${candidateUsers.size})", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_all_friends")
                    )
                    val onlineCount = candidateUsers.count { it.isOnline }
                    FilterChip(
                        selected = showOnlyOnline,
                        onClick = { showOnlyOnline = true },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BlinkGreen)
                            )
                        },
                        label = { Text("Online ($onlineCount)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_online_friends")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredFriends.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (query.isNotBlank()) "No friends matching \"$query\"" else "No friends available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.testTag("no_friends_found_text")
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("searchable_friends_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFriends, key = { it.id }) { user ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectUser(user) }
                                    .testTag("friend_contact_item_${user.username}"),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarBadge(
                                        name = user.displayName,
                                        colorHex = user.avatarColorHex,
                                        size = 44.dp,
                                        showOnline = true,
                                        isOnline = user.isOnline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (user.isOnline) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Online",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = BlinkGreen
                                                )
                                            }
                                        }
                                        Text(
                                            text = "@${user.username} • 📍 ${user.location}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (user.bio.isNotBlank()) {
                                            Text(
                                                text = user.bio,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { onSelectUser(user) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("start_chat_button_${user.username}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_friends_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}
