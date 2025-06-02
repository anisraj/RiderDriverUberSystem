package me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationUpdates(): Flow<Location>

    suspend fun getCurrentLocation(): Location

    class LocationException(message: String): Exception()
}