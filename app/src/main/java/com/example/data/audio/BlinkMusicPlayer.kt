package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.PI
import kotlin.math.sin

data class MusicTrackInfo(
    val title: String,
    val artist: String,
    val genre: String, // "Pop Hits", "Ballads & Acoustic", "R&B & Soul", "Rock & Indie", "Hip-Hop & Rap"
    val durationSeconds: Int = 30,
    val iconEmoji: String = "🎤",
    val description: String = "",
    val previewUrl: String? = null,
    val artworkUrl: String? = null
)

/**
 * Singleton Music & Audio Engine for Blinkchat.
 * Features:
 * 1. Online iTunes Search: search and fetch ANY song from ANY artist worldwide with 30s audio previews.
 * 2. Real audio streaming with Android's MediaPlayer for official artist previews.
 * 3. Offline pop/ballad/R&B synthesized chord engine fallback (via AudioTrack) so music is always audible.
 * 4. 25+ famous chart-topping songs from singers & artists (Taylor Swift, The Weeknd, Adele, Billie Eilish, Bruno Mars, etc.).
 */
object BlinkMusicPlayer {
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow("Cruel Summer - Taylor Swift")
    val currentTrack: StateFlow<String> = _currentTrack.asStateFlow()

    private val _currentArtworkUrl = MutableStateFlow<String?>(null)
    val currentArtworkUrl: StateFlow<String?> = _currentArtworkUrl.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var _musicVolume = 0.85f
    val musicVolume: Float get() = _musicVolume

    fun setMusicVolume(vol: Float) {
        _musicVolume = vol.coerceIn(0f, 1f)
        try {
            val eff = if (_isMuted.value) 0f else _musicVolume
            mediaPlayer?.setVolume(eff, eff)
            audioTrack?.setVolume(eff)
        } catch (_: Exception) {}
    }

    // 6 Trending TikTok Sounds (Stored and curated for video timeline sync)
    val trendingTikTokSounds = listOf(
        MusicTrackInfo("Tokyo Drift Phonk 🏎️", "Drift Beast", "Trending", 30, "🏎️", "Viral TikTok Phonk beat with heavy 808 bass & drift cowbell • 1.4M videos"),
        MusicTrackInfo("Makeba Dance Beat ✨", "Jain (Remix)", "Trending", 30, "✨", "Infectious viral dance beat taking over Reels & TikTok • 3.2M videos"),
        MusicTrackInfo("Cruel Summer Bridge 💖", "Taylor Swift", "Trending", 30, "💖", "The iconic screaming bridge anthem • 2.9M videos"),
        MusicTrackInfo("Cupid Twin Ver. 🌸", "FIFTY FIFTY", "Trending", 30, "🌸", "Sweet dreamy melody with viral acoustic pop groove • 4.1M videos"),
        MusicTrackInfo("Espresso Catchy Rhythm ☕", "Sabrina Carpenter", "Trending", 30, "☕", "Chart-topping upbeat dance-pop viral sensation • 2.6M videos"),
        MusicTrackInfo("GigaChad Phonk Anthem 🗿", "g3ox_em", "Trending", 30, "🗿", "Ultimate high-intensity workout & meme phonk sound • 1.8M videos")
    )

    private val sampleRate = 22050

    val availableTracks = listOf(
        // Pop Hits
        MusicTrackInfo("Cruel Summer - Taylor Swift", "Taylor Swift", "Pop Hits", 30, "💖", "Global #1 pop anthem with synth-pop groove", artworkUrl = null),
        MusicTrackInfo("Blinding Lights - The Weeknd", "The Weeknd", "Pop Hits", 30, "✨", "High-energy 80s synth-pop phenomenon with neon rhythm", artworkUrl = null),
        MusicTrackInfo("As It Was - Harry Styles", "Harry Styles", "Pop Hits", 30, "🌟", "Infectious upbeat indie-pop melody and vintage bells", artworkUrl = null),
        MusicTrackInfo("Flowers - Miley Cyrus", "Miley Cyrus", "Pop Hits", 30, "🌸", "Self-empowerment disco-pop smash hit with punchy bass", artworkUrl = null),
        MusicTrackInfo("Levitating - Dua Lipa", "Dua Lipa", "Pop Hits", 30, "🪩", "Futuristic nu-disco bounce with irresistible dance groove", artworkUrl = null),
        MusicTrackInfo("Shape of You - Ed Sheeran", "Ed Sheeran", "Pop Hits", 30, "🎸", "Iconic acoustic marimba hook and chart-topping singer-songwriter melody", artworkUrl = null),
        MusicTrackInfo("Vampire - Olivia Rodrigo", "Olivia Rodrigo", "Pop Hits", 30, "🩸", "Dramatic piano ballad building into an explosive pop-rock crescendo", artworkUrl = null),
        MusicTrackInfo("Bad Guy - Billie Eilish", "Billie Eilish", "Pop Hits", 30, "🖤", "Minimalist bass groove, whispered vocals, and viral pop beat", artworkUrl = null),
        MusicTrackInfo("Save Your Tears - The Weeknd", "The Weeknd", "Pop Hits", 30, "🕶️", "Melodic synth-wave pop gem with infectious chorus", artworkUrl = null),
        MusicTrackInfo("Anti-Hero - Taylor Swift", "Taylor Swift", "Pop Hits", 30, "🐱", "Midnights synth-pop anthem with introspective pop melody", artworkUrl = null),

        // Ballads & Acoustic
        MusicTrackInfo("Easy On Me - Adele", "Adele", "Ballads & Acoustic", 30, "🎹", "Emotional soulful piano ballad with powerful soaring vocals", artworkUrl = null),
        MusicTrackInfo("Someone Like You - Adele", "Adele", "Ballads & Acoustic", 30, "💔", "Timeless grand piano ballad and heartfelt vocals", artworkUrl = null),
        MusicTrackInfo("All of Me - John Legend", "John Legend", "Ballads & Acoustic", 30, "🎹", "Tender romantic piano love ballad with rich soulful chords", artworkUrl = null),
        MusicTrackInfo("Perfect - Ed Sheeran", "Ed Sheeran", "Ballads & Acoustic", 30, "💍", "Romantic acoustic guitar waltz and sweet acoustic strings", artworkUrl = null),
        MusicTrackInfo("Stay With Me - Sam Smith", "Sam Smith", "Ballads & Acoustic", 30, "🕊️", "Gospel-tinged soulful ballad with gentle organ & choir", artworkUrl = null),
        MusicTrackInfo("Until I Found You - Stephen Sanchez", "Stephen Sanchez", "Ballads & Acoustic", 30, "🎙️", "Vintage 50s doo-wop romantic crooner ballad", artworkUrl = null),
        MusicTrackInfo("Drivers License - Olivia Rodrigo", "Olivia Rodrigo", "Ballads & Acoustic", 30, "🚗", "Heart-wrenching piano ballad with cinematic emotional bridge", artworkUrl = null),

        // R&B & Soul
        MusicTrackInfo("Kill Bill - SZA", "SZA", "R&B & Soul", 30, "🗡️", "Smooth neo-soul groove with catchy guitar lick and modern R&B drums", artworkUrl = null),
        MusicTrackInfo("Die For You - The Weeknd", "The Weeknd", "R&B & Soul", 30, "❤️‍🔥", "Passionate R&B slow jam with lush synthesizers and falsetto", artworkUrl = null),
        MusicTrackInfo("Leave The Door Open - Bruno Mars", "Bruno Mars & Silk Sonic", "R&B & Soul", 30, "🚪", "Velvety 70s Motown soul with buttery harmonies and brass", artworkUrl = null),
        MusicTrackInfo("24K Magic - Bruno Mars", "Bruno Mars", "R&B & Soul", 30, "✨", "Funky throwback synth-funk with talkbox and gold-standard bounce", artworkUrl = null),
        MusicTrackInfo("Cuff It - Beyoncé", "Beyoncé", "R&B & Soul", 30, "👑", "Uptempo disco-funk R&B with infectious Nile Rodgers guitar groove", artworkUrl = null),
        MusicTrackInfo("Diamonds - Rihanna", "Rihanna", "R&B & Soul", 30, "💎", "Mid-tempo electro-R&B anthem with radiant vocal melodies", artworkUrl = null),

        // Rock & Indie
        MusicTrackInfo("Viva La Vida - Coldplay", "Coldplay", "Rock & Indie", 30, "👑", "Triumphant orchestral strings, bell chimes, and anthemic rock chorus", artworkUrl = null),
        MusicTrackInfo("Yellow - Coldplay", "Coldplay", "Rock & Indie", 30, "💛", "Iconic acoustic-electric indie rock ballad with sweeping guitars", artworkUrl = null),
        MusicTrackInfo("Radioactive - Imagine Dragons", "Imagine Dragons", "Rock & Indie", 30, "⚡", "Thunderous electronic-rock smash with massive sub-bass hits", artworkUrl = null),
        MusicTrackInfo("Believer - Imagine Dragons", "Imagine Dragons", "Rock & Indie", 30, "🔥", "Percussive rock anthem with hard-hitting rhythm and passionate vocals", artworkUrl = null),
        MusicTrackInfo("Counting Stars - OneRepublic", "OneRepublic", "Rock & Indie", 30, "⭐", "Folk-pop acoustic intro exploding into an exhilarating dance-rock beat", artworkUrl = null),

        // Hip-Hop & Rap
        MusicTrackInfo("God's Plan - Drake", "Drake", "Hip-Hop & Rap", 30, "🦉", "Warm 808 bass, upbeat synth keys, and legendary melodic flow", artworkUrl = null),
        MusicTrackInfo("Sunflower - Post Malone & Swae Lee", "Post Malone & Swae Lee", "Hip-Hop & Rap", 30, "🌻", "Sunny melodic hip-pop masterpiece with breezy synths", artworkUrl = null),
        MusicTrackInfo("Circles - Post Malone", "Post Malone", "Hip-Hop & Rap", 30, "⭕", "Melodic acoustic-pop rap jam with breezy melancholic bassline", artworkUrl = null),
        MusicTrackInfo("HUMBLE. - Kendrick Lamar", "Kendrick Lamar", "Hip-Hop & Rap", 30, "🔥", "Hard-hitting piano riff with menacing 808 trap bounce", artworkUrl = null),

        // Phonk & Drift (🏎️ Heavy 808, Cowbell & High-Octane Bass)
        MusicTrackInfo("Murder In My Mind - KORDHELL", "KORDHELL", "Phonk & Drift", 30, "🏎️", "Iconic distorted drift cowbell rhythm & aggressive 808 bass drop", artworkUrl = null),
        MusicTrackInfo("Close Eyes - DVRST", "DVRST", "Phonk & Drift", 30, "🌌", "Atmospheric viral TikTok drift phonk anthem with dark synth pads", artworkUrl = null),
        MusicTrackInfo("Sahara - Hensonn", "Hensonn", "Phonk & Drift", 30, "🔥", "Hypnotic dark wave phonk with thunderous bass pulse", artworkUrl = null),
        MusicTrackInfo("Montagem Coral - Dj Holanda & Mc Gw", "Dj Holanda & Mc Gw", "Phonk & Drift", 30, "🇧🇷", "High-energy Brazilian funk phonk montage with fast syncopated bounce", artworkUrl = null),
        MusicTrackInfo("Automotivo Bibi Fogosa - Bibi Babydoll", "Bibi Babydoll", "Phonk & Drift", 30, "⚡", "Viral Brazilian drift phonk with distorted bass slides and siren FX", artworkUrl = null),
        MusicTrackInfo("North Memphis - Pharmacist", "Pharmacist", "Phonk & Drift", 30, "📼", "Dirty south Memphis revival phonk with tape saturation and cowbell loop", artworkUrl = null),
        MusicTrackInfo("Phonky Town - Playaphonk", "Playaphonk", "Phonk & Drift", 30, "💨", "Heavy drift phonk anthem with classic lo-fi cassette distortion", artworkUrl = null),
        MusicTrackInfo("METAMORPHOSIS - INTERWORLD", "INTERWORLD", "Phonk & Drift", 30, "🦾", "Aggressive gym phonk anthem with relentless dark bassline", artworkUrl = null),
        MusicTrackInfo("GigaChad Theme (Phonk Remix) - g3ox_em", "g3ox_em", "Phonk & Drift", 30, "🗿", "Viral Chad workout phonk with booming horns and heavy 808 kick", artworkUrl = null),
        MusicTrackInfo("Midnight - PLAYAMANE & Nateki", "PLAYAMANE & Nateki", "Phonk & Drift", 30, "🌃", "Nocturnal Tokyo highway drift phonk with lush synthesizer arpeggios", artworkUrl = null)
    )

    /**
     * Search iTunes API online for ANY song from ANY artist worldwide.
     * Returns rich track info including 30-second official preview streams and album artwork.
     */
    suspend fun searchOnlineItunes(query: String): List<MusicTrackInfo> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encoded&entity=song&limit=25")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Blinkchat/1.0")
            }
            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<MusicTrackInfo>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val trackName = item.optString("trackName", "")
                    val artistName = item.optString("artistName", "")
                    if (trackName.isNotBlank() && artistName.isNotBlank()) {
                        val genre = item.optString("primaryGenreName", "Pop")
                        val preview = item.optString("previewUrl", "").ifBlank { null }
                        val artwork = item.optString("artworkUrl100", "").ifBlank { null }
                        val durationMs = item.optLong("trackTimeMillis", 30000L)
                        list.add(
                            MusicTrackInfo(
                                title = "$trackName - $artistName",
                                artist = artistName,
                                genre = genre,
                                durationSeconds = (durationMs / 1000).toInt().coerceIn(15, 300),
                                iconEmoji = when {
                                    genre.contains("Phonk", true) || genre.contains("Drift", true) -> "🏎️"
                                    genre.contains("Rock", true) -> "🎸"
                                    genre.contains("R&B", true) || genre.contains("Soul", true) -> "💖"
                                    genre.contains("Hip-Hop", true) || genre.contains("Rap", true) -> "🎧"
                                    genre.contains("Country", true) || genre.contains("Folk", true) -> "🤠"
                                    genre.contains("Dance", true) || genre.contains("Electronic", true) -> "⚡"
                                    else -> "🎤"
                                },
                                description = "Popular track by $artistName ($genre)",
                                previewUrl = preview,
                                artworkUrl = artwork
                            )
                        )
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Online iTunes search error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Play a track by title, with optional direct audio stream preview URL.
     */
    fun playTrack(trackTitle: String, previewUrl: String? = null, artworkUrl: String? = null) {
        val isNewTrack = _currentTrack.value != trackTitle
        _currentTrack.value = trackTitle
        if (artworkUrl != null) {
            _currentArtworkUrl.value = artworkUrl
        } else {
            val matchingTrack = availableTracks.firstOrNull { it.title.equals(trackTitle, ignoreCase = true) }
            _currentArtworkUrl.value = matchingTrack?.artworkUrl
        }

        // Determine stream preview URL
        val targetPreview = previewUrl ?: availableTracks.firstOrNull { it.title.equals(trackTitle, ignoreCase = true) }?.previewUrl

        if (!isNewTrack && _isPlaying.value && (mediaPlayer?.isPlaying == true || audioTrack != null)) {
            return
        }

        if (!targetPreview.isNullOrBlank()) {
            startStreamingPlayback(targetPreview)
        } else {
            startSynthesizedStream()
        }
    }

    private fun startStreamingPlayback(streamUrl: String) {
        stopAudioTrack()
        stopMediaPlayer()
        _isPlaying.value = true

        scope.launch(Dispatchers.Main) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(streamUrl)
                    setOnPreparedListener { player ->
                        if (_isPlaying.value) {
                            player.isLooping = true
                            player.setVolume(if (_isMuted.value) 0f else 0.85f, if (_isMuted.value) 0f else 0.85f)
                            player.start()
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w("BlinkMusicPlayer", "MediaPlayer streaming error ($what, $extra), falling back to pop synth")
                        startSynthesizedStream()
                        true
                    }
                }
                mediaPlayer = mp
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.w("BlinkMusicPlayer", "Failed to start streaming preview: ${e.message}")
                startSynthesizedStream()
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Failed to pause MediaPlayer: ${e.message}")
        }
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
            if (mediaPlayer != null) {
                try {
                    _isPlaying.value = true
                    mediaPlayer?.start()
                    return
                } catch (e: Exception) {
                    Log.w("BlinkMusicPlayer", "Failed to resume MediaPlayer: ${e.message}")
                }
            }
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
            val vol = if (newMuted) 0f else 0.85f
            mediaPlayer?.setVolume(vol, vol)
            audioTrack?.setVolume(vol)
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
                "dramatic", "dun_dun_dun" -> 110.0
                "laugh", "comedy" -> 440.0
                "squeak" -> 880.0
                "airhorn" -> 370.0
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
        stopMediaPlayer()
        stopAudioTrack()
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

            // Musical chords tailored to normal songs & artists (Pop, Ballads, R&B, Rock, Hip-Hop)
            val (baseNotes, beatTempo, hasSubHarmonics, isStaccato) = when {
                // Ballads & Acoustic: Slow emotive chords (Adele, Ed Sheeran, John Legend)
                titleLower.contains("adele") || titleLower.contains("ballad") || titleLower.contains("easy on me") || titleLower.contains("someone like you") || titleLower.contains("all of me") || titleLower.contains("perfect") || titleLower.contains("found you") -> {
                    Quadruple(
                        doubleArrayOf(261.63, 329.63, 392.00, 523.25, 440.00, 392.00, 329.63, 261.63), // C major romantic progression
                        1.2, // ~72 bpm gentle piano tempo
                        false,
                        false
                    )
                }
                // R&B & Soul: Neo-soul chord groove (SZA, Bruno Mars, The Weeknd, Beyonce, Rihanna)
                titleLower.contains("sza") || titleLower.contains("bruno") || titleLower.contains("r&b") || titleLower.contains("soul") || titleLower.contains("cuff it") || titleLower.contains("diamonds") || titleLower.contains("kill bill") -> {
                    Quadruple(
                        doubleArrayOf(220.00, 277.18, 329.63, 415.30, 370.00, 329.63, 277.18, 220.00), // A major 7th soul progression
                        2.4, // ~88 bpm R&B swing
                        true,
                        false
                    )
                }
                // Rock & Indie: Driving power rock notes (Coldplay, Imagine Dragons, OneRepublic)
                titleLower.contains("coldplay") || titleLower.contains("dragons") || titleLower.contains("rock") || titleLower.contains("viva") || titleLower.contains("believer") || titleLower.contains("radioactive") -> {
                    Quadruple(
                        doubleArrayOf(146.83, 196.00, 220.00, 261.63, 293.66, 261.63, 220.00, 196.00), // Driving D minor / G rock
                        3.6, // ~130 bpm driving pulse
                        true,
                        true
                    )
                }
                // Hip-Hop & Rap: 808 bass & melodic trap (Drake, Kendrick, Post Malone)
                titleLower.contains("drake") || titleLower.contains("kendrick") || titleLower.contains("post malone") || titleLower.contains("rap") || titleLower.contains("hip-hop") || titleLower.contains("sunflower") || titleLower.contains("humble") -> {
                    Quadruple(
                        doubleArrayOf(110.00, 130.81, 146.83, 164.81, 196.00, 164.81, 146.83, 110.00), // Deep 808 melodic bassline
                        2.8, // ~92 bpm hip-hop groove
                        true,
                        true
                    )
                }
                // Phonk & Drift: High-octane distorted cowbells & heavy 808 sub-bass (Kordhell, DVRST, Hensonn, Pharmacist, Playaphonk)
                titleLower.contains("phonk") || titleLower.contains("drift") || titleLower.contains("kordhell") || titleLower.contains("dvrst") || titleLower.contains("hensonn") || titleLower.contains("interworld") || titleLower.contains("pharmacist") || titleLower.contains("playaphonk") || titleLower.contains("montagem") || titleLower.contains("gigachad") || titleLower.contains("playamane") || titleLower.contains("bibi") -> {
                    Quadruple(
                        doubleArrayOf(622.25, 739.99, 830.61, 932.33, 830.61, 739.99, 622.25, 554.37), // TR-808 cowbell frequencies
                        4.6, // ~138 bpm aggressive drift phonk tempo
                        true, // sub-harmonics enabled
                        true  // staccato clipped attack
                    )
                }
                // Pop Hits: Bright I-V-vi-IV melodic pop chords (Taylor Swift, Harry Styles, Miley Cyrus, Dua Lipa, Billie Eilish)
                else -> {
                    Quadruple(
                        doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 440.00, 392.00), // Pop anthem progression
                        3.2, // ~118 bpm energetic pop dance tempo
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

                        val beatMod = if (isStaccato) {
                            val cycle = (t * beatTempo) % 1.0
                            if (cycle < 0.6) 1.0 else 0.20
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

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Error stopping MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
    }

    private fun stopAudioTrack() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w("BlinkMusicPlayer", "Error stopping AudioTrack: ${e.message}")
        }
        audioTrack = null
    }

    fun stopPlayback() {
        _isPlaying.value = false
        stopMediaPlayer()
        stopAudioTrack()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
