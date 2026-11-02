package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*

data class SuggestedCreator(
    val id: String,
    val name: String,
    val username: String,
    val avatarColorHex: String,
    val followersFormatted: String,
    val bio: String,
    val isVerified: Boolean = true,
    val isFollowing: Boolean = false
)

data class TrendingSound(
    val title: String,
    val artist: String,
    val usageCount: String,
    val tag: String
)

@Composable
fun ExploreTrendingHub(
    selectedTag: String?,
    onSelectTag: (String?) -> Unit,
    onFollowCreator: (creatorUsername: String) -> Unit,
    onUseTrendingSound: (soundTitle: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingHashtags = remember {
        listOf("all", "phonk", "erastour", "cyberpunk", "london", "tokyodrift", "cinematic", "skate", "vibes")
    }

    var suggestedCreators by remember {
        mutableStateOf(
            listOf(
                SuggestedCreator("c_1", "Kai Storm", "kaistorm", "#06B6D4", "12.4K", "Phonk producer & visualizer ⚡", isVerified = true, isFollowing = false),
                SuggestedCreator("c_2", "Maya Lin", "mayalin", "#EC4899", "28.1K", "Tokyo neon aesthetic & DJ sets ✨", isVerified = true, isFollowing = true),
                SuggestedCreator("c_3", "Zane Vance", "zanevance", "#10B981", "9.8K", "Synthwave & cyber photography 🌆", isVerified = true, isFollowing = false),
                SuggestedCreator("c_4", "Elena Vance", "elenavance", "#8B5CF6", "15.3K", "Visual effects & street style 🎨", isVerified = false, isFollowing = false)
            )
        )
    }

    val trendingSounds = remember {
        listOf(
            TrendingSound("Metamorphosis (Phonk Speed)", "Interworld", "148K Blinks", "phonk"),
            TrendingSound("Cruel Summer (Live Eras Tour)", "Taylor Swift", "312K Blinks", "erastour"),
            TrendingSound("Tokyo Drift (Remix)", "Teriyaki Boyz", "89K Blinks", "tokyodrift")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("explore_trending_hub")
    ) {
        // Trending Hashtags Scroll
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trendingHashtags) { tag ->
                val isSelected = (tag == "all" && selectedTag == null) || (selectedTag == tag)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) BlinkPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clickable {
                            if (tag == "all") onSelectTag(null)
                            else onSelectTag(if (selectedTag == tag) null else tag)
                        }
                        .testTag("hashtag_chip_$tag")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tag == "all") "🔥 Explore All" else "#$tag",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Who To Follow Title & Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Creators to Follow",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Discover ⚡",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BlinkCyanLight
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(suggestedCreators, key = { it.id }) { creator ->
                val avatarColor = remember(creator.avatarColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(creator.avatarColorHex))
                    } catch (_: Exception) {
                        Color(0xFF6366F1)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .width(148.dp)
                        .testTag("creator_card_${creator.username}")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = creator.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = creator.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (creator.isVerified) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Creator",
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Text(
                            text = "@${creator.username}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${creator.followersFormatted} followers",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlinkPrimaryLight,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Button(
                            onClick = {
                                suggestedCreators = suggestedCreators.map {
                                    if (it.id == creator.id) it.copy(isFollowing = !it.isFollowing) else it
                                }
                                onFollowCreator(creator.username)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = if (creator.isFollowing) {
                                ButtonDefaults.outlinedButtonColors()
                            } else {
                                ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .testTag("follow_toggle_${creator.username}")
                        ) {
                            Text(
                                text = if (creator.isFollowing) "Following" else "Follow +",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Trending Sounds Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(trendingSounds) { sound ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onUseTrendingSound(sound.title) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("trending_sound_${sound.tag}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = BlinkPinkLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = sound.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${sound.artist} • ${sound.usageCount}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = BlinkPink.copy(alpha = 0.15f),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Use in camera",
                                    tint = BlinkPinkLight,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
