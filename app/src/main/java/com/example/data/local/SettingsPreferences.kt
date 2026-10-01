package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shivai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "gemini_api_key_custom"
        private const val KEY_LIVE_MODEL = "gemini_live_model"
        private const val KEY_TEXT_MODEL = "gemini_text_model"
        private const val KEY_VOICE_NAME = "gemini_voice_name"
        private const val KEY_PERSONALITY = "shivai_personality"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_PHRASE = "wake_phrase"
        private const val KEY_WAKE_SENSITIVITY = "wake_sensitivity"
        private const val KEY_BARGE_IN = "barge_in_enabled"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_REQUIRE_CONFIRMATION = "require_confirmation"

        const val DEFAULT_LIVE_MODEL = "gemini-3.8-live"
        const val DEFAULT_TEXT_MODEL = "gemini-3.5-flash"
        const val DEFAULT_VOICE_NAME = "Aoede"
    }

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (custom.isNotBlank()) return custom
        // Fallback to BuildConfig if present and valid
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun deleteApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = getEffectiveApiKey()
    }

    fun getMaskedApiKey(): String {
        val key = getEffectiveApiKey()
        if (key.isBlank()) return "Not configured"
        return if (key.length > 8) {
            "${key.take(4)}••••••••${key.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    var liveModel: String
        get() = prefs.getString(KEY_LIVE_MODEL, DEFAULT_LIVE_MODEL) ?: DEFAULT_LIVE_MODEL
        set(value) = prefs.edit().putString(KEY_LIVE_MODEL, value).apply()

    var textModel: String
        get() = prefs.getString(KEY_TEXT_MODEL, DEFAULT_TEXT_MODEL) ?: DEFAULT_TEXT_MODEL
        set(value) = prefs.edit().putString(KEY_TEXT_MODEL, value).apply()

    var voiceName: String
        get() = prefs.getString(KEY_VOICE_NAME, DEFAULT_VOICE_NAME) ?: DEFAULT_VOICE_NAME
        set(value) = prefs.edit().putString(KEY_VOICE_NAME, value).apply()

    var personality: String
        get() = prefs.getString(KEY_PERSONALITY, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakePhrase: String
        get() = prefs.getString(KEY_WAKE_PHRASE, "Shivai") ?: "Shivai"
        set(value) = prefs.edit().putString(KEY_WAKE_PHRASE, value).apply()

    var wakeSensitivity: String
        get() = prefs.getString(KEY_WAKE_SENSITIVITY, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString(KEY_WAKE_SENSITIVITY, value).apply()

    var bargeInEnabled: Boolean
        get() = prefs.getBoolean(KEY_BARGE_IN, true)
        set(value) = prefs.edit().putBoolean(KEY_BARGE_IN, value).apply()

    var memoryEnabled: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_ENABLED, value).apply()

    var requireConfirmation: Boolean
        get() = prefs.getBoolean(KEY_REQUIRE_CONFIRMATION, true)
        set(value) = prefs.edit().putBoolean(KEY_REQUIRE_CONFIRMATION, value).apply()
}
