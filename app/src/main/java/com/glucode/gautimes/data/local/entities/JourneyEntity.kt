package com.glucode.gautimes.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey val id: String,
    val fromStationId: String,
    val toStationId: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationSeconds: Int,
    val distanceMetres: Int,
    val totalFareZar: Double,
    val parkingCostZar: Double?
)
