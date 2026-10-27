package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.User
import com.example.ui.theme.*

enum class DressingCategory(val title: String, val icon: String) {
    OUTFIT("Outfits", "🧥"),
    HEADWEAR("Headwear", "🧢"),
    ACCESSORY("Accessories", "🕶️"),
    AURA("Aura & Color", "🎨"),
    BANNER("Backdrops", "🌌"),
    VIP_UPGRADE("VIP Upgrades", "👑")
}

data class OutfitItem(val name: String, val emoji: String, val tag: String, val description: String)
data class HeadwearItem(val name: String, val emoji: String, val tag: String)
data class AccessoryItem(val name: String, val emoji: String, val tag: String)
data class BannerThemeItem(val name: String, val icon: String, val colors: List<Color>)
data class VipTierItem(val badge: String, val level: Int, val requiredXp: Int, val perks: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDressingDialog(
    user: User,
    onDismiss: () -> Unit,
    onSaveDressing: (
        outfitName: String,
        outfitEmoji: String,
        headwear: String,
        accessory: String,
        auraGlow: String,
        bannerTheme: String,
        vipBadge: String,
        avatarColorHex: String,
        newLevel: Int,
        newXp: Int
    ) -> Unit
) {
    // Current draft state for live preview
    var selectedCategory by remember { mutableStateOf(DressingCategory.OUTFIT) }
    var selectedOutfitName by remember { mutableStateOf(user.outfitName) }
    var selectedOutfitEmoji by remember { mutableStateOf(user.outfitEmoji) }
    var selectedHeadwear by remember { mutableStateOf(user.headwear) }
    var selectedAccessory by remember { mutableStateOf(user.accessory) }
    var selectedAuraGlow by remember { mutableStateOf(user.auraGlow) }
    var selectedBannerTheme by remember { mutableStateOf(user.bannerTheme) }
    var selectedVipBadge by remember { mutableStateOf(user.vipBadge) }
    var selectedAvatarColor by remember { mutableStateOf(user.avatarColorHex) }
    var currentLevel by remember { mutableStateOf(user.profileLevel) }
    var currentXp by remember { mutableStateOf(user.profileXp) }
    var showLevelUpSuccess by remember { mutableStateOf(false) }

    // Catalog items
    val outfits = remember {
        listOf(
            OutfitItem("Cyber Neon Trench", "🧥", "Legendary", "Glow-threaded matrix coat with HUD collar"),
            OutfitItem("Streetwear Bomber", "🥋", "Rare", "Oversized Tokyo cyber-urban bomber jacket"),
            OutfitItem("Obsidian Velvet Tux", "👔", "VIP Exclusive", "Luxury tailor-cut gala evening tuxedo"),
            OutfitItem("Holo Tech Lab Coat", "🥼", "Cyber Tech", "Illuminated neon scientist lab gear"),
            OutfitItem("Festival Rave Hoodie", "🎽", "Neon Pulse", "Reactive chromatic soundwave hoodie"),
            OutfitItem("Tokyo Silk Kimono", "👘", "Heritage", "Midnight silk kimono embroidered with neon koi"),
            OutfitItem("Cosmic Flight Suit", "🚀", "Galactic", "High-altitude interplanetary armor vest"),
            OutfitItem("Royal Emperor Cape", "👑", "Royalty", "Gold-trimmed velvet cape with diamond crest")
        )
    }

    val headwearList = remember {
        listOf(
            HeadwearItem("Hologram Halo 🪖", "🪖", "Angelic Tech"),
            HeadwearItem("Cyber Neon Cap 🧢", "🧢", "Streetwear"),
            HeadwearItem("Golden Diamond Crown 👑", "👑", "Royal Tier"),
            HeadwearItem("DJ Pro Headphones 🎧", "🎧", "Bass Boost"),
            HeadwearItem("Cyber Neko Ears 🐱", "🐱", "Kawaii Matrix"),
            HeadwearItem("Neon Cowboy Hat 🤠", "🤠", "Cyber Western"),
            HeadwearItem("VR Tech Goggles 🥽", "🥽", "Tactical HUD"),
            HeadwearItem("Sakura Floral Wreath 🌸", "🌸", "Zen Spring")
        )
    }

    val accessoryList = remember {
        listOf(
            AccessoryItem("Matrix Glitch Shades 🕶️", "🕶️", "Mirrored Glitch"),
            AccessoryItem("Cyber Gold Wireframe 👓", "👓", "Smart AR Glasses"),
            AccessoryItem("Diamond Iced Chain 💎", "💎", "Cuban Link Ice"),
            AccessoryItem("Lightning Bolt Pendant ⚡", "⚡", "Tesla Glow"),
            AccessoryItem("Star Shimmer Pin 🌟", "🌟", "Celestial Spark"),
            AccessoryItem("Neon Bead Rosary 📿", "📿", "Aesthetic Glow")
        )
    }

    val bannerThemes = remember {
        listOf(
            BannerThemeItem(
                "Synthwave Tokyo Night 🌆",
                "🌆",
                listOf(Color(0xFF2E0854), Color(0xFF6B21A8), Color(0xFF06B6D4))
            ),
            BannerThemeItem(
                "Deep Andromeda Nebula 🌌",
                "🌌",
                listOf(Color(0xFF0F172A), Color(0xFF3B0764), Color(0xFFEC4899))
            ),
            BannerThemeItem(
                "Cyberpunk Metropolis 2099 🏙️",
                "🏙️",
                listOf(Color(0xFF020617), Color(0xFF1E1B4B), Color(0xFF06B6D4))
            ),
            BannerThemeItem(
                "Neon Miami Sunset 🏖️",
                "🏖️",
                listOf(Color(0xFF4C0519), Color(0xFFBE185D), Color(0xFFF59E0B))
            ),
            BannerThemeItem(
                "Neo Kyoto Shrine 🏯",
                "🏯",
                listOf(Color(0xFF1E293B), Color(0xFF991B1B), Color(0xFFF43F5E))
            ),
            BannerThemeItem(
                "Electric Storm Grid ⚡",
                "⚡",
                listOf(Color(0xFF030712), Color(0xFF1E3A8A), Color(0xFF38BDF8))
            )
        )
    }

    val auraColors = remember {
        listOf(
            "#8B5CF6" to "Electric Violet",
            "#06B6D4" to "Neon Cyan",
            "#EC4899" to "Hot Magenta",
            "#F59E0B" to "Solar Gold",
            "#10B981" to "Matrix Emerald",
            "#3B82F6" to "Cobalt Blue",
            "#EF4444" to "Crimson Flame",
            "#6366F1" to "Indigo Astral"
        )
    }

    val vipTiers = remember {
        listOf(
            VipTierItem("⚡ VIP Creator", 5, 800, "Unlocks custom aura gradients & 2x creator reach"),
            VipTierItem("👑 Crown Legend", 10, 1500, "Unlocks Royal Cape & Gold Diamond Crown"),
            VipTierItem("💎 Diamond Pioneer", 15, 2500, "Unlocks Matrix Glitch Shades & Diamond Badge"),
            VipTierItem("🚀 Cosmic Elite", 20, 4000, "Unlocks Deep Space Backdrops & Cosmic Aura"),
            VipTierItem("🔥 Streak Master", 25, 6000, "Permanent glowing profile card & master verified icon")
        )
    }

    // Active Banner Gradient
    val activeBannerColors = remember(selectedBannerTheme) {
        bannerThemes.find { it.name == selectedBannerTheme }?.colors
            ?: listOf(Color(0xFF1E1B4B), Color(0xFF6B21A8), Color(0xFF06B6D4))
    }

    // Active Aura Color
    val parsedAuraColor = remember(selectedAuraGlow) {
        try {
            Color(android.graphics.Color.parseColor(selectedAuraGlow))
        } catch (e: Exception) {
            BlinkPrimary
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_dressing_studio_screen")
            ) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_dressing_studio_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dressing Room & Upgrades",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "👗✨", fontSize = 16.sp)
                        }
                        Text(
                            text = "Level $currentLevel • $selectedVipBadge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlinkPrimaryLight
                        )
                    }

                    IconButton(
                        onClick = {
                            // Quick Level Up XP boost
                            currentXp += 250
                            if (currentXp >= (currentLevel * 200)) {
                                currentLevel += 1
                                showLevelUpSuccess = true
                            }
                        },
                        modifier = Modifier.testTag("quick_boost_xp_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Boost XP", tint = BlinkAmber)
                    }
                }

                // Level Up celebration notification banner
                AnimatedVisibility(visible = showLevelUpSuccess) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        color = BlinkAmber.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, BlinkAmber),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎉", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Profile Upgraded to Level $currentLevel!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BlinkAmber
                                    )
                                    Text(
                                        text = "Unlocked new wardrobe perks and aura styles ⚡",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { showLevelUpSuccess = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // --- 1. Live Interactive 3D Avatar & Banner Showcase ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("dressing_live_preview_card"),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(activeBannerColors))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top banner info row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = selectedBannerTheme.split(" ").take(2).joinToString(" "), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Black.copy(alpha = 0.55f)
                                ) {
                                    Text(
                                        text = selectedVipBadge,
                                        fontSize = 10.sp,
                                        color = BlinkAmber,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Dressed Avatar Showcase
                            Box(contentAlignment = Alignment.Center) {
                                // Pulsating Aura Glow Ring
                                Box(
                                    modifier = Modifier
                                        .size(108.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(parsedAuraColor, BlinkPink, parsedAuraColor)
                                            )
                                        )
                                        .padding(3.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarBadge(
                                        name = user.displayName,
                                        colorHex = selectedAvatarColor,
                                        size = 96.dp
                                    )
                                }

                                // Floating Headwear Badge on top
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .offset(y = (-14).dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .border(1.5.dp, parsedAuraColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedHeadwear.takeLast(2).trim(),
                                        fontSize = 18.sp
                                    )
                                }

                                // Equipped Accessory badge on bottom right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .border(1.5.dp, BlinkCyanLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedAccessory.takeLast(2).trim(),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Equipped Outfit & Style Pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, parsedAuraColor.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedOutfitEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedOutfitName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // XP Progress Bar
                            val xpProgress = (currentXp % 1000) / 1000f
                            Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Style Upgrade Progress", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    Text(text = "${currentXp % 1000} / 1000 XP", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { xpProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = BlinkCyanLight,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }

                // --- 2. Category Navigation Tabs ---
                ScrollableTabRow(
                    selectedTabIndex = selectedCategory.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategory.ordinal]),
                            color = BlinkPrimaryLight
                        )
                    }
                ) {
                    DressingCategory.values().forEach { category ->
                        val isSelected = selectedCategory == category
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = category.icon, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = category.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.testTag("dressing_tab_${category.name.lowercase()}")
                        )
                    }
                }

                // --- 3. Interactive Wardrobe Category Items ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    when (selectedCategory) {
                        DressingCategory.OUTFIT -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(outfits) { outfit ->
                                    val isEquipped = selectedOutfitName == outfit.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedOutfitName = outfit.name
                                                selectedOutfitEmoji = outfit.emoji
                                            }
                                            .testTag("outfit_card_${outfit.name.replace(" ", "_")}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEquipped) BlinkPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isEquipped) BorderStroke(1.5.dp, BlinkPrimaryLight) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = outfit.emoji, fontSize = 28.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = BlinkCyan.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = outfit.tag,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BlinkCyanLight,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = outfit.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = outfit.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DressingCategory.HEADWEAR -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(headwearList) { item ->
                                    val isEquipped = selectedHeadwear == item.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedHeadwear = item.name }
                                            .testTag("headwear_card_${item.emoji}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEquipped) BlinkCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isEquipped) BorderStroke(1.5.dp, BlinkCyanLight) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = item.emoji, fontSize = 26.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.name.replace(item.emoji, "").trim(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(text = item.tag, fontSize = 10.sp, color = BlinkCyanLight)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DressingCategory.ACCESSORY -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(accessoryList) { item ->
                                    val isEquipped = selectedAccessory == item.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedAccessory = item.name }
                                            .testTag("accessory_card_${item.emoji}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isEquipped) BlinkPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isEquipped) BorderStroke(1.5.dp, BlinkPinkLight) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = item.emoji, fontSize = 26.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.name.replace(item.emoji, "").trim(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(text = item.tag, fontSize = 10.sp, color = BlinkPinkLight)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DressingCategory.AURA -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Choose Avatar Aura Glow & Background Color",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(auraColors) { (hex, label) ->
                                        val isSelected = selectedAuraGlow.equals(hex, ignoreCase = true)
                                        val itemColor = Color(android.graphics.Color.parseColor(hex))
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    selectedAuraGlow = hex
                                                    selectedAvatarColor = hex
                                                }
                                                .testTag("aura_color_${label.replace(" ", "_")}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(itemColor)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Text(
                                    text = "Aura Intensity Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("⚡ Pulse Sparkle", "🌌 Nebula Drift", "🔥 Hyper Flame").forEach { mode ->
                                        FilterChip(
                                            selected = true,
                                            onClick = { /* applied live */ },
                                            label = { Text(mode, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        DressingCategory.BANNER -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(1),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(bannerThemes) { theme ->
                                    val isSelected = selectedBannerTheme == theme.name
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedBannerTheme = theme.name }
                                            .testTag("banner_theme_card_${theme.icon}"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = if (isSelected) BorderStroke(2.dp, BlinkCyanLight) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp)
                                                .background(Brush.horizontalGradient(theme.colors))
                                                .padding(horizontal = 14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = theme.icon, fontSize = 24.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = theme.name,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }

                                                if (isSelected) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color.White
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp).padding(3.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DressingCategory.VIP_UPGRADE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Unlock VIP Status Badges & Creator Perks",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                vipTiers.forEach { tier ->
                                    val isSelected = selectedVipBadge == tier.badge
                                    val isUnlocked = currentLevel >= tier.level

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedVipBadge = tier.badge
                                                if (currentLevel < tier.level) {
                                                    currentLevel = tier.level
                                                    currentXp = tier.requiredXp
                                                    showLevelUpSuccess = true
                                                }
                                            }
                                            .testTag("vip_tier_card_${tier.level}"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) BlinkAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                        ),
                                        border = if (isSelected) BorderStroke(1.5.dp, BlinkAmber) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = BlinkAmber.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "Lv.${tier.level}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp,
                                                        color = BlinkAmber,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = tier.badge,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = tier.perks,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Surface(shape = CircleShape, color = BlinkAmber) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp).padding(3.dp))
                                                }
                                            } else if (!isUnlocked) {
                                                Text(
                                                    text = "Upgrade ⚡",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BlinkPrimaryLight
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 4. Bottom Save & Upgrade Button ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.35f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onSaveDressing(
                                    selectedOutfitName,
                                    selectedOutfitEmoji,
                                    selectedHeadwear,
                                    selectedAccessory,
                                    selectedAuraGlow,
                                    selectedBannerTheme,
                                    selectedVipBadge,
                                    selectedAvatarColor,
                                    currentLevel,
                                    currentXp
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(0.65f)
                                .height(48.dp)
                                .testTag("save_and_upgrade_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save & Upgrade Profile ⚡",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
