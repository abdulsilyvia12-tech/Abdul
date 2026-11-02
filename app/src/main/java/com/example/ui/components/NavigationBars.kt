package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.*

enum class BlinkTab(val title: String, val testTag: String) {
    LOCATION("Location", "tab_location"),
    FEED("Home", "tab_home"),
    CAMERA("Camera", "tab_camera"),
    CHATS("Chats", "tab_chats"),
    VIDEOS("Videos", "tab_videos"),
    PROFILE("Profile", "tab_profile")
}

@Composable
fun BlinkTopBar(
    currentUser: User?,
    isOffline: Boolean = false,
    unreadNotificationsCount: Int = 0,
    onLocationClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onCrmClick: () -> Unit,
    onSafetyCenterClick: () -> Unit,
    onCameraClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Location Feature (Beside Home on the left side) + Brand Logo / Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("app_brand_header")
            ) {
                // Interactive Location Feature Pill beside Home on the left side
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BlinkCyan.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .clickable { onLocationClick() }
                        .testTag("top_location_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = BlinkCyanLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = currentUser?.location?.split(",")?.first() ?: "Location",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlinkCyanLight
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Blink Logo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Blinkchat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Offline mode badge
                if (isOffline) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BlinkPink.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlinkPink.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "⚡ Cached",
                            fontSize = 10.sp,
                            color = BlinkPinkLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right icons: Notifications, CRM Admin, Safety Shield, Quick Camera, Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Social Notifications Center Bell
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("top_notifications_button")
                    ) {
                        Icon(
                            imageVector = if (unreadNotificationsCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = if (unreadNotificationsCount > 0) BlinkPinkLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (unreadNotificationsCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (unreadNotificationsCount > 9) "9+" else "$unreadNotificationsCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // CRM Dashboard Button (Admin)
                IconButton(
                    onClick = onCrmClick,
                    modifier = Modifier.testTag("top_crm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "CRM Dashboard",
                        tint = BlinkPrimaryLight
                    )
                }

                IconButton(
                    onClick = onSafetyCenterClick,
                    modifier = Modifier.testTag("safety_shield_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Safety & Privacy",
                        tint = BlinkCyanLight
                    )
                }

                IconButton(
                    onClick = onCameraClick,
                    modifier = Modifier.testTag("quick_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Open Camera",
                        tint = BlinkPinkLight
                    )
                }

                if (currentUser != null) {
                    AvatarBadge(
                        name = currentUser.displayName,
                        colorHex = currentUser.avatarColorHex,
                        size = 32.dp,
                        onClick = onProfileClick,
                        modifier = Modifier.testTag("header_profile_avatar")
                    )
                }
            }
        }
    }
}

@Composable
fun BlinkBottomBar(
    currentTab: BlinkTab,
    onTabSelected: (BlinkTab) -> Unit,
    unreadMessagesCount: Int = 0
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BlinkTab.values().forEach { tab ->
                val isSelected = currentTab == tab

                Box(
                    modifier = Modifier
                        .testTag(tab.testTag)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = when (tab) {
                                    BlinkTab.LOCATION -> if (isSelected) Icons.Default.LocationOn else Icons.Outlined.LocationOn
                                    BlinkTab.FEED -> if (isSelected) Icons.Default.Home else Icons.Outlined.Home
                                    BlinkTab.CAMERA -> if (isSelected) Icons.Default.CameraAlt else Icons.Outlined.CameraAlt
                                    BlinkTab.CHATS -> if (isSelected) Icons.Default.ChatBubble else Icons.Outlined.ChatBubbleOutline
                                    BlinkTab.VIDEOS -> if (isSelected) Icons.Default.PlayCircle else Icons.Outlined.PlayCircleOutline
                                    BlinkTab.PROFILE -> if (isSelected) Icons.Default.Person else Icons.Outlined.PersonOutline
                                },
                                contentDescription = tab.title,
                                tint = if (isSelected) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )

                            if (tab == BlinkTab.CHATS && unreadMessagesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(BlinkPink),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadMessagesCount",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
