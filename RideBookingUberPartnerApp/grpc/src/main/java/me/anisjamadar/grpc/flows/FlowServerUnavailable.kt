package me.anisjamadar.grpc.flows

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FlowServerUnavailable {

    private val _serverUnavailableFlow = MutableSharedFlow<Throwable>()
    val serverUnavailableFlow: SharedFlow<Throwable> = _serverUnavailableFlow.asSharedFlow()

    suspend fun emitException(throwable: Throwable) {
        _serverUnavailableFlow.emit(throwable)
    }

}