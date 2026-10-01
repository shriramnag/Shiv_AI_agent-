package com.example.ai

import com.example.data.local.MemoryDao
import com.example.data.local.SettingsPreferences
import com.example.service.ShivaiAccessibilityService
import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShivaiBrain(
    private val settingsPrefs: SettingsPreferences,
    private val memoryDao: MemoryDao,
    val toolRegistry: ToolRegistry,
    val liveClient: GeminiLiveClient,
    val restClient: GeminiRestClient
) {

    suspend fun buildSystemPrompt(): String = withContext(Dispatchers.IO) {
        val personalityMode = settingsPrefs.personality
        val toneInstruction = when (personalityMode) {
            "SERIOUS" -> "Maintain a tactical, concise, formal, and direct demeanor. Do not use filler or excessive pleasantries. Prioritize speed and clarity."
            "COMPANION" -> "Be warm, empathetic, engaging, friendly, and supportive, like a trustworthy companion who cares about the user's wellbeing."
            else -> "Be futuristic, efficient, polite, and sharp. Speak with high-tech poise and intelligent helpfulness."
        }

        val memorySection = if (settingsPrefs.memoryEnabled) {
            val memories = memoryDao.getAllMemoriesList()
            if (memories.isNotEmpty()) {
                val memoryList = memories.joinToString("\n") { "- [${it.category}] ${it.key}: ${it.value}" }
                "\n\n[USER LONG-TERM MEMORY]:\n$memoryList"
            } else ""
        } else ""

        val accessibilityActive = ShivaiAccessibilityService.isConnected()
        val screenStatus = if (accessibilityActive) {
            "Shivai Screen Assistant Accessibility Service is ENABLED. You can read screen content and perform clicks/inputs."
        } else {
            "Accessibility Service is currently DISABLED. If user asks to read screen or interact with apps on screen, guide them to enable it."
        }

        val currentDateTime = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.US).format(Date())

        """
        You are Shivai, a cutting-edge futuristic personal AI assistant for Android.
        Current date and time: $currentDateTime.
        $screenStatus
        
        Personality Mode: $personalityMode
        $toneInstruction
        
        $memorySection
        
        Guidelines:
        1. When user asks to open an app, search the web, manage notes, recall memory, control smart home devices, or search their knowledge vault, call the appropriate tool.
        2. For smart home commands ('turn on lights', 'set AC to 22', 'activate bedtime'), use control_smart_home.
        3. For private files, notes, or uploaded documents, use search_knowledge_vault.
        4. If user requests multiple actions (e.g., 'Open YouTube and search for AI tutorials'), plan and execute the actions step-by-step.
        5. Never fabricate successful actions if a tool returns an error. Report the real outcome.
        6. For phone calls, SMS, and WhatsApp messages, prepare the action clearly.
        7. Speak naturally, clearly, and concisely for voice conversations.
        """.trimIndent()
    }
}
