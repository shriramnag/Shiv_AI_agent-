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
        private const val KEY_FLOATING_OVERLAY = "floating_overlay_enabled"
        private const val KEY_BIOMETRIC_SECURITY = "biometric_security_enabled"
        private const val KEY_HOME_ASSISTANT_URL = "home_assistant_url"

        const val DEFAULT_LIVE_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val DEFAULT_TEXT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_VOICE_NAME = "Aoede"

        fun sanitizeApiKey(raw: String): String {
            if (raw.isBlank()) return ""
            var key = raw
            // Remove zero-width spaces, BOM, directional marks, and NBSP
            key = key.replace("[\u200B\u200C\u200D\uFEFF\u00A0\u200E\u200F\u202A-\u202E]".toRegex(), "")
            // Remove extra whitespace, newlines, tabs
            key = key.trim()
            // If copied as key=AIza... or GEMINI_API_KEY=AIza...
            if (key.contains("=")) {
                key = key.substringAfterLast("=").trim()
            }
            // If copied as Bearer AIza...
            if (key.startsWith("Bearer ", ignoreCase = true)) {
                key = key.substring(7).trim()
            }
            // Strip any surrounding quotes or punctuation
            key = key.removeSurrounding("\"").removeSurrounding("'").removeSurrounding("`").trim()
            key = key.replace("\"", "").replace("'", "").replace("`", "").replace(";", "").replace(",", "").trim()
            return key
        }
    }

    private val _apiKeyFlow = MutableStateFlow(getEffectiveApiKey())
    val apiKeyFlow: StateFlow<String> = _apiKeyFlow.asStateFlow()

    fun getCustomApiKey(): String {
        val raw = prefs.getString(KEY_API_KEY, "") ?: ""
        return sanitizeApiKey(raw)
    }

    fun getEffectiveApiKey(): String {
        val custom = getCustomApiKey()
        if (custom.isNotBlank()) return custom
        // Fallback to BuildConfig if present and valid
        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                sanitizeApiKey(buildConfigKey)
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(key: String) {
        val clean = sanitizeApiKey(key)
        prefs.edit().putString(KEY_API_KEY, clean).apply()
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
        get() {
            val m = prefs.getString(KEY_LIVE_MODEL, DEFAULT_LIVE_MODEL) ?: DEFAULT_LIVE_MODEL
            return if (m.contains("3.8")) DEFAULT_LIVE_MODEL else m
        }
        set(value) = prefs.edit().putString(KEY_LIVE_MODEL, value).apply()

    var textModel: String
        get() {
            val m = prefs.getString(KEY_TEXT_MODEL, DEFAULT_TEXT_MODEL) ?: DEFAULT_TEXT_MODEL
            return if (m.contains("3.5")) DEFAULT_TEXT_MODEL else m
        }
        set(value) = prefs.edit().putString(KEY_TEXT_MODEL, value).apply()

    var voiceName: String
        get() = prefs.getString(KEY_VOICE_NAME, DEFAULT_VOICE_NAME) ?: DEFAULT_VOICE_NAME
        set(value) = prefs.edit().putString(KEY_VOICE_NAME, value).apply()

    var personality: String
        get() = prefs.getString(KEY_PERSONALITY, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, false)
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

    var floatingOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_OVERLAY, value).apply()

    var biometricSecurityEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_SECURITY, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_SECURITY, value).apply()

    var homeAssistantUrl: String
        get() = prefs.getString(KEY_HOME_ASSISTANT_URL, "http://homeassistant.local:8123") ?: "http://homeassistant.local:8123"
        set(value) = prefs.edit().putString(KEY_HOME_ASSISTANT_URL, value).apply()
}
