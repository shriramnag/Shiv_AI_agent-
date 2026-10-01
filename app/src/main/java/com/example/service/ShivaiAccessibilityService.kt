package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

class ShivaiAccessibilityService : AccessibilityService() {

    companion object {
        private var serviceRef: WeakReference<ShivaiAccessibilityService>? = null

        fun isConnected(): Boolean {
            return serviceRef?.get() != null
        }

        fun getInstance(): ShivaiAccessibilityService? {
            return serviceRef?.get()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events monitored for screen state updates
    }

    override fun onInterrupt() {
        // Interrupted by system
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceRef?.get() == this) {
            serviceRef = null
        }
    }

    /**
     * Reads all visible text and interactive UI elements on screen into a structured summary for Gemini.
     */
    fun captureScreenHierarchy(): String {
        val root = rootInActiveWindow ?: return "Screen content unavailable or screen is locked."
        val builder = StringBuilder()
        builder.append("=== CURRENT SCREEN CONTENT ===\n")
        traverseNode(root, builder, 0)
        return builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, builder: StringBuilder, depth: Int) {
        if (node == null || depth > 8) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString()?.substringAfterLast(".")
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        if (!text.isNullOrEmpty() || !desc.isNullOrEmpty() || isClickable || isEditable) {
            val indent = "  ".repeat(depth)
            val info = buildList {
                if (!text.isNullOrEmpty()) add("text=\"$text\"")
                if (!desc.isNullOrEmpty()) add("desc=\"$desc\"")
                if (isClickable) add("[Clickable]")
                if (isEditable) add("[Input Field]")
                if (className != null && className != "View") add("type=$className")
            }.joinToString(", ")

            if (info.isNotEmpty()) {
                builder.append("$indent- $info\n")
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, builder, depth + 1)
        }
    }

    /**
     * Finds element with matching text or contentDescription and clicks it.
     */
    fun clickElementWithText(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val matchedNodes = root.findAccessibilityNodeInfosByText(target)
        for (node in matchedNodes) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                val success = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
        }
        return false
    }

    /**
     * Inputs text into currently focused or first found editable element.
     */
    fun typeTextIntoActiveField(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (targetNode == null) {
            targetNode = findFirstEditable(root)
        }
        if (targetNode != null) {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val result = findFirstEditable(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    fun scrollScreen(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return performScrollOnFirstScrollable(root, action)
    }

    private fun performScrollOnFirstScrollable(node: AccessibilityNodeInfo?, action: Int): Boolean {
        if (node == null) return false
        if (node.isScrollable && node.performAction(action)) {
            return true
        }
        for (i in 0 until node.childCount) {
            if (performScrollOnFirstScrollable(node.getChild(i), action)) {
                return true
            }
        }
        return false
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
}
