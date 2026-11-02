package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Post
import com.example.data.model.User
import com.example.ui.components.AvatarBadge
import com.example.ui.components.FollowersListDialog
import com.example.ui.components.FriendRequestsDialog
import com.example.ui.components.ProfileDressingDialog
import com.example.ui.components.SocialUserItem
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    userPosts: List<Post>,
    onOpenPrivacySettings: () -> Unit,
    onOpenCrm: (() -> Unit)? = null,
    onUpdateProfile: (displayName: String, bio: String, statusText: String, isGhostMode: Boolean) -> Unit,
    onUpdateLocation: ((String, String) -> Unit)? = null,
    onUpdateDressing: ((outfitName: String, outfitEmoji: String, headwear: String, accessory: String, auraGlow: String, bannerTheme: String, vipBadge: String, avatarColorHex: String, newLevel: Int, newXp: Int) -> Unit)? = null,
    friendRequests: List<com.example.data.model.FriendRequest> = emptyList(),
    onSearchUsers: ((String) -> List<User>)? = null,
    onSendFriendRequest: ((User) -> Boolean)? = null,
    onAcceptFriendRequest: ((String) -> Boolean)? = null,
    onDeclineFriendRequest: ((String) -> Boolean)? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDressingDialog by remember { mutableStateOf(false) }
    var showFriendNetworkDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }
    var showShareProfileSnackbar by remember { mutableStateOf(false) }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var customCityInput by remember { mutableStateOf("") }

    val sampleFollowers = remember {
        listOf(
            SocialUserItem("u_1", "Maya Lin", "mayalin", "#EC4899", "Tokyo aesthetic & DJ sets ✨", isVerified = true, isFollowing = true),
            SocialUserItem("u_2", "Kai Storm", "kaistorm", "#06B6D4", "Phonk producer & speed enthusiast ⚡", isVerified = true, isFollowing = true),
            SocialUserItem("u_3", "Zane Vance", "zanevance", "#10B981", "Synthwave artist 🌆", isVerified = true, isFollowing = false),
            SocialUserItem("u_4", "Elena Vance", "elenavance", "#8B5CF6", "Visual effects & street style 🎨", isVerified = false, isFollowing = false),
            SocialUserItem("u_5", "Liam Harper", "liamharper", "#F59E0B", "Film skater London 🛹", isVerified = false, isFollowing = true)
        )
    }

    val sampleFollowing = remember {
        listOf(
            SocialUserItem("u_1", "Maya Lin", "mayalin", "#EC4899", "Tokyo aesthetic & DJ sets ✨", isVerified = true, isFollowing = true),
            SocialUserItem("u_2", "Kai Storm", "kaistorm", "#06B6D4", "Phonk producer & speed enthusiast ⚡", isVerified = true, isFollowing = true),
            SocialUserItem("u_5", "Liam Harper", "liamharper", "#F59E0B", "Film skater London 🛹", isVerified = false, isFollowing = true)
        )
    }

    // Edit fields
    var editName by remember { mutableStateOf(user.displayName) }
    var editBio by remember { mutableStateOf(user.bio) }
    var editStatus by remember { mutableStateOf(user.statusText) }
    var editGhost by remember { mutableStateOf(user.isGhostMode) }

    // Popular major worldwide cities
    val worldwideCities = remember {
        listOf(
            "Tokyo, Japan" to "35.6762° N, 139.6503° E",
            "San Francisco, USA" to "37.7749° N, 122.4194° W",
            "London, UK" to "51.5074° N, 0.1278° W",
            "Paris, France" to "48.8566° N, 2.3522° E",
            "Berlin, Germany" to "52.5200° N, 13.4050° E",
            "Seoul, South Korea" to "37.5665° N, 126.9780° E",
            "Rio de Janeiro, Brazil" to "22.9068° S, 43.1729° W",
            "Nairobi, Kenya" to "1.2921° S, 36.8219° E",
            "Sydney, Australia" to "33.8688° S, 151.2093° E",
            "Dubai, UAE" to "25.2048° N, 55.2708° E",
            "New York, USA" to "40.7128° N, 74.0060° W",
            "Cairo, Egypt" to "30.0444° N, 31.2357° E",
            "Toronto, Canada" to "43.6532° N, 79.3832° W",
            "Singapore" to "1.3521° N, 103.8198° E",
            "Mumbai, India" to "19.0760° N, 72.8777° E"
        )
    }

    // Location Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                isDetectingLocation = true
                detectRealLocation(context) { locName, coords ->
                    onUpdateLocation?.invoke(locName, coords)
                    isDetectingLocation = false
                    showLocationDialog = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("profile_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bannerGradientColors = remember(user.bannerTheme) {
            when {
                user.bannerTheme.contains("Synthwave") -> listOf(Color(0xFF2E0854), Color(0xFF6B21A8), Color(0xFF06B6D4))
                user.bannerTheme.contains("Andromeda") -> listOf(Color(0xFF0F172A), Color(0xFF3B0764), Color(0xFFEC4899))
                user.bannerTheme.contains("Metropolis") -> listOf(Color(0xFF020617), Color(0xFF1E1B4B), Color(0xFF06B6D4))
                user.bannerTheme.contains("Miami") -> listOf(Color(0xFF4C0519), Color(0xFFBE185D), Color(0xFFF59E0B))
                user.bannerTheme.contains("Kyoto") -> listOf(Color(0xFF1E293B), Color(0xFF991B1B), Color(0xFFF43F5E))
                user.bannerTheme.contains("Electric") -> listOf(Color(0xFF030712), Color(0xFF1E3A8A), Color(0xFF38BDF8))
                else -> listOf(Color(0xFF1E1B4B), Color(0xFF6B21A8), Color(0xFF06B6D4))
            }
        }
        val auraColor = remember(user.auraGlow) {
            try {
                Color(android.graphics.Color.parseColor(user.auraGlow))
            } catch (e: Exception) {
                BlinkPrimary
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic Profile Banner Backdrop & VIP Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clickable { showDressingDialog = true }
                .testTag("profile_banner_backdrop"),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Brush.horizontalGradient(bannerGradientColors))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🌌", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = user.bannerTheme.split(" ").take(2).joinToString(" "),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lv.${user.profileLevel} • ${user.vipBadge}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlinkAmber
                            )
                        }
                    }
                }
            }
        }

        // Dressed Avatar with customized Aura Glow, Headwear & Accessory
        Box(
            modifier = Modifier
                .clickable { showDressingDialog = true }
                .testTag("dressed_profile_avatar"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(102.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(listOf(auraColor, BlinkPink, auraColor))
                    )
                    .padding(3.5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                AvatarBadge(
                    name = user.displayName,
                    colorHex = user.avatarColorHex,
                    size = 92.dp
                )
            }

            // Headwear badge floating on top
            if (user.headwear.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-14).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.5.dp, auraColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = user.headwear.takeLast(2).trim(), fontSize = 16.sp)
                }
            }

            // Accessory badge on bottom right
            if (user.accessory.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(1.5.dp, BlinkCyanLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = user.accessory.takeLast(2).trim(), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Name & Username with Verified Creator Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = user.displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (user.isVerified) {
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified Creator",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "@${user.username}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (user.isGhostMode) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BlinkCyan.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "👻 Ghost Mode",
                        fontSize = 10.sp,
                        color = BlinkCyanLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Equipped Outfit & Style Badge
        Surface(
            modifier = Modifier
                .padding(top = 6.dp)
                .clickable { showDressingDialog = true }
                .testTag("equipped_outfit_pill"),
            shape = RoundedCornerShape(12.dp),
            color = BlinkPrimary.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, BlinkPrimary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = user.outfitEmoji, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${user.outfitName} • ${user.vipBadge}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlinkPrimaryLight
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Tune, contentDescription = null, tint = BlinkPrimaryLight, modifier = Modifier.size(12.dp))
            }
        }

        // Worldwide Location Badge (Identifies location wherever user is worldwide)
        Surface(
            modifier = Modifier
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showLocationDialog = true }
                .testTag("profile_location_pill"),
            shape = RoundedCornerShape(16.dp),
            color = BlinkCyan.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${user.location} (${user.coordinates})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlinkCyanLight
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.EditLocationAlt,
                    contentDescription = "Change City",
                    tint = BlinkCyanLight.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Status pill
        Surface(
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = user.statusText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        // Bio
        if (user.bio.isNotBlank()) {
            Text(
                text = user.bio,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 10.dp, start = 20.dp, end = 20.dp),
                lineHeight = 18.sp
            )
        }

        // Social Media Showcase Links
        if (user.socialLinks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                user.socialLinks.forEach { link ->
                    val (iconEmoji, label) = when {
                        link.contains("instagram") -> "📸" to "@alex.chen"
                        link.contains("tiktok") -> "🎵" to "@alextravels"
                        link.contains("youtube") -> "▶️" to "AlexChenVlogs"
                        else -> "🔗" to "Link"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { showShareProfileSnackbar = true }
                            .testTag("social_link_$label")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = iconEmoji, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Social Media Stats Row (Followers, Following, Blinks, Streaks)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showFollowersDialog = true }
                        .testTag("stat_followers_count")
                ) {
                    Text(
                        text = "${user.followersCount}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BlinkCyanLight
                    )
                    Text(text = "Followers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { showFollowingDialog = true }
                        .testTag("stat_following_count")
                ) {
                    Text(
                        text = "${user.followingCount}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BlinkPrimaryLight
                    )
                    Text(text = "Following", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${user.postsCount}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BlinkPinkLight
                    )
                    Text(text = "Blinks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🔥 ${user.streaksCount}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = BlinkAmber
                    )
                    Text(text = "Streak", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Dressing Room & Profile Upgrades Button
        Button(
            onClick = { showDressingDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_dressing_room_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(text = "👗", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dressing Room & Upgrades ⚡", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BlinkAmber.copy(alpha = 0.25f)
            ) {
                Text(
                    text = "Lv.${user.profileLevel}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = BlinkAmber,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Wardrobe & VIP Status Showcase Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDressingDialog = true }
                .testTag("wardrobe_status_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Wardrobe & Style", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "✨", fontSize = 12.sp)
                    }
                    Text(text = "Customize ⚡", fontSize = 11.sp, color = BlinkPrimaryLight, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = user.outfitEmoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = user.outfitName.split(" ").take(2).joinToString(" "), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Outfit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = user.headwear.takeLast(2).trim(), fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = user.headwear.replace(user.headwear.takeLast(2), "").trim().split(" ").take(2).joinToString(" "), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Headwear", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = user.accessory.takeLast(2).trim(), fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = user.accessory.replace(user.accessory.takeLast(2), "").trim().split(" ").take(2).joinToString(" "), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Accessory", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(auraColor)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Aura", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Glow FX", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Friend Requests & User Search Button
        val incomingCount = friendRequests.count { it.receiverId == user.id && it.status == com.example.data.model.FriendRequest.STATUS_PENDING }
        OutlinedButton(
            onClick = { showFriendNetworkDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("open_friend_requests_btn"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, if (incomingCount > 0) BlinkPink else BlinkCyan.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (incomingCount > 0) BlinkPink.copy(alpha = 0.12f) else BlinkCyan.copy(alpha = 0.08f)
            )
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = if (incomingCount > 0) BlinkPink else BlinkCyanLight, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Find Friends & Requests ⚡", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            if (incomingCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BlinkPink
                ) {
                    Text(
                        text = "$incomingCount new",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row (Edit, Share Profile, Safety Center)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    editName = user.displayName
                    editBio = user.bio
                    editStatus = user.statusText
                    editGhost = user.isGhostMode
                    showEditSheet = true
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("edit_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showShareProfileSnackbar = true },
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .testTag("share_profile_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = BlinkCyanLight, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share Profile", color = BlinkCyanLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOpenPrivacySettings,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("privacy_settings_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = BlinkPrimaryLight, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Safety", color = BlinkPrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // User's Blinks & Posts Grid Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Blinks & Highlights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${userPosts.filter { it.authorId == user.id }.size} posts",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val myPosts = userPosts.filter { it.authorId == user.id }
        if (myPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No Blinks posted yet! Tap the Camera to create one ⚡", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                myPosts.forEach { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                if (!post.imageUri.isNullOrBlank()) {
                                    AsyncImage(
                                        model = post.imageUri,
                                        contentDescription = "Post",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (post.imageDrawableRes != null) {
                                    Image(
                                        painter = painterResource(id = post.imageDrawableRes),
                                        contentDescription = "Post",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = post.caption, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                Text(text = "Filter: ${post.filterTag} • ❤️ ${post.likesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CRM Dashboard Card
        if (onOpenCrm != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCrm() }
                    .testTag("profile_crm_dashboard_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BlinkPrimary.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BlinkPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BlinkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "CRM & Revenue Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Active users telemetry & payments (Snail1961)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BlinkPrimaryLight)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Customer Login Portal / Switch Account
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_switch_customer_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Switch Customer / Open Portal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logout
        TextButton(
            onClick = onLogout,
            modifier = Modifier.testTag("logout_button")
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = BlinkPink, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Out from Blinkchat", color = BlinkPink, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // --- Worldwide Location Selection Dialog ---
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = BlinkCyanLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Worldwide Location", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Identify and display your location accurately anywhere in the world on your Blinkchat account:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Auto-Detect Real GPS Location Button
                    Button(
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) {
                                coroutineScope.launch {
                                    isDetectingLocation = true
                                    detectRealLocation(context) { locName, coords ->
                                        onUpdateLocation?.invoke(locName, coords)
                                        isDetectingLocation = false
                                        showLocationDialog = false
                                    }
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auto_detect_location_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isDetectingLocation) "Detecting Device GPS..." else "Detect My Device Location")
                    }

                    // Custom City Entry
                    OutlinedTextField(
                        value = customCityInput,
                        onValueChange = { customCityInput = it },
                        placeholder = { Text("Or type any city (e.g. Kyoto, Japan)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (customCityInput.isNotBlank()) {
                                IconButton(onClick = {
                                    onUpdateLocation?.invoke(customCityInput.trim(), "Worldwide")
                                    showLocationDialog = false
                                }) {
                                    Icon(Icons.Default.Check, contentDescription = "Save", tint = BlinkGreen)
                                }
                            }
                        }
                    )

                    Text(text = "Or choose a global city:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(worldwideCities) { (city, coords) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onUpdateLocation?.invoke(city, coords)
                                        showLocationDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "📍", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = city, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(text = coords, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- Edit Profile Sheet ---
    if (showEditSheet) {
        ModalBottomSheet(onDismissRequest = { showEditSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editStatus,
                    onValueChange = { editStatus = it },
                    label = { Text("Status Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Ghost Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Hide live active status from friends", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = editGhost,
                        onCheckedChange = { editGhost = it }
                    )
                }

                Button(
                    onClick = {
                        onUpdateProfile(editName, editBio, editStatus, editGhost)
                        showEditSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // --- Profile Dressing Room & Upgrades Dialog ---
    if (showDressingDialog) {
        ProfileDressingDialog(
            user = user,
            onDismiss = { showDressingDialog = false },
            onSaveDressing = { outfitName, outfitEmoji, headwear, accessory, auraGlow, bannerTheme, vipBadge, avatarColorHex, newLevel, newXp ->
                onUpdateDressing?.invoke(outfitName, outfitEmoji, headwear, accessory, auraGlow, bannerTheme, vipBadge, avatarColorHex, newLevel, newXp)
            }
        )
    }

    // --- Friend Requests & User Search Dialog ---
    if (showFriendNetworkDialog && onSearchUsers != null) {
        FriendRequestsDialog(
            currentUser = user,
            friendRequests = friendRequests,
            onDismiss = { showFriendNetworkDialog = false },
            onSearchUsers = onSearchUsers,
            onSendFriendRequest = onSendFriendRequest ?: { false },
            onAcceptFriendRequest = onAcceptFriendRequest ?: { false },
            onDeclineFriendRequest = onDeclineFriendRequest ?: { false }
        )
    }

    // --- Interactive Followers Showcase Dialog ---
    if (showFollowersDialog) {
        FollowersListDialog(
            title = "Followers (${user.followersCount})",
            initialUsers = sampleFollowers,
            onDismiss = { showFollowersDialog = false }
        )
    }

    // --- Interactive Following Showcase Dialog ---
    if (showFollowingDialog) {
        FollowersListDialog(
            title = "Following (${user.followingCount})",
            initialUsers = sampleFollowing,
            onDismiss = { showFollowingDialog = false }
        )
    }

    // --- Share Profile Feedback Dialog ---
    if (showShareProfileSnackbar) {
        AlertDialog(
            onDismissRequest = { showShareProfileSnackbar = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = BlinkCyanLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Profile 🔗", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Share @${user.username}'s Blinkchat profile with friends or across social media:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "https://blinkchat.social/@${user.username}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlinkCyanLight,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showShareProfileSnackbar = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                ) {
                    Text("Copy & Done ✨")
                }
            }
        )
    }
}

private suspend fun detectRealLocation(
    context: Context,
    onResult: (name: String, coords: String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = locationManager?.getProviders(true) ?: emptyList()
            var bestLoc: android.location.Location? = null

            for (provider in providers) {
                val l = try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager?.getLastKnownLocation(provider)
                    } else null
                } catch (e: Exception) {
                    null
                }
                if (l != null && (bestLoc == null || l.accuracy < bestLoc.accuracy)) {
                    bestLoc = l
                }
            }

            if (bestLoc != null) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = try {
                    geocoder.getFromLocation(bestLoc.latitude, bestLoc.longitude, 1)
                } catch (e: Exception) {
                    null
                }

                val city = addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea ?: "Current Location"
                val country = addresses?.firstOrNull()?.countryName ?: ""
                val locName = if (country.isNotBlank()) "$city, $country" else city
                val latStr = String.format(Locale.US, "%.4f", bestLoc.latitude)
                val lonStr = String.format(Locale.US, "%.4f", bestLoc.longitude)
                val coords = "$latStr°, $lonStr°"

                withContext(Dispatchers.Main) {
                    onResult(locName, coords)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult("Tokyo, Japan", "35.6762° N, 139.6503° E")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("Tokyo, Japan", "35.6762° N, 139.6503° E")
            }
        }
    }
}
