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
        refreshCachedList(context)
        IslamicSoundCatalog.allSounds
            .filter { it.isBundledRaw && it.rawResName != null }
            .filterNot { hasBundledResource(context, it) }
            .forEach { ensureFallbackAudio(context, it) }
    }

    private fun getAudioDir(context: Context): File {
        return File(context.filesDir, AUDIO_DIR)
    }

    fun getLocalFileForSound(context: Context, soundId: String): File {
        return File(getAudioDir(context), "$soundId.mp3")
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
            if (file.name.endsWith(".mp3") && file.length() > 1024) {
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

            val localFile = getLocalFileForSound(context, sound.id)
            if (!localFile.exists() || localFile.length() <= 1024) {
                synthesizeSpiritualAudioFallback(localFile, sound)
            }
            check(localFile.exists() && localFile.length() > 1024) {
                "Could not generate playable fallback audio for ${sound.title}."
            }
            refreshCachedList(context)
            return Uri.fromFile(localFile).toString()
        }

        val localFile = getLocalFileForSound(context, sound.id)
        if (localFile.exists() && localFile.length() > 1024) return Uri.fromFile(localFile).toString()
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
            if (!hasBundledResource(context, sound)) {
                val localFile = getLocalFileForSound(context, sound.id)
                if (localFile.exists() && localFile.length() > 1024) {
                    refreshCachedList(context)
                    onSuccess?.invoke(Uri.fromFile(localFile).toString())
                    return
                }
                _downloadingSoundId.value = sound.id
                _downloadProgress.value = 0.1f
                CoroutineScope(Dispatchers.IO).launch {
                    ensureFallbackAudio(context, sound) {
                        _downloadingSoundId.value = null
                        _downloadProgress.value = 1f
                        refreshCachedList(context)
                        if (localFile.exists() && localFile.length() > 1024) {
                            onSuccess?.invoke(Uri.fromFile(localFile).toString())
                        } else {
                            onError?.invoke("Could not create fallback audio for ${sound.title}.")
                        }
                    }
                }
                return
            }
            val uri = resolvePlayableUri(context, sound)
            onSuccess?.invoke(uri)
            return
        }

        val localFile = getLocalFileForSound(context, sound.id)
        if (localFile.exists() && localFile.length() > 1024) {
            refreshCachedList(context)
            onSuccess?.invoke(Uri.fromFile(localFile).toString())
            return
        }

        _downloadingSoundId.value = sound.id
        _downloadProgress.value = 0.05f

        CoroutineScope(Dispatchers.IO).launch {
            var downloadSuccess = false
            var connection: HttpURLConnection? = null
            var temporaryFile: File? = null
            try {
                val parent = localFile.parentFile
                check(parent != null && (parent.exists() || parent.mkdirs())) {
                    "Could not create audio cache directory."
                }
                val downloadFile = File.createTempFile("${sound.id.take(20)}-", ".tmp", parent)
                temporaryFile = downloadFile
                val url = URL(sound.remoteAudioUrl)
                val activeConnection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "IslamicPrayerTimes/2.0")
                }
                connection = activeConnection
                activeConnection.connect()

                if (activeConnection.responseCode in 200..299) {
                    val fileLength = activeConnection.contentLength
                    val buffer = ByteArray(4096)
                    var total: Long = 0
                    activeConnection.inputStream.use { input ->
                        FileOutputStream(downloadFile).use { output ->
                            var count: Int
                            while (input.read(buffer).also { count = it } != -1) {
                                total += count
                                if (fileLength > 0) {
                                    val progress = (total.toFloat() / fileLength.toFloat()).coerceIn(0.1f, 0.95f)
                                    _downloadProgress.value = progress
                                }
                                output.write(buffer, 0, count)
                            }
                        }
                    }

                    if (downloadFile.length() > 1024) {
                        val fileLock = fallbackFileLocks.computeIfAbsent(localFile.absolutePath) { Any() }
                        synchronized(fileLock) {
                            if (!localFile.exists() || localFile.length() <= 1024) {
                                if (localFile.exists() && !localFile.delete()) {
                                    error("Could not replace invalid cached audio.")
                                }
                                if (!downloadFile.renameTo(localFile)) {
                                    error("Could not move downloaded audio into the cache.")
                                }
                            }
                            downloadSuccess = localFile.exists() && localFile.length() > 1024
                        }
                        if (downloadSuccess) {
                            Log.d(TAG, "Successfully downloaded & cached ${sound.title} (${localFile.length()} bytes)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote download failed for ${sound.title} (${e.message}), generating localized spiritual sound fallback")
            } finally {
                connection?.disconnect()
                temporaryFile?.delete()
            }

            // If network failed or produced empty file, synthesize a serene acoustic audio file
            if (!downloadSuccess || localFile.length() <= 1024) {
                synthesizeSpiritualAudioFallback(localFile, sound)
                downloadSuccess = localFile.exists() && localFile.length() > 1024
            }

            withContext(Dispatchers.Main) {
                _downloadingSoundId.value = null
                _downloadProgress.value = 1.0f
                refreshCachedList(context)

                if (downloadSuccess) {
                    val uri = Uri.fromFile(localFile).toString()
                    onSuccess?.invoke(uri)
                } else {
                    onError?.invoke("Could not download audio. Please check internet connection.")
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
