package com.glucode.gautimes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyIntermediateStationEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
import com.glucode.gautimes.data.local.entities.JourneyWithLegs

@Dao
interface JourneyDao {
    @Transaction
    @Query("SELECT * FROM journeys WHERE fromStationId = :from AND toStationId = :to")
    suspend fun getJourneysForRoute(from: String, to: String): List<JourneyWithLegs>

    @Transaction
    @Query("SELECT * FROM journeys WHERE fromStationId = :from AND toStationId = :to ORDER BY departureTime ASC")
    fun getJourneysStreamForRoute(from: String, to: String): kotlinx.coroutines.flow.Flow<List<JourneyWithLegs>>

    @Transaction
    @Query("SELECT * FROM journeys WHERE id = :id")
    suspend fun getJourneyById(id: String): JourneyWithLegs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneys(journeys: List<JourneyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegs(legs: List<JourneyLegEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntermediateStations(stations: List<JourneyIntermediateStationEntity>)

    @Query("DELETE FROM journeys WHERE fromStationId = :from AND toStationId = :to")
    suspend fun deleteJourneysForRoute(from: String, to: String)

    @Transaction
    suspend fun updateJourneysForRoute(
        from: String,
        to: String,
        journeys: List<JourneyEntity>,
        legs: List<JourneyLegEntity>,
        intermediateStations: List<JourneyIntermediateStationEntity>,
        metadata: JourneyQueryMetadataEntity
    ) {
        deleteJourneysForRoute(from, to)
        insertJourneys(journeys)
        insertLegs(legs)
        insertIntermediateStations(intermediateStations)
        insertMetadata(metadata)
    }

    @Transaction
    suspend fun appendJourneysForRoute(
        journeys: List<JourneyEntity>,
        legs: List<JourneyLegEntity>,
        intermediateStations: List<JourneyIntermediateStationEntity>,
        metadata: JourneyQueryMetadataEntity
    ) {
        insertJourneys(journeys)
        insertLegs(legs)
        insertIntermediateStations(intermediateStations)
        insertMetadata(metadata)
    }

    @Query("SELECT * FROM journey_query_metadata WHERE fromStation = :from AND toStation = :to")
    suspend fun getMetadataForRoute(from: String, to: String): JourneyQueryMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: JourneyQueryMetadataEntity)

    @Query("DELETE FROM journeys")
    suspend fun deleteAllJourneys()

    @Query("DELETE FROM journey_legs")
    suspend fun deleteAllLegs()

    @Query("DELETE FROM journey_intermediate_stations")
    suspend fun deleteAllIntermediateStations()

    @Query("DELETE FROM journey_query_metadata")
    suspend fun deleteAllMetadata()

    @Transaction
    suspend fun deleteAll() {
        deleteAllJourneys()
        deleteAllLegs()
        deleteAllIntermediateStations()
        deleteAllMetadata()
    }
}
