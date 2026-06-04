package com.glucode.gautimes.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PermissionRepository {
    fun isLocationPermissionGranted(): Boolean
    fun isLocationCardDismissed(): Boolean
    fun dismissLocationCard()
}

@Singleton
class DefaultPermissionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {

    private val sharedPreferences = context.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)

    override fun isLocationPermissionGranted(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    override fun isLocationCardDismissed(): Boolean {
        return sharedPreferences.getBoolean("location_card_dismissed", false)
    }

    override fun dismissLocationCard() {
        sharedPreferences.edit {
            putBoolean("location_card_dismissed", true)
        }
    }
}
