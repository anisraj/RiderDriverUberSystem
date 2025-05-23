package me.anisjamadar.grpc.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FlowOfLocationResponse {

    private val _locationFlow = MutableSharedFlow<Pair<Double, Double>>()
    val locationFlow: SharedFlow<Pair<Double, Double>> = _locationFlow.asSharedFlow()

    suspend fun emitLocationUpdates(latitude: Double, longitude: Double) {
        _locationFlow.emit(Pair(latitude, longitude))
    }

}