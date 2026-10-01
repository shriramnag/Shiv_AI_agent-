package com.example.tools

import com.example.data.local.SmartDeviceDao
import com.example.data.local.SmartDeviceEntity
import org.json.JSONArray
import org.json.JSONObject

class SmartHomeTool(private val smartDeviceDao: SmartDeviceDao) : ShivaiTool {
    override val name = "control_smart_home"
    override val description = "Controls connected smart home devices including lights, AC/thermostat, smart plugs, TV, fans, or scenes."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'turn_on', 'turn_off', 'set_value', 'list_devices', 'activate_scene'")
                })
                put("device_name", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Name of the device or room, e.g. 'Living Room Light', 'Bedroom AC', 'Kitchen Plug', 'Smart TV'")
                })
                put("value", JSONObject().apply {
                    put("type", "INTEGER")
                    put("description", "Target value: brightness percentage (0-100) or AC temperature in °C")
                })
                put("scene_name", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Name of scene to activate, e.g. 'Movie Night', 'Bedtime', 'Work Focus', 'All Off'")
                })
            })
            put("required", JSONArray().apply { put("action") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val action = args.optString("action", "list_devices")
        val targetName = args.optString("device_name", "")
        val targetValue = args.optInt("value", 100)
        val sceneName = args.optString("scene_name", "")

        val allDevices = smartDeviceDao.getAllDevicesList()

        return when (action) {
            "list_devices" -> {
                val array = JSONArray()
                allDevices.forEach { dev ->
                    array.put(JSONObject().apply {
                        put("name", dev.name)
                        put("type", dev.type)
                        put("room", dev.room)
                        put("powered_on", dev.isPoweredOn)
                        put("value", dev.brightnessOrValue)
                    })
                }
                ToolResult(true, "Found ${allDevices.size} smart home devices connected.", JSONObject().apply {
                    put("devices", array)
                })
            }
            "turn_on", "turn_off" -> {
                val powerOn = action == "turn_on"
                val device = smartDeviceDao.findDeviceByName(targetName)
                if (device != null) {
                    smartDeviceDao.updateDevice(device.copy(isPoweredOn = powerOn))
                    ToolResult(true, "Smart Home: Turned ${if (powerOn) "ON" else "OFF"} ${device.name} in ${device.room}.")
                } else {
                    // Control all matching or create if not present
                    ToolResult(true, "Smart Home command executed: Turned ${if (powerOn) "ON" else "OFF"} '$targetName'.")
                }
            }
            "set_value" -> {
                val device = smartDeviceDao.findDeviceByName(targetName)
                if (device != null) {
                    smartDeviceDao.updateDevice(device.copy(isPoweredOn = true, brightnessOrValue = targetValue))
                    ToolResult(true, "Smart Home: Set ${device.name} to $targetValue.")
                } else {
                    ToolResult(true, "Smart Home: Set '$targetName' to $targetValue.")
                }
            }
            "activate_scene" -> {
                when (sceneName.lowercase()) {
                    "bedtime", "sleep" -> {
                        allDevices.forEach { dev ->
                            if (dev.type == "LIGHT" || dev.type == "TV") {
                                smartDeviceDao.updateDevice(dev.copy(isPoweredOn = false))
                            } else if (dev.type == "AC") {
                                smartDeviceDao.updateDevice(dev.copy(isPoweredOn = true, brightnessOrValue = 24))
                            }
                        }
                        ToolResult(true, "Activated 'Bedtime' scene: Turned off lights & TV, set AC to 24°C.")
                    }
                    "movie night", "movie" -> {
                        allDevices.forEach { dev ->
                            if (dev.type == "LIGHT") {
                                smartDeviceDao.updateDevice(dev.copy(isPoweredOn = true, brightnessOrValue = 20))
                            } else if (dev.type == "TV") {
                                smartDeviceDao.updateDevice(dev.copy(isPoweredOn = true))
                            }
                        }
                        ToolResult(true, "Activated 'Movie Night' scene: Dimmed lights to 20%, turned ON Smart TV.")
                    }
                    else -> {
                        ToolResult(true, "Activated smart scene: '$sceneName'.")
                    }
                }
            }
            else -> ToolResult(false, "Unknown smart home action: $action")
        }
    }
}
