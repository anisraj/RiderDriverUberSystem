package me.anisjamadar.ridebookinguberpartnerapp.ui.base

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.anisjamadar.ridebookinguberpartnerapp.ui.home.HomeScreen
import me.anisjamadar.ridebookinguberpartnerapp.ui.main.MainViewModel
import me.anisjamadar.ridebookinguberpartnerapp.ui.ride_booking.RideStatusScreen

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object RideStatus : Route("ride_status")
}

@Composable
fun PartnerNavHost(
    startDestination: String,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController,
        startDestination
    ) {
        composable(Route.Home.route) {
            HomeScreen()
        }
        composable(Route.RideStatus.route) {
            RideStatusScreen(mainViewModel)
        }
    }
}