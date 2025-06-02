package me.anisjamadar.ridebookinguberpartnerapp.ui.ride_booking

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
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
import me.anisjamadar.grpc.GrpcConstant
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.locationsharingapplication.R
import me.anisjamadar.ridebookinguberpartnerapp.data.location_tracking.DefaultLocationClient
import me.anisjamadar.ridebookinguberpartnerapp.ui.main.MainActivity
import me.anisjamadar.ridebookinguberpartnerapp.ui.main.MainViewModel
import kotlin.math.max

@Composable
fun RideStatusScreen(mainViewModel: MainViewModel) {
    val rideStatusViewModel = hiltViewModel<RideStatusViewModel>()
    val status by rideStatusViewModel.rideStatus.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED -> "Heading to pickup"
                    GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> "Arrived to pickup"
                    GrpcConstant.DriverAction.RIDE_STARTED -> "On the way to drop"
                    else -> {
                        "Ride completed"
                    }
                },
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )

            if (status != GrpcConstant.DriverAction.RIDE_COMPLETED) {
                LocationItemLayout(mainViewModel, status)
            }

            OlaMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                mainViewModel = mainViewModel
            )

            if (status == GrpcConstant.DriverAction.RIDE_COMPLETED) {
                RideCompletedView()
            } else {
                Box(modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    SliderButton(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        text = when(status) {
                            GrpcConstant.DriverAction.ACCEPTED -> {
                                mainViewModel.drawCurrentLocationToOriginPolyline(
                                    DefaultLocationClient(
                                        LocalContext.current,
                                        LocationServices.getFusedLocationProviderClient(LocalContext.current)
                                    )
                                )
                                "Arrived to pickup"
                            }
                            GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> "Start the ride"
                            GrpcConstant.DriverAction.RIDE_STARTED -> {
                                mainViewModel.drawOriginToDestPolyline()
                                "Ride completed"
                            }
                            else -> {
                                ""
                            }
                        }
                    ) {
                        rideStatusViewModel.updateStatus()
                    }
                }
            }
        }
    }
}

@Composable
fun OlaMapView(modifier: Modifier, mainViewModel: MainViewModel) {
    var polyline by remember { mutableStateOf<Polyline?>(null) }

    var pickupMarker by remember { mutableStateOf<Marker?>(null) }
    var dropMarker by remember { mutableStateOf<Marker?>(null) }

    var map by remember { mutableStateOf<OlaMap?>(null) }

    var isMapReady by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

    val polylineData by mainViewModel.polyline.collectAsStateWithLifecycle()

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
        } else {
            polyline?.removePolyline()
            pickupMarker?.removeMarker()
            dropMarker?.removeMarker()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView }
        )
    }
}

@Composable
fun LocationItemLayout(mainViewModel: MainViewModel, status: GrpcConstant.DriverAction) {
    val pickAddress by mainViewModel.pickupAddress.collectAsStateWithLifecycle()
    val dropAddress by mainViewModel.dropAddress.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .height(85.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.location_icon),
            contentDescription = "image"
        )

        Column {
            Text(
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED, GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> pickAddress.first
                    GrpcConstant.DriverAction.RIDE_STARTED -> dropAddress.first
                    else -> { "" }
                },
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED, GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> pickAddress.second
                    GrpcConstant.DriverAction.RIDE_STARTED -> dropAddress.second
                    else -> { "" }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SliderButton(
    modifier: Modifier = Modifier,
    text: String,
    onConfirmed: () -> Unit
) {
    val threshold = 0.8f
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(Color.Green)
        .pointerInput(Unit) {
            containerWidth = size.width.toFloat()
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (sliderPosition >= threshold) {
                        onConfirmed()
                    }
                    sliderPosition = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    sliderPosition = (sliderPosition + dragAmount / containerWidth)
                        .coerceIn(0f, 1f)
                }
            )
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFF6200EE),
            style = TextStyle(

                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        val knobWidth = 70.dp
        Box(
            modifier = Modifier
                .offset {
                    val maxOffset = max(0f, ((containerWidth + 16.dp.toPx()) - knobWidth.toPx()))
                    val offset = sliderPosition * maxOffset
                    IntOffset(offset.toInt(), 0)
                }
                .width(knobWidth)
                .fillMaxHeight()
                .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                .background(Color(0xFF6200EE))
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "",
                tint = Color.White,
            )
        }
    }
}

@Composable
fun RideCompletedView() {
    val activity = LocalActivity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(color = Color.White),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp),
            text = "Your ride is completed.",
            style = TextStyle(
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
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
                activity?.let {
                    val intent = MainActivity.start(
                        it
                    )
                    it.startActivity(intent)
                    it.finish()
                }
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


@Preview(showBackground = true)
@Composable
private fun RideStatusPreview() {
    SliderButton(text = "confirm", onConfirmed = {})
}