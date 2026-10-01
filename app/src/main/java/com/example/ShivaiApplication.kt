package com.example

import android.app.Application
import com.example.ai.GeminiLiveClient
import com.example.ai.GeminiRestClient
import com.example.ai.ShivaiBrain
import com.example.data.local.SettingsPreferences
import com.example.data.local.ShivaiDatabase
import com.example.tools.AppControlTool
import com.example.tools.CommunicationTool
import com.example.tools.DeviceControlTool
import com.example.tools.MemoryTool
import com.example.tools.NotesTool
import com.example.tools.ScreenTool
import com.example.tools.ToolRegistry
import com.example.tools.WebSearchTool
import com.example.voice.AudioTrackPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConfirmationRequest(
    val prompt: String,
    val onConfirm: () -> Unit
)

class ShivaiApplication : Application() {

    lateinit var database: ShivaiDatabase
        private set
    lateinit var settingsPrefs: SettingsPreferences
        private set
    lateinit var toolRegistry: ToolRegistry
        private set
    lateinit var audioTrackPlayer: AudioTrackPlayer
        private set
    lateinit var liveClient: GeminiLiveClient
        private set
    lateinit var restClient: GeminiRestClient
        private set
    lateinit var brain: ShivaiBrain
        private set

    private val _pendingConfirmation = MutableStateFlow<ConfirmationRequest?>(null)
    val pendingConfirmation: StateFlow<ConfirmationRequest?> = _pendingConfirmation.asStateFlow()

    fun requestConfirmation(prompt: String, onConfirm: () -> Unit) {
        _pendingConfirmation.value = ConfirmationRequest(prompt, onConfirm)
    }

    fun clearConfirmation() {
        _pendingConfirmation.value = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ShivaiDatabase.getDatabase(this)
        settingsPrefs = SettingsPreferences(this)
        audioTrackPlayer = AudioTrackPlayer()

        val tools = listOf(
            AppControlTool(this),
            ScreenTool(),
            NotesTool(database.noteDao()),
            MemoryTool(database.memoryDao()),
            DeviceControlTool(this),
            CommunicationTool(this) { prompt, action ->
                requestConfirmation(prompt, action)
            },
            WebSearchTool(this)
        )

        toolRegistry = ToolRegistry(tools)
        restClient = GeminiRestClient(toolRegistry)

        liveClient = GeminiLiveClient(
            audioTrackPlayer = audioTrackPlayer,
            toolRegistry = toolRegistry,
            onTranscriptChunk = {},
            onTurnComplete = {},
            onError = {}
        )

        brain = ShivaiBrain(
            settingsPrefs = settingsPrefs,
            memoryDao = database.memoryDao(),
            toolRegistry = toolRegistry,
            liveClient = liveClient,
            restClient = restClient
        )
    }

    companion object {
        lateinit var instance: ShivaiApplication
            private set
    }
}
