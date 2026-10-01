package com.example.tools

import com.example.service.ShivaiAccessibilityService
import org.json.JSONArray
import org.json.JSONObject

class ScreenTool : ShivaiTool {
    override val name = "screen_interaction"
    override val description = "Reads visible screen elements or interacts with screen via tap, typing, scrolling, or navigation using Accessibility Service."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'read_screen', 'click_text', 'type_text', 'scroll_down', 'scroll_up', 'go_back', 'go_home', 'open_recents'")
                })
                put("target_text", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Text of button or element to click (for click_text action)")
                })
                put("input_text", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Text to type into active input field (for type_text action)")
                })
            })
            put("required", JSONArray().apply { put("action") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val service = ShivaiAccessibilityService.getInstance()
        if (service == null) {
            return ToolResult(
                success = false,
                message = "Shivai Screen Assistant Accessibility Service is currently disabled. Please enable it in Settings -> Accessibility -> Shivai Screen Assistant to allow screen reading and interaction."
            )
        }

        val action = args.optString("action", "read_screen")
        return when (action) {
            "read_screen" -> {
                val summary = service.captureScreenHierarchy()
                ToolResult(true, "Screen hierarchy read successfully.", JSONObject().apply {
                    put("screen_summary", summary)
                })
            }
            "click_text" -> {
                val target = args.optString("target_text", "")
                if (target.isEmpty()) {
                    ToolResult(false, "target_text parameter is required for click_text.")
                } else {
                    val clicked = service.clickElementWithText(target)
                    if (clicked) {
                        ToolResult(true, "Successfully clicked element containing '$target'.")
                    } else {
                        ToolResult(false, "Could not find clickable element with text '$target' on current screen.")
                    }
                }
            }
            "type_text" -> {
                val input = args.optString("input_text", "")
                val typed = service.typeTextIntoActiveField(input)
                if (typed) {
                    ToolResult(true, "Successfully typed '$input' into input field.")
                } else {
                    ToolResult(false, "No active editable input field found on screen to type text into.")
                }
            }
            "scroll_down" -> {
                val scrolled = service.scrollScreen(true)
                ToolResult(scrolled, if (scrolled) "Scrolled down." else "Could not scroll down.")
            }
            "scroll_up" -> {
                val scrolled = service.scrollScreen(false)
                ToolResult(scrolled, if (scrolled) "Scrolled up." else "Could not scroll up.")
            }
            "go_back" -> {
                val ok = service.performBack()
                ToolResult(ok, if (ok) "Performed Back navigation." else "Failed to navigate back.")
            }
            "go_home" -> {
                val ok = service.performHome()
                ToolResult(ok, if (ok) "Navigated Home." else "Failed to navigate home.")
            }
            "open_recents" -> {
                val ok = service.performRecents()
                ToolResult(ok, if (ok) "Opened Recent Apps." else "Failed to open recents.")
            }
            else -> ToolResult(false, "Unsupported screen action: $action")
        }
    }
}
