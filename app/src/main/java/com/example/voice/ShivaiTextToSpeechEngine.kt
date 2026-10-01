package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ShivaiTextToSpeechEngine(
    private val context: Context,
    private val onSpeechCompleted: () -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "ShivaiTTSEngine"
        private val HINDI_LOCALE = Locale.forLanguageTag("hi-IN")
        private val ENGLISH_IN_LOCALE = Locale.forLanguageTag("en-IN")
        private val ENGLISH_US_LOCALE = Locale.US
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentUtterance = MutableStateFlow("")
    val currentUtterance: StateFlow<String> = _currentUtterance.asStateFlow()

    private var pendingSpeechText: String? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            configureTtsEngine()
            isInitialized = true
            Log.i(TAG, "TextToSpeech engine successfully initialized.")

            pendingSpeechText?.let { text ->
                pendingSpeechText = null
                speak(text)
            }
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine. Status: $status")
            isInitialized = false
        }
    }

    private fun configureTtsEngine() {
        val engine = tts ?: return

        // Set high quality audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttributes)

        // Set natural defaults
        engine.setPitch(1.0f)
        engine.setSpeechRate(1.02f)

        // Set modern utterance listener
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                abandonAudioFocus()
                onSpeechCompleted()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                abandonAudioFocus()
                Log.w(TAG, "TTS Error on utterance: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                abandonAudioFocus()
                Log.w(TAG, "TTS Error code: $errorCode on utterance: $utteranceId")
            }
        })

        // Best natural voice selection
        selectBestVoice(detectLanguage("नमस्ते Hello"))
    }

    /**
     * Cleans up markdown, bullet points, asterisks, URLs, and code blocks
     * so TTS speaks naturally without pronouncing punctuation characters.
     */
    fun sanitizeForSpeech(rawText: String): String {
        var text = rawText

        // Strip code blocks ``` ... ```
        text = text.replace(Regex("```[\\s\\S]*?```"), " code snippet ")

        // Strip inline code `code`
        text = text.replace(Regex("`([^`]+)`"), "$1")

        // Strip markdown links [Title](url) -> Title
        text = text.replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")

        // Strip raw URLs
        text = text.replace(Regex("https?://\\S+"), "")

        // Strip Markdown bold/italics (**, *, __, _)
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("__([^_]+)__"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")

        // Strip Markdown headers (###, ##, #)
        text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")

        // Convert bullet points to natural pauses
        text = text.replace(Regex("(?m)^\\s*[-*•]\\s+"), ". ")
        text = text.replace(Regex("(?m)^\\s*\\d+\\.\\s+"), ". ")

        // Strip tool tags or json artefacts if any leaked
        text = text.replace(Regex("\\{[^}]*\\}"), "")

        // Collapse multiple spaces and line breaks
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }

    /**
     * Detects if the text is primarily Hindi (Devanagari script) or English/Latin.
     */
    fun detectLanguage(text: String): Locale {
        var devanagariCount = 0
        var latinCount = 0

        for (char in text) {
            val code = char.code
            if (code in 0x0900..0x097F) {
                devanagariCount++
            } else if (char.isLetter()) {
                latinCount++
            }
        }

        return if (devanagariCount > 0 && devanagariCount >= latinCount / 3) {
            HINDI_LOCALE
        } else {
            // Default to Indian English or US English
            ENGLISH_IN_LOCALE
        }
    }

    private fun selectBestVoice(targetLocale: Locale) {
        val engine = tts ?: return

        // Set locale first
        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English US
            engine.language = ENGLISH_US_LOCALE
        }

        // Try selecting a high quality neural voice if available
        try {
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val matchingVoices = voices.filter { voice ->
                    voice.locale.language == targetLocale.language && !voice.isNetworkConnectionRequired
                }

                val bestVoice = matchingVoices.firstOrNull { voice ->
                    voice.quality >= Voice.QUALITY_HIGH &&
                            (voice.name.contains("natural", ignoreCase = true) ||
                             voice.name.contains("neural", ignoreCase = true) ||
                             voice.name.contains("high", ignoreCase = true) ||
                             voice.name.contains("hie", ignoreCase = true) ||
                             voice.name.contains("ene", ignoreCase = true))
                } ?: matchingVoices.firstOrNull()

                if (bestVoice != null) {
                    engine.voice = bestVoice
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection fallback to default: ${e.localizedMessage}")
        }
    }

    fun speak(rawText: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        val cleanText = sanitizeForSpeech(rawText)
        if (cleanText.isBlank()) return

        if (!isInitialized || tts == null) {
            pendingSpeechText = cleanText
            return
        }

        requestAudioFocus()

        val detectedLocale = detectLanguage(cleanText)
        selectBestVoice(detectedLocale)

        _isSpeaking.value = true
        _currentUtterance.value = cleanText

        val utteranceId = "shivai_utterance_${System.currentTimeMillis()}"

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(cleanText, queueMode, params, utteranceId)
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attributes)
                        .setOnAudioFocusChangeListener { focusChange ->
                            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                                stop()
                            }
                        }
                        .build()
                }
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request audio focus: ${e.localizedMessage}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to abandon audio focus: ${e.localizedMessage}")
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
