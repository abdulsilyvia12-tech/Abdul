package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationType
import com.example.data.model.SocialNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<SocialNotification>,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onToggleFollowBack: (String) -> Unit,
    onDeleteNotification: (String) -> Unit,
    onSendBlinkStreak: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<NotificationType?>(null) }

    val filteredList = remember(notifications, selectedFilter) {
        if (selectedFilter == null) notifications
        else notifications.filter { it.type == selectedFilter }
    }

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("notifications_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$unreadCount",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllAsRead,
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark all as read",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark all read",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${notifications.size})") },
                        leadingIcon = if (selectedFilter == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("filter_all_notifications")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NotificationType.LIKE,
                        onClick = { selectedFilter = if (selectedFilter == NotificationType.LIKE) null else NotificationType.LIKE },
                        label = { Text("Likes ❤️") },
                        modifier = Modifier.testTag("filter_likes_notifications")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NotificationType.COMMENT,
                        onClick = { selectedFilter = if (selectedFilter == NotificationType.COMMENT) null else NotificationType.COMMENT },
                        label = { Text("Comments 💬") },
                        modifier = Modifier.testTag("filter_comments_notifications")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NotificationType.FOLLOW,
                        onClick = { selectedFilter = if (selectedFilter == NotificationType.FOLLOW) null else NotificationType.FOLLOW },
                        label = { Text("Follows 👥") },
                        modifier = Modifier.testTag("filter_follows_notifications")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == NotificationType.STREAK_ALERT,
                        onClick = { selectedFilter = if (selectedFilter == NotificationType.STREAK_ALERT) null else NotificationType.STREAK_ALERT },
                        label = { Text("Streaks 🔥") },
                        modifier = Modifier.testTag("filter_streaks_notifications")
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Notifications List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notifications yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "When people like, comment, or blink, you'll see it here!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        NotificationItemRow(
                            notification = item,
                            onClick = { onMarkAsRead(item.id) },
                            onToggleFollow = { onToggleFollowBack(item.id) },
                            onDelete = { onDeleteNotification(item.id) },
                            onSendBlinkStreak = { onSendBlinkStreak?.invoke(item.actorUsername) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItemRow(
    notification: SocialNotification,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onDelete: () -> Unit,
    onSendBlinkStreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarBg = remember(notification.actorAvatarColor) {
        try {
            Color(android.graphics.Color.parseColor(notification.actorAvatarColor))
        } catch (_: Exception) {
            Color(0xFF6366F1)
        }
    }

    val typeIcon = when (notification.type) {
        NotificationType.LIKE -> Icons.Filled.Favorite
        NotificationType.COMMENT -> Icons.Filled.ChatBubble
        NotificationType.FOLLOW -> Icons.Filled.PersonAdd
        NotificationType.MENTION -> Icons.Filled.AlternateEmail
        NotificationType.STREAK_ALERT -> Icons.Filled.LocalFireDepartment
    }

    val typeIconColor = when (notification.type) {
        NotificationType.LIKE -> Color(0xFFEF4444)
        NotificationType.COMMENT -> Color(0xFF3B82F6)
        NotificationType.FOLLOW -> Color(0xFF10B981)
        NotificationType.MENTION -> Color(0xFF8B5CF6)
        NotificationType.STREAK_ALERT -> Color(0xFFF97316)
    }

    Surface(
        color = if (notification.isRead) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Type Badge
            Box(
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notification.actorName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.actorName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notification.timeAgo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${notification.title} ${if (notification.description.isNotBlank()) "— ${notification.description}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Context Actions based on notification type
                if (notification.type == NotificationType.FOLLOW) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onToggleFollow,
                        colors = if (notification.isFollowedBack) {
                            ButtonDefaults.outlinedButtonColors()
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("follow_back_btn_${notification.id}")
                    ) {
                        Text(
                            text = if (notification.isFollowedBack) "Following ✓" else "Follow Back +",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (notification.type == NotificationType.STREAK_ALERT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = onSendBlinkStreak,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFFF7ED),
                            contentColor = Color(0xFFC2410C)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("streak_rescue_btn_${notification.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFEA580C)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save Streak with Snap 🔥",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Unread indicator dot
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
