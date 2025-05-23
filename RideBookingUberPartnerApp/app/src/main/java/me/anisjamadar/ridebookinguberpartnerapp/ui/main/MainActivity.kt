package me.anisjamadar.ridebookinguberpartnerapp.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView
import dagger.hilt.android.AndroidEntryPoint
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationService
import me.anisjamadar.ridebookinguberpartnerapp.ui.theme.LocationSharingApplicationTheme
import me.anisjamadar.ridebookinguberpartnerapp.ui.theme.Purple80

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            0
        )

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + this.packageName)
            )
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        setContent {
            LocationSharingApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Purple80,
                                titleContentColor = Color.Black
                            ),
                            title = {
                                Text("Home")
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    TrackLocationScreen(
                        applicationContext,
                        Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TrackLocationScreen(context: Context, modifier: Modifier = Modifier) {
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

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        val onlineStatus by LocationService.isServiceRunning.collectAsState()

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
                },
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
