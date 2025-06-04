package me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcHandler
import me.anisjamadar.grpc.flows.FlowBookingAcknowledge
import me.anisjamadar.ridebookinguberpartnerapp.APP_TAG
import me.anisjamadar.ridebookinguberpartnerapp.ui.booking_popup.BookingPopupActivity
import me.anisjamadar.locationsharingapplication.R
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var locationClient: LocationClient

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startGrpcService()
        observeBookings()
    }

    private fun startGrpcService() {
        GrpcHandler.startService()
    }

    private fun stopGrpcService() {
        GrpcHandler.stopService()
    }

    private fun observeBookings() {
        serviceScope.launch(Dispatchers.Main) {
            FlowBookingAcknowledge
                .bookingFlow
                .collectLatest {
                    println("$APP_TAG - LocationService $it")
                    val intent = BookingPopupActivity.start(
                        applicationContext,
                        deliveryFrom = it.deliveryFrom,
                        deliveryTo = it.deliveryTo,
                        time = it.time,
                        budget = it.budget,
                        ridePickupLatitude = it.ridePickupLatitude,
                        ridePickupLongitude = it.ridePickupLongitude,
                        rideDropLatitude = it.rideDropLatitude,
                        rideDropLongitude = it.rideDropLongitude,
                        distance = it.distanceInKm
                    )
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning.value) {
            start()
        } else {
            stop()
        }
        return START_STICKY
    }

    @SuppressLint("ServiceCast")
    private fun start() {
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Driver is online")
            .setSmallIcon(R.drawable.steering_wheel)
            .setOngoing(true)

        locationClient
            .getLocationUpdates()
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                val lat = location.latitude.toString()
                val long = location.longitude.toString()
                println("Location is $lat, $long")
                GrpcHandler.sendLocationUpdates(location.latitude, location.longitude)
            }
            .launchIn(serviceScope)

        startForeground(1, notification.build())
        _isServiceRunning.value = true
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopGrpcService()
        _isServiceRunning.value = false
    }

    companion object {
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }
}