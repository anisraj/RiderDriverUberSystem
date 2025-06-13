package me.anisjamadar.grpc

import driver.Driver.LocationRequest
import driver.Driver.NewJobRequest
import driver.Driver.NewJobResponse
import driver.DriverServiceGrpcKt
import driver.locationRequest
import driver.newJobRequest
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant.GRPC_HOST
import me.anisjamadar.grpc.GrpcConstant.GRPC_PORT
import me.anisjamadar.grpc.flows.FlowBookingAcknowledge
import me.anisjamadar.grpc.model.BookingData

object GrpcHandler {
    private var managedChannel: ManagedChannel? = null

    private lateinit var driverServiceCoroutineStub: DriverServiceGrpcKt.DriverServiceCoroutineStub
    private val locationRequestFlow = MutableSharedFlow<LocationRequest>(replay = 0)
    private val newJobRequestFlow = MutableSharedFlow<NewJobRequest>(replay = 0)

    private lateinit var scope: CoroutineScope

    fun startService() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        if (managedChannel == null) {
            managedChannel = ManagedChannelBuilder
                .forAddress(GRPC_HOST, GRPC_PORT)
                .usePlaintext()
                .executor(Dispatchers.IO.asExecutor())
                .build()
        }
        driverServiceCoroutineStub = DriverServiceGrpcKt.DriverServiceCoroutineStub(managedChannel!!)
        observeResponse()
        onBookingArrived()
    }

    fun stopService() {
        scope.cancel()
        managedChannel?.shutdown()
        managedChannel = null
    }

    suspend fun sendLocationUpdates(latitude: Double, longitude: Double) {
        locationRequestFlow
            .emit(
                locationRequest {
                    this.latitude = latitude
                    this.longitude = longitude
                }
            )
    }

    private fun observeResponse() {
        scope.launch {
            driverServiceCoroutineStub
                .driverLocation(locationRequestFlow)
                .collect {
                    println("APP_TAG - response $it")
                }
        }
    }

    suspend fun updateRideStatus(action: Int) {
        newJobRequestFlow.emit(newJobRequest { this.action = action })
    }

    private fun onBookingArrived() {
        scope.launch {
            driverServiceCoroutineStub
                .driverNewJob(newJobRequestFlow)
                .collect {
                    if (it.action == 1) {
                        handleBooking(it)
                    }
                }
        }
    }

    private fun handleBooking(value: NewJobResponse) {
        scope.launch {
            FlowBookingAcknowledge.emitBooking(BookingData(
                value.deliveryFrom,
                value.deliveryTo,
                value.time,
                value.budget,
                value.ridePickupLatitude,
                value.ridePickupLongitude,
                value.rideDropLatitude,
                value.rideDropLongitude,
                value.distanceInKm
            ))
        }
    }
}