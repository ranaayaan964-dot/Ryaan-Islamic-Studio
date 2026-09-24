package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * MODULE 8: REAL AUDIO RECORDING FOR HIFZ AI (Android 12+ compatible)
 */
class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null

    companion object {
        private const val TAG = "AudioRecorderHelper"
        const val AUDIO_FILE_NAME = "hifz_rec.m4a"
    }

    fun startRecording(): File {
        stopRecording()

        val outputFilePath = context.cacheDir.absolutePath + "/" + AUDIO_FILE_NAME
        val file = File(outputFilePath)
        if (file.exists()) {
            file.delete()
        }
        recordingFile = file

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
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        mediaRecorder = recorder
        Log.d(TAG, "MediaRecorder started recording to: ${file.absolutePath}")
        return file
    }

    fun stopRecording(): File? {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
        }
        return recordingFile
    }

    fun getRecordedAudioFile(): File? {
        val file = File(context.cacheDir, AUDIO_FILE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }
}
