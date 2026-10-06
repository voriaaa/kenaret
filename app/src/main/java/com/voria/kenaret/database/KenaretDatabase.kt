package com.voria.kenaret.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}

@Database(
    entities = [
        UserProfile::class,
        PeriodRecord::class,
        SymptomRecord::class,
        PartnerConnection::class,
        NotificationPreference::class,
        AppSettings::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class KenaretDatabase : RoomDatabase() {
    abstract fun dao(): KenaretDao

    companion object {
        fun create(context: Context): KenaretDatabase =
            Room.databaseBuilder(context.applicationContext, KenaretDatabase::class.java, "kenaret.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
