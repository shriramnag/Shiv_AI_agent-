package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "knowledge_documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val chunkCount: Int = 1,
    val fileType: String = "TXT",
    val uploadedAt: Long = System.currentTimeMillis()
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM knowledge_documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM knowledge_documents ORDER BY uploadedAt DESC")
    suspend fun getAllDocumentsList(): List<DocumentEntity>

    @Query("SELECT * FROM knowledge_documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchDocuments(query: String): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
