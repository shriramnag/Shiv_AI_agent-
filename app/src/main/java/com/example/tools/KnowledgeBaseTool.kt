package com.example.tools

import com.example.data.local.DocumentDao
import org.json.JSONArray
import org.json.JSONObject

class KnowledgeBaseTool(private val documentDao: DocumentDao) : ShivaiTool {
    override val name = "search_knowledge_vault"
    override val description = "Searches the user's uploaded personal documents, PDF notes, files, and private knowledge base."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Search keywords or concepts to locate within user's uploaded documents")
                })
            })
            put("required", JSONArray().apply { put("query") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val query = args.optString("query", "").trim()
        if (query.isEmpty()) {
            return ToolResult(false, "Query cannot be empty.")
        }

        val docs = documentDao.searchDocuments(query)
        if (docs.isEmpty()) {
            return ToolResult(true, "No documents in Knowledge Vault matched '$query'.")
        }

        val array = JSONArray()
        docs.forEach { doc ->
            array.put(JSONObject().apply {
                put("title", doc.title)
                put("snippet", doc.content.take(600))
            })
        }

        return ToolResult(true, "Retrieved ${docs.size} relevant document excerpts.", JSONObject().apply {
            put("results", array)
        })
    }
}
