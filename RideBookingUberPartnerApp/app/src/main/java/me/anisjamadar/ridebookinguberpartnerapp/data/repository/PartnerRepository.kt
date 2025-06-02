package me.anisjamadar.ridebookinguberpartnerapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.anisjamadar.ridebookinguberpartnerapp.data.api.NetworkService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerRepository @Inject constructor(
    private val networkService: NetworkService
) {
    fun getAddressByReverseGeocode(latlng: String): Flow<Pair<String, String>> {
        return flow {
            emit(networkService.getAddressByReverseGeocode(latlng))
        }.map {
            Pair(it.results.first().name ?: "Address not found", it.results.first().formattedAddress ?: "Address not found")
        }
    }
}