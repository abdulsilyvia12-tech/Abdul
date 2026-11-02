package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FeedFilterMode {
    ALL,
    SAVED
}

@Composable
fun FeedScreen(
    currentUser: User?,
    stories: List<Story>,
    posts: List<Post>,
    commentsMap: Map<String, List<Comment>>,
    conversations: List<Conversation> = emptyList(),
    onLikePost: (String) -> Unit,
    onReactionClick: (postId: String, emoji: String) -> Unit = { _, _ -> },
    onSavePost: (String) -> Unit,
    onAddComment: (postId: String, text: String) -> Unit,
    onSharePostToConversation: (postId: String, conversationId: String) -> Unit = { _, _ -> },
    onFollowCreator: (username: String) -> Unit = {},
    onOpenStory: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onBlockUser: (User) -> Unit,
    onReportPost: (postId: String, authorName: String) -> Unit,
    onUseTrendingSound: (String) -> Unit = {}
) {
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }
    var blockingUser by remember { mutableStateOf<User?>(null) }
    var sharingPost by remember { mutableStateOf<Post?>(null) }
    var filterMode by remember { mutableStateOf(FeedFilterMode.ALL) }
    var selectedHashtag by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val basePosts = when (filterMode) {
        FeedFilterMode.ALL -> posts
        FeedFilterMode.SAVED -> posts.filter { it.isSaved }
    }

    val displayedPosts = remember(basePosts, selectedHashtag) {
        if (selectedHashtag.isNullOrBlank()) {
            basePosts
        } else {
            val tag = selectedHashtag!!.lowercase()
            basePosts.filter { post ->
                post.caption.lowercase().contains("#$tag") ||
                post.filterTag.lowercase().contains(tag)
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("feed_post_list"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // --- Unified Feature Header & Filter Mode Bar ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Home",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BlinkCyan.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "⚡ Live",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlinkCyanLight,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Unified Home stream & live highlights",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Create button in header
                        IconButton(
                            onClick = onCreatePostClick,
                            modifier = Modifier.testTag("header_create_blink")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "New Blink",
                                tint = BlinkPrimaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Segmented Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterMode == FeedFilterMode.ALL,
                            onClick = { filterMode = FeedFilterMode.ALL },
                            label = { Text("⚡ All Blinks", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlinkPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = BlinkPrimaryLight
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = filterMode == FeedFilterMode.SAVED,
                            onClick = { filterMode = FeedFilterMode.SAVED },
                            label = { Text("🔖 Saved", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlinkPink.copy(alpha = 0.2f),
                                selectedLabelColor = BlinkPinkLight
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // --- Unified Stories Showcase Tray ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current User "+ Story / Blink" Button
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onAddStoryClick() }
                                    .testTag("add_story_button")
                            ) {
                                Box(
                                    modifier = Modifier.size(66.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AvatarBadge(
                                        name = currentUser?.displayName ?: "Me",
                                        colorHex = currentUser?.avatarColorHex ?: "#8B5CF6",
                                        size = 58.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(BlinkPrimary)
                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Blink",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Your Blink",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Friend Stories Cards with Active Glowing Ring
                        items(stories, key = { it.id }) { story ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onOpenStory(story) }
                                    .testTag("story_item_${story.username}")
                            ) {
                                StoryRingAvatar(
                                    name = story.userDisplayName,
                                    colorHex = story.userAvatarColor,
                                    hasUnseenStory = !story.isViewed,
                                    size = 66.dp,
                                    onClick = { onOpenStory(story) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = story.userDisplayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
                    )
                }
            }

            // --- Social Explore & Trending Hub (Hashtags, Creators, Sounds) ---
            item {
                ExploreTrendingHub(
                    selectedTag = selectedHashtag,
                    onSelectTag = { tag -> selectedHashtag = tag },
                    onFollowCreator = onFollowCreator,
                    onUseTrendingSound = onUseTrendingSound
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- Main Posts Feed ---
            if (displayedPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filterMode == FeedFilterMode.SAVED) "No saved Blinks yet." 
                                   else if (selectedHashtag != null) "No posts found for #$selectedHashtag" 
                                   else "No posts yet. Capture a Blink to get started!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onLike = { onLikePost(post.id) },
                        onReaction = { emoji -> onReactionClick(post.id, emoji) },
                        onShareClick = { sharingPost = post },
                        onSave = { onSavePost(post.id) },
                        onCommentClick = { activeCommentPostId = post.id },
                        onReportClick = { reportingPost = post },
                        onBlockClick = {
                            val blockedTarget = User(
                                id = post.authorId,
                                username = post.authorUsername,
                                displayName = post.authorName,
                                email = "${post.authorUsername}@blinkchat.com",
                                avatarColorHex = post.authorAvatarColor
                            )
                            blockingUser = blockedTarget
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Floating Action Button to Create Unified Blink (Post + Story)
        FloatingActionButton(
            onClick = onCreatePostClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("create_post_fab"),
            containerColor = BlinkPrimary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Create Blink")
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Blink ⚡", fontWeight = FontWeight.Bold)
            }
        }

        // Share Post Bottom Sheet
        if (sharingPost != null) {
            SharePostDialog(
                post = sharingPost!!,
                conversations = conversations,
                onDismiss = { sharingPost = null },
                onShareToConversation = { convId ->
                    onSharePostToConversation(sharingPost!!.id, convId)
                    snackbarMessage = "Shared to direct message! ✨"
                },
                onAddToStory = {
                    onAddStoryClick()
                },
                onCopyLink = {
                    snackbarMessage = "Post link copied to clipboard! 📋"
                }
            )
        }

        // Comments Bottom Sheet
        if (activeCommentPostId != null) {
            val postId = activeCommentPostId!!
            val postComments = commentsMap[postId] ?: emptyList()
            CommentSheet(
                comments = postComments,
                onDismiss = { activeCommentPostId = null },
                onAddComment = { text ->
                    onAddComment(postId, text)
                }
            )
        }

        // Safety Dialogs
        if (reportingPost != null) {
            ReportDialog(
                targetName = "@${reportingPost!!.authorUsername}'s post",
                onDismiss = { reportingPost = null },
                onSubmitReport = { _, _ ->
                    onReportPost(reportingPost!!.id, reportingPost!!.authorName)
                    reportingPost = null
                }
            )
        }

        if (blockingUser != null) {
            BlockConfirmDialog(
                targetName = "@${blockingUser!!.username}",
                onDismiss = { blockingUser = null },
                onConfirmBlock = {
                    onBlockUser(blockingUser!!)
                    blockingUser = null
                }
            )
        }

        // Social snackbar feedback
        if (snackbarMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { snackbarMessage = null }) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            ) {
                Text(snackbarMessage!!)
            }
        }
    }
}


@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onReaction: (emoji: String) -> Unit = {},
    onShareClick: () -> Unit = {},
    onSave: () -> Unit,
    onCommentClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showBigHeart by remember { mutableStateOf(false) }
    var showReactionsPicker by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val quickEmojis = remember { listOf("❤️", "🔥", "😂", "⚡", "😍", "👏") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("post_card_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Author info & options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(
                        name = post.authorName,
                        colorHex = post.authorAvatarColor,
                        size = 42.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (post.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Creator",
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // Filter Badge tag
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BlinkPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = post.filterTag,
                                    fontSize = 10.sp,
                                    color = BlinkPrimaryLight,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "@${post.authorUsername} • ${post.timeAgo}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Options Menu (Block / Report)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("post_options_${post.id}")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report Post", color = BlinkPink) },
                            leadingIcon = {
                                Icon(Icons.Default.Report, contentDescription = null, tint = BlinkPink)
                            },
                            onClick = {
                                showMenu = false
                                onReportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block @${post.authorUsername}", color = BlinkPink) },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null, tint = BlinkPink)
                            },
                            onClick = {
                                showMenu = false
                                onBlockClick()
                            }
                        )
                    }
                }
            }

            // Media Content (Photo with double-tap heart)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onReaction("❤️")
                                showBigHeart = true
                                coroutineScope.launch {
                                    delay(800)
                                    showBigHeart = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!post.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = post.imageUri,
                        contentDescription = "Post photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (post.imageDrawableRes != null) {
                    Image(
                        painter = painterResource(id = post.imageDrawableRes),
                        contentDescription = "Post photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Stylized dynamic visual card fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E1B4B),
                                        Color(0xFF3B0764)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = BlinkCyanLight,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = post.caption,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "⚡ #${post.filterTag} • Blink",
                                    color = BlinkCyanLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Big heart animation on double tap
                androidx.compose.animation.AnimatedVisibility(
                    visible = showBigHeart,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = BlinkPink,
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // Quick Multi-Emoji Reactions Bar (Expandable)
            AnimatedVisibility(visible = showReactionsPicker) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickEmojis.forEach { emoji ->
                            val isSelected = post.userReaction == emoji
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) BlinkPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        onReaction(emoji)
                                        showReactionsPicker = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Social Reaction Chips Display Row
            if (post.reactions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(post.reactions.entries.toList()) { (emoji, count) ->
                        val isUserReacted = post.userReaction == emoji
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUserReacted) BlinkPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isUserReacted) androidx.compose.foundation.BorderStroke(1.dp, BlinkPrimary) else null,
                            modifier = Modifier
                                .clickable { onReaction(emoji) }
                                .testTag("reaction_${post.id}_$emoji")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUserReacted) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like button
                    IconButton(
                        onClick = onLike,
                        modifier = Modifier.testTag("like_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) BlinkPink else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${post.likesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Emoji reactions toggle button
                    IconButton(
                        onClick = { showReactionsPicker = !showReactionsPicker },
                        modifier = Modifier.testTag("reaction_picker_button_${post.id}")
                    ) {
                        Text(
                            text = post.userReaction ?: "⚡",
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onCommentClick,
                        modifier = Modifier.testTag("comment_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("share_button_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (post.sharesCount > 0) {
                        Text(
                            text = "${post.sharesCount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("save_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isSaved) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Caption & comments count
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Row {
                    Text(
                        text = post.authorUsername,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = post.caption,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (post.commentsCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View all ${post.commentsCount} comments",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onCommentClick() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
