package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "meeting_summaries")
data class MeetingScribeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val executiveSummary: String,
    val keyPoints: String, // Bullet points
    val actionItems: String, // Tasks & owners
    val rawTranscript: String,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MeetingScribeDao {
    @Query("SELECT * FROM meeting_summaries ORDER BY timestamp DESC")
    fun getAllMeetings(): Flow<List<MeetingScribeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingScribeEntity): Long

    @Delete
    suspend fun deleteMeeting(meeting: MeetingScribeEntity)

    @Query("DELETE FROM meeting_summaries WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)
}
