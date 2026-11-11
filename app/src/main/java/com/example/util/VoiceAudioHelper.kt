package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File

/**
 * Handles recording audio voice clips using Android MediaRecorder with optimized AAC compression.
 */
class VoiceRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording: Boolean = false
        private set

    fun startRecording(outputFile: File): Boolean {
        try {
            stopRecordingInternal(deleteFile = true)
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(24000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            return true
        } catch (e: Exception) {
            Log.e("VoiceRecorderManager", "Failed to start recording: ${e.message}", e)
            stopRecordingInternal(deleteFile = true)
            return false
        }
    }

    fun stopRecording(): Pair<File?, Long> {
        val file = currentOutputFile
        val duration = System.currentTimeMillis() - recordingStartTime
        stopRecordingInternal(deleteFile = false)
        return Pair(file, duration.coerceAtLeast(0L))
    }

    fun cancelRecording() {
        stopRecordingInternal(deleteFile = true)
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun stopRecordingInternal(deleteFile: Boolean) {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w("VoiceRecorderManager", "Error stopping recorder: ${e.message}")
                    }
                    release()
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceRecorderManager", "Error releasing recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
            if (deleteFile && currentOutputFile != null) {
                try {
                    currentOutputFile?.delete()
                } catch (e: Exception) {
                    // Ignore
                }
                currentOutputFile = null
            }
        }
    }
}

/**
 * Handles playing audio clips from local files, content URIs, Base64 data URIs, or remote URLs.
 */
class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var currentUri: String? = null
        private set
    var isPreparing: Boolean = false
        private set

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

    val isPlayingOrPreparing: Boolean
        get() = isPreparing || isPlaying

    val currentPosition: Int
        get() = try {
            mediaPlayer?.currentPosition ?: 0
        } catch (e: Exception) {
            0
        }

    val duration: Int
        get() = try {
            mediaPlayer?.duration ?: 0
        } catch (e: Exception) {
            0
        }

    fun play(
        uriString: String,
        onProgress: ((currentMs: Int, totalMs: Int) -> Unit)? = null,
        onCompletion: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            stop()
            currentUri = uriString
            isPreparing = true

            // Resolve actual playable path: decode Base64 data URI if needed
            val resolvedPath: String = if (uriString.startsWith("data:audio")) {
                try {
                    val base64Data = uriString.substringAfter("base64,")
                    val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val cacheFile = File(context.cacheDir, "voice_cache_${uriString.hashCode().toString().replace("-", "n")}.m4a")
                    if (!cacheFile.exists() || cacheFile.length() != audioBytes.size.toLong()) {
                        cacheFile.writeBytes(audioBytes)
                    }
                    cacheFile.absolutePath
                } catch (e: Exception) {
                    Log.w("AudioPlayerManager", "Failed to decode base64 audio data URI: ${e.message}")
                    uriString
                }
            } else if (uriString.startsWith("file://")) {
                Uri.parse(uriString).path ?: uriString
            } else {
                uriString
            }

            val player = MediaPlayer().apply {
                if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
                    setDataSource(resolvedPath)
                } else if (resolvedPath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(resolvedPath))
                } else {
                    setDataSource(resolvedPath)
                }

                setOnPreparedListener { mp ->
                    isPreparing = false
                    mp.start()
                }
                setOnCompletionListener {
                    isPreparing = false
                    stop()
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    isPreparing = false
                    stop()
                    onError?.invoke("Playback error ($what, $extra)")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            isPreparing = false
            Log.e("AudioPlayerManager", "Failed to start audio playback: ${e.message}", e)
            stop()
            onError?.invoke(e.message ?: "Failed to play audio")
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error pausing: ${e.message}")
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !isPlaying) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error resuming: ${e.message}")
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error seeking: ${e.message}")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.w("AudioPlayerManager", "Error stopping player: ${e.message}")
        } finally {
            isPreparing = false
            mediaPlayer = null
            currentUri = null
        }
    }
}

/**
 * Handles uploading or encoding audio clips for Firebase storage and Firestore synchronization.
 * Uses Base64 Data URI storage when unauthenticated or when Cloud Storage is unprovisioned,
 * preventing FirebaseNoSignedInUserException and HTTP 404 StorageException errors.
 */
object FirebaseStorageAudioUploader {

    fun uploadVoiceClip(
        audioFile: File,
        conversationId: String,
        messageId: String,
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // 1. Prepare Base64 Data URI for reliable Firebase Firestore delivery
            val bytes = audioFile.readBytes()
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "data:audio/mp4;base64,$base64Data"

            // 2. Check if a real authenticated user exists for Firebase Storage
            val authUser = try {
                FirebaseAuth.getInstance().currentUser
            } catch (e: Exception) {
                null
            }

            // If user is not authenticated with Firebase Auth, do NOT attempt FirebaseStorage
            // to avoid FirebaseNoSignedInUserException. Directly return dataUri which syncs to Firestore.
            if (authUser == null) {
                onSuccess(dataUri)
                return
            }

            // 3. If authenticated, try Firebase Storage with fallback
            try {
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference
                val voiceRef = storageRef.child("voice_notes/$conversationId/${messageId}.m4a")

                val metadata = StorageMetadata.Builder()
                    .setContentType("audio/mp4")
                    .setCustomMetadata("conversationId", conversationId)
                    .setCustomMetadata("messageId", messageId)
                    .build()

                voiceRef.putFile(Uri.fromFile(audioFile), metadata)
                    .addOnSuccessListener {
                        voiceRef.downloadUrl.addOnSuccessListener { uri ->
                            onSuccess(uri.toString())
                        }.addOnFailureListener {
                            onSuccess(dataUri)
                        }
                    }
                    .addOnFailureListener {
                        onSuccess(dataUri)
                    }
            } catch (e: Exception) {
                onSuccess(dataUri)
            }
        } catch (e: Exception) {
            Log.w("AudioUploader", "Fallback to local file URI: ${e.message}")
            onSuccess(Uri.fromFile(audioFile).toString())
        }
    }
}
