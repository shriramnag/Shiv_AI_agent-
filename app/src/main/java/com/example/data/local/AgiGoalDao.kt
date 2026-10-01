package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgiGoalDao {
    @Query("SELECT * FROM agi_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<AgiGoalEntity>>

    @Query("SELECT * FROM agi_goals WHERE status = 'IN_PROGRESS' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveGoal(): AgiGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: AgiGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: AgiGoalEntity)

    @Query("DELETE FROM agi_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("DELETE FROM agi_goals")
    suspend fun clearAllGoals()
}
