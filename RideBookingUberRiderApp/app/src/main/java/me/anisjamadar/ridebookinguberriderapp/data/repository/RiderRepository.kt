package me.anisjamadar.ridebookinguberriderapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.anisjamadar.ridebookinguberriderapp.data.api.NetworkService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiderRepository @Inject constructor(
    private val networkService: NetworkService
) {
    fun getEncodedPolyline(origin: String, destination: String): Flow<String?> {
        return flow {
            emit(networkService.getEncodedPolyline(origin, destination))
        }.map {
            it.routes.firstOrNull()?.overviewPolyline
        }
    }
}