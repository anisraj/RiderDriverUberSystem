package me.anisjamadar.ridebookinguberriderapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

class LocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) {
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<Location> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw IllegalStateException("Missing location permission")
            }
            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000L)
                .setFastestInterval(1000L)

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        launch { send(location) }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location {
        return suspendCancellableCoroutine { continuation ->
            if (!context.hasLocationPermission()) {
                throw IllegalStateException("Missing location permission")
            }

            client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resumeWith(Result.success(location))
                } else {
                    continuation.resumeWithException(IllegalStateException("Location is null"))
                }
            }.addOnFailureListener {
                continuation.resumeWithException(it)
            }
        }
    }
}