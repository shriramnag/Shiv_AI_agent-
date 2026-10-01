package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class WakeWordEngine(
    private val context: Context,
    private val onWakeWordDetected: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var restartJob: Job? = null

    var wakePhrase: String = "Shivai"
    var sensitivity: String = "MEDIUM" // LOW, MEDIUM, HIGH

    private val _isEngineActive = MutableStateFlow(false)
    val isEngineActive: StateFlow<Boolean> = _isEngineActive.asStateFlow()

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        if (isListening) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
            val intent = createRecognizerIntent()
            speechRecognizer?.startListening(intent)
            isListening = true
            _isEngineActive.value = true
        } catch (e: Exception) {
            isListening = false
            _isEngineActive.value = false
        }
    }

    fun stopListening() {
        isListening = false
        _isEngineActive.value = false
        restartJob?.cancel()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        }
        speechRecognizer = null
    }

    private fun restartListening(delayMs: Long = 300) {
        if (!isListening) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isListening) {
                try {
                    speechRecognizer?.cancel()
                    val intent = createRecognizerIntent()
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    // Retry with new instance
                    try {
                        speechRecognizer?.destroy()
                        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                            setRecognitionListener(createListener())
                        }
                        speechRecognizer?.startListening(createRecognizerIntent())
                    } catch (ex: Exception) {
                        // Ignored
                    }
                }
            }
        }
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            // Error codes like NO_MATCH or SPEECH_TIMEOUT are normal during passive wake-word monitoring
            restartListening(500)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatches(matches)
            restartListening(300)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatches(matches)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun checkMatches(matches: List<String>?) {
        if (matches.isNullOrEmpty()) return

        val normalizedWake = wakePhrase.lowercase(Locale.US)
        val variants = when {
            normalizedWake.contains("hey") -> listOf("hey shivai", "hey shiva", "hey shivay", "hi shivai")
            else -> listOf("shivai", "shiva", "shivay", "sheevai", "shival")
        }

        for (phrase in matches) {
            val lower = phrase.lowercase(Locale.US)
            val matched = when (sensitivity) {
                "HIGH" -> variants.any { lower.contains(it) || it.contains(lower) }
                "LOW" -> variants.any { lower == it || lower.startsWith("$it ") }
                else -> variants.any { lower.contains(it) }
            }
            if (matched) {
                onWakeWordDetected(phrase)
                break
            }
        }
    }
}
