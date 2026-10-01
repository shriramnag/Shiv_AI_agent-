package com.example.voice

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class AndroidTtsFallback(
    context: Context,
    onSpeechDone: () -> Unit = {}
) {
    private val engine = ShivaiTextToSpeechEngine(context, onSpeechDone)

    val isSpeaking: StateFlow<Boolean> = engine.isSpeaking

    fun speak(text: String) {
        engine.speak(text)
    }

    fun stop() {
        engine.stop()
    }

    fun setSpeechRate(rate: Float) {
        engine.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        engine.setPitch(pitch)
    }

    fun shutdown() {
        engine.shutdown()
    }
}

