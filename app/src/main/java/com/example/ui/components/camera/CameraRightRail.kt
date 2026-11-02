package com.example.ui.components.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkCyanLight
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun CameraRailButton(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Brush.linearGradient(listOf(BlinkPink, BlinkCyan))
                    else Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.60f), Color.Black.copy(alpha = 0.60f)))
                )
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isActive) BlinkCyanLight else Color.White.copy(alpha = 0.9f),
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Vertical Right Tool Rail in Camera Tab.
 * Conditional Visibility System:
 * 1. The entire rail automatically disappears during video recording, drawing mode, or text entry.
 * 2. Creators can manually collapse/hide features using the top toggle button for a clean viewfinder.
 * 3. Individual buttons appear/disappear according to context:
 *    - "Video Edit" only appears in Video mode.
 *    - "Renew Filter" and "Change Filter" only appear when a creative filter is active.
 *    - "Shader FX" opens real-time OpenGL/AGSL parameter tuning.
 */
@Composable
fun CameraRightRail(
    isTalkingSelfieActive: Boolean,
    onToggleTalkingSelfie: () -> Unit,
    isTextActive: Boolean,
    onOpenTextTool: () -> Unit,
    hasMusicSelected: Boolean,
    onOpenMusicPicker: () -> Unit,
    isVideoMode: Boolean,
    onOpenVideoEdit: () -> Unit,
    isCreativeFilterActive: Boolean,
    onOpenAddFilter: () -> Unit,
    onRenewFilter: () -> Unit,
    onChangeFilter: () -> Unit,
    onOpenShaderTweak: () -> Unit,
    isFacePaintingActive: Boolean,
    onOpenFacePainting: () -> Unit,
    isDrawingModeActive: Boolean,
    onToggleDrawingMode: () -> Unit,
    isSoundboardActive: Boolean,
    onToggleSoundboard: () -> Unit,
    isCollapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isCollapsed) {
        // Collapsed Minimal Tool Trigger Pill
        Surface(
            modifier = modifier
                .statusBarsPadding()
                .padding(top = 70.dp, end = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onToggleCollapsed)
                .testTag("camera_expand_rail_pill"),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(1.dp, BlinkCyanLight.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "⚡", fontSize = 12.sp)
                Text(
                    text = "Tools",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Expand Tools",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        return
    }

    // Full Expanded Vertical Right Rail
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 64.dp, end = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(26.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. Collapse/Hide Rail Toggle (Allows creators to make rail disappear on demand)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(onClick = onToggleCollapsed)
                .testTag("camera_collapse_rail_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Hide tools",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }

        // 1. The round thing for talking selfies on faces
        CameraRailButton(
            icon = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTalkingSelfieActive) Brush.linearGradient(listOf(BlinkPink, BlinkCyan))
                            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                        )
                        .border(
                            if (isTalkingSelfieActive) 2.dp else 1.dp,
                            if (isTalkingSelfieActive) Color.White else Color.White.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🗣️", fontSize = 15.sp)
                }
            },
            label = "Talking",
            isActive = isTalkingSelfieActive,
            testTag = "camera_talking_selfie_button",
            onClick = onToggleTalkingSelfie
        )

        // 2. Text (T)
        CameraRailButton(
            icon = {
                Text(
                    text = "T",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isTextActive) BlinkCyanLight else Color.White
                )
            },
            label = "Text (T)",
            isActive = isTextActive,
            testTag = "camera_text_tool_button",
            onClick = onOpenTextTool
        )

        // 3. Music (Any song from any singer or artist: Taylor Swift, Adele, Drake, Billie Eilish, etc.)
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = if (hasMusicSelected) BlinkPink else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Music",
            isActive = hasMusicSelected,
            testTag = "camera_music_tool_button",
            onClick = onOpenMusicPicker
        )

        // 4. Video Edit Studio (Disappears in photo mode, appears in video mode)
        if (isVideoMode) {
            CameraRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Video Edit",
                        tint = BlinkCyanLight,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = "Video Edit",
                isActive = false,
                testTag = "camera_video_edit_button",
                onClick = onOpenVideoEdit
            )
        }

        // 5. Add Filter
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Add Filter",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Filter",
            isActive = false,
            testTag = "camera_add_filter_button",
            onClick = onOpenAddFilter
        )

        // 6. Shader FX Tuning (Real-time OpenGL / Compose shader adjustments)
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Shader FX Tuning",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Shader FX",
            isActive = false,
            testTag = "camera_shader_tuning_button",
            onClick = onOpenShaderTweak
        )

        // 7. Renew Filter & 8. Change Filter (Appear only when a creative filter is active!)
        if (isCreativeFilterActive) {
            CameraRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Renew Filter",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = "Renew",
                isActive = false,
                testTag = "camera_renew_filter_button",
                onClick = onRenewFilter
            )

            CameraRailButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Change Filter",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = "Change",
                isActive = false,
                testTag = "camera_change_filter_button",
                onClick = onChangeFilter
            )
        }

        // 9. Face Painting
        CameraRailButton(
            icon = {
                Text(text = "🎭", fontSize = 15.sp)
            },
            label = "Face Paint",
            isActive = isFacePaintingActive,
            testTag = "camera_face_painting_button",
            onClick = onOpenFacePainting
        )

        // 10. Drawings (Doodle on screen)
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Brush,
                    contentDescription = "Drawings",
                    tint = if (isDrawingModeActive) BlinkCyanLight else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Drawings",
            isActive = isDrawingModeActive,
            testTag = "camera_drawings_tool_button",
            onClick = onToggleDrawingMode
        )

        // 11. Soundboard FX
        CameraRailButton(
            icon = {
                Text(text = "📢", fontSize = 15.sp)
            },
            label = "Sounds",
            isActive = isSoundboardActive,
            testTag = "camera_soundboard_button",
            onClick = onToggleSoundboard
        )
    }
}
