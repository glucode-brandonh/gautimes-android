package com.glucode.gautimes.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journey_intermediate_stations",
    foreignKeys = [
        ForeignKey(
            entity = JourneyEntity::class,
            parentColumns = ["id"],
            childColumns = ["journeyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["journeyId"])]
)
data class JourneyIntermediateStationEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val id: String, // station slug/id
    val journeyId: String,
    val name: String,
    val arrivalTime: String?,
    val durationSeconds: Int
)
