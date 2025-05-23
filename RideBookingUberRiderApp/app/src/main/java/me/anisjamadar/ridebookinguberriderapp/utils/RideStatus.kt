package me.anisjamadar.ridebookinguberriderapp.utils

sealed class RideStatus

data object UnknownRideStatus : RideStatus()

data object RequestedRideStatus : RideStatus()

data class AcceptedRideStatus(
    val latitude: Double, val longitude: Double
) : RideStatus()

data object RejectedRideStatus : RideStatus()

data object ArrivedAtPickupStatus : RideStatus()

data object StartedRideStatus : RideStatus()

data object CompletedRideStatus : RideStatus()

data object AllDriversBusyStatus : RideStatus()

data object AllDriversOfflineStatus : RideStatus()