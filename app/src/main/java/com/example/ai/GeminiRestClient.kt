package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class RestGenerationResult(
    val success: Boolean,
    val text: String,
    val toolExecutions: List<Pair<String, ToolResult>> = emptyList(),
    val imageBase64: String? = null,
    val audioBase64: String? = null,
    val videoUriOrOp: String? = null,
    val groundingSources: List<String> = emptyList(),
    val errorMessage: String? = null
)

class GeminiRestClient(private val toolRegistry: ToolRegistry) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testApiKey(apiKey: String, model: String = "gemini-3.5-flash"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Pair(false, "API Key is empty.")
        }
        val cleanModel = model.removePrefix("models/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
        val testBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "Respond with 'OK'") })
                    })
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(testBody.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Pair(true, "API Key verified successfully!")
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val message = errJson?.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $bodyStr"
                    Pair(false, "Validation failed: $message")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection error: ${e.message}")
        }
    }

    /**
     * Standard text generation with tool execution and multi-turn context.
     */
    suspend fun generateContentWithTools(
        apiKey: String,
        model: String,
        systemInstruction: String,
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        enableTools: Boolean = true
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext RestGenerationResult(false, "", errorMessage = "Gemini API key is not configured. Go to Settings to add your key.")
        }

        val cleanModel = model.removePrefix("models/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        for ((role, text) in conversationHistory.takeLast(10)) {
            val geminiRole = if (role == "user") "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            })
        }

        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userPrompt) })
            })
        })

        val requestPayload = JSONObject().apply {
            put("contents", contentsArray)
            if (systemInstruction.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }
            if (enableTools) {
                put("tools", toolRegistry.getGeminiToolsDeclaration())
            }
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errJson = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val message = errJson?.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}: $bodyStr"
                    return@withContext RestGenerationResult(false, "", errorMessage = message)
                }

                val responseJson = JSONObject(bodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")

                if (parts == null || parts.length() == 0) {
                    return@withContext RestGenerationResult(true, "Shivai received no content.")
                }

                var textResponse = ""
                val executedTools = mutableListOf<Pair<String, ToolResult>>()

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textResponse += part.getString("text") + " "
                    }
                    if (part.has("functionCall")) {
                        val functionCall = part.getJSONObject("functionCall")
                        val name = functionCall.getString("name")
                        val args = functionCall.optJSONObject("args") ?: JSONObject()
                        val result = toolRegistry.executeTool(name, args)
                        executedTools.add(Pair(name, result))
                    }
                }

                if (executedTools.isNotEmpty() && textResponse.isBlank()) {
                    val toolSummaries = executedTools.joinToString("\n") { (name, res) ->
                        "Action: $name -> ${res.message}"
                    }
                    textResponse = toolSummaries
                }

                RestGenerationResult(true, textResponse.trim(), executedTools)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = "Request failed: ${e.message}")
        }
    }

    /**
     * Search Grounding using Google Search with gemini-3.5-flash
     */
    suspend fun generateWithSearchGrounding(
        apiKey: String,
        userPrompt: String
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
                val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

                // Extract grounding sources
                val sources = mutableListOf<String>()
                val groundingMetadata = candidate?.optJSONObject("groundingMetadata")
                val searchChunks = groundingMetadata?.optJSONArray("groundingChunks")
                if (searchChunks != null) {
                    for (i in 0 until searchChunks.length()) {
                        val chunk = searchChunks.getJSONObject(i)
                        val web = chunk.optJSONObject("web")
                        val uri = web?.optString("uri")
                        val title = web?.optString("title")
                        if (uri != null) sources.add("$title: $uri")
                    }
                }

                RestGenerationResult(true, text, groundingSources = sources)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Maps Grounding using Google Maps with gemini-3.5-flash
     */
    suspend fun generateWithMapsGrounding(
        apiKey: String,
        userPrompt: String
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("googleMaps", JSONObject())
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val candidate = responseJson.optJSONArray("candidates")?.optJSONObject(0)
                val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

                RestGenerationResult(true, text)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Image Generation & Editing using gemini-3.1-flash-image-preview
     */
    suspend fun generateOrEditImage(
        apiKey: String,
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "1:1"
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent?key=$apiKey"

        val parts = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
            if (inputImageBase64 != null) {
                put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", inputImageBase64)
                    })
                })
            }
        }

        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply { put("parts", parts) })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                })
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", "1K")
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext RestGenerationResult(false, "", errorMessage = "HTTP ${response.code}: $bodyStr")
                }

                val responseJson = JSONObject(bodyStr)
                val candidateParts = responseJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")

                var returnedText = ""
                var imageB64: String? = null

                if (candidateParts != null) {
                    for (i in 0 until candidateParts.length()) {
                        val p = candidateParts.getJSONObject(i)
                        if (p.has("text")) returnedText += p.getString("text")
                        if (p.has("inlineData")) {
                            imageB64 = p.getJSONObject("inlineData").optString("data")
                        }
                    }
                }

                RestGenerationResult(true, returnedText.ifBlank { "Image generated successfully." }, imageBase64 = imageB64)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Veo 3 Video Generation & Animation using veo-3.1-fast-generate-preview
     */
    suspend fun generateOrAnimateVideo(
        apiKey: String,
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "16:9" // or "9:16"
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"

        val requestPayload = JSONObject().apply {
            put("prompt", prompt)
            if (inputImageBase64 != null) {
                put("image", JSONObject().apply {
                    put("imageBytes", inputImageBase64)
                    put("mimeType", "image/jpeg")
                })
            }
            put("config", JSONObject().apply {
                put("numberOfVideos", 1)
                put("aspectRatio", aspectRatio)
                put("resolution", "720p")
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val opName = json.optString("name", "")
                RestGenerationResult(true, "Veo 3 video generation initiated. Task ID: $opName", videoUriOrOp = opName)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Lyria Music Generation using lyria-3-clip-preview (up to 30s) or lyria-3-pro-preview
     */
    suspend fun generateMusic(
        apiKey: String,
        prompt: String,
        isShortClip: Boolean = true
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val model = if (isShortClip) "lyria-3-clip-preview" else "lyria-3-pro-preview"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply { put("AUDIO") })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val parts = responseJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")

                var audioB64: String? = null
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val p = parts.getJSONObject(i)
                        if (p.has("inlineData")) {
                            audioB64 = p.getJSONObject("inlineData").optString("data")
                        }
                    }
                }

                RestGenerationResult(true, "Music track synthesized via $model.", audioBase64 = audioB64)
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Audio Transcription using gemini-3.5-transcribe
     */
    suspend fun transcribeAudio(
        apiKey: String,
        audioBytes: ByteArray
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-transcribe:generateContent?key=$apiKey"

        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/wav")
                                put("data", base64)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Transcribe the audio accurately.")
                        })
                    })
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val text = responseJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
                RestGenerationResult(true, text.trim())
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }

    /**
     * Multimodal Live Camera / Image Vision Analysis using gemini-3.5-flash
     */
    suspend fun analyzeImageVision(
        apiKey: String,
        bitmap: Bitmap,
        question: String
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext RestGenerationResult(false, "", errorMessage = "API Key missing.")

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val requestPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", question) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .post(requestPayload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyStr)
                val text = responseJson.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""
                RestGenerationResult(true, text.trim())
            }
        } catch (e: Exception) {
            RestGenerationResult(false, "", errorMessage = e.message)
        }
    }
}
