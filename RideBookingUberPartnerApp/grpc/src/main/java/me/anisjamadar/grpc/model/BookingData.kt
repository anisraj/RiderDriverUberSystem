package me.anisjamadar.grpc.model

data class BookingData(
    val deliveryFrom: String,
    val deliveryTo: String,
    val time: Int,
    val budget: Double,
)
