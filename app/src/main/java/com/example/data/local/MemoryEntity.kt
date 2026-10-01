package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String, // e.g. USER_PREFERENCES, IMPORTANT_INFO, CONVERSATION_SUMMARY
    val timestamp: Long = System.currentTimeMillis()
)
