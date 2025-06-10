package me.anisjamadar.ridebookinguberpartnerapp.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.LocationService
import me.anisjamadar.ridebookinguberpartnerapp.utils.PartnerAlertDialog
import me.anisjamadar.ridebookinguberpartnerapp.utils.hasLocationPermission
import me.anisjamadar.ridebookinguberpartnerapp.utils.hasNotificationPermission
import me.anisjamadar.ridebookinguberpartnerapp.utils.isLocationEnabled

@Composable
fun HomeScreen() {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            ChangeStatusView()
            OlaMapView()
        }
    }
}

@Composable
fun OlaMapView() {
    val context = LocalContext.current
    val mapView by remember {
        mutableStateOf(OlaMapView(context).also {
            it.getMap(
                apiKey = BuildConfig.API_KEY,
                olaMapCallback = object : OlaMapCallback {
                    override fun onMapError(error: String) {}
                    override fun onMapReady(olaMap: OlaMap) {
                        olaMap.showCurrentLocation()
                    }
                }
            )
        })
    }

    AndroidView(
        factory = { mapView }
    )
}

@Composable
fun ChangeStatusView() {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val onlineStatus by LocationService.isServiceRunning.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(!Settings.canDrawOverlays(context)) }
    var showLocationProviderDialog by remember { mutableStateOf(false) }
    val hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            if (permissionsMap.all { (_, granted) ->
                granted
            }) {
                if (!context.isLocationEnabled() && !LocationService.isServiceRunning.value) {
                    showLocationProviderDialog = true
                } else if (!Settings.canDrawOverlays(context)) {
                    showOverlayPermissionDialog = true
                } else {
                    Intent(context, LocationService::class.java).apply {
                        context.startService(this)
                    }
                }
            } else if (permissionsMap.any { (permission, _) ->
                activity?.shouldShowRequestPermissionRationale(permission) == true
            }) {
                showPermissionDialog = true
            } else if (permissionsMap.all { (permission, granted) ->
                !granted && activity?.shouldShowRequestPermissionRationale(permission) == false
            }) {
                showSettingsDialog = true
            }
        }
    )

    if (showOverlayPermissionDialog) {
        PartnerAlertDialog(
            title = "Allow Display Over Other Apps",
            text = "To provide you ride bookings, app needs permission to display over other apps. Please enable this permission in the next screen.",
            okButtonText = "OK",
            onOkClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                )
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                showOverlayPermissionDialog = false
            },
            onDismiss = {
                showOverlayPermissionDialog = false
            }
        )
    }

    if (showPermissionDialog) {
        PartnerAlertDialog(
            title = "Give permissions",
            text = "For getting your current location you need to give location permission.",
            okButtonText = "OK",
            onOkClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        PartnerAlertDialog(
            title = "Give permissions",
            text = "For getting your current location you need to give location permission.",
            okButtonText = "OK",
            onOkClick = {
                showSettingsDialog = false
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            },
            onDismiss = {
                showSettingsDialog = false
            }
        )
    }

    if (showLocationProviderDialog) {
        PartnerAlertDialog(
            title = "First Enable GPS",
            text = "GPS is disabled, for working this app correctly you need to enable GPS first.",
            okButtonText = "OK",
            onOkClick = {
                showLocationProviderDialog = false
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            },
            onDismiss = {
                showLocationProviderDialog = false
            }
        )
    }

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
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
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
}
