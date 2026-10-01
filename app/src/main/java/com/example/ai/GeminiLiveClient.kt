package com.example.ai

import android.util.Base64
import com.example.tools.ToolRegistry
import com.example.voice.AudioTrackPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class GeminiLiveClient(
    private val audioTrackPlayer: AudioTrackPlayer,
    private val toolRegistry: ToolRegistry,
    private val onTranscriptChunk: (String) -> Unit,
    private val onTurnComplete: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(LiveConnectionState.DISCONNECTED)
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    private var currentModel = "models/gemini-2.5-flash-native-audio-preview-12-2025"
    private var currentVoice = "Aoede"
    private var currentSystemPrompt = ""

    fun connect(
        apiKey: String,
        modelName: String = "gemini-2.5-flash-native-audio-preview-12-2025",
        voiceName: String = "Aoede",
        systemPrompt: String = ""
    ) {
        if (apiKey.isBlank()) {
            _connectionState.value = LiveConnectionState.ERROR
            _lastErrorMessage.value = "Gemini API key is required. Please set it in Settings."
            onError("Gemini API key missing.")
            return
        }

        disconnect()
        _connectionState.value = LiveConnectionState.CONNECTING
        _lastErrorMessage.value = null

        val resolvedModel = if (modelName.contains("3.8") || modelName.isBlank()) {
            "gemini-2.5-flash-native-audio-preview-12-2025"
        } else modelName

        currentModel = if (resolvedModel.startsWith("models/")) resolvedModel else "models/$resolvedModel"
        currentVoice = voiceName
        currentSystemPrompt = systemPrompt

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = LiveConnectionState.CONNECTED
                sendSetupMessage(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = LiveConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = LiveConnectionState.ERROR
                val msg = t.message ?: "WebSocket connection failed"
                _lastErrorMessage.value = msg
                onError(msg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = LiveConnectionState.DISCONNECTED
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        try {
            val setupObj = JSONObject().apply {
                put("model", currentModel)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", currentVoice)
                            })
                        })
                    })
                })
                if (currentSystemPrompt.isNotEmpty()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", currentSystemPrompt) })
                        })
                    })
                }
                put("tools", toolRegistry.getGeminiToolsDeclaration())
            }

            val payload = JSONObject().apply {
                put("setup", setupObj)
            }

            ws.send(payload.toString())
        } catch (e: Exception) {
            _lastErrorMessage.value = "Failed to send setup frame: ${e.message}"
        }
    }

    fun sendAudioPcmChunk(pcm16Bytes: ByteArray) {
        if (_connectionState.value != LiveConnectionState.CONNECTED) return
        val ws = webSocket ?: return

        try {
            val base64 = Base64.encodeToString(pcm16Bytes, Base64.NO_WRAP)
            val chunkObj = JSONObject().apply {
                put("mimeType", "audio/pcm;rate=16000")
                put("data", base64)
            }
            val realtimeInput = JSONObject().apply {
                put("mediaChunks", JSONArray().apply { put(chunkObj) })
            }
            val payload = JSONObject().apply {
                put("realtimeInput", realtimeInput)
            }
            ws.send(payload.toString())
        } catch (e: Exception) {
            // Buffer error
        }
    }

    fun sendTextMessage(text: String) {
        if (_connectionState.value != LiveConnectionState.CONNECTED) return
        val ws = webSocket ?: return

        try {
            val turn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            }
            val clientContent = JSONObject().apply {
                put("turns", JSONArray().apply { put(turn) })
                put("turnComplete", true)
            }
            val payload = JSONObject().apply {
                put("clientContent", clientContent)
            }
            ws.send(payload.toString())
        } catch (e: Exception) {
            // Error sending text
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)

            // Handle server audio / text turn
            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")
                if (serverContent.optBoolean("interrupted", false)) {
                    audioTrackPlayer.stopAndFlush()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("text")) {
                                val transcript = part.getString("text")
                                onTranscriptChunk(transcript)
                            }
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val data = inlineData.getString("data")
                                audioTrackPlayer.enqueueBase64Chunk(data)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    onTurnComplete()
                }
            }

            // Handle function call (tools)
            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    scope.launch {
                        handleToolCalls(functionCalls)
                    }
                }
            }
        } catch (e: Exception) {
            // Parse error
        }
    }

    private suspend fun handleToolCalls(functionCalls: JSONArray) {
        val responsesArray = JSONArray()

        for (i in 0 until functionCalls.length()) {
            val call = functionCalls.getJSONObject(i)
            val name = call.getString("name")
            val args = call.optJSONObject("args") ?: JSONObject()
            val callId = call.optString("id", "call_$i")

            val result = toolRegistry.executeTool(name, args)
            val responseObj = JSONObject().apply {
                put("id", callId)
                put("name", name)
                put("response", JSONObject().apply {
                    put("success", result.success)
                    put("message", result.message)
                    if (result.data != null) {
                        put("data", result.data)
                    }
                })
            }
            responsesArray.put(responseObj)
        }

        val toolResponsePayload = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", responsesArray)
            })
        }

        webSocket?.send(toolResponsePayload.toString())
    }

    fun interruptCurrentPlayback() {
        audioTrackPlayer.stopAndFlush()
        // Signal interruption to websocket if supported
        try {
            val interruptMsg = JSONObject().apply {
                put("clientContent", JSONObject().apply {
                    put("turnComplete", true)
                })
            }
            webSocket?.send(interruptMsg.toString())
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun disconnect() {
        _connectionState.value = LiveConnectionState.DISCONNECTED
        audioTrackPlayer.stopAndFlush()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (e: Exception) {
            // Ignored
        }
        webSocket = null
    }
}
