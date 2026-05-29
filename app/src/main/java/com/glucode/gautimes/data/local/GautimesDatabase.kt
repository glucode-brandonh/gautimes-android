package com.glucode.gautimes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glucode.gautimes.data.local.dao.StationDao
import com.glucode.gautimes.data.local.entities.StationEntity

@Database(entities = [StationEntity::class], version = 1, exportSchema = false)
abstract class GautimesDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
}
