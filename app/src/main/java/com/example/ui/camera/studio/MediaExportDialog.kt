package com.example.ui.camera.studio

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.ui.camera.export.ExportResult
import com.example.ui.camera.export.MediaExportEngine
import com.example.ui.theme.*

/**
 * High-Polished Export Dialog & Progress Dialog for Photos & Videos.
 */
@Composable
fun MediaExportProgressDialog(
    progress: Float,
    isVideo: Boolean
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BlinkCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("media_export_progress_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Photo,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (isVideo) "Mixing & Exporting Video..." else "Rendering High-Res Photo...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isVideo) "Synthesizing video frames with dual-channel audio mixing" else "Composing shaders, AR lenses, text and stickers",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Linear Progress with Percentage
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = BlinkCyan,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rendering", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlinkCyanLight)
                    }
                }
            }
        }
    }
}

@Composable
fun MediaExportSuccessDialog(
    context: Context,
    result: ExportResult,
    onDismiss: () -> Unit,
    onPostToStory: () -> Unit,
    onPostToFeed: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, BlinkCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("media_export_success_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (result.isVideo) "Video Exported Successfully! 🎉" else "Photo Exported Successfully! 🎉",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                // Export File Details Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("📁 Format:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (result.isVideo) "MP4 Video" else "JPEG Image", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("📐 Resolution:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(result.resolution, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("💾 File Size:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(result.fileSizeFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (result.isVideo && result.durationSeconds > 0) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("⏱️ Duration:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${result.durationSeconds}s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("📥 Location:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (result.isVideo) "Gallery / Movies" else "Gallery / Pictures", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }

                // Action Buttons
                // 1. Share via Android System Sheet
                Button(
                    onClick = {
                        MediaExportEngine.shareExportedMedia(context, result)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("share_exported_media_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Media ↗️", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // 2. Post to Story
                OutlinedButton(
                    onClick = onPostToStory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("export_post_to_story_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Post as Story 🌟", fontWeight = FontWeight.SemiBold)
                }

                // 3. Close
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
