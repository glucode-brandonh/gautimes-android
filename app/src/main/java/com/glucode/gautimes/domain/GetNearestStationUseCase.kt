package com.glucode.gautimes.domain

import android.location.Location
import com.glucode.gautimes.data.local.entities.StationEntity
import javax.inject.Inject

class GetNearestStationUseCase @Inject constructor() {
    operator fun invoke(
        location:  Location?,
        allStations: List<StationEntity>
    ): StationEntity? {
        if (location == null || allStations.isEmpty()) return null
        
        return allStations.minByOrNull { s ->
            val sLocation = Location("").apply {
                latitude = s.latitude
                longitude = s.longitude
            }
            location.distanceTo(sLocation)
        }
    }
}
