package com.example.tick.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, Subtask::class], // Added Subtask entity
    version = 3,  // INCREMENTED VERSION FROM 2 TO 3
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao // Added SubtaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .fallbackToDestructiveMigration() // Recreates database if schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}