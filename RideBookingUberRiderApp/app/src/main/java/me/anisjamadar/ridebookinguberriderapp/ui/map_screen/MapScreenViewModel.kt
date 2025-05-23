package me.anisjamadar.ridebookinguberriderapp.ui.map_screen

import android.annotation.SuppressLint
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.utils.PolylineUtils
import com.ola.mapsdk.model.OlaLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant
import me.anisjamadar.grpc.GrpcHandler
import me.anisjamadar.grpc.flows.FlowOfBookingResponse
import me.anisjamadar.ridebookinguberriderapp.data.model.LocationData
import me.anisjamadar.ridebookinguberriderapp.data.repository.RiderRepository
import me.anisjamadar.ridebookinguberriderapp.utils.AcceptedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.AllDriversBusyStatus
import me.anisjamadar.ridebookinguberriderapp.utils.AllDriversOfflineStatus
import me.anisjamadar.ridebookinguberriderapp.utils.ArrivedAtPickupStatus
import me.anisjamadar.ridebookinguberriderapp.utils.CompletedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.RequestedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.RideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.StartedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.UnknownRideStatus
import javax.inject.Inject

@HiltViewModel
class MapScreenViewModel @Inject constructor(
    private val repository: RiderRepository
) : ViewModel() {
    private val _pickupLocation = MutableStateFlow<LocationData?>(null)
    val pickupLocation: StateFlow<LocationData?> = _pickupLocation.asStateFlow()

    private val _dropLocation = MutableStateFlow<LocationData?>(null)
    val dropLocation: StateFlow<LocationData?> = _dropLocation.asStateFlow()

    private val _rideBookingEnabled = MutableSharedFlow<Boolean>(replay = 1)
    val rideBookingEnabled: SharedFlow<Boolean> = _rideBookingEnabled.asSharedFlow()

    private val _polyline = MutableStateFlow<ArrayList<OlaLatLng>>(arrayListOf())
    val polyline: StateFlow<ArrayList<OlaLatLng>> = _polyline.asStateFlow()

    private val _rideStatus = MutableStateFlow<RideStatus>(UnknownRideStatus)
    val rideStatus: StateFlow<RideStatus> = _rideStatus.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    private val _progressValue = MutableStateFlow(0)
    val progressValue: StateFlow<Int> = _progressValue.asStateFlow()

    init {
        fetchEncodedPolyline()
    }

    fun updatePickupLocation(locationData: LocationData) {
        _pickupLocation.value = locationData
        updateRideBookingEnabled(_dropLocation.value != null)
    }

    fun updateDropLocation(locationData: LocationData) {
        _dropLocation.value = locationData
        updateRideBookingEnabled(_pickupLocation.value != null)
    }

    private fun updateRideBookingEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            _rideBookingEnabled.emit(isEnabled)
        }
    }

    fun requestRide() {
        viewModelScope.launch {
            FlowOfBookingResponse
                .bookingFlow
                .collect {
                    println("Logs of status : $it")
                    when(GrpcConstant.DriverAction.fromAction(it)) {
                        GrpcConstant.DriverAction.REQUESTED_RIDE -> {
                            _rideStatus.value = RequestedRideStatus
                        }
                        GrpcConstant.DriverAction.ACCEPTED -> {
                            _rideStatus.value = AcceptedRideStatus(0.0, 0.0)
                        }
                        GrpcConstant.DriverAction.REJECTED -> {
                            _rideStatus.value = AllDriversBusyStatus
                        }
                        GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> {
                            _rideStatus.value = ArrivedAtPickupStatus
                        }
                        GrpcConstant.DriverAction.RIDE_STARTED -> {
                            _rideStatus.value = StartedRideStatus
                        }
                        GrpcConstant.DriverAction.RIDE_COMPLETED -> {
                            _rideStatus.value = CompletedRideStatus
                        }
                        GrpcConstant.DriverAction.UNKNOWN -> {
                            _rideStatus.value = UnknownRideStatus
                        }
                        GrpcConstant.DriverAction.DRIVER_OFFLINE -> {
                            _rideStatus.value = AllDriversOfflineStatus
                        }
                    }
                }
        }
        GrpcHandler.startGrpc()
        GrpcHandler.sendRideRequest(
            pickupLocation.value?.latitude ?: 0.0,
            pickupLocation.value?.longitude ?: 0.0,
            dropLocation.value?.latitude ?: 0.0,
            dropLocation.value?.longitude ?: 0.0,
            pickupLocation.value?.address ?: "",
            dropLocation.value?.address ?: ""
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchEncodedPolyline() {
        viewModelScope.launch {
            rideBookingEnabled
                .filter {
                    it
                }
                .flatMapLatest {
                    repository.getEncodedPolyline(
                        "${_pickupLocation.value?.latitude},${_pickupLocation.value?.longitude}",
                        "${_dropLocation.value?.latitude},${_dropLocation.value?.longitude}"
                    )
                }
                .filter {
                    !it.isNullOrEmpty()
                }
                .map { encodedPolyline ->
                    PolylineUtils.decode(encodedPolyline!!, 5)
                        .map {
                            OlaLatLng(it.latitude(), it.longitude())
                        }
                }
                .map {
                    it as ArrayList<OlaLatLng>
                }
                .flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                }
                .collect {
                    _polyline.value = it
                }
        }
    }

    fun startTimer(time: Int) {
        countDownTimer = null
        countDownTimer = object : CountDownTimer(time * 1000L, 1000) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val progress = ((seconds.toFloat() / time) * 100)
                _progressValue.value = progress.toInt()
            }

            override fun onFinish() {
                _progressValue.value = 0
                countDownTimer?.cancel()
                countDownTimer = null
                _rideStatus.value = AllDriversBusyStatus
            }
        }.start()
    }

    fun cancelCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    fun showFindTripView() {
        _rideStatus.value = UnknownRideStatus
    }

    override fun onCleared() {
        super.onCleared()
        GrpcHandler.stopGrpc()
    }
}