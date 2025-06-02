package me.anisjamadar.ridebookinguberpartnerapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import me.anisjamadar.ridebookinguberpartnerapp.data.model.AddressType
import me.anisjamadar.ridebookinguberpartnerapp.data.repository.PartnerRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val partnerRepository: PartnerRepository
) : ViewModel() {
    val ridePickupLatitude = MutableStateFlow<Double?>(null)
    val ridePickupLongitude = MutableStateFlow<Double?>(null)
    val rideDropLatitude = MutableStateFlow<Double?>(null)
    val rideDropLongitude = MutableStateFlow<Double?>(null)

    private val _pickupAddress = MutableStateFlow(Pair("", ""))
    val pickupAddress: StateFlow<Pair<String, String>> = _pickupAddress.asStateFlow()

    private val _dropAddress = MutableStateFlow(Pair("", ""))
    val dropAddress: StateFlow<Pair<String, String>> = _dropAddress.asStateFlow()

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
}