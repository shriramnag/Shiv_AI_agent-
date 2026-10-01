package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechRecognitionState {
    IDLE,
    READY,
    LISTENING,
    PROCESSING,
    ERROR
}

class RealtimeSpeechRecognizer(
    private val context: Context,
    private val onFinalResult: (String) -> Unit,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onErrorOccurred: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "RealtimeSpeechRecognizer"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    private val _liveRms = MutableStateFlow(0f)
    val liveRms: StateFlow<Float> = _liveRms.asStateFlow()

    private val _state = MutableStateFlow(SpeechRecognitionState.IDLE)
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun initRecognizerIfNeeded() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechRecognitionState.READY
            _isListening.value = true
            _liveSpokenText.value = ""
        }

        override fun onBeginningOfSpeech() {
            _state.value = SpeechRecognitionState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            // rmsdB typically ranges from -2dB to 10dB
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _liveRms.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = SpeechRecognitionState.PROCESSING
            _isListening.value = false
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _state.value = SpeechRecognitionState.ERROR
            _liveRms.value = 0f

            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission missing"
                SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try speaking again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Retrying..."
                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Speech recognition error code: $error"
            }

            Log.w(TAG, "SpeechRecognizer error: $errorMsg ($error)")

            // Only report serious errors to avoid toast spam on timeout/no match
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                onErrorOccurred?.invoke(errorMsg)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _state.value = SpeechRecognitionState.IDLE
            _liveRms.value = 0f

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull()?.trim()

            if (!recognizedText.isNullOrBlank()) {
                _liveSpokenText.value = recognizedText
                onFinalResult(recognizedText)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()
            if (!partial.isNullOrBlank()) {
                _liveSpokenText.value = partial
                onPartialResult?.invoke(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(preferHindi: Boolean = true) {
        mainHandler.post {
            try {
                if (_isListening.value) {
                    stopListening()
                }

                initRecognizerIfNeeded()

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    
                    // Support multilingual Hindi + English
                    if (preferHindi) {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    } else {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    }
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _state.value = SpeechRecognitionState.READY
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _isListening.value = false
                _state.value = SpeechRecognitionState.ERROR
                onErrorOccurred?.invoke("Could not start speech recognition: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping speech recognition", e)
            } finally {
                _isListening.value = false
                _state.value = SpeechRecognitionState.IDLE
                _liveRms.value = 0f
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling speech recognition", e)
            } finally {
                _isListening.value = false
                _state.value = SpeechRecognitionState.IDLE
                _liveRms.value = 0f
                _liveSpokenText.value = ""
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying speech recognition", e)
            }
        }
    }
}
