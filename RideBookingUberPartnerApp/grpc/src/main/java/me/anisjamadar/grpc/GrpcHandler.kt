package me.anisjamadar.grpc

import driver.Driver.LocationRequest
import driver.Driver.LocationResponse
import driver.Driver.NewJobRequest
import driver.Driver.NewJobResponse
import driver.DriverServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant.GRPC_HOST
import me.anisjamadar.grpc.GrpcConstant.GRPC_PORT
import me.anisjamadar.grpc.flows.FlowBookingAcknowledge
import me.anisjamadar.grpc.model.BookingData

object GrpcHandler {
    private var managedChannel: ManagedChannel? = null
    private lateinit var driverServiceGrpcStub: DriverServiceGrpc.DriverServiceStub
    private var locationRequestStreamObserver: StreamObserver<LocationRequest>? = null
    private var newJobRequestStreamObserver: StreamObserver<NewJobRequest>? = null

    private lateinit var scope: CoroutineScope

    fun startService() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        if (managedChannel == null) {
            managedChannel = ManagedChannelBuilder
                .forAddress(GRPC_HOST, GRPC_PORT)
                .usePlaintext()
                .build()
        }
        println("APP_TAG - startService: managedChannel = ${managedChannel?.getState(false)}")
        driverServiceGrpcStub = DriverServiceGrpc.newStub(managedChannel)
        observeResponse()
        onBookingArrived()
    }

    fun stopService() {
        scope.cancel()
        locationRequestStreamObserver?.onCompleted()
        managedChannel?.shutdown()
        locationRequestStreamObserver = null
        newJobRequestStreamObserver?.onCompleted()
        newJobRequestStreamObserver = null
        managedChannel = null
    }

    fun sendLocationUpdates(latitude: Double, longitude: Double) {
        locationRequestStreamObserver?.onNext(LocationRequest
            .newBuilder()
            .setLatitude(latitude)
            .setLongitude(longitude)
            .build()
        )
    }

    private fun observeResponse() {
        locationRequestStreamObserver = driverServiceGrpcStub.driverLocation(object : StreamObserver<LocationResponse> {
            override fun onNext(value: LocationResponse?) {
                println("APP_TAG - response on next $value")
            }

            override fun onError(t: Throwable?) {
                println("APP_TAG - response on error ${t?.localizedMessage} ")
            }

            override fun onCompleted() {
                println("APP_TAG - response on complete ")
            }
        })
    }

    fun updateRideStatus(action: Int) {
        newJobRequestStreamObserver?.onNext(NewJobRequest
            .newBuilder()
            .setAction(action)
            .build()
        )
    }

    private fun onBookingArrived() {
        newJobRequestStreamObserver = driverServiceGrpcStub.driverNewJob(object : StreamObserver<NewJobResponse> {
            override fun onNext(value: NewJobResponse?) {
                if (value?.action == 1) {
                    handleBooking(value)
                }
                println("APP_TAG - response on next $value")
            }

            override fun onError(t: Throwable?) {

            }

            override fun onCompleted() {

            }
        })
    }

    private fun handleBooking(value: NewJobResponse) {
        scope.launch {
            FlowBookingAcknowledge.emitBooking(BookingData(value.deliveryFrom, value.deliveryTo, value.time, value.budget))
        }
    }
}