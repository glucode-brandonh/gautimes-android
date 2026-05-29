package com.glucode.gautimes.di

import android.content.Context
import androidx.room.Room
import com.glucode.gautimes.data.local.GautimesDatabase
import com.glucode.gautimes.data.local.dao.StationDao
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
        ).build()

    @Provides
    fun provideStationDao(database: GautimesDatabase): StationDao =
        database.stationDao()
}
