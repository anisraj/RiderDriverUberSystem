package me.anisjamadar.grpc.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FlowOfBookingResponse {

    private val _bookingFlow = MutableSharedFlow<Int>()
    val bookingFlow: SharedFlow<Int> = _bookingFlow.asSharedFlow()

    suspend fun emitBooking(action: Int) {
        _bookingFlow.emit(action)
    }

}