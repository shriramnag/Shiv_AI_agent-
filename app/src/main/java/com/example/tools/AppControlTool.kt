package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject

class AppControlTool(private val context: Context) : ShivaiTool {
    override val name = "open_application"
    override val description = "Opens an installed Android application or system settings on the user's phone."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("app_name", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the app to open, e.g. YouTube, Maps, Chrome, Camera, WhatsApp, Spotify, Settings, Calculator, Clock")
                })
            })
            put("required", JSONArray().apply { put("app_name") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val appName = args.optString("app_name", "").trim()
        if (appName.isEmpty()) {
            return ToolResult(false, "No app name provided.")
        }

        val target = appName.lowercase()

        // Handle system settings explicitly
        if (target.contains("setting")) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(true, "Opened Android System Settings.")
        }

        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "whatsapp" to "com.whatsapp",
            "camera" to "com.google.android.GoogleCamera",
            "spotify" to "com.spotify.music",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "photos" to "com.google.android.apps.photos",
            "gmail" to "com.google.android.gm"
        )

        val directPkg = packageMap[target]
        val pm = context.packageManager

        if (directPkg != null) {
            val launchIntent = pm.getLaunchIntentForPackage(directPkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolResult(true, "Successfully launched $appName.")
            }
        }

        // Fuzzy match installed packages
        val installedApps = pm.getInstalledApplications(0)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(target) || target.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolResult(true, "Successfully launched ${pm.getApplicationLabel(app)}.")
                }
            }
        }

        // Fallback: If not found, open Google Play Store search for the app
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$appName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(playStoreIntent)
            ToolResult(true, "App $appName is not installed. Opened Play Store to download it.")
        } catch (e: Exception) {
            ToolResult(false, "Could not find or launch application: $appName.")
        }
    }
}
