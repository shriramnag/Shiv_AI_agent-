package com.example.tools

import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import org.json.JSONArray
import org.json.JSONObject

class MemoryTool(private val memoryDao: MemoryDao) : ShivaiTool {
    override val name = "manage_memory"
    override val description = "Saves, retrieves, or deletes long-term user memories, preferences, and important facts."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'remember', 'recall', 'forget'")
                })
                put("key", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Short identifier or topic (e.g. 'user_name', 'favorite_music')")
                })
                put("value", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Fact or preference to remember")
                })
                put("category", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Category: 'USER_PREFERENCES', 'IMPORTANT_INFO', 'ASSISTANT_PREFERENCES'")
                })
                put("memory_id", JSONObject().apply {
                    put("type", "INTEGER")
                    put("description", "ID of memory to delete (for forget action)")
                })
            })
            put("required", JSONArray().apply { put("action") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val action = args.optString("action", "remember")
        return when (action) {
            "remember" -> {
                val key = args.optString("key", "fact")
                val value = args.optString("value", "")
                val category = args.optString("category", "USER_PREFERENCES")
                if (value.isEmpty()) {
                    return ToolResult(false, "No value provided to remember.")
                }
                val entity = MemoryEntity(key = key, value = value, category = category)
                val id = memoryDao.insertMemory(entity)
                ToolResult(true, "Successfully remembered '$key': $value (ID $id).", JSONObject().apply {
                    put("memory_id", id)
                })
            }
            "recall" -> {
                val list = memoryDao.getAllMemoriesList()
                val array = JSONArray()
                list.forEach { mem ->
                    array.put(JSONObject().apply {
                        put("id", mem.id)
                        put("key", mem.key)
                        put("value", mem.value)
                        put("category", mem.category)
                    })
                }
                ToolResult(true, "Recalled ${list.size} stored memories.", JSONObject().apply {
                    put("memories", array)
                })
            }
            "forget" -> {
                val id = args.optLong("memory_id", -1L)
                if (id != -1L) {
                    memoryDao.deleteMemoryById(id)
                    ToolResult(true, "Removed memory ID $id from Shivai's memory.")
                } else {
                    ToolResult(false, "Invalid memory ID to forget.")
                }
            }
            else -> ToolResult(false, "Unknown memory action: $action")
        }
    }
}
