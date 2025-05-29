package me.anisjamadar.ridebookinguberriderapp.ui.map_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.ola.mapsdk.camera.OlaCameraUpdate
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.model.OlaLatLng
import com.ola.mapsdk.model.OlaLatLngBounds
import com.ola.mapsdk.model.OlaMarkerOptions
import com.ola.mapsdk.model.OlaPolylineOptions
import com.ola.mapsdk.view.Marker
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView
import com.ola.mapsdk.view.Polyline
import kotlinx.coroutines.launch
import me.anisjamadar.grpc.GrpcConstant.COUNTDOWN_TIME_IN_SECONDS
import me.anisjamadar.ridebookinguberriderapp.BuildConfig
import me.anisjamadar.ridebookinguberriderapp.R
import me.anisjamadar.ridebookinguberriderapp.data.model.LocationData
import me.anisjamadar.ridebookinguberriderapp.ui.base.ShowLoading
import me.anisjamadar.ridebookinguberriderapp.utils.AcceptedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.AllDriversBusyStatus
import me.anisjamadar.ridebookinguberriderapp.utils.AllDriversOfflineStatus
import me.anisjamadar.ridebookinguberriderapp.utils.ArrivedAtPickupStatus
import me.anisjamadar.ridebookinguberriderapp.utils.CompletedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.LocationClient
import me.anisjamadar.ridebookinguberriderapp.utils.RejectedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.RequestedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.StartedRideStatus
import me.anisjamadar.ridebookinguberriderapp.utils.UnknownRideStatus
import kotlin.math.abs
import kotlin.math.atan


@Composable
fun MapScreen(
    locationData: LocationData?,
    onLocationSearchClick: (String) -> Unit
) {
    val viewModel = hiltViewModel<MapScreenViewModel>()
    val rideStatus by viewModel.rideStatus.collectAsStateWithLifecycle()
    val loadingTrigger by viewModel.loadingTrigger.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (loadingTrigger) {
                ShowLoading()
            }
            OlaMapView(
                viewModel
            )
            when(rideStatus) {
                UnknownRideStatus -> {
                    FindTripView(
                        viewModel,
                        locationData,
                        onLocationSearchClick
                    )
                }
                RequestedRideStatus -> {
                    RequestedRideView(viewModel)
                }
                AcceptedRideStatus -> {
                    AcceptedRideView()
                }
                RejectedRideStatus -> {

                }
                ArrivedAtPickupStatus -> {

                }
                StartedRideStatus -> {

                }
                CompletedRideStatus -> {

                }
                AllDriversBusyStatus -> {
                    DriversBusyView(viewModel)
                }
                AllDriversOfflineStatus -> {
                    DriversOfflineView(viewModel)
                }
            }
        }
    }

}

@SuppressLint("MissingPermission")
@Composable
fun OlaMapView(viewModel: MapScreenViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var permissionsGranted by remember { mutableStateOf(false) }

    var polyline by remember { mutableStateOf<Polyline?>(null) }

    var pickupMarker by remember { mutableStateOf<Marker?>(null) }
    var dropMarker by remember { mutableStateOf<Marker?>(null) }
    var driverMarker by remember { mutableStateOf<Marker?>(null) }

    var previousLatLong by remember { mutableStateOf<OlaLatLng?>(null) }

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

    val polylineData by viewModel.polyline.collectAsStateWithLifecycle()
    LaunchedEffect(polylineData, isMapReady) {
        if (polylineData.isNotEmpty()) {
            polyline?.removePolyline()
            pickupMarker?.removeMarker()
            dropMarker?.removeMarker()

            val data = polylineData
            val bounds = OlaLatLngBounds.Builder()
                .includes(data)
                .build()
            val olaCameraUpdate = OlaCameraUpdate(
                bounds,
                paddingLeft = 100,
                paddingTop = 100,
                paddingRight = 100,
                paddingBottom = 100
            )

            val polylineOptions = OlaPolylineOptions.Builder()
                .setPolylineId("rider_path")
                .setColor("0xFF000000")
                .setPoints(data)
                .setWidth(5f)
                .build()

            val pickupMarkerOptions = OlaMarkerOptions.Builder()
                .setMarkerId("pickup_marker")
                .setPosition(OlaLatLng(data.first().latitude, data.first().longitude))
                .setIsIconClickable(false)
                .setIconIntRes(R.drawable.pickup_icon)
                .setIconRotation(0f)
                .setIsAnimationEnable(true)
                .build()

            val dropMarkerOptions = OlaMarkerOptions.Builder()
                .setMarkerId("drop_marker")
                .setPosition(OlaLatLng(data.last().latitude, data.last().longitude))
                .setIsIconClickable(false)
                .setIconIntRes(R.drawable.drop_icon)
                .setIconRotation(0f)
                .setIsAnimationEnable(true)
                .build()

            pickupMarker = map?.addMarker(pickupMarkerOptions)
            dropMarker = map?.addMarker(dropMarkerOptions)

            polyline = map?.addPolyline(polylineOptions)
            map?.easeCamera(olaCameraUpdate, 1000)
        }
    }

    val driverLocation by viewModel.driverLocation.collectAsStateWithLifecycle()
    LaunchedEffect(driverLocation, isMapReady) {
        driverLocation?.let { currentLatLong ->
            if (isMapReady) {
                dropMarker?.removeMarker()
                val from = previousLatLong ?: currentLatLong
                val markerOptions = OlaMarkerOptions.Builder()
                    .setMarkerId("driver_marker")
                    .setPosition(currentLatLong)
                    .setIsIconClickable(false)
                    .setIconIntRes(R.drawable.ic_car)
                    .setIconRotation(getRotation(from, currentLatLong))
                    .setIsAnimationEnable(true)
                    .build()
                driverMarker = map?.addMarker(markerOptions)
                previousLatLong = currentLatLong
            }
        }
    }

    val locationClient by remember {
        mutableStateOf(
            LocationClient(
                context,
                LocationServices.getFusedLocationProviderClient(context)
            )
        )
    }

    LaunchedEffect(Unit) {
        if (permissionsGranted) {
            scope.launch {
                runCatching {
                    locationClient.getCurrentLocation()
                }.onSuccess { location ->
                    map?.zoomToLocation(OlaLatLng(location.latitude, location.longitude), 15.0)
                    viewModel.updatePickupLocation(LocationData(FOR_PICKUP, "Current Location", location.latitude, location.longitude))
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }

    Box {
        AndroidView(
            factory = { mapView }
        )
    }
}

@Composable
fun FindTripView(
    viewModel: MapScreenViewModel,
    locationData: LocationData?,
    onLocationSearchClick: (String) -> Unit
) {
    if (locationData?.locationFor == FOR_PICKUP) {
        viewModel.updatePickupLocation(locationData)
    } else if (locationData?.locationFor == FOR_DROP) {
        viewModel.updateDropLocation(locationData)
    }
    val pickupData by viewModel.pickupLocation.collectAsStateWithLifecycle()
    val dropData by viewModel.dropLocation.collectAsStateWithLifecycle()
    val rideEnabled by viewModel.rideBookingEnabled.collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            text = "Find a trip",
            style = TextStyle(
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFFEEEEEE)
                )
                .clickable {
                    onLocationSearchClick.invoke(FOR_PICKUP)
                },
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.Start
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(26.dp),
                painter = painterResource(R.drawable.pickup_icon),
                contentDescription = "pickup icon"
            )

            if (!pickupData?.address.isNullOrEmpty()) {
                Text(
                    text = pickupData?.address!!,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Add a pick-up location",
                    color = Color(0xFF6B6B6B)
                )
            }
        }

        VerticalDivider(
            modifier = Modifier
                .padding(start = 43.dp)
                .width(2.dp)
                .height(16.dp)
                .background(Color.Black)
        )

        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    shape = RoundedCornerShape(5.dp),
                    color = Color(0xFFEEEEEE)
                )
                .clickable {
                    onLocationSearchClick.invoke(FOR_DROP)
                },
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.Start
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(26.dp),
                painter = painterResource(R.drawable.drop_icon),
                contentDescription = "drop icon"
            )

            if (!dropData?.address.isNullOrEmpty()) {
                Text(
                    text = dropData?.address!!,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Enter your destination",
                    color = Color(0xFF6B6B6B)
                )
            }
        }

        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(5.dp),
            onClick = {
                viewModel.requestRide()
            },
            enabled = rideEnabled,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Black,
                disabledContainerColor = Color(0xFFEEEEEE)
            )
        ) {
            Text("BOOK A RIDE")
        }
    }
}

@Composable
fun RequestedRideView(viewModel: MapScreenViewModel) {
    val progress by viewModel.progressValue.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startTimer(COUNTDOWN_TIME_IN_SECONDS)

        onDispose {
            viewModel.cancelCountDownTimer()
        }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color.White)
        .padding(top = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp),
            text = "Trip Requested",
            style = TextStyle(
                fontSize = 22.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp),
            text = "Finding drivers nearby",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.Black
            )
        )

        LinearProgressIndicator(
            progress = { progress.toFloat() / 100 },
            modifier = Modifier
                .padding(top = 32.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(2.dp)
                .padding(),
            color = Color.Black,
            trackColor = Color(0xFFE2E2E2)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun P() {
    AcceptedRideView()
}

@Composable
fun AcceptedRideView() {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color.White)
        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meet at the pickup point",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            )

            Box(modifier = Modifier
                .size(50.dp)
                .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = "2",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    )
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = "MIN",
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(2.dp)
                .padding(),
            color = Color(0xFFE2E2E2)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp)),
                painter = painterResource(R.drawable.ic_launcher_background),
                contentDescription = "driver image"
            )

            Column(
                modifier = Modifier,

            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Sanit",
                    textAlign = TextAlign.End,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "MH 50 SK5055",
                    textAlign = TextAlign.End,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "Maruti Suzuki",
                    textAlign = TextAlign.End,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                )
            }
        }
    }
}

@Composable
fun DriversBusyView(viewModel: MapScreenViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            text = "It seems all drivers are busy",
            style = TextStyle(
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 32.dp),
            text = "Please try after some time",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.Black
            )
        )

        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(5.dp),
            onClick = {
                viewModel.showFindTripView()
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Black,
                disabledContainerColor = Color(0xFFEEEEEE)
            )
        ) {
            Text("OK")
        }
    }
}

@Composable
fun DriversOfflineView(viewModel: MapScreenViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            text = "All drivers are offline",
            style = TextStyle(
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 32.dp),
            text = "It seems there are no drivers present in your are, please try after some time",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.Black
            )
        )

        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(5.dp),
            onClick = {
                viewModel.showFindTripView()
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Black,
                disabledContainerColor = Color(0xFFEEEEEE)
            )
        ) {
            Text("OK")
        }
    }
}

fun getRotation(start: OlaLatLng, end: OlaLatLng): Float {
    val latDifference: Double = abs(start.latitude - end.latitude)
    val lngDifference: Double = abs(start.longitude - end.longitude)
    var rotation = -1F
    when {
        start.latitude < end.latitude && start.longitude < end.longitude -> {
            rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
        }
        start.latitude >= end.latitude && start.longitude < end.longitude -> {
            rotation = (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
        }
        start.latitude >= end.latitude && start.longitude >= end.longitude -> {
            rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
        }
        start.latitude < end.latitude && start.longitude >= end.longitude -> {
            rotation =
                (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
        }
    }
    return rotation
}

const val FOR_PICKUP = "pickup"
const val FOR_DROP = "drop"