package me.anisjamadar.ridebookinguberriderapp.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import me.anisjamadar.ridebookinguberriderapp.utils.isLocationEnabled

@Composable
fun ShowLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .semantics {
                    contentDescription = "loading"
                }
        )
    }
}

@Composable
fun ShowError(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Red,
            modifier = Modifier
                .padding(4.dp)
        )
    }
}

@Composable
fun PermissionDialog(
    onOkClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    onOkClick()
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    containerColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Permission Required",
                color = Color.Black
            )
        },
        text = {
            Text("This app needs location permission to show your current location on the map.")
        }
    )
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionRequest(
    context: Context,
    onPermissionGranted: () -> Unit
) {
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    var isLocationEnabled by remember { mutableStateOf(context.isLocationEnabled()) }
    var showLocationProviderDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        PermissionDialog(
            onOkClick = {
                locationPermissionsState.launchMultiplePermissionRequest()
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    if (showLocationProviderDialog) {
        LocationProviderDialog(
            context,
            onDismiss = {
                showLocationProviderDialog = false
            }
        )
    }

    LaunchedEffect(locationPermissionsState.permissions.map { it.status }) {
        if (locationPermissionsState.allPermissionsGranted) {
                isLocationEnabled = context.isLocationEnabled()
                if (isLocationEnabled) {
                    showLocationProviderDialog = false
                    onPermissionGranted()
                } else {
                    showLocationProviderDialog = true
                }
        } else if (locationPermissionsState.permissions.any { it.status.shouldShowRationale }) {
            showPermissionDialog = true
        } else {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLocationEnabled = context.isLocationEnabled()
                if (isLocationEnabled && locationPermissionsState.allPermissionsGranted) {
                    showLocationProviderDialog = false
                    onPermissionGranted()
                } else if (locationPermissionsState.allPermissionsGranted && !isLocationEnabled){
                    showLocationProviderDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun LocationProviderDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Color.Black
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(5.dp),
                onClick = {
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.Black,
                    containerColor = Color.White
                )
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                text = "Enable GPS",
                color = Color.Black
            )
        },
        text = {
            Text("GPS is disabled, for getting your current location you can enable it.")
        }
    )
}