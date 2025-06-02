package me.anisjamadar.ridebookinguberriderapp.ui.base

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.FOR_DROP
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.FOR_PICKUP
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.MapScreen
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.LocationSearchScreen
import me.anisjamadar.ridebookinguberriderapp.ui.main.MainViewModel

sealed class Route(val name: String) {
    data object Main : Route("main")
    data object LocationSearchForPickup : Route("location_search?for=pickup")
    data object LocationSearchForDrop : Route("location_search?for=drop")
}

@Composable
fun RiderNavHost(mainViewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Main.name
    ) {
        composable(route = Route.Main.name) {
            MapScreen(
                mainViewModel,
                onLocationSearchClick = { screenFor ->
                    if (screenFor == FOR_PICKUP) {
                        navController.navigate(Route.LocationSearchForPickup.name)
                    } else {
                        navController.navigate(Route.LocationSearchForDrop.name)
                    }
                }
            )
        }
        composable(Route.LocationSearchForPickup.name) {
            LocationSearchScreen(
                mainViewModel = mainViewModel,
                locationFor = FOR_PICKUP,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.LocationSearchForDrop.name) {
            LocationSearchScreen(
                mainViewModel = mainViewModel,
                locationFor = FOR_DROP,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}