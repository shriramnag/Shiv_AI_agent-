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
import com.example.tools.KnowledgeBaseTool
import com.example.tools.MemoryTool
import com.example.tools.NotesTool
import com.example.tools.ScreenTool
import com.example.tools.SmartHomeTool
import com.example.tools.ToolRegistry
import com.example.tools.WebSearchTool
import com.example.voice.AudioTrackPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    lateinit var cyberShieldEngine: com.example.security.CyberShieldEngine
        private set
    lateinit var offlineCognitiveEngine: com.example.ai.OfflineCognitiveEngine
        private set
    lateinit var codingStudioEngine: com.example.ai.CodingStudioEngine
        private set
    lateinit var agiAgentEngine: com.example.ai.AgiAgentEngine
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
            SmartHomeTool(database.smartDeviceDao()),
            KnowledgeBaseTool(database.documentDao()),
            WebSearchTool(this, database.searchCacheDao())
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (database.smartDeviceDao().getAllDevicesList().isEmpty()) {
                database.smartDeviceDao().insertAll(
                    listOf(
                        com.example.data.local.SmartDeviceEntity(name = "Living Room Light", type = "LIGHT", room = "Living Room", isPoweredOn = true, brightnessOrValue = 80),
                        com.example.data.local.SmartDeviceEntity(name = "Bedroom AC", type = "AC", room = "Bedroom", isPoweredOn = true, brightnessOrValue = 24),
                        com.example.data.local.SmartDeviceEntity(name = "Kitchen Smart Plug", type = "PLUG", room = "Kitchen", isPoweredOn = false),
                        com.example.data.local.SmartDeviceEntity(name = "Smart TV", type = "TV", room = "Living Room", isPoweredOn = false)
                    )
                )
            }
        }

        toolRegistry = ToolRegistry(tools)
        restClient = GeminiRestClient(toolRegistry)

        liveClient = GeminiLiveClient(
            audioTrackPlayer = audioTrackPlayer,
            toolRegistry = toolRegistry,
            onTranscriptChunk = {},
            onTurnComplete = {},
            onError = {}
        )

        cyberShieldEngine = com.example.security.CyberShieldEngine(this)
        codingStudioEngine = com.example.ai.CodingStudioEngine(restClient)
        agiAgentEngine = com.example.ai.AgiAgentEngine(
            agiGoalDao = database.agiGoalDao(),
            noteDao = database.noteDao(),
            cyberShieldEngine = cyberShieldEngine,
            webSearchTool = tools.filterIsInstance<WebSearchTool>().first(),
            restClient = restClient
        )
        offlineCognitiveEngine = com.example.ai.OfflineCognitiveEngine(
            context = this,
            noteDao = database.noteDao(),
            memoryDao = database.memoryDao(),
            lexiconDao = database.languageLexiconDao(),
            deviceControlTool = tools.filterIsInstance<DeviceControlTool>().first(),
            appControlTool = tools.filterIsInstance<AppControlTool>().first(),
            cyberShieldEngine = cyberShieldEngine
        )

        brain = ShivaiBrain(
            settingsPrefs = settingsPrefs,
            memoryDao = database.memoryDao(),
            lexiconDao = database.languageLexiconDao(),
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
