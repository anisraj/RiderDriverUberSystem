package me.anisjamadar.ridebookinguberriderapp.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import me.anisjamadar.ridebookinguberriderapp.ui.base.RiderNavHost
import me.anisjamadar.ridebookinguberriderapp.ui.theme.RideBookingUberRiderAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mainViewModel by viewModels<MainViewModel>()

        setContent {
            RideBookingUberRiderAppTheme {
                RiderNavHost(mainViewModel)
            }
        }
    }
}
