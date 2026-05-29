package com.glucode.gautimes.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

data class JourneyWithLegs(
    @Embedded val journey: JourneyEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "journeyId"
    )
    val legs: List<JourneyLegEntity>
)
