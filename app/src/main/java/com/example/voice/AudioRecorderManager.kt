package com.example.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecorderManager(
    private val onPcmChunk: (ByteArray) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit,
    private val onSpeechStart: () -> Unit,
    private val onSpeechEnd: () -> Unit,
    private val onBargeIn: () -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 16000
        private const val SILENCE_THRESHOLD_MS = 1400L // End-of-speech silence window
        private const val SPEECH_AMPLITUDE_THRESHOLD = 0.12f
        private const val BARGE_IN_AMPLITUDE_THRESHOLD = 0.22f
    }

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var isSpeakingAI = false
    private var isUserSpeaking = false
    private var lastSpeechTime = 0L

    fun setAiSpeaking(speaking: Boolean) {
        isSpeakingAI = speaking
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun startRecording(): Boolean {
        if (_isRecording.value) return true

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return false

        val bufferSize = minBufferSize.coerceAtLeast(SAMPLE_RATE / 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(bufferSize)

                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Calculate RMS amplitude
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                            // Convert short to little-endian bytes
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        val rms = sqrt(sum / readCount)
                        val normalizedAmp = (rms / 32767.0).toFloat().coerceIn(0.0f, 1.0f)
                        onAmplitudeChanged(normalizedAmp)

                        val currentTime = System.currentTimeMillis()

                        // Barge-in detection while AI is speaking
                        if (isSpeakingAI && normalizedAmp > BARGE_IN_AMPLITUDE_THRESHOLD) {
                            onBargeIn()
                        }

                        // Speech onset & end detection (VAD)
                        if (normalizedAmp > SPEECH_AMPLITUDE_THRESHOLD) {
                            if (!isUserSpeaking) {
                                isUserSpeaking = true
                                onSpeechStart()
                            }
                            lastSpeechTime = currentTime
                        } else if (isUserSpeaking && currentTime - lastSpeechTime > SILENCE_THRESHOLD_MS) {
                            isUserSpeaking = false
                            onSpeechEnd()
                        }

                        // Dispatch PCM bytes chunk
                        val pcmChunk = byteBuffer.copyOf(readCount * 2)
                        onPcmChunk(pcmChunk)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            audioRecord?.release()
            audioRecord = null
            _isRecording.value = false
            return false
        }
    }

    @Synchronized
    fun stopRecording() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        }
        audioRecord = null
        isUserSpeaking = false
        onAmplitudeChanged(0.0f)
    }
}
