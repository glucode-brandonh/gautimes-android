package com.glucode.gautimes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glucode.gautimes.data.local.dao.JourneyDao
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
import com.glucode.gautimes.data.local.entities.StationEntity

@Database(
    entities = [
        StationEntity::class,
        JourneyEntity::class,
        JourneyLegEntity::class,
        JourneyQueryMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GautimesDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun journeyDao(): JourneyDao
}
