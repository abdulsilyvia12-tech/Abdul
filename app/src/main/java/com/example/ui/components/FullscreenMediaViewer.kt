package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.ChatMessage
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fullscreen interactive media viewer supporting smooth pinch-to-zoom,
 * pan gestures, double-tap zoom, on-screen zoom controls, and video playback overlays.
 */
@Composable
fun FullscreenMediaViewer(
    message: ChatMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVideo = message.mediaType.equals("video", ignoreCase = true)
    val haptics = LocalHapticFeedback.current

    // Gesture states for pinch-to-zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Video play/pause toggle
    var isVideoPlaying by remember { mutableStateOf(true) }

    // Toggle chrome overlay visibility on single tap
    var isChromeVisible by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen_media_viewer")
                .testTag("fullscreen_chat_media_dialog")
                .onSizeChanged { containerSize = it }
        ) {
            // Interactive gesture area for Pinch-to-Zoom, Pan & Double-tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Single tap toggles controls overlay
                                isChromeVisible = !isChromeVisible
                            },
                            onDoubleTap = { tapOffset ->
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (scale > 1.2f) {
                                    // Reset zoom
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    // Zoom in centered near tap point
                                    scale = 2.5f
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val panX = (centerX - tapOffset.x) * 1.5f
                                    val panY = (centerY - tapOffset.y) * 1.5f
                                    offset = Offset(panX, panY)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val oldScale = scale
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale

                            if (newScale > 1f) {
                                // Allow panning bounded by container bounds
                                val maxOffsetX = (size.width * (newScale - 1f)) / 2f
                                val maxOffsetY = (size.height * (newScale - 1f)) / 2f

                                offset = Offset(
                                    x = (offset.x + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Media Image / Video Frame with graphics transform layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = message.mediaUri,
                        contentDescription = if (isVideo) "Fullscreen Video Viewer" else "Fullscreen Photo Viewer",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("fullscreen_media_image"),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = BlinkCyanLight,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        },
                        error = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Failed to load",
                                    tint = BlinkPink,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Unable to load media preview",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    )

                    // Video Play / Pause Center Overlay
                    if (isVideo) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    isVideoPlaying = !isVideoPlaying
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(2.dp, BlinkCyanLight, CircleShape)
                                    .testTag("fullscreen_video_play_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isVideoPlaying) "Pause Video" else "Play Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top Header Bar Scrim & Controls
            AnimatedVisibility(
                visible = isChromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                .testTag("fullscreen_media_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }

                        // Media Type Badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BlinkCyanLight.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isVideo) "Video Snap" else "Photo Snap",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Reset Zoom button in header if zoomed in
                        if (scale > 1.05f) {
                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BlinkPrimary.copy(alpha = 0.7f))
                                    .testTag("media_viewer_zoom_reset_header")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Reset Zoom",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }

            // Floating Zoom Control Toolbar (Pinch-to-zoom helper & percentage display)
            AnimatedVisibility(
                visible = isChromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.testTag("fullscreen_media_zoom_controls")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        // Zoom in
                        IconButton(
                            onClick = {
                                val nextScale = (scale + 0.5f).coerceAtMost(5f)
                                scale = nextScale
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("media_viewer_zoom_in")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Zoom In",
                                tint = Color.White
                            )
                        }

                        // Current zoom level indicator
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = BlinkCyanLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("media_viewer_zoom_level")
                        )

                        // Zoom out
                        IconButton(
                            onClick = {
                                val nextScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = nextScale
                                if (nextScale == 1f) offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("media_viewer_zoom_out")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Zoom Out",
                                tint = Color.White
                            )
                        }

                        // Reset button
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("media_viewer_zoom_reset")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Zoom to 100%",
                                tint = if (scale > 1f) BlinkAmber else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Bottom Caption & Media Metadata Bar
            AnimatedVisibility(
                visible = isChromeVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (message.text.isNotBlank()) {
                                Text(
                                    text = message.text,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${if (message.isFromMe) "You" else message.senderName.ifBlank { "Friend" }} • ${message.timeFormatted}",
                                    color = BlinkTextSecondary,
                                    fontSize = 12.sp
                                )

                                Text(
                                    text = "Pinch or double-tap to zoom",
                                    color = BlinkCyanLight.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
