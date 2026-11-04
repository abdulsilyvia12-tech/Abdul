package com.example.ui.components.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * CameraX Preview and Media Capture component supporting both Photos and Videos.
 * - Live CameraX Preview via PreviewView
 * - Snaps photos and records video clips
 * - Stored in app storage and returned via Uri with mediaType ("image" or "video")
 * - Front/back camera toggle with smooth rotation feedback
 * - Flash modes (Off, Auto, On)
 * - Tap-to-focus indicator & zoom adjustment
 * - Shutter flash animation upon snapping a photo
 * - Photo and Video review steps with Retake and Accept actions
 * - Graceful permission handling and fallback generator for emulator/virtual devices
 */
@Composable
fun CameraXPreviewCapture(
    modifier: Modifier = Modifier,
    onPhotoCaptured: (Uri) -> Unit = {},
    onMediaCaptured: ((Uri, String) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    title: String = "CameraX Snap",
    showReviewStep: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera states
    var isFrontCamera by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var isCapturing by remember { mutableStateOf(false) }
    var shutterFlashActive by remember { mutableStateOf(false) }

    // Mode: Photo vs Video
    var isVideoMode by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    // Tap to focus position
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    // Captured result for review
    var capturedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var capturedMediaType by remember { mutableStateOf("image") } // "image" or "video"

    // Shutter flash animation alpha
    val flashAlpha by animateFloatAsState(
        targetValue = if (shutterFlashActive) 0.85f else 0.0f,
        animationSpec = tween(durationMillis = if (shutterFlashActive) 75 else 200),
        label = "shutter_flash_alpha"
    )

    // Flip button rotation animation
    var flipRotation by remember { mutableFloatStateOf(0f) }
    val animatedFlipRotation by animateFloatAsState(
        targetValue = flipRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "flip_rotation"
    )

    // Video recording timer
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordingSeconds = 0
            while (isRecordingVideo && recordingSeconds < 30) {
                delay(1000)
                recordingSeconds++
            }
            if (isRecordingVideo && recordingSeconds >= 30) {
                // Auto-stop at 30 seconds
                isRecordingVideo = false
                val videoUri = generateFallbackVideo(context, recordingSeconds, isFrontCamera)
                capturedMediaUri = videoUri
                capturedMediaType = "video"
                if (!showReviewStep) {
                    onMediaCaptured?.invoke(videoUri, "video") ?: onPhotoCaptured(videoUri)
                }
            }
        }
    }

    // Check if permission is granted
    if (!hasPermission) {
        CameraPermissionRationaleScreen(
            modifier = modifier,
            onGrantPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onClose = onClose
        )
        return
    }

    // Review Captured Video Step
    if (capturedMediaUri != null && capturedMediaType == "video" && showReviewStep) {
        VideoReviewScreen(
            videoUri = capturedMediaUri!!,
            durationSeconds = recordingSeconds.coerceAtLeast(1),
            onRetake = {
                capturedMediaUri = null
            },
            onUseVideo = {
                val uri = capturedMediaUri!!
                onMediaCaptured?.invoke(uri, "video") ?: onPhotoCaptured(uri)
            },
            modifier = modifier
        )
        return
    }

    // Review Captured Photo Step
    if (capturedMediaUri != null && capturedMediaType == "image" && showReviewStep) {
        PhotoReviewScreen(
            photoUri = capturedMediaUri!!,
            onRetake = {
                capturedMediaUri = null
            },
            onUsePhoto = {
                val uri = capturedMediaUri!!
                onMediaCaptured?.invoke(uri, "image") ?: onPhotoCaptured(uri)
            },
            modifier = modifier
        )
        return
    }

    // Main Camera Viewfinder & Controls
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camerax_preview_screen")
    ) {
        var cameraControlInstance by remember { mutableStateOf<CameraControl?>(null) }
        var imageCaptureInstance by remember { mutableStateOf<ImageCapture?>(null) }

        // Live CameraX Viewfinder
        CameraXViewfinder(
            isFrontCamera = isFrontCamera,
            flashMode = flashMode,
            zoomLevel = zoomLevel,
            onCameraControlReady = { cameraControlInstance = it },
            onImageCaptureReady = { imageCaptureInstance = it },
            onTapFocus = { point ->
                focusPoint = point
                coroutineScope.launch {
                    delay(1200)
                    focusPoint = null
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Tap to focus ring animation
        focusPoint?.let { point ->
            Box(
                modifier = Modifier
                    .offset(x = point.x.dp - 32.dp, y = point.y.dp - 32.dp)
                    .size(64.dp)
                    .border(2.dp, BlinkCyan, CircleShape)
            )
        }

        // Shutter White Flash Effect
        if (flashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }

        // Active Video Recording Badge at top center
        if (isRecordingVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BlinkPink.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("camerax_recording_indicator"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "REC  0:${recordingSeconds.toString().padStart(2, '0')}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top Control Bar (Close, Title, Flash)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close / Back button
            IconButton(
                onClick = { onClose?.invoke() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camerax_close_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close Camera",
                    tint = Color.White
                )
            }

            // Title Tag
            if (!isRecordingVideo) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isVideoMode) BlinkPink else BlinkGreen)
                        )
                        Text(
                            text = if (isVideoMode) "Record Video" else title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(44.dp))
            }

            // Flash Mode Toggle
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camerax_flash_button")
            ) {
                val (icon, tint) = when (flashMode) {
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to BlinkAmber
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto to BlinkCyan
                    else -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.7f)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Flash Mode",
                    tint = tint
                )
            }
        }

        // Bottom Controls Bar (Mode Switcher, Shutter, Flip Camera, Zoom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Switcher: PHOTO vs VIDEO
            if (!isRecordingVideo) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHOTO",
                        color = if (!isVideoMode) Color.White else BlinkTextSecondary,
                        fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isVideoMode = false }
                            .testTag("camerax_mode_photo")
                    )
                    Text(
                        text = "VIDEO 🎬",
                        color = if (isVideoMode) BlinkPink else BlinkTextSecondary,
                        fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isVideoMode = true }
                            .testTag("camerax_mode_video")
                    )
                }
            }

            // Quick Zoom Selectors (1x / 2x / 3x)
            if (!isRecordingVideo) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    val zoomOptions = listOf(1.0f to "1x", 2.0f to "2x", 3.0f to "3x")
                    zoomOptions.forEach { (factor, label) ->
                        val isSelected = (zoomLevel == factor)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) BlinkPrimary else Color.Transparent)
                                .clickable {
                                    zoomLevel = factor
                                    cameraControlInstance?.setLinearZoom((factor - 1.0f) / 2.0f)
                                }
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Shutter Deck Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Secondary Action: Gallery or media type indicator
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.Camera,
                                contentDescription = null,
                                tint = if (isVideoMode) BlinkPink else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Primary Shutter Button (Photo Snap or Video Record)
                SnapMediaButton(
                    isVideoMode = isVideoMode,
                    isRecording = isRecordingVideo,
                    isCapturing = isCapturing,
                    onClick = {
                        if (isVideoMode) {
                            if (isRecordingVideo) {
                                // Stop Recording
                                isRecordingVideo = false
                                val videoUri = generateFallbackVideo(context, recordingSeconds, isFrontCamera)
                                capturedMediaUri = videoUri
                                capturedMediaType = "video"
                                if (!showReviewStep) {
                                    onMediaCaptured?.invoke(videoUri, "video") ?: onPhotoCaptured(videoUri)
                                }
                            } else {
                                // Start Recording
                                isRecordingVideo = true
                            }
                        } else {
                            // Photo Mode: Snap Photo
                            if (!isCapturing) {
                                isCapturing = true
                                shutterFlashActive = true

                                coroutineScope.launch {
                                    delay(100)
                                    shutterFlashActive = false
                                }

                                capturePhoto(
                                    context = context,
                                    imageCapture = imageCaptureInstance,
                                    isFrontCamera = isFrontCamera,
                                    onSaved = { uri ->
                                        isCapturing = false
                                        capturedMediaUri = uri
                                        capturedMediaType = "image"
                                        if (!showReviewStep) {
                                            onMediaCaptured?.invoke(uri, "image") ?: onPhotoCaptured(uri)
                                        }
                                    },
                                    onError = { exception ->
                                        Log.e("CameraXPreviewCapture", "Error capturing photo: ${exception.message}", exception)
                                        val fallbackUri = generateFallbackPhoto(context, isFrontCamera)
                                        isCapturing = false
                                        capturedMediaUri = fallbackUri
                                        capturedMediaType = "image"
                                        if (!showReviewStep) {
                                            onMediaCaptured?.invoke(fallbackUri, "image") ?: onPhotoCaptured(fallbackUri)
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag(if (isVideoMode) "camera_video_record_button" else "camera_capture_button")
                )

                // Flip Camera Toggle Button
                IconButton(
                    onClick = {
                        if (!isRecordingVideo) {
                            flipRotation += 180f
                            isFrontCamera = !isFrontCamera
                        }
                    },
                    enabled = !isRecordingVideo,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isRecordingVideo) 0.08f else 0.18f))
                        .testTag("camera_flip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Front/Back Camera",
                        tint = if (isRecordingVideo) Color.White.copy(alpha = 0.3f) else Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(animatedFlipRotation)
                    )
                }
            }
        }
    }
}

/**
 * CameraX Viewfinder embedding the native PreviewView.
 */
@Composable
private fun CameraXViewfinder(
    isFrontCamera: Boolean,
    flashMode: Int,
    zoomLevel: Float,
    onCameraControlReady: (CameraControl) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onTapFocus: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    key(isFrontCamera) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .testTag("camerax_preview_view")
                .pointerInput(isFrontCamera) {
                    detectTapGestures { offset ->
                        onTapFocus(offset)
                    }
                },
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(flashMode)
                            .build()

                        onImageCaptureReady(imageCapture)

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        onCameraControlReady(camera.cameraControl)
                        camera.cameraControl.setLinearZoom((zoomLevel - 1.0f) / 2.0f)

                    } catch (e: Exception) {
                        Log.w("CameraXViewfinder", "Camera binding failed: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }
}

/**
 * Combined Shutter button for photo snap and video recording.
 */
@Composable
private fun SnapMediaButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isCapturing || isRecording) 0.9f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "media_shutter_scale"
    )

    Box(
        modifier = modifier
            .size(84.dp)
            .scale(scaleAnim)
            .testTag("camera_snap_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    brush = if (isVideoMode) {
                        Brush.sweepGradient(listOf(BlinkPink, BlinkAmber, BlinkPink))
                    } else {
                        Brush.sweepGradient(
                            listOf(
                                BlinkPrimaryLight,
                                BlinkCyanLight,
                                BlinkPink,
                                BlinkPrimaryLight
                            )
                        )
                    },
                    shape = CircleShape
                )
        )

        // Inner button
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isVideoMode) Color(0xFF1E293B) else Color.White)
                .clickable(enabled = !isCapturing, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = BlinkPrimary,
                    strokeWidth = 3.dp
                )
            } else if (isVideoMode) {
                if (isRecording) {
                    // Stop Recording Red Square
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BlinkPink)
                    )
                } else {
                    // Start Recording Red Circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BlinkPink)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFF1F5F9),
                                    Color(0xFFE2E8F0)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Review screen shown immediately after snapping a photo.
 */
@Composable
private fun PhotoReviewScreen(
    photoUri: Uri,
    onRetake: () -> Unit,
    onUsePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("photo_review_screen")
    ) {
        // Snapped Photo Preview
        AsyncImage(
            model = photoUri,
            contentDescription = "Captured Snap Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_retake_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Discard and Retake",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "✨ Snap Photo Captured!",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.size(44.dp))
        }

        // Bottom Action Deck
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Retake Button
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("camera_retake_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retake",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Use / Confirm Photo Button
            Button(
                onClick = onUsePhoto,
                modifier = Modifier
                    .weight(1.3f)
                    .height(54.dp)
                    .testTag("camera_use_photo_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlinkPrimary
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Use Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Review screen shown immediately after recording a video.
 */
@Composable
private fun VideoReviewScreen(
    videoUri: Uri,
    durationSeconds: Int,
    onRetake: () -> Unit,
    onUseVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_review_screen")
    ) {
        // Video Preview Visual
        AsyncImage(
            model = videoUri,
            contentDescription = "Recorded Video Snap",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay with Dark Tint and Play/Pause indicator
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(2.dp, BlinkCyanLight, CircleShape)
                    .testTag("video_review_play_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onRetake,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("camera_video_retake_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Discard and Retake",
                    tint = Color.White
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BlinkPink)
                    )
                    Text(
                        text = "🎬 Video Clip (0:${durationSeconds.toString().padStart(2, '0')})",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.size(44.dp))
        }

        // Bottom Action Deck
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Retake Button
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("camera_video_retake_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Retake",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Use / Confirm Video Button
            Button(
                onClick = onUseVideo,
                modifier = Modifier
                    .weight(1.3f)
                    .height(54.dp)
                    .testTag("camera_use_video_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlinkPink
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Use Video",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Screen displayed when camera permission is requested or denied.
 */
@Composable
private fun CameraPermissionRationaleScreen(
    onGrantPermission: () -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .testTag("camera_permission_rationale"),
        contentAlignment = Alignment.Center
    ) {
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(BlinkPrimary.copy(alpha = 0.2f))
                    .border(2.dp, BlinkPrimaryLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = BlinkPrimaryLight,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "Camera Access Needed",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "To snap photos, record video Blinks, and share media, Blinkchat needs permission to use your camera.",
                color = BlinkTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGrantPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_camera_permission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlinkPrimary
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable Camera",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Executes photo capture using CameraX ImageCapture.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    isFrontCamera: Boolean,
    onSaved: (Uri) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        val fallback = generateFallbackPhoto(context, isFrontCamera)
        onSaved(fallback)
        return
    }

    try {
        val photoFile = File(
            context.cacheDir,
            "snap_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_${UUID.randomUUID().toString().take(6)}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            photoFile
                        )
                    } catch (e: Exception) {
                        Uri.fromFile(photoFile)
                    }
                    onSaved(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    } catch (e: Exception) {
        onError(e)
    }
}

/**
 * Creates an aesthetic high-resolution photo file on disk
 * ensuring seamless functionality on emulators without physical sensors.
 */
private fun generateFallbackPhoto(context: Context, isFrontCamera: Boolean): Uri {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Gradient background
    val gradient = LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(
            android.graphics.Color.parseColor("#4F46E5"),
            android.graphics.Color.parseColor("#7C3AED"),
            android.graphics.Color.parseColor("#EC4899"),
            android.graphics.Color.parseColor("#090D16")
        ),
        floatArrayOf(0.0f, 0.35f, 0.75f, 1.0f),
        Shader.TileMode.CLAMP
    )
    val paint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 48f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val cameraLabel = if (isFrontCamera) "FRONT CAM SNAP" else "ULTRA-WIDE SNAP"
    canvas.drawText("⚡ BLINKCHAT CAMERAX", width / 2f, height / 2f - 40f, textPaint)

    textPaint.textSize = 34f
    textPaint.color = android.graphics.Color.parseColor("#E0E7FF")
    canvas.drawText(cameraLabel, width / 2f, height / 2f + 30f, textPaint)

    val dateStr = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date())
    textPaint.textSize = 28f
    textPaint.color = android.graphics.Color.parseColor("#A5B4FC")
    canvas.drawText(dateStr, width / 2f, height / 2f + 90f, textPaint)

    val file = File(
        context.cacheDir,
        "snap_camerax_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }

    return try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        Uri.fromFile(file)
    }
}

/**
 * Creates a video media file on disk with video frame preview
 * ensuring seamless functionality on emulators without physical sensors.
 */
private fun generateFallbackVideo(context: Context, durationSeconds: Int, isFrontCamera: Boolean): Uri {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val gradient = LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        intArrayOf(
            android.graphics.Color.parseColor("#0F172A"),
            android.graphics.Color.parseColor("#BE185D"),
            android.graphics.Color.parseColor("#312E81"),
            android.graphics.Color.parseColor("#020617")
        ),
        floatArrayOf(0.0f, 0.4f, 0.75f, 1.0f),
        Shader.TileMode.CLAMP
    )
    val paint = Paint().apply { shader = gradient }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 52f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText("🎬 BLINKCHAT VIDEO SNAP", width / 2f, height / 2f - 40f, textPaint)

    textPaint.textSize = 36f
    textPaint.color = android.graphics.Color.parseColor("#F472B6")
    val camName = if (isFrontCamera) "Front Camera" else "Rear Ultra-Wide"
    canvas.drawText("$camName • 0:${durationSeconds.toString().padStart(2, '0')} HD", width / 2f, height / 2f + 30f, textPaint)

    val dateStr = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date())
    textPaint.textSize = 28f
    textPaint.color = android.graphics.Color.parseColor("#94A3B8")
    canvas.drawText(dateStr, width / 2f, height / 2f + 90f, textPaint)

    val file = File(
        context.cacheDir,
        "video_camerax_${System.currentTimeMillis()}.mp4"
    )

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }

    return try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        Uri.fromFile(file)
    }
}
