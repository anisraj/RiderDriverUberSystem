package me.anisjamadar.ridebookinguberriderapp.utils

sealed class RideStatus

data object UnknownRideStatus : RideStatus()

data object RequestedRideStatus : RideStatus()

data object AcceptedRideStatus : RideStatus()

data object RejectedRideStatus : RideStatus()

data object ArrivedAtPickupStatus : RideStatus()

data object StartedRideStatus : RideStatus()

data object CompletedRideStatus : RideStatus()

data object AllDriversBusyStatus : RideStatus()

data object AllDriversOfflineStatus : RideStatus()