package me.anisjamadar.ridebookinguberpartnerapp.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import me.anisjamadar.ridebookinguberpartnerapp.data.model.AddressType
import me.anisjamadar.ridebookinguberpartnerapp.ui.base.PartnerNavHost
import me.anisjamadar.ridebookinguberpartnerapp.ui.base.Route
import me.anisjamadar.ridebookinguberpartnerapp.ui.theme.LocationSharingApplicationTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val ridePickupLatitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_PICKUP_LATITUDE, 0.0))

    private val ridePickupLongitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_PICKUP_LONGITUDE, 0.0))

    private val rideDropLatitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_DROP_LATITUDE, 0.0))

    private val rideDropLongitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_DROP_LONGITUDE, 0.0))

    private val forStatusChange: Boolean
        get() = checkNotNull(intent.getBooleanExtra(FOR_STATUS_CHANGE, false))

    private val viewModel by viewModels<MainViewModel>()

    companion object {
        private const val RIDE_PICKUP_LATITUDE = "rider_pickup_latitude"
        private const val RIDE_PICKUP_LONGITUDE = "rider_pickup_longitude"
        private const val RIDE_DROP_LATITUDE = "rider_drop_latitude"
        private const val RIDE_DROP_LONGITUDE = "rider_drop_longitude"
        private const val FOR_STATUS_CHANGE = "for_status_change"

        fun start(
            context: Context,
            ridePickupLatitude: Double,
            ridePickupLongitude: Double,
            rideDropLatitude: Double,
            rideDropLongitude: Double
        ): Intent {
            return Intent(
                context,
                MainActivity::class.java
            ).apply {
                putExtra(RIDE_PICKUP_LATITUDE, ridePickupLatitude)
                putExtra(RIDE_PICKUP_LONGITUDE, ridePickupLongitude)
                putExtra(RIDE_DROP_LATITUDE, rideDropLatitude)
                putExtra(RIDE_DROP_LONGITUDE, rideDropLongitude)
                putExtra(FOR_STATUS_CHANGE, true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        fun start(
            context: Context
        ): Intent {
            return Intent(
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (forStatusChange) {
            viewModel.ridePickupLatitude.value = ridePickupLatitude
            viewModel.ridePickupLongitude.value = ridePickupLongitude
            viewModel.rideDropLatitude.value = rideDropLatitude
            viewModel.rideDropLongitude.value = rideDropLongitude
            viewModel.fetchAddress("$ridePickupLatitude,$ridePickupLongitude", AddressType.PICKUP)
            viewModel.fetchAddress("$rideDropLatitude,$rideDropLongitude", AddressType.DROP)
        }

        setContent {
            LocationSharingApplicationTheme {
                PartnerNavHost(
                    startDestination = if (forStatusChange) {
                        Route.RideStatus.route
                    } else {
                        Route.Home.route
                    },
                    viewModel
                )
            }
        }
    }
}
