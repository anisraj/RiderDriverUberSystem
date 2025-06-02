package me.anisjamadar.ridebookinguberpartnerapp.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationService

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var map by remember { mutableStateOf<OlaMap?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    val mapView by remember {
        mutableStateOf(OlaMapView(context).also {
            it.getMap(
                apiKey = BuildConfig.API_KEY,
                olaMapCallback = object : OlaMapCallback {
                    override fun onMapError(error: String) {}
                    override fun onMapReady(olaMap: OlaMap) {
                        map = olaMap
                        map?.showCurrentLocation()
                        isMapReady = true
                    }
                }
            )
        })
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val onlineStatus by LocationService.isServiceRunning.collectAsStateWithLifecycle()

            Box(
                modifier = Modifier
                    .background(
                        if (onlineStatus) {
                            Color.Green
                        } else {
                            Color.Red
                        },
                        shape = RoundedCornerShape(25.dp)
                    )
                    .clickable {
                        if (onlineStatus) {
                            Intent(context, LocationService::class.java).apply {
                                action = LocationService.ACTION_STOP
                                context.startService(this)
                            }
                        } else {
                            Intent(context, LocationService::class.java).apply {
                                action = LocationService.ACTION_START
                                context.startService(this)
                            }
                        }
                    }
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =  if (onlineStatus) "GO OFFLINE" else "GO ONLINE",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 20.dp, start = 15.dp, end = 15.dp)
                        .align(Alignment.Center)
                )
            }

            AndroidView(
                factory = { mapView }
            )
        }
    }
}
