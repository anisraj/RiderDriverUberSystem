package me.anisjamadar.grpc

import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant.COUNTDOWN_TIME_IN_SECONDS
import me.anisjamadar.grpc.GrpcConstant.GRPC_HOST
import me.anisjamadar.grpc.GrpcConstant.GRPC_PORT
import me.anisjamadar.grpc.flows.FlowOfBookingResponse
import me.anisjamadar.grpc.flows.FlowOfLocationResponse
import rider.Rider.DriverLocationResponse
import rider.RiderServiceGrpcKt
import rider.rideRequest

object GrpcHandler {
    private var managedChannel: ManagedChannel? = null
    private lateinit var riderServiceCoroutineStub: RiderServiceGrpcKt.RiderServiceCoroutineStub
    private lateinit var scope: CoroutineScope

    fun startGrpc() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        if (managedChannel == null) {
            managedChannel = ManagedChannelBuilder
                .forAddress(GRPC_HOST, GRPC_PORT)
                .usePlaintext()
                .executor(Dispatchers.IO.asExecutor())
                .build()
        }
        riderServiceCoroutineStub = RiderServiceGrpcKt.RiderServiceCoroutineStub(managedChannel!!)
    }

    fun sendRideRequest(
        pickupLatitude: Double, pickupLongitude: Double,
        dropLatitude: Double, dropLongitude: Double,
        pickupAddress: String, dropAddress: String,
        distanceInKm: Double
    ) {
        val request = rideRequest {
            this.time = COUNTDOWN_TIME_IN_SECONDS
            this.pickupLatitude = pickupLatitude
            this.pickupLongitude = pickupLongitude
            this.dropLatitude = dropLatitude
            this.dropLongitude = dropLongitude
            this.pickupAddress = pickupAddress
            this.dropAddress = dropAddress
            this.distanceInKm = distanceInKm
        }

        scope.launch {
            riderServiceCoroutineStub
                .requestRide(request)
                .catch {
                    it.printStackTrace()
                }
                .collect {
                    FlowOfBookingResponse.emitBooking(it.action)
                }
        }
    }

    fun getDriverLocationUpdates() {
        scope.launch {
            riderServiceCoroutineStub
                .driverLocation(Empty.getDefaultInstance())
                .collect {
                    FlowOfLocationResponse.emitLocationUpdates(it.latitude, it.longitude)
                }
        }
    }

    fun stopGrpc() {
        scope.cancel()
        managedChannel?.shutdown()
        managedChannel = null
    }

}
