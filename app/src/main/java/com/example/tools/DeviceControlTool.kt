package com.example.tools

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import org.json.JSONArray
import org.json.JSONObject

class DeviceControlTool(private val context: Context) : ShivaiTool {
    override val name = "device_control"
    override val description = "Controls device hardware features: flashlight, vibration, volume, or media playback."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'toggle_flashlight', 'vibrate', 'volume_up', 'volume_down', 'media_play_pause', 'media_next', 'media_previous'")
                })
                put("flashlight_on", JSONObject().apply {
                    put("type", "BOOLEAN")
                    put("description", "Whether to turn flashlight ON (true) or OFF (false)")
                })
            })
            put("required", JSONArray().apply { put("action") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val action = args.optString("action", "")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        return when (action) {
            "toggle_flashlight" -> {
                val state = args.optBoolean("flashlight_on", true)
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                    val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, state)
                        ToolResult(true, "Turned flashlight ${if (state) "ON" else "OFF"}.")
                    } else {
                        ToolResult(false, "No camera flashlight found on device.")
                    }
                } catch (e: Exception) {
                    ToolResult(false, "Failed to control flashlight: ${e.message}")
                }
            }
            "vibrate" -> {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
                ToolResult(true, "Vibrated device.")
            }
            "volume_up" -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                ToolResult(true, "Increased media volume.")
            }
            "volume_down" -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                ToolResult(true, "Decreased media volume.")
            }
            "media_play_pause" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                ToolResult(true, "Toggled media playback.")
            }
            "media_next" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)
                ToolResult(true, "Skipped to next media track.")
            }
            "media_previous" -> {
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                ToolResult(true, "Rewound to previous media track.")
            }
            else -> ToolResult(false, "Unknown device control action: $action")
        }
    }

    private fun sendMediaKeyEvent(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(eventDown)
        audioManager?.dispatchMediaKeyEvent(eventUp)
    }
}
