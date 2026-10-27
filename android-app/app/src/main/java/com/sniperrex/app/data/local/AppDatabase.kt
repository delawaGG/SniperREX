package com.sniperrex.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SignalEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun signalDao(): SignalDao
}
