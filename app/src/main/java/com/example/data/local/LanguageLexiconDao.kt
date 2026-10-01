package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageLexiconDao {
    @Query("SELECT * FROM language_lexicon ORDER BY createdAt DESC")
    fun getAllLexiconEntries(): Flow<List<LanguageLexiconEntity>>

    @Query("SELECT * FROM language_lexicon WHERE languageName = :language ORDER BY createdAt DESC")
    fun getLexiconByLanguage(language: String): Flow<List<LanguageLexiconEntity>>

    @Query("SELECT DISTINCT languageName FROM language_lexicon")
    fun getDistinctLanguages(): Flow<List<String>>

    @Query("SELECT * FROM language_lexicon")
    suspend fun getAllEntriesSync(): List<LanguageLexiconEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLexicon(entry: LanguageLexiconEntity): Long

    @Query("DELETE FROM language_lexicon WHERE id = :id")
    suspend fun deleteLexiconById(id: Long)

    @Query("DELETE FROM language_lexicon")
    suspend fun clearAll()
}
