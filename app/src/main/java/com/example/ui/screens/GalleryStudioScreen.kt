package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.BlinkAiService
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StudioTab(val title: String, val icon: String) {
    GALLERY("Gallery", "🖼️"),
    TEXT_TO_VIDEO("Text to Video", "🎥"),
    ANIMATE_IMAGE("Animate Photo", "✨"),
    IMAGE_EDITOR("Create & Edit", "🎨")
}

@Composable
fun GalleryStudioScreen(
    onPostGeneratedContent: (caption: String, filterTag: String, isStory: Boolean) -> Unit
) {
    var activeTab by remember { mutableStateOf(StudioTab.GALLERY) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("gallery_studio_screen")
    ) {
        // Studio Segment Bar
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                    color = BlinkPrimaryLight
                )
            }
        ) {
            StudioTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = tab.icon, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) BlinkPrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.testTag("studio_tab_${tab.name.lowercase()}")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (activeTab) {
                StudioTab.GALLERY -> ExploreGallerySubScreen(onSelectRemix = { caption, filter ->
                    onPostGeneratedContent(caption, filter, false)
                })
                StudioTab.TEXT_TO_VIDEO -> TextToVideoSubScreen(onShareVideo = onPostGeneratedContent)
                StudioTab.ANIMATE_IMAGE -> AnimateImageSubScreen(onShareAnimated = onPostGeneratedContent)
                StudioTab.IMAGE_EDITOR -> ImageEditorSubScreen(onShareImage = onPostGeneratedContent)
            }
        }
    }
}

// --- 1. Explore Gallery ---
@Composable
fun ExploreGallerySubScreen(
    onSelectRemix: (caption: String, filter: String) -> Unit
) {
    val galleryItems = remember {
        listOf(
            Triple(R.drawable.feed_cyber_snap_1792586101459, "Neo Tokyo Night Run", "Cyberpunk Neon"),
            Triple(R.drawable.feed_sunset_snap_1792586114190, "Golden Rooftop Sessions", "Sunset Glow"),
            Triple(R.drawable.blinkchat_icon_1792586027238, "Blink AI Official Brand Snap", "Electric Aura"),
            Triple(R.drawable.feed_cyber_snap_1792586101459, "Arcade Reflections & Synthwave", "VHS 90s"),
            Triple(R.drawable.feed_sunset_snap_1792586114190, "Summer Festival Crowd Waves", "Aura Dream")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(BlinkPrimary.copy(alpha = 0.3f), BlinkCyan.copy(alpha = 0.2f)))
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🌟", fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Community Snap Gallery", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Explore trending creative snaps, remixes, animated reels, and AI creations.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Featured Creations",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(galleryItems) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        Image(
                            painter = painterResource(id = item.first),
                            contentDescription = item.second,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = item.third,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = item.second, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Remixable AI Filter & Motion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { onSelectRemix("Remix: ${item.second}", item.third) },
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Remix Snap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Text to Video Generator ---
@Composable
fun TextToVideoSubScreen(
    onShareVideo: (caption: String, filterTag: String, isStory: Boolean) -> Unit
) {
    var videoPrompt by remember { mutableStateOf("Neon hologram cat racing across futuristic Tokyo skyline in the rain") }
    var selectedAspectRatio by remember { mutableStateOf("9:16") }
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf(0) }
    var generatedVideoReady by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Motion loop animation for video preview
    val infiniteTransition = rememberInfiniteTransition(label = "videoMotion")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    val panOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "pan"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "🎥 AI Text-to-Video Generator", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            text = "Generate stunning animated video snaps from any creative text description.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = videoPrompt,
            onValueChange = { videoPrompt = it },
            label = { Text("Video Prompt") },
            modifier = Modifier.fillMaxWidth().testTag("text_to_video_input"),
            shape = RoundedCornerShape(14.dp),
            maxLines = 3
        )

        // Aspect ratio selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "Aspect Ratio:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            listOf("9:16 (Portrait)", "16:9 (Cinema)").forEach { ratio ->
                val isSelected = selectedAspectRatio == ratio.take(4)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedAspectRatio = ratio.take(4) },
                    label = { Text(ratio, fontSize = 11.sp) }
                )
            }
        }

        // Generate Button
        Button(
            onClick = {
                coroutineScope.launch {
                    isGenerating = true
                    generationProgress = 0
                    for (i in 1..10) {
                        delay(250)
                        generationProgress = i * 10
                    }
                    isGenerating = false
                    generatedVideoReady = true
                }
            },
            enabled = !isGenerating && videoPrompt.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("generate_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.MovieCreation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isGenerating) "Synthesizing Video Frames... ($generationProgress%)" else "Generate Video Snap ⚡", fontWeight = FontWeight.Bold)
        }

        if (isGenerating) {
            LinearProgressIndicator(
                progress = { generationProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = BlinkCyanLight
            )
        }

        // Generated Video Player Simulation
        if (generatedVideoReady) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.feed_cyber_snap_1792586101459),
                        contentDescription = "Video frames",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale)
                            .graphicsLayer { translationX = panOffset },
                        contentScale = ContentScale.Crop
                    )

                    // Overlay badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "▶ AI Video Generated • 60 FPS",
                            fontSize = 11.sp,
                            color = BlinkCyanLight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Share button
                    Button(
                        onClick = {
                            onShareVideo(videoPrompt, "AI Video", true)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(14.dp)
                            .fillMaxWidth(0.9f),
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Video to Home ⚡", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 3. Animate Images into Videos ---
@Composable
fun AnimateImageSubScreen(
    onShareAnimated: (caption: String, filterTag: String, isStory: Boolean) -> Unit
) {
    val motionStyles = listOf(
        "🌊 Fluid Neon Wave",
        "⚡ Cyberpunk Glitch Zoom",
        "🌌 Parallax Star Drift",
        "🎞️ Cinematic Slow Pan",
        "💓 Aura Bloom Pulse"
    )
    var selectedMotion by remember { mutableStateOf(motionStyles[0]) }
    var isRendering by remember { mutableStateOf(false) }
    var isRendered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "motion")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave"
    )
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "rot"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "✨ Animate Static Photo into Video", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(
            text = "Convert still snapshots into alive, breathing video clips using AI depth & motion shaders.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Select Motion Style
        Text(text = "Choose Animation Motion Style:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(motionStyles) { style ->
                FilterChip(
                    selected = selectedMotion == style,
                    onClick = { selectedMotion = style },
                    label = { Text(style, fontSize = 11.sp) }
                )
            }
        }

        // Preview Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.feed_sunset_snap_1792586114190),
                contentDescription = "Animated preview",
                modifier = Modifier
                    .fillMaxSize()
                    .scale(if (isRendered) waveScale else 1f)
                    .graphicsLayer { rotationZ = if (isRendered) rotationAngle else 0f },
                contentScale = ContentScale.Crop
            )

            if (!isRendered && !isRendering) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Static Image Ready to Animate",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (isRendering) {
                CircularProgressIndicator(color = BlinkCyanLight)
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    isRendering = true
                    delay(1200)
                    isRendering = false
                    isRendered = true
                }
            },
            enabled = !isRendering,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("animate_photo_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRendered) "Re-Animate Motion" else "Animate Photo into Video 🎥", fontWeight = FontWeight.Bold)
        }

        if (isRendered) {
            Button(
                onClick = {
                    onShareAnimated("Animated snap using $selectedMotion! ✨", "Animated Video", true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Video to Home ⚡", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 4. Create & Edit Images ---
@Composable
fun ImageEditorSubScreen(
    onShareImage: (caption: String, filterTag: String, isStory: Boolean) -> Unit
) {
    var prompt by remember { mutableStateOf("Futuristic cyber festival stage with glowing neon crowd") }
    var brightness by remember { mutableStateOf(1.0f) }
    var contrast by remember { mutableStateOf(1.0f) }
    var saturation by remember { mutableStateOf(1.0f) }
    var activeSticker by remember { mutableStateOf("⚡") }
    var isGeneratingImage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = "🎨 Create & Edit Images", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(
                text = "Generate custom digital artwork or edit your snaps with professional sliders.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Image Creation Prompt
        item {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("AI Art / Image Prompt") },
                modifier = Modifier.fillMaxWidth().testTag("image_prompt_input"),
                shape = RoundedCornerShape(14.dp),
                maxLines = 2
            )
        }

        item {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isGeneratingImage = true
                        delay(1200)
                        isGeneratingImage = false
                    }
                },
                enabled = !isGeneratingImage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
            ) {
                Text(if (isGeneratingImage) "Generating Art..." else "✨ Generate New Image", fontWeight = FontWeight.Bold)
            }
        }

        // Canvas preview with adjustment controls
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.feed_cyber_snap_1792586101459),
                    contentDescription = "Editing image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = brightness.coerceIn(0.5f, 1.2f)
                            scaleX = contrast.coerceIn(0.8f, 1.2f)
                            scaleY = contrast.coerceIn(0.8f, 1.2f)
                        },
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = activeSticker,
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                )
            }
        }

        // Editing Sliders
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Brightness (${String.format("%.1f", brightness)})", fontSize = 12.sp)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.5f..1.5f)

                Text(text = "Contrast (${String.format("%.1f", contrast)})", fontSize = 12.sp)
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.8f..1.4f)

                Text(text = "Add Creative Sticker:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("⚡", "🔥", "✨", "👑", "💖", "🎧").forEach { sticker ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { activeSticker = sticker },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = sticker, fontSize = 20.sp)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    onShareImage("Custom edited snap with $activeSticker sticker! 🎨", "Creative Studio", false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Post Edited Snap to Home ⚡", fontWeight = FontWeight.Bold)
            }
        }
    }
}
