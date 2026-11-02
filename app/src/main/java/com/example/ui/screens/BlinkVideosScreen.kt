package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BlinkVideo
import com.example.data.audio.BlinkMusicPlayer
import com.example.data.model.Conversation
import com.example.data.model.User
import com.example.data.model.VideoCategory
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlinkVideosScreen(
    videos: List<BlinkVideo>,
    currentUser: User?,
    conversations: List<Conversation>,
    onLikeVideo: (String) -> Unit,
    onSaveVideo: (String) -> Unit,
    onAddComment: (String, String) -> Unit,
    onShareVideo: (String, String) -> Unit,
    onCreateVideo: (String, String, List<String>, Boolean, String?, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf(VideoCategory.ALL) }
    var activeCommentVideo by remember { mutableStateOf<BlinkVideo?>(null) }
    var activeShareVideo by remember { mutableStateOf<BlinkVideo?>(null) }
    var showCreateVideoDialog by remember { mutableStateOf(false) }

    val isMusicPlaying by BlinkMusicPlayer.isPlaying.collectAsState()
    val isMusicMuted by BlinkMusicPlayer.isMuted.collectAsState()

    // Filter videos by selected category
    val filteredVideos = remember(videos, selectedCategory) {
        when (selectedCategory) {
            VideoCategory.ALL -> videos
            VideoCategory.PHONK -> videos.filter { it.category == VideoCategory.PHONK || it.musicTrack.contains("phonk", true) || it.musicTrack.contains("kordhell", true) || it.musicTrack.contains("dvrst", true) || it.musicTrack.contains("drift", true) }
            VideoCategory.DRAMATIC -> videos.filter { it.category == VideoCategory.DRAMATIC }
            VideoCategory.FUNNY -> videos.filter { it.category == VideoCategory.FUNNY }
            VideoCategory.MUSIC -> videos.filter { it.category == VideoCategory.MUSIC }
            VideoCategory.BONGO -> videos.filter { it.category == VideoCategory.BONGO }
            VideoCategory.BLINKS -> videos.filter { it.isBlink }
            VideoCategory.COMMUNITY -> videos.filter { !it.isBlink }
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { filteredVideos.size.coerceAtLeast(1) }
    )

    // Automatically stream synthesized music tracks while scrolling and watching videos
    LaunchedEffect(pagerState.currentPage, filteredVideos) {
        val currentVideo = filteredVideos.getOrNull(pagerState.currentPage)
        if (currentVideo != null) {
            BlinkMusicPlayer.playTrack(currentVideo.musicTrack)
        }
    }

    // Pause music sound when leaving the Videos section
    DisposableEffect(Unit) {
        onDispose {
            BlinkMusicPlayer.pausePlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("scrolling_videos_screen")
    ) {
        if (filteredVideos.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No videos in this category yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Be the first to post a Blink or community video!",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showCreateVideoDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                    modifier = Modifier.testTag("empty_post_video_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Post a Video")
                }
            }
        } else {
            // Vertical Video Pager
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("vertical_video_pager")
            ) { page ->
                val video = filteredVideos.getOrNull(page)
                if (video != null) {
                    VideoPageItem(
                        video = video,
                        isCurrentPage = pagerState.currentPage == page,
                        onLike = { onLikeVideo(video.id) },
                        onSave = { onSaveVideo(video.id) },
                        onCommentClick = { activeCommentVideo = video },
                        onShareClick = { activeShareVideo = video }
                    )
                }
            }
        }

        // --- Top Category Selector Overlay ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category switch chips
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    VideoCategory.ALL to "All",
                    VideoCategory.PHONK to "🏎️ Phonk",
                    VideoCategory.MUSIC to "🎵 Music",
                    VideoCategory.DRAMATIC to "🎭 Dramatic",
                    VideoCategory.FUNNY to "😂 Funny",
                    VideoCategory.BONGO to "🎤 Pop Hits",
                    VideoCategory.BLINKS to "⚡ Blinks",
                    VideoCategory.COMMUNITY to "👥 Community"
                ).forEach { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .then(
                                if (isSelected) Modifier.background(Brush.horizontalGradient(listOf(BlinkPrimary, BlinkPink)))
                                else Modifier.background(Color.Transparent)
                            )
                            .clickable {
                                selectedCategory = cat
                                coroutineScope.launch { pagerState.scrollToPage(0) }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_video_${cat.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound Toggle Button
                IconButton(
                    onClick = { BlinkMusicPlayer.toggleMute() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .testTag("video_sound_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMusicMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Sound Mute/Unmute",
                        tint = if (isMusicMuted) BlinkPink else BlinkCyanLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Create Button on Top Right
                Surface(
                    onClick = { showCreateVideoDialog = true },
                    shape = CircleShape,
                    color = BlinkPrimary.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("post_video_fab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Post Video",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Interactive Comment Bottom Sheet ---
        if (activeCommentVideo != null) {
            val video = activeCommentVideo!!
            VideoCommentBottomSheet(
                video = video,
                onDismiss = { activeCommentVideo = null },
                onAddComment = { text ->
                    onAddComment(video.id, text)
                    // Keep bottom sheet updated with latest comment
                    activeCommentVideo = videos.find { it.id == video.id }
                }
            )
        }

        // --- Interactive Share to Chat Bottom Sheet ---
        if (activeShareVideo != null) {
            val video = activeShareVideo!!
            VideoShareBottomSheet(
                video = video,
                conversations = conversations,
                onDismiss = { activeShareVideo = null },
                onSendToConversation = { convId ->
                    onShareVideo(video.id, convId)
                    activeShareVideo = null
                }
            )
        }

        // --- Post New Video Modal ---
        if (showCreateVideoDialog) {
            CreateVideoDialog(
                onDismiss = { showCreateVideoDialog = false },
                onPublish = { title, caption, tags, isBlink, filter, music ->
                    onCreateVideo(title, caption, tags, isBlink, filter, music)
                    showCreateVideoDialog = false
                    selectedCategory = if (isBlink) VideoCategory.BLINKS else VideoCategory.COMMUNITY
                    coroutineScope.launch {
                        delay(200)
                        pagerState.animateScrollToPage(0)
                    }
                }
            )
        }
    }
}

@Composable
fun VideoPageItem(
    video: BlinkVideo,
    isCurrentPage: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    // Looping playback progress animation
    val infiniteTransition = rememberInfiniteTransition(label = "video_playback")
    val playbackProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = video.durationSeconds * 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "playback_progress"
    )

    // Animated camera pan/zoom motion simulation to create lively video motion
    val cameraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "camera_motion"
    )

    // Rotating vinyl sound disc animation
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    // Play or pause the music sound synchronously with video playback state
    LaunchedEffect(isCurrentPage, isPlaying, video.musicTrack) {
        if (isCurrentPage) {
            if (isPlaying) {
                BlinkMusicPlayer.playTrack(video.musicTrack)
            } else {
                BlinkMusicPlayer.pausePlayback()
            }
        }
    }

    // Reset isPlaying to true when scrolling to another page
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            isPlaying = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!video.isLiked) {
                            onLike()
                        }
                        showHeartAnimation = true
                    },
                    onTap = {
                        val willPlay = !isPlaying
                        isPlaying = willPlay
                        if (willPlay) {
                            BlinkMusicPlayer.playTrack(video.musicTrack)
                        } else {
                            BlinkMusicPlayer.pausePlayback()
                        }
                    }
                )
            }
            .testTag("video_page_${video.id}")
    ) {
        // --- Video Media Layer ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isPlaying) cameraScale else 1.0f)
        ) {
            if (video.thumbnailRes != null) {
                Image(
                    painter = painterResource(id = video.thumbnailRes),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Procedural ambient gradient background
                val gradColors = video.videoGradientColors.map { Color(it) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(gradColors))
                )
            }

            // Darker vignette scrim for text readability and cinematic look
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Dynamic Equalizer Overlay for Music/Blink Vibe
            if (isPlaying && isCurrentPage) {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 60.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val heights = listOf(14, 28, 20, 36, 18, 30, 22, 16)
                    heights.forEachIndexed { idx, h ->
                        val animHeight by infiniteTransition.animateFloat(
                            initialValue = 6f,
                            targetValue = h.toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 280 + (idx * 60), easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "eq_bar_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(animHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (video.isBlink) BlinkPinkLight else BlinkCyanLight)
                        )
                    }
                }
            }
        }

        // --- Center Play/Pause indicator when paused ---
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .clickable {
                        isPlaying = true
                        BlinkMusicPlayer.playTrack(video.musicTrack)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume Video and Sound",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // --- Double-Tap Heart Splash Animation ---
        if (showHeartAnimation) {
            LaunchedEffect(showHeartAnimation) {
                delay(700)
                showHeartAnimation = false
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = BlinkPink,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(1.2f)
                )
            }
        }

        // --- Right-Side Floating Action Column ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Author Avatar with (+) Follow
            Box(contentAlignment = Alignment.BottomCenter) {
                AvatarBadge(
                    name = video.authorName,
                    colorHex = video.authorAvatarColor,
                    size = 46.dp
                )
                Box(
                    modifier = Modifier
                        .offset(y = 6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BlinkPink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Follow",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Like Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("video_like_button_${video.id}")
                ) {
                    Icon(
                        imageVector = if (video.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (video.isLiked) BlinkPink else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "${video.likesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("video_comment_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${video.commentsCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Save / Bookmark Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("video_save_button_${video.id}")
                ) {
                    Icon(
                        imageVector = if (video.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (video.isSaved) BlinkCyanLight else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (video.isSaved) "Saved" else "Save",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Share Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .testTag("video_share_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${video.sharesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Spinning Vinyl Record for Music
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .rotate(if (isPlaying) discRotation else 0f)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(listOf(Color.DarkGray, Color.Black, Color.DarkGray))
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(BlinkPrimary)
                )
            }
        }

        // --- Bottom Left Video Info & Caption Overlay ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.80f)
                .padding(start = 16.dp, bottom = 22.dp)
        ) {
            // Blink Pill or Category badge
            if (video.isBlink) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlinkPink.copy(alpha = 0.25f))
                        .border(1.dp, BlinkPink.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = BlinkPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Blink Video (${video.durationSeconds}s loop)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Author Username
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${video.authorUsername}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${video.createdAtFormatted}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Video Title & Caption
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = video.caption,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Hashtags
            if (video.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    video.hashtags.take(4).forEach { tag ->
                        Text(
                            text = tag,
                            color = BlinkCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // AI Filter or Prompt Badge
            if (video.filterName != null || video.isAiGenerated) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BlinkPrimaryLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (video.filterName != null) "Filter: ${video.filterName}" else "AI Video Generator",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Music / Audio Track
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = video.musicTrack,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- Bottom Timeline Playback Progress Bar ---
        LinearProgressIndicator(
            progress = { if (isPlaying) playbackProgress else 0.5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter),
            color = if (video.isBlink) BlinkPink else BlinkPrimaryLight,
            trackColor = Color.White.copy(alpha = 0.2f),
        )
    }
}

// --- Interactive Comments Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCommentBottomSheet(
    video: BlinkVideo,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("video_comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments (${video.comments.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Comments List
            if (video.comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Start the conversation! 💬",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(video.comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AvatarBadge(
                                name = comment.authorName,
                                colorHex = comment.authorAvatarColor,
                                size = 32.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = comment.timestampFormatted,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { /* Like comment */ },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like comment",
                                    tint = if (comment.isLiked) BlinkPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input field to add comment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_video_comment_input"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlinkPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            onAddComment(newCommentText)
                            newCommentText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (newCommentText.isNotBlank()) BlinkPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("send_video_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post Comment",
                        tint = if (newCommentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// --- Interactive Share Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoShareBottomSheet(
    video: BlinkVideo,
    conversations: List<Conversation>,
    onDismiss: () -> Unit,
    onSendToConversation: (String) -> Unit
) {
    var shareSuccessMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Share Video",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Send \"${video.title}\" to friends in Blinkchat",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (shareSuccessMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BlinkCyan.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BlinkCyanLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(shareSuccessMessage!!, color = BlinkCyanLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Text(
                text = "Send via Direct Message:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    Surface(
                        onClick = {
                            onSendToConversation(conv.id)
                            shareSuccessMessage = "Sent to ${conv.name}! 🚀"
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(name = conv.name, colorHex = conv.avatarColorHex, size = 38.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = conv.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "@${conv.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    onSendToConversation(conv.id)
                                    shareSuccessMessage = "Sent to ${conv.name}! 🚀"
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Send", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Close", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// --- Create New Video Dialog ---
@Composable
fun CreateVideoDialog(
    onDismiss: () -> Unit,
    onPublish: (title: String, caption: String, tags: List<String>, isBlink: Boolean, filter: String?, music: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var hashtagsInput by remember { mutableStateOf("#blink #vibes") }
    var isBlinkMode by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Cyber Neon") }
    var musicTrack by remember { mutableStateOf("⚡ Tokyo Drift Synthwave") }

    val filterOptions = listOf("Cyber Neon", "Sunset Glow", "VHS Retro", "Holo Matrix", "Pure Natural")
    val musicOptions = remember { BlinkMusicPlayer.availableTracks.map { it.title } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isBlinkMode) Icons.Default.Bolt else Icons.Default.VideoCameraBack,
                    contentDescription = null,
                    tint = if (isBlinkMode) BlinkPink else BlinkPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isBlinkMode) "Create Blink Video" else "Post Community Video")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Blink vs Community toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isBlinkMode) BlinkPink else Color.Transparent)
                            .clickable { isBlinkMode = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⚡ Blink (15s)",
                            color = if (isBlinkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isBlinkMode) BlinkPrimary else Color.Transparent)
                            .clickable { isBlinkMode = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "👥 Community Video",
                            color = if (!isBlinkMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Video Title") },
                    placeholder = { Text("e.g. Tokyo Midnight Cruise") },
                    modifier = Modifier.fillMaxWidth().testTag("video_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption") },
                    placeholder = { Text("Describe your video...") },
                    modifier = Modifier.fillMaxWidth().testTag("video_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = hashtagsInput,
                    onValueChange = { hashtagsInput = it },
                    label = { Text("Hashtags (space separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("AI Camera Filter:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filterOptions.take(3).forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = hashtagsInput.split(" ").filter { it.isNotBlank() }
                    onPublish(title, caption, tags, isBlinkMode, selectedFilter, musicTrack)
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (isBlinkMode) BlinkPink else BlinkPrimary),
                modifier = Modifier.testTag("publish_video_button")
            ) {
                Text("Publish Video 🚀", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
