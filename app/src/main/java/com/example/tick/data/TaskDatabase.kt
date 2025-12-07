package com.example.tick.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class],
    version = 1,
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            // If instance is not null, return it, otherwise create database instance
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

        // Optional: Method to close database (useful for testing)
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}