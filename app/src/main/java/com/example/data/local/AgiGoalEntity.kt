package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agi_goals")
data class AgiGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalTitle: String,
    val status: String = "IN_PROGRESS", // "PLANNING", "IN_PROGRESS", "COMPLETED", "FAILED"
    val planStepsJson: String,          // JSON string of steps
    val currentStepIndex: Int = 0,
    val executionLog: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
