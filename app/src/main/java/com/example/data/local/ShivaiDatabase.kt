package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        MemoryEntity::class,
        ChatMessageEntity::class,
        DocumentEntity::class,
        MeetingScribeEntity::class,
        SmartDeviceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ShivaiDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun memoryDao(): MemoryDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun documentDao(): DocumentDao
    abstract fun meetingScribeDao(): MeetingScribeDao
    abstract fun smartDeviceDao(): SmartDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: ShivaiDatabase? = null

        fun getDatabase(context: Context): ShivaiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShivaiDatabase::class.java,
                    "shivai_database"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
