package com.glucode.gautimes.di

import android.content.Context
import androidx.room.Room
import com.glucode.gautimes.data.local.GautimesDatabase
import com.glucode.gautimes.data.local.dao.JourneyDao
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.dao.UserSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GautimesDatabase =
        Room.databaseBuilder(
            context,
            GautimesDatabase::class.java,
            "gautimes.db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideStationDao(database: GautimesDatabase): StationDao =
        database.stationDao()

    @Provides
    fun provideJourneyDao(database: GautimesDatabase): JourneyDao =
        database.journeyDao()

    @Provides
    fun provideUserSettingsDao(database: GautimesDatabase): UserSettingsDao =
        database.userSettingsDao()
}
