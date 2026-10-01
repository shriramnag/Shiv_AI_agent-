package com.example.tools

import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry(toolsList: List<ShivaiTool>) {
    private val toolsByName = toolsList.associateBy { it.name }

    val allTools: List<ShivaiTool> get() = toolsByName.values.toList()

    fun getGeminiToolsDeclaration(): JSONArray {
        val funcDecls = JSONArray()
        for (tool in toolsByName.values) {
            val decl = JSONObject().apply {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", tool.getParametersSchema())
            }
            funcDecls.put(decl)
        }

        val wrapper = JSONObject().apply {
            put("functionDeclarations", funcDecls)
        }

        return JSONArray().apply { put(wrapper) }
    }

    suspend fun executeTool(name: String, args: JSONObject): ToolResult {
        val tool = toolsByName[name]
            ?: return ToolResult(false, "Unknown tool: '$name'. Available tools: ${toolsByName.keys.joinToString()}")
        return try {
            tool.execute(args)
        } catch (e: Exception) {
            ToolResult(false, "Error executing tool '$name': ${e.message}")
        }
    }
}
