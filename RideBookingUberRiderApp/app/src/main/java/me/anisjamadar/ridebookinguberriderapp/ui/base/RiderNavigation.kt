package me.anisjamadar.ridebookinguberriderapp.ui.base

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.anisjamadar.ridebookinguberriderapp.data.model.LocationData
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.FOR_DROP
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.FOR_PICKUP
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.MapScreen
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.LATITUDE
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.LOCATION_FOR
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.LONGITUDE
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.LocationSearchScreen
import me.anisjamadar.ridebookinguberriderapp.ui.location_search.MAIN_TEXT

sealed class Route(val name: String) {
    data object Main : Route("main")
    data object LocationSearchForPickup : Route("location_search?for=pickup")
    data object LocationSearchForDrop : Route("location_search?for=drop")
}

@Composable
fun RiderNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Main.name
    ) {
        composable(route = Route.Main.name) { entry ->
            val savedStateHandle = entry.savedStateHandle
            val locationFor = savedStateHandle.get<String>(LOCATION_FOR)
            val mainText = savedStateHandle.get<String>(MAIN_TEXT)
            val latitude = savedStateHandle.get<Double>(LATITUDE)
            val longitude = savedStateHandle.get<Double>(LONGITUDE)

            MapScreen(
                LocationData(
                    locationFor = locationFor,
                    address = mainText,
                    latitude = latitude,
                    longitude = longitude
                ),
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
                navController = navController,
                locationFor = FOR_PICKUP,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.LocationSearchForDrop.name) {
            LocationSearchScreen(
                navController = navController,
                locationFor = FOR_DROP,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}