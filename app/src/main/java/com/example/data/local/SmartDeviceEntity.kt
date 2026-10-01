package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "smart_devices")
data class SmartDeviceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "LIGHT", "AC", "PLUG", "TV", "FAN"
    val room: String = "Living Room",
    val isPoweredOn: Boolean = false,
    val brightnessOrValue: Int = 100, // 0-100 or temperature
    val entityId: String = "" // Home Assistant entity ID, e.g. light.living_room
)

@Dao
interface SmartDeviceDao {
    @Query("SELECT * FROM smart_devices ORDER BY room ASC")
    fun getAllDevices(): Flow<List<SmartDeviceEntity>>

    @Query("SELECT * FROM smart_devices ORDER BY room ASC")
    suspend fun getAllDevicesList(): List<SmartDeviceEntity>

    @Query("SELECT * FROM smart_devices WHERE name LIKE '%' || :name || '%' OR room LIKE '%' || :name || '%' LIMIT 1")
    suspend fun findDeviceByName(name: String): SmartDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: SmartDeviceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<SmartDeviceEntity>)

    @Update
    suspend fun updateDevice(device: SmartDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: SmartDeviceEntity)
}
