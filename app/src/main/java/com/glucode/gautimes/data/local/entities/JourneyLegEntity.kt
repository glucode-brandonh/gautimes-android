package com.glucode.gautimes.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journey_legs",
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
data class JourneyLegEntity(
    @PrimaryKey val id: String,
    val journeyId: String,
    val mode: String,
    val lineName: String?,
    val lineColour: String?,
    val departureStop: String,
    val arrivalStop: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationSeconds: Int,
    val distanceMetres: Int,
    val headsign: String?,
    val carriages: Int,
    val fareAmountZar: Double,
    val fareIsPeak: Boolean,
    val fareProduct: String?,
    val tripId: String?
)
