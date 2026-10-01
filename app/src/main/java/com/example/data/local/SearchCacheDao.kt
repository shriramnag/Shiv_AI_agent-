package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SearchCacheDao {
    @Query("SELECT * FROM search_cache WHERE query = :query LIMIT 1")
    suspend fun getCachedResult(query: String): SearchCacheEntity?

    @Query("SELECT * FROM search_cache WHERE query LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findMatchingCachedResult(query: String): SearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: SearchCacheEntity)

    @Query("SELECT * FROM search_cache ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentSearches(): List<SearchCacheEntity>

    @Query("DELETE FROM search_cache")
    suspend fun clearCache()
}
