package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.model.BlockedUser
import com.example.data.model.PrivacySettings
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    settings: PrivacySettings,
    blockedUsers: List<BlockedUser>,
    onBack: () -> Unit,
    onUpdateSettings: (PrivacySettings) -> Unit,
    onUnblockUser: (String) -> Unit
) {
    var ghostMode by remember { mutableStateOf(settings.ghostMode) }
    var storyAudience by remember { mutableStateOf(settings.storyAudience) }
    var whoCanMessage by remember { mutableStateOf(settings.whoCanMessage) }
    var whoCanCall by remember { mutableStateOf(settings.whoCanCall) }
    var readReceipts by remember { mutableStateOf(settings.readReceipts) }
    var activeStatus by remember { mutableStateOf(settings.activeStatus) }

    fun notifyChanged() {
        onUpdateSettings(
            PrivacySettings(
                ghostMode = ghostMode,
                storyAudience = storyAudience,
                whoCanMessage = whoCanMessage,
                whoCanCall = whoCanCall,
                readReceipts = readReceipts,
                activeStatus = activeStatus
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy & Safety Controls",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("privacy_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("privacy_settings_list"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // --- Ghost Mode Highlight Banner ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BlinkCyan.copy(alpha = 0.25f),
                                        BlinkPrimaryDark.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👻", fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Ghost Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (ghostMode) "Active • You are invisible" else "Disabled",
                                        fontSize = 12.sp,
                                        color = if (ghostMode) BlinkCyanLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = ghostMode,
                                onCheckedChange = {
                                    ghostMode = it
                                    notifyChanged()
                                },
                                modifier = Modifier.testTag("ghost_mode_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "When Ghost Mode is on, your active presence, online green dot, and approximate location are hidden from everyone. You can still browse snaps and chat freely.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // --- Audience & Privacy Permissions ---
            item {
                Text(
                    text = "Who Can Connect With Me",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Story Audience
                        Column {
                            Text(
                                text = "Who Can View My Blinks & Posts",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Everyone", "Friends Only", "Close Friends").forEach { option ->
                                    val isSelected = storyAudience == option
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            storyAudience = option
                                            notifyChanged()
                                        },
                                        label = { Text(option, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Who can message me
                        Column {
                            Text(
                                text = "Who Can Direct Message Me",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Everyone", "Friends Only").forEach { option ->
                                    val isSelected = whoCanMessage == option
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            whoCanMessage = option
                                            notifyChanged()
                                        },
                                        label = { Text(option, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Who can call me
                        Column {
                            Text(
                                text = "Who Can Call Me (Voice & Video)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Everyone", "Friends Only").forEach { option ->
                                    val isSelected = whoCanCall == option
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            whoCanCall = option
                                            notifyChanged()
                                        },
                                        label = { Text(option, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Receipts & Online Status ---
            item {
                Text(
                    text = "Presence & Indicators",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Read Receipts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Send blue double-checks when you read a friend's message",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = readReceipts,
                                onCheckedChange = {
                                    readReceipts = it
                                    notifyChanged()
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Show Activity Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    text = "Allow friends to see when you are active on the app",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = activeStatus,
                                onCheckedChange = {
                                    activeStatus = it
                                    notifyChanged()
                                }
                            )
                        }
                    }
                }
            }

            // --- Blocked Users Management ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Blocked Users (${blockedUsers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (blockedUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BlinkGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No blocked users",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Anyone you block from chats or posts will show up here.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            blockedUsers.forEach { blocked ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarBadge(name = blocked.displayName, colorHex = blocked.avatarColorHex, size = 36.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = blocked.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = "@${blocked.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onUnblockUser(blocked.userId) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("unblock_${blocked.username}")
                                    ) {
                                        Text("Unblock", fontSize = 12.sp, color = BlinkPrimaryLight)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Safety & Guidelines ---
            item {
                Text(
                    text = "Safety Center & Community",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = BlinkCyanLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Blinkchat Safety Standards", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "• Zero tolerance for harassment, bullying, and graphic violence\n" +
                                   "• Disappearing Blink messages leave no permanent trace\n" +
                                   "• Instant 24/7 report review and content isolation",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
