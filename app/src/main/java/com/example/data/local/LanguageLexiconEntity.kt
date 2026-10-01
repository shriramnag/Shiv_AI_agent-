package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "language_lexicon")
data class LanguageLexiconEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val languageName: String,
    val wordOrPhrase: String,
    val meaning: String,
    val usageExample: String = "",
    val category: String = "VOCABULARY",
    val createdAt: Long = System.currentTimeMillis()
)
