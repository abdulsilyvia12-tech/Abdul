package com.example.ui.components.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Brush.linearGradient(listOf(BlinkPink, BlinkCyan))
                    else Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.55f)))
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
 * Vertical Right Tool Rail in Camera Tab (Top to Bottom):
 * 1. The round thing for talking selfies on faces
 * 2. Text (T)
 * 3. Music (Saved Internet music: Bongo Flava, Afrobeat, Dramatic, Comedy, EDM)
 * 4. Video Edit Studio
 * 5. Add Filter
 * 6. Renew Filter
 * 7. Change Filter
 * 8. Face Painting
 * 9. Drawings
 * 10. Soundboard FX (More intelligent & brave features)
 */
@Composable
fun CameraRightRail(
    isTalkingSelfieActive: Boolean,
    onToggleTalkingSelfie: () -> Unit,
    isTextActive: Boolean,
    onOpenTextTool: () -> Unit,
    hasMusicSelected: Boolean,
    onOpenMusicPicker: () -> Unit,
    onOpenVideoEdit: () -> Unit,
    onOpenAddFilter: () -> Unit,
    onRenewFilter: () -> Unit,
    onChangeFilter: () -> Unit,
    isFacePaintingActive: Boolean,
    onOpenFacePainting: () -> Unit,
    isDrawingModeActive: Boolean,
    onToggleDrawingMode: () -> Unit,
    isSoundboardActive: Boolean,
    onToggleSoundboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 64.dp, end = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.Black.copy(alpha = 0.40f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. The round thing for talking selfies on faces
        CameraRailButton(
            icon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
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
                    Text(text = "🗣️", fontSize = 16.sp)
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
                    fontSize = 19.sp,
                    color = if (isTextActive) BlinkCyanLight else Color.White
                )
            },
            label = "Text (T)",
            isActive = isTextActive,
            testTag = "camera_text_tool_button",
            onClick = onOpenTextTool
        )

        // 3. Music (Saved Internet Bongo, Afrobeat, Dramatic, Comedy)
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = if (hasMusicSelected) BlinkPink else Color.White,
                    modifier = Modifier.size(19.dp)
                )
            },
            label = "Music",
            isActive = hasMusicSelected,
            testTag = "camera_music_tool_button",
            onClick = onOpenMusicPicker
        )

        // 4. Video Edit Studio
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Video Edit",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Video Edit",
            isActive = false,
            testTag = "camera_video_edit_button",
            onClick = onOpenVideoEdit
        )

        // 5. Add Filter
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Add Filter",
                    tint = BlinkCyanLight,
                    modifier = Modifier.size(19.dp)
                )
            },
            label = "Add Filter",
            isActive = false,
            testTag = "camera_add_filter_button",
            onClick = onOpenAddFilter
        )

        // 6. Renew Filter
        CameraRailButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Renew Filter",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            },
            label = "Renew",
            isActive = false,
            testTag = "camera_renew_filter_button",
            onClick = onRenewFilter
        )

        // 7. Change Filter
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

        // 8. Face Painting
        CameraRailButton(
            icon = {
                Text(text = "🎭", fontSize = 16.sp)
            },
            label = "Face Paint",
            isActive = isFacePaintingActive,
            testTag = "camera_face_painting_button",
            onClick = onOpenFacePainting
        )

        // 9. Drawings
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

        // 10. Soundboard FX (More Intelligent & Brave features)
        CameraRailButton(
            icon = {
                Text(text = "📢", fontSize = 16.sp)
            },
            label = "Sounds",
            isActive = isSoundboardActive,
            testTag = "camera_soundboard_button",
            onClick = onToggleSoundboard
        )
    }
}
