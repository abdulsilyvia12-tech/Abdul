package com.example.ui.components.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

data class DrawnStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isGlow: Boolean = false
)

@Composable
fun CameraDrawingCanvas(
    isDrawingModeActive: Boolean,
    drawnStrokes: MutableList<DrawnStroke>,
    currentStrokePoints: MutableList<Offset>,
    currentPenColor: Color,
    currentStrokeWidth: Float,
    isGlowPen: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isDrawingModeActive) {
                if (isDrawingModeActive) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStrokePoints.clear()
                            currentStrokePoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStrokePoints.add(change.position)
                        },
                        onDragEnd = {
                            if (currentStrokePoints.size > 1) {
                                drawnStrokes.add(
                                    DrawnStroke(
                                        points = currentStrokePoints.toList(),
                                        color = currentPenColor,
                                        strokeWidth = currentStrokeWidth,
                                        isGlow = isGlowPen
                                    )
                                )
                            }
                            currentStrokePoints.clear()
                        },
                        onDragCancel = {
                            currentStrokePoints.clear()
                        }
                    )
                }
            }
            .testTag("camera_drawing_canvas")
    ) {
        // Draw completed strokes
        drawnStrokes.forEach { stroke ->
            if (stroke.points.size > 1) {
                if (stroke.isGlow) {
                    // Outer diffuse neon aura
                    for (i in 0 until stroke.points.size - 1) {
                        drawLine(
                            color = stroke.color.copy(alpha = 0.35f),
                            start = stroke.points[i],
                            end = stroke.points[i + 1],
                            strokeWidth = stroke.strokeWidth * 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                // Crisp core line
                for (i in 0 until stroke.points.size - 1) {
                    drawLine(
                        color = stroke.color,
                        start = stroke.points[i],
                        end = stroke.points[i + 1],
                        strokeWidth = stroke.strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw in-progress stroke
        if (currentStrokePoints.size > 1) {
            if (isGlowPen) {
                for (i in 0 until currentStrokePoints.size - 1) {
                    drawLine(
                        color = currentPenColor.copy(alpha = 0.35f),
                        start = currentStrokePoints[i],
                        end = currentStrokePoints[i + 1],
                        strokeWidth = currentStrokeWidth * 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            for (i in 0 until currentStrokePoints.size - 1) {
                drawLine(
                    color = currentPenColor,
                    start = currentStrokePoints[i],
                    end = currentStrokePoints[i + 1],
                    strokeWidth = currentStrokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun CameraDrawingToolbar(
    visible: Boolean,
    currentPenColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    isGlowPen: Boolean,
    onToggleGlow: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
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
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.5.dp, BlinkCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colors & Glow Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val penColors = listOf(
                    Color(0xFF06B6D4), // Cyan
                    Color(0xFFEC4899), // Pink
                    Color(0xFFFACC15), // Yellow
                    Color(0xFF10B981), // Lime Green
                    Color(0xFFA855F7), // Purple
                    Color(0xFFFFFFFF)  // White
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    penColors.forEach { color ->
                        val isSelected = currentPenColor == color
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                                    CircleShape
                                )
                                .clickable { onColorSelected(color) }
                                .testTag("pen_color_${color.value}")
                        )
                    }
                }

                // Glow Mode Toggle
                Surface(
                    onClick = onToggleGlow,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGlowPen) BlinkCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isGlowPen) BlinkCyan else Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (isGlowPen) "✨ Glow ON" else "✨ Glow",
                        color = if (isGlowPen) BlinkCyan else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Stroke Width & Action buttons (Undo, Clear, Done)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brush sizes
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(4f to "S", 8f to "M", 16f to "L").forEach { (width, label) ->
                        val isSelected = currentStrokeWidth == width
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BlinkPrimary else Color.White.copy(alpha = 0.15f))
                                .clickable { onStrokeWidthSelected(width) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onUndo,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("drawing_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("drawing_clear_button")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear All", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("drawing_done_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
