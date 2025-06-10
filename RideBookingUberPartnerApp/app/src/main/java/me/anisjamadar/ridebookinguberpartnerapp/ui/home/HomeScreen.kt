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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ola.mapsdk.camera.MapControlSettings
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
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            ChangeStatusView(
                modifier = Modifier
                    .padding(padding)
            )
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
                        olaMap.updateMapUiSettings(
                            MapControlSettings.Builder()
                                .setCompassEnabled(false)
                                .build()
                        )
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
fun ChangeStatusView(modifier: Modifier) {
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        ""
    }

    val allPermissions = if (notificationPermission.isNotEmpty()) {
        locationPermissions.plus(notificationPermission)
    } else {
        locationPermissions
    }

    val context = LocalContext.current
    val activity = LocalActivity.current

    val onlineStatus by LocationService.isServiceRunning.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var shouldShowCommonRationalDialog by remember { mutableStateOf(false) }
    var shouldShowLocationRationalDialog by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(!Settings.canDrawOverlays(context)) }
    var showLocationProviderDialog by remember { mutableStateOf(false) }

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
                if (permissionsMap.any { (permission, granted) ->
                    permission == notificationPermission && !granted && activity?.shouldShowRequestPermissionRationale(permission) == true
                }) {
                    shouldShowCommonRationalDialog = true
                } else {
                    shouldShowLocationRationalDialog = true
                }
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

    if (shouldShowLocationRationalDialog) {
        PartnerAlertDialog(
            title = "Give permissions",
            text = "We need location access to track your location. Please grant permission.",
            okButtonText = "OK",
            onOkClick = {
                permissionLauncher.launch(
                    allPermissions
                )
                shouldShowCommonRationalDialog = false
            },
            onDismiss = {
                shouldShowCommonRationalDialog = false
            }
        )
    }

    if (shouldShowCommonRationalDialog) {
        PartnerAlertDialog(
            title = "Give permissions",
            text = "We need location access to track your location and notification access to show location service is running. Please grant both permissions.",
            okButtonText = "OK",
            onOkClick = {
                permissionLauncher.launch(
                    allPermissions
                )
                shouldShowCommonRationalDialog = false
            },
            onDismiss = {
                shouldShowCommonRationalDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        PartnerAlertDialog(
            title = "Give permissions",
            text = "For working app correctly you need to give permissions. Please give permissions in the next screen.",
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
        modifier = modifier
            .background(
                if (onlineStatus) {
                    Color.Green
                } else {
                    Color.Red
                },
                shape = RoundedCornerShape(25.dp)
            )
            .zIndex(1f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp, start = 15.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 10.dp
            )
        ) {
            Switch(
                checked = onlineStatus,
                onCheckedChange = {
                    permissionLauncher.launch(
                        allPermissions
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF00C853),
                    checkedTrackColor = Color(0xFFB2FF59),
                    uncheckedThumbColor = Color(0xFFD32F2F),
                    uncheckedTrackColor = Color(0xFFFF8A80)
                )
            )

            Text(
                text =  if (onlineStatus) "GO OFFLINE" else "GO ONLINE",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
