package com.voria.kenaret.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface KenaretDao {
    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfile?

    @Upsert
    suspend fun upsertProfile(profile: UserProfile)

    // Periods
    @Query("SELECT * FROM period_records ORDER BY startDate ASC")
    fun observePeriods(): Flow<List<PeriodRecord>>

    @Query("SELECT * FROM period_records ORDER BY startDate ASC")
    suspend fun getPeriods(): List<PeriodRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeriod(record: PeriodRecord): Long

    @Update
    suspend fun updatePeriod(record: PeriodRecord)

    @Query("DELETE FROM period_records WHERE id = :id")
    suspend fun deletePeriod(id: Long)

    // Symptoms
    @Query("SELECT * FROM symptom_records ORDER BY date ASC")
    fun observeSymptoms(): Flow<List<SymptomRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(records: List<SymptomRecord>)

    @Query("DELETE FROM symptom_records WHERE date = :date")
    suspend fun deleteSymptomsOn(date: LocalDate)

    // Partner
    @Query("SELECT * FROM partner_connection WHERE id = 1")
    fun observePartner(): Flow<PartnerConnection?>

    @Query("SELECT * FROM partner_connection WHERE id = 1")
    suspend fun getPartner(): PartnerConnection?

    @Upsert
    suspend fun upsertPartner(connection: PartnerConnection)

    // Notifications
    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    fun observeNotificationPreference(): Flow<NotificationPreference?>

    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    suspend fun getNotificationPreference(): NotificationPreference?

    @Upsert
    suspend fun upsertNotificationPreference(preference: NotificationPreference)

    // App settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettings?>

    @Upsert
    suspend fun upsertSettings(settings: AppSettings)
}
