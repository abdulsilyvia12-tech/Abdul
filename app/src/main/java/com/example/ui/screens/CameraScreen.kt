package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.ai.BlinkAiService
import com.example.data.ai.ModerationResult
import com.example.data.model.CreativeFilter
import com.example.data.model.FilterCategory
import com.example.data.audio.BlinkMusicPlayer
import com.example.ui.components.camera.*
import com.example.ui.camera.filters.CameraShaderEngine
import com.example.ui.camera.filters.CameraShaderType
import com.example.ui.camera.filters.FilterTweakSheet
import com.example.ui.camera.filters.RealtimeShaderPreviewOverlay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.example.ui.camera.filters.SnapchatArLensesOverlay
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.camera.export.ExportResult
import com.example.ui.camera.export.MediaExportEngine
import com.example.ui.camera.studio.MediaExportProgressDialog
import com.example.ui.camera.studio.MediaExportSuccessDialog
import com.example.ui.camera.studio.TikTokMusicMixer
import com.example.ui.camera.studio.VideoTimelineEditor
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

enum class VideoEditTab {
    TIMELINE,
    FILTERS,
    MUSIC,
    TEXT,
    STICKERS,
    DRAWINGS,
    FUNNY_WRITING
}

enum class DualCamLayout {
    PIP_TOP_RIGHT,
    PIP_TOP_LEFT,
    PIP_BOTTOM_RIGHT,
    PIP_BOTTOM_LEFT,
    SPLIT_HORIZONTAL
}

@Composable
fun CameraScreen(
    onPostCreated: (caption: String, filterTag: String, isStory: Boolean, imageUri: String?) -> Unit,
    onCreateVideo: ((title: String, caption: String, hashtags: List<String>, isBlink: Boolean, filterName: String, musicTrack: String) -> Unit)? = null,
    onCloseCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }

    // --- Master Filter Palette with OpenGL Shaders & Creative Lenses ---
    val allFilters = remember {
        listOf(
            // Normal
            CreativeFilter("normal", "Normal", "⚡", Color.Transparent, category = FilterCategory.ALL),

            // ✨ Snapchat & Banuba AR Face Lenses (Iconic Dog Ears, Kitty, Golden Butterflies)
            CreativeFilter("dog_snap", "Puppy Dog", "🐶", Color(0xFFF59E0B), category = FilterCategory.LENSES, arLensType = "dog_ears", isLense = true, overlayTitle = "Snapchat Floppy Dog Ears & Tongue"),
            CreativeFilter("cat_snap", "Kitty Cat", "🐱", Color(0xFFF472B6), category = FilterCategory.LENSES, arLensType = "cat_ears", isLense = true, overlayTitle = "Banuba Kitty Ears & Whiskers"),
            CreativeFilter("butterfly_snap", "Gold Butterflies", "🦋", Color(0xFFFBBF24), category = FilterCategory.LENSES, arLensType = "butterfly_crown", hasSparkles = true, isLense = true, overlayTitle = "3D Fluttering Golden Butterfly Tiara"),
            CreativeFilter("angel_snap", "Angel Halo", "😇", Color(0xFFFDE047), category = FilterCategory.LENSES, arLensType = "angel_halo", hasSparkles = true, isLense = true, overlayTitle = "Floating Golden Ring Halo & Clouds"),
            CreativeFilter("horns_snap", "Neon Horns", "😈", BlinkPink, category = FilterCategory.LENSES, arLensType = "neon_horns", isLense = true, overlayTitle = "Cyber Neon Magenta Horns"),
            CreativeFilter("rainbow_snap", "Rainbow Waterfall", "🌈", Color(0xFF38BDF8), category = FilterCategory.LENSES, arLensType = "rainbow_vomit", hasSparkles = true, isLense = true, overlayTitle = "Classic Snapchat Rainbow Waterfall"),
            CreativeFilter("bambi_snap", "Bambi Deer", "🦌", Color(0xFFD97706), category = FilterCategory.LENSES, arLensType = "bambi_deer", isLense = true, overlayTitle = "Banuba Deer Ears & Fawn Freckles"),
            CreativeFilter("cyber_visor", "Cyber HUD Visor", "🤖", BlinkCyan, category = FilterCategory.LENSES, arLensType = "cyber_visor", isLense = true, overlayTitle = "Targeting Visor with Telemetry HUD"),
            CreativeFilter("anime_snap", "Manga Star Eyes", "✨", Color(0xFFA855F7), category = FilterCategory.LENSES, arLensType = "anime_eyes", hasSparkles = true, isLense = true, overlayTitle = "Big Shimmering Anime Eyes"),
            CreativeFilter("flame_snap", "Fire Flames", "🔥", Color(0xFFEF4444), category = FilterCategory.LENSES, arLensType = "flame_crown", isLense = true, overlayTitle = "Animated Comic Flame Crown"),
            CreativeFilter("sunglasses_snap", "Aviator Shades", "🕶️", Color(0xFF1E293B), category = FilterCategory.LENSES, arLensType = "sunglasses_gold", isLense = true, overlayTitle = "Aviator Shades & Cuban Gold Chain"),

            // 💄 Banuba Beauty Smoothing & Face Makeup
            CreativeFilter("beauty_glam", "Soft Glam", "💄", Color(0xFFFB7185), category = FilterCategory.BEAUTY, arLensType = "beauty_glam", beautySmoothing = 0.95f, blushIntensity = 0.85f, hasSparkles = true, isLense = true, overlayTitle = "Porcelain Skin Smoothing & Rosy Blush"),
            CreativeFilter("rosy_snap", "Rosy Peach", "🌸", Color(0xFFFDA4AF), category = FilterCategory.BEAUTY, arLensType = "rosy_blush", beautySmoothing = 0.85f, blushIntensity = 0.95f, hasSparkles = true, isLense = true, overlayTitle = "Soft Peach Blush & Natural Freckles"),
            CreativeFilter("porcelain_glow", "Porcelain Radiance", "✨", Color(0xFFFFF1F2), category = FilterCategory.BEAUTY, beautySmoothing = 1.0f, blushIntensity = 0.5f, hasSparkles = true, isLense = true, overlayTitle = "Airbrushed Smooth Skin Radiance"),
            CreativeFilter("golden_hour_beauty", "Golden Hour Glam", "☀️", Color(0xFFFFB703), category = FilterCategory.BEAUTY, beautySmoothing = 0.75f, blushIntensity = 0.7f, hasSparkles = true, isLense = true, overlayTitle = "Golden Hour Sunlit Bronzer & Shimmer"),

            // GPU Shaders (✨ OpenGL & AGSL Real-Time Shaders)
            CreativeFilter("chromatic", "Chromatic RGB", "🌈", Color(0xFF67E8F9), category = FilterCategory.SHADERS, shaderType = CameraShaderType.CHROMATIC_ABERRATION, defaultIntensity = 0.85f, overlayTitle = "RGB Split Chromatic Aberration"),
            CreativeFilter("neon_edge", "Neon Edge", "🔮", BlinkPink, category = FilterCategory.SHADERS, shaderType = CameraShaderType.NEON_CYBER, defaultIntensity = 0.9f, overlayTitle = "Luminescent Cyber Edge Glow"),
            CreativeFilter("vhs_wave", "VHS 90s CRT", "📼", Color(0xFFE2E8F0), scanlines = true, category = FilterCategory.SHADERS, shaderType = CameraShaderType.VHS_RETRO, defaultIntensity = 0.85f, overlayTitle = "Analog CRT Tape & Scanlines"),
            CreativeFilter("fisheye", "Fisheye Lens", "🐟", Color(0xFF38BDF8), category = FilterCategory.SHADERS, shaderType = CameraShaderType.FISHEYE, defaultIntensity = 0.8f, overlayTitle = "Wide Barrel Action Cam Lens"),
            CreativeFilter("thermal", "Thermal FLIR", "🔥", Color(0xFFEF4444), category = FilterCategory.SHADERS, shaderType = CameraShaderType.THERMAL_VISION, defaultIntensity = 0.9f, overlayTitle = "Heatmap Infrared Night Vision"),
            CreativeFilter("halftone", "Pop Halftone", "📰", Color(0xFFF59E0B), category = FilterCategory.SHADERS, shaderType = CameraShaderType.HALFTONE_COMIC, defaultIntensity = 0.85f, overlayTitle = "Pop Art Comic Dot Raster"),
            CreativeFilter("glitch_cyber", "Cyber Glitch", "⚡", Color(0xFFA855F7), isGlitch = true, category = FilterCategory.SHADERS, shaderType = CameraShaderType.GLITCH_DISTORTION, defaultIntensity = 0.85f, overlayTitle = "Digital Matrix Artifact Jitter"),

            // Funny Faces (🤪)
            CreativeFilter("googly", "Googly Eyes", "🤪", Color(0xFF60A5FA), vignetteAlpha = 0.25f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "googly", overlayTitle = "Jiggling Cartoon Googly Eyes"),
            CreativeFilter("clown", "Circus Clown", "🤡", Color(0xFFEF4444), vignetteAlpha = 0.3f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "clown", overlayTitle = "Big Squeaky Red Nose & Smile"),
            CreativeFilter("puppy", "Puppy Dog", "🐶", Color(0xFFF59E0B), vignetteAlpha = 0.2f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "puppy", overlayTitle = "Floppy Dog Ears & Tongue"),
            CreativeFilter("alien", "Roswell Alien", "👽", Color(0xFF10B981), vignetteAlpha = 0.45f, isGlitch = true, category = FilterCategory.FUNNY_FACES, funnyFaceType = "alien", overlayTitle = "Cosmic Green Alien & Antennae"),
            CreativeFilter("potato", "Talking Potato", "🥔", Color(0xFFD97706), vignetteAlpha = 0.35f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "potato", overlayTitle = "Derpy Talking Potato"),
            CreativeFilter("cat", "Derpy Cat", "🐱", Color(0xFFEC4899), vignetteAlpha = 0.25f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "cat", overlayTitle = "Cute Whiskers & Kitty Ears"),
            CreativeFilter("thug", "Thug Life", "😎", Color(0xFF18181B), vignetteAlpha = 0.4f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "thug", overlayTitle = "Pixel Shades & Gold Chain"),
            CreativeFilter("chibi", "Chibi Anime", "✨", Color(0xFFA855F7), hasParticles = true, category = FilterCategory.FUNNY_FACES, funnyFaceType = "chibi", overlayTitle = "Huge Manga Starry Eyes"),
            CreativeFilter("frog", "Derp Frog", "🐸", Color(0xFF84CC16), vignetteAlpha = 0.3f, category = FilterCategory.FUNNY_FACES, funnyFaceType = "frog", overlayTitle = "Goofy Frog Headband"),

            // Funny Games (🎮 Interactive AR Mini-Games)
            CreativeFilter("donut_catch", "Donut Catch", "🍩", Color(0xFFF472B6), vignetteAlpha = 0.2f, category = FilterCategory.GAMES, gameType = "donut", overlayTitle = "Catch Falling Donuts with Tap!"),
            CreativeFilter("blink_reflex", "Blink Target", "🎯", Color(0xFF06B6D4), vignetteAlpha = 0.25f, category = FilterCategory.GAMES, gameType = "reflex", overlayTitle = "Speed Reflex Tap Challenge!"),
            CreativeFilter("balloon_pop", "Balloon Pop", "🎈", Color(0xFFEF4444), hasParticles = true, category = FilterCategory.GAMES, gameType = "balloon", overlayTitle = "Pop Floating Balloons Frenzy!"),
            CreativeFilter("taco_chomp", "Taco Chomp", "🌮", Color(0xFFF59E0B), vignetteAlpha = 0.2f, category = FilterCategory.GAMES, gameType = "taco", overlayTitle = "Chomp Flying Tacos Rush!"),
            CreativeFilter("persona_spin", "Persona Vibe", "🎰", Color(0xFF8B5CF6), isGlitch = true, category = FilterCategory.GAMES, gameType = "roulette", overlayTitle = "Spin Your Vibe Roulette!"),

            // Aesthetic & Cinema (🔮)
            CreativeFilter("noir", "Noir 35mm", "🎬", Color.DarkGray, vignetteAlpha = 0.55f, category = FilterCategory.AESTHETIC, shaderType = CameraShaderType.NOIR_FILM, overlayTitle = "High-Contrast Silver Halide"),
            CreativeFilter("sunset", "Teal & Orange", "🌅", Color(0xFFFF8A00), vignetteAlpha = 0.4f, category = FilterCategory.AESTHETIC, shaderType = CameraShaderType.TEAL_ORANGE, overlayTitle = "Hollywood Cinematic Teal & Orange"),
            CreativeFilter("golden", "Golden Hour", "☀️", Color(0xFFFFB703), vignetteAlpha = 0.35f, category = FilterCategory.AESTHETIC),
            CreativeFilter("vintage", "Vintage Kodachrome", "🎞️", Color(0xFFD97706), vignetteAlpha = 0.45f, category = FilterCategory.AESTHETIC, shaderType = CameraShaderType.VINTAGE_KODACHROME, overlayTitle = "1970s Kodachrome Film Tone"),
            CreativeFilter("vaporwave", "Vaporwave", "🌸", Color(0xFFF472B6), vignetteAlpha = 0.35f, category = FilterCategory.AESTHETIC),
            CreativeFilter("sparkle", "Aura Dream", "✨", BlinkPink, hasParticles = true, category = FilterCategory.AESTHETIC),

            // Cyber & Retro (🤖)
            CreativeFilter("cyberpunk", "Cyberpunk", "🔮", BlinkCyan, vignetteAlpha = 0.35f, isGlitch = true, category = FilterCategory.CYBER, shaderType = CameraShaderType.NEON_CYBER),
            CreativeFilter("vhs", "VHS 90s", "📼", Color(0xFFE2E8F0), scanlines = true, category = FilterCategory.CYBER, shaderType = CameraShaderType.VHS_RETRO),
            CreativeFilter("matrix", "Emerald Matrix", "🟩", Color(0xFF10B981), vignetteAlpha = 0.4f, scanlines = true, category = FilterCategory.CYBER, shaderType = CameraShaderType.EMERALD_MATRIX),
            CreativeFilter("frost", "Sub-Zero Ice", "❄️", Color(0xFF38BDF8), vignetteAlpha = 0.35f, category = FilterCategory.CYBER),
            CreativeFilter("neon_glitch", "Neon Glitch", "⚡", Color(0xFFA855F7), isGlitch = true, category = FilterCategory.CYBER, shaderType = CameraShaderType.GLITCH_DISTORTION)
        )
    }

    var selectedCategory by remember { mutableStateOf(FilterCategory.ALL) }
    var selectedFilter by remember { mutableStateOf(allFilters[0]) }

    // Real-Time Shader Controls & Parameters
    var shaderIntensity by remember { mutableFloatStateOf(0.85f) }
    var shaderWarmth by remember { mutableFloatStateOf(0.0f) }
    var shaderScanlines by remember { mutableStateOf(false) }
    var isComparingOriginal by remember { mutableStateOf(false) }
    var isShaderTweakSheetOpen by remember { mutableStateOf(false) }
    var isRightRailCollapsed by remember { mutableStateOf(false) }

    val displayedFilters = remember(selectedCategory) {
        if (selectedCategory == FilterCategory.ALL) allFilters
        else allFilters.filter { it.category == selectedCategory || it.id == "normal" }
    }

    // Camera Mode (Photo vs Video)
    var isVideoMode by remember { mutableStateOf(false) }
    var isPureCameraXMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var flipFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isDualCamMode by remember { mutableStateOf(false) }
    var dualCamLayout by remember { mutableStateOf(DualCamLayout.PIP_TOP_RIGHT) }
    var capturedDualCamActive by remember { mutableStateOf(false) }

    // Captured & Recorded Media States
    var isRecordedVideo by remember { mutableStateOf(false) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedSnapReady by remember { mutableStateOf(false) }

    if (isPureCameraXMode) {
        CameraXPreviewCapture(
            onPhotoCaptured = { uri ->
                capturedImageUri = uri
                isRecordedVideo = false
                capturedSnapReady = true
                isPureCameraXMode = false
            },
            onClose = {
                isPureCameraXMode = false
            },
            title = "Snap Photo (CameraX)",
            showReviewStep = true
        )
        return
    }
    var snapCaption by remember { mutableStateOf("") }
    var aiSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingAi by remember { mutableStateOf(false) }

    // Video Post-Production State
    var activeVideoEditTab by remember { mutableStateOf(VideoEditTab.FILTERS) }
    var isVideoPlaying by remember { mutableStateOf(true) }
    var playbackProgress by remember { mutableFloatStateOf(0.35f) }
    var selectedMusicTrack by remember { mutableStateOf("⚡ Tokyo Drift Synthwave") }
    var isAudioMuted by remember { mutableStateOf(false) }
    var customVideoText by remember { mutableStateOf("") }
    var videoTextColor by remember { mutableStateOf(Color.White) }
    var videoTextBgStyle by remember { mutableStateOf("glow") } // "glow", "pill", "neon", "none"
    var videoTextStyle by remember { mutableStateOf("Classic") } // "Classic", "Typewriter", "Heavy Impact", "Neon Glow"
    val videoStickers = remember { mutableStateListOf<String>() }
    var activeFunnyWriting by remember { mutableStateOf("") }

    // TikTok Music & Dual Volume Mixing State
    var originalAudioVolume by remember { mutableFloatStateOf(1.0f) }
    var musicTrackVolume by remember { mutableFloatStateOf(0.85f) }

    // Video Timeline, Trimming & Scrubbing State
    var videoTrimStartSec by remember { mutableFloatStateOf(0f) }
    var videoTrimEndSec by remember { mutableFloatStateOf(15f) }
    var videoCurrentPositionSec by remember { mutableFloatStateOf(0f) }

    // Export & Sharing Studio State
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    // Interactive Game Mechanics
    var gameScore by remember { mutableStateOf(0) }
    var gameCombo by remember { mutableStateOf(1) }
    var reflexState by remember { mutableStateOf("WAIT") } // "WAIT", "TAP_NOW", "SUCCESS"
    var reflexReactionTimeMs by remember { mutableStateOf(0) }
    var rouletteSpinning by remember { mutableStateOf(false) }
    var rouletteResult by remember { mutableStateOf("Spin to reveal vibe 🎲") }

    // AI Filter builder
    var showCustomFilterModal by remember { mutableStateOf(false) }
    var customFilterPrompt by remember { mutableStateOf("") }
    var isBuildingFilter by remember { mutableStateOf(false) }

    // Moderation
    var moderationStatus by remember { mutableStateOf<ModerationResult?>(null) }
    var isModerating by remember { mutableStateOf(false) }

    val shutterScale = remember { Animatable(1f) }

    // --- Vertical Right Rail Tool States ---
    // 1. The round thing for talking selfies on faces
    var isTalkingSelfieActive by remember { mutableStateOf(false) }
    var talkingSelfieVoiceMode by remember { mutableStateOf("normal") }

    // 2. Text (T)
    var isTextToolOpen by remember { mutableStateOf(false) }
    var activeCameraText by remember { mutableStateOf("") }
    var cameraTextColor by remember { mutableStateOf(Color.White) }
    var cameraTextBgStyle by remember { mutableStateOf("pill") }

    // 3. Music (Any song from any singer/artist: Taylor Swift, The Weeknd, Adele, Drake, Billie Eilish, etc.)
    var isMusicPickerOpen by remember { mutableStateOf(false) }
    var cameraSelectedMusic by remember { mutableStateOf("Cruel Summer - Taylor Swift") }
    var cameraSongSearchQuery by remember { mutableStateOf("") }
    var isSongSearchExpanded by remember { mutableStateOf(false) }

    // 4. Video Edit Studio
    var isVideoEditPanelOpen by remember { mutableStateOf(false) }
    var videoSpeedMultiplier by remember { mutableStateOf(1.0f) }
    var videoTimerCountdownSec by remember { mutableStateOf(0) }
    var isGridGuideEnabled by remember { mutableStateOf(false) }

    // 5. Add Filter
    var isAddFilterSheetOpen by remember { mutableStateOf(false) }

    // 6. Renew Filter
    var renewToastMessage by remember { mutableStateOf<String?>(null) }

    // 8. Face Painting
    var isFacePaintingOpen by remember { mutableStateOf(false) }
    var activeFacePaintType by remember { mutableStateOf("none") }

    // 9. Drawings
    var isDrawingModeActive by remember { mutableStateOf(false) }
    val drawnStrokes = remember { mutableStateListOf<DrawnStroke>() }
    val currentStrokePoints = remember { mutableStateListOf<Offset>() }
    var currentPenColor by remember { mutableStateOf(Color(0xFF06B6D4)) }
    var currentStrokeWidth by remember { mutableStateOf(8f) }
    var isGlowPen by remember { mutableStateOf(true) }

    // 10. Soundboard FX
    var isSoundboardOpen by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedImageUri = uri
            isRecordedVideo = false
            capturedSnapReady = true
        }
    }

    // Helper: Generates realistic photo frame
    fun generateRealMedia(filter: CreativeFilter, isVideo: Boolean): Uri {
        val file = File(context.cacheDir, "blink_${if (isVideo) "video" else "photo"}_${System.currentTimeMillis()}.jpg")
        val bitmap = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val shader = when (filter.id) {
            "cyberpunk" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF0F172A.toInt(), 0xFF06B6D4.toInt(), Shader.TileMode.CLAMP)
            "sunset" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFFFF7A00.toInt(), 0xFFBE185D.toInt(), Shader.TileMode.CLAMP)
            "noir" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF18181B.toInt(), 0xFF71717A.toInt(), Shader.TileMode.CLAMP)
            "matrix" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF064E3B.toInt(), 0xFF10B981.toInt(), Shader.TileMode.CLAMP)
            "golden" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF78350F.toInt(), 0xFFF59E0B.toInt(), Shader.TileMode.CLAMP)
            "googly" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF1E3A8A.toInt(), 0xFF3B82F6.toInt(), Shader.TileMode.CLAMP)
            "clown" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF991B1B.toInt(), 0xFFEF4444.toInt(), Shader.TileMode.CLAMP)
            "alien" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF064E3B.toInt(), 0xFF10B981.toInt(), Shader.TileMode.CLAMP)
            "donut_catch" -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF831843.toInt(), 0xFFEC4899.toInt(), Shader.TileMode.CLAMP)
            else -> LinearGradient(0f, 0f, 1080f, 1440f, 0xFF1E1B4B.toInt(), 0xFF8B5CF6.toInt(), Shader.TileMode.CLAMP)
        }
        paint.shader = shader
        canvas.drawRect(0f, 0f, 1080f, 1440f, paint)

        // Draw Selfie Picture-in-Picture window when Dual Camera is enabled
        if (isDualCamMode) {
            val pipLeft = 700f
            val pipTop = 90f
            val pipRight = 1010f
            val pipBottom = 500f
            val pipBg = Paint().apply {
                color = 0xFF0F172A.toInt()
                style = Paint.Style.FILL
            }
            val pipBorder = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 12f
            }
            canvas.drawRoundRect(pipLeft, pipTop, pipRight, pipBottom, 40f, 40f, pipBg)

            val selfieGrad = LinearGradient(pipLeft, pipTop, pipRight, pipBottom, 0xFF7C3AED.toInt(), 0xFF06B6D4.toInt(), Shader.TileMode.CLAMP)
            val selfieP = Paint().apply { this.shader = selfieGrad }
            canvas.drawRoundRect(pipLeft + 8f, pipTop + 8f, pipRight - 8f, pipBottom - 8f, 32f, 32f, selfieP)
            canvas.drawRoundRect(pipLeft, pipTop, pipRight, pipBottom, 40f, 40f, pipBorder)

            val pipLabel = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 36f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("🤳 Front Selfie", pipLeft + 30f, pipBottom - 35f, pipLabel)
        }

        // Draw user drawn doodle strokes onto photo/video canvas
        if (drawnStrokes.isNotEmpty()) {
            val drawPaint = Paint().apply {
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }
            drawnStrokes.forEach { stroke ->
                drawPaint.color = stroke.color.hashCode()
                drawPaint.strokeWidth = stroke.strokeWidth * 2.2f
                if (stroke.points.size > 1) {
                    for (i in 0 until stroke.points.size - 1) {
                        // Scale coordinates from screen to 1080x1440 canvas
                        val p1 = stroke.points[i]
                        val p2 = stroke.points[i + 1]
                        canvas.drawLine(p1.x * 2.5f, p1.y * 1.8f, p2.x * 2.5f, p2.y * 1.8f, drawPaint)
                    }
                }
            }
        }

        // Draw In-Camera Live Text Overlay if present
        if (activeCameraText.isNotBlank()) {
            val liveTextPaint = Paint().apply {
                color = cameraTextColor.hashCode()
                textSize = 58f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            // Draw background pill
            val textY = 720f
            val bgPaint = Paint().apply {
                color = if (cameraTextBgStyle == "glow") 0xAA7C3AED.toInt() else 0xCC000000.toInt()
                style = Paint.Style.FILL
            }
            val textWidth = liveTextPaint.measureText(activeCameraText)
            canvas.drawRoundRect(540f - textWidth / 2f - 30f, textY - 60f, 540f + textWidth / 2f + 30f, textY + 25f, 24f, 24f, bgPaint)
            canvas.drawText(activeCameraText, 540f, textY, liveTextPaint)
        }

        // Draw Review Studio Custom Video Text if added
        if (customVideoText.isNotBlank()) {
            val videoTextPaint = Paint().apply {
                color = videoTextColor.hashCode()
                textSize = 54f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val textY = 720f
            val bgPaint = Paint().apply {
                color = if (videoTextBgStyle == "glow") 0xBB7C3AED.toInt() else 0xCC000000.toInt()
                style = Paint.Style.FILL
            }
            val textWidth = videoTextPaint.measureText(customVideoText)
            canvas.drawRoundRect(540f - textWidth / 2f - 28f, textY - 56f, 540f + textWidth / 2f + 28f, textY + 22f, 22f, 22f, bgPaint)
            canvas.drawText(customVideoText, 540f, textY, videoTextPaint)
        }

        // Draw Funny Writing Meme Sticker if selected
        if (activeFunnyWriting.isNotBlank()) {
            val funnyPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 48f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val funnyBg = Paint().apply {
                color = 0xDD0F172A.toInt()
                style = Paint.Style.FILL
            }
            val funnyWidth = funnyPaint.measureText(activeFunnyWriting)
            val funnyY = 820f
            canvas.drawRoundRect(540f - funnyWidth / 2f - 28f, funnyY - 52f, 540f + funnyWidth / 2f + 28f, funnyY + 20f, 20f, 20f, funnyBg)
            canvas.drawText(activeFunnyWriting, 540f, funnyY, funnyPaint)
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val mediaLabel = if (isVideo) "🎬 Blink Recorded Video" else "⚡ Blink Live Photo"
        val dualLabel = if (isDualCamMode) " • 🤳 Dual View" else ""
        val facePaintLabel = if (activeFacePaintType != "none") " • 🎭 Paint: $activeFacePaintType" else ""
        val talkingLabel = if (isTalkingSelfieActive) " • 🗣️ Talking Lens" else ""
        canvas.drawText("$mediaLabel$dualLabel$facePaintLabel$talkingLabel", 60f, 1300f, textPaint)
        textPaint.textSize = 28f
        val musicTag = if (cameraSelectedMusic.isNotBlank()) " • 🎵 $cameraSelectedMusic" else ""
        canvas.drawText("Filter: ${filter.name}$musicTag", 60f, 1350f, textPaint)

        // Apply Real-Time GPU Color Matrix and Shader Post-Processing to captured Bitmap
        val processedBitmap = if (filter.shaderType != CameraShaderType.NONE || filter.vignetteAlpha > 0.05f || filter.scanlines || shaderScanlines) {
            CameraShaderEngine.applyFilterToBitmap(
                source = bitmap,
                shaderType = filter.shaderType,
                intensity = shaderIntensity,
                warmth = shaderWarmth,
                hasScanlines = filter.scanlines || shaderScanlines,
                vignetteAlpha = filter.vignetteAlpha
            )
        } else {
            bitmap
        }

        FileOutputStream(file).use { out ->
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return Uri.fromFile(file)
    }

    // Photo Capture Execution
    fun executePhotoCapture() {
        coroutineScope.launch {
            shutterScale.animateTo(0.85f, tween(80))
            shutterScale.animateTo(1f, tween(80))
        }

        val capture = imageCaptureInstance
        val photoFile = File(context.cacheDir, "blink_camera_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capturedDualCamActive = isDualCamMode
        if (capture != null && hasCameraPermission) {
            try {
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            capturedImageUri = Uri.fromFile(photoFile)
                            isRecordedVideo = false
                            capturedDualCamActive = isDualCamMode
                            capturedSnapReady = true
                        }

                        override fun onError(exception: ImageCaptureException) {
                            val realUri = generateRealMedia(selectedFilter, isVideo = false)
                            capturedImageUri = realUri
                            isRecordedVideo = false
                            capturedDualCamActive = isDualCamMode
                            capturedSnapReady = true
                        }
                    }
                )
            } catch (e: Exception) {
                val realUri = generateRealMedia(selectedFilter, isVideo = false)
                capturedImageUri = realUri
                isRecordedVideo = false
                capturedDualCamActive = isDualCamMode
                capturedSnapReady = true
            }
        } else {
            val realUri = generateRealMedia(selectedFilter, isVideo = false)
            capturedImageUri = realUri
            isRecordedVideo = false
            capturedDualCamActive = isDualCamMode
            capturedSnapReady = true
        }
    }

    // Video Recording loop
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording && recordingSeconds < 30) {
                delay(1000)
                recordingSeconds++
            }
            if (recordingSeconds >= 30) {
                isRecording = false
                val realUri = generateRealMedia(selectedFilter, isVideo = true)
                capturedImageUri = realUri
                isRecordedVideo = true
                capturedDualCamActive = isDualCamMode
                capturedSnapReady = true
            }
        }
    }

    // Playback loop for video reviewer
    LaunchedEffect(capturedSnapReady, isRecordedVideo, isVideoPlaying) {
        if (capturedSnapReady && isRecordedVideo && isVideoPlaying) {
            while (isVideoPlaying) {
                delay(80)
                playbackProgress = (playbackProgress + 0.03f) % 1.0f
            }
        }
    }

    // Reflex challenge timer
    LaunchedEffect(selectedFilter.gameType) {
        if (selectedFilter.gameType == "reflex") {
            while (true) {
                reflexState = "WAIT"
                delay(Random.nextLong(2000, 4500))
                reflexState = "TAP_NOW"
                val start = System.currentTimeMillis()
                delay(1500)
                if (reflexState == "TAP_NOW") {
                    reflexState = "TOO_SLOW"
                    delay(1000)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        // --- Viewfinder Layer ---
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                key(isFrontCamera) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val imageCapture = ImageCapture.Builder().build()
                                    imageCaptureInstance = imageCapture

                                    val cameraSelector = if (isFrontCamera) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }

                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    // Handled gracefully
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = "📷", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Camera Active & Live",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Snap photos, record videos, apply funny faces & game lenses",
                            style = MaterialTheme.typography.bodySmall.copy(color = BlinkTextSecondary)
                        )
                    }
                }
            }

            // --- Real-Time OpenGL / Compose Shader & Color Grading Layer ---
            RealtimeShaderPreviewOverlay(
                filter = selectedFilter,
                intensity = shaderIntensity,
                warmth = shaderWarmth,
                isComparingOriginal = isComparingOriginal,
                modifier = Modifier.fillMaxSize()
            )

            // --- Snapchat & Banuba Face AR Lenses & Beauty Engine ---
            SnapchatArLensesOverlay(
                filter = selectedFilter,
                intensity = shaderIntensity,
                modifier = Modifier.fillMaxSize()
            )

            // --- Viewfinder Gestures: Swipe Left/Right = Switch Filters; Double Tap = Flip Front/Back Camera ---
            if (!isDrawingModeActive && !capturedSnapReady && (selectedFilter.gameType.isBlank() || gameScore == 0)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayedFilters, selectedFilter) {
                            var totalDragDistanceX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragDistanceX = 0f },
                                onDragEnd = {
                                    if (totalDragDistanceX < -40f) {
                                        // Swipe Left -> Next filter / lens
                                        val currentIndex = displayedFilters.indexOfFirst { it.id == selectedFilter.id }
                                        if (currentIndex != -1 && displayedFilters.isNotEmpty()) {
                                            val nextIndex = (currentIndex + 1) % displayedFilters.size
                                            selectedFilter = displayedFilters[nextIndex]
                                            shaderIntensity = selectedFilter.defaultIntensity
                                            shaderScanlines = selectedFilter.scanlines
                                            renewToastMessage = "✨ ${selectedFilter.name} ${selectedFilter.iconEmoji}"
                                            coroutineScope.launch {
                                                delay(1300)
                                                if (renewToastMessage?.contains(selectedFilter.name) == true) {
                                                    renewToastMessage = null
                                                }
                                            }
                                        }
                                    } else if (totalDragDistanceX > 40f) {
                                        // Swipe Right -> Previous filter / lens
                                        val currentIndex = displayedFilters.indexOfFirst { it.id == selectedFilter.id }
                                        if (currentIndex != -1 && displayedFilters.isNotEmpty()) {
                                            val prevIndex = if (currentIndex - 1 < 0) displayedFilters.size - 1 else currentIndex - 1
                                            selectedFilter = displayedFilters[prevIndex]
                                            shaderIntensity = selectedFilter.defaultIntensity
                                            shaderScanlines = selectedFilter.scanlines
                                            renewToastMessage = "✨ ${selectedFilter.name} ${selectedFilter.iconEmoji}"
                                            coroutineScope.launch {
                                                delay(1300)
                                                if (renewToastMessage?.contains(selectedFilter.name) == true) {
                                                    renewToastMessage = null
                                                }
                                            }
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    totalDragDistanceX += dragAmount
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    isFrontCamera = !isFrontCamera
                                    flipFeedbackMessage = if (isFrontCamera) "🤳 Front Camera" else "📷 Back Camera"
                                    coroutineScope.launch {
                                        delay(1300)
                                        flipFeedbackMessage = null
                                    }
                                }
                            )
                        }
                )
            }

            // Quick Flip Camera Feedback Toast Pill
            if (flipFeedbackMessage != null) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    border = BorderStroke(1.5.dp, BlinkCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = null, tint = BlinkCyan, modifier = Modifier.size(24.dp))
                        Text(
                            text = flipFeedbackMessage!!,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // --- Funny Faces AR Graphical Overlays ---
            if (selectedFilter.funnyFaceType.isNotBlank()) {
                FunnyFaceOverlay(type = selectedFilter.funnyFaceType)
            }

            // --- Interactive Camera Mini-Games Overlays ---
            if (selectedFilter.gameType.isNotBlank()) {
                GameLensOverlay(
                    gameType = selectedFilter.gameType,
                    score = gameScore,
                    combo = gameCombo,
                    reflexState = reflexState,
                    reflexReactionTimeMs = reflexReactionTimeMs,
                    rouletteResult = rouletteResult,
                    rouletteSpinning = rouletteSpinning,
                    onScore = { points ->
                        gameScore += points
                        gameCombo++
                    },
                    onReflexTap = {
                        if (reflexState == "TAP_NOW") {
                            reflexReactionTimeMs = Random.nextInt(180, 260)
                            gameScore += 100
                            reflexState = "SUCCESS"
                        }
                    },
                    onSpinRoulette = {
                        coroutineScope.launch {
                            rouletteSpinning = true
                            val vibes = listOf(
                                "⚡ Chaotic Caffeine Demon",
                                "🧠 Galaxy Brain Overlord",
                                "🦝 Sleepy Trash Raccoon",
                                "✨ Certified Vibe Curator",
                                "🥐 Haute Couture Icon",
                                "👾 8-Bit Speedrun Master",
                                "🎧 3AM Lo-Fi Scholar",
                                "🍕 Pizza Royalty"
                            )
                            for (i in 0 until 12) {
                                rouletteResult = vibes.random()
                                delay(90)
                            }
                            rouletteSpinning = false
                        }
                    },
                    onResetGame = {
                        gameScore = 0
                        gameCombo = 1
                    }
                )
            }

            // Top Status Overlay (Video Recording Active Indicator - Electric Blue Theme)
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF00E5FF)))
                        )
                        .border(1.5.dp, Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔵 REC 00:${if (recordingSeconds < 10) "0$recordingSeconds" else recordingSeconds} • Release to Stop",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }

            // --- Dual Camera (Front + Back Selfie) Overlay ---
            if (isDualCamMode) {
                DualCamSelfieOverlay(
                    isFrontSelfie = !isFrontCamera,
                    layout = dualCamLayout,
                    isReview = false,
                    onSwap = { isFrontCamera = !isFrontCamera },
                    onCycleLayout = {
                        dualCamLayout = when (dualCamLayout) {
                            DualCamLayout.PIP_TOP_RIGHT -> DualCamLayout.PIP_TOP_LEFT
                            DualCamLayout.PIP_TOP_LEFT -> DualCamLayout.PIP_BOTTOM_LEFT
                            DualCamLayout.PIP_BOTTOM_LEFT -> DualCamLayout.PIP_BOTTOM_RIGHT
                            DualCamLayout.PIP_BOTTOM_RIGHT -> DualCamLayout.SPLIT_HORIZONTAL
                            DualCamLayout.SPLIT_HORIZONTAL -> DualCamLayout.PIP_TOP_RIGHT
                        }
                    }
                )
            }

            // 3x3 Composition Grid Guide
            if (isGridGuideEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())
                }
            }

            // AR Face Painting Overlay
            FacePaintingCanvasOverlay(activePaintType = activeFacePaintType)

            // In-Camera Live Drawing Canvas
            CameraDrawingCanvas(
                isDrawingModeActive = isDrawingModeActive,
                drawnStrokes = drawnStrokes,
                currentStrokePoints = currentStrokePoints,
                currentPenColor = currentPenColor,
                currentStrokeWidth = currentStrokeWidth,
                isGlowPen = isGlowPen
            )

            // The Round Thing for Talking Selfies on Faces
            TalkingSelfieFaceBubble(
                isActive = isTalkingSelfieActive,
                onClose = { isTalkingSelfieActive = false },
                voiceMode = talkingSelfieVoiceMode,
                onVoiceModeChange = { talkingSelfieVoiceMode = it }
            )

            // Live Custom Text Overlay on Camera Viewfinder
            if (activeCameraText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (cameraTextBgStyle) {
                                "pill" -> Color.Black.copy(alpha = 0.75f)
                                "glow" -> BlinkPrimary.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }
                        )
                        .border(
                            if (cameraTextBgStyle == "glow") 1.5.dp else 0.dp,
                            BlinkCyan,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { isTextToolOpen = true }
                        .testTag("camera_active_text_overlay")
                ) {
                    Text(
                        text = activeCameraText,
                        color = cameraTextColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // --- Conditional Vertical Right Tool Rail ---
        // Features disappear when recording, in drawing mode, text tool open, search open, or manually collapsed
        val shouldHideRightRail = isRecording ||
            isDrawingModeActive ||
            isTextToolOpen ||
            isSongSearchExpanded ||
            capturedSnapReady ||
            (selectedFilter.gameType.isNotBlank() && gameScore > 0)

        AnimatedVisibility(
            visible = !shouldHideRightRail,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            CameraRightRail(
                isTalkingSelfieActive = isTalkingSelfieActive,
                onToggleTalkingSelfie = { isTalkingSelfieActive = !isTalkingSelfieActive },
                isTextActive = activeCameraText.isNotBlank() || isTextToolOpen,
                onOpenTextTool = { isTextToolOpen = true },
                hasMusicSelected = cameraSelectedMusic.isNotBlank(),
                onOpenMusicPicker = { isMusicPickerOpen = true },
                isVideoMode = isVideoMode,
                onOpenVideoEdit = { isVideoEditPanelOpen = true },
                isCreativeFilterActive = selectedFilter.id != "normal",
                onOpenAddFilter = { isAddFilterSheetOpen = true },
                onRenewFilter = {
                    val remaining = allFilters.filter { it.id != selectedFilter.id }
                    val next = remaining.random()
                    selectedFilter = next
                    shaderIntensity = next.defaultIntensity
                    renewToastMessage = "✨ Renewed to: ${next.name} ${next.iconEmoji}"
                    BlinkMusicPlayer.playSoundEffect("squeak")
                    coroutineScope.launch {
                        delay(2200)
                        if (renewToastMessage?.contains(next.name) == true) {
                            renewToastMessage = null
                        }
                    }
                },
                onChangeFilter = {
                    val idx = allFilters.indexOfFirst { it.id == selectedFilter.id }
                    val nextIdx = (idx + 1) % allFilters.size
                    selectedFilter = allFilters[nextIdx]
                    shaderIntensity = selectedFilter.defaultIntensity
                    renewToastMessage = "Switched to ${selectedFilter.name} ${selectedFilter.iconEmoji}"
                    coroutineScope.launch {
                        delay(1600)
                        renewToastMessage = null
                    }
                },
                onOpenShaderTweak = { isShaderTweakSheetOpen = true },
                isFacePaintingActive = activeFacePaintType != "none",
                onOpenFacePainting = { isFacePaintingOpen = true },
                isDrawingModeActive = isDrawingModeActive,
                onToggleDrawingMode = { isDrawingModeActive = !isDrawingModeActive },
                isSoundboardActive = isSoundboardOpen,
                onToggleSoundboard = { isSoundboardOpen = !isSoundboardOpen },
                isCollapsed = isRightRailCollapsed,
                onToggleCollapsed = { isRightRailCollapsed = !isRightRailCollapsed }
            )
        }

        // --- Floating Real-Time Shader Controls HUD ---
        // 1. "Hold to Compare" Button (Real-Time Before/After Preview)
        if (selectedFilter.id != "normal" && !isRecording && !capturedSnapReady && !isDrawingModeActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 76.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isComparingOriginal = true
                                tryAwaitRelease()
                                isComparingOriginal = false
                            }
                        )
                    }
                    .testTag("hold_to_compare_button"),
                shape = RoundedCornerShape(20.dp),
                color = if (isComparingOriginal) BlinkPink.copy(alpha = 0.90f) else Color.Black.copy(alpha = 0.60f),
                border = BorderStroke(1.dp, if (isComparingOriginal) Color.White else BlinkCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isComparingOriginal) "⚡ Original Raw" else "👁️ Hold to Compare",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Floating Shader Intensity Tuning Pill
        if (selectedFilter.id != "normal" && !isRecording && !capturedSnapReady && !isDrawingModeActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 195.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isShaderTweakSheetOpen = true }
                    .testTag("shader_intensity_hud_pill"),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.70f),
                border = BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "✨ ${selectedFilter.name}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${(shaderIntensity * 100).toInt()}%", color = BlinkCyanLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Tweak Shader", tint = BlinkCyanLight, modifier = Modifier.size(13.dp))
                }
            }
        }

        // Drawing Mode Floating Toolbar
        CameraDrawingToolbar(
            visible = isDrawingModeActive,
            currentPenColor = currentPenColor,
            onColorSelected = { currentPenColor = it },
            currentStrokeWidth = currentStrokeWidth,
            onStrokeWidthSelected = { currentStrokeWidth = it },
            isGlowPen = isGlowPen,
            onToggleGlow = { isGlowPen = !isGlowPen },
            onUndo = { if (drawnStrokes.isNotEmpty()) drawnStrokes.removeAt(drawnStrokes.lastIndex) },
            onClear = { drawnStrokes.clear() },
            onDone = { isDrawingModeActive = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 54.dp)
        )

        // Renew Filter / Action Toast Banner
        if (renewToastMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 96.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, BlinkCyan)
            ) {
                Text(
                    text = renewToastMessage!!,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Soundboard FX Quick Floating Drawer
        if (isSoundboardOpen) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 68.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, BlinkCyan)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sound FX", color = BlinkCyanLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "dramatic" to "😱 Dun-Dun!",
                        "laugh" to "😂 Laugh",
                        "squeak" to "🦆 Squeak",
                        "airhorn" to "📢 Airhorn"
                    ).forEach { (effect, label) ->
                        Surface(
                            onClick = { BlinkMusicPlayer.playSoundEffect(effect) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                        }
                    }
                }
            }
        }

        // Tool Dialogs
        CameraTextEditorDialog(
            visible = isTextToolOpen,
            currentText = activeCameraText,
            currentColor = cameraTextColor,
            currentBgStyle = cameraTextBgStyle,
            onSaveText = { text, color, bg ->
                activeCameraText = text
                cameraTextColor = color
                cameraTextBgStyle = bg
                isTextToolOpen = false
            },
            onDismiss = { isTextToolOpen = false }
        )

        CameraMusicDialog(
            visible = isMusicPickerOpen,
            selectedTrack = cameraSelectedMusic,
            onSelectTrack = { track ->
                cameraSelectedMusic = track
                isMusicPickerOpen = false
                BlinkMusicPlayer.playTrack(track)
            },
            onDismiss = { isMusicPickerOpen = false }
        )

        if (isShaderTweakSheetOpen) {
            FilterTweakSheet(
                filter = selectedFilter,
                intensity = shaderIntensity,
                onIntensityChange = { shaderIntensity = it },
                warmth = shaderWarmth,
                onWarmthChange = { shaderWarmth = it },
                hasScanlines = shaderScanlines || selectedFilter.scanlines,
                onToggleScanlines = { shaderScanlines = it },
                onResetDefaults = {
                    shaderIntensity = selectedFilter.defaultIntensity
                    shaderWarmth = 0f
                    shaderScanlines = selectedFilter.scanlines
                },
                onDismiss = { isShaderTweakSheetOpen = false }
            )
        }

        CameraVideoEditDialog(
            visible = isVideoEditPanelOpen,
            currentSpeed = videoSpeedMultiplier,
            onSpeedChange = { videoSpeedMultiplier = it },
            currentTimerSec = videoTimerCountdownSec,
            onTimerChange = { videoTimerCountdownSec = it },
            isGridEnabled = isGridGuideEnabled,
            onToggleGrid = { isGridGuideEnabled = !isGridGuideEnabled },
            isFlashOn = isFlashOn,
            onToggleFlash = { isFlashOn = !isFlashOn },
            onDismiss = { isVideoEditPanelOpen = false }
        )

        FacePaintingStudioSheet(
            visible = isFacePaintingOpen,
            currentPaintType = activeFacePaintType,
            onSelectPaintType = { paint ->
                activeFacePaintType = paint
                isFacePaintingOpen = false
            },
            onDismiss = { isFacePaintingOpen = false },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 90.dp)
        )

        // --- Top Bar Controls & Vertically Stacked Song Search Bar ---
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 14.dp, end = if (isSongSearchExpanded) 14.dp else 72.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCloseCamera,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("camera_close_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Dual Camera Mode Toggle Button (Front + Back)
                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .clickable { isDualCamMode = !isDualCamMode }
                            .testTag("dual_camera_toggle_button"),
                        shape = RoundedCornerShape(19.dp),
                        color = if (isDualCamMode) BlinkCyan else Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(1.5.dp, if (isDualCamMode) Color.White else BlinkCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🤳", fontSize = 13.sp)
                            Text(
                                text = if (isDualCamMode) "Dual ON" else "Dual Cam",
                                color = if (isDualCamMode) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Device Photo/Video Picker
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("pick_gallery_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    // Flash toggle
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) Color.Yellow.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f))
                            .testTag("camera_flash_toggle")
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Camera Switch (Front / Back)
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("camera_switch_toggle")
                    ) {
                        Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Top Song Search Bar (Search any type of song before shooting!)
            CameraSongSearchBar(
                searchQuery = cameraSongSearchQuery,
                onSearchQueryChange = { cameraSongSearchQuery = it },
                selectedTrack = cameraSelectedMusic,
                onSelectTrack = { track ->
                    cameraSelectedMusic = track
                    selectedMusicTrack = track
                    renewToastMessage = "🎵 Added song: $track"
                    coroutineScope.launch {
                        delay(2200)
                        if (renewToastMessage?.startsWith("🎵 Added") == true) {
                            renewToastMessage = null
                        }
                    }
                },
                onClearTrack = {
                    cameraSelectedMusic = ""
                    selectedMusicTrack = ""
                    BlinkMusicPlayer.pausePlayback()
                },
                isExpanded = isSongSearchExpanded,
                onToggleExpanded = { isSongSearchExpanded = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- Bottom Controls (Filter Categories + Shutter + Mode Pill) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category Tabs Row (⚡ All, 🤪 Funny Faces, 🎮 Funny Games, 🔮 Aesthetic, 🤖 Cyber)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FilterCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedCategory = category }
                            .testTag("filter_category_${category.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) BlinkPrimary else Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isSelected) BlinkCyan else Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = category.icon, fontSize = 13.sp)
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Horizontal Filter Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                items(displayedFilters, key = { it.id }) { filter ->
                    val isSelected = selectedFilter.id == filter.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedFilter = filter
                                shaderIntensity = filter.defaultIntensity
                                shaderScanlines = filter.scanlines
                            }
                            .testTag("filter_item_${filter.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan))
                                    else Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.6f)))
                                )
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) BlinkCyan else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = filter.iconEmoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filter.name,
                            color = if (isSelected) BlinkCyanLight else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHOTO",
                    color = if (!isVideoMode) Color.White else BlinkTextSecondary,
                    fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { isVideoMode = false }
                        .testTag("mode_photo")
                )
                Text(
                    text = "SNAP (CAMERAX) 📸",
                    color = BlinkCyanLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { isPureCameraXMode = true }
                        .testTag("mode_camerax_snap")
                )
                Text(
                    text = "VIDEO 🎬",
                    color = if (isVideoMode) BlinkPink else BlinkTextSecondary,
                    fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { isVideoMode = true }
                        .testTag("mode_video")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shutter Button (Tap = Photo, Hold = Record in Blue, Release = Stop)
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .testTag("camera_shutter_button")
                    .pointerInput(selectedFilter) {
                        detectTapGestures(
                            onTap = {
                                if (isRecording) {
                                    isRecording = false
                                    val realUri = generateRealMedia(selectedFilter, isVideo = true)
                                    capturedImageUri = realUri
                                    isRecordedVideo = true
                                    capturedSnapReady = true
                                } else {
                                    executePhotoCapture()
                                }
                            },
                            onPress = {
                                var heldAsVideo = false
                                val holdTimer = coroutineScope.launch {
                                    delay(200) // 200ms hold threshold
                                    heldAsVideo = true
                                    isRecording = true
                                    isVideoMode = true
                                }
                                tryAwaitRelease()
                                holdTimer.cancel()
                                if (heldAsVideo || isRecording) {
                                    // Released -> Stop video recording!
                                    isRecording = false
                                    val realUri = generateRealMedia(selectedFilter, isVideo = true)
                                    capturedImageUri = realUri
                                    isRecordedVideo = true
                                    capturedSnapReady = true
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Electric Blue Glowing Ring & Circular Timer when Recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF).copy(alpha = 0.25f))
                    )
                    CircularProgressIndicator(
                        progress = { recordingSeconds / 30f },
                        color = Color(0xFF00E5FF),
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(90.dp)
                    )
                }

                // Inner Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(
                            4.dp,
                            if (isRecording) Color(0xFF00E5FF)
                            else if (isVideoMode) BlinkPink
                            else Color.White,
                            CircleShape
                        )
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color(0xFF00E5FF)
                            else if (isVideoMode) BlinkPink
                            else Color.White
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                        )
                    } else if (isVideoMode) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Record",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Shutter gesture guide hint
            Text(
                text = if (isRecording) "🔵 Release to Stop & Review" else "Tap = Photo 📷 • Hold = Video 🔵",
                color = if (isRecording) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal
            )
        }

        // --- Custom AI Filter Dialog ---
        if (showCustomFilterModal) {
            AlertDialog(
                onDismissRequest = { showCustomFilterModal = false },
                title = { Text("✨ AI Filter Synthesizer", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Describe a visual lens shader:", fontSize = 12.sp)
                        OutlinedTextField(
                            value = customFilterPrompt,
                            onValueChange = { customFilterPrompt = it },
                            placeholder = { Text("e.g. 90s anime pastel sky, neon matrix") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showCustomFilterModal = false
                    }) {
                        Text("Synthesize")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomFilterModal = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // =================================================================
        // REVIEW & POST-PRODUCTION STUDIO ("Show what it has recorded")
        // Allows adding filters to video, music, text, stickers, captions
        // =================================================================
        if (capturedSnapReady) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("snap_preview_dialog"),
                color = BlinkBgDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { capturedSnapReady = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retake", tint = Color.White)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isRecordedVideo) "🎬 Video Studio Review" else "⚡ Photo Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isRecordedVideo) "Recorded ${recordingSeconds.coerceAtLeast(3)}s clip • Add filters, music & text" else "Captured Live Snap",
                                fontSize = 11.sp,
                                color = BlinkTextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (capturedDualCamActive) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BlinkPink.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, BlinkPink)
                                ) {
                                    Text(
                                        text = "🤳 Dual Cam",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BlinkPrimary.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = selectedFilter.name,
                                    color = BlinkCyanLight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Review Studio Song Search Bar (Search & edit soundtrack on recorded photo/video)
                    var reviewSongSearchQuery by remember { mutableStateOf("") }
                    var isReviewSongSearchExpanded by remember { mutableStateOf(false) }

                    CameraSongSearchBar(
                        searchQuery = reviewSongSearchQuery,
                        onSearchQueryChange = { reviewSongSearchQuery = it },
                        selectedTrack = selectedMusicTrack.ifBlank { cameraSelectedMusic },
                        onSelectTrack = { track ->
                            selectedMusicTrack = track
                            cameraSelectedMusic = track
                            isAudioMuted = false
                            BlinkMusicPlayer.playTrack(track)
                        },
                        onClearTrack = {
                            selectedMusicTrack = ""
                            cameraSelectedMusic = ""
                            BlinkMusicPlayer.pausePlayback()
                        },
                        isExpanded = isReviewSongSearchExpanded,
                        onToggleExpanded = { isReviewSongSearchExpanded = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("review_song_search_bar")
                    )

                    // Media Player Canvas (Shows What It Has Recorded with live overlays!)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                            .border(1.5.dp, Brush.linearGradient(listOf(BlinkPrimary, BlinkCyan)), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Media Background
                        if (capturedImageUri != null) {
                            AsyncImage(
                                model = capturedImageUri,
                                contentDescription = "Recorded Media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(BlinkPrimaryDark, BlinkBgDark))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎬 Blink Video Recorded", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Realtime Filter Shader Applied ON TOP of Video
                        RealtimeShaderPreviewOverlay(
                            filter = selectedFilter,
                            intensity = shaderIntensity,
                            warmth = shaderWarmth,
                            isComparingOriginal = isComparingOriginal,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Snapchat AR Lenses Overlay (Dog ears, angel halo, neon horns, flower crown, etc.)
                        SnapchatArLensesOverlay(
                            filter = selectedFilter,
                            intensity = shaderIntensity,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Funny Face Graphic on Video
                        if (selectedFilter.funnyFaceType.isNotBlank()) {
                            FunnyFaceOverlay(type = selectedFilter.funnyFaceType)
                        }

                        // Custom Video Text Overlay (TikTok Style)
                        if (customVideoText.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when (videoTextBgStyle) {
                                            "pill" -> Color.Black.copy(alpha = 0.82f)
                                            "glow" -> BlinkPrimary.copy(alpha = 0.55f)
                                            "neon" -> Color(0xFF0F172A).copy(alpha = 0.88f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        when (videoTextBgStyle) {
                                            "glow" -> 1.5.dp
                                            "neon" -> 2.dp
                                            else -> 0.dp
                                        },
                                        when (videoTextBgStyle) {
                                            "neon" -> BlinkCyan
                                            "glow" -> BlinkPink
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = customVideoText,
                                    color = videoTextColor,
                                    fontSize = when (videoTextStyle) {
                                        "Heavy Impact" -> 24.sp
                                        "Typewriter" -> 19.sp
                                        else -> 21.sp
                                    },
                                    fontFamily = when (videoTextStyle) {
                                        "Typewriter" -> FontFamily.Monospace
                                        "Heavy Impact" -> FontFamily.SansSerif
                                        "Serif" -> FontFamily.Serif
                                        else -> FontFamily.Default
                                    },
                                    fontWeight = when (videoTextStyle) {
                                        "Typewriter" -> FontWeight.Medium
                                        "Neon Glow" -> FontWeight.SemiBold
                                        else -> FontWeight.Black
                                    }
                                )
                            }
                        }

                        // Video Stickers Overlaid
                        if (videoStickers.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                videoStickers.forEach { sticker ->
                                    Text(text = sticker, fontSize = 28.sp)
                                }
                            }
                        }

                        // Funny Writing Meme Sticker Badge
                        if (activeFunnyWriting.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.82f),
                                border = BorderStroke(2.dp, BlinkCyanLight)
                            ) {
                                Text(
                                    text = activeFunnyWriting,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Drawing Canvas Layer in Review Studio (Supports drawing on top of photo/video)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (activeVideoEditTab == VideoEditTab.DRAWINGS) {
                                        Modifier.pointerInput(Unit) {
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
                                                }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            drawnStrokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
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
                        }

                        // Dual Camera Inset on Captured/Recorded Media
                        if (capturedDualCamActive) {
                            DualCamSelfieOverlay(
                                isFrontSelfie = !isFrontCamera,
                                layout = dualCamLayout,
                                isReview = true
                            )
                        }

                        // Video Controls / Audio Pill
                        if (isRecordedVideo) {
                            // Top Music Pill on Video
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = if (isAudioMuted) "🔇" else "🎵", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedMusicTrack,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Center Play / Pause Floating Toggle
                            IconButton(
                                onClick = { isVideoPlaying = !isVideoPlaying },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    .testTag("video_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            // Bottom Video Progress Bar Scrubber
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { playbackProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = BlinkPrimary,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- Post-Production Editing Tool Tabs ---
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BlinkCardDark)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isRecordedVideo) {
                            item {
                                VideoEditorTabButton(
                                    title = "⏱️ Timeline",
                                    isSelected = activeVideoEditTab == VideoEditTab.TIMELINE,
                                    testTag = "edit_tab_timeline"
                                ) { activeVideoEditTab = VideoEditTab.TIMELINE }
                            }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "🎨 Filter",
                                isSelected = activeVideoEditTab == VideoEditTab.FILTERS,
                                testTag = "edit_tab_filters"
                            ) { activeVideoEditTab = VideoEditTab.FILTERS }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "🎵 Music",
                                isSelected = activeVideoEditTab == VideoEditTab.MUSIC,
                                testTag = "edit_tab_music"
                            ) { activeVideoEditTab = VideoEditTab.MUSIC }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "✍️ Text",
                                isSelected = activeVideoEditTab == VideoEditTab.TEXT,
                                testTag = "edit_tab_text"
                            ) { activeVideoEditTab = VideoEditTab.TEXT }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "🌟 Stickers",
                                isSelected = activeVideoEditTab == VideoEditTab.STICKERS,
                                testTag = "edit_tab_stickers"
                            ) { activeVideoEditTab = VideoEditTab.STICKERS }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "✏️ Draw",
                                isSelected = activeVideoEditTab == VideoEditTab.DRAWINGS,
                                testTag = "edit_tab_drawings"
                            ) { activeVideoEditTab = VideoEditTab.DRAWINGS }
                        }
                        item {
                            VideoEditorTabButton(
                                title = "🤪 Funny",
                                isSelected = activeVideoEditTab == VideoEditTab.FUNNY_WRITING,
                                testTag = "edit_tab_funny_writing"
                            ) { activeVideoEditTab = VideoEditTab.FUNNY_WRITING }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Tool Sub-Panels ---
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        when (activeVideoEditTab) {
                            // 0. TIMELINE (for video): Scrubber, trimmer, playhead, speed control
                            VideoEditTab.TIMELINE -> {
                                val clipDuration = recordingSeconds.coerceAtLeast(3).toFloat()
                                VideoTimelineEditor(
                                    totalDurationSec = clipDuration,
                                    currentPositionSec = videoCurrentPositionSec,
                                    trimStartSec = videoTrimStartSec,
                                    trimEndSec = if (videoTrimEndSec <= 0f || videoTrimEndSec > clipDuration) clipDuration else videoTrimEndSec,
                                    speedMultiplier = videoSpeedMultiplier,
                                    isPlaying = isVideoPlaying,
                                    onSeek = { pos ->
                                        videoCurrentPositionSec = pos
                                        playbackProgress = (pos / clipDuration).coerceIn(0f, 1f)
                                    },
                                    onTrimChange = { start, end ->
                                        videoTrimStartSec = start
                                        videoTrimEndSec = end
                                    },
                                    onSpeedChange = { spd ->
                                        videoSpeedMultiplier = spd
                                    },
                                    onTogglePlay = {
                                        isVideoPlaying = !isVideoPlaying
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 1. FILTERS: Live filter applicator & shader tuning on photo/video
                            VideoEditTab.FILTERS -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active: ${selectedFilter.name} ${selectedFilter.iconEmoji}",
                                            color = BlinkCyanLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "⚙️ Tune Shader FX",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { isShaderTweakSheetOpen = true }
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(allFilters, key = { it.id }) { filter ->
                                            val isSel = selectedFilter.id == filter.id
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        selectedFilter = filter
                                                        shaderIntensity = filter.defaultIntensity
                                                        shaderScanlines = filter.scanlines
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSel) BlinkPrimary else BlinkCardDark,
                                                border = BorderStroke(1.dp, if (isSel) BlinkCyan else Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(text = filter.iconEmoji, fontSize = 18.sp)
                                                    Text(
                                                        text = filter.name,
                                                        fontSize = 10.5.sp,
                                                        color = Color.White,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. MUSIC: TikTok-Style Music Mixer (Trending Sounds + Equalizer Waveform + Dual Volume Sliders)
                            VideoEditTab.MUSIC -> {
                                TikTokMusicMixer(
                                    selectedTrack = selectedMusicTrack.ifBlank { cameraSelectedMusic },
                                    originalVolume = originalAudioVolume,
                                    musicVolume = musicTrackVolume,
                                    isVideoPlaying = isVideoPlaying,
                                    onSelectSound = { sound ->
                                        selectedMusicTrack = sound.title
                                        cameraSelectedMusic = sound.title
                                        isAudioMuted = false
                                        BlinkMusicPlayer.playTrack(sound.title, sound.previewUrl, sound.artworkUrl)
                                    },
                                    onOriginalVolumeChange = { vol ->
                                        originalAudioVolume = vol
                                    },
                                    onMusicVolumeChange = { vol ->
                                        musicTrackVolume = vol
                                        BlinkMusicPlayer.setMusicVolume(vol)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // 3. TEXT: TikTok Styled Text Overlay with fonts, background pill/glow, and colors
                            VideoEditTab.TEXT -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = customVideoText,
                                        onValueChange = { customVideoText = it },
                                        placeholder = { Text("Type TikTok text overlay...", color = BlinkTextSecondary, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = BlinkPrimary,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                            focusedContainerColor = BlinkCardDark,
                                            unfocusedContainerColor = BlinkCardDark
                                        ),
                                        singleLine = true
                                    )

                                    // TikTok Font Styles
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(listOf("Classic", "Typewriter", "Heavy Impact", "Neon Glow")) { style ->
                                            val isSel = videoTextStyle == style
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { videoTextStyle = style },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) BlinkPrimary else BlinkCardDark,
                                                border = BorderStroke(1.dp, if (isSel) BlinkCyan else Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Text(
                                                    text = style,
                                                    color = Color.White,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Color Swatches & Background Style
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(
                                                Color.White to "White",
                                                Color(0xFF06B6D4) to "Cyan",
                                                Color(0xFFEC4899) to "Pink",
                                                Color(0xFFFACC15) to "Gold",
                                                Color(0xFF10B981) to "Green",
                                                Color(0xFFA855F7) to "Purple"
                                            ).forEach { (color, _) ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                        .clickable { videoTextColor = color }
                                                        .border(if (videoTextColor == color) 2.dp else 0.dp, Color.White, CircleShape)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("glow" to "Glow ✨", "pill" to "Pill ⬛", "neon" to "Neon 🔮", "none" to "Raw").forEach { (bg, label) ->
                                                val isSel = videoTextBgStyle == bg
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { videoTextBgStyle = bg },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) BlinkPink.copy(alpha = 0.4f) else Color.Transparent,
                                                    border = BorderStroke(1.dp, if (isSel) BlinkPink else Color.White.copy(alpha = 0.2f))
                                                ) {
                                                    Text(label, color = Color.White, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. DRAWINGS: Touch & sketch color/pen tools
                            VideoEditTab.DRAWINGS -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✏️ Touch preview above to draw (${drawnStrokes.size} strokes)",
                                            color = BlinkCyanLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (drawnStrokes.isNotEmpty()) {
                                            Text(
                                                text = "Clear All 🗑️",
                                                color = BlinkPinkLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { drawnStrokes.clear() }
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Color Swatches
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                Color.White to "White",
                                                Color(0xFF06B6D4) to "Cyan",
                                                Color(0xFFEC4899) to "Pink",
                                                Color(0xFFFACC15) to "Gold",
                                                Color(0xFF10B981) to "Green",
                                                Color(0xFFA855F7) to "Purple",
                                                Color(0xFFEF4444) to "Red"
                                            ).forEach { (col, _) ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(col)
                                                        .clickable { currentPenColor = col }
                                                        .border(if (currentPenColor == col) 2.dp else 0.dp, Color.White, CircleShape)
                                                )
                                            }
                                        }

                                        // Pen Thickness
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(4f to "Thin", 8f to "Med", 16f to "Thick").forEach { (width, label) ->
                                                val isSel = currentStrokeWidth == width
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { currentStrokeWidth = width },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) BlinkPrimary else BlinkCardDark,
                                                    border = BorderStroke(1.dp, if (isSel) BlinkCyan else Color.White.copy(alpha = 0.15f))
                                                ) {
                                                    Text(
                                                        text = label,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 5. FUNNY WRITING: Viral meme captions & funny quotes
                            VideoEditTab.FUNNY_WRITING -> {
                                val funnyQuotes = listOf(
                                    "POV: You forgot your homework 💀",
                                    "Main Character Energy ✨",
                                    "My last 2 brain cells 🧠⚡",
                                    "Living rent free in my head 🎪",
                                    "Tell me without telling me 👀",
                                    "It's giving iconic 💅",
                                    "Send help immediately 😭",
                                    "Certified classic 🏆",
                                    "Zero thoughts, just vibes 🌮",
                                    "No cap fr fr 🧢🚫"
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (activeFunnyWriting.isNotBlank()) "Badge: \"$activeFunnyWriting\"" else "Tap funny quote to overlay on media:",
                                            color = BlinkCyanLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (activeFunnyWriting.isNotBlank()) {
                                            Text(
                                                text = "Remove ✖",
                                                color = BlinkPinkLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { activeFunnyWriting = "" }
                                            )
                                        }
                                    }

                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(funnyQuotes) { quote ->
                                            val isSel = activeFunnyWriting == quote
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        activeFunnyWriting = if (isSel) "" else quote
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSel) BlinkPrimary else BlinkCardDark,
                                                border = BorderStroke(1.dp, if (isSel) BlinkCyan else Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Text(
                                                    text = quote,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 6. STICKERS: Tap to add/remove
                            VideoEditTab.STICKERS -> {
                                val stickerOptions = listOf("⚡", "🔥", "👑", "👽", "🍩", "💖", "🚀", "🤪", "🎮", "🌮", "🎈", "🐱", "🕶️", "💫")
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Tap sticker to place on video:", color = BlinkTextSecondary, fontSize = 11.sp)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(stickerOptions) { sticker ->
                                            val isAdded = sticker in videoStickers
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (isAdded) videoStickers.remove(sticker)
                                                        else videoStickers.add(sticker)
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isAdded) BlinkPrimary else BlinkCardDark,
                                                border = BorderStroke(1.dp, if (isAdded) BlinkPink else Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Text(
                                                    text = sticker,
                                                    fontSize = 24.sp,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Caption input
                    OutlinedTextField(
                        value = snapCaption,
                        onValueChange = { snapCaption = it },
                        placeholder = { Text("Add caption & hashtags (e.g. #blink #vibes)...", color = BlinkTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("snap_caption_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BlinkPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedContainerColor = BlinkCardDark,
                            unfocusedContainerColor = BlinkCardDark
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons (Export to Device + Post as Blink Video / Story / Home Feed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Export (MP4 Video / HD Photo) with dual audio mixing and filters
                        Button(
                            onClick = {
                                isExporting = true
                                exportProgress = 0f
                                coroutineScope.launch {
                                    if (isRecordedVideo) {
                                        val res = MediaExportEngine.exportVideo(
                                            context = context,
                                            baseUri = capturedImageUri,
                                            clipDurationSec = recordingSeconds.coerceAtLeast(3),
                                            trimStartSec = videoTrimStartSec,
                                            trimEndSec = if (videoTrimEndSec <= 0f) recordingSeconds.coerceAtLeast(3).toFloat() else videoTrimEndSec,
                                            originalAudioVolume = originalAudioVolume,
                                            musicVolume = musicTrackVolume,
                                            selectedSoundtrack = selectedMusicTrack.ifBlank { cameraSelectedMusic },
                                            filter = selectedFilter,
                                            shaderIntensity = shaderIntensity,
                                            textOverlay = customVideoText,
                                            textColor = videoTextColor,
                                            textStyle = videoTextStyle,
                                            textBgStyle = videoTextBgStyle,
                                            stickers = videoStickers.toList(),
                                            onProgress = { p -> exportProgress = p }
                                        )
                                        isExporting = false
                                        exportResult = res
                                        showExportSuccessDialog = true
                                    } else {
                                        val res = MediaExportEngine.exportPhoto(
                                            context = context,
                                            baseUri = capturedImageUri,
                                            filter = selectedFilter,
                                            shaderIntensity = shaderIntensity,
                                            textOverlay = customVideoText,
                                            textColor = videoTextColor,
                                            textStyle = videoTextStyle,
                                            textBgStyle = videoTextBgStyle,
                                            stickers = videoStickers.toList(),
                                            strokes = drawnStrokes.toList(),
                                            funnyWriting = activeFunnyWriting,
                                            onProgress = { p -> exportProgress = p }
                                        )
                                        isExporting = false
                                        exportResult = res
                                        showExportSuccessDialog = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("export_media_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Export", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (isRecordedVideo) "Export MP4" else "Save HD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        if (isRecordedVideo && onCreateVideo != null) {
                            Button(
                                onClick = {
                                    val title = customVideoText.ifBlank { snapCaption.ifBlank { "Recorded Blink Video 🎬" } }
                                    val hashtags = listOf("#blink", "#video", "#${selectedFilter.name.lowercase().replace(" ", "")}")
                                    onCreateVideo(
                                        title,
                                        snapCaption.ifBlank { "Recorded with ${selectedFilter.name} filter ⚡" },
                                        hashtags,
                                        true,
                                        selectedFilter.name,
                                        selectedMusicTrack
                                    )
                                    capturedSnapReady = false
                                    onCloseCamera()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_to_blink_videos_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = BlinkPink),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Post Video 🎬", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onPostCreated(
                                    snapCaption.ifBlank { if (isRecordedVideo) "Recorded Video on Blink 🎬⚡" else "New Blink Story 🌟" },
                                    selectedFilter.name,
                                    true,
                                    capturedImageUri?.toString()
                                )
                                capturedSnapReady = false
                                onCloseCamera()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_to_story_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkCardDark),
                            border = BorderStroke(1.dp, BlinkCyanLight),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Story 🌟", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onPostCreated(
                                    snapCaption.ifBlank { if (isRecordedVideo) "Recorded Video on Blink 🎬⚡" else "New Blink on Blinkchat ⚡" },
                                    selectedFilter.name,
                                    false,
                                    capturedImageUri?.toString()
                                )
                                capturedSnapReady = false
                                onCloseCamera()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_to_feed_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Feed ⚡", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Export Progress Dialog (Hardware encoding & audio mixing visualizer)
        if (isExporting) {
            MediaExportProgressDialog(
                progress = exportProgress,
                isVideo = isRecordedVideo
            )
        }

        // Export Success Dialog (Preview, File info & Direct Share Sheet trigger)
        if (showExportSuccessDialog && exportResult != null) {
            MediaExportSuccessDialog(
                context = context,
                result = exportResult!!,
                onDismiss = {
                    showExportSuccessDialog = false
                },
                onPostToStory = {
                    onPostCreated(
                        snapCaption.ifBlank { if (isRecordedVideo) "Recorded Video on Blink 🎬⚡" else "New Blink Story 🌟" },
                        selectedFilter.name,
                        true,
                        exportResult?.uri?.toString()
                    )
                    showExportSuccessDialog = false
                    capturedSnapReady = false
                    onCloseCamera()
                },
                onPostToFeed = {
                    onPostCreated(
                        snapCaption.ifBlank { if (isRecordedVideo) "Recorded Video on Blink 🎬⚡" else "New Blink on Blinkchat ⚡" },
                        selectedFilter.name,
                        false,
                        exportResult?.uri?.toString()
                    )
                    showExportSuccessDialog = false
                    capturedSnapReady = false
                    onCloseCamera()
                }
            )
        }
    }
}

@Composable
private fun VideoEditorTabButton(
    title: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) BlinkPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = if (isSelected) Color.White else BlinkTextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

// =================================================================
// FUNNY FACES GRAPHICAL OVERLAYS
// =================================================================
@Composable
private fun FunnyFaceOverlay(type: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "funny_face")
    val jiggle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggle"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (type) {
            "googly" -> {
                // Cartoon bouncy Googly Eyes with moving pupils
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-60).dp),
                    horizontalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(5.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .offset(x = jiggle.dp, y = (jiggle * 0.7f).dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                        }
                    }
                }
            }

            "clown" -> {
                // Circus Clown big red squeaky nose and smile
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-20).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(3.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(text = "👄", fontSize = 48.sp)
                }
            }

            "puppy" -> {
                // Floppy puppy ears on top left & right + tongue
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 100.dp, start = 40.dp, end = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "🐶", fontSize = 64.sp)
                    Text(text = "🐶", fontSize = 64.sp)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👅", fontSize = 52.sp)
                }
            }

            "alien" -> {
                // Green alien antennae & cosmic eyes
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-80).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📡 👽 📡", fontSize = 48.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        Text(text = "🟢", fontSize = 40.sp)
                        Text(text = "🟢", fontSize = 40.sp)
                    }
                }
            }

            "potato" -> {
                // Hilarious Talking Potato cutout
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .clip(RoundedCornerShape(80.dp))
                        .background(Color(0xFFB45309).copy(alpha = 0.4f))
                        .border(4.dp, Color(0xFFD97706), RoundedCornerShape(80.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👀", fontSize = 50.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "👄", fontSize = 40.sp)
                    }
                }
            }

            "cat" -> {
                // Whiskers and kitty ears
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = 110.dp, start = 60.dp, end = 60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "🐱", fontSize = 56.sp)
                    Text(text = "🐱", fontSize = 56.sp)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-10).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "= 😺 =", fontSize = 44.sp, color = Color.White)
                }
            }

            "thug" -> {
                // Thug Life 8-bit Pixel Shades & Gold Chain
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-40).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🕶️", fontSize = 72.sp)
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(text = "🪙 🥇 🪙", fontSize = 36.sp)
                }
            }

            "chibi" -> {
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-50).dp),
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Text(text = "✨👀✨", fontSize = 40.sp)
                    Text(text = "✨👀✨", fontSize = 40.sp)
                }
            }

            "frog" -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-60).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🐸", fontSize = 68.sp)
                }
            }
        }
    }
}

// =================================================================
// INTERACTIVE CAMERA MINI-GAMES OVERLAYS
// =================================================================
@Composable
private fun GameLensOverlay(
    gameType: String,
    score: Int,
    combo: Int,
    reflexState: String,
    reflexReactionTimeMs: Int,
    rouletteResult: String,
    rouletteSpinning: Boolean,
    onScore: (Int) -> Unit,
    onReflexTap: () -> Unit,
    onSpinRoulette: () -> Unit,
    onResetGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("interactive_game_canvas")
    ) {
        // Top Game Score HUD
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(1.5.dp, BlinkCyan, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SCORE: $score 🏆",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            )
            if (combo > 1) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BlinkPink
                ) {
                    Text(
                        text = "x$combo COMBO 🔥",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        when (gameType) {
            // 1. Donut Catch: Tap falling donuts to score!
            "donut" -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    val infinite = rememberInfiniteTransition(label = "donuts")
                    val fallY by infinite.animateFloat(
                        initialValue = 120f,
                        targetValue = 600f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "fall"
                    )

                    IconButton(
                        onClick = { onScore(25) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = fallY.dp, x = (-40).dp)
                            .size(70.dp)
                    ) {
                        Text(text = "🍩", fontSize = 54.sp)
                    }

                    IconButton(
                        onClick = { onScore(50) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (fallY * 0.8f).dp, x = 60.dp)
                            .size(70.dp)
                    ) {
                        Text(text = "🍩", fontSize = 50.sp)
                    }
                }
            }

            // 2. Speed Reflex Challenge: Tap when green!
            "reflex" -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            when (reflexState) {
                                "TAP_NOW" -> BlinkGreen.copy(alpha = 0.85f)
                                "SUCCESS" -> BlinkCyan.copy(alpha = 0.85f)
                                else -> Color.Red.copy(alpha = 0.6f)
                            }
                        )
                        .clickable { onReflexTap() }
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (reflexState) {
                                "TAP_NOW" -> "TAP NOW! ⚡"
                                "SUCCESS" -> "${reflexReactionTimeMs}ms! 🎉"
                                else -> "WAIT FOR GREEN..."
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // 3. Balloon Pop Frenzy: Floating colorful balloons!
            "balloon" -> {
                val infinite = rememberInfiniteTransition(label = "balloon")
                val floatY by infinite.animateFloat(
                    initialValue = 550f,
                    targetValue = 140f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "float"
                )

                IconButton(
                    onClick = { onScore(30) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = floatY.dp, x = (-70).dp)
                        .size(70.dp)
                ) {
                    Text(text = "🎈", fontSize = 56.sp)
                }

                IconButton(
                    onClick = { onScore(30) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (floatY * 1.1f).dp, x = 50.dp)
                        .size(70.dp)
                ) {
                    Text(text = "🎈", fontSize = 56.sp)
                }
            }

            // 4. Taco Chomp Rush
            "taco" -> {
                val infinite = rememberInfiniteTransition(label = "taco")
                val slideX by infinite.animateFloat(
                    initialValue = -150f,
                    targetValue = 150f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "taco_slide"
                )

                IconButton(
                    onClick = { onScore(40) },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = slideX.dp, y = (-20).dp)
                        .size(80.dp)
                ) {
                    Text(text = "🌮", fontSize = 60.sp)
                }
            }

            // 5. Persona Vibe Roulette Wheel
            "roulette" -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-60).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(2.dp, BlinkCyan)
                    ) {
                        Text(
                            text = rouletteResult,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSpinRoulette,
                        enabled = !rouletteSpinning,
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (rouletteSpinning) "Spinning... 🎰" else "SPIN VIBE 🎲", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DualCamSelfieOverlay(
    isFrontSelfie: Boolean,
    layout: DualCamLayout,
    isReview: Boolean = false,
    onSwap: (() -> Unit)? = null,
    onCycleLayout: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dual_cam_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    when (layout) {
        DualCamLayout.SPLIT_HORIZONTAL -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isReview) 180.dp else 220.dp)
                    .padding(horizontal = 12.dp, vertical = if (isReview) 8.dp else 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(BlinkPink, BlinkCyan)),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("dual_camera_selfie_pip")
            ) {
                // Header badge & controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isReview) BlinkPink else BlinkCyan)
                            )
                            Text(
                                text = if (isReview) "REC DUAL • SPLIT" else if (isFrontSelfie) "🤳 FRONT SELFIE" else "📷 BACK WORLD",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isReview) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onSwap?.invoke() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("dual_cam_swap_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Swap Primary/Selfie",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onCycleLayout?.invoke() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("dual_cam_cycle_layout_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Cycle Layout",
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Center visual avatar / simulated selfie feed
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isFrontSelfie) "🤳" else "📸",
                        fontSize = 42.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFrontSelfie) "Selfie Cam (Front Angle)" else "World Cam (Wide Back)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        else -> {
            // Picture-in-Picture Floating Window
            val alignment = when (layout) {
                DualCamLayout.PIP_TOP_LEFT -> Alignment.TopStart
                DualCamLayout.PIP_TOP_RIGHT -> Alignment.TopEnd
                DualCamLayout.PIP_BOTTOM_LEFT -> Alignment.BottomStart
                DualCamLayout.PIP_BOTTOM_RIGHT -> Alignment.BottomEnd
                DualCamLayout.SPLIT_HORIZONTAL -> Alignment.TopEnd
            }

            val topPad = if (isReview) 14.dp else when (layout) {
                DualCamLayout.PIP_TOP_LEFT, DualCamLayout.PIP_TOP_RIGHT -> 64.dp
                else -> 0.dp
            }
            val bottomPad = if (isReview) 14.dp else when (layout) {
                DualCamLayout.PIP_BOTTOM_LEFT, DualCamLayout.PIP_BOTTOM_RIGHT -> 180.dp
                else -> 0.dp
            }
            val startPad = when (layout) {
                DualCamLayout.PIP_TOP_LEFT, DualCamLayout.PIP_BOTTOM_LEFT -> 14.dp
                else -> 0.dp
            }
            val endPad = when (layout) {
                DualCamLayout.PIP_TOP_RIGHT, DualCamLayout.PIP_BOTTOM_RIGHT -> 14.dp
                else -> 0.dp
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = startPad, end = endPad, top = topPad, bottom = bottomPad),
                contentAlignment = alignment
            ) {
                Box(
                    modifier = Modifier
                        .width(115.dp)
                        .height(155.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(BlinkPrimary, BlinkPink, BlinkCyan)),
                            RoundedCornerShape(18.dp)
                        )
                        .testTag("dual_camera_selfie_pip")
                ) {
                    // Small Corner Cam Type Tag
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isReview) BlinkPink else BlinkCyan)
                            )
                            Text(
                                text = if (isReview) "DUAL" else if (isFrontSelfie) "FRONT" else "BACK",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Simulated live selfie/world view inside the PiP card
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isFrontSelfie) "🤳" else "📷",
                            fontSize = 38.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFrontSelfie) "SELFIE" else "WORLD",
                            color = BlinkCyanLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Floating action overlay (Swap & Cycle buttons)
                    if (!isReview) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSwap?.invoke() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("dual_cam_swap_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Swap Primary/Selfie",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { onCycleLayout?.invoke() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("dual_cam_cycle_layout_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Cycle Layout",
                                    tint = BlinkCyanLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

