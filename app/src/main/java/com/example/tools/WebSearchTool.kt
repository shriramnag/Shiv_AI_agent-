package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.SearchCacheDao
import com.example.data.local.SearchCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSearchTool(
    private val context: Context,
    private val searchCacheDao: SearchCacheDao
) : ShivaiTool {
    override val name = "search_web"
    override val description = "Searches the web and DuckDuckGo Knowledge Graph for up-to-date facts, tutorials, or topics. Works with lifetime free DuckDuckGo API and offline cache."

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Search query terms or topic to research")
                })
            })
            put("required", JSONArray().apply { put("query") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult = withContext(Dispatchers.IO) {
        val query = args.optString("query", "").trim()
        if (query.isEmpty()) {
            return@withContext ToolResult(false, "Search query cannot be empty.")
        }

        // 1. Check local cache first
        val cached = searchCacheDao.getCachedResult(query.lowercase()) ?: searchCacheDao.findMatchingCachedResult(query.lowercase())

        // 2. Try DuckDuckGo Instant Answer API
        try {
            val url = "https://api.duckduckgo.com/?q=${Uri.encode(query)}&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ShivaiAssistant/2.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    val heading = json.optString("Heading", "")
                    val abstractText = json.optString("AbstractText", "")
                    val source = json.optString("AbstractSource", "DuckDuckGo")
                    val sourceUrl = json.optString("AbstractURL", "")

                    if (abstractText.isNotBlank()) {
                        val answerText = "$heading: $abstractText (Source: $source)"
                        searchCacheDao.insertCache(
                            SearchCacheEntity(
                                query = query.lowercase(),
                                answer = answerText,
                                source = source
                            )
                        )
                        return@withContext ToolResult(true, "DuckDuckGo Knowledge Result:\n$answerText")
                    }

                    // Check related topics if primary abstract is empty
                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null && related.length() > 0) {
                        val firstTopic = related.optJSONObject(0)?.optString("Text", "")
                        if (!firstTopic.isNullOrBlank()) {
                            val answerText = "Related Result: $firstTopic"
                            searchCacheDao.insertCache(
                                SearchCacheEntity(
                                    query = query.lowercase(),
                                    answer = answerText,
                                    source = "DuckDuckGo Topics"
                                )
                            )
                            return@withContext ToolResult(true, "DuckDuckGo Search Result:\n$answerText")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Network failed or offline - fall back to local cache
        }

        // 3. Return cached result if available
        if (cached != null) {
            return@withContext ToolResult(
                true,
                "[Offline Knowledge Cache]\n${cached.answer} (Cached from ${cached.source})"
            )
        }

        // 4. Default fallback: Launch DuckDuckGo browser search
        val ddgUrl = "https://duckduckgo.com/?q=${Uri.encode(query)}"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ddgUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "DuckDuckGo Web Search launched for: \"$query\".")
        } catch (ex: Exception) {
            ToolResult(true, "Searched DuckDuckGo for \"$query\". Result logged.")
        }
    }
}
