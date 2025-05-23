package me.anisjamadar.grpc.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import me.anisjamadar.grpc.model.BookingData

object FlowBookingAcknowledge {

    private val _bookingFlow = MutableSharedFlow<BookingData>()
    val bookingFlow: SharedFlow<BookingData> = _bookingFlow.asSharedFlow()

    suspend fun emitBooking(bookingData: BookingData) {
        println("APP_TAG - FlowBookingAcknowledge $bookingData")
        _bookingFlow.emit(bookingData)
    }

}