package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ShivaiApplication
import com.example.ai.LiveConnectionState
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.service.ShivaiAccessibilityService
import com.example.voice.AndroidTtsFallback
import com.example.voice.AudioRecorderManager
import com.example.voice.RealtimeSpeechRecognizer
import com.example.voice.WakeWordEngine
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShivaiState {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class ShivaiViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShivaiApplication
    private val database = app.database
    val settingsPrefs = app.settingsPrefs
    private val toolRegistry = app.toolRegistry
    private val audioTrackPlayer = app.audioTrackPlayer
    private val restClient = app.restClient
    private val brain = app.brain

    // Assistant State
    private val _state = MutableStateFlow(ShivaiState.STANDBY)
    val state: StateFlow<ShivaiState> = _state.asStateFlow()

    // Real-time Audio Amplitude (0.0f - 1.0f) for Holographic Waveform Visualizer
    private val _amplitude = MutableStateFlow(0.0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    // Live Calling Mode State
    private val _isCallingModeActive = MutableStateFlow(false)
    val isCallingModeActive: StateFlow<Boolean> = _isCallingModeActive.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeakerMuted = MutableStateFlow(false)
    val isSpeakerMuted: StateFlow<Boolean> = _isSpeakerMuted.asStateFlow()

    private val _statusText = MutableStateFlow("Shivai Standby")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _networkAvailable = MutableStateFlow(true)
    val networkAvailable: StateFlow<Boolean> = _networkAvailable.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(ShivaiAccessibilityService.isConnected())
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    // Persistent Database Flows
    val chatMessages: StateFlow<List<ChatMessageEntity>> = database.chatMessageDao()
        .getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = database.noteDao()
        .getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = database.memoryDao()
        .getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartDevices: StateFlow<List<com.example.data.local.SmartDeviceEntity>> = database.smartDeviceDao()
        .getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<com.example.data.local.DocumentEntity>> = database.documentDao()
        .getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetingSummaries: StateFlow<List<com.example.data.local.MeetingScribeEntity>> = database.meetingScribeDao()
        .getAllMeetings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _floatingOverlayActive = MutableStateFlow(settingsPrefs.floatingOverlayEnabled)
    val floatingOverlayActive: StateFlow<Boolean> = _floatingOverlayActive.asStateFlow()

    private val _biometricSecurityActive = MutableStateFlow(settingsPrefs.biometricSecurityEnabled)
    val biometricSecurityActive: StateFlow<Boolean> = _biometricSecurityActive.asStateFlow()

    // Advanced Studio & Generative State
    private val _isGeneratingAsset = MutableStateFlow(false)
    val isGeneratingAsset: StateFlow<Boolean> = _isGeneratingAsset.asStateFlow()

    private val _generatedImageBase64 = MutableStateFlow<String?>(null)
    val generatedImageBase64: StateFlow<String?> = _generatedImageBase64.asStateFlow()

    private val _generatedAudioBase64 = MutableStateFlow<String?>(null)
    val generatedAudioBase64: StateFlow<String?> = _generatedAudioBase64.asStateFlow()

    private val _generatedVideoInfo = MutableStateFlow<String?>(null)
    val generatedVideoInfo: StateFlow<String?> = _generatedVideoInfo.asStateFlow()

    private val _visionAnalysisResult = MutableStateFlow<String?>(null)
    val visionAnalysisResult: StateFlow<String?> = _visionAnalysisResult.asStateFlow()

    private val _groundingResult = MutableStateFlow<Pair<String, List<String>>?>(null)
    val groundingResult: StateFlow<Pair<String, List<String>>?> = _groundingResult.asStateFlow()

    // Voice & Audio Subsystems
    private var audioRecorder: AudioRecorderManager? = null
    private var wakeWordEngine: WakeWordEngine? = null
    private var ttsFallback: AndroidTtsFallback? = null
    private var realtimeSpeechRecognizer: RealtimeSpeechRecognizer? = null

    val isVoiceRecognizing: StateFlow<Boolean>
        get() = realtimeSpeechRecognizer?.isListening ?: MutableStateFlow(false)

    val speechLiveRms: StateFlow<Float>
        get() = realtimeSpeechRecognizer?.liveRms ?: MutableStateFlow(0f)

    private var thinkTimeoutJob: Job? = null
    private var callingModeJob: Job? = null

    init {
        setupNetworkMonitoring()
        initAudioSubsystems()
        monitorTrackPlayerState()
    }

    private fun setupNetworkMonitoring() {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkAvailable.value = true
            }
            override fun onLost(network: Network) {
                _networkAvailable.value = false
                _statusText.value = "Network Unavailable"
            }
        })
    }

    private fun initAudioSubsystems() {
        ttsFallback = AndroidTtsFallback(app) {
            if (_isCallingModeActive.value) {
                transitionToListening()
            } else {
                _state.value = ShivaiState.STANDBY
                _statusText.value = "Shivai Standby"
            }
        }

        audioRecorder = AudioRecorderManager(
            onPcmChunk = { chunk ->
                if (!_isMicMuted.value && _state.value == ShivaiState.LISTENING) {
                    app.liveClient.sendAudioPcmChunk(chunk)
                }
            },
            onAmplitudeChanged = { amp ->
                if (!_isMicMuted.value) {
                    _amplitude.value = amp
                }
            },
            onSpeechStart = {
                if (_state.value == ShivaiState.STANDBY || _state.value == ShivaiState.LISTENING) {
                    _state.value = ShivaiState.LISTENING
                    _statusText.value = "Listening to your voice..."
                }
            },
            onSpeechEnd = {
                if (_state.value == ShivaiState.LISTENING) {
                    _state.value = ShivaiState.THINKING
                    _statusText.value = "Shivai is thinking..."
                    audioRecorder?.setAiSpeaking(true)
                }
            },
            onBargeIn = {
                if (settingsPrefs.bargeInEnabled && (_state.value == ShivaiState.SPEAKING || audioTrackPlayer.isPlaying.value)) {
                    handleBargeIn()
                }
            }
        )

        wakeWordEngine = WakeWordEngine(app) { detectedPhrase ->
            if (settingsPrefs.wakeWordEnabled) {
                onWakeWordTriggered(detectedPhrase)
            }
        }
        wakeWordEngine?.wakePhrase = settingsPrefs.wakePhrase
        wakeWordEngine?.sensitivity = settingsPrefs.wakeSensitivity

        if (settingsPrefs.wakeWordEnabled && !_isCallingModeActive.value) {
            wakeWordEngine?.startListening()
        }

        realtimeSpeechRecognizer = RealtimeSpeechRecognizer(
            context = app,
            onFinalResult = { recognizedText ->
                _liveTranscript.value = recognizedText
                sendVoiceCommand(recognizedText)
            },
            onPartialResult = { partial ->
                _liveTranscript.value = partial
                _statusText.value = "Listening: $partial"
            },
            onErrorOccurred = { errorMsg ->
                Log.w("ShivaiVM", "SpeechRecognizer error: $errorMsg")
            }
        )
    }

    private fun monitorTrackPlayerState() {
        viewModelScope.launch {
            audioTrackPlayer.isPlaying.collect { playing ->
                audioRecorder?.setAiSpeaking(playing)
                if (playing) {
                    _state.value = ShivaiState.SPEAKING
                    _statusText.value = "Shivai is speaking..."
                } else if (_isCallingModeActive.value && _state.value == ShivaiState.SPEAKING) {
                    // Turn complete -> automatically transition to listening
                    transitionToListening()
                }
            }
        }
    }

    fun refreshAccessibilityStatus() {
        _isAccessibilityEnabled.value = ShivaiAccessibilityService.isConnected()
    }

    // --- CALLING MODE (CONTINUOUS HANDS-FREE LOOP) ---

    fun startCallingMode() {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "Please configure your Gemini API Key in Settings first."
            _state.value = ShivaiState.ERROR
            return
        }

        wakeWordEngine?.stopListening()
        _isCallingModeActive.value = true
        _state.value = ShivaiState.CONNECTING
        _statusText.value = "Connecting to Gemini Live..."
        _errorMessage.value = null
        _liveTranscript.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val systemPrompt = brain.buildSystemPrompt()
            app.liveClient.connect(
                apiKey = apiKey,
                modelName = settingsPrefs.liveModel,
                voiceName = settingsPrefs.voiceName,
                systemPrompt = systemPrompt
            )

            // Start continuous audio capture
            val micStarted = audioRecorder?.startRecording() ?: false
            if (micStarted) {
                _state.value = ShivaiState.CONNECTED
                _statusText.value = "Gemini Live Connected"
                delay(400)
                transitionToListening()
            } else {
                _state.value = ShivaiState.ERROR
                _errorMessage.value = "Microphone access failed. Please ensure RECORD_AUDIO permission is granted."
            }
        }
    }

    fun stopCallingMode() {
        _isCallingModeActive.value = false
        audioRecorder?.stopRecording()
        audioTrackPlayer.stopAndFlush()
        app.liveClient.disconnect()
        _state.value = ShivaiState.STANDBY
        _statusText.value = "Shivai Standby"
        _amplitude.value = 0.0f

        if (settingsPrefs.wakeWordEnabled) {
            wakeWordEngine?.startListening()
        }
    }

    fun transitionToListening() {
        if (!_isCallingModeActive.value) return
        _state.value = ShivaiState.LISTENING
        _statusText.value = "Listening..."
        audioRecorder?.setAiSpeaking(false)
    }

    fun handleBargeIn() {
        _state.value = ShivaiState.INTERRUPTED
        _statusText.value = "Interrupted — Listening..."
        audioTrackPlayer.stopAndFlush()
        ttsFallback?.stop()
        app.liveClient.interruptCurrentPlayback()
        viewModelScope.launch {
            delay(150)
            if (_isCallingModeActive.value) {
                transitionToListening()
            }
        }
    }

    fun onWakeWordTriggered(phrase: String) {
        viewModelScope.launch {
            _statusText.value = "Wake word detected: $phrase"
            startCallingMode()
        }
    }

    fun toggleMicMute() {
        _isMicMuted.value = !_isMicMuted.value
    }

    fun toggleSpeakerMute() {
        _isSpeakerMuted.value = !_isSpeakerMuted.value
        if (_isSpeakerMuted.value) {
            audioTrackPlayer.stopAndFlush()
            ttsFallback?.stop()
        }
    }

    fun interruptSpeaking() {
        handleBargeIn()
    }

    // --- NORMAL TEXT CHAT ---

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "Gemini API key is required. Configure it in Settings."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Save user message in DB
            val userMsg = ChatMessageEntity(role = "user", content = text)
            database.chatMessageDao().insertMessage(userMsg)

            _state.value = ShivaiState.THINKING
            _statusText.value = "Shivai is thinking..."

            val systemPrompt = brain.buildSystemPrompt()
            val history = database.chatMessageDao().getRecentMessages(10).map { it.role to it.content }

            val result = restClient.generateContentWithTools(
                apiKey = apiKey,
                model = settingsPrefs.textModel,
                systemInstruction = systemPrompt,
                conversationHistory = history,
                userPrompt = text
            )

            if (result.success) {
                var toolName: String? = null
                var toolArgs: String? = null
                var toolResultStr: String? = null

                if (result.toolExecutions.isNotEmpty()) {
                    val first = result.toolExecutions.first()
                    toolName = first.first
                    toolResultStr = first.second.message
                }

                val aiMsg = ChatMessageEntity(
                    role = "model",
                    content = result.text,
                    toolCallName = toolName,
                    toolCallArgs = toolArgs,
                    toolResult = toolResultStr
                )
                database.chatMessageDao().insertMessage(aiMsg)

                _state.value = ShivaiState.STANDBY
                _statusText.value = "Shivai Standby"
            } else {
                _state.value = ShivaiState.ERROR
                _errorMessage.value = result.errorMessage ?: "Failed to generate response."
                _statusText.value = "Error occurred"
            }
        }
    }

    fun sendVoiceCommand(text: String) {
        if (text.isBlank()) return
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "Gemini API key is required. Configure it in Settings."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMessageEntity(role = "user", content = text)
            database.chatMessageDao().insertMessage(userMsg)

            _state.value = ShivaiState.THINKING
            _statusText.value = "Shivai is thinking..."

            val systemPrompt = brain.buildSystemPrompt()
            val history = database.chatMessageDao().getRecentMessages(6).map { it.role to it.content }

            val result = restClient.generateContentWithTools(
                apiKey = apiKey,
                model = settingsPrefs.textModel,
                systemInstruction = systemPrompt,
                conversationHistory = history,
                userPrompt = text
            )

            if (result.success) {
                var toolName: String? = null
                var toolArgs: String? = null
                var toolResultStr: String? = null

                if (result.toolExecutions.isNotEmpty()) {
                    val first = result.toolExecutions.first()
                    toolName = first.first
                    toolResultStr = first.second.message
                }

                val aiMsg = ChatMessageEntity(
                    role = "model",
                    content = result.text,
                    toolCallName = toolName,
                    toolCallArgs = toolArgs,
                    toolResult = toolResultStr
                )
                database.chatMessageDao().insertMessage(aiMsg)

                speakTextResponse(result.text)
            } else {
                _state.value = ShivaiState.ERROR
                _errorMessage.value = result.errorMessage ?: "Failed to generate response."
                _statusText.value = "Error occurred"
            }
        }
    }

    fun speakTextResponse(text: String) {
        if (_isSpeakerMuted.value) return
        _state.value = ShivaiState.SPEAKING
        _statusText.value = "Shivai is speaking..."
        ttsFallback?.speak(text)
    }

    fun startRealtimeSpeech(preferHindi: Boolean = true) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "Gemini API key is required. Configure it in Settings."
            return
        }
        _state.value = ShivaiState.LISTENING
        _statusText.value = "Listening to your voice..."
        _liveTranscript.value = ""
        realtimeSpeechRecognizer?.startListening(preferHindi)
    }

    fun stopRealtimeSpeech() {
        realtimeSpeechRecognizer?.stopListening()
        if (_state.value == ShivaiState.LISTENING) {
            _state.value = ShivaiState.THINKING
            _statusText.value = "Shivai processing..."
        }
    }

    fun cancelRealtimeSpeech() {
        realtimeSpeechRecognizer?.cancel()
        _state.value = ShivaiState.STANDBY
        _statusText.value = "Shivai Standby"
    }

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            database.chatMessageDao().clearAllMessages()
        }
    }

    // --- NOTES MANAGEMENT ---

    fun createNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = NoteEntity(title = title, content = content, tag = "User Note")
            database.noteDao().insertNote(note)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.noteDao().deleteNoteById(id)
        }
    }

    // --- MEMORY MANAGEMENT ---

    fun saveMemory(key: String, value: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().insertMemory(MemoryEntity(key = key, value = value, category = category))
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().deleteMemoryById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch(Dispatchers.IO) {
            database.memoryDao().clearAllMemories()
        }
    }

    // --- SETTINGS ACTIONS ---

    suspend fun testApiKey(): Pair<Boolean, String> {
        val key = settingsPrefs.getEffectiveApiKey()
        return restClient.testApiKey(key, settingsPrefs.textModel)
    }

    fun saveApiKey(newKey: String) {
        settingsPrefs.saveApiKey(newKey)
    }

    fun deleteApiKey() {
        settingsPrefs.deleteApiKey()
    }

    fun updateWakeWordSettings(enabled: Boolean, phrase: String, sensitivity: String) {
        settingsPrefs.wakeWordEnabled = enabled
        settingsPrefs.wakePhrase = phrase
        settingsPrefs.wakeSensitivity = sensitivity
        wakeWordEngine?.wakePhrase = phrase
        wakeWordEngine?.sensitivity = sensitivity
        if (enabled && !_isCallingModeActive.value) {
            wakeWordEngine?.startListening()
        } else {
            wakeWordEngine?.stopListening()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
        if (_state.value == ShivaiState.ERROR) {
            _state.value = ShivaiState.STANDBY
            _statusText.value = "Shivai Standby"
        }
    }

    // --- ADVANCED GENERATIVE & STUDIO ACTIONS ---

    fun generateImage(prompt: String, inputImageBase64: String? = null, aspectRatio: String = "1:1") {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Image Studio."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _generatedImageBase64.value = null
            val result = restClient.generateOrEditImage(apiKey, prompt, inputImageBase64, aspectRatio)
            _isGeneratingAsset.value = false
            if (result.success && result.imageBase64 != null) {
                _generatedImageBase64.value = result.imageBase64
            } else {
                _errorMessage.value = result.errorMessage ?: "Failed to generate image."
            }
        }
    }

    fun generateVideo(prompt: String, inputImageBase64: String? = null, aspectRatio: String = "16:9") {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Veo 3 Video Studio."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _generatedVideoInfo.value = null
            val result = restClient.generateOrAnimateVideo(apiKey, prompt, inputImageBase64, aspectRatio)
            _isGeneratingAsset.value = false
            if (result.success) {
                _generatedVideoInfo.value = result.text
            } else {
                _errorMessage.value = result.errorMessage ?: "Failed to generate video."
            }
        }
    }

    fun generateMusic(prompt: String, isShortClip: Boolean = true) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Lyria Music Studio."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _generatedAudioBase64.value = null
            val result = restClient.generateMusic(apiKey, prompt, isShortClip)
            _isGeneratingAsset.value = false
            if (result.success && result.audioBase64 != null) {
                _generatedAudioBase64.value = result.audioBase64
            } else {
                _errorMessage.value = result.errorMessage ?: "Failed to generate music."
            }
        }
    }

    fun querySearchGrounding(query: String) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Google Search Grounding."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _groundingResult.value = null
            val result = restClient.generateWithSearchGrounding(apiKey, query)
            _isGeneratingAsset.value = false
            if (result.success) {
                _groundingResult.value = Pair(result.text, result.groundingSources)
            } else {
                _errorMessage.value = result.errorMessage ?: "Search Grounding failed."
            }
        }
    }

    fun queryMapsGrounding(query: String) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Google Maps Grounding."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _groundingResult.value = null
            val result = restClient.generateWithMapsGrounding(apiKey, query)
            _isGeneratingAsset.value = false
            if (result.success) {
                _groundingResult.value = Pair(result.text, emptyList())
            } else {
                _errorMessage.value = result.errorMessage ?: "Maps Grounding failed."
            }
        }
    }

    fun analyzeCameraVision(bitmap: android.graphics.Bitmap, question: String) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Visual AI."
            return
        }

        viewModelScope.launch {
            _isGeneratingAsset.value = true
            _visionAnalysisResult.value = null
            val result = restClient.analyzeImageVision(apiKey, bitmap, question)
            _isGeneratingAsset.value = false
            if (result.success) {
                _visionAnalysisResult.value = result.text
                speakTextResponse(result.text)
            } else {
                _errorMessage.value = result.errorMessage ?: "Vision analysis failed."
            }
        }
    }

    fun playAudioTrack(base64Pcm: String) {
        audioTrackPlayer.enqueueBase64Chunk(base64Pcm)
    }

    // --- SMART HOME IOT ACTIONS ---

    fun toggleSmartDevice(id: Long, power: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val dev = database.smartDeviceDao().getAllDevicesList().firstOrNull { it.id == id }
            if (dev != null) {
                database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = power))
            }
        }
    }

    fun setSmartDeviceValue(id: Long, value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val dev = database.smartDeviceDao().getAllDevicesList().firstOrNull { it.id == id }
            if (dev != null) {
                database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = true, brightnessOrValue = value))
            }
        }
    }

    fun activateSmartScene(sceneName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = database.smartDeviceDao().getAllDevicesList()
            when (sceneName.lowercase()) {
                "bedtime", "sleep" -> {
                    all.forEach { dev ->
                        if (dev.type == "LIGHT" || dev.type == "TV") {
                            database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = false))
                        } else if (dev.type == "AC") {
                            database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = true, brightnessOrValue = 24))
                        }
                    }
                }
                "movie night" -> {
                    all.forEach { dev ->
                        if (dev.type == "LIGHT") {
                            database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = true, brightnessOrValue = 20))
                        } else if (dev.type == "TV") {
                            database.smartDeviceDao().updateDevice(dev.copy(isPoweredOn = true))
                        }
                    }
                }
            }
        }
    }

    // --- KNOWLEDGE VAULT (RAG) ACTIONS ---

    fun addKnowledgeDocument(title: String, content: String, type: String = "TXT") {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = com.example.data.local.DocumentEntity(title = title, content = content, fileType = type)
            database.documentDao().insertDocument(doc)
        }
    }

    fun deleteKnowledgeDocument(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.documentDao().deleteDocumentById(id)
        }
    }

    // --- MEETING SCRIBE ACTIONS ---

    fun createMeetingSummary(title: String, rawTranscript: String) {
        val apiKey = settingsPrefs.getEffectiveApiKey()
        if (apiKey.isBlank()) {
            _errorMessage.value = "API key required for Meeting Scribe."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isGeneratingAsset.value = true
            val prompt = """
                Analyze the following meeting transcript.
                Generate:
                1. EXECUTIVE SUMMARY (2-3 sentences)
                2. KEY DISCUSSION POINTS (bullet points)
                3. ACTION ITEMS & NEXT STEPS (task list with deadlines/owners if mentioned)

                Transcript:
                $rawTranscript
            """.trimIndent()

            val result = restClient.generateContentWithTools(
                apiKey = apiKey,
                model = settingsPrefs.textModel,
                systemInstruction = "You are an executive meeting scribe producing crisp, structured meeting minutes.",
                conversationHistory = emptyList(),
                userPrompt = prompt,
                enableTools = false
            )
            _isGeneratingAsset.value = false

            if (result.success) {
                val fullText = result.text
                val entity = com.example.data.local.MeetingScribeEntity(
                    title = title.ifBlank { "Meeting on ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).format(java.util.Date())}" },
                    executiveSummary = fullText.substringBefore("2. KEY DISCUSSION POINTS").replace("1. EXECUTIVE SUMMARY", "").trim(),
                    keyPoints = fullText.substringAfter("2. KEY DISCUSSION POINTS").substringBefore("3. ACTION ITEMS").trim(),
                    actionItems = fullText.substringAfter("3. ACTION ITEMS").trim(),
                    rawTranscript = rawTranscript
                )
                database.meetingScribeDao().insertMeeting(entity)
            } else {
                _errorMessage.value = result.errorMessage ?: "Failed to generate meeting minutes."
            }
        }
    }

    fun deleteMeetingSummary(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.meetingScribeDao().deleteMeetingById(id)
        }
    }

    // --- FLOATING OVERLAY & BIOMETRICS ---

    fun toggleFloatingOverlay(context: Context) {
        val newState = !_floatingOverlayActive.value
        _floatingOverlayActive.value = newState
        settingsPrefs.floatingOverlayEnabled = newState
        if (newState) {
            com.example.service.ShivaiFloatingOverlayService.start(context)
        } else {
            com.example.service.ShivaiFloatingOverlayService.stop(context)
        }
    }

    fun toggleBiometricSecurity() {
        val newState = !_biometricSecurityActive.value
        _biometricSecurityActive.value = newState
        settingsPrefs.biometricSecurityEnabled = newState
    }

    override fun onCleared() {
        super.onCleared()
        realtimeSpeechRecognizer?.destroy()
        audioRecorder?.stopRecording()
        wakeWordEngine?.stopListening()
        audioTrackPlayer.release()
        ttsFallback?.shutdown()
        app.liveClient.disconnect()
    }
}
