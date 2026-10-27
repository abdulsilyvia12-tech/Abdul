package com.example.ui.components.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

@Composable
fun CameraTextEditorDialog(
    visible: Boolean,
    currentText: String,
    currentColor: Color,
    currentBgStyle: String,
    onSaveText: (String, Color, String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    var textInput by remember { mutableStateOf(currentText) }
    var selectedColor by remember { mutableStateOf(currentColor) }
    var selectedBgStyle by remember { mutableStateOf(currentBgStyle) }

    val colors = listOf(
        Color(0xFFFFFFFF), // White
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899), // Pink
        Color(0xFFFACC15), // Yellow
        Color(0xFF10B981), // Lime Green
        Color(0xFFA855F7)  // Purple
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.5.dp, BlinkCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("camera_text_editor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Add Text Overlay (T)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (selectedBgStyle) {
                                    "pill" -> Color.Black.copy(alpha = 0.75f)
                                    "glow" -> BlinkPrimary.copy(alpha = 0.45f)
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                if (selectedBgStyle == "glow") 1.5.dp else 0.dp,
                                BlinkCyan,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (textInput.isBlank()) "Type something..." else textInput,
                            color = selectedColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Text Input
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Enter caption text...") },
                    modifier = Modifier.fillMaxWidth().testTag("camera_text_input"),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 2
                )

                // Color Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                // Background Style Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("pill" to "Pill Bar", "glow" to "Neon Glow", "none" to "Clean").forEach { (style, label) ->
                        val isSelected = selectedBgStyle == style
                        Surface(
                            onClick = { selectedBgStyle = style },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BlinkPrimary else Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, if (isSelected) BlinkCyan else Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            textInput = ""
                            onSaveText("", selectedColor, selectedBgStyle)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Clear Text", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSaveText(textInput, selectedColor, selectedBgStyle)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("save_camera_text_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                    ) {
                        Text("Apply Text", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
