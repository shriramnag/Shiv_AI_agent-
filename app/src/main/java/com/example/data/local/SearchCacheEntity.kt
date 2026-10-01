package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_cache")
data class SearchCacheEntity(
    @PrimaryKey
    val query: String,
    val answer: String,
    val source: String = "DuckDuckGo",
    val timestamp: Long = System.currentTimeMillis()
)
