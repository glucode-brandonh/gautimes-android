package com.glucode.gautimes.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
