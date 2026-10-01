package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class WebSearchTool(private val context: Context) : ShivaiTool {
    override val name = "search_web"
    override val description = "Searches the web for up-to-date information, tutorials, or topics requested by the user."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Search query terms")
                })
            })
            put("required", JSONArray().apply { put("query") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val query = args.optString("query", "").trim()
        if (query.isEmpty()) {
            return ToolResult(false, "Search query cannot be empty.")
        }

        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult(true, "Searched the web for: \"$query\".")
        } catch (e: Exception) {
            ToolResult(false, "Failed to launch web browser for query: $query.")
        }
    }
}
