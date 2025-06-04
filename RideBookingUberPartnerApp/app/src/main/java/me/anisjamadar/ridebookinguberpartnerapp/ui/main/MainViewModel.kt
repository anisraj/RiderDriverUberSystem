package me.anisjamadar.ridebookinguberpartnerapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.utils.PolylineUtils
import com.ola.mapsdk.model.OlaLatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationClient
import me.anisjamadar.ridebookinguberpartnerapp.data.model.AddressType
import me.anisjamadar.ridebookinguberpartnerapp.data.repository.PartnerRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val partnerRepository: PartnerRepository,
    private val locationClient: LocationClient
) : ViewModel() {
    private val _polyline = MutableStateFlow<ArrayList<OlaLatLng>>(arrayListOf())
    val polyline: StateFlow<ArrayList<OlaLatLng>> = _polyline.asStateFlow()

    private val _pickupAddress = MutableStateFlow(Pair("", ""))
    val pickupAddress: StateFlow<Pair<String, String>> = _pickupAddress.asStateFlow()

    private val _dropAddress = MutableStateFlow(Pair("", ""))
    val dropAddress: StateFlow<Pair<String, String>> = _dropAddress.asStateFlow()

    val ridePickupLatitude = MutableStateFlow<Double?>(null)
    val ridePickupLongitude = MutableStateFlow<Double?>(null)
    val rideDropLatitude = MutableStateFlow<Double?>(null)
    val rideDropLongitude = MutableStateFlow<Double?>(null)

    fun fetchAddress(latlng: String, addressType: AddressType) {
        viewModelScope.launch {
            partnerRepository
                .getAddressByReverseGeocode(latlng)
                .catch {
                    it.printStackTrace()
                }
                .collect {
                    when (addressType) {
                        AddressType.PICKUP -> _pickupAddress.value = it
                        AddressType.DROP -> _dropAddress.value = it
                    }
                }
        }
    }

    fun drawOriginToDestPolyline() {
        viewModelScope.launch {
            partnerRepository.getEncodedPolyline(
                "${ridePickupLatitude.value},${ridePickupLongitude.value}",
                "${rideDropLatitude.value},${rideDropLongitude.value}"
            ).executePolylineFlow()
        }
    }

    fun drawCurrentLocationToOriginPolyline() {
        viewModelScope.launch {
            runCatching {
                locationClient.getCurrentLocation()
            }.onSuccess {
                partnerRepository.getEncodedPolyline(
                    "${it.latitude},${it.longitude}",
                    "${ridePickupLatitude.value},${ridePickupLongitude.value}"
                ).executePolylineFlow()
            }
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
            it.printStackTrace()
        }
        .collect {
            _polyline.value = it
        }
    }
}