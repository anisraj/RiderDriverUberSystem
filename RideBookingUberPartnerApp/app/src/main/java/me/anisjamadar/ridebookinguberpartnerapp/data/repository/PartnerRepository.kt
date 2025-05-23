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
    fun getAddressByReverseGeocode(origin: String, destination: String): Flow<String?> {
        return flow {
            emit(networkService.getAddressByReverseGeocode(origin, destination))
        }.map {
            it.results.first().formattedAddress
        }
    }
}