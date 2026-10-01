package com.example.tools

import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import org.json.JSONArray
import org.json.JSONObject

class NotesTool(private val noteDao: NoteDao) : ShivaiTool {
    override val name = "manage_notes"
    override val description = "Create, search, view, or delete personal notes for the user."

    override fun getParametersSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                put("action", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Action to perform: 'create', 'search', 'get_recent', 'delete'")
                })
                put("title", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Title of the note (for create action)")
                })
                put("content", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Content or body of the note (for create action)")
                })
                put("query", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Search query keyword (for search action)")
                })
                put("note_id", JSONObject().apply {
                    put("type", "INTEGER")
                    put("description", "ID of note to delete")
                })
            })
            put("required", JSONArray().apply { put("action") })
        }
    }

    override suspend fun execute(args: JSONObject): ToolResult {
        val action = args.optString("action", "create")
        return when (action) {
            "create" -> {
                val title = args.optString("title", "Untitled Note")
                val content = args.optString("content", "")
                if (content.isEmpty() && title.isEmpty()) {
                    return ToolResult(false, "Cannot create empty note.")
                }
                val entity = NoteEntity(
                    title = title,
                    content = content,
                    tag = "Assistant",
                    isVoiceCreated = true
                )
                val id = noteDao.insertNote(entity)
                ToolResult(true, "Note saved successfully with ID $id.", JSONObject().apply {
                    put("note_id", id)
                    put("title", title)
                })
            }
            "search" -> {
                val query = args.optString("query", "")
                val notes = noteDao.searchNotes(query)
                val array = JSONArray()
                notes.forEach { note ->
                    array.put(JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                    })
                }
                ToolResult(true, "Found ${notes.size} matching notes.", JSONObject().apply {
                    put("notes", array)
                })
            }
            "get_recent" -> {
                val notes = noteDao.searchNotes("")
                val recent = notes.take(5)
                val array = JSONArray()
                recent.forEach { note ->
                    array.put(JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                    })
                }
                ToolResult(true, "Retrieved ${recent.size} recent notes.", JSONObject().apply {
                    put("notes", array)
                })
            }
            "delete" -> {
                val id = args.optLong("note_id", -1L)
                if (id != -1L) {
                    noteDao.deleteNoteById(id)
                    ToolResult(true, "Deleted note ID $id.")
                } else {
                    ToolResult(false, "Invalid note ID.")
                }
            }
            else -> ToolResult(false, "Unknown notes action: $action")
        }
    }
}
