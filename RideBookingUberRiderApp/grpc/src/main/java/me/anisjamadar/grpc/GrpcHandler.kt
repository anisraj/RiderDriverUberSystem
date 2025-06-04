package me.anisjamadar.grpc

import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant.COUNTDOWN_TIME_IN_SECONDS
import me.anisjamadar.grpc.GrpcConstant.GRPC_HOST
import me.anisjamadar.grpc.GrpcConstant.GRPC_PORT
import me.anisjamadar.grpc.flows.FlowOfBookingResponse
import me.anisjamadar.grpc.flows.FlowOfLocationResponse
import rider.Rider.DriverLocationResponse
import rider.Rider.RideRequest
import rider.Rider.RideResponse
import rider.RiderServiceGrpc

object GrpcHandler {
    private var managedChannel: ManagedChannel? = null
    private lateinit var riderServiceGrpcStub: RiderServiceGrpc.RiderServiceStub
    private lateinit var scope: CoroutineScope

    fun startGrpc() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        if (managedChannel == null) {
            managedChannel = ManagedChannelBuilder
                .forAddress(GRPC_HOST, GRPC_PORT)
                .usePlaintext()
                .build()
        }
        riderServiceGrpcStub = RiderServiceGrpc.newStub(managedChannel)
    }

    fun sendRideRequest(
        pickupLatitude: Double, pickupLongitude: Double,
        dropLatitude: Double, dropLongitude: Double,
        pickupAddress: String, dropAddress: String,
        distanceInKm: Double
    ) {
        val request = RideRequest.newBuilder()
            .setTime(COUNTDOWN_TIME_IN_SECONDS)
            .setPickupLatitude(pickupLatitude)
            .setPickupLongitude(pickupLongitude)
            .setDropLatitude(dropLatitude)
            .setDropLongitude(dropLongitude)
            .setPickupAddress(pickupAddress)
            .setDropAddress(dropAddress)
            .setDistanceInKm(distanceInKm)
            .build()

        val responseObserver = object : StreamObserver<RideResponse> {
            override fun onNext(value: RideResponse) {
                scope.launch {
                    FlowOfBookingResponse.emitBooking(value.action)
                }
                println("Driver action: ${value.action}")
            }

            override fun onError(t: Throwable?) {

            }

            override fun onCompleted() {

            }
        }

        riderServiceGrpcStub.requestRide(request, responseObserver)
    }

    fun getDriverLocationUpdates() {
        val responseObserver = object : StreamObserver<DriverLocationResponse> {
            override fun onNext(value: DriverLocationResponse) {
                scope.launch {
                    FlowOfLocationResponse.emitLocationUpdates(value.latitude, value.longitude)
                }
            }

            override fun onError(t: Throwable?) {

            }

            override fun onCompleted() {

            }
        }

        riderServiceGrpcStub.driverLocation(Empty.getDefaultInstance(), responseObserver)
    }

    fun stopGrpc() {
        scope.cancel()
        managedChannel?.shutdown()
        managedChannel = null
    }

}