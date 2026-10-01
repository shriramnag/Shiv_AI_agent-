package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

class CommunicationTool(
    private val context: Context,
    private val onConfirmationRequired: (prompt: String, onConfirm: () -> Unit) -> Unit
) : ShivaiTool {
    override val name = "communicate"
    override val description = "Prepares or executes phone calls, SMS text messages, WhatsApp messages, or emails."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("type", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Communication channel: 'call', 'sms', 'whatsapp', 'email'")
                })
                put("recipient", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Phone number, contact name, or email address")
                })
                put("message", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Text message body or email content")
                })
                put("subject", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Subject line for email")
                })
            })
            put("required", JSONArray().apply { put("type"); put("recipient") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val type = args.optString("type", "call").lowercase()
        val recipient = args.optString("recipient", "").trim()
        val message = args.optString("message", "")
        val subject = args.optString("subject", "Shivai Assistant Message")

        if (recipient.isEmpty()) {
            return ToolResult(false, "Recipient phone number or contact is required.")
        }

        return when (type) {
            "call" -> {
                val prompt = "Confirm phone call to $recipient?"
                onConfirmationRequired(prompt) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$recipient")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                ToolResult(
                    success = true,
                    message = "Prepared call to $recipient. User confirmation requested.",
                    requiresUserConfirmation = true,
                    confirmationPrompt = prompt
                )
            }
            "sms" -> {
                val prompt = "Confirm sending SMS to $recipient: \"$message\"?"
                onConfirmationRequired(prompt) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient")).apply {
                        putExtra("sms_body", message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                ToolResult(
                    success = true,
                    message = "Prepared SMS to $recipient with text \"$message\". User confirmation requested.",
                    requiresUserConfirmation = true,
                    confirmationPrompt = prompt
                )
            }
            "whatsapp" -> {
                val prompt = "Confirm sending WhatsApp message to $recipient: \"$message\"?"
                onConfirmationRequired(prompt) {
                    val encodedMsg = Uri.encode(message)
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$recipient&text=$encodedMsg")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Open in browser if app not installed
                        val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                }
                ToolResult(
                    success = true,
                    message = "Prepared WhatsApp message to $recipient. User confirmation requested.",
                    requiresUserConfirmation = true,
                    confirmationPrompt = prompt
                )
            }
            "email" -> {
                val prompt = "Confirm opening email to $recipient with subject \"$subject\"?"
                onConfirmationRequired(prompt) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, message)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                ToolResult(
                    success = true,
                    message = "Prepared email to $recipient. User confirmation requested.",
                    requiresUserConfirmation = true,
                    confirmationPrompt = prompt
                )
            }
            else -> ToolResult(false, "Unsupported communication type: $type")
        }
    }
}
