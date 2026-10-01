package com.example.ai

import android.content.Context
import com.example.data.local.LanguageLexiconDao
import com.example.data.local.MemoryDao
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.security.CyberShieldEngine
import com.example.tools.AppControlTool
import com.example.tools.DeviceControlTool
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfflineResponse(
    val replyText: String,
    val actionExecuted: String? = null,
    val isHandledLocally: Boolean = true
)

class OfflineCognitiveEngine(
    private val context: Context,
    private val noteDao: NoteDao,
    private val memoryDao: MemoryDao,
    private val lexiconDao: LanguageLexiconDao,
    private val deviceControlTool: DeviceControlTool,
    private val appControlTool: AppControlTool,
    private val cyberShieldEngine: CyberShieldEngine
) {

    suspend fun processOfflineCommand(query: String): OfflineResponse {
        val clean = query.trim().lowercase(Locale.ROOT)

        // 1. Device Hardware Controls (Torch / Flashlight)
        if (clean.contains("torch on") || clean.contains("flashlight on") || clean.contains("टॉर्च जलाओ") || clean.contains("टॉर्च ऑन")) {
            val result = deviceControlTool.execute(JSONObject().apply {
                put("action", "toggle_flashlight")
                put("flashlight_on", true)
            })
            return OfflineResponse(result.message, "FLASHLIGHT_ON")
        }
        if (clean.contains("torch off") || clean.contains("flashlight off") || clean.contains("टॉर्च बंद") || clean.contains("टॉर्च ऑफ")) {
            val result = deviceControlTool.execute(JSONObject().apply {
                put("action", "toggle_flashlight")
                put("flashlight_on", false)
            })
            return OfflineResponse(result.message, "FLASHLIGHT_OFF")
        }

        // 2. Volume Controls & Vibration
        if (clean.contains("volume") || clean.contains("आवाज")) {
            val action = if (clean.contains("down") || clean.contains("कम")) "volume_down" else "volume_up"
            val result = deviceControlTool.execute(JSONObject().apply {
                put("action", action)
            })
            return OfflineResponse(result.message, "VOLUME_CONTROL")
        }
        if (clean.contains("vibrate") || clean.contains("कंपन")) {
            val result = deviceControlTool.execute(JSONObject().apply {
                put("action", "vibrate")
            })
            return OfflineResponse(result.message, "VIBRATE")
        }

        // 3. App Launching
        if (clean.startsWith("open ") || clean.startsWith("खोलो ") || clean.contains("लांच करो")) {
            val appQuery = clean.removePrefix("open ").removePrefix("खोलो ").trim()
            val result = appControlTool.execute(JSONObject().apply {
                put("app_name", appQuery)
            })
            return OfflineResponse(result.message, "OPEN_APP")
        }

        // 4. Time & Date Queries
        if (clean.contains("time") || clean.contains("समय") || clean.contains("कितने बजे")) {
            val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return OfflineResponse("वर्तमान समय $time है।", "GET_TIME")
        }
        if (clean.contains("date") || clean.contains("तारीख") || clean.contains("दिन")) {
            val date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            return OfflineResponse("आज की तारीख $date है।", "GET_DATE")
        }

        // 5. Notes & Quick Memory
        if (clean.startsWith("note ") || clean.startsWith("नोट ") || clean.startsWith("याद रखो ")) {
            val content = query.substringAfter(" ").trim()
            noteDao.insertNote(NoteEntity(title = "त्वरित नोट (Offline)", content = content, tag = "OFFLINE_NOTE"))
            return OfflineResponse("नोट सुरक्षित कर लिया गया है: \"$content\"", "SAVE_NOTE")
        }

        // 6. Security Audit Command
        if (clean.contains("security scan") || clean.contains("सुरक्षा जांच") || clean.contains("audit security")) {
            val report = cyberShieldEngine.auditDeviceSecurity()
            val msg = "सुरक्षा रिपोर्ट: स्कोर ${report.overallSecurityScore}%\nस्थिति: ${report.securityStatusLabel}\nरूटेड: ${if (report.isRooted) "हाँ (असुरक्षित)" else "नहीं (सुरक्षित)"}\nUSB डिबगिंग: ${if (report.isUsbDebuggingActive) "सक्रिय" else "बंद"}"
            return OfflineResponse(msg, "AUDIT_SECURITY")
        }

        // 7. Check Local Linguistic Lexicon
        try {
            val allLexicon = lexiconDao.getAllEntriesSync()
            val matchedWord = allLexicon.firstOrNull { clean.contains(it.wordOrPhrase.lowercase(Locale.ROOT)) }
            if (matchedWord != null) {
                return OfflineResponse(
                    "भाषा: ${matchedWord.languageName}\nशब्द: ${matchedWord.wordOrPhrase}\nअर्थ: ${matchedWord.meaning}\nउदाहरण: ${matchedWord.usageExample}",
                    "LEXICON_LOOKUP"
                )
            }
        } catch (e: Exception) {
            // ignore
        }

        // 8. General Identity & Fallback
        if (clean.contains("who are you") || clean.contains("तुम कौन हो") || clean.contains("शिवाय")) {
            return OfflineResponse("मैं शिवाय (Shivai) हूँ, आपका ऑटोनॉमस साइबर AI असिस्टेंट। वर्तमान में मैं ऑफ़लाइन मोड में काम कर रहा हूँ। आप टॉर्च, वॉल्यूम, ऐप लॉन्च, नोट्स या साइबर सुरक्षा कमांड दे सकते हैं।", "IDENTITY")
        }

        return OfflineResponse(
            "ऑफलाइन मोड में यह कमांड उपलब्ध नहीं है। इंटरनेट कनेक्ट होने पर मैं इस पर पूरा रिसर्च और जनरेशन कर सकूँगा। आप ऑफलाइन रहते हुए ऐप्स खोल सकते हैं, टॉर्च, वॉल्यूम, नोट्स या सुरक्षा स्कैन चला सकते हैं।",
            "OFFLINE_FALLBACK"
        )
    }
}
