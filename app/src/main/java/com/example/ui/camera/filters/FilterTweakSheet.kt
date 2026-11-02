package com.example.ui.camera.filters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreativeFilter
import com.example.ui.theme.BlinkCyan
import com.example.ui.theme.BlinkCyanLight
import com.example.ui.theme.BlinkPink
import com.example.ui.theme.BlinkPrimary

/**
 * Bottom Sheet for Real-Time Camera Shader & Filter Tuning.
 * Allows tweaking filter intensity, warmth/color temperature, scanlines, and shader parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTweakSheet(
    filter: CreativeFilter,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    warmth: Float,
    onWarmthChange: (Float) -> Unit,
    hasScanlines: Boolean,
    onToggleScanlines: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 40.dp, height = 4.dp),
                shape = RoundedCornerShape(2.dp),
                color = Color.White.copy(alpha = 0.3f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .testTag("filter_tweak_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = filter.iconEmoji, fontSize = 22.sp)
                    Column {
                        Text(
                            text = "${filter.name} Shader Tuning",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (filter.shaderType != CameraShaderType.NONE) "GPU AGSL / Compose Shader Active" else "Color Grading Profile",
                            color = BlinkCyanLight,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // 1. Intensity Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Intensity",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(intensity * 100).toInt()}%",
                        color = BlinkCyanLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = BlinkCyan,
                        activeTrackColor = BlinkCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("intensity_slider")
                )
            }

            // 2. Warmth / Color Temperature Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color Temperature / Warmth",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            warmth > 0.1f -> "Warm +${(warmth * 100).toInt()}%"
                            warmth < -0.1f -> "Cool ${(warmth * 100).toInt()}%"
                            else -> "Neutral"
                        },
                        color = if (warmth > 0f) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = warmth,
                    onValueChange = onWarmthChange,
                    valueRange = -1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (warmth >= 0f) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                        activeTrackColor = BlinkPrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("warmth_slider")
                )
            }

            // 3. Scanlines CRT Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CRT Retro Scanlines",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Add vintage analog television raster lines",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = hasScanlines,
                    onCheckedChange = onToggleScanlines,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BlinkCyan,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("scanlines_switch")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Reset Defaults", color = Color.White, fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                ) {
                    Text("Apply & Close", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
