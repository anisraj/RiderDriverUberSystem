package me.anisjamadar.ridebookinguberpartnerapp.ui.ride_booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant
import me.anisjamadar.grpc.GrpcHandler
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationService
import javax.inject.Inject

@HiltViewModel
class RideStatusViewModel @Inject constructor() : ViewModel() {
    private val _rideStatus = MutableStateFlow(GrpcConstant.DriverAction.ACCEPTED)
    val rideStatus: StateFlow<GrpcConstant.DriverAction> = _rideStatus.asStateFlow()

    fun updateStatus() {
        when(rideStatus.value) {
            GrpcConstant.DriverAction.ACCEPTED -> {
                _rideStatus.value = GrpcConstant.DriverAction.ARRIVED_AT_PICKUP
            }
            GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> {
                _rideStatus.value = GrpcConstant.DriverAction.RIDE_STARTED
            }
            GrpcConstant.DriverAction.RIDE_STARTED -> {
                _rideStatus.value = GrpcConstant.DriverAction.RIDE_COMPLETED
            }
            else -> {
                _rideStatus.value = GrpcConstant.DriverAction.ACCEPTED
            }
        }

        viewModelScope.launch {
            LocationService.isServiceRunning
                .collect {
                    if (it) {
                        GrpcHandler.updateRideStatus(rideStatus.value.ordinal)
                    }
                }
        }
    }

}