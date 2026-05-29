package com.glucode.gautimes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.glucode.gautimes.data.local.entities.StationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations ORDER BY name ASC")
    fun getStations(): Flow<List<StationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<StationEntity>)

    @Query("DELETE FROM stations")
    suspend fun deleteAllStations()
}
