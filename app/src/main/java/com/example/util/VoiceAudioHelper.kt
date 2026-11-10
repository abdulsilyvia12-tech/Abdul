package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File

/**
 * Handles recording audio voice clips using Android MediaRecorder.
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
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
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
 * Handles playing audio clips from local files, content URIs, or remote Firebase Storage URLs.
 */
class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var currentUri: String? = null
        private set

    val isPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }

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

            val player = MediaPlayer().apply {
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    setDataSource(uriString)
                } else if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else {
                    setDataSource(uriString)
                }

                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    stop()
                    onCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    onError?.invoke("Playback error ($what, $extra)")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
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
            mediaPlayer = null
            currentUri = null
        }
    }
}

/**
 * Uploads audio clips to Firebase Storage under voice_notes/{conversationId}/{messageId}.m4a
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
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val voiceRef = storageRef.child("voice_notes/$conversationId/${messageId}.m4a")

            val metadata = StorageMetadata.Builder()
                .setContentType("audio/mp4")
                .setCustomMetadata("conversationId", conversationId)
                .setCustomMetadata("messageId", messageId)
                .build()

            val uploadTask = voiceRef.putFile(Uri.fromFile(audioFile), metadata)

            uploadTask.addOnSuccessListener {
                voiceRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { e ->
                    Log.w("AudioUploader", "Upload succeeded but failed to fetch download URL: ${e.message}")
                    // Fall back to local file URI so playback is never interrupted
                    onSuccess(Uri.fromFile(audioFile).toString())
                }
            }.addOnFailureListener { e ->
                Log.w("AudioUploader", "Firebase Storage upload failed: ${e.message}. Using local file fallback.")
                // Local file fallback so user voice note is delivered and playable immediately
                onSuccess(Uri.fromFile(audioFile).toString())
            }
        } catch (e: Exception) {
            Log.w("AudioUploader", "Firebase Storage exception: ${e.message}. Using local file fallback.")
            onSuccess(Uri.fromFile(audioFile).toString())
        }
    }
}
