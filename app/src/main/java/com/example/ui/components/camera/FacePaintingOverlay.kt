package com.example.ui.components.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkCyanLight
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

data class FacePaintOption(
    val id: String,
    val name: String,
    val emoji: String,
    val primaryColor: Color,
    val description: String
)

val facePaintOptions = listOf(
    FacePaintOption("none", "Natural", "🚫", Color.Gray, "No face paint"),
    FacePaintOption("tiger", "Neon Tiger", "🐯", Color(0xFFF97316), "Glowing whiskers and forehead stripes"),
    FacePaintOption("butterfly", "Butterfly Mask", "🦋", Color(0xFF06B6D4), "Prismatic glowing wings over eyes"),
    FacePaintOption("skull", "Neon Skull", "💀", Color(0xFFA855F7), "Sugar skull floral eyes and stitch smile"),
    FacePaintOption("rainbow", "Rainbow Cheeks", "🌈", Color(0xFFF43F5E), "Vibrant rainbow bars and star sparkles"),
    FacePaintOption("jester", "Circus Clown", "🤡", Color(0xFFEF4444), "Red clown nose and diamond eye paint"),
    FacePaintOption("warrior", "Cyber Warrior", "⚔️", Color(0xFF10B981), "Neon HUD cyber warpaint slashes")
)

@Composable
fun FacePaintingCanvasOverlay(
    activePaintType: String,
    modifier: Modifier = Modifier
) {
    if (activePaintType == "none") return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h * 0.38f // Center face region

        when (activePaintType) {
            "tiger" -> {
                // Orange & neon yellow tiger whiskers and nose
                val orange = Color(0xFFF97316)
                val yellow = Color(0xFFFDE047)

                // Nose triangle
                drawCircle(color = orange, radius = 18f, center = Offset(centerX, centerY))

                // Left Whiskers
                drawLine(color = yellow, start = Offset(centerX - 30f, centerY - 8f), end = Offset(centerX - 130f, centerY - 28f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = orange, start = Offset(centerX - 35f, centerY + 10f), end = Offset(centerX - 145f, centerY + 10f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = yellow, start = Offset(centerX - 30f, centerY + 28f), end = Offset(centerX - 130f, centerY + 48f), strokeWidth = 8f, cap = StrokeCap.Round)

                // Right Whiskers
                drawLine(color = yellow, start = Offset(centerX + 30f, centerY - 8f), end = Offset(centerX + 130f, centerY - 28f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = orange, start = Offset(centerX + 35f, centerY + 10f), end = Offset(centerX + 145f, centerY + 10f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = yellow, start = Offset(centerX + 30f, centerY + 28f), end = Offset(centerX + 130f, centerY + 48f), strokeWidth = 8f, cap = StrokeCap.Round)

                // Forehead Tiger Stripes
                drawLine(color = orange, start = Offset(centerX, centerY - 140f), end = Offset(centerX, centerY - 80f), strokeWidth = 10f, cap = StrokeCap.Round)
                drawLine(color = yellow, start = Offset(centerX - 40f, centerY - 130f), end = Offset(centerX - 15f, centerY - 90f), strokeWidth = 9f, cap = StrokeCap.Round)
                drawLine(color = yellow, start = Offset(centerX + 40f, centerY - 130f), end = Offset(centerX + 15f, centerY - 90f), strokeWidth = 9f, cap = StrokeCap.Round)
            }

            "butterfly" -> {
                // Cyan and Pink Butterfly Wings Mask around eyes
                val cyan = Color(0xFF06B6D4)
                val pink = Color(0xFFEC4899)

                // Left Wing Arches
                drawArc(
                    color = cyan.copy(alpha = 0.55f),
                    startAngle = 135f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - 160f, centerY - 110f),
                    size = Size(130f, 95f),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = pink.copy(alpha = 0.55f),
                    startAngle = 120f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(centerX - 145f, centerY - 45f),
                    size = Size(110f, 85f),
                    style = Stroke(width = 9f, cap = StrokeCap.Round)
                )

                // Right Wing Arches
                drawArc(
                    color = cyan.copy(alpha = 0.55f),
                    startAngle = 225f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX + 30f, centerY - 110f),
                    size = Size(130f, 95f),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = pink.copy(alpha = 0.55f),
                    startAngle = 270f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(centerX + 35f, centerY - 45f),
                    size = Size(110f, 85f),
                    style = Stroke(width = 9f, cap = StrokeCap.Round)
                )

                // Center Antennae
                drawLine(color = Color.White, start = Offset(centerX, centerY - 60f), end = Offset(centerX - 25f, centerY - 110f), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(color = Color.White, start = Offset(centerX, centerY - 60f), end = Offset(centerX + 25f, centerY - 110f), strokeWidth = 5f, cap = StrokeCap.Round)
            }

            "skull" -> {
                // Purple and Teal Sugar Skull
                val purple = Color(0xFFA855F7)
                val teal = Color(0xFF14B8A6)

                // Floral Eye Outlines
                drawCircle(color = purple.copy(alpha = 0.6f), radius = 38f, center = Offset(centerX - 70f, centerY - 45f), style = Stroke(width = 7f))
                drawCircle(color = purple.copy(alpha = 0.6f), radius = 38f, center = Offset(centerX + 70f, centerY - 45f), style = Stroke(width = 7f))

                // Nose Spade
                drawCircle(color = Color.Black.copy(alpha = 0.8f), radius = 14f, center = Offset(centerX, centerY + 5f))

                // Stitched Teeth Smile
                drawLine(color = teal, start = Offset(centerX - 80f, centerY + 65f), end = Offset(centerX + 80f, centerY + 65f), strokeWidth = 6f, cap = StrokeCap.Round)
                for (step in -60..60 step 20) {
                    drawLine(color = teal, start = Offset(centerX + step, centerY + 52f), end = Offset(centerX + step, centerY + 78f), strokeWidth = 5f, cap = StrokeCap.Round)
                }
            }

            "rainbow" -> {
                // Pastel rainbow stripes on cheeks
                val colors = listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFACC15), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6))
                colors.forEachIndexed { i, c ->
                    val yOff = (i * 7).toFloat()
                    // Left Cheek
                    drawLine(color = c, start = Offset(centerX - 130f, centerY + 10f + yOff), end = Offset(centerX - 60f, centerY + 10f + yOff), strokeWidth = 6f, cap = StrokeCap.Round)
                    // Right Cheek
                    drawLine(color = c, start = Offset(centerX + 60f, centerY + 10f + yOff), end = Offset(centerX + 130f, centerY + 10f + yOff), strokeWidth = 6f, cap = StrokeCap.Round)
                }
            }

            "jester" -> {
                // Red nose and blue diamonds
                val blue = Color(0xFF38BDF8)
                val red = Color(0xFFEF4444)

                // Clown red nose
                drawCircle(color = red, radius = 24f, center = Offset(centerX, centerY + 5f))

                // Left Diamond Eye
                drawLine(color = blue, start = Offset(centerX - 70f, centerY - 80f), end = Offset(centerX - 70f, centerY - 15f), strokeWidth = 8f, cap = StrokeCap.Round)
                // Right Diamond Eye
                drawLine(color = blue, start = Offset(centerX + 70f, centerY - 80f), end = Offset(centerX + 70f, centerY - 15f), strokeWidth = 8f, cap = StrokeCap.Round)

                // Wide red smile
                drawArc(
                    color = red,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - 80f, centerY + 25f),
                    size = Size(160f, 65f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }

            "warrior" -> {
                // Cyberpunk HUD Neon Warpaint
                val green = Color(0xFF10B981)
                val cyan = Color(0xFF06B6D4)

                // Diagonal eye slashes
                drawLine(color = green, start = Offset(centerX - 140f, centerY - 65f), end = Offset(centerX - 50f, centerY - 25f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = cyan, start = Offset(centerX - 130f, centerY - 45f), end = Offset(centerX - 55f, centerY - 10f), strokeWidth = 7f, cap = StrokeCap.Round)

                drawLine(color = green, start = Offset(centerX + 140f, centerY - 65f), end = Offset(centerX + 50f, centerY - 25f), strokeWidth = 8f, cap = StrokeCap.Round)
                drawLine(color = cyan, start = Offset(centerX + 130f, centerY - 45f), end = Offset(centerX + 55f, centerY - 10f), strokeWidth = 7f, cap = StrokeCap.Round)

                // Forehead Cyber Visor bar
                drawLine(color = cyan, start = Offset(centerX - 75f, centerY - 110f), end = Offset(centerX + 75f, centerY - 110f), strokeWidth = 6f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun FacePaintingStudioSheet(
    visible: Boolean,
    currentPaintType: String,
    onSelectPaintType: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.90f))
                .border(1.5.dp, BlinkPink.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎭", fontSize = 18.sp)
                    Text("AR Face Painting Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(facePaintOptions, key = { it.id }) { paint ->
                    val isSelected = currentPaintType == paint.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectPaintType(paint.id) }
                            .testTag("face_paint_option_${paint.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(BlinkPink, BlinkCyan))
                                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                )
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(paint.emoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = paint.name,
                            color = if (isSelected) BlinkCyanLight else Color.White,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
