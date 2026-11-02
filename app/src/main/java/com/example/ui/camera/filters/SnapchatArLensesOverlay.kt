package com.example.ui.camera.filters

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreativeFilter
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Snapchat & Banuba-style Real-Time Face AR Lenses & Beauty Engine.
 * Renders iconic Dog Ears with panting tongue, Cat whiskers, Golden Butterfly Crown,
 * Beauty Smooth & Rosy Makeup, Neon Horns, Angel Halo, Rainbow Stream, and Cyber Lenses.
 */
@Composable
fun SnapchatArLensesOverlay(
    filter: CreativeFilter,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    if (filter.arLensType.isBlank() && filter.beautySmoothing <= 0f && !filter.isLense) {
        return
    }

    // Continuous clock for fluttering wings, ears wagging, pulsing glow, and particle floating
    val infiniteTransition = rememberInfiniteTransition(label = "snapchat_ar_engine")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val earFlopAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earFlop"
    )

    val flutterOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flutterOffset"
    )

    val rainbowFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbowFlow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("snapchat_ar_lens_overlay_${filter.arLensType}")
    ) {
        // --- 1. Banuba Beauty Skin Smoothing & Rosy Cheek Blush Layer ---
        if (filter.beautySmoothing > 0f || filter.blushIntensity > 0f) {
            BeautySkinAndBlushLayer(
                smoothingAlpha = (filter.beautySmoothing * intensity * 0.28f).coerceIn(0f, 0.45f),
                blushAlpha = (filter.blushIntensity * intensity * 0.42f).coerceIn(0f, 0.65f),
                hasSparkles = filter.hasSparkles
            )
        }

        // --- 2. Specific Snapchat / Banuba AR Face Lenses ---
        when (filter.arLensType) {
            "dog_ears" -> {
                DogFaceArLens(earFlopAngle = earFlopAngle)
            }
            "cat_ears" -> {
                CatFaceArLens(pulseScale = pulseScale)
            }
            "butterfly_crown" -> {
                GoldenButterflyCrownArLens(flutterOffset = flutterOffset, pulseScale = pulseScale)
            }
            "beauty_glam" -> {
                BeautyGlamArLens(pulseScale = pulseScale)
            }
            "angel_halo" -> {
                AngelHaloArLens(pulseScale = pulseScale, flutterOffset = flutterOffset)
            }
            "neon_horns" -> {
                NeonDevilHornsArLens(pulseScale = pulseScale)
            }
            "rainbow_vomit" -> {
                RainbowVomitArLens(rainbowFlow = rainbowFlow, pulseScale = pulseScale)
            }
            "bambi_deer" -> {
                BambiDeerArLens(earFlopAngle = earFlopAngle)
            }
            "cyber_visor" -> {
                CyberVisorArLens(pulseScale = pulseScale)
            }
            "anime_eyes" -> {
                AnimeMangaEyesArLens(pulseScale = pulseScale)
            }
            "flame_crown" -> {
                FlameCrownArLens(flutterOffset = flutterOffset)
            }
            "sunglasses_gold" -> {
                SunglassesGoldChainArLens()
            }
            "rosy_blush" -> {
                RosyBlushFrecklesArLens(pulseScale = pulseScale)
            }
        }
    }
}

/**
 * 💄 Banuba Beauty Skin Radiance & Rosy Cheek Blush
 */
@Composable
private fun BeautySkinAndBlushLayer(
    smoothingAlpha: Float,
    blushAlpha: Float,
    hasSparkles: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Soft porcelain skin evening glow
        if (smoothingAlpha > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFEDD5).copy(alpha = smoothingAlpha),
                        Color(0xFFFED7AA).copy(alpha = smoothingAlpha * 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.55f
                )
            )
        }

        // Rosy cheek blush on left & right cheekbones
        if (blushAlpha > 0f) {
            val leftCheek = Offset(w * 0.32f, h * 0.46f)
            val rightCheek = Offset(w * 0.68f, h * 0.46f)
            val blushRadius = w * 0.14f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFB7185).copy(alpha = blushAlpha),
                        Color(0xFFFDA4AF).copy(alpha = blushAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = leftCheek,
                    radius = blushRadius
                ),
                radius = blushRadius,
                center = leftCheek
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFB7185).copy(alpha = blushAlpha),
                        Color(0xFFFDA4AF).copy(alpha = blushAlpha * 0.5f),
                        Color.Transparent
                    ),
                    center = rightCheek,
                    radius = blushRadius
                ),
                radius = blushRadius,
                center = rightCheek
            )
        }
    }
}

/**
 * 🐶 Classic Snapchat Dog Lens: Floppy animated ears, cute puppy nose button & panting tongue
 */
@Composable
private fun DogFaceArLens(earFlopAngle: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Left & Right Floppy Spotted Ears
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 95.dp, start = 28.dp, end = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Floppy Ear
            Box(
                modifier = Modifier
                    .rotate(earFlopAngle - 15f)
                    .size(width = 88.dp, height = 115.dp)
                    .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 30.dp, topStart = 20.dp, topEnd = 35.dp))
                    .background(Color(0xFF92400E))
                    .border(3.dp, Color(0xFF78350F), RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 30.dp, topStart = 20.dp, topEnd = 35.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Inner ear spot
                Box(
                    modifier = Modifier
                        .size(width = 45.dp, height = 70.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFFFDE68A).copy(alpha = 0.85f))
                )
            }

            // Right Floppy Ear
            Box(
                modifier = Modifier
                    .rotate(-earFlopAngle + 15f)
                    .size(width = 88.dp, height = 115.dp)
                    .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 50.dp, topStart = 35.dp, topEnd = 20.dp))
                    .background(Color(0xFF92400E))
                    .border(3.dp, Color(0xFF78350F), RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 50.dp, topStart = 35.dp, topEnd = 20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Inner ear spot
                Box(
                    modifier = Modifier
                        .size(width = 45.dp, height = 70.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xFFFDE68A).copy(alpha = 0.85f))
                )
            }
        }

        // Puppy Nose Button & Whiskers
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-15).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp, 34.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF18181B))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.TopStart
            ) {
                // Nose Specular Highlight Dot
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp, top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Whiskers dots
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(text = "• • •", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(text = "• • •", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        // Animated Floppy Pink Tongue extending down
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 48.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 72.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFB7185), Color(0xFFF43F5E))
                        )
                    )
                    .border(2.dp, Color(0xFFE11D48), RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 8.dp, topEnd = 8.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Tongue central crease line
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.5.dp)
                        .height(46.dp)
                        .background(Color(0xFFBE123C).copy(alpha = 0.6f))
                )
            }
        }
    }
}

/**
 * 🐱 Banuba & Snapchat Cat Ears Lens: Fluffy pink kitty ears, cute nose, and whiskers
 */
@Composable
private fun CatFaceArLens(pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Cat Ears on Top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 105.dp, start = 48.dp, end = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Cat Ear (Triangle with inner pink)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .rotate(-15f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = Path().apply {
                        moveTo(size.width * 0.5f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(p, Color(0xFF1E293B))
                    drawPath(p, Color.White, style = Stroke(4f))

                    // Inner pink fluff
                    val inner = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.25f)
                        lineTo(size.width * 0.85f, size.height * 0.9f)
                        lineTo(size.width * 0.15f, size.height * 0.9f)
                        close()
                    }
                    drawPath(inner, Color(0xFFF472B6))
                }
            }

            // Right Cat Ear
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .rotate(15f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val p = Path().apply {
                        moveTo(size.width * 0.5f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(p, Color(0xFF1E293B))
                    drawPath(p, Color.White, style = Stroke(4f))

                    val inner = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.25f)
                        lineTo(size.width * 0.85f, size.height * 0.9f)
                        lineTo(size.width * 0.15f, size.height * 0.9f)
                        close()
                    }
                    drawPath(inner, Color(0xFFF472B6))
                }
            }
        }

        // Cat Nose & Whiskers
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pink triangular nose
            Canvas(modifier = Modifier.size(24.dp, 18.dp)) {
                val np = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width * 0.5f, size.height)
                    close()
                }
                drawPath(np, Color(0xFFF43F5E))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Cat Whiskers
            Row(
                modifier = Modifier.width(220.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left whiskers
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(55.dp).height(2.5.dp).rotate(-6f).background(Color.White))
                    Box(modifier = Modifier.width(62.dp).height(2.5.dp).background(Color.White))
                    Box(modifier = Modifier.width(55.dp).height(2.5.dp).rotate(6f).background(Color.White))
                }
                // Right whiskers
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(55.dp).height(2.5.dp).rotate(6f).background(Color.White))
                    Box(modifier = Modifier.width(62.dp).height(2.5.dp).background(Color.White))
                    Box(modifier = Modifier.width(55.dp).height(2.5.dp).rotate(-6f).background(Color.White))
                }
            }
        }
    }
}

/**
 * 🦋 Golden Butterfly Crown: 3D fluttering golden butterflies around the head with star sparkles
 */
@Composable
private fun GoldenButterflyCrownArLens(flutterOffset: Float, pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Golden Butterfly Tiara Banner
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 95.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🦋",
                fontSize = 42.sp,
                modifier = Modifier
                    .offset(y = flutterOffset.dp)
                    .scale(pulseScale)
            )
            Text(
                text = "👑 ✨",
                fontSize = 36.sp,
                modifier = Modifier.scale(pulseScale * 1.05f)
            )
            Text(
                text = "🦋",
                fontSize = 42.sp,
                modifier = Modifier
                    .offset(y = (-flutterOffset).dp)
                    .scale(pulseScale)
            )
        }

        // Floating golden star sparkles across forehead and cheeks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val sparkles = listOf(
                Offset(w * 0.22f, h * 0.30f),
                Offset(w * 0.38f, h * 0.24f),
                Offset(w * 0.62f, h * 0.24f),
                Offset(w * 0.78f, h * 0.30f),
                Offset(w * 0.28f, h * 0.44f),
                Offset(w * 0.72f, h * 0.44f)
            )

            sparkles.forEachIndexed { i, pt ->
                val r = (6f + (i % 3) * 3f) * pulseScale
                drawCircle(Color(0xFFFDE047), radius = r, center = pt)
                drawLine(
                    color = Color.White,
                    start = Offset(pt.x - r * 1.6f, pt.y),
                    end = Offset(pt.x + r * 1.6f, pt.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(pt.x, pt.y - r * 1.6f),
                    end = Offset(pt.x, pt.y + r * 1.6f),
                    strokeWidth = 2f
                )
            }
        }
    }
}

/**
 * 💄 Banuba Beauty Glam: Rosy cheek blush, sparkling eyes, pink lip gloss
 */
@Composable
private fun BeautyGlamArLens(pulseScale: Float) {
    BeautySkinAndBlushLayer(smoothingAlpha = 0.35f, blushAlpha = 0.55f, hasSparkles = true)
    Box(modifier = Modifier.fillMaxSize()) {
        // Sparkle on temple
        Text(
            text = "✨",
            fontSize = 32.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 100.dp, y = (-70).dp)
                .scale(pulseScale)
        )
        Text(
            text = "✨",
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-95).dp, y = (-65).dp)
                .scale(pulseScale)
        )
    }
}

/**
 * 😇 Angel Halo Lens: Floating golden ring halo above head with ethereal glow
 */
@Composable
private fun AngelHaloArLens(pulseScale: Float, flutterOffset: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ethereal Golden Ring
            Box(
                modifier = Modifier
                    .offset(y = flutterOffset.dp)
                    .scale(pulseScale)
                    .size(width = 160.dp, height = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(6.dp, Color(0xFFFDE047), RoundedCornerShape(24.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "☁️ ✨ ☁️", fontSize = 28.sp)
        }
    }
}

/**
 * 😈 Neon Devil Horns: Cyberpunk magenta/cyan glowing horns with animated pulse
 */
@Composable
private fun NeonDevilHornsArLens(pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 95.dp, start = 60.dp, end = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Horn
            Text(
                text = "😈",
                fontSize = 58.sp,
                modifier = Modifier
                    .rotate(-15f)
                    .scale(pulseScale)
            )
            // Right Horn
            Text(
                text = "😈",
                fontSize = 58.sp,
                modifier = Modifier
                    .rotate(15f)
                    .scale(pulseScale)
            )
        }
    }
}

/**
 * 🌈 Snapchat Classic Rainbow Waterfall Lens
 */
@Composable
private fun RainbowVomitArLens(rainbowFlow: Float, pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Big Shimmering Eyes
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-70).dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Text(text = "🌟", fontSize = 52.sp, modifier = Modifier.scale(pulseScale))
            Text(text = "🌟", fontSize = 52.sp, modifier = Modifier.scale(pulseScale))
        }

        // Rainbow Cascade Streaming Down From Mouth to Bottom
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp)
                .fillMaxWidth(0.42f)
                .height(300.dp)
        ) {
            val colors = listOf(
                Color(0xFFEF4444),
                Color(0xFFF97316),
                Color(0xFFFBBF24),
                Color(0xFF22C55E),
                Color(0xFF06B6D4),
                Color(0xFF6366F1),
                Color(0xFFA855F7)
            )
            val stripWidth = size.width / colors.size
            colors.forEachIndexed { i, col ->
                drawRect(
                    color = col,
                    topLeft = Offset(i * stripWidth, 0f),
                    size = Size(stripWidth, size.height)
                )
            }
            // Sparkles on rainbow stream
            for (s in 0..4) {
                val y = (s * 60f + rainbowFlow * 60f) % size.height
                drawCircle(Color.White, radius = 6f, center = Offset(size.width * 0.5f + (s % 2 * 30f - 15f), y))
            }
        }
    }
}

/**
 * 🦌 Banuba Bambi / Deer Lens: Cute deer ears, white fawn spots & nose
 */
@Composable
private fun BambiDeerArLens(earFlopAngle: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Deer Ears
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 95.dp, start = 40.dp, end = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🦌", fontSize = 60.sp, modifier = Modifier.rotate(earFlopAngle - 10f))
            Text(text = "🦌", fontSize = 60.sp, modifier = Modifier.rotate(-earFlopAngle + 10f))
        }

        // Fawn Freckle Dots & Button Nose
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Text(text = "• • •", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "• • •", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp, 22.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

/**
 * 🤖 Cyber Visor HUD Lens: Sci-Fi glowing glasses with HUD crosshairs and telemetry
 */
@Composable
private fun CyberVisorArLens(pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-65).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Neon Cyan Visor
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF06B6D4).copy(alpha = 0.35f))
                    .border(2.5.dp, Color(0xFF22D3EE), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SYS.OK 98.4%", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("🎯 TARGET LOCK", color = Color(0xFF67E8F9), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("⚡ BLINK_AI", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * ✨ Big Anime Manga Eyes Lens
 */
@Composable
private fun AnimeMangaEyesArLens(pulseScale: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-70).dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B4B))
                    .border(4.dp, Color(0xFFA855F7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✨👀✨", fontSize = 28.sp)
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B4B))
                    .border(4.dp, Color(0xFFA855F7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✨👀✨", fontSize = 28.sp)
            }
        }
    }
}

/**
 * 🔥 Flame Crown Lens: Flickering cartoon comic flames over head
 */
@Composable
private fun FlameCrownArLens(flutterOffset: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 95.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = "🔥", fontSize = 48.sp, modifier = Modifier.offset(y = flutterOffset.dp))
            Text(text = "🔥", fontSize = 56.sp, modifier = Modifier.offset(y = (-flutterOffset).dp))
            Text(text = "🔥", fontSize = 48.sp, modifier = Modifier.offset(y = flutterOffset.dp))
        }
    }
}

/**
 * 🕶️ Sunglasses & Gold Chain Lens
 */
@Composable
private fun SunglassesGoldChainArLens() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-45).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🕶️", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "🪙 🥇 🪙", fontSize = 38.sp)
        }
    }
}

/**
 * 🌸 Rosy Blush & Freckles Lens
 */
@Composable
private fun RosyBlushFrecklesArLens(pulseScale: Float) {
    BeautySkinAndBlushLayer(smoothingAlpha = 0.25f, blushAlpha = 0.65f, hasSparkles = true)
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "🌸",
            fontSize = 36.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 100.dp, y = (-75).dp)
                .scale(pulseScale)
        )
    }
}
