package com.example.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class AudioTrackPlayer {

    companion object {
        const val SAMPLE_RATE = 24000 // Gemini Live standard output sample rate
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = ConcurrentLinkedQueue<ByteArray>()
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    @Synchronized
    private fun initAudioTrack() {
        if (audioTrack == null) {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBufferSize.coerceAtLeast(SAMPLE_RATE * 2)

            audioTrack = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack?.play()
        }
    }

    fun enqueueBase64Chunk(base64Pcm: String) {
        try {
            val pcmBytes = Base64.decode(base64Pcm, Base64.DEFAULT)
            enqueueBytes(pcmBytes)
        } catch (e: Exception) {
            // Ignore malformed chunks
        }
    }

    fun enqueueBytes(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        audioQueue.add(pcmBytes)
        ensurePlaybackRunning()
    }

    private fun ensurePlaybackRunning() {
        if (playbackJob?.isActive == true) return

        playbackJob = scope.launch {
            initAudioTrack()
            _isPlaying.value = true

            while (audioQueue.isNotEmpty()) {
                val chunk = audioQueue.poll() ?: break
                audioTrack?.write(chunk, 0, chunk.size)
            }

            _isPlaying.value = false
        }
    }

    /**
     * Instantly stops playback and clears all pending audio (for user barge-in / interruption).
     */
    @Synchronized
    fun stopAndFlush() {
        audioQueue.clear()
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            // Track might be released
        }
        _isPlaying.value = false
    }

    @Synchronized
    fun release() {
        stopAndFlush()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignored
        }
        audioTrack = null
    }
}
