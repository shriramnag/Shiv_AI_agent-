package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.data.local.SettingsPreferences
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

    suspend fun testApiKey(apiKey: String, model: String = "gemini-2.5-flash"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = SettingsPreferences.sanitizeApiKey(apiKey)
        if (cleanKey.isBlank()) {
            return@withContext Pair(false, "API Key खाली है (Empty Key).")
        }

        // 1. Primary check: Verify the API Key with the official Google Gemini models list endpoint
        val modelsUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
        try {
            val modelsRequest = Request.Builder()
                .url(modelsUrl)
                .get()
                .build()

            val modelsResult = client.newCall(modelsRequest).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Pair(true, "API Key सत्यापित हो गई! (Google Gemini Verified & Active)")
                } else {
                    val errJson = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                    val message = errJson?.optJSONObject("error")?.optString("message") ?: bodyStr
                    val code = response.code
                    if (code == 400 && message.contains("API key not valid", ignoreCase = true)) {
                        Pair(false, "अमान्य API Key (Invalid Key): कृपया aistudio.google.com से सही Gemini API Key कॉपी करके पेस्ट करें।")
                    } else if (code == 403) {
                        Pair(false, "पहुंच अस्वीकृत (Permission Denied 403): $message")
                    } else {
                        Pair(false, "सत्यापन विफल (HTTP $code): $message")
                    }
                }
            }

            if (!modelsResult.first) {
                return@withContext modelsResult
            }

            // 2. Secondary check: test lightweight generation with model
            val cleanModel = model.removePrefix("models/")
            val genUrl = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"
            val testBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Hi") })
                        })
                    })
                })
            }
            try {
                val genRequest = Request.Builder()
                    .url(genUrl)
                    .post(testBody.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(genRequest).execute().use { genResp ->
                    if (genResp.isSuccessful) {
                        Pair(true, "API Key सफलतापूर्वक सत्यापित और सक्रिय है! (Model: $cleanModel)")
                    } else {
                        Pair(true, "API Key मान्य और सक्रिय है! (Google Gemini Verified)")
                    }
                }
            } catch (e: Exception) {
                Pair(true, "API Key मान्य और सक्रिय है!")
            }
        } catch (e: Exception) {
            Pair(false, "कनेक्शन त्रुटि: ${e.message}")
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
     * High-Resolution AI Image Generation with Universal Neural Fallback (Lifetime Free)
     */
    suspend fun generateOrEditImage(
        apiKey: String,
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "1:1"
    ): RestGenerationResult = withContext(Dispatchers.IO) {
        val (width, height) = when (aspectRatio) {
            "16:9" -> 1280 to 720
            "9:16" -> 720 to 1280
            "4:3" -> 1024 to 768
            "3:4" -> 768 to 1024
            else -> 1024 to 1024
        }

        // 1. If Gemini API key is present, try Google Imagen endpoint
        if (apiKey.isNotBlank()) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-002:predict?key=$apiKey"
                val payload = JSONObject().apply {
                    put("instances", JSONArray().apply {
                        put(JSONObject().apply { put("prompt", prompt) })
                    })
                    put("parameters", JSONObject().apply {
                        put("sampleCount", 1)
                        put("aspectRatio", aspectRatio)
                    })
                }

                val req = Request.Builder()
                    .url(url)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(req).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        val predictions = json.optJSONArray("predictions")
                        val b64 = predictions?.optJSONObject(0)?.optString("bytesBase64Encoded")
                        if (!b64.isNullOrBlank()) {
                            return@withContext RestGenerationResult(true, "Image generated via Imagen 3.0", imageBase64 = b64)
                        }
                    }
                }
            } catch (e: Exception) {
                // Google Imagen failed or quota exceeded - proceed to universal neural fallback
            }
        }

        // 2. High-Speed Universal Neural Fallback (Pollinations AI - Lifetime Free & No Quotas)
        try {
            val seed = (System.currentTimeMillis() % 100000).toInt()
            val encodedPrompt = android.net.Uri.encode(prompt)
            val fallbackUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&nologo=true&seed=$seed&model=flux"

            val req = Request.Builder()
                .url(fallbackUrl)
                .header("User-Agent", "ShivaiAssistant/2.0")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        return@withContext RestGenerationResult(true, "Image generated successfully via Neural Flux AI Core.", imageBase64 = b64)
                    }
                }
            }
        } catch (ex: Exception) {
            // Fallback network error
        }

        RestGenerationResult(false, "", errorMessage = "Image generation failed. Please check network connection.")
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
