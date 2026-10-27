package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FriendRequest
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun FriendRequestsDialog(
    currentUser: User,
    friendRequests: List<FriendRequest>,
    onDismiss: () -> Unit,
    onSearchUsers: (String) -> List<User>,
    onSendFriendRequest: (User) -> Boolean,
    onAcceptFriendRequest: (String) -> Boolean,
    onDeclineFriendRequest: (String) -> Boolean
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Find Friends, 1: Pending Requests
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val incomingRequests = remember(friendRequests, currentUser) {
        friendRequests.filter { it.receiverId == currentUser.id && it.status == FriendRequest.STATUS_PENDING }
    }

    val sentRequests = remember(friendRequests, currentUser) {
        friendRequests.filter { it.senderId == currentUser.id && it.status == FriendRequest.STATUS_PENDING }
    }

    // Refresh search results when query changes
    LaunchedEffect(searchQuery) {
        searchResults = onSearchUsers(searchQuery)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan)),
                    shape = RoundedCornerShape(24.dp)
                )
                .testTag("friend_requests_dialog"),
            color = BlinkBgDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BlinkPrimary, BlinkPink))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Friends",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Friend Network ⚡",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 20.sp
                                )
                            )
                            Text(
                                text = "Search by username & manage requests",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BlinkTextSecondary
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("close_friend_requests_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlinkCardDark)
                        .padding(4.dp)
                ) {
                    TabPill(
                        title = "Find Friends 🔍",
                        badgeCount = 0,
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        testTag = "find_friends_tab"
                    ) {
                        selectedTab = 0
                    }
                    TabPill(
                        title = "Requests 📬",
                        badgeCount = incomingRequests.size,
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        testTag = "pending_requests_tab"
                    ) {
                        selectedTab = 1
                    }
                }

                // Feedback Banner
                AnimatedVisibility(
                    visible = feedbackMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    feedbackMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BlinkPrimary.copy(alpha = 0.2f))
                                .border(1.dp, BlinkPrimary, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_search_input"),
                        placeholder = {
                            Text(
                                text = "Search @username or name...",
                                color = BlinkTextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = BlinkPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = BlinkTextSecondary
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BlinkPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = BlinkCardDark,
                            unfocusedContainerColor = BlinkCardDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (searchQuery.isBlank()) "Discover People You May Know" else "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BlinkTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults, key = { it.id }) { targetUser ->
                            val isConnected = friendRequests.any {
                                ((it.senderId == currentUser.id && it.receiverId == targetUser.id) ||
                                 (it.senderId == targetUser.id && it.receiverId == currentUser.id)) &&
                                it.status == FriendRequest.STATUS_ACCEPTED
                            }

                            val isSent = friendRequests.any {
                                it.senderId == currentUser.id &&
                                it.receiverId == targetUser.id &&
                                it.status == FriendRequest.STATUS_PENDING
                            }

                            val hasIncoming = friendRequests.any {
                                it.senderId == targetUser.id &&
                                it.receiverId == currentUser.id &&
                                it.status == FriendRequest.STATUS_PENDING
                            }

                            UserSearchResultCard(
                                user = targetUser,
                                isConnected = isConnected,
                                isSent = isSent,
                                hasIncoming = hasIncoming,
                                onSendRequest = {
                                    val success = onSendFriendRequest(targetUser)
                                    if (success) {
                                        feedbackMessage = "Connection request sent to @${targetUser.username}! 🚀"
                                    } else {
                                        feedbackMessage = "Request already pending with @${targetUser.username}"
                                    }
                                }
                            )
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🔍 No users found",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = "Try searching by exact username (e.g. zane_flux, maya_lin)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BlinkTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Pending Requests Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Incoming Requests (${incomingRequests.size})",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (incomingRequests.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(BlinkCardDark)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Inbox Zero 📬✨",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "No pending incoming friend requests right now.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = BlinkTextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            items(incomingRequests, key = { it.id }) { req ->
                                IncomingRequestCard(
                                    request = req,
                                    onAccept = {
                                        onAcceptFriendRequest(req.id)
                                        feedbackMessage = "Accepted @${req.senderUsername}'s request! You're now connected 🎉"
                                    },
                                    onDecline = {
                                        onDeclineFriendRequest(req.id)
                                        feedbackMessage = "Declined connection request."
                                    }
                                )
                            }
                        }

                        if (sentRequests.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Sent Requests (${sentRequests.size})",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = BlinkTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            items(sentRequests, key = { it.id }) { req ->
                                SentRequestCard(request = req)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    badgeCount: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) Brush.linearGradient(listOf(BlinkPrimary, BlinkPink))
                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isSelected) Color.White else BlinkTextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else BlinkPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) BlinkPrimary else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSearchResultCard(
    user: User,
    isConnected: Boolean,
    isSent: Boolean,
    hasIncoming: Boolean,
    onSendRequest: () -> Unit
) {
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(user.avatarColorHex))
    } catch (e: Exception) {
        BlinkPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlinkCardDark)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("user_card_${user.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (user.vipBadge.isNotBlank()) {
                        Text(text = user.vipBadge, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BlinkCyan,
                        fontSize = 12.sp
                    )
                )
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BlinkTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        when {
            isConnected -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BlinkGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BlinkGreen)
                ) {
                    Text(
                        text = "Friends 🤝",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BlinkGreen,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            isSent -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "Sent ⏳",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BlinkTextSecondary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            hasIncoming -> {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BlinkPink.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, BlinkPink)
                ) {
                    Text(
                        text = "Requested You 📬",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BlinkPink,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
            else -> {
                Button(
                    onClick = onSendRequest,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlinkPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_friend_btn_${user.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val avatarColor = try {
        Color(android.graphics.Color.parseColor(request.senderAvatarColorHex))
    } catch (e: Exception) {
        BlinkPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlinkCardDark)
            .border(1.5.dp, BlinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("incoming_request_${request.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = request.senderDisplayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.senderDisplayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "@${request.senderUsername} wants to connect with you",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BlinkCyan,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .testTag("decline_request_btn_${request.id}"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Decline ✕")
            }

            Button(
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .testTag("accept_request_btn_${request.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlinkPrimary
                )
            ) {
                Text("Accept ⚡", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SentRequestCard(request: FriendRequest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BlinkCardDark.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "@${request.receiverUsername}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "Request sent",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BlinkTextSecondary,
                    fontSize = 11.sp
                )
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f)
        ) {
            Text(
                text = "Pending ⏳",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BlinkTextSecondary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
