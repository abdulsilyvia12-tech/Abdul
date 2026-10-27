package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.sin

data class MusicTrackInfo(
    val title: String,
    val artist: String,
    val genre: String, // "Bongo & Afro", "Dramatic & Epic", "Funny & Comedy", "Electronic & EDM", "Lofi & Chill"
    val durationSeconds: Int = 30,
    val iconEmoji: String = "🎵",
    val description: String = ""
)

/**
 * Singleton Music & Beat Synthesizer for Blinkchat.
 * Generates continuous smooth musical soundscapes (Bongo Flava, Afrobeats, Dramatic/Cinematic,
 * Funny/Comedy ragtime, Synthwave EDM, Lofi chill) using Android's native AudioTrack.
 */
object BlinkMusicPlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow("🌴 Bongo Flava Vibe - Dar es Salaam All-Stars")
    val currentTrack: StateFlow<String> = _currentTrack.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val sampleRate = 22050

    val availableTracks = listOf(
        // Bongo Flava & Afrobeats
        MusicTrackInfo("🌴 Bongo Flava Vibe - Dar es Salaam All-Stars", "Diamond & Harmonize Style", "Bongo & Afro", 30, "🌴", "Authentic Tanzanian acoustic rhythm, vibrant guitars and log drum bounce"),
        MusicTrackInfo("🔥 Afrobeat Fire - Burna Rhythm Express", "Lagos Groove Masters", "Bongo & Afro", 28, "🔥", "High-energy polyrhythmic Afro-fusion percussion with brass brass hits"),
        MusicTrackInfo("🥁 Amapiano Sunrise - Deep Log Groove", "Soweto Bass Collective", "Bongo & Afro", 32, "🥁", "Deep rolling log drums, lush Rhodes chords, and hypnotic shakers"),
        MusicTrackInfo("🌊 Zanzibar Sunset - Island Bongo Groove", "Swahili Coast Band", "Bongo & Afro", 25, "🌊", "Gentle ocean breeze guitars, acoustic congas, and coastal vibes"),

        // Dramatic & Cinematic
        MusicTrackInfo("🎭 Dramatic Requiem - Dark Epic Orchestra", "Hans Cinematic", "Dramatic & Epic", 30, "🎭", "Intense low cello drones, pounding war drums, and suspenseful cliffhangers"),
        MusicTrackInfo("⚡ Hans Zimmer Suspense - Time Ticking", "Cinematic Studio London", "Dramatic & Epic", 28, "⚡", "Sub-bass tension pulse, ominous rising strings, and epic climax"),
        MusicTrackInfo("🌩️ Thunderstorm Fury - Cinema Apex", "Apex Film Scores", "Dramatic & Epic", 32, "🌩️", "Climactic orchestral brass swell for peak emotional twists"),
        MusicTrackInfo("🌑 Midnight Betrayal - Moody Noir Thriller", "Shadow Noir Ensemble", "Dramatic & Epic", 26, "🌑", "Eerie deep bass drone and melancholic solo violin"),

        // Funny & Comedy
        MusicTrackInfo("🎪 Circus Ragtime Bounce - Comedy Beats", "The Goofy Orchestra", "Funny & Comedy", 22, "🎪", "Playful jumping ragtime piano, clown whistles, and cartoon bounce"),
        MusicTrackInfo("🍌 Banana Slip Polka - Cartoon Whistle", "Slapstick Brothers", "Funny & Comedy", 20, "🍌", "Hilarious comedic wobble and bouncy cartoon fail soundscapes"),
        MusicTrackInfo("🤹 Squeaky Toy March - Funny Animals", "Pet Comedy Club", "Funny & Comedy", 18, "🤹", "Silly bouncy tempo with squeaks, perfect for funny dog & cat moments"),
        MusicTrackInfo("🎺 Kazoo Party Disaster - Comedy Gold", "Whistle & Kazoo Band", "Funny & Comedy", 24, "🎺", "Goofy upbeat party chaos and kazoo chorus for viral funny fails"),
        MusicTrackInfo("🦆 The Quack Attack - Silly Goose Shuffle", "Barnyard Funk", "Funny & Comedy", 20, "🦆", "Waddling comedic bassline and quirky slapstick soundbites"),

        // Electronic & Synthwave
        MusicTrackInfo("⚡ Tokyo Drift Synthwave - Kai Storm", "Kai Storm", "Electronic & EDM", 25, "⚡", "Pumping 130BPM retro-futuristic bassline and glittering neon synths"),
        MusicTrackInfo("💾 1999 Tape Saturation - Retro Wave", "Synth Boy 99", "Electronic & EDM", 24, "💾", "Warm analog VHS compression, punchy 80s drums, and cyber keys"),
        MusicTrackInfo("💖 Hyperpop Sparkle - Kawaii Glitch", "Neon Starlight", "Electronic & EDM", 22, "💖", "High-tempo sparkly bubblegum glitch pop and bouncy 808s"),

        // Lofi & Chill
        MusicTrackInfo("🌌 Cosmic Pulsar - Lofi Starlight", "Elena Vance", "Lofi & Chill", 30, "🌌", "Soothing relaxing Rhodes piano, soft vinyl crackle, and cosmic chill"),
        MusicTrackInfo("🎸 Golden Coast Sunset - Acoustic Jam", "Alex Rivers", "Lofi & Chill", 26, "🎸", "Warm acoustic guitar fingerpicking and gentle seaside ambient breeze")
    )

    fun playTrack(trackTitle: String) {
        val isNewTrack = _currentTrack.value != trackTitle
        _currentTrack.value = trackTitle
        if (!isNewTrack && _isPlaying.value && audioTrack != null) {
            return
        }
        startSynthesizedStream()
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Failed to pause AudioTrack: ${e.message}")
        }
    }

    fun resumePlayback() {
        if (!_isPlaying.value) {
            startSynthesizedStream()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        try {
            audioTrack?.setVolume(if (newMuted) 0f else 0.75f)
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Failed to set volume: ${e.message}")
        }
    }

    /**
     * Play short sound effects (Dramatic sting, Laugh, Airhorn, Squeak)
     */
    fun playSoundEffect(effectName: String) {
        scope.launch {
            val freq = when (effectName.lowercase()) {
                "dramatic", "dun_dun_dun" -> 110.0 // Low dramatic brass sting
                "laugh", "comedy" -> 440.0 // Comedy chord
                "squeak" -> 880.0 // High squeak
                "airhorn" -> 370.0 // Reggae horn
                else -> 523.25
            }
            try {
                val tempTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(2048)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                tempTrack.play()
                val buf = ShortArray(2048)
                for (step in 0 until 4) {
                    for (i in buf.indices) {
                        val t = (step * 2048 + i).toDouble() / sampleRate
                        val sample = (sin(2.0 * PI * freq * t) * (1.0 - t.coerceIn(0.0, 1.0))) * 0.6
                        buf[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                    }
                    tempTrack.write(buf, 0, buf.size)
                }
                delay(400)
                tempTrack.stop()
                tempTrack.release()
            } catch (e: Exception) {
                Log.w("BlinkMusicPlayer", "Effect error: ${e.message}")
            }
        }
    }

    private fun startSynthesizedStream() {
        stopPlayback()
        _isPlaying.value = true

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(if (_isMuted.value) 0f else 0.75f)
            audioTrack?.play()

            val titleLower = _currentTrack.value.lowercase()

            // Musical note collections and rhythm tempos tailored to the genre!
            val (baseNotes, beatTempo, hasSubHarmonics, isStaccato) = when {
                // Bongo Flava & Afrobeats: Syncopated Afro pentatonic (C, D, E, G, A, high C)
                titleLower.contains("bongo") || titleLower.contains("afro") || titleLower.contains("amapiano") || titleLower.contains("zanzibar") -> {
                    Quadruple(
                        doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 392.00, 329.63),
                        3.2, // ~106 bpm polyrhythmic syncopation
                        true,
                        false
                    )
                }
                // Dramatic & Epic: Deep minor suspense progression in D minor (D2, F2, G2, A2, Bb2)
                titleLower.contains("dramatic") || titleLower.contains("requiem") || titleLower.contains("suspense") || titleLower.contains("thunder") || titleLower.contains("betrayal") || titleLower.contains("epic") -> {
                    Quadruple(
                        doubleArrayOf(146.83, 174.61, 196.00, 220.00, 233.08, 196.00, 174.61, 146.83),
                        1.0, // Slow, heavy dramatic 60 bpm pulse
                        true,
                        false
                    )
                }
                // Funny & Comedy: Playful bouncy ragtime arpeggio jumping between high & low octaves
                titleLower.contains("circus") || titleLower.contains("ragtime") || titleLower.contains("banana") || titleLower.contains("squeak") || titleLower.contains("kazoo") || titleLower.contains("quack") || titleLower.contains("funny") || titleLower.contains("comedy") -> {
                    Quadruple(
                        doubleArrayOf(261.63, 523.25, 329.63, 659.25, 392.00, 783.99, 329.63, 523.25),
                        4.5, // Fast staccato bounce ~135 bpm
                        false,
                        true
                    )
                }
                // Electronic & Synthwave: Driving 128 BPM electronic bass
                titleLower.contains("synth") || titleLower.contains("tokyo") || titleLower.contains("tape") || titleLower.contains("sparkle") || titleLower.contains("cyber") -> {
                    Quadruple(
                        doubleArrayOf(220.00, 220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 329.63),
                        4.2, // Pumping 128 bpm dance pulse
                        true,
                        false
                    )
                }
                // Lofi & Chill
                else -> {
                    Quadruple(
                        doubleArrayOf(220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25),
                        2.0, // Chill 75 bpm slow wave
                        true,
                        false
                    )
                }
            }

            playbackJob = scope.launch {
                val buffer = ShortArray(1024)
                var step = 0
                var noteIndex = 0

                while (isActive && _isPlaying.value) {
                    val currentFreq = baseNotes[noteIndex % baseNotes.size]
                    val bassFreq = currentFreq / 2.0

                    for (i in buffer.indices) {
                        val t = (step + i).toDouble() / sampleRate
                        val melody = sin(2.0 * PI * currentFreq * t) * 0.40
                        val bass = if (hasSubHarmonics) sin(2.0 * PI * bassFreq * t) * 0.35 else 0.0
                        val harmonic = sin(2.0 * PI * (currentFreq * 1.5) * t) * 0.15

                        // Rhythmic beat modulation envelope
                        val beatMod = if (isStaccato) {
                            val cycle = (t * beatTempo) % 1.0
                            if (cycle < 0.6) 1.0 else 0.15
                        } else {
                            0.75 + 0.25 * sin(2.0 * PI * beatTempo * t)
                        }

                        val combined = (melody + bass + harmonic) * beatMod
                        buffer[i] = (combined * Short.MAX_VALUE * 0.42).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    step += buffer.size
                    val noteDurationSamples = (sampleRate / beatTempo).toInt().coerceAtLeast(1024)
                    if (step % noteDurationSamples < buffer.size) {
                        noteIndex++
                    }

                    try {
                        audioTrack?.write(buffer, 0, buffer.size)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Failed to start AudioTrack: ${e.message}")
            _isPlaying.value = false
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Error stopping: ${e.message}")
        }
        audioTrack = null
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
