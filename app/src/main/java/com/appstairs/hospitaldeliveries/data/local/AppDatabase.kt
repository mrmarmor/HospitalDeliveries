package com.appstairs.hospitaldeliveries.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.appstairs.hospitaldeliveries.domain.model.Location
import com.appstairs.hospitaldeliveries.domain.model.TaskStatus

@Database(entities = [TaskDbEntity::class], version = 1, exportSchema = false)
@TypeConverters(EnumConverters::class)//input contains enums that needed to be converted to string
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        // this elvis operator makes it like singleton
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hospital-deliveries.db",
            ).build().also { instance = it }
        }
    }
}

class EnumConverters {
    @TypeConverter fun fromStatus(value: TaskStatus): String = value.name // from enum to sring
    @TypeConverter fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value) // from string to enum
    @TypeConverter fun fromLocation(value: Location): String = value.name
    @TypeConverter fun toLocation(value: String): Location = Location.valueOf(value)
}