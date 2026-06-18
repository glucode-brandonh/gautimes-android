package com.glucode.gautimes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glucode.gautimes.data.local.dao.JourneyDao
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.dao.UserSettingsDao
import com.glucode.gautimes.data.local.entities.JourneyEntity
import com.glucode.gautimes.data.local.entities.JourneyIntermediateStationEntity
import com.glucode.gautimes.data.local.entities.JourneyLegEntity
import com.glucode.gautimes.data.local.entities.JourneyQueryMetadataEntity
import com.glucode.gautimes.data.local.entities.StationEntity
import com.glucode.gautimes.data.local.entities.UserSettingsEntity

@Database(
    entities = [
        StationEntity::class,
        JourneyEntity::class,
        JourneyLegEntity::class,
        JourneyIntermediateStationEntity::class,
        JourneyQueryMetadataEntity::class,
        UserSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GautimesDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun journeyDao(): JourneyDao
    abstract fun userSettingsDao(): UserSettingsDao
}
