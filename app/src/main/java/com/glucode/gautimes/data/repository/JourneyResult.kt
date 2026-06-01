package com.glucode.gautimes.data.repository

import com.glucode.gautimes.data.local.entities.JourneyWithLegs

sealed class JourneyResult {
    data object Loading : JourneyResult()
    data class Success(val journeys: List<JourneyWithLegs>, val isStale: Boolean = false) : JourneyResult()
    data class Error(val message: String) : JourneyResult()
}
