package com.glucode.gautimes.data.local.entities

import androidx.room.Entity

@Entity(
    tableName = "journey_query_metadata",
    primaryKeys = ["fromStation", "toStation"]
)
data class JourneyQueryMetadataEntity(
    val fromStation: String,
    val toStation: String,
    val lastUpdatedMillis: Long,
    val nextCursor: String? = null
)
