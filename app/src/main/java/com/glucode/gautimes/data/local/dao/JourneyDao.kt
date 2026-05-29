package com.glucode.gautimes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs

@Dao
interface JourneyDao {
    @Transaction
    @Query("SELECT * FROM journeys WHERE fromStationId = :from AND toStationId = :to")
    suspend fun getJourneysForRoute(from: String, to: String): List<JourneyWithLegs>

    @Transaction
    @Query("SELECT * FROM journeys WHERE fromStationId = :from AND toStationId = :to")
    fun getJourneysStreamForRoute(from: String, to: String): kotlinx.coroutines.flow.Flow<List<JourneyWithLegs>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneys(journeys: List<JourneyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegs(legs: List<JourneyLegEntity>)

    @Query("DELETE FROM journeys WHERE fromStationId = :from AND toStationId = :to")
    suspend fun deleteJourneysForRoute(from: String, to: String)

    @Transaction
    suspend fun updateJourneysForRoute(
        from: String,
        to: String,
        journeys: List<JourneyEntity>,
        legs: List<JourneyLegEntity>,
        metadata: JourneyQueryMetadataEntity
    ) {
        deleteJourneysForRoute(from, to)
        insertJourneys(journeys)
        insertLegs(legs)
        insertMetadata(metadata)
    }

    @Query("SELECT * FROM journey_query_metadata WHERE fromStation = :from AND toStation = :to")
    suspend fun getMetadataForRoute(from: String, to: String): JourneyQueryMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: JourneyQueryMetadataEntity)
}
