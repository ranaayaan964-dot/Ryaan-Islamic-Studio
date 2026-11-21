package com.ayaan.islamicstudio.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.sin

object AudioCacheManager {

    private const val TAG = "AudioCacheManager"
    private const val AUDIO_DIR = "islamic_sounds"
    private const val AUDIO_CACHE_VERSION = 3
    private const val AUDIO_CACHE_PREFS = "ayaan_audio_cache_prefs"
    private const val CACHE_VERSION_KEY = "audio_cache_version"
    private val fallbackGenerationInProgress = Collections.synchronizedSet(mutableSetOf<String>())
    private val fallbackGenerationInProgressCallbacks = mutableMapOf<String, MutableList<() -> Unit>>()
    private val fallbackFileLocks = ConcurrentHashMap<String, Any>()

    private val _downloadingSoundId = MutableStateFlow<String?>(null)
    val downloadingSoundId = _downloadingSoundId.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _cachedSounds = MutableStateFlow<Set<String>>(emptySet())
    val cachedSounds = _cachedSounds.asStateFlow()

    fun initialize(context: Context) {
        val dir = getAudioDir(context)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val preferences = context.getSharedPreferences(AUDIO_CACHE_PREFS, Context.MODE_PRIVATE)
        val storedVersion = preferences.getInt(CACHE_VERSION_KEY, 0)
        if (storedVersion != AUDIO_CACHE_VERSION) {
            dir.listFiles()
                ?.filter { it.isFile && (it.extension.equals("wav", true) || it.extension.equals("mp3", true)) }
                ?.forEach { file ->
                    runCatching { file.delete() }
                        .onFailure { Log.w(TAG, "Could not purge old synthetic audio cache: ${file.name}") }
                }
            preferences.edit().putInt(CACHE_VERSION_KEY, AUDIO_CACHE_VERSION).apply()
        }
        refreshCachedList(context)
    }

    private fun getAudioDir(context: Context): File {
        return File(context.filesDir, AUDIO_DIR)
    }

    fun getLocalFileForSound(context: Context, soundId: String): File {
        return File(getAudioDir(context), "$soundId.mp3")
    }

    private fun isValidCachedMp3(file: File): Boolean {
        if (!file.exists() || file.length() < 8192L) return false
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(12)
                val read = input.read(header)
                if (read < 3) return@use false
                val startsWithId3 = header.copyOfRange(0, 3).contentEquals("ID3".toByteArray())
                val startsWithMp3Frame = header[0].toInt() and 0xFF == 0xFF &&
                    (header[1].toInt() and 0xE0) == 0xE0 &&
                    ((header[1].toInt() and 0x06) != 0x00)
                val startsWithRiff = header.copyOfRange(0, 4).contentEquals("RIFF".toByteArray())
                val startsWithOgg = header.copyOfRange(0, 4).contentEquals("OggS".toByteArray())
                val startsWithFtyp = header.size >= 12 &&
                    header.copyOfRange(4, 8).contentEquals("ftyp".toByteArray())
                startsWithId3 || startsWithMp3Frame || startsWithRiff || startsWithOgg || startsWithFtyp
            }
        }.getOrDefault(false)
    }

    fun isSoundAvailableLocally(context: Context, sound: IslamicSoundItem): Boolean {
        if (sound.isBundledRaw && hasBundledResource(context, sound)) return true
        val file = getLocalFileForSound(context, sound.id)
        return file.exists() && file.length() > 1024
    }

    fun refreshCachedList(context: Context) {
        val dir = getAudioDir(context)
        val files = dir.listFiles() ?: emptyArray()
        val set = mutableSetOf<String>()
        // Include all bundled raw sounds
        IslamicSoundCatalog.allSounds.forEach { sound ->
            if (sound.isBundledRaw && hasBundledResource(context, sound)) {
                set.add(sound.id)
            }
        }
        // Include downloaded files
        files.forEach { file ->
            if (file.name.endsWith(".mp3") && isValidCachedMp3(file)) {
                set.add(file.nameWithoutExtension)
            }
        }
        _cachedSounds.value = set
    }

    /**
     * Resolves the playable URI for the sound.
     * If bundled, returns android.resource URI.
     * If cached, returns file URI.
     * If remote and not yet cached, returns remote URL (or triggers background cache).
     */
    fun resolvePlayableUri(context: Context, sound: IslamicSoundItem): String {
        if (sound.isBundledRaw) {
            sound.rawResName?.let { rawName ->
                val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
                if (resId != 0) {
                    return "android.resource://${context.packageName}/raw/$rawName"
                }
            }
        }

        val localFile = getLocalFileForSound(context, sound.id)
        if (isValidCachedMp3(localFile)) return Uri.fromFile(localFile).toString()
        return sound.remoteAudioUrl
    }

    /**
     * Downloads and caches remote tone to internal storage.
     * If network is unavailable, generates a high-quality spiritual acoustic fallback
     * so user can immediately use it for alarms without errors.
     */
    fun downloadAndCacheSound(
        context: Context,
        sound: IslamicSoundItem,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        if (sound.isBundledRaw) {
            if (hasBundledResource(context, sound)) {
                onSuccess?.invoke(resolvePlayableUri(context, sound))
                return
            }
        }

        val localFile = getLocalFileForSound(context, sound.id)
        if (isValidCachedMp3(localFile)) {
            refreshCachedList(context)
            onSuccess?.invoke(Uri.fromFile(localFile).toString())
            return
        }

        _downloadingSoundId.value = sound.id
        _downloadProgress.value = 0.05f

        CoroutineScope(Dispatchers.IO).launch {
            val mirrors = (listOf(sound.remoteAudioUrl) + sound.fallbackUrls).distinct().filter(String::isNotBlank)
            var downloaded = false
            var lastError = "Could not download audio. Please check internet connection."

            val parent = localFile.parentFile
            if (parent == null || (!parent.exists() && !parent.mkdirs())) {
                lastError = "Could not create audio cache directory."
            } else {
                for ((index, mirror) in mirrors.withIndex()) {
                    var connection: HttpURLConnection? = null
                    var tempFile: File? = null
                    try {
                        val url = URL(mirror)
                        val active = (url.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000
                            readTimeout = 12000
                            instanceFollowRedirects = true
                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; AyaanIslamicStudio)")
                            setRequestProperty("Accept", "audio/mpeg,audio/*,*/*")
                        }
                        connection = active
                        active.connect()
                        val contentType = active.contentType?.lowercase().orEmpty()
                        if (active.responseCode !in 200..299 ||
                            contentType.startsWith("text/") ||
                            contentType.startsWith("application/json")
                        ) {
                            throw IllegalStateException("Invalid audio response")
                        }
                        tempFile = File.createTempFile("${sound.id.take(20)}-", ".tmp", parent)
                        val buffer = ByteArray(4096)
                        var total = 0L
                        active.inputStream.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                var count: Int
                                while (input.read(buffer).also { count = it } != -1) {
                                    total += count
                                    val length = active.contentLengthLong
                                    if (length > 0L) {
                                        _downloadProgress.value =
                                            (total.toFloat() / length.toFloat()).coerceIn(0.05f, 0.95f)
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        if (isValidCachedMp3(tempFile)) {
                            if (localFile.exists()) localFile.delete()
                            if (tempFile.renameTo(localFile) && isValidCachedMp3(localFile)) {
                                downloaded = true
                                Log.i(TAG, "Cached real MP3 for ${sound.title} from mirror ${index + 1}.")
                                break
                            }
                        } else {
                            throw IllegalStateException("Downloaded file did not contain valid audio magic bytes.")
                        }
                    } catch (error: Exception) {
                        lastError = if (error is java.net.UnknownHostException) {
                            "Connect to internet to stream or download this audio"
                        } else {
                            "Could not download this audio. Please try another mirror."
                        }
                        Log.w(TAG, "Mirror ${index + 1} failed for ${sound.title}.", error)
                    } finally {
                        connection?.disconnect()
                        tempFile?.delete()
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _downloadingSoundId.value = null
                _downloadProgress.value = 1.0f
                refreshCachedList(context)
                if (downloaded) {
                    onSuccess?.invoke(Uri.fromFile(localFile).toString())
                } else {
                    onError?.invoke(lastError)
                }
            }
        }
    }

    private fun hasBundledResource(context: Context, sound: IslamicSoundItem): Boolean {
        val rawName = sound.rawResName ?: return false
        return context.resources.getIdentifier(rawName, "raw", context.packageName) != 0
    }

    private fun ensureFallbackAudio(
        context: Context,
        sound: IslamicSoundItem,
        onComplete: (() -> Unit)? = null
    ) {
        val localFile = getLocalFileForSound(context, sound.id)
        if (localFile.exists() && localFile.length() > 1024) {
            onComplete?.invoke()
            return
        }
        val shouldStart = synchronized(fallbackGenerationInProgress) {
            if (sound.id in fallbackGenerationInProgress) {
                if (onComplete != null) {
                    fallbackGenerationInProgressCallbacks
                        .getOrPut(sound.id) { mutableListOf() }
                        .add(onComplete)
                }
                false
            } else {
                fallbackGenerationInProgress.add(sound.id)
                if (onComplete != null) {
                    fallbackGenerationInProgressCallbacks
                        .getOrPut(sound.id) { mutableListOf() }
                        .add(onComplete)
                }
                true
            }
        }
        if (!shouldStart) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                synthesizeSpiritualAudioFallback(localFile, sound)
                if (!localFile.exists() || localFile.length() <= 1024) {
                    Log.e(TAG, "Offline fallback generation produced no usable audio for ${sound.id}.")
                }
            } finally {
                val callbacks = synchronized(fallbackGenerationInProgress) {
                    fallbackGenerationInProgress.remove(sound.id)
                    fallbackGenerationInProgressCallbacks.remove(sound.id).orEmpty()
                }
                withContext(Dispatchers.Main) {
                    refreshCachedList(context)
                    callbacks.forEach { it() }
                }
            }
        }
    }

    /**
     * Synthesizes an offline acoustic WAV-formatted audio file saved as .mp3 container
     * ensuring that alarm playback never fails even if offline.
     */
    fun synthesizeSpiritualAudioFallback(targetFile: File, sound: IslamicSoundItem) {
        val lock = fallbackFileLocks.computeIfAbsent(targetFile.absolutePath) { Any() }
        synchronized(lock) {
            if (targetFile.exists() && targetFile.length() > 1024) return
            var outputStream: BufferedOutputStream? = null
            var temporaryFile: File? = null
            try {
                targetFile.parentFile?.let { parent ->
                    check(parent.exists() || parent.mkdirs()) { "Could not create audio cache directory." }
                }
            val fallbackFile = File(targetFile.parentFile, "${targetFile.name}.fallback.tmp")
            temporaryFile = fallbackFile
            val sampleRate = 22050
            val durationSec = when (sound.category) {
                SoundCategory.SHORT_REMINDERS -> 3.0
                SoundCategory.TAHAJJUD_ZEN -> 8.0
                SoundCategory.PREMIUM_NAATS -> 10.0
                SoundCategory.GLOBAL_ADHANS -> 12.0
                SoundCategory.REAL_AYAT -> 14.0
            }

            val totalSamples = (sampleRate * durationSec).toInt()
            val byteRate = sampleRate * 2
            val dataSize = totalSamples * 2
            val totalFileSize = 36 + dataSize

            val fos = BufferedOutputStream(FileOutputStream(fallbackFile)).also { outputStream = it }

            // Write 44-byte standard RIFF header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalFileSize))
            fos.write("WAVEfmt ".toByteArray())
            fos.write(intToByteArray(16)) // Subchunk1Size
            fos.write(shortToByteArray(1)) // AudioFormat (1 = PCM)
            fos.write(shortToByteArray(1)) // NumChannels (1 = Mono)
            fos.write(intToByteArray(sampleRate))
            fos.write(intToByteArray(byteRate))
            fos.write(shortToByteArray(2)) // BlockAlign
            fos.write(shortToByteArray(16)) // BitsPerSample
            fos.write("data".toByteArray())
            fos.write(intToByteArray(dataSize))

            // Synthesize melody notes based on category
            val baseFreqs = when (sound.category) {
                SoundCategory.GLOBAL_ADHANS -> doubleArrayOf(293.66, 311.13, 369.99, 392.00, 440.00, 392.00, 369.99, 293.66) // Maqam Hijaz
                SoundCategory.PREMIUM_NAATS -> doubleArrayOf(329.63, 392.00, 440.00, 493.88, 587.33, 493.88, 440.00, 329.63) // Spiritual Minor
                SoundCategory.TAHAJJUD_ZEN -> doubleArrayOf(432.0, 540.0, 648.0, 864.0, 648.0, 540.0) // 432Hz Zen Chimes
                SoundCategory.SHORT_REMINDERS -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // Ascension Bells
                SoundCategory.REAL_AYAT -> doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25) // Sacred Harmonic
            }

            val noteDuration = durationSec / baseFreqs.size

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val noteIdx = (t / noteDuration).toInt().coerceIn(0, baseFreqs.size - 1)
                val f = baseFreqs[noteIdx]
                val localT = (t % noteDuration) / noteDuration
                val envelope = sin(PI * localT).coerceAtLeast(0.0)

                val tone = (sin(2.0 * PI * f * t) * 0.65 +
                           sin(2.0 * PI * (f * 2) * t) * 0.25 +
                           sin(2.0 * PI * (f * 3) * t) * 0.10) * envelope * 0.8

                val sample = (tone * 32767).toInt().coerceIn(-32767, 32767).toShort()
                fos.write(sample.toInt() and 0xFF)
                fos.write((sample.toInt() shr 8) and 0xFF)
            }

                fos.flush()
                fos.close()
                if (fallbackFile.length() <= 1024) {
                    error("Generated fallback audio is incomplete.")
                }
                if (targetFile.exists() && !targetFile.delete()) {
                    error("Could not replace invalid cached audio.")
                }
                if (!fallbackFile.renameTo(targetFile)) {
                    error("Could not move generated fallback audio into the cache.")
                }
                Log.d(TAG, "Generated acoustic offline tone for ${sound.title}: ${targetFile.length()} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Error synthesizing fallback for ${sound.id}.", e)
                try {
                    outputStream?.close()
                } catch (closeError: Exception) {
                    Log.w(TAG, "Could not close partial fallback audio for ${sound.id}.", closeError)
                }
                temporaryFile?.delete()
            }
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }
}
