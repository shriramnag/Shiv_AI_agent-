package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CodeAnalysisResult(
    val language: String,
    val outputCode: String,
    val explanation: String,
    val identifiedBugs: List<String>,
    val optimizations: List<String>,
    val securityVulnerabilities: List<String>
)

class CodingStudioEngine(private val restClient: GeminiRestClient) {

    /**
     * Autonomous Code Generation and Architecture Synthesis.
     */
    suspend fun generateCode(
        apiKey: String,
        model: String,
        prompt: String,
        targetLanguage: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are Shivai Coding Studio & DevSecOps Engineering Core.
            Write high-performance, secure, clean, production-grade $targetLanguage code for the user.
            Include thorough inline comments, handle edge cases, and ensure top-tier security standards (OWASP, memory safety).
            Provide the complete runnable code inside a single ```$targetLanguage ... ``` block, followed by an explanation of architecture.
        """.trimIndent()

        val result = restClient.generateContentWithTools(
            apiKey = apiKey,
            model = model,
            systemInstruction = systemPrompt,
            conversationHistory = emptyList(),
            userPrompt = "Generate $targetLanguage code for: $prompt",
            enableTools = false
        )

        if (result.success) {
            Pair(true, result.text)
        } else {
            Pair(false, result.errorMessage ?: "Failed to generate code.")
        }
    }

    /**
     * Code Debugger, Bug Fixer, and Security Vulnerability Inspector.
     */
    suspend fun inspectAndFixCode(
        apiKey: String,
        model: String,
        codeSnippet: String,
        issueDescription: String = ""
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are Shivai Cyber Security & Code Debugger Core.
            Analyze the provided code, find all bugs, logical flaws, syntax errors, and security vulnerabilities (e.g. SQL injection, buffer overflow, hardcoded secrets, race conditions).
            Provide:
            1. FIXED & SECURED CODE (in ```code``` block).
            2. LIST OF BUGS FOUND & HOW THEY WERE FIXED.
            3. SECURITY & PERFORMANCE ENHANCEMENTS APPLIED.
        """.trimIndent()

        val userPrompt = if (issueDescription.isNotBlank()) {
            "Here is the issue: $issueDescription\n\nCode to inspect and fix:\n```\n$codeSnippet\n```"
        } else {
            "Inspect, fix any bugs, optimize and secure this code:\n```\n$codeSnippet\n```"
        }

        val result = restClient.generateContentWithTools(
            apiKey = apiKey,
            model = model,
            systemInstruction = systemPrompt,
            conversationHistory = emptyList(),
            userPrompt = userPrompt,
            enableTools = false
        )

        if (result.success) {
            Pair(true, result.text)
        } else {
            Pair(false, result.errorMessage ?: "Code inspection failed.")
        }
    }
}
