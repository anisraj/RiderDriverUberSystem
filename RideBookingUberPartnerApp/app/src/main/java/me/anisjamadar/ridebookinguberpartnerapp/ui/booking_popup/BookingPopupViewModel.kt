package me.anisjamadar.ridebookinguberpartnerapp.ui.booking_popup

import android.annotation.SuppressLint
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcHandler
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationService
import javax.inject.Inject

@HiltViewModel
class BookingPopupViewModel @Inject constructor() : ViewModel() {

    private val _progressValue = MutableStateFlow(0)
    val progressValue: StateFlow<Int> = _progressValue.asStateFlow()

    private val _timer = MutableStateFlow("20")
    val timer: StateFlow<String> = _timer.asStateFlow()

    private val _playSound = MutableStateFlow(false)
    val playSound: StateFlow<Boolean> = _playSound.asStateFlow()

    private val _finishPage = MutableStateFlow(false)
    val finishPage: StateFlow<Boolean> = _finishPage.asStateFlow()

    private var countDownTimer: CountDownTimer? = null

    fun startTimer(time: Int) {
        _playSound.value = true
        countDownTimer = null
        countDownTimer = object : CountDownTimer(time * 1000L, 1000) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val progress = ((seconds.toFloat() / time) * 100)
                _progressValue.value = progress.toInt()
                _timer.value = String.format("%02d", seconds)
            }

            override fun onFinish() {
                _timer.value = ""
                _progressValue.value = 0
                cancelCountDownTimer()
            }
        }.start()
    }

    private fun cancelCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        finishPage()

    }

    fun updateBooking(action: Int) {
        viewModelScope.launch {
            LocationService.isServiceRunning
                .collect {
                    if (it) {
                        GrpcHandler.updateRideStatus(action)
                    }
                }
        }
        finishPage()
    }

    private fun finishPage() {
        _finishPage.value = true
    }
}