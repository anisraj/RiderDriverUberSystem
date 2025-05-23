package me.anisjamadar.grpc

object GrpcConstant {
    const val GRPC_HOST = "10.0.2.2"
    const val GRPC_PORT = 50051
    const val COUNTDOWN_TIME_IN_SECONDS = 30

    enum class DriverAction(private val action: Int) {
        UNKNOWN(0),
        REQUESTED_RIDE(1),
        ACCEPTED(2),
        REJECTED(3),
        ARRIVED_AT_PICKUP(4),
        RIDE_STARTED(5),
        RIDE_COMPLETED(6),
        DRIVER_OFFLINE(7);

        companion object {
            fun fromAction(action: Int): DriverAction {
                return entries.find { it.action == action } ?: UNKNOWN
            }
        }
    }
}
