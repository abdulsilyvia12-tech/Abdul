package com.example.ui.camera.filters

import android.graphics.ColorMatrixColorFilter
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import com.example.data.model.CreativeFilter
import kotlin.random.Random

/**
 * Real-time Camera Filter & Shader Rendering Layer.
 * Renders OpenGL / AGSL RuntimeShaders and Compose GPU ColorMatrix pipelines
 * over the live CameraX viewfinder and captured review preview.
 */
@Composable
fun RealtimeShaderPreviewOverlay(
    filter: CreativeFilter,
    intensity: Float,
    warmth: Float,
    isComparingOriginal: Boolean,
    modifier: Modifier = Modifier
) {
    if (isComparingOriginal || (filter.id == "normal" && filter.shaderType == CameraShaderType.NONE && warmth == 0f)) {
        return
    }

    // Continuous time animation for dynamic shaders (VHS waves, glitch, neon pulse, chromatic drift)
    val infiniteTransition = rememberInfiniteTransition(label = "shader_clock")
    val timeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeSec"
    )

    val colorMatrix = remember(filter.shaderType, intensity, warmth) {
        CameraShaderEngine.getColorMatrix(filter.shaderType, intensity, warmth)
    }

    val androidColorMatrix = remember(colorMatrix) {
        android.graphics.ColorMatrix(colorMatrix.values)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Apply hardware RenderEffects on Android 12+ (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val cfEffect = android.graphics.RenderEffect.createColorFilterEffect(
                            ColorMatrixColorFilter(androidColorMatrix)
                        )
                        val agslEffect = if (CameraShaderEngine.isAgslSupported && filter.shaderType != CameraShaderType.NONE && size.width > 0 && size.height > 0) {
                            CameraShaderEngine.createAgslRenderEffect(
                                shaderType = filter.shaderType,
                                width = size.width,
                                height = size.height,
                                timeSec = timeSec,
                                intensity = intensity
                            )
                        } else null

                        val combined = if (agslEffect != null) {
                            android.graphics.RenderEffect.createChainEffect(agslEffect, cfEffect)
                        } else {
                            cfEffect
                        }
                        this.renderEffect = combined.asComposeRenderEffect()
                    } catch (e: Exception) {
                        // Handled gracefully
                    }
                }
            }
    ) {
        // Universal Canvas Post-Processing Shaders (Scanlines, Vignette, Film Grain, Fallback RGB split)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // On devices < Android 12, apply ColorFilter via Canvas overlay
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && filter.tintColor != Color.Transparent) {
                drawRect(
                    color = filter.tintColor.copy(alpha = (filter.vignetteAlpha * intensity).coerceIn(0.1f, 0.45f)),
                    blendMode = BlendMode.Overlay
                )
            }

            // 1. Vignette Radial Falloff
            if (filter.vignetteAlpha > 0.05f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = (filter.vignetteAlpha * intensity).coerceIn(0f, 0.9f))
                        ),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w / 1.5f).coerceAtLeast(h / 1.8f)
                    )
                )
            }

            // 2. Animated CRT Scanlines
            if (filter.scanlines || filter.shaderType == CameraShaderType.VHS_RETRO) {
                val scanlineAlpha = (0.16f * intensity).coerceIn(0.05f, 0.35f)
                val lineSpacing = 6f
                val offset = (timeSec * 20f) % lineSpacing
                var y = offset
                while (y < h) {
                    drawLine(
                        color = Color.Black.copy(alpha = scanlineAlpha),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 2f
                    )
                    y += lineSpacing
                }
            }

            // 3. Cinematic Film Grain / Digital Glitch Noise
            if (filter.hasParticles || filter.shaderType == CameraShaderType.NOIR_FILM || filter.shaderType == CameraShaderType.VINTAGE_KODACHROME) {
                val grainAlpha = 0.07f * intensity
                val seed = (timeSec * 1000).toInt()
                val rng = Random(seed)
                for (i in 0 until 40) {
                    val gx = rng.nextFloat() * w
                    val gy = rng.nextFloat() * h
                    val gRadius = rng.nextFloat() * 1.8f + 0.8f
                    drawCircle(
                        color = if (rng.nextBoolean()) Color.White.copy(alpha = grainAlpha) else Color.Black.copy(alpha = grainAlpha),
                        radius = gRadius,
                        center = Offset(gx, gy)
                    )
                }
            }

            // 4. Matrix Digital Stream fallback
            if (filter.shaderType == CameraShaderType.EMERALD_MATRIX) {
                val matrixAlpha = 0.12f * intensity
                val colSpacing = 36f
                var x = 18f
                while (x < w) {
                    val streamY = ((timeSec * 120f + x * 7f) % (h + 200f)) - 100f
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF10B981).copy(alpha = matrixAlpha), Color.Transparent),
                            startY = streamY - 80f,
                            endY = streamY + 80f
                        ),
                        start = Offset(x, streamY - 80f),
                        end = Offset(x, streamY + 80f),
                        strokeWidth = 3f
                    )
                    x += colSpacing
                }
            }
        }
    }
}
