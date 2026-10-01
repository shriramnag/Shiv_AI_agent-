package com.example.tools

import org.json.JSONObject

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: JSONObject? = null,
    val requiresUserConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)

interface ShivaiTool {
    val name: String
    val description: String
    fun getParametersSchema(): JSONObject
    suspend fun execute(args: JSONObject): ToolResult
}
