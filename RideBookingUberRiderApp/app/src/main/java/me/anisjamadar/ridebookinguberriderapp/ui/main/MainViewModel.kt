package me.anisjamadar.ridebookinguberriderapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.utils.PolylineUtils
import com.ola.mapsdk.model.OlaLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcHandler
import me.anisjamadar.grpc.flows.FlowOfLocationResponse
import me.anisjamadar.ridebookinguberriderapp.data.model.LocationData
import me.anisjamadar.ridebookinguberriderapp.data.repository.RiderRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
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

    private val _loadingTrigger = MutableStateFlow(false)
    val loadingTrigger: StateFlow<Boolean> = _loadingTrigger.asStateFlow()

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

    fun startGrpc() {
        GrpcHandler.startGrpc()
    }

    fun requestRide() {
        GrpcHandler.sendRideRequest(
            pickupLocation.value?.latitude ?: 0.0,
            pickupLocation.value?.longitude ?: 0.0,
            dropLocation.value?.latitude ?: 0.0,
            dropLocation.value?.longitude ?: 0.0,
            pickupLocation.value?.address ?: "",
            dropLocation.value?.address ?: ""
        )
    }

    fun drawOriginToDestPolyline() {
        viewModelScope.launch {
            repository.getEncodedPolyline(
                "${_pickupLocation.value?.latitude},${_pickupLocation.value?.longitude}",
                "${_dropLocation.value?.latitude},${_dropLocation.value?.longitude}"
            ).executePolylineFlow()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun drawDriverToRiderPolyline() {
        viewModelScope.launch {
            FlowOfLocationResponse
                .locationFlow
                .take(1)
                .flatMapLatest {
                    repository.getEncodedPolyline(
                        "${it.first},${it.second}",
                        "${_pickupLocation.value?.latitude},${_pickupLocation.value?.longitude}"
                    )
                }
                .executePolylineFlow()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchEncodedPolyline() {
        viewModelScope.launch {
            rideBookingEnabled
                .filter {
                    it
                }
                .flatMapLatest {
                    _loadingTrigger.value = true
                    repository.getEncodedPolyline(
                        "${_pickupLocation.value?.latitude},${_pickupLocation.value?.longitude}",
                        "${_dropLocation.value?.latitude},${_dropLocation.value?.longitude}"
                    )
                }
                .executePolylineFlow()
        }
    }

    fun reset() {
        _pickupLocation.value = null
        _dropLocation.value = null
        _polyline.value = arrayListOf()
        viewModelScope.launch {
            _rideBookingEnabled.emit(false)
        }
    }

    private suspend fun Flow<String?>.executePolylineFlow() {
        filter {
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
            _loadingTrigger.value = false
            it.printStackTrace()
        }
        .collect {
            _polyline.value = it
            _loadingTrigger.value = false
        }
    }


    override fun onCleared() {
        super.onCleared()
        GrpcHandler.stopGrpc()
    }
}